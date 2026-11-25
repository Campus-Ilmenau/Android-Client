package de.kaigothe.campusilmenau.domain.model.events

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Was die Detailansicht aus einer Veranstaltung ableitet: welche Teile und Tasten es gibt. Fehlendes entfällt ohne
 * Lücke. „Knapp“ (nur Titel, Beginn, Status) ist kein eigener Fall, sondern ergibt sich daraus.
 */
class EventDetailDisplay(val event: CampusEvent) {
    /** Ab etwa 40 Zeichen wird der Titel kleiner. */
    val titleSize: Int get() = if (event.title.length > 40) 26 else 32

    val cancelled: Boolean get() = event.status == EventStatus.Cancelled
    val tentative: Boolean get() = event.status == EventStatus.Tentative

    val organizer: String? get() = event.organizer.cleaned()
    val location: String? get() = event.location.cleaned()
    val address: String? get() = event.address.cleaned()
    val description: String? get() = event.description.cleaned()

    /** Link „In Karten öffnen“: nur mit Adresse. Eine `geo:`-Suche mit der Adresse, keine Koordinaten. */
    val mapQuery: String? get() = address

    /** Tickets gibt es nur mit Link und solange die Veranstaltung nicht abgesagt ist. */
    val showTickets: Boolean get() = event.ticketUrl != null && !cancelled
    val showCalendar: Boolean get() = !cancelled

    /** Der Kalender-Button ist der Hauptbutton, wenn es keine Tickets gibt. */
    val calendarIsPrimary: Boolean get() = !showTickets
    val showInfoPage: Boolean get() = event.infoUrl != null

    /** Der Hinweis „öffnet eine Seite außerhalb der App“ steht nur, wenn es einen solchen Button gibt. */
    val showExternalHint: Boolean get() = showTickets || showInfoPage

    /** Was das Teilen-Blatt anbietet: der Info- oder Ticket-Link, sonst Titel und Datum als Text. */
    val shareUrl: String? get() = event.infoUrl ?: event.ticketUrl

    private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}

/** Alles, was in den Kalender geschrieben wird (Insert-Intent des Kalenders). Zeiten in Millisekunden seit 1970. */
data class CalendarEntry(
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    /** „Ort, Adresse“. */
    val location: String?,
    /** Der Link der Veranstaltung, steht in der Beschreibung. */
    val url: String?,
) {
    companion object {
        /** So lange dauert eine Veranstaltung ohne Ende. */
        val DefaultDuration: Duration = Duration.ofHours(2)

        fun of(event: CampusEvent): CalendarEntry {
            val begin: Long
            val end: Long
            if (event.allDay) {
                // Ganztägige Einträge rechnet der Kalender in UTC: von Mitternacht des ersten bis Mitternacht nach dem
                // letzten Tag.
                begin = utcMidnight(event.firstDay)
                end = utcMidnight(event.lastDay.plusDays(1))
            } else {
                begin = event.start.toEpochMilli()
                val realEnd = event.end
                end = if (realEnd != null && realEnd > event.start) realEnd.toEpochMilli() else event.start.plus(DefaultDuration).toEpochMilli()
            }
            val place = listOfNotNull(event.location, event.address).map { it.trim() }.filter { it.isNotEmpty() }
            return CalendarEntry(
                title = event.title,
                beginMillis = begin,
                endMillis = end,
                allDay = event.allDay,
                location = place.takeIf { it.isNotEmpty() }?.joinToString(", "),
                url = event.infoUrl,
            )
        }

        private fun utcMidnight(day: LocalDate): Long = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}
