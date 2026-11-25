package de.kaigothe.campusilmenau.ui.thoska

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.data.nfc.NfcAvailability
import de.kaigothe.campusilmenau.data.nfc.NfcState
import de.kaigothe.campusilmenau.data.nfc.ThoskaReader
import de.kaigothe.campusilmenau.data.repository.ThoskaRepository
import de.kaigothe.campusilmenau.data.repository.ThoskaRequests
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaBalance
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaCardNaming
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

data class ThoskaUiState(
    val nfc: NfcState,
    /** Die zuletzt ausgelesene Auslesung (aller Karten), aus dem Verlauf. `null`, solange noch nie ausgelesen wurde. */
    val balance: ThoskaBalance? = null,
    /** Der vom Nutzer vergebene Name der Karte der letzten Auslesung. */
    val cardName: String? = null,
    val isScanning: Boolean = false,
    /** „Guthaben aktualisiert“ bleibt nach dem ersten erfolgreichen Auslesen sichtbar, bis die Ansicht verlassen wird. */
    val showsConfirmation: Boolean = false,
    val error: ThoskaError? = null,
    val now: Instant,
)

/** Zustand der Ansicht „Thoska“: NFC-Zustand, letzte Auslesung, Auslesen, Umbenennen. */
@HiltViewModel
class ThoskaViewModel @Inject constructor(
    private val repository: ThoskaRepository,
    private val reader: ThoskaReader,
    private val availability: NfcAvailability,
    private val requests: ThoskaRequests,
    private val clock: Clock,
) : ViewModel() {

    private data class Local(
        val nfc: NfcState,
        val isScanning: Boolean = false,
        val showsConfirmation: Boolean = false,
        val error: ThoskaError? = null,
    )

    private val local = MutableStateFlow(Local(availability.state()))
    private var scanJob: Job? = null

    /** Ein Link wünscht das Auslesen. Die Ansicht startet es, sobald sie sichtbar ist, und meldet es dann mit [consumeScanRequest]. */
    val scanRequested: StateFlow<Boolean> = requests.scanRequested

    val state: StateFlow<ThoskaUiState> = combine(repository.balances, repository.names, local) { balances, names, local ->
        val latest = balances.maxByOrNull { it.scannedAt }
        ThoskaUiState(
            nfc = local.nfc,
            balance = latest,
            cardName = latest?.let { ThoskaCardNaming.customName(it.cardId, names) },
            isScanning = local.isScanning,
            showsConfirmation = local.showsConfirmation,
            error = local.error,
            now = clock.instant(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ThoskaUiState(nfc = local.value.nfc, now = clock.instant()))

    /** Liest den NFC-Zustand neu, zum Beispiel nach der Rückkehr aus den Einstellungen. */
    fun refreshNfc() {
        local.update { it.copy(nfc = availability.state()) }
    }

    fun consumeScanRequest() = requests.consume()

    /** Wartet auf die Karte und liest sie. Ein Fehler steht im Zustand, ein laufendes Warten bleibt bestehen. */
    fun scan(activity: Activity) {
        if (local.value.isScanning) return
        val nfc = availability.state()
        local.update { it.copy(nfc = nfc, error = null) }
        when (nfc) {
            NfcState.Missing -> return fail(ThoskaError.NoNfc())
            NfcState.Off -> return fail(ThoskaError.NfcOff())
            NfcState.On -> Unit
        }
        scanJob = viewModelScope.launch {
            local.update { it.copy(isScanning = true) }
            try {
                val balance = reader.scan(activity)
                // Jedes erfolgreiche Auslesen landet im Verlauf.
                repository.save(balance)
                local.update { it.copy(showsConfirmation = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ThoskaError) {
                fail(e)
            } finally {
                local.update { it.copy(isScanning = false) }
            }
        }
    }

    /** Beendet das Warten (die Ansicht ist nicht mehr sichtbar). Kein Fehler. */
    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
    }

    fun rename(cardId: String, name: String) {
        viewModelScope.launch { repository.rename(cardId, name) }
    }

    private fun fail(error: ThoskaError) = local.update { it.copy(error = error) }
}
