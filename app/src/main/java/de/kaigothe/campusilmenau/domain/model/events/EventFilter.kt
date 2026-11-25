package de.kaigothe.campusilmenau.domain.model.events

import de.kaigothe.campusilmenau.core.time.BerlinTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Alles, was die App für eine Abfrage der Event-Liste braucht (Ergebnis von [EventFilter.query]). */
data class EventQuery(
    /** Parameter des Zeitfilters. */
    val time: List<Pair<String, String>>,
    /** Parameter ohne Zeitfilter (Veranstalter, Kategorie, abgesagte). */
    val common: List<Pair<String, String>>,
    /** Berliner Beginn des Zeitraums: Treffer davor schneidet die App ab. */
    val fromBoundary: Instant?,
    /** Berliner Ende des Zeitraums (exklusiv): Treffer ab hier schneidet die App ab. */
    val toBoundary: Instant?,
    /** Bei „ab Tag X“ auch laufende mehrtägige Veranstaltungen dieses Tages holen. */
    val runningOn: LocalDate?,
) {
    val params: List<Pair<String, String>> get() = time + common
}

/** Filter der Event-Liste. Die Regeln für Zeit und „Heute Abend“ stehen in [query]. */
data class EventFilter(
    val period: Period? = null,
    /** Ein einzelner Tag. Enthält auch mehrtägige Veranstaltungen, die vorher begonnen haben. */
    val day: LocalDate? = null,
    /** Ab 17 Uhr am Tag; ganztägige fallen weg. */
    val eveningOnly: Boolean = false,
    /**
     * ID des Veranstalters (zum Beispiel `cafe-bohne`). Die API akzeptiert für `organizer` nur die ID, keinen Namen
     * (sonst Fehler 400 `invalid_parameter_format`).
     */
    val organizerId: String? = null,
    val hideCancelled: Boolean = false,
    /** „Veranstaltung“ oder „Frist“ (Groß- und Kleinschreibung egal). */
    val category: String? = null,
) {
    /** Zeitraum nach Berliner Tagen: ab [from], bis [to] (exklusiv). */
    data class Period(val from: LocalDate, val to: LocalDate? = null)

    /**
     * Der Zeitfilter, der tatsächlich gilt. „Heute Abend“ lässt sich nicht mit einem Zeitraum kombinieren, dann gilt der
     * Tag (Standard: heute). Ein Tag geht jedem Zeitraum vor. Ohne beides gilt ab heute 00:00, sonst kämen vergangene
     * Veranstaltungen.
     */
    sealed interface TimeFilter {
        data class Evening(val day: LocalDate) : TimeFilter
        data class Day(val day: LocalDate) : TimeFilter
        data class Range(val from: LocalDate, val to: LocalDate?) : TimeFilter
    }

    fun timeFilter(now: Instant): TimeFilter {
        val today = BerlinTime.date(now)
        return when {
            eveningOnly -> TimeFilter.Evening(day ?: today)
            day != null -> TimeFilter.Day(day)
            else -> TimeFilter.Range(period?.from ?: today, period?.to)
        }
    }

    fun query(now: Instant): EventQuery {
        val time = mutableListOf<Pair<String, String>>()
        var fromBoundary: Instant? = null
        var toBoundary: Instant? = null
        var runningOn: LocalDate? = null

        when (val filter = timeFilter(now)) {
            is TimeFilter.Evening -> {
                time += "date" to filter.day.toString()
                time += "evening" to "true"
            }

            is TimeFilter.Day -> time += "date" to filter.day.toString()
            is TimeFilter.Range -> {
                // `from` und `to` zählen in UTC-Tagen, die Berliner Mitternacht liegt aber am Vortag um 22/23 Uhr UTC.
                // Damit zum Beispiel ein ganztägiges Event, das heute um 00:00 beginnt, nicht fehlt, wird das Fenster
                // verbreitert. Das Zuschneiden auf Berliner Tage übernimmt das Repository.
                time += "from" to utcDate(filter.from)
                fromBoundary = BerlinTime.startOfDay(filter.from)
                runningOn = filter.from
                if (filter.to != null) {
                    time += "to" to utcDate(filter.to.plusDays(1))
                    toBoundary = BerlinTime.startOfDay(filter.to)
                }
            }
        }

        val common = mutableListOf<Pair<String, String>>()
        organizerId?.trim()?.takeIf { it.isNotEmpty() }?.let { common += "organizer" to it }
        category?.trim()?.takeIf { it.isNotEmpty() }?.let { common += "category" to it }
        // Immer ausdrücklich senden, damit nicht die Voreinstellung des Servers entscheidet.
        common += "include_cancelled" to if (hideCancelled) "false" else "true"
        return EventQuery(time, common, fromBoundary, toBoundary, runningOn)
    }

    companion object {
        /** Der UTC-Tag des Berliner Tagesbeginns (so zählen `from` und `to`). */
        fun utcDate(day: LocalDate): String = BerlinTime.startOfDay(day).atOffset(ZoneOffset.UTC).toLocalDate().toString()
    }
}
