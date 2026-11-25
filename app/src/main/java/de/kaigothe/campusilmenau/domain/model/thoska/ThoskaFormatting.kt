package de.kaigothe.campusilmenau.domain.model.thoska

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.heute.HeuteClock
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

/** Darstellung von Guthaben und Zeitpunkten für die Thoska-Ansicht. Beträge kommen in Tausendstel Euro. */
object ThoskaFormatting {
    private const val MINUS = "− "
    private const val PLUS = "+ "

    /** „12,40 €“ */
    fun euro(milli: Long, locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        format.currency = Currency.getInstance("EUR")
        return format.format(milli / 1000.0)
    }

    /** Für TalkBack: „12,40 Euro“. */
    fun spokenEuro(milli: Long, locale: Locale = Locale.getDefault()): UiText {
        val number = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(milli / 1000.0)
        return UiText(R.string.thoska_euro_sprechen, number)
    }

    /** „− 2,90 €“ mit echtem Minuszeichen und festem Leerzeichen. */
    fun minusEuro(milli: Long, locale: Locale = Locale.getDefault()): String = MINUS + euro(milli, locale)

    /** „+ 20,00 €“ */
    fun plusEuro(milli: Long, locale: Locale = Locale.getDefault()): String = PLUS + euro(milli, locale)

    /** „Gerade eben“, „Heute, 08:42“, „Gestern, 08:42“, sonst „Mo., 02.11., 08:42“. */
    fun lastRead(date: Instant, now: Instant, locale: Locale = Locale.getDefault()): UiText {
        if (abs(Duration.between(date, now).seconds) < 60) return UiText(R.string.thoska_gerade_eben)
        val time = HeuteClock.time(date)
        val day = BerlinTime.date(date)
        val today = BerlinTime.date(now)
        return when (day) {
            today -> UiText(R.string.thoska_zeit_heute, time)
            today.minusDays(1) -> UiText(R.string.thoska_zeit_gestern, time)
            else -> UiText(R.string.thoska_zeit_datum, day.format(DateTimeFormatter.ofPattern("EEE, dd.MM.", locale)), time)
        }
    }
}
