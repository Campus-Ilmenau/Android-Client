package de.kaigothe.campusilmenau.domain.model.library.merkliste

/** Auswahl in der Merkliste per Pfeiltasten (Pendant zu `MerklistePad` der iOS-App). */
object MerklisteSelection {
    /**
     * Der Titel, der nach [step] Schritten auf [current] folgt (`1` nach unten, `-1` nach oben). Ohne Auswahl gibt es den
     * ersten Titel (nach unten) bzw. den letzten (nach oben). Am Rand bleibt die Auswahl stehen. `null`, wenn die Liste leer
     * ist: Die Taste wird dann nicht verbraucht.
     */
    fun move(ids: List<String>, current: String?, step: Int): String? {
        if (ids.isEmpty()) return null
        val index = ids.indexOf(current)
        if (index < 0) return if (step >= 0) ids.first() else ids.last()
        return ids[(index + step).coerceIn(0, ids.lastIndex)]
    }
}
