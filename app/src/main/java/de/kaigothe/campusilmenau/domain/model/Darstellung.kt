package de.kaigothe.campusilmenau.domain.model

/**
 * Darstellung der ganzen App: dem System folgen, immer hell oder immer dunkel (iOS: `AppDarstellung`).
 * Die Schlüssel werden gespeichert, nicht umbenennen.
 */
enum class Darstellung(val key: String) {
    System("system"),
    Hell("hell"),
    Dunkel("dunkel");

    companion object {
        /** Unbekannte oder fehlende Werte gelten als [System]. */
        fun fromKey(key: String?): Darstellung = entries.firstOrNull { it.key == key } ?: System
    }
}
