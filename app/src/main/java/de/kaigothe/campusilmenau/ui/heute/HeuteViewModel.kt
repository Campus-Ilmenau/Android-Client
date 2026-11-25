package de.kaigothe.campusilmenau.ui.heute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.core.common.WriteThroughState
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.data.repository.HeuteEventsSource
import de.kaigothe.campusilmenau.data.repository.MensaRepository
import de.kaigothe.campusilmenau.data.repository.NoticesSource
import de.kaigothe.campusilmenau.domain.model.heute.HeuteContent
import de.kaigothe.campusilmenau.domain.model.heute.HeuteEvent
import de.kaigothe.campusilmenau.domain.model.heute.Hinweis
import de.kaigothe.campusilmenau.domain.model.heute.Tagesphase
import de.kaigothe.campusilmenau.domain.model.mensa.Canteen
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

/**
 * Was die Startseite geladen hat. Jeder Teil lädt für sich: Fehlt einer (Fehler, Zeitlimit), bleibt sein alter Stand
 * (am Anfang `null`) und nur sein Abschnitt fehlt. [loaded] wird `true`, sobald der erste Ladevorgang durch ist, auch
 * mit Fehlern. Davor zeigt die Seite Platzhalter.
 */
data class HeuteUiState(
    val now: Instant,
    val venue: MensaVenue = MensaVenue.MensaEhrenberg,
    val filter: MensaFilter = MensaFilter(),
    val priceGroup: MensaPriceGroup = MensaPriceGroup.Studierende,
    val canteens: List<Canteen>? = null,
    val mealsToday: List<MensaMeal>? = null,
    val mealsTomorrow: List<MensaMeal>? = null,
    val eventsEvening: List<HeuteEvent>? = null,
    val eventsTomorrow: List<HeuteEvent>? = null,
    val notices: List<Hinweis> = emptyList(),
    val dismissedNotices: Set<String> = emptySet(),
    val loaded: Boolean = false,
) {
    val content: HeuteContent = HeuteContent(
        now = now, venue = venue, filter = filter, priceGroup = priceGroup, canteens = canteens,
        mealsToday = mealsToday, mealsTomorrow = mealsTomorrow, eventsEvening = eventsEvening,
        eventsTomorrow = eventsTomorrow, notices = notices, dismissedNotices = dismissedNotices,
    )
}

@HiltViewModel
class HeuteViewModel @Inject constructor(
    private val mensa: MensaRepository,
    private val events: HeuteEventsSource,
    private val noticesSource: NoticesSource,
    private val settings: AppSettings,
    private val clock: Clock,
) : ViewModel() {

    companion object {
        /** Spätestens nach so langer Zeit lädt die Seite beim Aktivwerden neu. */
        val ReloadInterval: Duration = Duration.ofMinutes(10)

        /** So lange wartet jeder Teil, dann entfällt sein Abschnitt. */
        val TimeLimit = 8.seconds
    }

    private val venue = WriteThroughState(viewModelScope, settings.heuteMensa, settings::setHeuteMensa)

    private val _state = MutableStateFlow(HeuteUiState(now = clock.instant()))
    val state: StateFlow<HeuteUiState> = _state.asStateFlow()

    private var loadedAt: Instant? = null
    private var loadedPhase: Tagesphase? = null

    init {
        venue.state.filterNotNull().onEach { value -> _state.update { it.copy(venue = value) } }.launchIn(viewModelScope)
        settings.mensaFilter.onEach { value -> _state.update { it.copy(filter = value) } }.launchIn(viewModelScope)
        settings.priceGroup.onEach { value -> _state.update { it.copy(priceGroup = value) } }.launchIn(viewModelScope)
        settings.dismissedNotices.onEach { value -> _state.update { it.copy(dismissedNotices = value) } }.launchIn(viewModelScope)
    }

    /** Wählt die Mensa auf der Startseite und merkt sie sich. */
    fun selectVenue(value: MensaVenue) = venue.set(value)

    /** Blendet einen Hinweis dauerhaft aus. */
    fun dismissNotice(id: String) {
        viewModelScope.launch { settings.dismissNotice(id) }
    }

    /** Lädt neu, wenn der Tag oder die Tagesphase gewechselt hat oder die Daten älter als 10 Minuten sind. */
    suspend fun refresh() {
        val now = clock.instant()
        _state.update { it.copy(now = now) }
        val at = loadedAt
        if (at == null || !sameDay(at, now) || Tagesphase.of(now) != loadedPhase || Duration.between(at, now) > ReloadInterval) {
            load()
        }
    }

    /**
     * Hält die Seite aktuell: wacht zu jeder vollen Minute auf (die Uhrzeit im Kopf zeigt Minuten) und zu jedem früheren
     * Wechsel (Phase, Begrüßung, Öffnen/Schließen, Eventbeginn und -ende) und stellt dann `now` nach. Die Daten werden nur
     * bei Wechsel der Phase oder des Tages neu geladen. Läuft, bis die Aufgabe abgebrochen wird (die Ansicht ruft es
     * nur, solange sie sichtbar ist).
     */
    suspend fun watch() {
        refresh()
        while (true) {
            val current = _state.value
            val now = current.now
            val nextMinute = Instant.ofEpochSecond((now.epochSecond / 60) * 60 + 60)
            val target = minOf(current.content.nextChange, nextMinute)
            delay(maxOf(500L, Duration.between(now, target).toMillis()))
            val before = _state.value.now
            val after = clock.instant()
            _state.update { it.copy(now = after) }
            if (Tagesphase.of(before) != Tagesphase.of(after) || !sameDay(before, after)) refresh()
        }
    }

    /** Lädt alle Teile parallel und unabhängig. Jeder Teil erscheint, sobald er da ist. */
    suspend fun load() {
        val now = clock.instant()
        val today = BerlinTime.date(now)
        // Neuer Tag: Daten von gestern sind falsch, nicht nur alt.
        loadedAt?.let { if (!sameDay(it, now)) reset() }
        _state.update { it.copy(now = now) }

        coroutineScope {
            launch { fetch { mensa.canteens(forceRefresh = true) }?.let { value -> _state.update { it.copy(canteens = value) } } }
            launch { fetch { mensa.meals(today, forceRefresh = true) }?.let { value -> _state.update { it.copy(mealsToday = value) } } }
            launch {
                fetch { mensa.meals(today.plusDays(1), forceRefresh = true) }
                    ?.let { value -> _state.update { it.copy(mealsTomorrow = value) } }
            }
            launch { fetch { events.evening(now) }?.let { value -> _state.update { it.copy(eventsEvening = value) } } }
            launch { fetch { events.tomorrow(now) }?.let { value -> _state.update { it.copy(eventsTomorrow = value) } } }
            launch { fetch { noticesSource.notices(now) }?.let { value -> _state.update { it.copy(notices = value) } } }
        }
        // Erst jetzt gilt der Stand als geladen: Wird der Vorgang abgebrochen (die Ansicht war nicht sichtbar), versucht
        // es der nächste `refresh()` erneut.
        loadedAt = now
        loadedPhase = Tagesphase.of(now)
        _state.update { it.copy(now = clock.instant(), loaded = true) }
    }

    private fun reset() {
        _state.update {
            it.copy(canteens = null, mealsToday = null, mealsTomorrow = null, eventsEvening = null, eventsTomorrow = null)
        }
    }

    /** Ein Teil mit Zeitlimit. Fehler und Zeitablauf geben `null`, nur ein Abbruch der Seite wird weitergereicht. */
    private suspend fun <T> fetch(block: suspend () -> T): T? = try {
        withTimeoutOrNull(TimeLimit) { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: ApiError) {
        null
    }

    private fun sameDay(a: Instant, b: Instant) = BerlinTime.date(a) == BerlinTime.date(b)
}
