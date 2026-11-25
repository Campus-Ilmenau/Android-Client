package de.kaigothe.campusilmenau.domain.model.thoska

import de.kaigothe.campusilmenau.core.time.BerlinTime
import java.time.DayOfWeek
import java.time.Instant
import java.time.temporal.TemporalAdjusters

enum class ThoskaHistoryRange {
    TwoWeeks,
    ThreeMonths,
    ;

    /** Beginn des Zeitraums: 14 Tage bzw. 3 Kalendermonate vor [now] (Berliner Zeit). */
    fun startDate(now: Instant): Instant {
        val zoned = now.atZone(BerlinTime.zone)
        return when (this) {
            TwoWeeks -> zoned.minusDays(14)
            ThreeMonths -> zoned.minusMonths(3)
        }.toInstant()
    }
}

data class ThoskaHistoryEntry(
    val id: Int,
    val date: Instant,
    val balanceMilli: Long,
    val lastTransactionMilli: Long,
    /** Das Guthaben ist gegenüber der vorherigen Auslesung gestiegen: Die Karte wurde aufgeladen. */
    val isTopUp: Boolean,
)

data class ThoskaChartBar(val id: Int, val date: Instant, val balanceMilli: Long, val isTopUp: Boolean)

/** Auswertung der Auslesungen einer Karte für einen Zeitraum. */
class ThoskaHistorySummary(readings: List<ThoskaReading>, val range: ThoskaHistoryRange, now: Instant) {
    /** Auslesungen im Zeitraum, älteste zuerst. */
    val entries: List<ThoskaHistoryEntry>

    /** Ausgaben aus den Guthaben-Unterschieden aufeinanderfolgender Auslesungen im Zeitraum, ohne Aufladungen (Tausendstel). */
    val spentMilli: Long

    /** Ein Balken pro Auslesung, bei 3 Monaten einer pro Woche (mit dem letzten Stand der Woche). */
    val bars: List<ThoskaChartBar>

    init {
        val sorted = readings.sortedBy { it.date }
        val start = range.startDate(now)

        // Aufladung gegenüber dem Vorgänger, auch wenn dieser vor dem Zeitraum liegt.
        val all = sorted.mapIndexed { index, reading ->
            val isTopUp = index > 0 && reading.balanceMilli > sorted[index - 1].balanceMilli + TOLERANCE_MILLI
            ThoskaHistoryEntry(index, reading.date, reading.balanceMilli, reading.lastTransactionMilli, isTopUp)
        }
        entries = all.filter { it.date >= start && it.date <= now }

        spentMilli = entries.zipWithNext().sumOf { (previous, current) ->
            if (current.balanceMilli < previous.balanceMilli) previous.balanceMilli - current.balanceMilli else 0L
        }

        bars = when (range) {
            ThoskaHistoryRange.TwoWeeks -> entries.map { ThoskaChartBar(it.id, it.date, it.balanceMilli, it.isTopUp) }
            ThoskaHistoryRange.ThreeMonths -> {
                val byWeek = entries.groupBy { weekStart(it.date) }
                byWeek.keys.sorted().mapIndexed { offset, weekStart ->
                    val week = byWeek.getValue(weekStart)
                    val last = week.maxBy { it.date }
                    ThoskaChartBar(offset, weekStart, last.balanceMilli, week.any { it.isTopUp })
                }
            }
        }
    }

    private companion object {
        /** Rundungsrauschen der Kartenwerte soll keine Aufladung ergeben (0,005 € = 5 Tausendstel). */
        const val TOLERANCE_MILLI = 5L

        /** Beginn der Berliner Kalenderwoche (Montag, 00:00). */
        fun weekStart(date: Instant): Instant {
            val monday = BerlinTime.date(date).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return BerlinTime.startOfDay(monday)
        }
    }
}
