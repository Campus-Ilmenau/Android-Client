package de.kaigothe.campusilmenau.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Ein Wert, der sofort reagiert und im Hintergrund gespeichert wird (zum Beispiel die gewählte Mensa).
 *
 * [set] ändert den Zustand sofort, die Oberfläche wartet nicht auf das Speichern. Die Einstellung selbst kommt als
 * [source] zurück. Würde jedes Echo des Speicherns den Zustand überschreiben, spränge er bei schnellen Änderungen
 * kurz zurück (A → B → C zeigt dann zwischendurch wieder B). Darum werden Meldungen der Quelle ignoriert, solange
 * noch gespeichert wird. Eine Änderung von außen (zum Beispiel ein Deeplink) kommt an, sobald nichts mehr aussteht.
 *
 * [state] ist `null`, bis die Quelle das erste Mal geliefert hat.
 */
class WriteThroughState<T : Any>(
    private val scope: CoroutineScope,
    source: Flow<T>,
    private val write: suspend (T) -> Unit,
) {
    private val _state = MutableStateFlow<T?>(null)
    val state: StateFlow<T?> = _state.asStateFlow()

    val value: T? get() = _state.value

    // Nur im Haupt-Thread des Scopes verändert (viewModelScope).
    private var pendingWrites = 0

    init {
        source.onEach { fromSource -> if (pendingWrites == 0) _state.value = fromSource }.launchIn(scope)
    }

    fun set(value: T) {
        _state.value = value
        pendingWrites++
        scope.launch {
            try {
                write(value)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Speichern gescheitert: Der Wert gilt bis zum Ende der Sitzung, die App läuft weiter.
            } finally {
                pendingWrites--
            }
        }
    }
}
