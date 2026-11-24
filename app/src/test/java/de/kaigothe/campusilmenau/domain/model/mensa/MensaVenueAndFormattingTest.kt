package de.kaigothe.campusilmenau.domain.model.mensa

import de.kaigothe.campusilmenau.domain.model.mensa.MensaFormatting.RelativeDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

class MensaVenueAndFormattingTest {

    // Mensen

    @Test
    fun `die vier Mensen in der Reihenfolge der App, mit den gespeicherten Schluesseln`() {
        assertEquals(
            listOf("mensaEhrenberg", "cafeteriaEhrenberg", "nanoteria", "roentgenbau"),
            MensaVenue.entries.map { it.key },
        )
    }

    @Test
    fun `die Zuordnung ueber den API-Namen ignoriert Leerraum und Schreibweise`() {
        assertTrue(MensaVenue.MensaEhrenberg.matches("Mensa Ehrenberg"))
        assertTrue(MensaVenue.MensaEhrenberg.matches("  mensa ehrenberg "))
        assertTrue(MensaVenue.Roentgenbau.matches("Cafeteria Röntgenbau"))
        assertTrue(MensaVenue.CafeteriaEhrenberg.matches("Cafeteria Mensa Ehrenberg"))
        // Die Cafeteria Ehrenberg ist nicht die Mensa Ehrenberg.
        assertFalse(MensaVenue.MensaEhrenberg.matches("Cafeteria Mensa Ehrenberg"))
        assertFalse(MensaVenue.Nanoteria.matches("Cafeteria Mini"))
    }

    @Test
    fun `nur die Cafeteria Ehrenberg hat Mittag und Abendessen`() {
        assertEquals(listOf(MensaVenue.CafeteriaEhrenberg), MensaVenue.entries.filter { it.hasMealtimes })
    }

    @Test
    fun `die naechste Mensa geht reihum`() {
        assertEquals(MensaVenue.CafeteriaEhrenberg, MensaVenue.MensaEhrenberg.next)
        assertEquals(MensaVenue.MensaEhrenberg, MensaVenue.Roentgenbau.next)
    }

    @Test
    fun `der Schluessel fuehrt zur Mensa zurueck`() {
        MensaVenue.entries.forEach { assertEquals(it, MensaVenue.fromKey(it.key)) }
        assertNull(MensaVenue.fromKey("mensaMini"))
        assertNull(MensaVenue.fromKey(null))
    }

    // Formatierung

    @Test
    fun `Werktage ueberspringen Samstag und Sonntag`() {
        // Donnerstag, 26.11.2026
        val days = MensaFormatting.workdays(LocalDate.of(2026, 11, 26))
        assertEquals(15, days.size)
        assertTrue(days.none { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY })
        assertEquals(LocalDate.of(2026, 11, 26), days.first())
        assertEquals(LocalDate.of(2026, 11, 27), days[1])
        // Nach dem Freitag kommt der Montag.
        assertEquals(LocalDate.of(2026, 11, 30), days[2])
        assertEquals(LocalDate.of(2026, 12, 16), days.last())
    }

    @Test
    fun `an einem Wochenende beginnen die Werktage am Montag`() {
        val days = MensaFormatting.workdays(LocalDate.of(2026, 11, 28))
        assertEquals(LocalDate.of(2026, 11, 30), days.first())
    }

    @Test
    fun `heute, morgen und sonst`() {
        val today = LocalDate.of(2026, 11, 24)
        assertEquals(RelativeDay.Today, MensaFormatting.relative(today, today))
        assertEquals(RelativeDay.Tomorrow, MensaFormatting.relative(today.plusDays(1), today))
        assertEquals(RelativeDay.Other, MensaFormatting.relative(today.plusDays(2), today))
        assertEquals(RelativeDay.Other, MensaFormatting.relative(today.minusDays(1), today))
    }

    @Test
    fun `deutsche Datumsangaben`() {
        val day = LocalDate.of(2026, 10, 29)
        assertEquals("Do., 29. Okt.", MensaFormatting.shortDate(day, Locale.GERMAN))
        assertEquals("Donnerstag, 29. Oktober", MensaFormatting.spokenDay(day, Locale.GERMAN))
        assertEquals("Donnerstag, 29. Oktober 2026", MensaFormatting.fullDate(day, Locale.GERMAN))
        assertEquals("Do", MensaFormatting.weekdayShort(day, Locale.GERMAN))
        assertEquals("29", MensaFormatting.dayOfMonth(day))
    }

    @Test
    fun `englische Datumsangaben`() {
        val day = LocalDate.of(2026, 10, 29)
        assertEquals("Thu, Oct 29", MensaFormatting.shortDate(day, Locale.ENGLISH))
        assertEquals("Thursday, October 29, 2026", MensaFormatting.fullDate(day, Locale.ENGLISH))
        assertEquals("Thu", MensaFormatting.weekdayShort(day, Locale.ENGLISH))
    }
}
