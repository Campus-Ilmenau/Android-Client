package de.kaigothe.campusilmenau.domain.model

/**
 * Die Sprache der App. [Automatisch] folgt dem Gerät (Deutsch oder Englisch, sonst Deutsch), die anderen gelten sofort.
 * [tag] ist der BCP-47-Code für Android, `null` bei [Automatisch].
 */
enum class SpracheWahl(val tag: String?) {
    Automatisch(null),
    Deutsch("de"),
    Englisch("en"),
    ;

    companion object {
        /** Der Code einer gespeicherten Wahl. Unbekannte Werte (auch andere Regionen wie `de-AT`) gelten über die Sprache. */
        fun ofTag(tag: String?): SpracheWahl {
            val language = tag?.substringBefore('-')?.substringBefore('_')?.lowercase() ?: return Automatisch
            return entries.firstOrNull { it.tag == language } ?: Automatisch
        }

        /** Die Sprache, in der die App dem Gerät folgend erscheint: Englisch, wenn das Gerät Englisch spricht, sonst Deutsch. */
        fun fuerGeraet(deviceLanguage: String?): SpracheWahl = if (deviceLanguage?.lowercase() == "en") Englisch else Deutsch
    }
}
