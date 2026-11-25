package de.kaigothe.campusilmenau.ui.thoska

import android.app.Activity
import de.kaigothe.campusilmenau.data.nfc.NfcAvailability
import de.kaigothe.campusilmenau.data.nfc.NfcState
import de.kaigothe.campusilmenau.data.nfc.ThoskaReader
import de.kaigothe.campusilmenau.data.repository.ThoskaRepository
import de.kaigothe.campusilmenau.data.repository.ThoskaRequests
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaBalance
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaCardNaming
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaError
import de.kaigothe.campusilmenau.testing.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ThoskaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val now = Instant.parse("2026-11-25T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val activity = mockk<Activity>()

    private class FakeRepository : ThoskaRepository {
        val stored = MutableStateFlow<List<ThoskaBalance>>(emptyList())
        val nameMap = MutableStateFlow<Map<String, String>>(emptyMap())
        override val balances = stored
        override val names = nameMap
        override suspend fun save(balance: ThoskaBalance) {
            stored.value = stored.value + balance
        }

        override suspend fun rename(cardId: String, input: String) {
            val name = ThoskaCardNaming.normalized(input)
            nameMap.value = if (name == null) nameMap.value - cardId else nameMap.value + (cardId to name)
        }
    }

    private class FakeReader(var action: suspend () -> ThoskaBalance) : ThoskaReader {
        var calls = 0
        override suspend fun scan(activity: Activity): ThoskaBalance {
            calls++
            return action()
        }
    }

    private class FakeNfc(var state: NfcState = NfcState.On) : NfcAvailability {
        override fun state() = state
    }

    private fun balance(cardId: String = "04a1b2c3d4", milli: Long = 12_400, at: Instant = now) =
        ThoskaBalance(cardId, milli, 2_900, at)

    private val repository = FakeRepository()
    private val nfc = FakeNfc()
    private val requests = ThoskaRequests()

    private fun viewModel(reader: FakeReader) = ThoskaViewModel(repository, reader, nfc, requests, clock)

    @Test
    fun `ohne Auslesung ist nichts da`() = runTest {
        val state = viewModel(FakeReader { balance() }).state.value
        assertNull(state.balance)
        assertEquals(NfcState.On, state.nfc)
        assertFalse(state.isScanning)
        assertFalse(state.showsConfirmation)
    }

    @Test
    fun `die letzte gespeicherte Auslesung und der Kartenname werden gezeigt`() = runTest {
        repository.stored.value = listOf(balance(milli = 20_000, at = now.minusSeconds(86_400)), balance(milli = 12_400))
        repository.nameMap.value = mapOf("04a1b2c3d4" to " Mensakarte ")

        val state = viewModel(FakeReader { balance() }).state.value

        assertEquals(12_400L, state.balance?.balanceMilli)
        assertEquals("Mensakarte", state.cardName)
    }

    @Test
    fun `erfolgreiches Auslesen speichert und zeigt die Bestaetigung`() = runTest {
        val reader = FakeReader { balance(milli = 9_500) }
        val viewModel = viewModel(reader)

        viewModel.scan(activity)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, reader.calls)
        assertEquals(9_500L, state.balance?.balanceMilli)
        assertEquals(1, repository.stored.value.size)
        assertTrue(state.showsConfirmation)
        assertFalse(state.isScanning)
        assertNull(state.error)
    }

    @Test
    fun `waehrend des Wartens ist es Scanning und ein zweiter Start wird ignoriert`() = runTest {
        val gate = CompletableDeferred<ThoskaBalance>()
        val reader = FakeReader { gate.await() }
        val viewModel = viewModel(reader)

        viewModel.scan(activity)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isScanning)
        viewModel.scan(activity)
        advanceUntilIdle()
        assertEquals(1, reader.calls)

        gate.complete(balance())
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isScanning)
    }

    @Test
    fun `Fehler der Karte stehen im Zustand und speichern nichts`() = runTest {
        val error = ThoskaError.UnsupportedCard()
        val viewModel = viewModel(FakeReader { throw error })

        viewModel.scan(activity)
        advanceUntilIdle()

        assertSame(error, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isScanning)
        assertFalse(viewModel.state.value.showsConfirmation)
        assertTrue(repository.stored.value.isEmpty())
    }

    @Test
    fun `ein neuer Versuch loescht den alten Fehler`() = runTest {
        val reader = FakeReader { throw ThoskaError.ConnectionLost() }
        val viewModel = viewModel(reader)
        viewModel.scan(activity)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.error is ThoskaError.ConnectionLost)

        reader.action = { balance() }
        viewModel.scan(activity)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.showsConfirmation)
    }

    @Test
    fun `ohne NFC oder bei ausgeschaltetem NFC wird die Karte gar nicht erst gelesen`() = runTest {
        val reader = FakeReader { balance() }
        val viewModel = viewModel(reader)

        nfc.state = NfcState.Off
        viewModel.scan(activity)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.error is ThoskaError.NfcOff)
        assertEquals(NfcState.Off, viewModel.state.value.nfc)

        nfc.state = NfcState.Missing
        viewModel.scan(activity)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.error is ThoskaError.NoNfc)
        assertEquals(0, reader.calls)
    }

    @Test
    fun `refreshNfc liest den Zustand nach der Rueckkehr aus den Einstellungen neu`() = runTest {
        nfc.state = NfcState.Off
        val viewModel = viewModel(FakeReader { balance() })
        assertEquals(NfcState.Off, viewModel.state.value.nfc)

        nfc.state = NfcState.On
        viewModel.refreshNfc()

        assertEquals(NfcState.On, viewModel.state.value.nfc)
    }

    @Test
    fun `ein Abbruch beendet das Warten ohne Fehler`() = runTest {
        val gate = CompletableDeferred<ThoskaBalance>()
        val viewModel = viewModel(FakeReader { gate.await() })
        viewModel.scan(activity)
        advanceUntilIdle()

        viewModel.cancelScan()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isScanning)
        assertNull(viewModel.state.value.error)
        assertTrue(repository.stored.value.isEmpty())
    }

    @Test
    fun `der Link wuenscht das Auslesen und die Ansicht uebernimmt den Wunsch`() = runTest {
        val viewModel = viewModel(FakeReader { balance() })
        assertFalse(viewModel.scanRequested.value)

        requests.requestScan()
        assertTrue(viewModel.scanRequested.value)

        viewModel.consumeScanRequest()
        assertFalse(viewModel.scanRequested.value)
    }

    @Test
    fun `Umbenennen setzt und loescht den Namen`() = runTest {
        repository.stored.value = listOf(balance())
        val viewModel = viewModel(FakeReader { balance() })

        viewModel.rename("04a1b2c3d4", "  Mensa  ")
        advanceUntilIdle()
        assertEquals("Mensa", viewModel.state.value.cardName)

        viewModel.rename("04a1b2c3d4", "   ")
        advanceUntilIdle()
        assertNull(viewModel.state.value.cardName)
    }
}
