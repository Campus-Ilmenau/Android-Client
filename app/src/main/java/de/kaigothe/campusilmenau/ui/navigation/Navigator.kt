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

    /** Öffnet das Ziel eines Links: wählt den Tab oder öffnet „Mehr“. Ohne NFC bleibt Thoska verschlossen. */
    fun open(link: DeepLink) {
        when (val tab = link.tab) {
            null -> openMehr((link as DeepLink.Mehr).bereich)
            else -> selectTab(tab)
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
