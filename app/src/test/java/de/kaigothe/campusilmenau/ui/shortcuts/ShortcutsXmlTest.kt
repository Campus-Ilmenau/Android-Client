package de.kaigothe.campusilmenau.ui.shortcuts

import de.kaigothe.campusilmenau.ui.navigation.DeepLink
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prüft `res/xml/shortcuts.xml`: Jeder Shortcut öffnet einen Link, den die App versteht, und hat Texte in beiden Sprachen. */
class ShortcutsXmlTest {
    private val xml = File("src/main/res/xml/shortcuts.xml").readText()
    private val de = File("src/main/res/values/strings.xml").readText()
    private val en = File("src/main/res/values-en/strings.xml").readText()

    private fun values(attribute: String) = Regex("""$attribute="([^"]+)"""").findAll(xml).map { it.groupValues[1] }.toList()

    @Test
    fun `every shortcut opens a link the app understands`() {
        val links = values("android:data")
        assertEquals(5, links.size)
        links.forEach { url -> assertNotNull("Kein Ziel für $url", DeepLink.parse(url)) }
    }

    @Test
    fun `the targets are the ones the labels promise`() {
        val targets = values("android:data").mapNotNull(DeepLink::parse)
        assertEquals(
            listOf(
                DeepLink.Mensa(null),
                DeepLink.EventsHeuteAbend,
                DeepLink.KatalogSuche,
                DeepLink.Merkliste,
                DeepLink.MensaZeiten,
            ),
            targets,
        )
    }

    @Test
    fun `ids are unique and every shortcut starts MainActivity of this app`() {
        val ids = values("android:shortcutId")
        assertEquals(ids.size, ids.toSet().size)
        values("android:targetClass").forEach { assertEquals("de.kaigothe.campusilmenau.MainActivity", it) }
        values("android:targetPackage").forEach { assertEquals("de.kaigothe.campusilmenau", it) }
    }

    @Test
    fun `every label exists in german and english`() {
        val names = Regex("""@string/(\w+)""").findAll(xml).map { it.groupValues[1] }.toSet()
        assertTrue(names.isNotEmpty())
        names.forEach { name ->
            assertTrue("$name fehlt auf Deutsch", de.contains("""name="$name""""))
            assertTrue("$name fehlt auf Englisch", en.contains("""name="$name""""))
        }
    }

    @Test
    fun `short labels stay short`() {
        // Launcher schneiden lange Kurzbeschriftungen ab; 10 Zeichen sind die Empfehlung, 14 die Grenze dieser App.
        val shorts = Regex("""name="(shortcut_\w+_kurz)"[^>]*>([^<]+)<""")
        listOf(de, en).forEach { text ->
            shorts.findAll(text).forEach { match ->
                assertTrue("${match.groupValues[1]} ist zu lang", match.groupValues[2].length <= 14)
            }
        }
    }
}
