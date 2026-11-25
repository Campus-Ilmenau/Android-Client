package de.kaigothe.campusilmenau.ui.heute

import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.data.repository.HeuteEventsSource
import de.kaigothe.campusilmenau.data.repository.NoticesSource
import de.kaigothe.campusilmenau.domain.model.heute.HeuteEvent
import de.kaigothe.campusilmenau.domain.model.heute.Hinweis
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.testing.FakeMensaRepository
import de.kaigothe.campusilmenau.testing.InMemoryPreferences
import de.kaigothe.campusilmenau.testing.MainDispatcherRule
import de.kaigothe.campusilmenau.testing.testCanteen
import de.kaigothe.campusilmenau.testing.testMeal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class HeuteViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val day = LocalDate.of(2026, 11, 24)

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = BerlinTime.zone
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }

    private class FakeEvents(
        var evening: suspend () -> List<HeuteEvent> = { emptyList() },
        var tomorrow: suspend () -> List<HeuteEvent> = { emptyList() },
    ) : HeuteEventsSource {
        var calls = 0
        override suspend fun evening(now: Instant): List<HeuteEvent> {
            calls++
            return evening.invoke()
        }

        override suspend fun tomorrow(now: Instant): List<HeuteEvent> = tomorrow.invoke()
    }

    private class FakeNotices(var list: List<Hinweis> = emptyList()) : NoticesSource {
        override suspend fun notices(now: Instant): List<Hinweis> = list
    }

    private fun at(hour: Int, minute: Int = 0, date: LocalDate = day): Instant = BerlinTime.at(date, hour * 60 + minute)

    private val settings = AppSettings(InMemoryPreferences())
    private val meal = testMeal("m1", "Pasta")

    private fun repository(vararg dates: LocalDate) = FakeMensaRepository(
        mealsByDay = dates.associateWith { listOf(meal) },
        canteens = listOf(testCanteen("Mensa Ehrenberg")),
    )

    private fun TestScope.viewModel(
        clock: MutableClock,
        repository: FakeMensaRepository = repository(day, day.plusDays(1)),
        events: FakeEvents = FakeEvents(),
        notices: FakeNotices = FakeNotices(),
    ) = HeuteViewModel(repository, events, notices, settings, clock).also { advanceUntilIdle() }

    @Test
    fun `der erste Ladevorgang fuellt alle Teile`() = runTest {
        val viewModel = viewModel(MutableClock(at(12)))
        assertFalse(viewModel.state.value.loaded)

        viewModel.load()

        val state = viewModel.state.value
        assertTrue(state.loaded)
        assertEquals(listOf(meal), state.mealsToday)
        assertEquals(listOf(meal), state.mealsTomorrow)
        assertNotNull(state.canteens)
        assertEquals(emptyList<HeuteEvent>(), state.eventsEvening)
    }

    @Test
    fun `fehlt ein Teil, entfaellt nur sein Abschnitt`() = runTest {
        // Für morgen gibt es keinen Speiseplan (Netzfehler), der Rest kommt.
        val viewModel = viewModel(MutableClock(at(12)), repository = repository(day))

        viewModel.load()

        val state = viewModel.state.value
        assertTrue(state.loaded)
        assertNotNull(state.mealsToday)
        assertNull(state.mealsTomorrow)
    }

    @Test
    fun `ein Fehler der Events-Quelle laesst die Seite nicht haengen`() = runTest {
        val events = FakeEvents(evening = { throw ApiError.Network(IOException("offline")) })
        val viewModel = viewModel(MutableClock(at(12)), events = events)

        viewModel.load()

        assertTrue(viewModel.state.value.loaded)
        assertNull(viewModel.state.value.eventsEvening)
        assertNotNull(viewModel.state.value.eventsTomorrow)
    }

    @Test
    fun `nach dem Zeitlimit entfaellt der Teil und die Seite ist trotzdem geladen`() = runTest {
        val events = FakeEvents(evening = {
            delay(60.seconds)
            emptyList()
        })
        val viewModel = viewModel(MutableClock(at(12)), events = events)

        viewModel.load()

        assertTrue(viewModel.state.value.loaded)
        assertNull(viewModel.state.value.eventsEvening)
        assertNotNull(viewModel.state.value.mealsToday)
    }

    @Test
    fun `refresh laedt erst nach Phasenwechsel, neuem Tag oder zehn Minuten neu`() = runTest {
        val clock = MutableClock(at(11, 0))
        val events = FakeEvents()
        val viewModel = viewModel(clock, events = events)
        viewModel.load()
        assertEquals(1, events.calls)

        clock.now = at(11, 5)
        viewModel.refresh()
        assertEquals(1, events.calls)
        assertEquals(at(11, 5), viewModel.state.value.now)

        clock.now = at(11, 11)
        viewModel.refresh()
        assertEquals(2, events.calls)

        // Phasenwechsel 14:00 nach weniger als zehn Minuten.
        clock.now = at(13, 59)
        viewModel.load()
        val before = events.calls
        clock.now = at(14, 1)
        viewModel.refresh()
        assertEquals(before + 1, events.calls)
    }

    @Test
    fun `ein abgebrochener Ladevorgang wird beim naechsten refresh wiederholt`() = runTest {
        // Die Seite war nicht sichtbar (anderer Tab), der Ladevorgang wurde abgebrochen, bevor er fertig war.
        val hang = kotlinx.coroutines.CompletableDeferred<List<HeuteEvent>>()
        val events = FakeEvents(evening = { hang.await() })
        val viewModel = viewModel(MutableClock(at(12)), events = events)
        val job = launch { viewModel.load() }
        // Nur bis zum Hängen, ohne die Zeit bis zum Zeitlimit vorzustellen.
        runCurrent()
        job.cancel()
        assertFalse(viewModel.state.value.loaded)

        events.evening = { emptyList() }
        viewModel.refresh()

        assertTrue(viewModel.state.value.loaded)
        assertEquals(2, events.calls)
    }

    @Test
    fun `ein neuer Tag setzt die Daten von gestern zurueck`() = runTest {
        val clock = MutableClock(at(23, 0))
        val repository = repository(day)
        val viewModel = viewModel(clock, repository = repository)
        viewModel.load()
        assertNotNull(viewModel.state.value.mealsToday)

        // Am nächsten Tag gibt es keinen Speiseplan (Netzfehler): Die Daten von gestern bleiben nicht stehen.
        clock.now = at(8, 0, day.plusDays(1))
        viewModel.refresh()

        assertNull(viewModel.state.value.mealsToday)
        assertTrue(viewModel.state.value.loaded)
    }

    @Test
    fun `die gewaehlte Mensa wird gespeichert`() = runTest {
        val clock = MutableClock(at(12))
        val viewModel = viewModel(clock)
        assertEquals(MensaVenue.MensaEhrenberg, viewModel.state.value.venue)

        viewModel.selectVenue(MensaVenue.Nanoteria)
        advanceUntilIdle()

        assertEquals(MensaVenue.Nanoteria, viewModel.state.value.venue)
        assertEquals(MensaVenue.Nanoteria, viewModel.state.value.content.venue)
        assertEquals(MensaVenue.Nanoteria, viewModel(clock).state.value.venue)
    }

    @Test
    fun `ein weggetippter Hinweis bleibt weg`() = runTest {
        val notice = Hinweis("n1", "Studium", title = "Titel", text = "Text")
        val clock = MutableClock(at(12))
        val viewModel = viewModel(clock, notices = FakeNotices(listOf(notice)))
        viewModel.load()
        assertEquals(listOf("n1"), viewModel.state.value.content.activeNotices.map { it.id })

        viewModel.dismissNotice("n1")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.content.activeNotices.isEmpty())
        assertEquals(setOf("n1"), viewModel.state.value.dismissedNotices)
    }
}
