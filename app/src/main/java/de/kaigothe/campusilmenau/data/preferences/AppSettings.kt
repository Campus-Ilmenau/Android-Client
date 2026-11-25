package de.kaigothe.campusilmenau.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import de.kaigothe.campusilmenau.domain.model.Darstellung
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFavorites
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Einstellungen der App (Pendant zu `@AppStorage`/`UserDefaults`). App und Widgets lesen denselben DataStore.
 * Hier liegt nur, was nicht veraltet: keine Kontodaten, kein Token.
 * Die Schlüssel entsprechen denen der iOS-App (siehe `docs/migration/spec/lokale-daten.md`).
 */
@Singleton
class AppSettings @Inject constructor(private val dataStore: DataStore<Preferences>) {

    /** Darstellung der App. Standard: dem System folgen. */
    val darstellung: Flow<Darstellung> =
        dataStore.data.map { Darstellung.fromKey(it[Keys.DARSTELLUNG]) }

    suspend fun setDarstellung(darstellung: Darstellung) {
        dataStore.edit { it[Keys.DARSTELLUNG] = darstellung.key }
    }

    /** Preisgruppe, deren Preis in der Mensa groß steht. Standard: Studierende. */
    val priceGroup: Flow<MensaPriceGroup> =
        dataStore.data.map { MensaPriceGroup.fromKey(it[Keys.PRICE_GROUP]) }.distinctUntilChanged()

    suspend fun setPriceGroup(group: MensaPriceGroup) {
        dataStore.edit { it[Keys.PRICE_GROUP] = group.key }
    }

    /** Die zuletzt gewählte Mensa im Mensa-Tab. Start ist die Mensa Ehrenberg. */
    val mensaVenue: Flow<MensaVenue> =
        dataStore.data.map { MensaVenue.fromKey(it[Keys.MENSA_VENUE]) ?: MensaVenue.MensaEhrenberg }.distinctUntilChanged()

    suspend fun setMensaVenue(venue: MensaVenue) {
        dataStore.edit { it[Keys.MENSA_VENUE] = venue.key }
    }

    /** Der Mensa-Filter (Ernährung, Bio, Allergene). Ein unlesbarer gespeicherter Wert gilt als „kein Filter“. */
    val mensaFilter: Flow<MensaFilter> =
        dataStore.data.map { decodeFilter(it[Keys.MENSA_FILTER]) }.distinctUntilChanged()

    suspend fun setMensaFilter(filter: MensaFilter) {
        dataStore.edit { it[Keys.MENSA_FILTER] = FilterJson.encodeToString(MensaFilter.serializer(), filter) }
    }

    /** Favoriten: Namen der Gerichte, getrimmt und klein geschrieben (siehe [MensaFavorites]). */
    val mensaFavorites: Flow<Set<String>> =
        dataStore.data.map { it[Keys.MENSA_FAVORITES].orEmpty() }.distinctUntilChanged()

    suspend fun setMensaFavorite(mealName: String, favorite: Boolean) {
        val key = MensaFavorites.key(mealName)
        dataStore.edit { preferences ->
            val current = preferences[Keys.MENSA_FAVORITES].orEmpty()
            preferences[Keys.MENSA_FAVORITES] = if (favorite) current + key else current - key
        }
    }

    /** Die Mensa auf der Startseite, unabhängig von der Auswahl im Mensa-Tab. Start ist die Mensa Ehrenberg. */
    val heuteMensa: Flow<MensaVenue> =
        dataStore.data.map { MensaVenue.fromKey(it[Keys.HEUTE_MENSA]) ?: MensaVenue.MensaEhrenberg }.distinctUntilChanged()

    suspend fun setHeuteMensa(venue: MensaVenue) {
        dataStore.edit { it[Keys.HEUTE_MENSA] = venue.key }
    }

    /** Die Hinweise, die der Nutzer auf „Heute“ weggetippt hat (dauerhaft). */
    val dismissedNotices: Flow<Set<String>> =
        dataStore.data.map { it[Keys.NOTICES_DISMISSED].orEmpty() }.distinctUntilChanged()

    suspend fun dismissNotice(id: String) {
        dataStore.edit { it[Keys.NOTICES_DISMISSED] = it[Keys.NOTICES_DISMISSED].orEmpty() + id }
    }

    /** Gemerkte Veranstaltungen (IDs), nur auf diesem Gerät. Den Filter „Gemerkt“ wendet die App auf die geladene Liste an. */
    val eventBookmarks: Flow<Set<String>> =
        dataStore.data.map { it[Keys.EVENT_BOOKMARKS].orEmpty() }.distinctUntilChanged()

    /** Merkt die Veranstaltung oder nimmt sie wieder weg. */
    suspend fun toggleEventBookmark(id: String) {
        dataStore.edit { preferences ->
            val current = preferences[Keys.EVENT_BOOKMARKS].orEmpty()
            preferences[Keys.EVENT_BOOKMARKS] = if (id in current) current - id else current + id
        }
    }

    private fun decodeFilter(text: String?): MensaFilter =
        text?.let { runCatching { FilterJson.decodeFromString(MensaFilter.serializer(), it) }.getOrNull() }
            ?: MensaFilter()

    private object Keys {
        /** Gleiche Schlüssel wie in iOS. */
        val DARSTELLUNG = stringPreferencesKey("darstellung")
        val PRICE_GROUP = stringPreferencesKey("mensaPriceGroup")
        val MENSA_VENUE = stringPreferencesKey("mensaSelectedVenue")
        val MENSA_FILTER = stringPreferencesKey("MensaFilter")
        val MENSA_FAVORITES = stringSetPreferencesKey("mensaFavoriteMeals")
        val HEUTE_MENSA = stringPreferencesKey("heuteMensa")
        val EVENT_BOOKMARKS = stringSetPreferencesKey("eventsMerkliste")
        val NOTICES_DISMISSED = stringSetPreferencesKey("heuteHinweiseAusgeblendet")
    }

    private companion object {
        /** Alle Felder schreiben (auch Standardwerte), das Format entspricht dem der iOS-App. */
        val FilterJson = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
