package de.kaigothe.campusilmenau.ui.mensa

import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.domain.model.mensa.MealBadge
import de.kaigothe.campusilmenau.domain.model.mensa.MensaDiet
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealtime
import de.kaigothe.campusilmenau.domain.model.mensa.MensaStatus
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.domain.model.mensa.OpeningHour
import de.kaigothe.campusilmenau.testing.FakeMensaRepository
import de.kaigothe.campusilmenau.testing.InMemoryPreferences
import de.kaigothe.campusilmenau.testing.MainDispatcherRule
import de.kaigothe.campusilmenau.testing.testCanteen
import de.kaigothe.campusilmenau.testing.testMeal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MensaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    /** Dienstag, 24.11.2026, 10:00 Uhr in Berlin. */
    private val clock = Clock.fixed(Instant.parse("2026-11-24T09:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 11, 24)
    private val tomorrow = LocalDate.of(2026, 11, 25)

    private fun settings() = AppSettings(InMemoryPreferences())

    private fun TestScope.viewModel(repository: FakeMensaRepository, settings: AppSettings = settings()) =
        MensaViewModel(repository, settings, clock).also { advanceUntilIdle() }

    private val pasta = testMeal("pasta", badges = setOf(MealBadge.Vegetarian))
    private val schnitzel = testMeal("schnitzel", allergies = listOf("Gluten"))
    private val lunchCafeteria = testMeal("c-lunch", canteen = "Cafeteria Mensa Ehrenberg")
    private val dinnerCafeteria =
        testMeal("c-dinner", canteen = "Cafeteria Mensa Ehrenberg", category = "Abendessen")

    @Test
    fun `startet mit heutigem Speiseplan und 15 Werktagen`() = runTest {
        val repository = FakeMensaRepository(mapOf(today to listOf(pasta, schnitzel)))
        val viewModel = viewModel(repository)

        val state = viewModel.state.value
        assertEquals(MensaLoadStateLoaded, state.loadState)
        assertEquals(listOf(pasta, schnitzel), state.meals)
        assertEquals(15, state.days.size)
        assertEquals(today, state.selectedDay)
        assertEquals(listOf(today), repository.requestedDays)
        assertFalse(viewModel.loadingGate.isActive.value)
    }

    @Test
    fun `Tagwechsel laedt den neuen Tag`() = runTest {
        val repository = FakeMensaRepository(mapOf(today to listOf(pasta), tomorrow to listOf(schnitzel)))
        val viewModel = viewModel(repository)

        viewModel.selectDay(1)

        assertEquals(tomorrow, viewModel.state.value.selectedDay)
        assertEquals(listOf(schnitzel), viewModel.state.value.meals)
        assertEquals(listOf(today, tomorrow), repository.requestedDays)
    }

    @Test
    fun `gleicher oder ungueltiger Tag laedt nichts`() = runTest {
        val repository = FakeMensaRepository(mapOf(today to listOf(pasta)))
        val viewModel = viewModel(repository)

        viewModel.selectDay(0)
        viewModel.selectDay(-1)
        viewModel.selectDay(15)

        assertEquals(listOf(today), repository.requestedDays)
    }

    @Test
    fun `eine langsame Antwort des frueheren Tags ueberschreibt den neuen nicht`() = runTest {
        val hold = CompletableDeferred<Unit>()
        val repository = FakeMensaRepository(
            mapOf(today to listOf(pasta), tomorrow to listOf(schnitzel)),
            gate = { day -> if (day == today) hold.await() },
        )
        val viewModel = viewModel(repository)
        assertEquals(MensaLoadState.Loading, viewModel.state.value.loadState)

        viewModel.selectDay(1)
        hold.complete(Unit)

        assertEquals(listOf(schnitzel), viewModel.state.value.meals)
        assertEquals(tomorrow, viewModel.state.value.selectedDay)
    }

    @Test
    fun `Fehler zeigt Failed und Erneut versuchen umgeht den Zwischenspeicher`() = runTest {
        val repository = FakeMensaRepository(emptyMap())
        val viewModel = viewModel(repository)
        assertTrue(viewModel.state.value.loadState is MensaLoadState.Failed)
        assertFalse(viewModel.loadingGate.isActive.value)

        repository.mealsByDay = mapOf(today to listOf(pasta))
        viewModel.retry()

        assertEquals(MensaLoadStateLoaded, viewModel.state.value.loadState)
        assertEquals(listOf(false, true), repository.forced)
    }

    @Test
    fun `fehlende Oeffnungszeiten stoeren den Speiseplan nicht`() = runTest {
        val repository = FakeMensaRepository(mapOf(today to listOf(pasta)), canteens = null)
        val viewModel = viewModel(repository)

        assertEquals(MensaLoadStateLoaded, viewModel.state.value.loadState)
        assertEquals(emptyList<Any>(), viewModel.state.value.canteens)
    }

    @Test
    fun `Gerichte je Mensa und Mittag oder Abend der Cafeteria`() = runTest {
        val repository = FakeMensaRepository(mapOf(today to listOf(pasta, lunchCafeteria, dinnerCafeteria)))
        val viewModel = viewModel(repository)
        val state = { viewModel.state.value }

        assertEquals(listOf(pasta), state().mealsFor(MensaVenue.MensaEhrenberg))
        assertEquals(listOf(lunchCafeteria), state().mealsFor(MensaVenue.CafeteriaEhrenberg))

        viewModel.selectMealtime(MensaMealtime.Dinner)
        assertEquals(listOf(dinnerCafeteria), state().mealsFor(MensaVenue.CafeteriaEhrenberg))
        // Die Mensa ohne Mahlzeiten ignoriert die Auswahl.
        assertEquals(listOf(pasta), state().mealsFor(MensaVenue.MensaEhrenberg))
        assertEquals(emptyList<Any>(), state().mealsFor(MensaVenue.Nanoteria))
    }

    @Test
    fun `Filter wirkt auf die Liste und wird gespeichert`() = runTest {
        val settings = settings()
        val repository = FakeMensaRepository(mapOf(today to listOf(pasta, schnitzel)))
        val viewModel = viewModel(repository, settings)

        val filter = MensaFilter(MensaDiet.Vegetarisch)
        viewModel.setFilter(filter)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(filter, state.filter)
        assertEquals(listOf(pasta), state.mealsFor(MensaVenue.MensaEhrenberg))
        assertEquals(listOf(pasta, schnitzel), state.mealsFor(MensaVenue.MensaEhrenberg, filtered = false))
        assertEquals(filter, settings.mensaFilter.first())

        viewModel.resetFilter()
        advanceUntilIdle()
        assertEquals(MensaFilter(), viewModel.state.value.filter)
        assertEquals(MensaFilter(), settings.mensaFilter.first())
    }

    @Test
    fun `gewaehlte Mensa wird gespeichert und beim Start gelesen`() = runTest {
        val settings = settings()
        val repository = FakeMensaRepository(mapOf(today to listOf(pasta)))
        val first = viewModel(repository, settings)
        assertTrue(first.state.value.ready)
        assertEquals(MensaVenue.MensaEhrenberg, first.state.value.venue)

        first.selectVenue(MensaVenue.Roentgenbau)
        advanceUntilIdle()
        assertEquals(MensaVenue.Roentgenbau, first.state.value.venue)
        assertEquals(MensaVenue.Roentgenbau, settings.mensaVenue.first())

        assertEquals(MensaVenue.Roentgenbau, viewModel(repository, settings).state.value.venue)
    }

    @Test
    fun `schnelles Umschalten springt nicht zurueck`() = runTest {
        val settings = settings()
        val viewModel = viewModel(FakeMensaRepository(mapOf(today to listOf(pasta))), settings)

        viewModel.selectVenue(MensaVenue.CafeteriaEhrenberg)
        viewModel.selectVenue(MensaVenue.Nanoteria)
        viewModel.selectVenue(MensaVenue.Roentgenbau)
        advanceUntilIdle()

        assertEquals(MensaVenue.Roentgenbau, viewModel.state.value.venue)
        assertEquals(MensaVenue.Roentgenbau, settings.mensaVenue.first())
    }

    @Test
    fun `Aenderung von aussen kommt an`() = runTest {
        val settings = settings()
        val viewModel = viewModel(FakeMensaRepository(mapOf(today to listOf(pasta))), settings)

        // Zum Beispiel ein Deeplink `campusilmenau://mensa?mensa=nanoteria`.
        settings.setMensaVenue(MensaVenue.Nanoteria)
        advanceUntilIdle()

        assertEquals(MensaVenue.Nanoteria, viewModel.state.value.venue)
    }

    @Test
    fun `Favoriten lassen sich merken und entfernen`() = runTest {
        val settings = settings()
        val viewModel = viewModel(FakeMensaRepository(mapOf(today to listOf(pasta))), settings)

        viewModel.toggleFavorite(pasta)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isFavorite(pasta))
        assertFalse(viewModel.state.value.isFavorite(schnitzel))

        viewModel.toggleFavorite(pasta)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isFavorite(pasta))
    }

    @Test
    fun `Status der Mensa kommt aus den Oeffnungszeiten`() = runTest {
        val canteen = testCanteen("Mensa Ehrenberg", OpeningHour("tue", "11:00", "14:00"))
        val repository = FakeMensaRepository(mapOf(today to listOf(pasta)), canteens = listOf(canteen))
        val viewModel = viewModel(repository)

        val status = viewModel.state.value.status(MensaVenue.MensaEhrenberg, LocalDateTime.of(2026, 11, 24, 10, 0))

        assertEquals(MensaStatus.FromClock("11:00"), status)
        assertEquals(null, viewModel.state.value.status(MensaVenue.Nanoteria, LocalDateTime.of(2026, 11, 24, 10, 0)))
    }

    @Test
    fun `die Uhrzeit ist die Berliner Ortszeit`() = runTest {
        val viewModel = viewModel(FakeMensaRepository(mapOf(today to listOf(pasta))))

        assertEquals(LocalDateTime.of(2026, 11, 24, 10, 0), viewModel.now.value)
    }

    private companion object {
        val MensaLoadStateLoaded = MensaLoadState.Loaded
    }
}
