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
