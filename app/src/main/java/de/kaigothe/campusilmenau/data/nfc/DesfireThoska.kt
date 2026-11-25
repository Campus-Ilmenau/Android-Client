package de.kaigothe.campusilmenau.data.nfc

import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaBalance
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaError
import java.time.Instant

/** Der Weg zur Karte: Ein Befehl hinaus, die Antwort (mit Statusbytes) zurück. Die NFC-Schicht und Tests setzen sie ein. */
interface CardTransport {
    /** Die UID der Karte. */
    val uid: ByteArray

    /** Sendet die Bytes und liefert die Antwort einschließlich der zwei Statusbytes. Wirft bei Verbindungsverlust. */
    fun transceive(command: ByteArray): ByteArray
}

/**
 * Das Auslesen des Guthabens einer Thoska-/Mensa-Chipkarte (MIFARE DESFire). Die Karte wird über das native
 * DESFire-Kommandoset angesprochen, das als Pseudo-APDU verpackt wird (CLA `0x90`). App-ID und Datei-ID sind feste,
 * kartenseitig vorgegebene Werte der Studierendenwerke, nötig für die Interoperabilität mit der Kartenhardware.
 * Die Befehle sind unverändert die der iOS-App.
 */
object DesfireThoska {
    /** App-ID der Thoska-Anwendung auf der Karte. */
    val ApplicationId: ByteArray = byteArrayOf(0x5F, 0x84.toByte(), 0x15)

    /** Datei mit dem Guthaben (Wertdatei). */
    const val BalanceFileId: Byte = 0x01

    private const val CLA: Byte = 0x90.toByte()
    private const val SELECT_APPLICATION: Byte = 0x5A
    private const val GET_VALUE: Byte = 0x6C
    private const val GET_FILE_SETTINGS: Byte = 0xF5.toByte()

    /** Verpackt ein natives DESFire-Kommando als Pseudo-APDU: `90 <cmd> 00 00 <Lc> <Parameter> 00`. */
    fun command(command: Byte, parameter: ByteArray): ByteArray =
        byteArrayOf(CLA, command, 0x00, 0x00, parameter.size.toByte()) + parameter + byteArrayOf(0x00)

    fun selectApplication(): ByteArray = command(SELECT_APPLICATION, ApplicationId)
    fun getValue(): ByteArray = command(GET_VALUE, byteArrayOf(BalanceFileId))
    fun getFileSettings(): ByteArray = command(GET_FILE_SETTINGS, byteArrayOf(BalanceFileId))

    /**
     * Die Value-Antwort besteht aus einem Little-Endian-Int32 (in Tausendstel der Währung) und zwei Statusbytes.
     * Der Wert ist vorzeichenbehaftet, wie die Wertdatei der Karte.
     */
    fun parseBalance(response: ByteArray): Long {
        if (response.size < 4) throw ThoskaError.CardRefused("Antwort zu kurz: ${response.size} Bytes")
        val value = (response[0].toInt() and 0xFF) or
            ((response[1].toInt() and 0xFF) shl 8) or
            ((response[2].toInt() and 0xFF) shl 16) or
            ((response[3].toInt() and 0xFF) shl 24)
        return value.toLong()
    }

    /** Der zuletzt gebuchte Betrag steht an einem festen Offset (Byte 12 und 13, Little-Endian) der File-Settings-Antwort. */
    fun parseLastTransaction(response: ByteArray): Long {
        if (response.size <= 13) return 0
        return ((response[13].toInt() and 0xFF) shl 8 or (response[12].toInt() and 0xFF)).toLong()
    }

    /** Karten-ID als Hex der UID, klein geschrieben (wie in der iOS-App). */
    fun cardId(uid: ByteArray): String = uid.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    /**
     * Prüft die Statusbytes am Ende der Antwort. Ein klarer Fehler der Karte (`91 xx` mit `xx` ≠ `00`, oder ein
     * ISO-Fehlerstatus `6x xx`) bricht ab. Alles andere gilt als Erfolg, so wie die iOS-App die Antworten behandelt.
     */
    fun checkStatus(response: ByteArray) {
        if (response.size < 2) return
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        val failed = (sw1 == 0x91 && sw2 != 0x00) || sw1 in 0x67..0x6F
        if (failed) throw ThoskaError.CardRefused("Status %02X %02X".format(sw1, sw2))
    }

    /** Liest Guthaben und letzte Buchung: Anwendung wählen, Wert lesen, Dateieinstellungen lesen. */
    fun read(transport: CardTransport, now: Instant): ThoskaBalance {
        val cardId = cardId(transport.uid)
        checkStatus(transport.transceive(selectApplication()))
        val value = transport.transceive(getValue()).also(::checkStatus)
        val settings = transport.transceive(getFileSettings()).also(::checkStatus)
        return ThoskaBalance(
            cardId = cardId,
            balanceMilli = parseBalance(value),
            lastTransactionMilli = parseLastTransaction(settings),
            scannedAt = now,
        )
    }
}
