package de.kaigothe.campusilmenau.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.data.repository.EventsRepository
import de.kaigothe.campusilmenau.data.repository.EventsRequest
import de.kaigothe.campusilmenau.data.repository.EventsRequests
import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventOrganizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EventDetailState {
    data object Loading : EventDetailState

    /** Die Veranstaltung gibt es nicht (mehr), 404. */
    data object NotFound : EventDetailState
    data object Failed : EventDetailState

    /** [upcomingCount]: kommende Veranstaltungen dieses Veranstalters laut Veranstalter-Liste, falls bekannt. */
    data class Loaded(
        val event: CampusEvent,
        val isBookmarked: Boolean,
        val upcomingCount: Int?,
    ) : EventDetailState
}

/** Zustand der Detailansicht einer Veranstaltung: lädt `detail(id)` und merkt Veranstaltungen. */
@HiltViewModel(assistedFactory = EventDetailViewModel.Factory::class)
class EventDetailViewModel @AssistedInject constructor(
    @Assisted private val id: String,
    private val repository: EventsRepository,
    private val settings: AppSettings,
    private val requests: EventsRequests,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(id: String): EventDetailViewModel
    }

    private data class Fetched(val event: CampusEvent?, val kind: EventDetailState?, val organizers: List<EventOrganizer>)

    private val fetched = MutableStateFlow(Fetched(null, EventDetailState.Loading, emptyList()))

    val state: StateFlow<EventDetailState> = combine(fetched, settings.eventBookmarks) { fetched, bookmarks ->
        val event = fetched.event
        if (event == null) {
            fetched.kind ?: EventDetailState.Loading
        } else {
            val count = event.organizerId?.let { organizer -> fetched.organizers.firstOrNull { it.id == organizer }?.upcomingCount }
            EventDetailState.Loaded(event, isBookmarked = event.id in bookmarks, upcomingCount = count)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EventDetailState.Loading)

    init {
        load()
    }

    fun retry() = load()

    fun toggleBookmark() {
        viewModelScope.launch { settings.toggleEventBookmark(id) }
    }

    /** „Alle kommenden von …“: die Liste öffnet sich mit diesem Veranstalter. */
    fun showAllFromOrganizer(event: CampusEvent) {
        val organizerId = event.organizerId ?: return
        requests.post(EventsRequest.ShowOrganizer(event.organizer ?: organizerId, organizerId))
    }

    private fun load() {
        viewModelScope.launch {
            if (fetched.value.event == null) fetched.value = Fetched(null, EventDetailState.Loading, fetched.value.organizers)
            try {
                val event = repository.detail(id)
                fetched.value = fetched.value.copy(event = event, kind = null)
                loadOrganizerCount()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                // Mit geladenen Daten bleibt die Ansicht bei einem Fehler stehen, nur ein 404 ersetzt sie.
                val notFound = e is ApiError.Http && e.isNotFound
                if (notFound) {
                    fetched.value = fetched.value.copy(event = null, kind = EventDetailState.NotFound)
                } else if (fetched.value.event == null) {
                    fetched.value = fetched.value.copy(kind = EventDetailState.Failed)
                }
            }
        }
    }

    /** Die Zahl für „Alle n kommenden von …“. Fehler bleiben still, der Link heißt dann ohne Zahl. */
    private suspend fun loadOrganizerCount() {
        try {
            fetched.value = fetched.value.copy(organizers = repository.organizers())
        } catch (e: CancellationException) {
            throw e
        } catch (_: ApiError) {
            // still
        }
    }
}
