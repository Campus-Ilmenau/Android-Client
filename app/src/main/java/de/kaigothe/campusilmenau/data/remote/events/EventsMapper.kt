package de.kaigothe.campusilmenau.data.remote.events

import de.kaigothe.campusilmenau.core.time.ApiDates
import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventOrganizer
import de.kaigothe.campusilmenau.domain.model.events.EventStatus
import kotlinx.serialization.SerializationException
import java.net.URI

/**
 * Der Beginn muss lesbar sein (sonst `SerializationException`, die Antwort ist unbrauchbar). Alles andere ist
 * Beiwerk: Texte werden getrimmt (leer → `null`), Links gelten nur mit `http`/`https` und Host, die Kategorie steht
 * mit großem Anfangsbuchstaben („FRIST“ → „Frist“).
 */
fun EventDto.toDomain(): CampusEvent {
    val start = ApiDates.parse(startAt) ?: throw SerializationException("Ungültiges Datum: $startAt")
    return CampusEvent(
        id = id,
        title = title,
        start = start,
        end = endAt?.let(ApiDates::parse),
        allDay = allDay,
        status = EventStatus.fromApi(status),
        location = location.clean(),
        address = locationAddress.clean(),
        organizer = organizer?.name.clean(),
        organizerId = organizer?.id.clean(),
        description = description.clean(),
        imageUrl = imageUrl.webUrl(),
        infoUrl = sourceUrl.webUrl(),
        ticketUrl = ticketUrl.webUrl(),
        category = category.clean()?.let { it.take(1).uppercase() + it.drop(1).lowercase() },
    )
}

fun OrganizerDto.toDomain(): EventOrganizer {
    val clean = name.trim()
    return EventOrganizer(id = id.clean() ?: clean, name = clean, upcomingCount = upcomingEventCount)
}

private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** Nur Webadressen sind brauchbare Links; alles andere entfällt. */
private fun String?.webUrl(): String? {
    val text = clean() ?: return null
    val uri = runCatching { URI(text) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    return if ((scheme == "http" || scheme == "https") && !uri.host.isNullOrEmpty()) text else null
}
