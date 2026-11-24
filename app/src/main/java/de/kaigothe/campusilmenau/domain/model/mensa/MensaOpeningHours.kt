package de.kaigothe.campusilmenau.domain.model.mensa

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Öffnungszeiten aus den Daten der API (`Canteen.openingHours`): Wochenzeilen, heutige Zeit und Status.
 * Alle Zeiten sind Berliner Ortszeit, `now` ist also schon in Berlin umgerechnet.
 */
object MensaOpeningHours {
    /** Montag bis Sonntag, wie die API die Wochentage nennt. Die Reihenfolge entspricht [DayOfWeek]. */
    private val Keys = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")

    /** Eine Wochenzeile: aufeinanderfolgende Tage mit gleicher Zeit. [range] ist `null`, wenn geschlossen ist. */
    data class WeekRow(val days: List<DayOfWeek>, val range: String?)

    /** Abendessen der Cafeteria Ehrenberg: Die API nennt keinen Beginn, nur das Ende der Öffnungszeit. */
    data class DinnerHours(val closes: String, val fridayCloses: String?)

    enum class State {
        Open,

        /** Schließt in weniger als 30 Minuten. */
        ClosingSoon,

        /** Heute geschlossen, vor der Öffnung oder nach dem Schließen. */
        Closed,
    }

    fun key(day: DayOfWeek): String = Keys[day.ordinal]

    private fun entry(day: DayOfWeek, hours: List<OpeningHour>): OpeningHour? =
        hours.firstOrNull { it.weekday.lowercase() == key(day) }

    /** Zeit als „11:00–14:00“ oder `null`, wenn der Tag fehlt. */
    fun range(day: DayOfWeek, hours: List<OpeningHour>): String? =
        entry(day, hours)?.let { "${it.opensAt}–${it.closesAt}" }

    /**
     * Wochenzeilen: Tage mit gleicher Zeit nebeneinander zusammengefasst, zum Beispiel „Montag – Donnerstag“ mit
     * „11:00–14:00“ und „Samstag, Sonntag“ ohne Zeit (geschlossen).
     */
    fun weekRows(hours: List<OpeningHour>): List<WeekRow> {
        val days = DayOfWeek.entries.map { it to range(it, hours) }
        val rows = mutableListOf<WeekRow>()
        var start = 0
        while (start < days.size) {
            var end = start
            while (end + 1 < days.size && days[end + 1].second == days[start].second) end++
            rows += WeekRow(days.subList(start, end + 1).map { it.first }, days[start].second)
            start = end + 1
        }
        return rows
    }

    /** „Montag“, „Samstag, Sonntag“ oder „Montag – Donnerstag“. */
    fun label(row: WeekRow, locale: Locale = Locale.getDefault()): String {
        val names = row.days.map { it.getDisplayName(TextStyle.FULL, locale) }
        return when (names.size) {
            1 -> names[0]
            2 -> "${names[0]}, ${names[1]}"
            else -> "${names.first()} – ${names.last()}"
        }
    }

    fun dinnerHours(hours: List<OpeningHour>): DinnerHours? {
        val monday = entry(DayOfWeek.MONDAY, hours) ?: return null
        val friday = entry(DayOfWeek.FRIDAY, hours)
        return DinnerHours(monday.closesAt, friday?.closesAt?.takeIf { it != monday.closesAt })
    }

    fun state(hours: List<OpeningHour>, now: LocalDateTime): State {
        val today = entry(now.dayOfWeek, hours) ?: return State.Closed
        val opens = minutes(today.opensAt) ?: return State.Closed
        val closes = minutes(today.closesAt) ?: return State.Closed
        val current = now.hour * 60 + now.minute
        if (current < opens || current >= closes) return State.Closed
        return if (closes - current < 30) State.ClosingSoon else State.Open
    }

    /** Minuten seit Mitternacht zu „11:00“ oder `null`, wenn der Text keine Uhrzeit ist. */
    fun minutes(time: String): Int? {
        val parts = time.split(':')
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    /** „11:00“ → „11“, „10:30“ → „10:30“. */
    fun short(time: String): String {
        val total = minutes(time) ?: return time
        return if (total % 60 == 0) "${total / 60}" else clock(time)
    }

    /** „08:00“ → „8:00“. */
    fun clock(time: String): String {
        val total = minutes(time) ?: return time
        return "%d:%02d".format(Locale.ROOT, total / 60, total % 60)
    }
}

/** Wie auffällig der Status-Chip einer Mensa ist. Die Oberfläche bildet es auf die Chip-Varianten ab. */
enum class StatusTone { Danger, Neutral, Warning, Green }

/**
 * Status-Chip einer Mensa aus den Öffnungszeiten der API. Die Texte baut die Oberfläche aus den String-Ressourcen,
 * hier stehen nur Fall und Zahlen.
 */
sealed interface MensaStatus {
    val tone: StatusTone

    /** „Geschlossen“. */
    data object Closed : MensaStatus {
        override val tone = StatusTone.Danger
    }

    /** „Bis 20:00 Uhr“ (Abendessen). */
    data class UntilClock(val clock: String) : MensaStatus {
        override val tone = StatusTone.Neutral
    }

    /** „11–14 Uhr“ (an einem anderen Tag). */
    data class Range(val from: String, val to: String) : MensaStatus {
        override val tone = StatusTone.Neutral
    }

    /** „Ab 8:00 Uhr“ (heute, noch nicht geöffnet). */
    data class FromClock(val clock: String) : MensaStatus {
        override val tone = StatusTone.Neutral
    }

    /** „Gleich zu“ (heute, höchstens 30 Minuten bis zum Schluss). */
    data object ClosingSoon : MensaStatus {
        override val tone = StatusTone.Warning
    }

    /** „Geöffnet · 11–14 Uhr“. */
    data class Open(val from: String, val to: String) : MensaStatus {
        override val tone = StatusTone.Green
    }

    companion object {
        /**
         * `null`, solange keine Öffnungszeiten der Mensa bekannt sind (dann zeigt die Seite keinen Chip).
         * Beachte: Hier gilt „gleich zu“ ab 30 Minuten, der Zustand in [MensaOpeningHours.state] erst darunter.
         * Das entspricht der iOS-App.
         */
        fun chip(
            venue: MensaVenue,
            canteens: List<Canteen>,
            day: LocalDate,
            mealtime: MensaMealtime,
            now: LocalDateTime,
        ): MensaStatus? {
            val canteen = canteens.firstOrNull { venue.matches(it.name) } ?: return null

            val key = MensaOpeningHours.key(day.dayOfWeek)
            val hours = canteen.openingHours.firstOrNull { it.weekday.lowercase() == key } ?: return Closed
            val opens = MensaOpeningHours.minutes(hours.opensAt) ?: return Closed
            val closes = MensaOpeningHours.minutes(hours.closesAt) ?: return Closed

            val isToday = day == now.toLocalDate()
            val nowMinutes = now.hour * 60 + now.minute

            if (isToday && nowMinutes >= closes) return Closed
            if (venue.hasMealtimes && mealtime == MensaMealtime.Dinner) {
                return UntilClock(MensaOpeningHours.clock(hours.closesAt))
            }
            if (!isToday) return Range(MensaOpeningHours.short(hours.opensAt), MensaOpeningHours.short(hours.closesAt))
            if (nowMinutes < opens) return FromClock(MensaOpeningHours.clock(hours.opensAt))
            if (closes - nowMinutes <= 30) return ClosingSoon
            return Open(MensaOpeningHours.short(hours.opensAt), MensaOpeningHours.short(hours.closesAt))
        }
    }
}
