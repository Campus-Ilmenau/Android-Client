package de.kaigothe.campusilmenau.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Verarbeitet Navigationsereignisse (vor, zurück, Tab wechseln, Ziel von außen) und verändert damit den
 * [NavigationState]. Pendant zum `AppRouter` der iOS-App, ohne dessen „Wünsche“ an die Ansichten.
 */
class Navigator(val state: NavigationState) {

    private val _reselectEvents = MutableSharedFlow<AppTab>(extraBufferCapacity = 1)

    /**
     * Der Nutzer tippt den bereits gewählten Tab erneut. Ansichten können darauf reagieren, zum Beispiel
     * „nach oben scrollen“.
     */
    val reselectEvents: SharedFlow<AppTab> = _reselectEvents.asSharedFlow()

    fun selectTab(tab: AppTab) {
        if (tab in state.tabs) state.selectedTab = tab
    }

    fun onReselect(tab: AppTab) {
        _reselectEvents.tryEmit(tab)
    }

    /** Legt [route] auf den Stack des gewählten Tabs. Dieselbe Route zweimal hintereinander wird ignoriert. */
    fun navigate(route: NavKey) {
        val stack = state.currentStack
        if (stack.lastOrNull() != route) stack.add(route)
    }

    /** Öffnet „Mehr“ über „Heute“, mit vorgewähltem [bereich]. Ein früheres „Mehr“ wird ersetzt. */
    fun openMehr(bereich: String?) {
        state.selectedTab = state.startTab
        val stack = state.backStacks.getValue(state.startTab)
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        stack.add(MehrRoute(bereich))
    }

    /**
     * Öffnet das Ziel eines Links: wählt den Tab oder öffnet „Mehr“. Ohne NFC bleibt Thoska verschlossen.
     * Ziele unterhalb eines Tabs (ein Gericht, die Öffnungszeiten) liegen auf der Wurzel des Tabs, ein früherer
     * Zustand dieses Tabs wird ersetzt.
     */
    fun open(link: DeepLink) {
        val tab = link.tab
        if (tab == null) {
            openMehr((link as DeepLink.Mehr).bereich)
            return
        }
        if (tab !in state.tabs) return
        selectTab(tab)
        val detail: NavKey? = when (link) {
            is DeepLink.Gericht -> MensaGerichtRoute(link.id, link.day)
            DeepLink.MensaZeiten -> MensaZeitenRoute
            is DeepLink.Event -> EventDetailRoute(link.id)
            DeepLink.KatalogSuche -> KatalogRoute()
            is DeepLink.KatalogSuchen -> KatalogRoute(link.query)
            is DeepLink.KatalogTitel -> KatalogTitelRoute(link.id)
            DeepLink.Merkliste -> MerklisteRoute
            else -> null
        }
        // Die Liste der Veranstaltungen beginnt bei einem Link ohne Unterziel wieder vorn (ohne ein offenes Detail).
        val resetsStack = detail != null || link == DeepLink.Events || link == DeepLink.EventsHeuteAbend
        if (resetsStack) {
            val stack = state.backStacks.getValue(tab)
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            detail?.let(stack::add)
        }
    }

    /**
     * Ein Schritt zurück. Liegt auf dem Stack des Tabs mehr als die Wurzel, wird die oberste Route entfernt, sonst
     * geht es zurück zum Start-Tab („Ausstieg über Heute“).
     * @return `false`, wenn die App verlassen werden soll.
     */
    fun goBack(): Boolean {
        val stack = state.currentStack
        return when {
            stack.size > 1 -> {
                stack.removeAt(stack.lastIndex)
                true
            }

            state.selectedTab != state.startTab -> {
                state.selectedTab = state.startTab
                true
            }

            else -> false
        }
    }
}
