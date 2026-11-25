package de.kaigothe.campusilmenau.ui.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {

    private val allTabs = AppTab.visible(hasNfc = true)
    private val noNfcTabs = AppTab.visible(hasNfc = false)

    private fun navigator(tabs: List<AppTab> = allTabs) = Navigator(navigationStateOf(tabs))

    private fun Navigator.stack(tab: AppTab): List<NavKey> = state.backStacks.getValue(tab).toList()

    @Test
    fun `Tabs mit und ohne NFC`() {
        assertEquals(listOf(AppTab.Heute, AppTab.Mensa, AppTab.Thoska, AppTab.Bibliothek, AppTab.Events), allTabs)
        assertEquals(listOf(AppTab.Heute, AppTab.Mensa, AppTab.Bibliothek, AppTab.Events), noNfcTabs)
    }

    @Test
    fun `Start ist Heute, und von dort verlaesst man die App`() {
        val navigator = navigator()
        assertEquals(AppTab.Heute, navigator.state.selectedTab)
        assertFalse(navigator.goBack())
    }

    @Test
    fun `zurueck fuehrt von jedem Tab ueber Heute`() {
        val navigator = navigator()
        navigator.selectTab(AppTab.Mensa)
        navigator.selectTab(AppTab.Events)
        assertEquals(AppTab.Events, navigator.state.selectedTab)

        assertTrue(navigator.goBack())
        assertEquals(AppTab.Heute, navigator.state.selectedTab)
        assertFalse(navigator.goBack())
    }

    @Test
    fun `nur Heute und der gewaehlte Tab sind im Baum`() {
        val navigator = navigator()
        assertEquals(listOf(AppTab.Heute), navigator.state.tabsInUse())
        navigator.selectTab(AppTab.Bibliothek)
        assertEquals(listOf(AppTab.Heute, AppTab.Bibliothek), navigator.state.tabsInUse())
    }

    @Test
    fun `ein Tab ohne NFC-Hardware laesst sich nicht waehlen`() {
        val navigator = navigator(noNfcTabs)
        navigator.selectTab(AppTab.Thoska)
        assertEquals(AppTab.Heute, navigator.state.selectedTab)
    }

    @Test
    fun `navigate legt eine Route auf den Stack des gewaehlten Tabs, zurueck nimmt sie weg`() {
        val navigator = navigator()
        navigator.selectTab(AppTab.Mensa)
        navigator.navigate(MehrRoute("x"))
        assertEquals(listOf(MensaRoute, MehrRoute("x")), navigator.stack(AppTab.Mensa))

        assertTrue(navigator.goBack())
        assertEquals(listOf<NavKey>(MensaRoute), navigator.stack(AppTab.Mensa))
        assertEquals(AppTab.Mensa, navigator.state.selectedTab)
    }

    @Test
    fun `dieselbe Route zweimal hintereinander wird ignoriert`() {
        val navigator = navigator()
        navigator.navigate(MehrRoute(null))
        navigator.navigate(MehrRoute(null))
        assertEquals(2, navigator.stack(AppTab.Heute).size)
    }

    @Test
    fun `Mehr oeffnet ueber Heute, auch aus einem anderen Tab`() {
        val navigator = navigator()
        navigator.selectTab(AppTab.Events)
        navigator.openMehr("konto")

        assertEquals(AppTab.Heute, navigator.state.selectedTab)
        assertEquals(listOf(HeuteRoute, MehrRoute("konto")), navigator.stack(AppTab.Heute))
    }

    @Test
    fun `ein zweites Mehr ersetzt das erste`() {
        val navigator = navigator()
        navigator.openMehr("konto")
        navigator.openMehr("sprache")
        assertEquals(listOf(HeuteRoute, MehrRoute("sprache")), navigator.stack(AppTab.Heute))
    }

    @Test
    fun `von Mehr geht es zurueck auf Heute`() {
        val navigator = navigator()
        navigator.openMehr(null)
        assertTrue(navigator.goBack())
        assertEquals(listOf<NavKey>(HeuteRoute), navigator.stack(AppTab.Heute))
        assertFalse(navigator.goBack())
    }

    @Test
    fun `Links waehlen den Tab oder oeffnen Mehr`() {
        val navigator = navigator()

        navigator.open(DeepLink.Event("a"))
        assertEquals(AppTab.Events, navigator.state.selectedTab)

        navigator.open(DeepLink.KatalogSuchen("x"))
        assertEquals(AppTab.Bibliothek, navigator.state.selectedTab)

        navigator.open(DeepLink.Mehr("darstellung"))
        assertEquals(AppTab.Heute, navigator.state.selectedTab)
        assertEquals(MehrRoute("darstellung"), navigator.stack(AppTab.Heute).last())
    }

    @Test
    fun `ein Gericht liegt auf der Wurzel des Mensa-Tabs und ersetzt fruehere Ziele`() {
        val navigator = navigator()

        navigator.open(DeepLink.Gericht("abc", "2026-11-24"))
        assertEquals(AppTab.Mensa, navigator.state.selectedTab)
        assertEquals(
            listOf<NavKey>(MensaRoute, MensaGerichtRoute("abc", "2026-11-24")),
            navigator.stack(AppTab.Mensa),
        )

        navigator.open(DeepLink.MensaZeiten)
        assertEquals(listOf<NavKey>(MensaRoute, MensaZeitenRoute), navigator.stack(AppTab.Mensa))

        // Zurück führt auf den Speiseplan, nicht aus dem Tab.
        assertTrue(navigator.goBack())
        assertEquals(AppTab.Mensa, navigator.state.selectedTab)
        assertEquals(listOf<NavKey>(MensaRoute), navigator.stack(AppTab.Mensa))
    }

    @Test
    fun `der Link Mensa waehlt den Tab und laesst dessen Stack unveraendert`() {
        val navigator = navigator()
        navigator.open(DeepLink.Gericht("abc", null))

        navigator.open(DeepLink.Mensa("nanoteria"))

        assertEquals(AppTab.Mensa, navigator.state.selectedTab)
        assertEquals(MensaGerichtRoute("abc", null), navigator.stack(AppTab.Mensa).last())
    }

    @Test
    fun `ein Event-Link oeffnet das Detail, die Liste beginnt bei Events wieder vorn`() {
        val navigator = navigator()

        navigator.open(DeepLink.Event("abc"))
        assertEquals(AppTab.Events, navigator.state.selectedTab)
        assertEquals(listOf<NavKey>(EventsRoute, EventDetailRoute("abc")), navigator.stack(AppTab.Events))

        navigator.navigate(EventsVergangeneRoute("All", false))
        navigator.open(DeepLink.EventsHeuteAbend)
        assertEquals(listOf<NavKey>(EventsRoute), navigator.stack(AppTab.Events))

        navigator.open(DeepLink.Event("x"))
        navigator.open(DeepLink.Events)
        assertEquals(listOf<NavKey>(EventsRoute), navigator.stack(AppTab.Events))
    }

    @Test
    fun `der Link Thoska auslesen wird ohne NFC ignoriert`() {
        val navigator = navigator(noNfcTabs)
        navigator.selectTab(AppTab.Mensa)
        navigator.open(DeepLink.ThoskaAuslesen)
        assertEquals(AppTab.Mensa, navigator.state.selectedTab)
    }

    @Test
    fun `erneutes Tippen auf den Tab meldet ein Ereignis`() {
        val navigator = navigator()
        navigator.onReselect(AppTab.Heute)
        // Ohne Sammler geht das Ereignis verloren, ohne Fehler (kein Puffer noetig).
        assertEquals(AppTab.Heute, navigator.state.selectedTab)
    }
}
