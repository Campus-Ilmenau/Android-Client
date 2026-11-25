package de.kaigothe.campusilmenau.domain.model.thoska

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import java.util.Locale

/** Namen für Karten: Der Name hängt an der Karten-ID und bleibt nur auf dem Gerät. */
object ThoskaCardNaming {

    /**
     * Der vergebene Name. Ein leerer Name oder ein Name, der gleich der Karten-ID ist (früher wurde die ID als
     * Platzhalter gespeichert), gilt als „ohne Namen“.
     */
    fun customName(cardId: String, names: Map<String, String>): String? {
        val stored = names[cardId]?.trim().orEmpty()
        return stored.takeIf { it.isNotEmpty() && it != cardId }
    }

    /** „Karte …C3D4“: die letzten vier Zeichen der ID, groß geschrieben. */
    fun fallbackName(cardId: String): UiText = UiText(R.string.thoska_karte_fallback, cardId.takeLast(4).uppercase(Locale.ROOT))

    /** Der gespeicherte Name nach dem Umbenennen: getrimmt, leer heißt „zurücksetzen“ (`null`). */
    fun normalized(input: String): String? = input.trim().takeIf { it.isNotEmpty() }
}
