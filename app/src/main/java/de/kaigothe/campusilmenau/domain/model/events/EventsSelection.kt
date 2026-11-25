package de.kaigothe.campusilmenau.domain.model.events

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.time.BerlinTime
import java.time.Instant
import java.time.LocalDate

/** Die „Art“ im Filter-Sheet. Fest vorgegeben, weil die API keine Kategorien-Liste hat. */
enum class EventKind(val category: String?) {
    All(null),
    Event("Veranstaltung"),
    Deadline("Frist"),
}

/**
 * Alles, was man in der Ansicht „Veranstaltungen“ wählen kann: Tag, „Heute Abend“, „Gemerkt“, Veranstalter und die
 * beiden Einstellungen aus dem Filter-Sheet. Daraus wird der [EventFilter] für die API.
 */
data class EventsSelection(
    /** `null` heißt „Ab heute“. */
    val day: LocalDate? = null,
    val eveningOnly: Boolean = false,
    /** Gilt nur in der App, die API kennt kein Merken. */
    val onlyBookmarked: Boolean = false,
    /** Name des gewählten Veranstalters, nur für die Anzeige („Alles von Café Bohne“). */
    val organizerName: String? = null,
    /** ID des gewählten Veranstalters (`cafe-bohne`). Die API filtert nur nach der ID, nicht nach dem Namen. */
    val organizerId: String? = null,
    val kind: EventKind = EventKind.All,
    val hideCancelled: Boolean = false,
) {
    /** Tag wählen; „Ab heute“ (`null`) schaltet „Heute Abend“ aus, weil es sich nicht mit einem Zeitraum verträgt. */
    fun withDay(newDay: LocalDate?): EventsSelection =
        copy(day = newDay, eveningOnly = if (newDay == null) false else eveningOnly)

    /** „Heute Abend“ ein- oder ausschalten. Läuft gerade „Ab heute“, wird daraus „Heute“. */
    fun toggleEvening(now: Instant): EventsSelection {
        val on = !eveningOnly
        return copy(eveningOnly = on, day = if (on && day == null) BerlinTime.date(now) else day)
    }

    /** Immer nur ein Veranstalter; derselbe nochmal hebt die Wahl auf. */
    fun toggleOrganizer(name: String, id: String): EventsSelection =
        if (organizerId == id) copy(organizerName = null, organizerId = null) else withOrganizer(name, id)

    /** Setzt den Veranstalter, ohne ihn bei erneutem Aufruf abzuwählen („Alle kommenden von …“ im Detail). */
    fun withOrganizer(name: String, id: String): EventsSelection = copy(organizerName = name, organizerId = id)

    /** Zahl an der Filter-Taste: gesetzte Einstellungen im Sheet. */
    val sheetCount: Int get() = (if (kind == EventKind.All) 0 else 1) + (if (hideCancelled) 1 else 0)

    fun resetSheet(): EventsSelection = copy(kind = EventKind.All, hideCancelled = false)

    fun toFilter() = EventFilter(
        day = day,
        eveningOnly = eveningOnly,
        organizerId = organizerId,
        hideCancelled = hideCancelled,
        category = kind.category,
    )

    /** Was eine neue Abfrage an die API auslöst. „Gemerkt“ gehört nicht dazu, das filtert die App. */
    val loadKey: List<Any?> get() = listOf(day, eveningOnly, organizerId, kind, hideCancelled)

    /** Unterzeile unter dem Titel: „Was in Ilmenau ansteht“, „Heute ab 17 Uhr · Baracke5“, „Alles von Baracke5“. */
    fun subtitle(now: Instant): UiText {
        val today = BerlinTime.date(now)
        val extra = organizerName
        fun withExtra(text: UiText) = if (extra == null) text else UiText(R.string.events_unterzeile_zusatz, text, extra)
        if (eveningOnly) {
            val evening = day ?: today
            val whenText = if (evening == today) UiText(R.string.tag_heute) else UiText.plain(EventTimes.short(evening))
            return withExtra(UiText(R.string.events_unterzeile_abend, whenText))
        }
        if (day != null) return withExtra(EventTimes.sectionTitle(day, today))
        if (extra != null) return UiText(R.string.events_unterzeile_alles_von, extra)
        return UiText(R.string.events_unterzeile_standard)
    }

    /** Welcher Leer-Zustand passt, wenn nichts gefunden wurde. „Gemerkt“ geht vor „Heute Abend“. */
    sealed interface EmptyKind {
        data object Bookmarked : EmptyKind
        data class Evening(val today: Boolean) : EmptyKind
        data object General : EmptyKind
    }

    fun emptyKind(now: Instant): EmptyKind = when {
        onlyBookmarked -> EmptyKind.Bookmarked
        eveningOnly -> EmptyKind.Evening((day ?: BerlinTime.date(now)) == BerlinTime.date(now))
        else -> EmptyKind.General
    }

    companion object {
        /** „Heute Abend“ für heute, so öffnet „Alle“ auf der Heute-Seite die Liste. */
        fun evening(now: Instant): EventsSelection = EventsSelection().toggleEvening(now)
    }
}

/** Eine Tagesgruppe der Liste. */
data class EventSection(val day: LocalDate, val events: List<CampusEvent>)

/** Gruppiert die geladenen Veranstaltungen nach Tagen. */
object EventGrouping {
    /**
     * Kommende, aufsteigend. Gewählter Tag bzw. „Heute Abend“: alles steht unter diesem Tag, auch Mehrtägige, die vorher
     * begonnen haben. Sonst unter dem Beginn, Mehrtägige, die schon laufen, unter heute.
     */
    fun upcoming(events: List<CampusEvent>, selection: EventsSelection, now: Instant): List<EventSection> {
        val today = BerlinTime.date(now)
        return events
            .groupBy { selection.day ?: maxOf(today, it.firstDay) }
            .toSortedMap()
            .map { (day, list) -> EventSection(day, list.sortedBy { it.start }) }
    }

    /** Vergangene, der neueste Tag zuerst. Innerhalb eines Tages bleibt die Uhrzeit aufsteigend. */
    fun past(events: List<CampusEvent>): List<EventSection> =
        events
            .groupBy { it.firstDay }
            .toSortedMap(compareByDescending { it })
            .map { (day, list) -> EventSection(day, list.sortedBy { it.start }) }
}
