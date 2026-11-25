package de.kaigothe.campusilmenau.domain.model.events

import de.kaigothe.campusilmenau.core.time.BerlinTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Testfälle aus `EventsTests` der iOS-App: Status, letzter Tag, Veranstalter, Filter. */
class CampusEventTest {

    private val day = LocalDate.of(2026, 11, 24)
    private fun at(hour: Int, minute: Int = 0, date: LocalDate = day): Instant = BerlinTime.at(date, hour * 60 + minute)
    private fun event(start: Instant, end: Instant? = null, allDay: Boolean = false) =
        CampusEvent(id = "e", title = "Titel", start = start, end = end, allDay = allDay)

    @Test
    fun `Status der API`() {
        assertEquals(EventStatus.Scheduled, EventStatus.fromApi("scheduled"))
        assertEquals(EventStatus.Scheduled, EventStatus.fromApi(null))
        assertEquals(EventStatus.Scheduled, EventStatus.fromApi("irgendwas"))
        assertEquals(EventStatus.Tentative, EventStatus.fromApi(" Tentative "))
        assertEquals(EventStatus.Tentative, EventStatus.fromApi("unconfirmed"))
        assertEquals(EventStatus.Cancelled, EventStatus.fromApi("CANCELLED"))
        assertEquals(EventStatus.Cancelled, EventStatus.fromApi("canceled"))
    }

    @Test
    fun `ohne Ende oder mit Ende am Starttag ist der Starttag der letzte`() {
        assertEquals(day, event(at(18)).lastDay)
        assertEquals(day, event(at(18), end = at(18)).lastDay)
        assertEquals(day, event(at(18), end = at(17)).lastDay)
        assertEquals(day, event(at(18), end = at(22)).lastDay)
        assertFalse(event(at(18), end = at(22)).isMultiDay)
    }

    @Test
    fun `eine Nachtveranstaltung bis 6 Uhr zaehlt noch zum Starttag`() {
        val nextDay = day.plusDays(1)
        assertEquals(day, event(at(22), end = at(6, 0, nextDay)).lastDay)
        // Eine Minute nach 06:00 ist es der Folgetag.
        assertEquals(nextDay, event(at(22), end = at(6, 1, nextDay)).lastDay)
    }

    @Test
    fun `ein Ende genau um Mitternacht gehoert zum Vortag`() {
        val midnight = BerlinTime.startOfDay(day.plusDays(1))
        assertEquals(day, event(BerlinTime.startOfDay(day), end = midnight, allDay = true).lastDay)
        // Dreitägig ganztägig: Ende exklusiv am vierten Tag um 00:00.
        val threeDays = event(BerlinTime.startOfDay(day), end = BerlinTime.startOfDay(day.plusDays(3)), allDay = true)
        assertEquals(day.plusDays(2), threeDays.lastDay)
        assertTrue(threeDays.isMultiDay)
    }

    @Test
    fun `mehrtaegige Veranstaltungen laufen an jedem Tag dazwischen`() {
        val event = event(at(18), end = at(12, 0, day.plusDays(2)))
        assertFalse(event.runsOn(day.minusDays(1)))
        assertTrue(event.runsOn(day))
        assertTrue(event.runsOn(day.plusDays(1)))
        assertTrue(event.runsOn(day.plusDays(2)))
        assertFalse(event.runsOn(day.plusDays(3)))
    }

    @Test
    fun `Frist wird ohne Beachtung der Schreibweise erkannt`() {
        assertTrue(event(at(9)).copy(category = "Frist").isDeadline)
        assertTrue(event(at(9)).copy(category = "FRIST").isDeadline)
        assertFalse(event(at(9)).copy(category = "Veranstaltung").isDeadline)
        assertFalse(event(at(9)).isDeadline)
    }

    @Test
    fun `der Veranstalter entfaellt, wenn er leer oder gleich dem Ort ist`() {
        val base = event(at(9))
        assertNull(base.organizerForDisplay)
        assertNull(base.copy(organizer = "  ").organizerForDisplay)
        assertNull(base.copy(organizer = "Club Ilmenau", location = " club ilmenau ").organizerForDisplay)
        assertEquals("Verein", base.copy(organizer = " Verein ", location = "Club").organizerForDisplay)
        assertEquals("Verein", base.copy(organizer = "Verein").organizerForDisplay)
    }

    @Test
    fun `die Sicht der Startseite uebernimmt Absage und Veranstalter`() {
        val heute = event(at(20)).copy(status = EventStatus.Cancelled, organizer = "Verein", location = "Club").toHeuteEvent()
        assertTrue(heute.cancelled)
        assertEquals("Verein", heute.organizer)
        assertEquals("Club", heute.location)
    }

    // MARK: Detail und Kalender

    @Test
    fun `Tickets und Kalender entfallen bei einer Absage`() {
        val base = event(at(20)).copy(ticketUrl = "https://t.example", infoUrl = "https://i.example")
        val normal = EventDetailDisplay(base)
        assertTrue(normal.showTickets)
        assertFalse(normal.calendarIsPrimary)
        assertTrue(normal.showExternalHint)
        assertEquals("https://i.example", normal.shareUrl)

        val cancelled = EventDetailDisplay(base.copy(status = EventStatus.Cancelled))
        assertFalse(cancelled.showTickets)
        assertFalse(cancelled.showCalendar)
        // Der Infolink bleibt, damit der Hinweis zur externen Seite stimmt.
        assertTrue(cancelled.showExternalHint)
    }

    @Test
    fun `ohne Tickets ist der Kalender der Hauptbutton und ohne Links entfaellt der Hinweis`() {
        val display = EventDetailDisplay(event(at(20)))
        assertTrue(display.calendarIsPrimary)
        assertFalse(display.showExternalHint)
        assertNull(display.shareUrl)
        assertEquals("t", EventDetailDisplay(event(at(20)).copy(ticketUrl = "t")).shareUrl)
    }

    @Test
    fun `die Titelgroesse sinkt bei langen Titeln und leere Texte entfallen`() {
        assertEquals(32, EventDetailDisplay(event(at(20))).titleSize)
        assertEquals(26, EventDetailDisplay(event(at(20)).copy(title = "x".repeat(41))).titleSize)
        val display = EventDetailDisplay(event(at(20)).copy(location = "  ", description = " Text ", address = ""))
        assertNull(display.location)
        assertNull(display.address)
        assertNull(display.mapQuery)
        assertEquals("Text", display.description)
    }

    @Test
    fun `der Kalendereintrag nimmt ohne Ende zwei Stunden und fasst Ort und Adresse zusammen`() {
        val entry = CalendarEntry.of(event(at(19)).copy(location = "Club", address = " Ehrenbergstr. 1 ", infoUrl = "https://i.example"))
        assertEquals(at(19).toEpochMilli(), entry.beginMillis)
        assertEquals(at(21).toEpochMilli(), entry.endMillis)
        assertFalse(entry.allDay)
        assertEquals("Club, Ehrenbergstr. 1", entry.location)
        assertEquals("https://i.example", entry.url)
        // Ein Ende vor dem Beginn zählt nicht.
        assertEquals(at(21).toEpochMilli(), CalendarEntry.of(event(at(19), end = at(18))).endMillis)
        assertNull(CalendarEntry.of(event(at(19))).location)
    }

    @Test
    fun `ein ganztaegiger Eintrag laeuft in UTC bis Mitternacht nach dem letzten Tag`() {
        val allDay = event(BerlinTime.startOfDay(day), end = BerlinTime.startOfDay(day.plusDays(2)), allDay = true)
        val entry = CalendarEntry.of(allDay)
        assertTrue(entry.allDay)
        // 24.11. und 25.11., Ende exklusiv am 26.11. 00:00 UTC
        assertEquals(LocalDate.of(2026, 11, 24).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(), entry.beginMillis)
        assertEquals(LocalDate.of(2026, 11, 26).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(), entry.endMillis)
    }

    @Test
    fun `Zeitzeile der Karte`() {
        fun text(event: CampusEvent, reference: LocalDate = day) = EventTimes.timeLine(event, reference, java.util.Locale.GERMAN)
        assertEquals(de.kaigothe.campusilmenau.core.common.UiText(de.kaigothe.campusilmenau.R.string.events_ganztaegig), text(event(at(0), allDay = true)))
        assertEquals(
            de.kaigothe.campusilmenau.core.common.UiText(de.kaigothe.campusilmenau.R.string.events_zeit_von_bis, "19:00", "23:00"),
            text(event(at(19), end = at(23))),
        )
        assertEquals(
            de.kaigothe.campusilmenau.core.common.UiText(de.kaigothe.campusilmenau.R.string.events_zeit_ab, "20:00"),
            text(event(at(20))),
        )
        // Mehrtägig, schon vor dem Bezugstag begonnen: „Läuft seit … · bis …“.
        val multi = event(at(10, 0, day.minusDays(1)), end = at(12, 0, day.plusDays(1)))
        val running = text(multi)
        assertEquals(de.kaigothe.campusilmenau.R.string.events_zeit_laeuft_seit, running.id)
    }

    @Test
    fun `Titel der Tagesabschnitte`() {
        val locale = java.util.Locale.GERMAN
        assertEquals(de.kaigothe.campusilmenau.R.string.events_abschnitt_heute, EventTimes.sectionTitle(day, day, locale).id)
        assertEquals(de.kaigothe.campusilmenau.R.string.events_abschnitt_morgen, EventTimes.sectionTitle(day.plusDays(1), day, locale).id)
        assertEquals(de.kaigothe.campusilmenau.R.string.events_abschnitt_datum, EventTimes.sectionTitle(day.plusDays(5), day, locale).id)
        assertEquals(de.kaigothe.campusilmenau.R.string.events_abschnitt_gestern, EventTimes.pastSectionTitle(day.minusDays(1), day, locale).id)
        assertEquals("Dienstag", EventTimes.weekday(day, locale))
        assertEquals("Di", EventTimes.weekdayShort(day, locale))
    }

    // MARK: Filter

    private val now = at(10)

    @Test
    fun `ohne Filter gilt ab heute 00 Uhr Berliner Zeit`() {
        val query = EventFilter().query(now)
        // Berliner Mitternacht liegt am Vortag in UTC.
        assertEquals(listOf("from" to "2026-11-23"), query.time)
        assertEquals(BerlinTime.startOfDay(day), query.fromBoundary)
        assertEquals(day, query.runningOn)
        assertNull(query.toBoundary)
        assertEquals(listOf("include_cancelled" to "true"), query.common)
    }

    @Test
    fun `ein Zeitraum verbreitert das Fenster und merkt sich die Grenzen`() {
        val filter = EventFilter(period = EventFilter.Period(day, day.plusDays(7)))
        val query = filter.query(now)
        assertEquals(listOf("from" to "2026-11-23", "to" to "2026-12-01"), query.time)
        assertEquals(BerlinTime.startOfDay(day.plusDays(7)), query.toBoundary)
    }

    @Test
    fun `ein Tag geht dem Zeitraum vor`() {
        val filter = EventFilter(period = EventFilter.Period(day), day = day.plusDays(2))
        val query = filter.query(now)
        assertEquals(listOf("date" to "2026-11-26"), query.time)
        assertNull(query.fromBoundary)
        assertNull(query.runningOn)
    }

    @Test
    fun `Heute Abend nimmt den Tag oder heute und ignoriert den Zeitraum`() {
        assertEquals(
            listOf("date" to "2026-11-24", "evening" to "true"),
            EventFilter(eveningOnly = true, period = EventFilter.Period(day.plusDays(3))).query(now).time,
        )
        assertEquals(
            listOf("date" to "2026-11-27", "evening" to "true"),
            EventFilter(eveningOnly = true, day = day.plusDays(3)).query(now).time,
        )
    }

    @Test
    fun `Veranstalter, Kategorie und abgesagte kommen in die gemeinsamen Parameter`() {
        val query = EventFilter(organizerId = " cafe-bohne ", category = "Frist", hideCancelled = true).query(now)
        assertEquals(
            listOf("organizer" to "cafe-bohne", "category" to "Frist", "include_cancelled" to "false"),
            query.common,
        )
        assertEquals(query.time + query.common, query.params)
        // Leere Werte entfallen.
        assertEquals(listOf("include_cancelled" to "true"), EventFilter(organizerId = " ", category = "").query(now).common)
    }
}
