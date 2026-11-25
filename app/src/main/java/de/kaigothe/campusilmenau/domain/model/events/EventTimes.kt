package de.kaigothe.campusilmenau.domain.model.events

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.heute.HeuteClock
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Zeit- und Datumstexte der Veranstaltungen in der Sprache der App und in Europe/Berlin. */
object EventTimes {

    /**
     * Zeitzeile der Karte: „19:00–23:00 Uhr“, „Ab 20:00 Uhr“, „Ganztägig“ oder bei mehrtägigen „Läuft seit Mi 28. Okt. ·
     * bis So 1. Nov.“. [referenceDay] ist der Tag, unter dem die Karte steht: Mehrtägige, die vorher begonnen haben,
     * „laufen seit“.
     */
    fun timeLine(event: CampusEvent, referenceDay: LocalDate, locale: Locale = Locale.getDefault()): UiText {
        if (event.isMultiDay) {
            val until = short(event.lastDay, locale)
            if (event.firstDay < referenceDay) {
                return UiText(R.string.events_zeit_laeuft_seit, short(event.firstDay, locale), until)
            }
            if (event.allDay) return UiText(R.string.events_tage_bis, short(event.firstDay, locale), until)
            return UiText(R.string.events_zeit_ab_bis_tag, HeuteClock.time(event.start), until)
        }
        if (event.allDay) return UiText(R.string.events_ganztaegig)
        val end = event.end
        if (end != null && end > event.start) {
            return UiText(R.string.events_zeit_von_bis, HeuteClock.time(event.start), HeuteClock.time(end))
        }
        return UiText(R.string.events_zeit_ab, HeuteClock.time(event.start))
    }

    /** Titel eines Tagesabschnitts: „Heute · Donnerstag“, „Morgen · Freitag“, sonst „Sonntag, 1. Nov.“. */
    fun sectionTitle(day: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): UiText {
        val weekday = weekday(day, locale)
        return when (day) {
            today -> UiText(R.string.events_abschnitt_heute, weekday)
            today.plusDays(1) -> UiText(R.string.events_abschnitt_morgen, weekday)
            else -> UiText(R.string.events_abschnitt_datum, weekday, dayMonth(day, locale))
        }
    }

    /** Titel eines Tagesabschnitts bei den vergangenen Veranstaltungen: „Gestern · Mittwoch“, sonst „Sonntag, 1. Nov.“. */
    fun pastSectionTitle(day: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): UiText =
        if (day == today.minusDays(1)) UiText(R.string.events_abschnitt_gestern, weekday(day, locale)) else sectionTitle(day, today, locale)

    /** Datum und Zeit im Detail, zwei Zeilen: „Freitag, 30. Oktober“ / „21:00 – 03:00 Uhr“. */
    fun dateAndTime(event: CampusEvent, now: Instant, locale: Locale = Locale.getDefault()): Pair<String, UiText> {
        if (event.isMultiDay) {
            val date = "${short(event.firstDay, locale)} – ${short(event.lastDay, locale)}"
            if (event.allDay) {
                return date to UiText(if (isRunning(event, now)) R.string.events_ganztaegig_laeuft else R.string.events_ganztaegig)
            }
            val begin = UiText(R.string.events_zeit_ab, HeuteClock.time(event.start))
            val end = event.end
            if (end != null && end > event.start) {
                return date to UiText(R.string.events_zeit_ab_und_bis_uhr, begin, HeuteClock.time(end))
            }
            return date to begin
        }
        val date = fullDay(event.firstDay, locale)
        if (event.allDay) return date to UiText(R.string.events_ganztaegig)
        val end = event.end
        if (end != null && end > event.start) {
            return date to UiText(R.string.events_zeit_von_bis_detail, HeuteClock.time(event.start), HeuteClock.time(end))
        }
        return date to UiText(R.string.events_zeit_ab, HeuteClock.time(event.start))
    }

    private fun isRunning(event: CampusEvent, now: Instant): Boolean {
        val end = event.end ?: return false
        return event.start <= now && now < end
    }

    /** „Donnerstag“. */
    fun weekday(day: LocalDate, locale: Locale = Locale.getDefault()): String = day.dayOfWeek.getDisplayName(TextStyle.FULL, locale)

    /** „Do“ (ohne Punkt). */
    fun weekdayShort(day: LocalDate, locale: Locale = Locale.getDefault()): String =
        day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).replace(".", "")

    /** „1. Nov.“ bzw. „1 Nov“. */
    fun dayMonth(day: LocalDate, locale: Locale = Locale.getDefault()): String {
        val month = day.month.getDisplayName(TextStyle.SHORT, locale)
        return if (locale.language == "de") "${day.dayOfMonth}. $month" else "${day.dayOfMonth} $month"
    }

    /** „Mi 28. Okt.“ (Wochentag ohne Punkt). */
    fun short(day: LocalDate, locale: Locale = Locale.getDefault()): String = "${weekdayShort(day, locale)} ${dayMonth(day, locale)}"

    /** „Freitag, 30. Oktober“. */
    fun fullDay(day: LocalDate, locale: Locale = Locale.getDefault()): String =
        day.format(java.time.format.DateTimeFormatter.ofPattern(if (locale.language == "de") "EEEE, d. MMMM" else "EEEE, d MMMM", locale))

    /** Tage der Tagesleiste: heute und die 13 folgenden. */
    fun stripDays(now: Instant, count: Int = 14): List<LocalDate> {
        val today = BerlinTime.date(now)
        return (0 until count).map { today.plusDays(it.toLong()) }
    }
}
