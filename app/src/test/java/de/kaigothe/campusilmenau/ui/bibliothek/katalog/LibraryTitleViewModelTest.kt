package de.kaigothe.campusilmenau.ui.bibliothek.katalog

import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.data.local.MerklisteDao
import de.kaigothe.campusilmenau.data.local.MerklisteTitelEntity
import de.kaigothe.campusilmenau.data.repository.LibraryCatalogRepository
import de.kaigothe.campusilmenau.data.repository.LibraryCoverLoader
import de.kaigothe.campusilmenau.data.repository.RoomMerklisteRepository
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailability
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryCover
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryCoverResult
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaDetail
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchQuery
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchResponse
import de.kaigothe.campusilmenau.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Testfälle aus `LibraryTitleTests` der iOS-App (Laden, Fehler, Aktualisieren, Merken), ohne Netz und ohne Room. */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryTitleViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private class FakeCatalog : LibraryCatalogRepository {
        class MediaCall(val searchId: String?, val track: Boolean?)

        val calls = mutableListOf<MediaCall>()
        var detail: LibraryMediaDetail = sampleDetail("med_1")
        var failure: ApiError? = null
        var contents = true
        var contentsFailure: ApiError? = null

        override suspend fun media(id: String, searchId: String?, track: Boolean?): LibraryMediaDetail {
            calls += MediaCall(searchId, track)
            failure?.let { throw it }
            return detail
        }

        override suspend fun contentsAvailable(): Boolean {
            contentsFailure?.let { throw it }
            return contents
        }

        override suspend fun search(query: LibrarySearchQuery): LibrarySearchResponse = error("not used")
        override suspend fun availability(ids: Collection<String>): Map<String, LibraryAvailability> = emptyMap()
        override suspend fun cover(cover: LibraryCover): LibraryCoverResult = LibraryCoverResult.Missing
    }

    private class FakeDao : MerklisteDao {
        val rows = MutableStateFlow<List<MerklisteTitelEntity>>(emptyList())
        override fun observeAll() = rows
        override suspend fun find(id: String) = rows.value.firstOrNull { it.id == id }
        override suspend fun insertIfAbsent(entity: MerklisteTitelEntity): Long {
            if (find(entity.id) != null) return -1
            rows.value += entity
            return 1
        }
        override suspend fun replace(entity: MerklisteTitelEntity) {
            rows.value = rows.value.filter { it.id != entity.id } + entity
        }
        override suspend fun delete(id: String) { rows.value = rows.value.filter { it.id != id } }
        override suspend fun deleteAll() { rows.value = emptyList() }
    }

    private val catalog = FakeCatalog()
    private val dao = FakeDao()
    private val clock = Clock.fixed(Instant.parse("2026-03-12T10:00:00Z"), ZoneOffset.UTC)
    private val merkliste = RoomMerklisteRepository(dao, clock)

    private fun viewModel(id: String = "med_1", searchId: String? = "s1") =
        LibraryTitleViewModel(id, searchId, catalog, LibraryCoverLoader(catalog), merkliste, clock)

    private fun kotlinx.coroutines.test.TestScope.watch(vm: LibraryTitleViewModel) {
        backgroundScope.launch { vm.state.collect { } }
    }

    @Test
    fun `loading sends the search id and counts the view only once`() = runTest(dispatcher) {
        val vm = viewModel()
        watch(vm)
        advanceUntilIdle()
        assertEquals("s1", catalog.calls.single().searchId)
        // `null` lässt den Server zählen; erst das Neuladen sagt ausdrücklich „nicht zählen“.
        assertNull(catalog.calls.single().track)

        vm.refresh()
        advanceUntilIdle()
        assertEquals(2, catalog.calls.size)
        assertEquals(false, catalog.calls.last().track)
    }

    @Test
    fun `a loaded title shows its detail and the contents setting of the catalog`() = runTest(dispatcher) {
        val vm = viewModel()
        watch(vm)
        advanceUntilIdle()
        assertEquals(TitlePhase.Loaded, vm.state.value.phase)
        assertEquals("med_1", vm.state.value.media?.id)
        assertTrue(vm.state.value.contentsEnabled)
    }

    @Test
    fun `contents that cannot be asked for do not stop the title`() = runTest(dispatcher) {
        catalog.contentsFailure = ApiError.Http(status = 503)
        val vm = viewModel()
        watch(vm)
        advanceUntilIdle()
        assertEquals(TitlePhase.Loaded, vm.state.value.phase)
        assertFalse(vm.state.value.contentsEnabled)
        assertFalse(vm.state.value.showsContents)
    }

    @Test
    fun `a missing title shows not found`() = runTest(dispatcher) {
        catalog.failure = ApiError.Http(status = 404)
        val vm = viewModel()
        watch(vm)
        advanceUntilIdle()
        assertEquals(TitlePhase.NotFound, vm.state.value.phase)
    }

    @Test
    fun `an unavailable catalog shows failure and retry recovers`() = runTest(dispatcher) {
        catalog.failure = ApiError.Http(status = 503)
        val vm = viewModel()
        watch(vm)
        advanceUntilIdle()
        assertEquals(TitlePhase.Failed, vm.state.value.phase)

        catalog.failure = null
        vm.refresh()
        advanceUntilIdle()
        assertEquals(TitlePhase.Loaded, vm.state.value.phase)
    }

    @Test
    fun `a failed reload keeps the shown title and says so once`() = runTest(dispatcher) {
        val vm = viewModel()
        watch(vm)
        advanceUntilIdle()

        catalog.failure = ApiError.Http(status = 503)
        vm.refresh()
        advanceUntilIdle()
        assertEquals(TitlePhase.Loaded, vm.state.value.phase)
        assertNotNull(vm.state.value.detail)
        assertTrue(vm.state.value.refreshFailed)

        vm.refreshFailedShown()
        advanceUntilIdle()
        assertFalse(vm.state.value.refreshFailed)
    }

    @Test
    fun `the contents folds open and close on their own`() = runTest(dispatcher) {
        val vm = viewModel()
        watch(vm)
        advanceUntilIdle()
        assertTrue(vm.state.value.tocOpen)
        assertFalse(vm.state.value.blurbOpen)
        vm.toggleToc()
        vm.toggleBlurb()
        advanceUntilIdle()
        assertFalse(vm.state.value.tocOpen)
        assertTrue(vm.state.value.blurbOpen)
    }

    @Test
    fun `bookmarking puts the title on the list and tells the person`() = runTest(dispatcher) {
        val vm = viewModel()
        watch(vm)
        val messages = mutableListOf<TitelMeldung>()
        backgroundScope.launch { vm.meldungen.collect { messages += it } }
        advanceUntilIdle()
        assertFalse(vm.state.value.isBookmarked)

        vm.toggleBookmark(more = "u. a.")
        advanceUntilIdle()
        assertTrue(vm.state.value.isBookmarked)
        assertEquals(listOf(TitelMeldung.Gemerkt), messages)
        assertEquals("T med_1", merkliste.liste.first().offen.single().titel)

        vm.toggleBookmark(more = "u. a.")
        advanceUntilIdle()
        assertFalse(vm.state.value.isBookmarked)
        assertEquals(listOf(TitelMeldung.Gemerkt, TitelMeldung.Entfernt), messages)
        assertTrue(merkliste.liste.first().isEmpty)
    }

    @Test
    fun `bookmarking needs a loaded title`() = runTest(dispatcher) {
        catalog.failure = ApiError.Http(status = 503)
        val vm = viewModel()
        watch(vm)
        advanceUntilIdle()
        vm.toggleBookmark(more = "u. a.")
        advanceUntilIdle()
        assertTrue(merkliste.liste.first().isEmpty)
    }

    @Test
    fun `a title already on the list shows as bookmarked from the start`() = runTest(dispatcher) {
        merkliste.merken(de.kaigothe.campusilmenau.domain.model.library.merkliste.GemerkterTitel(id = "med_1", titel = "T", gemerktAm = clock.instant()))
        val vm = viewModel()
        watch(vm)
        advanceUntilIdle()
        assertTrue(vm.state.value.isBookmarked)
    }
}

private fun sampleDetail(id: String) = LibraryMediaDetail(
    media = LibraryMedia(id = id, title = "T $id", authors = listOf("Forster, Otto"), year = "2016"),
    items = emptyList(),
    availabilityStatus = LibraryAvailabilityStatus.Available,
    contentsStatus = null,
    contents = null,
)
