package de.kaigothe.campusilmenau.ui.mensa

import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.testing.FakeMensaRepository
import de.kaigothe.campusilmenau.testing.InMemoryPreferences
import de.kaigothe.campusilmenau.testing.MainDispatcherRule
import de.kaigothe.campusilmenau.testing.testMeal
import de.kaigothe.campusilmenau.ui.navigation.MensaGerichtRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MensaMealDetailViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-24T09:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 11, 24)
    private val pasta = testMeal("pasta", name = "Pasta Pomodoro")

    private fun settings() = AppSettings(InMemoryPreferences())

    private fun TestScope.viewModel(
        route: MensaGerichtRoute,
        repository: FakeMensaRepository = FakeMensaRepository(mapOf(today to listOf(pasta))),
        settings: AppSettings = settings(),
    ) = MensaMealDetailViewModel(route, repository, settings, clock).also { advanceUntilIdle() }

    @Test
    fun `findet das Gericht des Tages`() = runTest {
        val state = viewModel(MensaGerichtRoute("pasta", "2026-11-24")).state.value

        assertTrue(state is MensaMealDetailState.Loaded)
        state as MensaMealDetailState.Loaded
        assertEquals(pasta, state.meal)
        assertEquals(MensaPriceGroup.Studierende, state.priceGroup)
        assertEquals(false, state.isFavorite)
    }

    @Test
    fun `ohne Tag gilt heute`() = runTest {
        val state = viewModel(MensaGerichtRoute("pasta", null)).state.value

        assertTrue(state is MensaMealDetailState.Loaded)
    }

    @Test
    fun `unbekannte ID ist nicht gefunden`() = runTest {
        assertEquals(
            MensaMealDetailState.NotFound,
            viewModel(MensaGerichtRoute("gibt-es-nicht", "2026-11-24")).state.value,
        )
    }

    @Test
    fun `ungueltiger Tag ist nicht gefunden und fragt das Netz nicht`() = runTest {
        val repository = FakeMensaRepository(mapOf(today to listOf(pasta)))

        val state = viewModel(MensaGerichtRoute("pasta", "morgen"), repository).state.value

        assertEquals(MensaMealDetailState.NotFound, state)
        assertEquals(emptyList<LocalDate>(), repository.requestedDays)
    }

    @Test
    fun `Netzfehler ist ein Fehlerzustand und Wiederholen laedt neu`() = runTest {
        val repository = FakeMensaRepository(emptyMap())
        val viewModel = viewModel(MensaGerichtRoute("pasta", "2026-11-24"), repository)
        assertTrue(viewModel.state.value is MensaMealDetailState.Failed)

        repository.mealsByDay = mapOf(today to listOf(pasta))
        viewModel.retry()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is MensaMealDetailState.Loaded)
    }

    @Test
    fun `Favorit und Preisgruppe kommen aus den Einstellungen`() = runTest {
        val settings = settings()
        val viewModel = viewModel(MensaGerichtRoute("pasta", "2026-11-24"), settings = settings)

        viewModel.toggleFavorite()
        settings.setPriceGroup(MensaPriceGroup.Gaeste)
        advanceUntilIdle()

        val loaded = viewModel.state.value as MensaMealDetailState.Loaded
        assertEquals(true, loaded.isFavorite)
        assertEquals(MensaPriceGroup.Gaeste, loaded.priceGroup)

        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertEquals(false, (viewModel.state.value as MensaMealDetailState.Loaded).isFavorite)
    }
}
