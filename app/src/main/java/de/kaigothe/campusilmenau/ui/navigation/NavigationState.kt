package de.kaigothe.campusilmenau.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * Navigationszustand: ein Back Stack je Tab, dazu der gewählte Tab. Der Zustand überlebt Konfigurationswechsel
 * und das Beenden des Prozesses. Er wird nur über den [Navigator] verändert.
 *
 * Die App folgt dem Muster „Ausstieg über Heute“: Der Stack von „Heute“ ist immer dabei, dazu höchstens der des
 * gewählten Tabs. Der Zustand der übrigen Tabs bleibt erhalten, ihre Ansichten werden erst beim Besuch aufgebaut.
 */
class NavigationState(
    val tabs: List<AppTab>,
    private val selectedName: MutableState<String>,
    val backStacks: Map<AppTab, NavBackStack<NavKey>>,
) {
    /** Der Tab, mit dem die App startet und über den sie verlassen wird. */
    val startTab: AppTab = AppTab.Heute

    var selectedTab: AppTab
        get() = tabs.firstOrNull { it.name == selectedName.value } ?: startTab
        set(value) {
            selectedName.value = value.name
        }

    /** Der Back Stack des gewählten Tabs. */
    val currentStack: NavBackStack<NavKey>
        get() = backStacks.getValue(selectedTab)

    /** Die Tabs, deren Ansichten gerade im Baum stehen: immer [startTab], dazu der gewählte. */
    fun tabsInUse(): List<AppTab> =
        if (selectedTab == startTab) listOf(startTab) else listOf(startTab, selectedTab)

    /**
     * Wandelt die Back Stacks in Einträge um, die ihren Zustand (`rememberSaveable`) und ihre ViewModels je Eintrag
     * behalten.
     */
    @Composable
    fun toDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
        val decorated = backStacks.mapValues { (_, stack) ->
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator<NavKey>(),
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider,
            )
        }
        return tabsInUse().flatMap { decorated[it].orEmpty() }
    }
}

/** Legt den Navigationszustand für die gezeigten [tabs] an. Er bleibt über Konfigurationswechsel erhalten. */
@Composable
fun rememberNavigationState(tabs: List<AppTab>): NavigationState {
    val selected = rememberSaveable { mutableStateOf(AppTab.Heute.name) }
    val backStacks = tabs.associateWith { tab -> rememberNavBackStack(tab.route) }
    return remember(tabs, backStacks) { NavigationState(tabs, selected, backStacks) }
}

/** Zustand ohne Compose, für Tests. */
fun navigationStateOf(tabs: List<AppTab>): NavigationState = NavigationState(
    tabs = tabs,
    selectedName = mutableStateOf(AppTab.Heute.name),
    backStacks = tabs.associateWith { NavBackStack<NavKey>(it.route) },
)
