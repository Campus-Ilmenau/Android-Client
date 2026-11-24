package de.kaigothe.campusilmenau.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * DataStore ohne Datei und ohne eigene Threads. Der echte DataStore liest und schreibt auf `Dispatchers.IO`, das der
 * Test-Scheduler nicht kennt; ViewModel-Tests wären darin nicht deterministisch. Die Datei-Variante prüft `AppSettingsTest`.
 */
class InMemoryPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
