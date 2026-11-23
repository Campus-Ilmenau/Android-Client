package de.kaigothe.campusilmenau.core.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BerlinTimeTest {

    @Test
    fun `Tag ist der Berliner Tag, nicht der UTC-Tag`() {
        // 23:30 UTC im Winter ist 00:30 Uhr Berliner Zeit am Folgetag.
        val instant = Instant.parse("2026-11-23T23:30:00Z")
        assertEquals(LocalDate.of(2026, 11, 24), BerlinTime.date(instant))
    }

    @Test
    fun `Tagesbeginn im Winter ist 23 Uhr UTC des Vortags`() {
        val start = BerlinTime.startOfDay(LocalDate.of(2026, 11, 24))
        assertEquals(Instant.parse("2026-11-23T23:00:00Z"), start)
    }

    @Test
    fun `Tagesbeginn im Sommer ist 22 Uhr UTC des Vortags`() {
        val start = BerlinTime.startOfDay(LocalDate.of(2026, 7, 1))
        assertEquals(Instant.parse("2026-06-30T22:00:00Z"), start)
    }

    @Test
    fun `Tagesbeginn am Tag der Umstellung auf Sommerzeit`() {
        // 29.03.2026: um 02:00 springt die Uhr auf 03:00. Mitternacht liegt noch in der Winterzeit.
        val start = BerlinTime.startOfDay(LocalDate.of(2026, 3, 29))
        assertEquals(Instant.parse("2026-03-28T23:00:00Z"), start)
    }

    @Test
    fun `startOfDay eines Zeitpunkts gehoert zum Berliner Tag`() {
        val start = BerlinTime.startOfDay(Instant.parse("2026-11-23T23:30:00Z"))
        assertEquals(Instant.parse("2026-11-23T23:00:00Z"), start)
    }

    @Test
    fun `Minuten seit Mitternacht in Berliner Zeit`() {
        assertEquals(12 * 60 + 30, BerlinTime.minutesOfDay(Instant.parse("2026-07-01T10:30:00Z")))
        assertEquals(11 * 60 + 30, BerlinTime.minutesOfDay(Instant.parse("2026-12-01T10:30:00Z")))
    }

    @Test
    fun `at setzt eine Uhrzeit auf den Berliner Tag`() {
        val instant = BerlinTime.at(LocalDate.of(2026, 7, 1), 12 * 60)
        assertEquals(Instant.parse("2026-07-01T10:00:00Z"), instant)
    }
}
