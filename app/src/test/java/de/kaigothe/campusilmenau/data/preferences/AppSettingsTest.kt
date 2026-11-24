package de.kaigothe.campusilmenau.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.kaigothe.campusilmenau.domain.model.Darstellung
import de.kaigothe.campusilmenau.domain.model.mensa.MensaDiet
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppSettingsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun settings(scope: TestScope) = AppSettings(
        PreferenceDataStoreFactory.create(
            scope = scope.backgroundScope,
            produceFile = { File(folder.root, "test.preferences_pb") },
        ),
    )

    @Test
    fun `Standard ist dem System folgen`() = runTest {
        assertEquals(Darstellung.System, settings(this).darstellung.first())
    }

    @Test
    fun `gewaehlte Darstellung bleibt erhalten`() = runTest {
        val settings = settings(this)
        settings.setDarstellung(Darstellung.Dunkel)
        assertEquals(Darstellung.Dunkel, settings.darstellung.first())
        settings.setDarstellung(Darstellung.Hell)
        assertEquals(Darstellung.Hell, settings.darstellung.first())
    }

    @Test
    fun `unbekannter Schluessel gilt als System`() {
        assertEquals(Darstellung.System, Darstellung.fromKey("violett"))
        assertEquals(Darstellung.System, Darstellung.fromKey(null))
    }

    @Test
    fun `Schluessel entsprechen denen der iOS-App`() {
        assertEquals(listOf("system", "hell", "dunkel"), Darstellung.entries.map { it.key })
    }

    @Test
    fun `Preisgruppe ist standardmaessig Studierende und bleibt erhalten`() = runTest {
        val settings = settings(this)
        assertEquals(MensaPriceGroup.Studierende, settings.priceGroup.first())
        settings.setPriceGroup(MensaPriceGroup.Gaeste)
        assertEquals(MensaPriceGroup.Gaeste, settings.priceGroup.first())
    }

    @Test
    fun `gewaehlte Mensa startet bei Ehrenberg und bleibt erhalten`() = runTest {
        val settings = settings(this)
        assertEquals(MensaVenue.MensaEhrenberg, settings.mensaVenue.first())
        settings.setMensaVenue(MensaVenue.entries.last())
        assertEquals(MensaVenue.entries.last(), settings.mensaVenue.first())
    }

    @Test
    fun `Filter ist standardmaessig leer und wird vollstaendig gespeichert`() = runTest {
        val settings = settings(this)
        assertEquals(MensaFilter(), settings.mensaFilter.first())
        val filter = MensaFilter(MensaDiet.Vegan, onlyBio = true, excludedAllergens = setOf("Gluten", "Soja"))
        settings.setMensaFilter(filter)
        assertEquals(filter, settings.mensaFilter.first())
    }

    @Test
    fun `Filter-Format entspricht dem der iOS-App`() {
        val json = Json { encodeDefaults = true }.encodeToString(MensaFilter.serializer(), MensaFilter())
        assertEquals("""{"diet":"alle","onlyBio":false,"excludedAllergens":[]}""", json)
    }

    @Test
    fun `unlesbarer Filter gilt als kein Filter`() = runTest {
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(folder.root, "corrupt.preferences_pb") },
        )
        store.edit { it[stringPreferencesKey("MensaFilter")] = "{kaputt" }
        assertEquals(MensaFilter(), AppSettings(store).mensaFilter.first())
    }

    @Test
    fun `Favoriten werden normalisiert gemerkt und wieder entfernt`() = runTest {
        val settings = settings(this)
        assertEquals(emptySet<String>(), settings.mensaFavorites.first())
        settings.setMensaFavorite("  Linsen-Dal ", true)
        settings.setMensaFavorite("Pasta", true)
        assertEquals(setOf("linsen-dal", "pasta"), settings.mensaFavorites.first())
        settings.setMensaFavorite("LINSEN-DAL", false)
        assertEquals(setOf("pasta"), settings.mensaFavorites.first())
    }
}
