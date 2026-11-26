package de.kaigothe.campusilmenau.domain.model.library

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import java.time.Instant

/** Die fünf laut PAIA-Spezifikation zulässigen Status-Werte eines Items. Unbekannte Werte gelten als „Kein Status“. */
enum class PaiaItemStatus(val code: Int, val label: UiText) {
    None(0, UiText(R.string.bib_status_kein)),
    Reserved(1, UiText(R.string.bib_status_vorgemerkt)),
    Ordered(2, UiText(R.string.bib_status_bestellt)),
    Held(3, UiText(R.string.bib_status_ausgeliehen)),
    Provided(4, UiText(R.string.bib_status_abholbereit)),
    Rejected(5, UiText(R.string.bib_status_abgelehnt)),
    ;

    companion object {
        fun fromCode(code: Int?): PaiaItemStatus = entries.firstOrNull { it.code == code } ?: None
    }
}

/**
 * Ein ausgeliehenes, vorgemerktes oder bestelltes Medium aus dem PAIA-Nutzerkonto
 * (https://gbv.github.io/paia/paia.html#items).
 */
data class PaiaLoanItem(
    /** URI des Exemplars. Ohne sie lässt sich das Medium nicht verlängern. */
    val item: String?,
    val edition: String?,
    val about: String?,
    val label: String?,
    val queue: Int?,
    val renewals: Int?,
    val reminder: Int?,
    val start: Instant?,
    val end: Instant?,
    val canCancel: Boolean?,
    val canRenew: Boolean?,
    val storage: String?,
    val storageId: String?,
    val comment: String?,
    val status: PaiaItemStatus,
) {
    /** Stabile Kennung für Listen: URI des Exemplars, sonst `about`, sonst ein Wert aus Signatur und Fälligkeit. */
    val id: String get() = item ?: about ?: "${label.orEmpty()}|${end?.toEpochMilli() ?: 0}|${status.code}"

    /** Laufende Ausleihe (im Gegensatz zu Vormerkung, Bestellung usw.). */
    val isLoan: Boolean get() = status == PaiaItemStatus.Held

    /** Nur ein ausdrückliches `false` des Servers sperrt das Verlängern. */
    val canBeRenewed: Boolean get() = canRenew != false

    val displayTitle: String? get() = about ?: label

    /** Die Signatur steht unter dem Titel, solange sie nicht selbst der Titel ist. */
    val signature: String? get() = if (about != null) label else null

    /** Eine Kopie mit neuer Fälligkeit nach einer erfolgreichen Verlängerung. */
    fun renewed(newEnd: Instant?, newStatus: PaiaItemStatus?): PaiaLoanItem =
        copy(renewals = (renewals ?: 0) + 1, end = newEnd ?: end, status = newStatus ?: status)

    /**
     * PPN des Titels. PAIA nennt sie in der Adresse der Ausgabe (`edition`, zum Beispiel `…:ppn:1009895648`), ersatzweise
     * steht sie manchmal in der Adresse des Exemplars (`item`).
     */
    val catalogPpn: String?
        get() = listOf(edition, item).firstNotNullOfOrNull { ppnIn(it) }

    private companion object {
        /** „…:ppn:123456789“, „…/ppn/123456789“ oder „…?PPN=123456789“. */
        val PpnPattern = Regex("ppn[:/=]([0-9]{6,12}[0-9Xx]?)", RegexOption.IGNORE_CASE)

        fun ppnIn(uri: String?): String? = uri?.takeIf { it.isNotEmpty() }?.let { PpnPattern.find(it)?.groupValues?.get(1)?.uppercase() }
    }
}

/** Eine Benachrichtigung aus dem PAIA-Nutzerkonto (https://gbv.github.io/paia/paia.html#notifications). */
data class PaiaNotification(
    val id: String,
    val about: String,
    val date: Instant,
    val item: String?,
    val url: String?,
)

/**
 * Nutzerkonto aus PAIA `GET /core/{patron}` für den Bibliotheksausweis. Absichtlich nur die Felder, die der Ausweis
 * braucht: `email` und `address` werden gar nicht erst gelesen. Das Modell lebt nur im Speicher.
 */
data class PaiaPatron(
    val name: String? = null,
    val expires: Instant? = null,
    val status: Int? = null,
    /** Nutzergruppen als URIs; die Anzeige geht über [PaiaUserGroup]. */
    val types: List<String>? = null,
    val note: String? = null,
) {
    val userGroup: UiText? get() = PaiaUserGroup.text(types.orEmpty())
}

/** Feste Tabelle: `type`-URI aus PAIA → Text für den Chip auf dem Ausweis. */
object PaiaUserGroup {
    /**
     * Die echten URIs der UB Ilmenau sind noch nicht bekannt (auch in der iOS-App ist die Tabelle leer). Bei der UB erfragen
     * oder aus einer `/core/{patron}`-Antwort ablesen und hier eintragen. Bis dahin bleibt der Chip weg.
     */
    val table: Map<String, UiText> = emptyMap()

    /** Text der ersten bekannten URI. Unbekannte URIs ergeben `null`, die URI selbst wird nie gezeigt. */
    fun text(uris: List<String>, table: Map<String, UiText> = PaiaUserGroup.table): UiText? =
        uris.firstNotNullOfOrNull { table[it] }
}

/** Warum eine PAIA-Anfrage scheiterte. Die Oberfläche wählt den Text. */
sealed class PaiaError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    /** Die Antwort war nicht lesbar. */
    class InvalidResponse(cause: Throwable? = null) : PaiaError(cause = cause)

    class Http(val status: Int) : PaiaError("HTTP $status")

    /** Der Server lehnt Benutzernummer oder Passwort ab; [serverMessage] ist seine Meldung (`error_description` oder `error`). */
    class AuthenticationFailed(val serverMessage: String) : PaiaError(serverMessage)

    class NotAuthenticated : PaiaError()

    /** Die Sitzung ist abgelaufen (lokal oder per 401/403). Das Login-Sheet übernimmt, es ist keine normale Fehlermeldung. */
    class SessionExpired : PaiaError()

    class RenewalFailed(val serverMessage: String) : PaiaError(serverMessage)

    /** Das Medium hat keine URI und lässt sich nicht verlängern. */
    class MissingItemIdentifier : PaiaError()

    /** Kein Netz, Zeitüberschreitung. */
    class Network(cause: Throwable) : PaiaError(cause.message, cause)
}
