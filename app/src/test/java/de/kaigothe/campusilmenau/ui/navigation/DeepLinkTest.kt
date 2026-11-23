package de.kaigothe.campusilmenau.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkTest {

    private val all: List<DeepLink> = listOf(
        DeepLink.Mensa(venueId = null),
        DeepLink.Mensa(venueId = "mensaEhrenberg"),
        DeepLink.Gericht(id = "meal_42", day = null),
        DeepLink.Gericht(id = "meal_42", day = "2026-11-23"),
        DeepLink.MensaZeiten,
        DeepLink.Events,
        DeepLink.EventsHeuteAbend,
        DeepLink.Event(id = "evt-123"),
        DeepLink.ThoskaAuslesen,
        DeepLink.KatalogSuche,
        DeepLink.KatalogSuchen(query = "Kotlin in Action"),
        DeepLink.KatalogTitel(id = "med_1234567890"),
        DeepLink.Merkliste,
        DeepLink.Mehr(bereich = null),
        DeepLink.Mehr(bereich = "konto"),
    )

    @Test
    fun `jede URL fuehrt zum selben Ziel zurueck`() {
        all.forEach { link ->
            assertEquals("Rundweg ${link.url}", link, DeepLink.parse(link.url))
        }
    }

    @Test
    fun `URLs entsprechen der Tabelle der iOS-App`() {
        assertEquals("campusilmenau://mensa", DeepLink.Mensa(null).url)
        assertEquals("campusilmenau://mensa?mensa=mensaEhrenberg", DeepLink.Mensa("mensaEhrenberg").url)
        assertEquals("campusilmenau://mensa/gericht/abc?tag=2026-11-23", DeepLink.Gericht("abc", "2026-11-23").url)
        assertEquals("campusilmenau://mensa/zeiten", DeepLink.MensaZeiten.url)
        assertEquals("campusilmenau://events", DeepLink.Events.url)
        assertEquals("campusilmenau://events?filter=heute-abend", DeepLink.EventsHeuteAbend.url)
        assertEquals("campusilmenau://events/abc", DeepLink.Event("abc").url)
        assertEquals("campusilmenau://thoska/auslesen", DeepLink.ThoskaAuslesen.url)
        assertEquals("campusilmenau://katalog/suche", DeepLink.KatalogSuche.url)
        assertEquals("campusilmenau://katalog/suche?q=Java", DeepLink.KatalogSuchen("Java").url)
        assertEquals("campusilmenau://katalog/titel/med_1", DeepLink.KatalogTitel("med_1").url)
        assertEquals("campusilmenau://bibliothek/merkliste", DeepLink.Merkliste.url)
        assertEquals("campusilmenau://mehr?bereich=darstellung", DeepLink.Mehr("darstellung").url)
    }

    @Test
    fun `Schema und Bereich sind unabhaengig von Gross- und Kleinschreibung`() {
        assertEquals(DeepLink.Mensa(null), DeepLink.parse("CampusIlmenau://MENSA"))
    }

    @Test
    fun `Events ohne Filter, mit Heute-Abend und mit unbekanntem Filter`() {
        assertEquals(DeepLink.Events, DeepLink.parse("campusilmenau://events"))
        assertEquals(DeepLink.EventsHeuteAbend, DeepLink.parse("campusilmenau://events?filter=heute-abend"))
        // Ein unbekannter Filter ist kein Ziel, er heisst nicht „alle“.
        assertNull(DeepLink.parse("campusilmenau://events?filter=gestern"))
    }

    @Test
    fun `leere Parameter gelten als fehlend`() {
        assertEquals(DeepLink.Mensa(null), DeepLink.parse("campusilmenau://mensa?mensa="))
        assertEquals(DeepLink.Gericht("x", null), DeepLink.parse("campusilmenau://mensa/gericht/x?tag="))
        assertEquals(DeepLink.KatalogSuche, DeepLink.parse("campusilmenau://katalog/suche?q="))
        assertEquals(DeepLink.Mehr(null), DeepLink.parse("campusilmenau://mehr?bereich="))
    }

    @Test
    fun `ein Plus im Suchbegriff bleibt ein Plus`() {
        assertEquals(DeepLink.KatalogSuchen("C++"), DeepLink.parse("campusilmenau://katalog/suche?q=C%2B%2B"))
        assertEquals(DeepLink.KatalogSuchen("C++"), DeepLink.parse("campusilmenau://katalog/suche?q=C++"))
        assertEquals("campusilmenau://katalog/suche?q=C%2B%2B", DeepLink.KatalogSuchen("C++").url)
    }

    @Test
    fun `Umlaute, Leerzeichen und Sonderzeichen ueberstehen den Rundweg`() {
        val link = DeepLink.KatalogSuchen("Über Ärzte & mehr: 100 % ?")
        assertEquals(link, DeepLink.parse(link.url))
        val event = DeepLink.Event("a/b c")
        assertEquals(event, DeepLink.parse(event.url))
    }

    @Test
    fun `ein unbekannter Bereich von Mehr bleibt erhalten`() {
        // Der Router wertet ihn aus: ein unbekannter Bereich gilt wie keiner.
        assertEquals(DeepLink.Mehr("gibtsnicht"), DeepLink.parse("campusilmenau://mehr?bereich=gibtsnicht"))
    }

    @Test
    fun `Pfade, die nicht passen, sind kein Ziel`() {
        assertNull(DeepLink.parse("campusilmenau://thoska"))
        assertNull(DeepLink.parse("campusilmenau://thoska/lesen"))
        assertNull(DeepLink.parse("campusilmenau://mensa/gericht"))
        assertNull(DeepLink.parse("campusilmenau://mensa/gericht/a/b"))
        assertNull(DeepLink.parse("campusilmenau://katalog/titel"))
        assertNull(DeepLink.parse("campusilmenau://bibliothek"))
        assertNull(DeepLink.parse("campusilmenau://unbekannt"))
    }

    @Test
    fun `fremde und kaputte URLs sind kein Ziel`() {
        assertNull(DeepLink.parse("https://campus-ilmenau.de/mensa"))
        assertNull(DeepLink.parse("mailto:info@example.org"))
        assertNull(DeepLink.parse("campusilmenau:mensa"))
        assertNull(DeepLink.parse("das ist keine url"))
        assertNull(DeepLink.parse(""))
    }

    @Test
    fun `jeder Link gehoert zum richtigen Tab`() {
        assertEquals(AppTab.Mensa, DeepLink.Mensa(null).tab)
        assertEquals(AppTab.Mensa, DeepLink.Gericht("a", null).tab)
        assertEquals(AppTab.Mensa, DeepLink.MensaZeiten.tab)
        assertEquals(AppTab.Events, DeepLink.Events.tab)
        assertEquals(AppTab.Events, DeepLink.EventsHeuteAbend.tab)
        assertEquals(AppTab.Events, DeepLink.Event("a").tab)
        assertEquals(AppTab.Thoska, DeepLink.ThoskaAuslesen.tab)
        assertEquals(AppTab.Bibliothek, DeepLink.KatalogSuche.tab)
        assertEquals(AppTab.Bibliothek, DeepLink.KatalogSuchen("a").tab)
        assertEquals(AppTab.Bibliothek, DeepLink.KatalogTitel("a").tab)
        assertEquals(AppTab.Bibliothek, DeepLink.Merkliste.tab)
        // „Mehr“ ist kein Tab.
        assertNull(DeepLink.Mehr(null).tab)
    }
}
