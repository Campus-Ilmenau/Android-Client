package de.kaigothe.campusilmenau.domain.model.mensa

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Formatierung von Tagen im Speiseplan. Die Sprache bestimmt der Aufrufer (Standard: Sprache der App). */
object MensaFormatting {

    /** Wie nah ein Tag an heute liegt. Die Oberfläche macht daraus „Heute“, „Morgen“ oder das Datum. */
    enum class RelativeDay { Today, Tomorrow, Other }

    fun relative(day: LocalDate, today: LocalDate): RelativeDay = when (day) {
        today -> RelativeDay.Today
        today.plusDays(1) -> RelativeDay.Tomorrow
        else -> RelativeDay.Other
    }

    private fun isGerman(locale: Locale) = locale.language == "de"

    private fun formatter(germanPattern: String, otherPattern: String, locale: Locale) =
        DateTimeFormatter.ofPattern(if (isGerman(locale)) germanPattern else otherPattern, locale)

    /** „Do., 29. Okt.“ */
    fun shortDate(day: LocalDate, locale: Locale = Locale.getDefault()): String =
        day.format(formatter("EEE, d. MMM", "EEE, MMM d", locale))

    /** Für TalkBack: „Donnerstag, 29. Oktober“. */
    fun spokenDay(day: LocalDate, locale: Locale = Locale.getDefault()): String =
        day.format(formatter("EEEE, d. MMMM", "EEEE, MMMM d", locale))

    /** „Donnerstag, 29. Oktober 2026“. */
    fun fullDate(day: LocalDate, locale: Locale = Locale.getDefault()): String =
        day.format(formatter("EEEE, d. MMMM yyyy", "EEEE, MMMM d, yyyy", locale))

    /** „Do“ (ohne Punkt) für die Tagesleiste. */
    fun weekdayShort(day: LocalDate, locale: Locale = Locale.getDefault()): String =
        day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replace(".", "")

    /** Tag des Monats für die Tagesleiste: „29“. */
    fun dayOfMonth(day: LocalDate): String = day.dayOfMonth.toString()

    /** 15 Werktage ab [start] (drei Wochen), Samstag und Sonntag übersprungen. */
    fun workdays(start: LocalDate, count: Int = 15): List<LocalDate> {
        val days = ArrayList<LocalDate>(count)
        var day = start
        while (days.size < count) {
            if (day.dayOfWeek != DayOfWeek.SATURDAY && day.dayOfWeek != DayOfWeek.SUNDAY) days += day
            day = day.plusDays(1)
        }
        return days
    }
}
