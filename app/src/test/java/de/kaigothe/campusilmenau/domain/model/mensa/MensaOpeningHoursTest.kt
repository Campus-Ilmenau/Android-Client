package de.kaigothe.campusilmenau.domain.model.mensa

import de.kaigothe.campusilmenau.domain.model.mensa.MensaOpeningHours.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

/** Testfälle aus `MensaDetailTests` (Öffnungszeiten) und Status-Chip der iOS-App. */
class MensaOpeningHoursTest {

    private val ehrenberg = listOf("mon", "tue", "wed", "thu").map { OpeningHour(it, "11:00", "14:00") } +
        OpeningHour("fri", "11:00", "13:30")

    private val cafeteria = listOf("mon", "tue", "wed", "thu").map { OpeningHour(it, "08:00", "20:00") } +
        OpeningHour("fri", "08:00", "19:30") + OpeningHour("sat", "11:30", "14:00")

    /** 4. November 2026 ist ein Mittwoch, der 6. ein Freitag, der 8. ein Sonntag. */
    private fun at(day: Int, hour: Int, minute: Int) = LocalDateTime.of(2026, 11, day, hour, minute)

    @Test
    fun `Wochenzeilen fassen Tage mit gleicher Zeit zusammen`() {
        val rows = MensaOpeningHours.weekRows(ehrenberg)
        assertEquals(
            listOf("Montag – Donnerstag", "Freitag", "Samstag, Sonntag"),
            rows.map { MensaOpeningHours.label(it, Locale.GERMAN) },
        )
        assertEquals(listOf("11:00–14:00", "11:00–13:30", null), rows.map { it.range })
    }

    @Test
    fun `ein geoeffneter Samstag bekommt eine eigene Zeile`() {
        val rows = MensaOpeningHours.weekRows(cafeteria)
        assertEquals(
            listOf("Montag – Donnerstag", "Freitag", "Samstag", "Sonntag"),
            rows.map { MensaOpeningHours.label(it, Locale.GERMAN) },
        )
    }

    @Test
    fun `ohne Zeiten ist die ganze Woche eine geschlossene Zeile`() {
        val rows = MensaOpeningHours.weekRows(emptyList())
        assertEquals(1, rows.size)
        assertEquals(7, rows.single().days.size)
        assertNull(rows.single().range)
    }

    @Test
    fun `die heutige Zeit folgt dem Wochentag`() {
        assertEquals("11:00–14:00", MensaOpeningHours.range(DayOfWeek.WEDNESDAY, ehrenberg))
        assertEquals("11:00–13:30", MensaOpeningHours.range(DayOfWeek.FRIDAY, ehrenberg))
        assertNull(MensaOpeningHours.range(DayOfWeek.SUNDAY, ehrenberg))
    }

    @Test
    fun `Status offen, gleich zu und geschlossen mit den Grenzen der iOS-App`() {
        fun state(hour: Int, minute: Int, day: Int = 4) = MensaOpeningHours.state(ehrenberg, at(day, hour, minute))
        assertEquals(State.Closed, state(10, 59))
        assertEquals(State.Open, state(11, 0))
        // Genau 30 Minuten vor Schluss ist es noch nicht „gleich zu“.
        assertEquals(State.Open, state(13, 30))
        assertEquals(State.ClosingSoon, state(13, 31))
        assertEquals(State.ClosingSoon, state(13, 59))
        assertEquals(State.Closed, state(14, 0))
        // Sonntag
        assertEquals(State.Closed, state(12, 0, day = 8))
        assertEquals(State.Closed, MensaOpeningHours.state(emptyList(), at(4, 12, 0)))
    }

    @Test
    fun `das Abendessen nennt das Ende der Oeffnungszeit`() {
        assertEquals(MensaOpeningHours.DinnerHours("20:00", "19:30"), MensaOpeningHours.dinnerHours(cafeteria))
        val gleich = listOf(OpeningHour("mon", "08:00", "20:00"), OpeningHour("fri", "08:00", "20:00"))
        assertEquals(MensaOpeningHours.DinnerHours("20:00", null), MensaOpeningHours.dinnerHours(gleich))
        assertNull(MensaOpeningHours.dinnerHours(emptyList()))
    }

    @Test
    fun `Uhrzeiten lesen und kuerzen`() {
        assertEquals(660, MensaOpeningHours.minutes("11:00"))
        assertEquals(630, MensaOpeningHours.minutes("10:30:00"))
        assertNull(MensaOpeningHours.minutes("elf"))
        assertNull(MensaOpeningHours.minutes("11"))
        assertEquals("11", MensaOpeningHours.short("11:00"))
        assertEquals("10:30", MensaOpeningHours.short("10:30"))
        assertEquals("8:00", MensaOpeningHours.clock("08:00"))
        assertEquals("kaputt", MensaOpeningHours.clock("kaputt"))
    }

    // Status-Chip

    private fun canteen(venue: MensaVenue, hours: List<OpeningHour>) =
        Canteen(1, venue.apiName, true, "Ilmenau", null, hours)

    private fun chip(
        venue: MensaVenue = MensaVenue.MensaEhrenberg,
        hours: List<OpeningHour> = ehrenberg,
        day: LocalDate = LocalDate.of(2026, 11, 4),
        mealtime: MensaMealtime = MensaMealtime.Lunch,
        now: LocalDateTime,
    ) = MensaStatus.chip(venue, listOf(canteen(venue, hours)), day, mealtime, now)

    @Test
    fun `ohne Oeffnungszeiten der Mensa gibt es keinen Chip`() {
        assertNull(MensaStatus.chip(MensaVenue.Nanoteria, emptyList(), LocalDate.of(2026, 11, 4), MensaMealtime.Lunch, at(4, 12, 0)))
        // Die Zeiten einer anderen Mensa zählen nicht.
        assertNull(
            MensaStatus.chip(
                MensaVenue.Nanoteria,
                listOf(canteen(MensaVenue.MensaEhrenberg, ehrenberg)),
                LocalDate.of(2026, 11, 4),
                MensaMealtime.Lunch,
                at(4, 12, 0),
            ),
        )
    }

    @Test
    fun `heute vor der Oeffnung, offen, gleich zu und nach Schluss`() {
        assertEquals(MensaStatus.FromClock("11:00"), chip(now = at(4, 9, 0)))
        assertEquals(MensaStatus.Open("11", "14"), chip(now = at(4, 12, 0)))
        // Hier gilt „gleich zu“ schon bei genau 30 Minuten (anders als im Zustand der Öffnungszeiten).
        assertEquals(MensaStatus.ClosingSoon, chip(now = at(4, 13, 30)))
        assertEquals(MensaStatus.Open("11", "14"), chip(now = at(4, 13, 29)))
        assertEquals(MensaStatus.Closed, chip(now = at(4, 14, 0)))
    }

    @Test
    fun `ein anderer Tag zeigt die Zeit, auch wenn jetzt schon geschlossen ist`() {
        assertEquals(MensaStatus.Range("11", "14"), chip(day = LocalDate.of(2026, 11, 5), now = at(4, 20, 0)))
        assertEquals(MensaStatus.Range("11", "13:30"), chip(day = LocalDate.of(2026, 11, 6), now = at(4, 9, 0)))
    }

    @Test
    fun `ein Tag ohne Zeiten ist geschlossen`() {
        assertEquals(MensaStatus.Closed, chip(day = LocalDate.of(2026, 11, 8), now = at(4, 9, 0)))
    }

    @Test
    fun `Abendessen der Cafeteria nennt das Ende`() {
        val status = chip(
            venue = MensaVenue.CafeteriaEhrenberg,
            hours = cafeteria,
            mealtime = MensaMealtime.Dinner,
            now = at(4, 12, 0),
        )
        assertEquals(MensaStatus.UntilClock("20:00".let { MensaOpeningHours.clock(it) }), status)
        // Nach Schluss geht „geschlossen“ vor.
        assertEquals(
            MensaStatus.Closed,
            chip(venue = MensaVenue.CafeteriaEhrenberg, hours = cafeteria, mealtime = MensaMealtime.Dinner, now = at(4, 20, 30)),
        )
    }

    @Test
    fun `die Wahl Abendessen gilt nur fuer die Cafeteria Ehrenberg`() {
        val status = chip(mealtime = MensaMealtime.Dinner, now = at(4, 12, 0))
        assertEquals(MensaStatus.Open("11", "14"), status)
    }

    @Test
    fun `Farbtoene der Chips`() {
        assertEquals(StatusTone.Danger, MensaStatus.Closed.tone)
        assertEquals(StatusTone.Warning, MensaStatus.ClosingSoon.tone)
        assertEquals(StatusTone.Green, MensaStatus.Open("11", "14").tone)
        assertEquals(StatusTone.Neutral, MensaStatus.FromClock("8:00").tone)
        assertEquals(StatusTone.Neutral, MensaStatus.Range("11", "14").tone)
        assertEquals(StatusTone.Neutral, MensaStatus.UntilClock("20:00").tone)
    }
}
