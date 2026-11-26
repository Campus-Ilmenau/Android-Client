package de.kaigothe.campusilmenau.domain.model.share

import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventStatus
import de.kaigothe.campusilmenau.domain.model.events.EventTimes
import de.kaigothe.campusilmenau.domain.model.heute.HeuteClock

/** Eine Veranstaltung als Text. Tage und Uhrzeiten laufen in Berlin wie überall bei den Veranstaltungen. */
class EventShare(
    val event: CampusEvent,
    private val strings: ShareStrings,
) : ShareContent {
    override val title: String get() = "${event.title} · ${EventTimes.short(event.firstDay, strings.locale)}"

    override val text: String
        get() {
            val name = if (event.status == EventStatus.Cancelled) strings.eventCancelled(event.title) else event.title
            var timeLine = "$dateText · $timeText"
            if (event.status == EventStatus.Tentative) timeLine += " " + strings.eventTentative()
            val place = listOfNotNull(ShareText.clean(event.location), ShareText.clean(event.address)).joinToString(", ")
            val organizer = ShareText.clean(event.organizer)?.let(strings::eventOrganizer).orEmpty()
            val head = listOf(name, timeLine, place, organizer)

            // Abgesagt: keine Beschreibung, wie auf dem Bild.
            val description = if (event.status == EventStatus.Cancelled) {
                ""
            } else {
                ShareText.clean(event.description)?.let { ShareText.shortened(it, DescriptionLimit) }.orEmpty()
            }

            val link = when {
                !event.ticketUrl.isNullOrBlank() -> strings.eventTickets(event.ticketUrl)
                !event.infoUrl.isNullOrBlank() -> strings.eventInfo(event.infoUrl)
                else -> ""
            }
            return ShareText.compose(listOf(head, listOf(description), listOf(link)), strings)
        }

    /** „Freitag, 30. Oktober“, mehrtägig „Mittwoch, 28. Oktober – Sonntag, 1. November“. */
    val dateText: String
        get() {
            val first = EventTimes.fullDay(event.firstDay, strings.locale)
            if (!event.isMultiDay) return first
            return "$first – ${EventTimes.fullDay(event.lastDay, strings.locale)}"
        }

    /** „21:00–03:00 Uhr“, ohne Ende „ab 21:00 Uhr“, ganztägig „ganztägig“. */
    private val timeText: String
        get() {
            if (event.allDay) return strings.eventAllDay()
            val start = HeuteClock.time(event.start)
            val end = event.end
            if (end != null && end > event.start && !event.isMultiDay) return strings.eventRange(start, HeuteClock.time(end))
            return strings.eventFrom(start)
        }

    companion object {
        /** Längste Beschreibung im Text. */
        const val DescriptionLimit = 280
    }
}
