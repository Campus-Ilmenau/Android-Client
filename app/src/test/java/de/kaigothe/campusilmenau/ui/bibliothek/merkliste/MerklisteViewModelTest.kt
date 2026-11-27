package de.kaigothe.campusilmenau.ui.bibliothek.merkliste

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
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
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryItem
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryLocation
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaDetail
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchQuery
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchResponse
import de.kaigothe.campusilmenau.domain.model.library.merkliste.GemerkterTitel
import de.kaigothe.campusilmenau.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Testfälle aus `BibMerklisteTests` der iOS-App (Status laden, Widerrufen, Meldungen), ohne Netz und ohne Room. */
@OptIn(ExperimentalCoroutinesApi::class)
class MerklisteViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private class FakeCatalog : LibraryCatalogRepository {
        val availabilityCalls = mutableListOf<List<String>>()
        var failure: ApiError? = null
        var answer: (String) -> LibraryAvailability? = { id -> LibraryAvailability(id, LibraryAvailabilityStatus.Available, emptyList()) }

        override suspend fun availability(ids: Collection<String>): Map<String, LibraryAvailability> {
            availabilityCalls += ids.toList()
            failure?.let { throw it }
            return ids.mapNotNull { id -> answer(id)?.let { id to it } }.toMap()
        }

        override suspend fun search(query: LibrarySearchQuery): LibrarySearchResponse = error("not used")
        override suspend fun media(id: String, searchId: String?, track: Boolean?): LibraryMediaDetail = error("not used")
        override suspend fun contentsAvailable() = false
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
    private val clock = Clock.fixed(Instant.parse("2026-03-12T10:00:00Z"), ZoneOffset.UTC)
    private val repository = RoomMerklisteRepository(FakeDao(), clock)

    private fun viewModel() = MerklisteViewModel(repository, catalog, LibraryCoverLoader(catalog))

    private fun titel(id: String, minutes: Long = 1) =
        GemerkterTitel(id = id, titel = "T $id", gemerktAm = Instant.parse("2026-03-01T10:00:00Z").plusSeconds(minutes * 60))

    private fun TestScope.watch(vm: MerklisteViewModel) {
        backgroundScope.launch { vm.state.collect { } }
    }

    private fun TestScope.collectMessages(vm: MerklisteViewModel): List<MerklisteMeldung> {
        val messages = mutableListOf<MerklisteMeldung>()
        backgroundScope.launch { vm.meldungen.collect { messages += it } }
        return messages
    }

    // --- Status laden

    @Test
    fun `the status of every title is asked for in one call`() = runTest(dispatcher) {
        repository.merken(titel("med_1", 1))
        repository.merken(titel("med_2", 2))
        val vm = viewModel()
        watch(vm)
        vm.ladeStatus()
        advanceUntilIdle()
        assertEquals(1, catalog.availabilityCalls.size)
        assertEquals(setOf("med_1", "med_2"), catalog.availabilityCalls.single().toSet())
        assertEquals(LibraryAvailabilityStatus.Available, vm.state.value.status["med_1"])
    }

    @Test
    fun `an empty list sends no request`() = runTest(dispatcher) {
        val vm = viewModel()
        watch(vm)
        vm.ladeStatus()
        advanceUntilIdle()
        assertTrue(catalog.availabilityCalls.isEmpty())
    }

    @Test
    fun `a failure shows unknown and keeps every entry`() = runTest(dispatcher) {
        repository.merken(titel("med_1", 1))
        repository.merken(titel("med_2", 2))
        catalog.failure = ApiError.Http(status = 503)
        val vm = viewModel()
        watch(vm)
        vm.ladeStatus()
        advanceUntilIdle()
        assertEquals(LibraryAvailabilityStatus.Unknown, vm.state.value.status["med_1"])
        assertEquals(LibraryAvailabilityStatus.Unknown, vm.state.value.status["med_2"])
        assertEquals(2, vm.state.value.liste.anzahl)
    }

    @Test
    fun `an id missing in the answer counts as unknown`() = runTest(dispatcher) {
        repository.merken(titel("med_1", 1))
        repository.merken(titel("med_2", 2))
        catalog.answer = { id -> if (id == "med_1") LibraryAvailability(id, LibraryAvailabilityStatus.OnLoan, emptyList()) else null }
        val vm = viewModel()
        watch(vm)
        vm.ladeStatus()
        advanceUntilIdle()
        assertEquals(LibraryAvailabilityStatus.OnLoan, vm.state.value.status["med_1"])
        assertEquals(LibraryAvailabilityStatus.Unknown, vm.state.value.status["med_2"])
    }

    @Test
    fun `reloading refreshes the status`() = runTest(dispatcher) {
        repository.merken(titel("med_1"))
        val vm = viewModel()
        watch(vm)
        vm.ladeStatus()
        advanceUntilIdle()
        assertEquals(LibraryAvailabilityStatus.Available, vm.state.value.status["med_1"])

        catalog.answer = { id -> LibraryAvailability(id, LibraryAvailabilityStatus.OnLoan, emptyList()) }
        vm.ladeStatus()
        advanceUntilIdle()
        assertEquals(LibraryAvailabilityStatus.OnLoan, vm.state.value.status["med_1"])
        assertEquals(2, catalog.availabilityCalls.size)
    }

    @Test
    fun `the exemplar takes signature and location of the best copy and is not stored`() = runTest(dispatcher) {
        repository.merken(titel("med_1"))
        val item = LibraryItem(
            label = "ZQ 5000 R444(16)-1", location = LibraryLocation(null, null, "Lehrbuchsammlung", "https://example.org/plan"),
            status = LibraryAvailabilityStatus.Available, dueDate = null, reserveUrl = null, queue = null,
        )
        catalog.answer = { id -> LibraryAvailability(id, LibraryAvailabilityStatus.Available, listOf(item)) }
        val vm = viewModel()
        watch(vm)
        vm.ladeStatus()
        advanceUntilIdle()
        val exemplar = vm.state.value.exemplare["med_1"]
        assertNotNull(exemplar)
        assertEquals("Lehrbuchsammlung", exemplar!!.standort)
        assertEquals("ZQ 5000 R444(16)-1", exemplar.signatur)
        // Die Liste selbst kennt nur, was gemerkt wurde: kein Status im Eintrag.
        assertEquals("T med_1", repository.liste.first().offen.single().titel)
    }

    // --- Meldungen und Widerrufen

    @Test
    fun `removing says so and can be undone with the same position`() = runTest(dispatcher) {
        repository.merken(titel("med_1", 1))
        repository.merken(titel("med_2", 2))
        val vm = viewModel()
        watch(vm)
        val messages = collectMessages(vm)
        advanceUntilIdle()

        vm.entfernen(titel("med_2", 2))
        advanceUntilIdle()
        assertEquals(listOf("med_1"), repository.liste.first().offen.map { it.id })
        val message = messages.single()
        assertEquals(UiText(R.string.bib_merkliste_entfernt, UiText.plain("T med_2")), message.text)
        val undo = message.widerruf as MerklisteWiderruf.Entfernt

        vm.widerrufen(undo)
        advanceUntilIdle()
        assertEquals(listOf("med_2", "med_1"), repository.liste.first().offen.map { it.id })
        // Nach dem Zurückholen lädt der Status des Titels wieder.
        assertTrue(catalog.availabilityCalls.isNotEmpty())
    }

    @Test
    fun `marking found moves the entry and undoing puts it back`() = runTest(dispatcher) {
        repository.merken(titel("med_1"))
        val vm = viewModel()
        watch(vm)
        val messages = collectMessages(vm)
        advanceUntilIdle()

        vm.setzeGefunden(titel("med_1"), true)
        advanceUntilIdle()
        assertEquals(listOf("med_1"), repository.liste.first().gefunden.map { it.id })
        val message = messages.single()
        assertEquals(UiText(R.string.bib_merkliste_gefunden_meldung, UiText.plain("T med_1")), message.text)

        vm.widerrufen(message.widerruf!!)
        advanceUntilIdle()
        assertTrue(repository.liste.first().gefunden.isEmpty())
        assertEquals(listOf("med_1"), repository.liste.first().offen.map { it.id })
    }

    @Test
    fun `an entry that is gone gives no message`() = runTest(dispatcher) {
        val vm = viewModel()
        watch(vm)
        val messages = collectMessages(vm)
        vm.entfernen(titel("missing"))
        vm.setzeGefunden(titel("missing"), true)
        advanceUntilIdle()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `clearing asks first, empties the list and cannot be undone`() = runTest(dispatcher) {
        repository.merken(titel("med_1"))
        repository.merken(titel("med_2"))
        val vm = viewModel()
        watch(vm)
        val messages = collectMessages(vm)
        vm.zeigeLeerenDialog()
        advanceUntilIdle()
        assertTrue(vm.state.value.clearDialogVisible)

        vm.leeren()
        advanceUntilIdle()
        assertTrue(repository.liste.first().isEmpty)
        assertEquals(false, vm.state.value.clearDialogVisible)
        assertNull(messages.single().widerruf)
        assertEquals(UiText(R.string.bib_merkliste_geleert), messages.single().text)
    }

    @Test
    fun `dismissing the dialog keeps the list`() = runTest(dispatcher) {
        repository.merken(titel("med_1"))
        val vm = viewModel()
        watch(vm)
        vm.zeigeLeerenDialog()
        vm.schliesseLeerenDialog()
        advanceUntilIdle()
        assertEquals(false, vm.state.value.clearDialogVisible)
        assertEquals(1, repository.liste.first().anzahl)
    }

    @Test
    fun `the summary follows the counts`() = runTest(dispatcher) {
        val vm = viewModel()
        watch(vm)
        advanceUntilIdle()
        assertEquals(UiText(R.string.bib_merkliste_noch_keine), vm.state.value.zusammenfassung)

        repository.merken(titel("med_1", 1))
        advanceUntilIdle()
        assertEquals(UiText(R.string.bib_merkliste_ein_titel), vm.state.value.zusammenfassung)

        repository.merken(titel("med_2", 2))
        advanceUntilIdle()
        assertEquals(UiText(R.string.bib_merkliste_zusammen, 2), vm.state.value.zusammenfassung)

        repository.setzeGefunden("med_1", true)
        advanceUntilIdle()
        assertEquals(UiText(R.string.bib_merkliste_zusammen_gefunden, 2, 1), vm.state.value.zusammenfassung)
    }
}
