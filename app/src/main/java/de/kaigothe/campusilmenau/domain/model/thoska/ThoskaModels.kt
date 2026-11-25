package de.kaigothe.campusilmenau.domain.model.thoska

import java.time.Instant

/**
 * Guthaben, das von einer Thoska-Chipkarte per NFC ausgelesen wurde. Beträge stehen in Tausendstel Euro (`Long`), so wie
 * die Karte sie liefert: Es gibt kein Rundungsrauschen und der Vergleich zweier Auslesungen ist exakt.
 */
data class ThoskaBalance(
    val cardId: String,
    val balanceMilli: Long,
    val lastTransactionMilli: Long,
    val scannedAt: Instant,
)

/** Eine Auslesung, wie sie die Verlauf-Ansicht braucht. */
data class ThoskaReading(
    val date: Instant,
    val balanceMilli: Long,
    val lastTransactionMilli: Long,
)

/** Eine Karte mit Namen, wie Auswahl und Umbenennen sie brauchen. */
data class ThoskaCardOption(
    val id: String,
    /** Der vergebene Name, bei Karten ohne Namen leer (die Ansicht zeigt dann `ThoskaCardNaming.fallbackName`). */
    val displayName: String,
    /** Nur der vom Nutzer vergebene Name, sonst `null`. */
    val customName: String?,
)

/** Warum das Auslesen scheiterte. Die Oberfläche wählt den Text (und bietet bei [NfcOff] den Weg in die Einstellungen an). */
sealed class ThoskaError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    /** Das Gerät hat kein NFC. */
    class NoNfc : ThoskaError()

    /** NFC ist ausgeschaltet. */
    class NfcOff : ThoskaError()

    /** Die Karte ist keine Thoska (kein ISO-DEP) oder antwortet nicht wie eine. */
    class UnsupportedCard(message: String? = null) : ThoskaError(message)

    /** Die Verbindung zur Karte brach ab (Karte zu früh weggenommen). */
    class ConnectionLost(cause: Throwable? = null) : ThoskaError(cause = cause)

    /** Die Karte meldete einen Fehlerstatus oder die Antwort war unlesbar. */
    class CardRefused(message: String? = null) : ThoskaError(message)
}
