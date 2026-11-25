package de.kaigothe.campusilmenau.data.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import dagger.hilt.android.qualifiers.ApplicationContext
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaBalance
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Was das Gerät mit NFC kann. */
enum class NfcState {
    /** Keine NFC-Hardware: Der Thoska-Tab entfällt. */
    Missing,

    /** NFC ist ausgeschaltet. */
    Off,
    On,
}

interface NfcAvailability {
    fun state(): NfcState
}

@Singleton
class SystemNfcAvailability @Inject constructor(@ApplicationContext private val context: Context) : NfcAvailability {
    override fun state(): NfcState {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcState.Missing
        return if (adapter.isEnabled) NfcState.On else NfcState.Off
    }
}

/** Liest eine Thoska-Karte. Die Implementierung bindet sich an die sichtbare Activity, ein Abbruch beendet das Warten. */
interface ThoskaReader {
    /** Wartet, bis eine Karte anliegt, und liest sie. Wirft [ThoskaError]. Ein Abbruch der Coroutine beendet den Lesemodus. */
    suspend fun scan(activity: Activity): ThoskaBalance
}

/**
 * Liest über `NfcAdapter.enableReaderMode` und `IsoDep`. Der Lesemodus gilt, solange die Activity im Vordergrund ist und
 * gewartet wird; es gibt keinen System-Dialog wie auf iOS, die Oberfläche zeigt den Hinweis „Halte deine Thoska an dein
 * Gerät“. Die Karte spricht DESFire im ISO-DEP-Rahmen (`NfcA`, Pseudo-APDU), siehe [DesfireThoska].
 */
@Singleton
class NfcThoskaReader @Inject constructor(private val clock: Clock) : ThoskaReader {

    override suspend fun scan(activity: Activity): ThoskaBalance {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: throw ThoskaError.NoNfc()
        if (!adapter.isEnabled) throw ThoskaError.NfcOff()

        try {
            return suspendCancellableCoroutine { continuation ->
                val callback = NfcAdapter.ReaderCallback { tag ->
                    // Der Rückruf kommt auf einem NFC-Thread. Nur der erste Treffer zählt.
                    if (continuation.isActive) {
                        val outcome = try {
                            Result.success(read(tag))
                        } catch (e: ThoskaError) {
                            Result.failure(e)
                        } catch (e: TagLostException) {
                            Result.failure(ThoskaError.ConnectionLost(e))
                        } catch (e: IOException) {
                            Result.failure(ThoskaError.ConnectionLost(e))
                        } catch (e: SecurityException) {
                            Result.failure(ThoskaError.ConnectionLost(e))
                        }
                        continuation.resumeWith(outcome)
                    }
                }
                adapter.enableReaderMode(
                    activity,
                    callback,
                    NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    null,
                )
            }
        } finally {
            // Auch nach einem Abbruch: Der Lesemodus darf nicht hängen bleiben.
            withContext(NonCancellable + Dispatchers.Main) {
                runCatching { adapter.disableReaderMode(activity) }
            }
        }
    }

    private fun read(tag: Tag): ThoskaBalance {
        val isoDep = IsoDep.get(tag) ?: throw ThoskaError.UnsupportedCard("Kein ISO-DEP")
        isoDep.use {
            it.timeout = TIMEOUT_MILLIS
            it.connect()
            val transport = object : CardTransport {
                override val uid: ByteArray = tag.id
                override fun transceive(command: ByteArray): ByteArray = it.transceive(command)
            }
            return DesfireThoska.read(transport, clock.instant())
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 3_000
    }
}

private inline fun <T> IsoDep.use(block: (IsoDep) -> T): T =
    try {
        block(this)
    } finally {
        try {
            close()
        } catch (_: IOException) {
            // Die Karte ist schon weg.
        }
    }
