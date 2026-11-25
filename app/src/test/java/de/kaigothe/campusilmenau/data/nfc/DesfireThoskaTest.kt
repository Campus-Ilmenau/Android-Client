package de.kaigothe.campusilmenau.data.nfc

import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaError
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.time.Instant

class DesfireThoskaTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    /** Eine Karte, die auf die drei Befehle der Thoska-Auslesung antwortet. */
    private class FakeCard(
        override val uid: ByteArray = byteArrayOf(0x04, 0xA1.toByte(), 0xB2.toByte(), 0xC3.toByte(), 0xD4.toByte()),
        val onCommand: (ByteArray) -> ByteArray,
    ) : CardTransport {
        val sent = mutableListOf<ByteArray>()
        override fun transceive(command: ByteArray): ByteArray {
            sent += command
            return onCommand(command)
        }
    }

    private val ok = byteArrayOf(0x91.toByte(), 0x00)

    @Test
    fun `die Befehle sind unveraendert die der iOS-App`() {
        // Select Application 5F 84 15
        assertArrayEquals(bytes(0x90, 0x5A, 0x00, 0x00, 0x03, 0x5F, 0x84, 0x15, 0x00), DesfireThoska.selectApplication())
        // Get Value, Datei 01
        assertArrayEquals(bytes(0x90, 0x6C, 0x00, 0x00, 0x01, 0x01, 0x00), DesfireThoska.getValue())
        // Get File Settings, Datei 01
        assertArrayEquals(bytes(0x90, 0xF5, 0x00, 0x00, 0x01, 0x01, 0x00), DesfireThoska.getFileSettings())
    }

    @Test
    fun `das Guthaben ist ein Little-Endian-Int32 in Tausendstel`() {
        // 12,400 € = 12400 = 0x00003070
        assertEquals(12_400L, DesfireThoska.parseBalance(bytes(0x70, 0x30, 0x00, 0x00, 0x91, 0x00)))
        assertEquals(0L, DesfireThoska.parseBalance(bytes(0, 0, 0, 0, 0x91, 0x00)))
        // 0x01020304 – jedes Byte an seiner Stelle
        assertEquals(0x01020304L, DesfireThoska.parseBalance(bytes(0x04, 0x03, 0x02, 0x01, 0x91, 0x00)))
        // Die Wertdatei ist vorzeichenbehaftet.
        assertEquals(-1L, DesfireThoska.parseBalance(bytes(0xFF, 0xFF, 0xFF, 0xFF, 0x91, 0x00)))
    }

    @Test
    fun `eine zu kurze Antwort ist ein Fehler`() {
        try {
            DesfireThoska.parseBalance(bytes(0x01, 0x02))
            fail("Erwartet: CardRefused")
        } catch (_: ThoskaError.CardRefused) {
            // erwartet
        }
    }

    @Test
    fun `die letzte Buchung steht an Byte 12 und 13`() {
        val settings = ByteArray(18).also {
            it[12] = 0x22 // 0x0122 = 290 → 0,290 €
            it[13] = 0x01
        }
        assertEquals(290L, DesfireThoska.parseLastTransaction(settings))
        // Zu kurz: keine Angabe.
        assertEquals(0L, DesfireThoska.parseLastTransaction(ByteArray(13)))
        assertEquals(0L, DesfireThoska.parseLastTransaction(ByteArray(0)))
    }

    @Test
    fun `die Karten-ID ist die UID in kleinen Hex-Ziffern`() {
        assertEquals("04a1b2c3d4", DesfireThoska.cardId(bytes(0x04, 0xA1, 0xB2, 0xC3, 0xD4)))
        assertEquals("000f", DesfireThoska.cardId(bytes(0x00, 0x0F)))
    }

    @Test
    fun `Auslesen schickt die drei Befehle der Reihe nach und liest Werte`() {
        val card = FakeCard { command ->
            when (command[1].toInt() and 0xFF) {
                0x5A -> ok
                0x6C -> bytes(0x70, 0x30, 0x00, 0x00, 0x91, 0x00)
                else -> ByteArray(16).also { it[12] = 0x22; it[13] = 0x01 } + ok
            }
        }
        val now = Instant.parse("2026-11-25T10:00:00Z")

        val balance = DesfireThoska.read(card, now)

        assertEquals(listOf(0x5A, 0x6C, 0xF5), card.sent.map { it[1].toInt() and 0xFF })
        assertEquals("04a1b2c3d4", balance.cardId)
        assertEquals(12_400L, balance.balanceMilli)
        assertEquals(290L, balance.lastTransactionMilli)
        assertEquals(now, balance.scannedAt)
    }

    @Test
    fun `ein Fehlerstatus der Karte bricht das Auslesen ab`() {
        val card = FakeCard { command ->
            // Anwendung nicht gefunden: 91 A0
            if (command[1] == 0x5A.toByte()) bytes(0x91, 0xA0) else ok
        }
        try {
            DesfireThoska.read(card, Instant.EPOCH)
            fail("Erwartet: CardRefused")
        } catch (e: ThoskaError.CardRefused) {
            assertEquals("Status 91 A0", e.message)
        }
        // Nur der erste Befehl ging raus.
        assertEquals(1, card.sent.size)
    }

    @Test
    fun `ISO-Fehlerstatus und Erfolg werden unterschieden`() {
        DesfireThoska.checkStatus(bytes(0x91, 0x00))
        DesfireThoska.checkStatus(bytes(0x90, 0x00))
        DesfireThoska.checkStatus(bytes(0x01))
        for (status in listOf(bytes(0x6A, 0x82), bytes(0x69, 0x85), bytes(0x6D, 0x00), bytes(0x91, 0xAE))) {
            try {
                DesfireThoska.checkStatus(status)
                fail("Erwartet: CardRefused für ${status.joinToString()}")
            } catch (_: ThoskaError.CardRefused) {
                // erwartet
            }
        }
    }

    @Test
    fun `ein Verbindungsfehler der Karte wird nicht verschluckt`() {
        val card = FakeCard { throw IOException("Tag was lost") }
        try {
            DesfireThoska.read(card, Instant.EPOCH)
            fail("Erwartet: IOException")
        } catch (e: IOException) {
            assertEquals("Tag was lost", e.message)
        }
    }
}
