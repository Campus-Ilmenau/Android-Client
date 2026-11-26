package de.kaigothe.campusilmenau.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.ui.components.ToastHost
import de.kaigothe.campusilmenau.ui.components.ToastState
import de.kaigothe.campusilmenau.ui.components.rememberToastState
import de.kaigothe.campusilmenau.domain.model.events.EventKind
import de.kaigothe.campusilmenau.domain.model.events.EventsSelection
import de.kaigothe.campusilmenau.ui.events.EventDetailScreen
import de.kaigothe.campusilmenau.ui.events.EventsPastScreen
import de.kaigothe.campusilmenau.ui.events.EventsScreen
import de.kaigothe.campusilmenau.ui.heute.HeuteScreen
import de.kaigothe.campusilmenau.ui.thoska.ThoskaHistoryScreen
import de.kaigothe.campusilmenau.ui.thoska.ThoskaScreen
import de.kaigothe.campusilmenau.ui.mensa.MensaMealDetailScreen
import de.kaigothe.campusilmenau.ui.mensa.MensaOpeningHoursScreen
import de.kaigothe.campusilmenau.ui.mensa.MensaScreen
import de.kaigothe.campusilmenau.ui.bibliothek.BibliothekScreen
import de.kaigothe.campusilmenau.ui.placeholder.FeaturePlaceholder
import de.kaigothe.campusilmenau.ui.placeholder.MehrButton

/** Der Toast der App-Shell. Jede Ansicht darf damit eine kurze Bestätigung zeigen. */
val LocalToastState = staticCompositionLocalOf<ToastState> {
    error("Kein ToastState vorhanden: Die Ansicht läuft nicht in der AppShell.")
}

/**
 * Die Hülle der App: gewählter Tab mit Tab-Leiste darunter. Jeder Tab hat einen eigenen Back Stack und behält
 * seinen Zustand, ein Tab wird erst beim ersten Besuch aufgebaut. Die Tab-Leiste verschwindet, solange die
 * Tastatur sichtbar ist. Ziele von außen (Deeplinks) wählen den Tab, sobald [launchFinished] gilt.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppShell(
    modifier: Modifier = Modifier,
    launchFinished: Boolean = true,
    viewModel: ShellViewModel = hiltViewModel(),
) {
    val navigationState = rememberNavigationState(viewModel.tabs)
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val toastState = rememberToastState()
    val request by viewModel.deepLinkRequest.collectAsStateWithLifecycle()

    // Ein Ziel von außen wartet, bis die Start-Animation vorbei ist, sonst käme es unter ihr an und ginge verloren.
    LaunchedEffect(request?.id, launchFinished) {
        val pending = request
        if (pending != null && launchFinished) {
            viewModel.prepare(pending.link)
            navigator.open(pending.link)
            viewModel.consume(pending.id)
        }
    }

    // Ziele von Karten und Terminen laufen über dieselben Links wie Widgets und Shortcuts.
    fun open(link: DeepLink) {
        viewModel.prepare(link)
        navigator.open(link)
    }

    val entries = entryProvider<NavKey> {
        entry<HeuteRoute> {
            HeuteScreen(
                onOpenMehr = { navigator.openMehr(bereich = null) },
                onZiel = { ziel -> open(ziel.toDeepLink()) },
            )
        }
        entry<MensaRoute> {
            MensaScreen(
                onOpenMeal = { meal, day -> navigator.navigate(MensaGerichtRoute(meal.id, day.toString())) },
                onOpenHours = { navigator.navigate(MensaZeitenRoute) },
            )
        }
        entry<MensaGerichtRoute> { route ->
            MensaMealDetailScreen(
                route = route,
                onBack = { navigator.goBack() },
                onOpenHours = { navigator.navigate(MensaZeitenRoute) },
            )
        }
        entry<MensaZeitenRoute> { MensaOpeningHoursScreen(onBack = { navigator.goBack() }) }
        entry<ThoskaRoute> { ThoskaScreen(onOpenHistory = { navigator.navigate(ThoskaVerlaufRoute) }) }
        entry<ThoskaVerlaufRoute> { ThoskaHistoryScreen(onBack = { navigator.goBack() }) }
        entry<BibliothekRoute> { BibliothekScreen() }
        entry<EventsRoute> {
            EventsScreen(
                onOpenEvent = { id -> navigator.navigate(EventDetailRoute(id)) },
                onOpenPast = { selection ->
                    navigator.navigate(EventsVergangeneRoute(selection.kind.name, selection.hideCancelled))
                },
            )
        }
        entry<EventsVergangeneRoute> { route ->
            EventsPastScreen(
                initial = EventsSelection(
                    kind = EventKind.entries.firstOrNull { it.name == route.kind } ?: EventKind.All,
                    hideCancelled = route.hideCancelled,
                ),
                onBack = { navigator.goBack() },
                onOpenEvent = { id -> navigator.navigate(EventDetailRoute(id)) },
            )
        }
        entry<EventDetailRoute> { route -> EventDetailScreen(id = route.id, onBack = { navigator.goBack() }) }
        entry<MehrRoute> { FeaturePlaceholder(title = R.string.mehr_titel) }
    }

    val imeVisible = WindowInsets.isImeVisible

    CompositionLocalProvider(LocalToastState provides toastState) {
        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0.dp),
            bottomBar = {
                if (!imeVisible) {
                    CampusTabBar(
                        tabs = navigationState.tabs,
                        selected = navigationState.selectedTab,
                        onSelect = navigator::selectTab,
                        onReselect = navigator::onReselect,
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .then(if (imeVisible) Modifier.imePadding() else Modifier),
            ) {
                NavDisplay(
                    entries = navigationState.toDecoratedEntries(entries),
                    onBack = { navigator.goBack() },
                )
                ToastHost(state = toastState, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
