package de.kaigothe.campusilmenau.ui.mensa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.core.common.WriteThroughState
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.data.repository.MensaRepository
import de.kaigothe.campusilmenau.domain.model.mensa.Canteen
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFavorites
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFormatting
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealtime
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaStatus
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.ui.components.LoadingGate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/** Ladezustand des Speiseplans für den gewählten Tag. */
sealed interface MensaLoadState {
    data object Loading : MensaLoadState
    data object Loaded : MensaLoadState

    /** [error] bestimmt den Text („Keine Verbindung“ gegen „Dienst gerade nicht erreichbar“). */
    data class Failed(val error: ApiError) : MensaLoadState
}

/** Alles, was die Mensa-Ansicht zeigt. Reine Daten, die Ableitungen sind Funktionen ohne Seiteneffekte. */
data class MensaUiState(
    /** Einstellungen (gewählte Mensa, Filter) sind gelesen. Vorher zeigt die Ansicht nichts, damit nichts springt. */
    val ready: Boolean = false,
    val days: List<LocalDate>,
    val selectedDayIndex: Int = 0,
    val venue: MensaVenue = MensaVenue.MensaEhrenberg,
    val mealtime: MensaMealtime = MensaMealtime.Lunch,
    val loadState: MensaLoadState = MensaLoadState.Loading,
    /** Alle Gerichte aller Mensen für [selectedDay], ungefiltert. */
    val meals: List<MensaMeal> = emptyList(),
    val canteens: List<Canteen> = emptyList(),
    val filter: MensaFilter = MensaFilter(),
    val priceGroup: MensaPriceGroup = MensaPriceGroup.Studierende,
    val favorites: Set<String> = emptySet(),
) {
    val selectedDay: LocalDate get() = days[selectedDayIndex]

    /**
     * Gerichte einer Mensa. Die Cafeteria Ehrenberg zeigt je nach [mealtime] Mittag oder Abendessen.
     * [filtered] wendet den Filter an.
     */
    fun mealsFor(venue: MensaVenue, filtered: Boolean = true): List<MensaMeal> {
        val ofVenue = meals.filter { venue.matches(it.canteenName) }
        val ofMealtime =
            if (venue.hasMealtimes) ofVenue.filter { MensaMealtime.isDinner(it) == (mealtime == MensaMealtime.Dinner) } else ofVenue
        return if (filtered) filter.apply(ofMealtime) else ofMealtime
    }

    /** Status-Chip der Mensa, `null`, solange die Öffnungszeiten fehlen. */
    fun status(venue: MensaVenue, now: LocalDateTime): MensaStatus? =
        MensaStatus.chip(venue, canteens, selectedDay, mealtime, now)

    /** Allergene für das Filter-Sheet (immer alle bekannten, dazu unbekannte aus dem Speiseplan). */
    val allergenOptions: List<String>
        get() = MensaFilter.availableAllergens(meals, filter.excludedAllergens)

    fun isFavorite(meal: MensaMeal): Boolean = MensaFavorites.key(meal) in favorites
}

@HiltViewModel
class MensaViewModel @Inject constructor(
    private val repository: MensaRepository,
    private val settings: AppSettings,
    clock: Clock,
) : ViewModel() {

    private val days: List<LocalDate> = MensaFormatting.workdays(BerlinTime.date(clock.instant()))

    private val venue = WriteThroughState(viewModelScope, settings.mensaVenue, settings::setMensaVenue)
    private val filter = WriteThroughState(viewModelScope, settings.mensaFilter, settings::setMensaFilter)

    private val _state = MutableStateFlow(MensaUiState(days = days))
    val state: StateFlow<MensaUiState> = _state.asStateFlow()

    /** Zeigt die Ziege beim Laden (nach 0,4 s, mindestens 0,6 s, Hinweis ab 8 s). */
    val loadingGate = LoadingGate(viewModelScope, initiallyLoading = true)

    /** Die aktuelle Berliner Zeit, alle 30 Sekunden erneuert, solange die Ansicht sichtbar ist (Status-Chip). */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.ofInstant(clock.instant(), BerlinTime.zone))
            delay(30.seconds)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LocalDateTime.ofInstant(clock.instant(), BerlinTime.zone),
    )

    private var mealsJob: Job? = null
    private var canteensJob: Job? = null

    init {
        venue.state.filterNotNull().onEach { value ->
            _state.update { it.copy(venue = value, ready = it.ready || filter.value != null) }
        }.launchIn(viewModelScope)
        filter.state.filterNotNull().onEach { value ->
            _state.update { it.copy(filter = value, ready = it.ready || venue.value != null) }
        }.launchIn(viewModelScope)
        settings.priceGroup.onEach { value -> _state.update { it.copy(priceGroup = value) } }.launchIn(viewModelScope)
        settings.mensaFavorites.onEach { value -> _state.update { it.copy(favorites = value) } }.launchIn(viewModelScope)

        loadMeals(forceRefresh = false)
        loadCanteens(forceRefresh = false)
    }

    fun selectDay(index: Int) {
        if (index !in days.indices || index == _state.value.selectedDayIndex) return
        _state.update { it.copy(selectedDayIndex = index) }
        loadMeals(forceRefresh = false)
    }

    fun selectVenue(value: MensaVenue) {
        if (value != _state.value.venue) venue.set(value)
    }

    fun selectMealtime(value: MensaMealtime) {
        _state.update { it.copy(mealtime = value) }
    }

    fun setFilter(value: MensaFilter) = filter.set(value)

    fun resetFilter() = filter.set(MensaFilter())

    /** „Erneut versuchen“: umgeht den Zwischenspeicher. */
    fun retry() {
        loadMeals(forceRefresh = true)
        loadCanteens(forceRefresh = true)
    }

    fun toggleFavorite(meal: MensaMeal) {
        viewModelScope.launch { settings.setMensaFavorite(meal.name, !_state.value.isFavorite(meal)) }
    }

    /** Lädt den gewählten Tag. Ein laufender Ladevorgang (früherer Tag) wird abgebrochen und kann nichts mehr überschreiben. */
    private fun loadMeals(forceRefresh: Boolean) {
        mealsJob?.cancel()
        val day = _state.value.selectedDay
        _state.update { it.copy(loadState = MensaLoadState.Loading, meals = emptyList()) }
        loadingGate.setLoading(true)
        mealsJob = viewModelScope.launch {
            try {
                val meals = repository.meals(day, forceRefresh)
                _state.update { it.copy(loadState = MensaLoadState.Loaded, meals = meals) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                _state.update { it.copy(loadState = MensaLoadState.Failed(e)) }
            }
            loadingGate.setLoading(false)
        }
    }

    /** Öffnungszeiten sind Beiwerk: Ein Fehler lässt den Status-Chip weg, stört den Speiseplan aber nicht. */
    private fun loadCanteens(forceRefresh: Boolean) {
        canteensJob?.cancel()
        canteensJob = viewModelScope.launch {
            try {
                val canteens = repository.canteens(forceRefresh)
                _state.update { it.copy(canteens = canteens) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: ApiError) {
                // Ohne Öffnungszeiten gibt es keinen Chip.
            }
        }
    }
}
