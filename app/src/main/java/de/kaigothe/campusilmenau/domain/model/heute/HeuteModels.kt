package de.kaigothe.campusilmenau.domain.model.heute

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.mensa.MealChip
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import java.time.Instant

/** Uhrzeit-Rechnung der Heute-Seite. Alles läuft in Europe/Berlin (siehe [BerlinTime]). */
object HeuteClock {
    /** 630 → „10:30“. Fest 24-Stunden-Schreibweise. */
    fun time(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

    fun time(instant: Instant): String = time(BerlinTime.minutesOfDay(instant))

    /** „11:00“ → 660; `null`, wenn der Text keine Uhrzeit ist. */
    fun minutes(text: String): Int? {
        val parts = text.split(':')
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    fun isSameDay(a: Instant, b: Instant): Boolean = BerlinTime.date(a) == BerlinTime.date(b)
}

/** Phase des Tages. Sie bestimmt, was die Jetzt-Karte zeigt. */
enum class Tagesphase {
    /** bis 10:30 */
    Morgen,

    /** 10:30–14:00 */
    Mittag,

    /** 14:00–16:30 */
    Nachmittag,

    /** 16:30–19:30 */
    Abend,

    /** ab 19:30 */
    Nacht,
    ;

    /** Überschrift der Jetzt-Karte. */
    val heading: UiText
        get() = when (this) {
            Morgen -> UiText(R.string.heute_ueberschrift_heute)
            Mittag, Abend, Nacht -> UiText(R.string.heute_ueberschrift_jetzt)
            Nachmittag -> UiText(R.string.heute_ueberschrift_gleich)
        }

    companion object {
        /** Minuten seit Mitternacht, ab denen die nächste Phase gilt. */
        val Borders = listOf(10 * 60 + 30, 14 * 60, 16 * 60 + 30, 19 * 60 + 30)

        fun of(minutes: Int): Tagesphase = when {
            minutes < Borders[0] -> Morgen
            minutes < Borders[1] -> Mittag
            minutes < Borders[2] -> Nachmittag
            minutes < Borders[3] -> Abend
            else -> Nacht
        }

        fun of(instant: Instant): Tagesphase = of(BerlinTime.minutesOfDay(instant))

        /** Nächster Zeitpunkt, an dem sich Phase oder Begrüßung ändern: die Grenzen, 11, 14, 18 Uhr, Mitternacht. */
        fun nextChange(now: Instant): Instant {
            val fixed = (Borders + Begruessung.Borders).sorted()
            val minutes = BerlinTime.minutesOfDay(now)
            val today = BerlinTime.date(now)
            return fixed.firstOrNull { it > minutes }?.let { BerlinTime.at(today, it) }
                ?: BerlinTime.startOfDay(today.plusDays(1))
        }
    }
}

/** Begrüßung nach Uhrzeit: bis 11 Uhr „Guten Morgen“, bis 14 „Mahlzeit!“, bis 18 „Hallo!“, danach „Guten Abend“. */
enum class Begruessung(val text: UiText) {
    GutenMorgen(UiText(R.string.begruessung_morgen)),
    Mahlzeit(UiText(R.string.begruessung_mahlzeit)),
    Hallo(UiText(R.string.begruessung_hallo)),
    GutenAbend(UiText(R.string.begruessung_abend)),
    ;

    companion object {
        val Borders = listOf(11 * 60, 14 * 60, 18 * 60)

        fun of(instant: Instant): Begruessung {
            val minutes = BerlinTime.minutesOfDay(instant)
            return when {
                minutes < Borders[0] -> GutenMorgen
                minutes < Borders[1] -> Mahlzeit
                minutes < Borders[2] -> Hallo
                else -> GutenAbend
            }
        }
    }
}

/**
 * Eine Veranstaltung, so wie die Heute-Seite sie braucht. Die Events-Domäne (M5) liefert sie über
 * [HeuteEventsSource]; bis dahin ist die Quelle leer.
 */
data class HeuteEvent(
    val id: String,
    val title: String,
    val start: Instant,
    val end: Instant?,
    val allDay: Boolean,
    val cancelled: Boolean,
    val location: String?,
    val organizer: String?,
)

/** Redaktioneller Hinweis für die Startseite, mit Zeitraum `[von, bis)` und Priorität. */
data class Hinweis(
    val id: String,
    val category: String,
    val timeLabel: String? = null,
    val title: String,
    val text: String,
    val tone: Tone = Tone.Blue,
    val from: Instant? = null,
    val until: Instant? = null,
    /** Höhere Zahl zuerst. */
    val priority: Int = 0,
) {
    enum class Tone { Blue, Green }

    fun appliesAt(moment: Instant): Boolean {
        if (from != null && moment < from) return false
        if (until != null && moment >= until) return false
        return true
    }
}

/** Wohin ein Tipp auf Karte, Link oder Termin führt. Die Oberfläche übersetzt das in Navigation. */
sealed interface HeuteZiel {
    data class Speiseplan(val venue: MensaVenue) : HeuteZiel
    data class Oeffnungszeiten(val venue: MensaVenue) : HeuteZiel

    /** `day` ist `yyyy-MM-dd`, `null` heißt heute (die IDs der API gelten nur einen Tag). */
    data class Gericht(val id: String, val day: String?) : HeuteZiel
    data class Event(val id: String) : HeuteZiel
    data object AlleEvents : HeuteZiel
}

/** Eine Zeile der Jetzt-Karte: Gericht mit Chips und Preis, oder „Danach: …“ mit Zeit. */
data class HeuteZeile(
    val id: String,
    val name: UiText,
    val chips: List<MealChip>,
    /** Preis bzw. Zeit rechts. */
    val right: String?,
    val ziel: HeuteZiel? = null,
)

/** Fertige Anzeige der Jetzt-Karte. */
data class HeuteJetztKarte(
    val heading: UiText,
    val kicker: UiText?,
    val title: UiText,
    val subtitle: UiText?,
    val rows: List<HeuteZeile>,
    val linkText: UiText,
    val ziel: HeuteZiel,
    /** „Mensa ändern“ neben der Überschrift. */
    val changeVenueVisible: Boolean,
    val style: Style,
) {
    enum class Style {
        /** Kopf Blau mit grünem Band. */
        Mensa,

        /** Kopf Nachtblau mit Sonnen-Ausschnitt. */
        Event,
    }
}

/** Farbpunkt eines Termins in „Als Nächstes“. */
enum class HeuteFarbe { BrandOrange, Blue }

/** Ein Eintrag von „Als Nächstes“. */
data class HeuteTermin(
    val id: String,
    /** „11:00“ oder Wochentag „Fr“. */
    val time: String,
    val color: HeuteFarbe,
    val title: UiText,
    val subtitle: UiText,
    val ziel: HeuteZiel,
)
