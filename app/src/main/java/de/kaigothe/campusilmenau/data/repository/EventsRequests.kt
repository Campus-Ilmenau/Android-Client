package de.kaigothe.campusilmenau.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Ein Wunsch an die Ansicht „Veranstaltungen“, der von außen kommt (Link, „Alle“ auf Heute, „Alle kommenden von …“). */
sealed interface EventsRequest {
    /** `campusilmenau://events`: die Liste ab heute, ohne die Filter vom letzten Besuch. */
    data object ShowAll : EventsRequest

    /** „Heute Abend“ für heute. */
    data object ShowEvening : EventsRequest

    /** Die Liste ab heute mit diesem Veranstalter. */
    data class ShowOrganizer(val name: String, val id: String) : EventsRequest
}

/**
 * Wünsche an die Events-Liste. Die Liste kann beim Eintreffen noch nicht existieren (der Tab wurde nie geöffnet), darum
 * bleibt der Wunsch stehen, bis sie ihn übernimmt.
 */
@Singleton
class EventsRequests @Inject constructor() {
    private val _pending = MutableStateFlow<EventsRequest?>(null)
    val pending: StateFlow<EventsRequest?> = _pending.asStateFlow()

    fun post(request: EventsRequest) {
        _pending.value = request
    }

    /** Die Liste hat den Wunsch übernommen. */
    fun consume(request: EventsRequest) {
        _pending.compareAndSet(request, null)
    }
}
