package de.kaigothe.campusilmenau.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.data.repository.EventsRepository
import de.kaigothe.campusilmenau.data.repository.EventsRequest
import de.kaigothe.campusilmenau.data.repository.EventsRequests
import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventFilter
import de.kaigothe.campusilmenau.domain.model.events.EventGrouping
import de.kaigothe.campusilmenau.domain.model.events.EventOrganizer
import de.kaigothe.campusilmenau.domain.model.events.EventSection
import de.kaigothe.campusilmenau.domain.model.events.EventsSelection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** „Kommende“ (Liste ab heute oder gewählter Tag) oder „Vergangene“ (der letzte Monat). */
enum class EventsMode { Upcoming, Past }

sealed interface EventsLoadState {
    data object Loading : EventsLoadState
    data object Loaded : EventsLoadState
    data object Failed : EventsLoadState
}

data class EventsUiState(
    val mode: EventsMode,
    val selection: EventsSelection = EventsSelection(),
    val loadState: EventsLoadState = EventsLoadState.Loading,
    val events: List<CampusEvent> = emptyList(),
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val organizers: List<EventOrganizer> = emptyList(),
    /** Tage der Tagesleiste, an denen etwas stattfindet. */
    val daysWithEvents: Set<LocalDate> = emptySet(),
    val bookmarks: Set<String> = emptySet(),
    val now: Instant,
) {
    val today: LocalDate get() = BerlinTime.date(now)

    /** Die 14 Tage der Tagesleiste, beginnend mit heute. */
    val days: List<LocalDate> get() = (0 until STRIP_DAYS).map { today.plusDays(it.toLong()) }

    val visibleEvents: List<CampusEvent>
        get() = when (mode) {
            EventsMode.Upcoming -> if (selection.onlyBookmarked) events.filter { it.id in bookmarks } else events
            // Eine mehrtägige Veranstaltung, die heute noch läuft, gehört zu den kommenden.
            EventsMode.Past -> events.filter { it.lastDay < today }
        }

    val sections: List<EventSection>
        get() = when (mode) {
            EventsMode.Upcoming -> EventGrouping.upcoming(visibleEvents, selection, now)
            EventsMode.Past -> EventGrouping.past(visibleEvents)
        }

    companion object {
        const val STRIP_DAYS = 14
    }
}

@HiltViewModel(assistedFactory = EventsViewModel.Factory::class)
class EventsViewModel @AssistedInject constructor(
    @Assisted private val mode: EventsMode,
    @Assisted initial: EventsSelection,
    private val repository: EventsRepository,
    private val settings: AppSettings,
    private val requests: EventsRequests,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(mode: EventsMode, initial: EventsSelection): EventsViewModel
    }

    companion object {
        /** So viele Tage zurück zeigt „Vergangene“. */
        const val PAST_DAYS = 30L

        /** Obergrenze für das Nachladen am Stück (Seiten à 100), damit nichts endlos läuft. */
        const val MAX_PAGES = 10
    }

    private val _state = MutableStateFlow(EventsUiState(mode = mode, selection = initial, now = clock.instant()))
    val state: StateFlow<EventsUiState> = _state.asStateFlow()

    private var page = 0
    private var loadJob: Job? = null
    private var restJob: Job? = null

    init {
        settings.eventBookmarks.onEach { value -> _state.update { it.copy(bookmarks = value) } }.launchIn(viewModelScope)
        if (mode == EventsMode.Upcoming) {
            requests.pending.onEach { request ->
                if (request != null) {
                    apply(request)
                    requests.consume(request)
                }
            }.launchIn(viewModelScope)
            loadExtras()
        }
        load()
    }

    // MARK: Aktionen

    /** Ändert die Auswahl. Eine neue Abfrage startet nur, wenn sich etwas ändert, das die API betrifft. */
    fun select(change: (EventsSelection) -> EventsSelection) {
        val before = _state.value.selection
        val after = change(before)
        if (after == before) return
        _state.update { it.copy(selection = after) }
        if (after.loadKey != before.loadKey) {
            load()
        } else if (after.onlyBookmarked && !before.onlyBookmarked) {
            // „Gemerkt“ filtert in der App und muss deshalb alles kennen.
            loadRest()
        }
    }

    fun toggleEvening() = select { it.toggleEvening(clock.instant()) }

    fun reset() = select { EventsSelection() }

    fun retry() = load()

    fun toggleBookmark(id: String) {
        viewModelScope.launch { settings.toggleEventBookmark(id) }
    }

    private fun apply(request: EventsRequest) {
        when (request) {
            EventsRequest.ShowAll -> select { EventsSelection() }
            EventsRequest.ShowEvening -> select { EventsSelection.evening(clock.instant()) }
            is EventsRequest.ShowOrganizer -> select { EventsSelection().withOrganizer(request.name, request.id) }
        }
    }

    // MARK: Laden

    /** Ziehen zum Aktualisieren: Liste, Veranstalter und Punkte der Tagesleiste neu. Die alte Liste bleibt stehen. */
    suspend fun refresh() = coroutineScope {
        launch { load(quiet = true).join() }
        launch { loadExtrasNow() }
    }

    /** Lädt die erste Seite neu. Mit [quiet] bleibt die alte Liste stehen, und ein Fehler ersetzt sie nicht. */
    fun load(quiet: Boolean = false): Job {
        loadJob?.cancel()
        restJob?.cancel()
        page = 0
        val job = viewModelScope.launch {
            _state.update {
                it.copy(
                    now = clock.instant(),
                    loadingMore = false,
                    loadState = if (!quiet || it.events.isEmpty()) EventsLoadState.Loading else it.loadState,
                )
            }
            try {
                val result = repository.list(filterFor(_state.value), 1)
                page = 1
                _state.update {
                    it.copy(events = result.events, hasMore = result.hasMore, loadState = EventsLoadState.Loaded)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: ApiError) {
                if (!quiet || _state.value.events.isEmpty()) {
                    _state.update { it.copy(events = emptyList(), hasMore = false, loadState = EventsLoadState.Failed) }
                }
                return@launch
            }
            // „Vergangene“ braucht alles, um umzudrehen, „Gemerkt“ filtert in der App.
            if (mode == EventsMode.Past || _state.value.selection.onlyBookmarked) loadRestNow()
        }
        loadJob = job
        return job
    }

    /** Nächste Seite, wenn das letzte Element erscheint. */
    fun loadMore() {
        val current = _state.value
        if (current.loadState != EventsLoadState.Loaded || !current.hasMore || current.loadingMore) return
        if (page >= MAX_PAGES && mode == EventsMode.Past) return
        viewModelScope.launch { loadNextPage() }
    }

    /** Lädt alle weiteren Seiten, auch wenn „Gemerkt“ erst nach dem Laden eingeschaltet wurde. */
    private fun loadRest() {
        restJob?.cancel()
        restJob = viewModelScope.launch { loadRestNow() }
    }

    private suspend fun loadRestNow() {
        while (_state.value.hasMore && _state.value.loadState == EventsLoadState.Loaded && page < MAX_PAGES) {
            val before = page
            loadNextPage()
            if (page == before) break
        }
    }

    private suspend fun loadNextPage() {
        if (_state.value.loadingMore) return
        _state.update { it.copy(loadingMore = true) }
        try {
            val result = repository.list(filterFor(_state.value), page + 1)
            // Seiten können sich am Rand überschneiden, wenn sich der Bestand zwischendurch ändert.
            _state.update { state ->
                val known = state.events.map { it.id }.toSet()
                state.copy(events = state.events + result.events.filter { it.id !in known }, hasMore = result.hasMore)
            }
            page = result.page
        } catch (e: CancellationException) {
            throw e
        } catch (_: ApiError) {
            // Die geladene Liste bleibt, beim nächsten Erscheinen des letzten Elements wird neu versucht.
        } finally {
            _state.update { it.copy(loadingMore = false) }
        }
    }

    private fun filterFor(state: EventsUiState): EventFilter = when (mode) {
        EventsMode.Upcoming -> state.selection.toFilter()
        EventsMode.Past -> {
            val today = BerlinTime.date(clock.instant())
            EventFilter(
                period = EventFilter.Period(today.minusDays(PAST_DAYS), today),
                hideCancelled = state.selection.hideCancelled,
                category = state.selection.kind.category,
            )
        }
    }

    // MARK: Beiwerk

    private fun loadExtras() {
        viewModelScope.launch { loadExtrasNow() }
    }

    /** Veranstalter-Chips und Punkte der Tagesleiste. Fehler bleiben still, die alten Werte bleiben stehen. */
    private suspend fun loadExtrasNow() = coroutineScope {
        if (mode != EventsMode.Upcoming) return@coroutineScope
        launch {
            try {
                val list = repository.organizers()
                _state.update { it.copy(organizers = list) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: ApiError) {
                // still
            }
        }
        launch { loadDaysWithEvents() }
    }

    /** Einmal die 14 Tage laden (höchstens 3 Seiten) und daraus die Tage mit Einträgen ableiten. */
    private suspend fun loadDaysWithEvents() {
        val today = BerlinTime.date(clock.instant())
        val filter = EventFilter(period = EventFilter.Period(today, today.plusDays(EventsUiState.STRIP_DAYS.toLong())))
        val all = mutableListOf<CampusEvent>()
        try {
            var number = 1
            while (number <= 3) {
                val result = repository.list(filter, number)
                all += result.events
                if (!result.hasMore) break
                number++
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: ApiError) {
            return
        }
        val days = (0 until EventsUiState.STRIP_DAYS).map { today.plusDays(it.toLong()) }.filter { day -> all.any { it.runsOn(day) } }
        _state.update { it.copy(daysWithEvents = days.toSet()) }
    }
}
