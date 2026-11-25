package de.kaigothe.campusilmenau.data.remote.events

import kotlinx.serialization.Serializable

/**
 * Antwortmodelle der Events-API. Bewusst tolerant: Bis auf `id`, `title` und `startAt` hat jedes Feld einen
 * Standardwert, und Datums- und Link-Felder bleiben Text, damit ein kaputter Wert nur sein Feld verliert
 * (der Mapper räumt auf). Die Schlüssel stehen als camelCase, die Antworten werden vorher umgewandelt (`CamelCaseKeys`).
 */
@Serializable
data class EventListDto(
    val data: List<EventDto> = emptyList(),
    val hasMore: Boolean = false,
    val totalCount: Int? = null,
    val page: Int = 1,
)

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val category: String? = null,
    val startAt: String,
    val endAt: String? = null,
    val allDay: Boolean = false,
    val status: String? = null,
    val location: String? = null,
    val locationAddress: String? = null,
    val organizer: OrganizerRefDto? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val ticketUrl: String? = null,
)

@Serializable
data class OrganizerRefDto(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
data class OrganizerListDto(val data: List<OrganizerDto> = emptyList())

@Serializable
data class OrganizerDto(
    val id: String? = null,
    val name: String,
    val upcomingEventCount: Int = 0,
)
