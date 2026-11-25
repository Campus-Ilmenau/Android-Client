package de.kaigothe.campusilmenau.ui.events

import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.data.repository.EventsRepository
import de.kaigothe.campusilmenau.data.repository.EventsRequest
import de.kaigothe.campusilmenau.data.repository.EventsRequests
import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventFilter
import de.kaigothe.campusilmenau.domain.model.events.EventKind
import de.kaigothe.campusilmenau.domain.model.events.EventOrganizer
import de.kaigothe.campusilmenau.domain.model.events.EventPage
import de.kaigothe.campusilmenau.domain.model.events.EventsSelection
import de.kaigothe.campusilmenau.testing.InMemoryPreferences
import de.kaigothe.campusilmenau.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    /** Dienstag, 24.11.2026, 11:00 Berliner Zeit. */
    private val today = LocalDate.of(2026, 11, 24)
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = BerlinTime.zone
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = BerlinTime.at(today, 11 * 60)
    }

    private class FakeEvents : EventsRepository {
        val calls = mutableListOf<Pair<EventFilter, Int>>()
        var handler: (EventFilter, Int) -> EventPage = { _, _ -> EventPage(emptyList(), 1, false, 0) }
        var organizers: List<EventOrganizer> = emptyList()
        var fail = false

        override suspend fun list(filter: EventFilter, page: Int): EventPage {
            calls += filter to page
            if (fail) throw ApiError.Network(IOException("offline"))
            return handler(filter, page)
        }

        override suspend fun detail(id: String): CampusEvent = throw UnsupportedOperationException()
        override suspend fun organizers(): List<EventOrganizer> = organizers
    }

    private fun event(id: String, date: LocalDate, hour: Int = 18, end: Instant? = null) =
        CampusEvent(id = id, title = "Event $id", start = BerlinTime.at(date, hour * 60), end = end)

    private fun page(vararg events: CampusEvent, hasMore: Boolean = false, number: Int = 1) =
        EventPage(events.toList(), number, hasMore, events.size)

    private val settings = AppSettings(InMemoryPreferences())
    private val requests = EventsRequests()

    private fun TestScope.viewModel(
        repository: FakeEvents,
        mode: EventsMode = EventsMode.Upcoming,
        initial: EventsSelection = EventsSelection(),
    ) = EventsViewModel(mode, initial, repository, settings, requests, clock).also { advanceUntilIdle() }

    @Test
    fun `die Liste laedt beim Start und gruppiert nach Tagen`() = runTest {
        val repository = FakeEvents().apply {
            handler = { _, _ -> page(event("b", today.plusDays(2)), event("a", today), event("c", today)) }
        }
        val viewModel = viewModel(repository)

        val state = viewModel.state.value
        assertEquals(EventsLoadState.Loaded, state.loadState)
        assertEquals(listOf(today, today.plusDays(2)), state.sections.map { it.day })
        assertEquals(listOf("a", "c"), state.sections.first().events.map { it.id })
    }

    @Test
    fun `ein Fehler zeigt Failed und Erneut versuchen laedt neu`() = runTest {
        val repository = FakeEvents().apply { fail = true }
        val viewModel = viewModel(repository)
        assertEquals(EventsLoadState.Failed, viewModel.state.value.loadState)

        repository.fail = false
        repository.handler = { _, _ -> page(event("a", today)) }
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(EventsLoadState.Loaded, viewModel.state.value.loadState)
        assertEquals(1, viewModel.state.value.events.size)
    }

    @Test
    fun `eine Aenderung der Abfrage laedt neu, Gemerkt und Kopf nicht`() = runTest {
        val repository = FakeEvents()
        val viewModel = viewModel(repository)
        val callsAfterStart = repository.calls.size

        viewModel.select { it.withDay(today.plusDays(1)) }
        advanceUntilIdle()
        assertTrue(repository.calls.size > callsAfterStart)
        assertEquals(today.plusDays(1), repository.calls.last().first.day)

        val callsAfterDay = repository.calls.size
        viewModel.select { it.copy(onlyBookmarked = true) }
        advanceUntilIdle()
        assertEquals(callsAfterDay, repository.calls.size)

        viewModel.select { it.copy(kind = EventKind.Deadline, onlyBookmarked = false) }
        advanceUntilIdle()
        assertEquals("Frist", repository.calls.last().first.category)
    }

    @Test
    fun `Gemerkt zeigt nur gemerkte und laedt dazu alle Seiten`() = runTest {
        val repository = FakeEvents().apply {
            handler = { _, number ->
                if (number == 1) page(event("a", today), hasMore = true, number = 1) else page(event("b", today), number = 2)
            }
        }
        val viewModel = viewModel(repository)
        assertEquals(1, viewModel.state.value.events.size)

        viewModel.toggleBookmark("b")
        viewModel.select { it.copy(onlyBookmarked = true) }
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), viewModel.state.value.events.map { it.id })
        assertEquals(listOf("b"), viewModel.state.value.visibleEvents.map { it.id })
        assertFalse(viewModel.state.value.hasMore)
    }

    @Test
    fun `eine gemerkte Veranstaltung laesst sich wieder entfernen`() = runTest {
        val viewModel = viewModel(FakeEvents())

        viewModel.toggleBookmark("a")
        advanceUntilIdle()
        assertEquals(setOf("a"), viewModel.state.value.bookmarks)

        viewModel.toggleBookmark("a")
        advanceUntilIdle()
        assertEquals(emptySet<String>(), viewModel.state.value.bookmarks)
    }

    @Test
    fun `weitere Seiten haengen sich ohne Doppelte an`() = runTest {
        val repository = FakeEvents().apply {
            handler = { _, number ->
                if (number == 1) page(event("a", today), event("b", today), hasMore = true) else page(event("b", today), event("c", today), number = 2)
            }
        }
        val viewModel = viewModel(repository)

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c"), viewModel.state.value.events.map { it.id })
        assertFalse(viewModel.state.value.hasMore)
        assertFalse(viewModel.state.value.loadingMore)
    }

    @Test
    fun `ein Fehler beim Nachladen laesst die Liste stehen`() = runTest {
        val repository = FakeEvents().apply { handler = { _, _ -> page(event("a", today), hasMore = true) } }
        val viewModel = viewModel(repository)

        repository.fail = true
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(EventsLoadState.Loaded, viewModel.state.value.loadState)
        assertEquals(1, viewModel.state.value.events.size)
        assertFalse(viewModel.state.value.loadingMore)
    }

    @Test
    fun `Veranstalter und Tage mit Veranstaltungen kommen als Beiwerk`() = runTest {
        val repository = FakeEvents().apply {
            organizers = listOf(EventOrganizer("club", "Club", 3))
            handler = { filter, _ ->
                // Die Abfrage der Tagesleiste hat einen Zeitraum mit Ende.
                if (filter.period?.to != null) page(event("x", today.plusDays(3))) else page()
            }
        }
        val viewModel = viewModel(repository)

        assertEquals(listOf("club"), viewModel.state.value.organizers.map { it.id })
        assertEquals(setOf(today.plusDays(3)), viewModel.state.value.daysWithEvents)
        assertEquals(14, viewModel.state.value.days.size)
        assertEquals(today, viewModel.state.value.days.first())
    }

    @Test
    fun `Vergangene fragt den letzten Monat, behaelt nur Beendete und dreht um`() = runTest {
        val repository = FakeEvents().apply {
            handler = { _, _ ->
                page(
                    event("alt", today.minusDays(5)),
                    event("gestern", today.minusDays(1)),
                    // läuft noch heute: gehört zu den kommenden
                    event("laeuft", today.minusDays(2), end = BerlinTime.at(today.plusDays(1), 12 * 60)),
                )
            }
        }
        val viewModel = viewModel(
            repository, EventsMode.Past, EventsSelection(kind = EventKind.Event, hideCancelled = true),
        )

        val filter = repository.calls.first().first
        assertEquals(EventFilter.Period(today.minusDays(30), today), filter.period)
        assertEquals("Veranstaltung", filter.category)
        assertTrue(filter.hideCancelled)
        assertEquals(listOf("gestern", "alt"), viewModel.state.value.sections.flatMap { it.events }.map { it.id })
        assertEquals(listOf(today.minusDays(1), today.minusDays(5)), viewModel.state.value.sections.map { it.day })
        // Keine Veranstalter und keine Tagesleiste für „Vergangene“.
        assertTrue(viewModel.state.value.organizers.isEmpty())
    }

    @Test
    fun `ein Wunsch von aussen setzt die Auswahl`() = runTest {
        val repository = FakeEvents()
        val viewModel = viewModel(repository, initial = EventsSelection(organizerId = "x", organizerName = "X"))

        requests.post(EventsRequest.ShowEvening)
        advanceUntilIdle()

        val selection = viewModel.state.value.selection
        assertTrue(selection.eveningOnly)
        assertEquals(today, selection.day)
        assertEquals(null, selection.organizerId)
        assertEquals(null, requests.pending.value)

        requests.post(EventsRequest.ShowAll)
        advanceUntilIdle()
        assertEquals(EventsSelection(), viewModel.state.value.selection)
    }

    @Test
    fun `ein spaeter angelegter Verlauf uebernimmt einen wartenden Wunsch`() = runTest {
        requests.post(EventsRequest.ShowEvening)

        val viewModel = viewModel(FakeEvents())

        assertTrue(viewModel.state.value.selection.eveningOnly)
    }
}
