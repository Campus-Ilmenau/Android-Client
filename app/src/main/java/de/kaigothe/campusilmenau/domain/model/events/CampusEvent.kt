package de.kaigothe.campusilmenau.domain.model.events

import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.heute.HeuteEvent
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Zustand einer Veranstaltung. Unbekannte Werte der API gelten als „findet statt“. */
enum class EventStatus(val apiValue: String) {
    Scheduled("scheduled"),
    Tentative("tentative"),
    Cancelled("cancelled"),
    ;

    companion object {
        /** `scheduled`, `tentative`, `cancelled`; alles andere (und fehlend) ist „findet statt“. */
        fun fromApi(value: String?): EventStatus = when (value.orEmpty().trim().lowercase()) {
            "tentative", "unconfirmed" -> Tentative
            "cancelled", "canceled" -> Cancelled
            else -> Scheduled
        }
    }
}

/**
 * Eine Veranstaltung oder Frist. Alles außer Titel, Beginn und Status ist optional, fehlende Felder sind kein Fehler.
 * Alle Tage sind Berliner Tage (siehe [BerlinTime]).
 */
data class CampusEvent(
    val id: String,
    val title: String,
    val start: Instant,
    val end: Instant? = null,
    val allDay: Boolean = false,
    val status: EventStatus = EventStatus.Scheduled,
    val location: String? = null,
    val address: String? = null,
    val organizer: String? = null,
    /** ID des Veranstalters laut API (zum Filtern, ohne den Namen zu kennen). */
    val organizerId: String? = null,
    /** Reiner Text, mit Absätzen. */
    val description: String? = null,
    val imageUrl: String? = null,
    val infoUrl: String? = null,
    val ticketUrl: String? = null,
    /** „Veranstaltung“, „Frist“ … */
    val category: String? = null,
) {
    /** Erster Berliner Tag der Veranstaltung. */
    val firstDay: LocalDate get() = BerlinTime.date(start)

    /**
     * Letzter Berliner Tag. Das Ende ganztägiger Veranstaltungen ist exklusiv (Mitternacht), ein Ende genau um Mitternacht
     * gehört zum Vortag, und eine Nachtveranstaltung, die bis 06:00 am Folgetag geht, zählt noch zum Starttag.
     */
    val lastDay: LocalDate
        get() {
            val first = firstDay
            val end = end
            if (end == null || end <= start) return first
            val endDay = BerlinTime.date(end)
            val endDayStart = BerlinTime.startOfDay(endDay)
            if (end == endDayStart) {
                val before = BerlinTime.date(end.minusSeconds(1))
                return if (before > first) before else first
            }
            if (endDay == first) return first
            if (endDay == first.plusDays(1) && Duration.between(endDayStart, end) <= NightGrace) return first
            return endDay
        }

    val isMultiDay: Boolean get() = lastDay > firstDay

    /** Findet die Veranstaltung an diesem (Berliner) Tag statt? Mehrtägige zählen an jedem Tag dazwischen. */
    fun runsOn(day: LocalDate): Boolean = day >= firstDay && day <= lastDay

    val isDeadline: Boolean get() = category.equals("frist", ignoreCase = true)

    val isCancelled: Boolean get() = status == EventStatus.Cancelled

    /** Der Veranstalter für die Anzeige. `null`, wenn er fehlt oder gleich dem Ort ist. */
    val organizerForDisplay: String?
        get() {
            val name = organizer?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            if (location?.trim().equals(name, ignoreCase = true)) return null
            return name
        }

    /** Die Sicht der Startseite „Heute“ auf diese Veranstaltung. */
    fun toHeuteEvent() = HeuteEvent(
        id = id,
        title = title,
        start = start,
        end = end,
        allDay = allDay,
        cancelled = isCancelled,
        location = location,
        organizer = organizerForDisplay,
    )

    private companion object {
        val NightGrace: Duration = Duration.ofHours(6)
    }
}

/** Ein Veranstalter mit der Zahl seiner kommenden Veranstaltungen. */
data class EventOrganizer(val id: String, val name: String, val upcomingCount: Int)

/** Eine Seite der Liste (bis zu 100 Veranstaltungen). */
data class EventPage(
    val events: List<CampusEvent>,
    val page: Int,
    val hasMore: Boolean,
    /** Gesamtzahl für die gesetzten Filter laut API. */
    val total: Int,
)
