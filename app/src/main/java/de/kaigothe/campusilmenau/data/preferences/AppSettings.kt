package de.kaigothe.campusilmenau.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.kaigothe.campusilmenau.domain.model.Darstellung
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Einstellungen der App (Pendant zu `@AppStorage`/`UserDefaults`). App und Widgets lesen denselben
 * DataStore. Hier liegt nur, was nicht veraltet: keine Kontodaten, kein Token.
 * Weitere Einstellungen (Preisgruppe, Mensa auf Heute, Mensa-Filter) kommen mit den Meilensteinen M3 und M4.
 */
@Singleton
class AppSettings @Inject constructor(private val dataStore: DataStore<Preferences>) {

    /** Darstellung der App. Standard: dem System folgen. */
    val darstellung: Flow<Darstellung> =
        dataStore.data.map { Darstellung.fromKey(it[Keys.DARSTELLUNG]) }

    suspend fun setDarstellung(darstellung: Darstellung) {
        dataStore.edit { it[Keys.DARSTELLUNG] = darstellung.key }
    }

    private object Keys {
        /** Gleicher Schlüssel wie in iOS. */
        val DARSTELLUNG = stringPreferencesKey("darstellung")
    }
}
