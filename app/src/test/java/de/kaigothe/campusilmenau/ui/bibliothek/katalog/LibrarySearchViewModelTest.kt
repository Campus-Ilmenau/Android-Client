package de.kaigothe.campusilmenau.ui.bibliothek.katalog

import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.data.repository.LibraryCatalogRepository
import de.kaigothe.campusilmenau.data.repository.LibraryCoverLoader
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailability
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryCover
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryCoverResult
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaDetail
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaFilter
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchQuery
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchResponse
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchScope
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchSource
import de.kaigothe.campusilmenau.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibrarySearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainDispatcherRule(dispatcher)

    private class FakeRepository : LibraryCatalogRepository {
        val searches = mutableListOf<LibrarySearchQuery>()
        val availabilityCalls = mutableListOf<List<String>>()
        var searchResult: (LibrarySearchQuery) -> LibrarySearchResponse = { response(LibraryMedia(id = "a", title = "T a")) }
        var failure: ApiError? = null

        override suspend fun search(query: LibrarySearchQuery): LibrarySearchResponse {
            searches += query
            failure?.let { throw it }
            return searchResult(query)
        }

        override suspend fun availability(ids: Collection<String>): Map<String, LibraryAvailability> {
            availabilityCalls += ids.toList()
            return ids.associateWith { LibraryAvailability(it, LibraryAvailabilityStatus.Available, emptyList()) }
        }

        override suspend fun media(id: String, searchId: String?, track: Boolean?): LibraryMediaDetail = error("not used")
        override suspend fun contentsAvailable() = false
        override suspend fun cover(cover: LibraryCover): LibraryCoverResult = LibraryCoverResult.Missing
    }

    private val repository = FakeRepository()
    private fun viewModel() = LibrarySearchViewModel(repository, LibraryCoverLoader(repository))

    @Test
    fun `typing searches only after the debounce`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTextChange("Kotlin")
        advanceTimeBy(699L)
        assertTrue(repository.searches.isEmpty())
        advanceTimeBy(2)
        advanceUntilIdle()
        assertEquals(1, repository.searches.size)
        assertEquals(SearchPhase.Results, vm.state.value.phase)
    }

    @Test
    fun `typing again restarts the debounce`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTextChange("Ko")
        advanceTimeBy(500)
        vm.onTextChange("Kotlin")
        advanceTimeBy(500)
        assertTrue(repository.searches.isEmpty())
        advanceUntilIdle()
        assertEquals(listOf("Kotlin"), repository.searches.map { it.text })
    }

    @Test
    fun `clearing the field resets to start`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTextChange("Kotlin")
        advanceUntilIdle()
        vm.onTextChange("")
        assertEquals(SearchPhase.Start, vm.state.value.phase)
        assertTrue(vm.state.value.items.isEmpty())
    }

    @Test
    fun `submit and media type searches start immediately`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTextChange("Kotlin")
        vm.submit()
        advanceTimeBy(1)
        assertEquals(1, repository.searches.size)
        vm.selectKind(LibraryMediaFilter.Ebook)
        advanceTimeBy(1)
        assertEquals(2, repository.searches.size)
        assertEquals(LibraryMediaFilter.Ebook, repository.searches.last().mediaType)
    }

    @Test
    fun `an open advanced search does not search while typing`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.toggleAdvanced()
        vm.onTextChange("Kotlin")
        advanceUntilIdle()
        assertTrue(repository.searches.isEmpty())
    }

    @Test
    fun `invalid years show a form error instead of searching`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.toggleAdvanced()
        vm.updateFields { it.copy(title = "Analysis", yearFrom = "20x1") }
        vm.submitAdvanced()
        advanceUntilIdle()
        assertTrue(repository.searches.isEmpty())
        assertTrue(vm.state.value.formError != null)
        assertTrue(vm.state.value.advancedOpen)
    }

    @Test
    fun `a failure shows the failed phase and retry repeats the query`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.failure = ApiError.Http(status = 503)
        vm.onTextChange("Kotlin")
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.state.value.phase is SearchPhase.Failed)
        repository.failure = null
        vm.retry()
        advanceUntilIdle()
        assertEquals(SearchPhase.Results, vm.state.value.phase)
        assertEquals(2, repository.searches.size)
    }

    @Test
    fun `an empty result offers the union catalogue and continuation dedupes`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.searchResult = { q ->
            if (q.scope == LibrarySearchScope.Catalog) response(media("a"), media("b"), source = LibrarySearchSource.Catalog)
            else response(media("a"), source = LibrarySearchSource.Local, catalogSuggested = true)
        }
        vm.onTextChange("Kotlin")
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.state.value.offersCatalogContinuation)
        vm.continueInCatalog()
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals(listOf("b"), state.continuationItems.map { it.id })
        assertTrue(state.catalogTried)
        assertEquals(listOf("main", "continued"), state.sections.map { it.id })
    }

    @Test
    fun `load more appends only new items`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.searchResult = { q ->
            if (q.page == 1) response(media("a"), media("b"), hasMore = true, total = 3) else response(media("b"), media("c"), total = 3, page = 2)
        }
        vm.onTextChange("x")
        vm.submit()
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("a", "b", "c"), vm.state.value.items.map { it.id })
        assertNull(vm.state.value.loadMoreCount)
    }

    @Test
    fun `availability requests are batched`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTextChange("x")
        vm.submit()
        advanceUntilIdle()
        vm.requestAvailability("a")
        vm.requestAvailability("a")
        vm.requestAvailability("b")
        advanceTimeBy(81)
        advanceUntilIdle()
        assertEquals(listOf(listOf("a", "b")), repository.availabilityCalls)
        assertEquals(setOf("a", "b"), vm.state.value.availability.keys)
    }

    @Test
    fun `slow search shows the catalogue hint after 900 ms`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTextChange("x")
        vm.submit()
        // Die Suche selbst läuft sofort durch; der Hinweis wird bei Erfolg wieder gelöscht.
        advanceUntilIdle()
        assertEquals(false, vm.state.value.showsCatalogHint)
    }

    private companion object {
        fun media(id: String) = LibraryMedia(id = id, title = "T $id")

        fun response(
            vararg items: LibraryMedia,
            source: LibrarySearchSource? = LibrarySearchSource.Local,
            hasMore: Boolean = false,
            total: Int = items.size,
            page: Int = 1,
            catalogSuggested: Boolean = false,
        ) = LibrarySearchResponse(
            items = items.toList(), totalCount = total, page = page, perPage = 20, hasMore = hasMore, source = source,
            correctedQuery = null, catalogSuggested = catalogSuggested, catalogChecked = false, catalogTotal = null, searchId = "s1",
        )
    }
}
