package de.kaigothe.campusilmenau.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import de.kaigothe.campusilmenau.domain.model.Darstellung
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
}
