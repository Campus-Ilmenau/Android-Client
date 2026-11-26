package de.kaigothe.campusilmenau.domain.model.library

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.events.EventTimes
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Dringlichkeit einer Ausleihe. Bestimmt die Farbe von Fälligkeits-Chip und Zeitleiste. */
enum class LoanUrgency {
    /** Mehr als 5 Tage Zeit: neutral. */
    Normal,

    /** Noch höchstens 5 Tage: `warningFill`. */
    Soon,

    /** Noch höchstens 3 Tage oder überfällig: `dangerSoft`/`danger`. */
    Urgent,
}

/** Alles, was eine Ausleihkarte an Texten und Werten braucht, aus Medium und aktuellem Tag berechnet (Berliner Tage). */
class LoanInfo(item: PaiaLoanItem, today: LocalDate, private val locale: Locale = Locale.getDefault()) {
    /** Tage bis zur Fälligkeit, negativ wenn überfällig. `null`, wenn der Server keine Fälligkeit liefert. */
    val days: Int? = item.end?.let { ChronoUnit.DAYS.between(today, BerlinTime.date(it)).toInt() }
    val dueDate: LocalDate? = item.end?.let(BerlinTime::date)
    val renewals: Int = item.renewals ?: 0
    val canRenew: Boolean = item.canBeRenewed

    val isOverdue: Boolean get() = (days ?: 0) < 0

    val urgency: LoanUrgency
        get() {
            val days = days ?: return LoanUrgency.Normal
            return when {
                days <= URGENT_DAYS -> LoanUrgency.Urgent
                days <= SOON_DAYS -> LoanUrgency.Soon
                else -> LoanUrgency.Normal
            }
        }

    /** Text im Fälligkeits-Chip: „Fällig 12. Nov.“, „Fällig in 5 Tagen“, „Überfällig seit 2 Tagen“. */
    val dueText: UiText?
        get() {
            val days = days ?: return null
            val due = dueDate ?: return null
            return when {
                days < 0 -> UiText.plural(R.plurals.bib_ueberfaellig_seit, -days, -days)
                days == 0 -> UiText(R.string.bib_heute_faellig)
                days <= SOON_DAYS -> UiText.plural(R.plurals.bib_faellig_in, days, days)
                else -> UiText(R.string.bib_faellig_am, EventTimes.dayMonth(due, locale))
            }
        }

    /** Anteil der aktuellen Leihperiode, der vorbei ist. Überfällig: voll. Mindestens 4 %, damit der Strich sichtbar bleibt. */
    val progress: Double
        get() {
            val days = days ?: return 0.0
            if (days < 0) return 1.0
            val elapsed = (LOAN_PERIOD_DAYS - days).toDouble() / LOAN_PERIOD_DAYS
            return min(1.0, max(0.04, elapsed))
        }

    /** Fußzeile: „2× verlängert“ bzw. „Noch nicht verlängert“, bei Sperre „· nicht verlängerbar“. */
    val metaText: UiText
        get() {
            val renewed = if (renewals > 0) UiText(R.string.bib_verlaengert_n, renewals) else UiText(R.string.bib_noch_nicht_verlaengert)
            return if (canRenew) renewed else UiText(R.string.bib_nicht_verlaengerbar, renewed)
        }

    /** TalkBack-Text der Zeitleiste: „Leihfrist: fällig am 12. Nov., 1-mal verlängert“. */
    val timelineLabel: UiText
        get() {
            val due = when {
                isOverdue -> UiText(R.string.bib_timeline_ueberfaellig)
                dueDate != null -> UiText(R.string.bib_timeline_faellig_am, EventTimes.dayMonth(dueDate, locale))
                else -> UiText(R.string.bib_timeline_ohne)
            }
            val renewed = if (renewals > 0) UiText(R.string.bib_timeline_n_mal, renewals) else UiText(R.string.bib_timeline_noch_nicht)
            return UiText(R.string.bib_timeline, due, renewed)
        }

    companion object {
        /** Standard-Leihfrist von 3 Wochen. */
        const val LOAN_PERIOD_DAYS = 21
        const val SOON_DAYS = 5
        const val URGENT_DAYS = 3
    }
}

/** Farbe des Status-Chips einer Vormerkung. Die Oberfläche bildet sie auf die Chip-Varianten ab. */
enum class HoldTone { Green, Blue, Neutral, DangerSoft }

/** Status-Chip und Zusatzzeile einer Vormerkung, Bestellung usw. */
class HoldInfo(item: PaiaLoanItem, private val locale: Locale = Locale.getDefault()) {
    private val chip: Pair<UiText, HoldTone> = when (item.status) {
        PaiaItemStatus.Provided -> UiText(R.string.bib_status_abholbereit) to HoldTone.Green
        PaiaItemStatus.Ordered -> UiText(R.string.bib_status_bestellt) to HoldTone.Blue
        PaiaItemStatus.Reserved -> UiText(R.string.bib_status_vorgemerkt) to HoldTone.Neutral
        PaiaItemStatus.Rejected -> UiText(R.string.bib_status_abgelehnt) to HoldTone.DangerSoft
        PaiaItemStatus.None, PaiaItemStatus.Held -> UiText(R.string.bib_status_kein) to HoldTone.Neutral
    }
    val statusText: UiText get() = chip.first
    val tone: HoldTone get() = chip.second

    /** Nur, wenn der Server dazu Daten liefert. */
    val detail: List<UiText>

    init {
        // Aus den PAIA-Feldern comment, storage, queue und (bei Bereitstellung) end zusammengesetzt.
        detail = buildList {
            item.comment?.trim()?.takeIf { it.isNotEmpty() }?.let { add(UiText.plain(it)) }
            item.storage?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add(UiText(if (item.status == PaiaItemStatus.Provided) R.string.bib_abholort else R.string.bib_standort, it))
            }
            item.queue?.takeIf { it > 0 }?.let { add(UiText(R.string.bib_warteschlange, it)) }
            if (item.status == PaiaItemStatus.Provided) {
                item.end?.let { add(UiText(R.string.bib_abholbar_bis, EventTimes.dayMonth(BerlinTime.date(it), locale))) }
            }
        }
    }
}

/** Teilt die eine PAIA-Liste in die beiden Segmente. */
class LoanLists(items: List<PaiaLoanItem>) {
    /** Status „ausgeliehen“, nach Fälligkeit sortiert. Überfälliges steht zuerst, Medien ohne Fälligkeit zuletzt. */
    val loans: List<PaiaLoanItem> = items.filter { it.isLoan }.sortedWith(
        compareBy<PaiaLoanItem, java.time.Instant?>(nullsLast()) { it.end }.thenBy { it.displayTitle.orEmpty() },
    )

    /** Vorgemerkt, bestellt, bereitgestellt, abgelehnt und ohne Status. */
    val holds: List<PaiaLoanItem> = items.filter { !it.isLoan }

    /** Zeile unter dem Titel: „4 ausgeliehen · 1 überfällig · 2 bald fällig“. */
    fun summary(today: LocalDate): List<UiText> {
        if (loans.isEmpty()) return listOf(UiText(R.string.bib_nichts_ausgeliehen))
        val infos = loans.map { LoanInfo(it, today) }
        val overdue = infos.count { it.isOverdue }
        val soon = infos.count { (it.days ?: Int.MAX_VALUE) in 0..LoanInfo.SOON_DAYS }
        return buildList {
            add(UiText(R.string.bib_n_ausgeliehen, loans.size))
            if (overdue > 0) add(UiText(R.string.bib_n_ueberfaellig, overdue))
            if (soon > 0) add(UiText(R.string.bib_n_bald_faellig, soon))
        }
    }
}
