package de.kaigothe.campusilmenau.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Alle Tage und Uhrzeiten der App laufen in der Zeitzone Europe/Berlin, nicht in der des Geräts
 * (wie `EventKalender` und `HeuteUhr` in der iOS-App). Hier stehen die Grundoperationen.
 */
object BerlinTime {
    val zone: ZoneId = ZoneId.of("Europe/Berlin")

    /** Der Berliner Kalendertag des Zeitpunkts. */
    fun date(instant: Instant): LocalDate = instant.atZone(zone).toLocalDate()

    /** Beginn des Berliner Tages (Mitternacht Berliner Zeit). */
    fun startOfDay(instant: Instant): Instant = startOfDay(date(instant))

    fun startOfDay(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant()

    /** Minuten seit Mitternacht (Berlin). */
    fun minutesOfDay(instant: Instant): Int {
        val time = instant.atZone(zone).toLocalTime()
        return time.hour * 60 + time.minute
    }

    /** Der Zeitpunkt `minutes` nach Mitternacht am Berliner Tag `date`. */
    fun at(date: LocalDate, minutes: Int): Instant =
        date.atStartOfDay(zone).plusMinutes(minutes.toLong()).toInstant()
}
