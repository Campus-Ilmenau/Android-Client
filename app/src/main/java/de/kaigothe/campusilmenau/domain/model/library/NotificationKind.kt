package de.kaigothe.campusilmenau.domain.model.library

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import java.util.Locale

/**
 * Art einer Mitteilung der Bibliothek. PAIA liefert dafür kein eigenes Feld, nur den Text (`about`). Die Art wird darum
 * aus Schlüsselwörtern im Text abgeleitet; passt keines, bleibt es „Mitteilung“.
 */
enum class NotificationKind(val title: UiText) {
    Reminder(UiText(R.string.bib_art_mahnung)),
    Provided(UiText(R.string.bib_art_bereitstellung)),
    HoldExpired(UiText(R.string.bib_art_abgelaufen)),
    Other(UiText(R.string.bib_art_mitteilung)),
    ;

    companion object {
        /**
         * Die Reihenfolge zählt: „nicht abgeholt“ enthält weder „bereit“ noch „überfällig“, aber „Vormerkung“ allein sagt
         * nichts, daher zuerst die eindeutigen Formulierungen.
         */
        fun of(about: String): NotificationKind {
            val text = about.lowercase(Locale.ROOT)
            return when {
                "nicht abgeholt" in text || ("vormerkung" in text && "abgelaufen" in text) -> HoldExpired
                listOf("überfällig", "ueberfaellig", "mahnung", "mahngebühr", "erinnerung").any { it in text } -> Reminder
                listOf("bereit", "abholung", "abholen", "bereitgestellt").any { it in text } -> Provided
                else -> Other
            }
        }
    }
}

val PaiaNotification.kind: NotificationKind get() = NotificationKind.of(about)
