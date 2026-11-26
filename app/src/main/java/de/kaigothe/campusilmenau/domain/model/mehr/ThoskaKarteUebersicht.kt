package de.kaigothe.campusilmenau.domain.model.mehr

import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaBalance
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaCardNaming
import java.time.Instant

/** Was die App von einer Thoska-Karte gespeichert hat, für „Gespeicherte Daten“. */
data class ThoskaKarteUebersicht(
    val cardId: String,
    val name: UiText,
    val anzahl: Int,
    val erste: Instant,
    val letzte: Instant,
    /** Guthaben der letzten Auslesung in Tausendstel Euro. */
    val letzterStandMilli: Long,
) {
    companion object {
        /** Die Auslesungen je Karte, die zuletzt benutzte zuerst. */
        fun of(balances: List<ThoskaBalance>, names: Map<String, String>): List<ThoskaKarteUebersicht> =
            balances.groupBy { it.cardId }
                .map { (id, entries) ->
                    val sorted = entries.sortedBy { it.scannedAt }
                    ThoskaKarteUebersicht(
                        cardId = id,
                        name = ThoskaCardNaming.customName(id, names)?.let(UiText::plain) ?: ThoskaCardNaming.fallbackName(id),
                        anzahl = sorted.size,
                        erste = sorted.first().scannedAt,
                        letzte = sorted.last().scannedAt,
                        letzterStandMilli = sorted.last().balanceMilli,
                    )
                }
                .sortedByDescending { it.letzte }
    }
}
