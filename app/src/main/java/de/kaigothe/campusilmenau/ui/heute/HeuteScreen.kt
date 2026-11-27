package de.kaigothe.campusilmenau.ui.heute

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import de.kaigothe.campusilmenau.ui.adaptive.AdaptiveColumns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.heute.HeuteClock
import de.kaigothe.campusilmenau.domain.model.heute.HeuteEvent
import de.kaigothe.campusilmenau.domain.model.heute.HeuteZiel
import de.kaigothe.campusilmenau.domain.model.heute.Hinweis
import de.kaigothe.campusilmenau.domain.model.heute.Tagesphase
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFormatting
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.mensa.Canteen
import de.kaigothe.campusilmenau.domain.model.mensa.OpeningHour
import de.kaigothe.campusilmenau.ui.mensa.MensaPreviewData
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Jede der zwei Spalten braucht mindestens so viel Breite (dp), sonst steht alles untereinander. */
private const val ColumnMinDp = 340

/**
 * Die Startseite „Heute“: Kopf, Hinweise, Jetzt-Karte, dann „Als Nächstes“ und „Heute Abend“ (ab 15:00 getauscht).
 * Nach `Heute.dc.html`. [onZiel] öffnet, was eine Karte oder ein Termin meint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeuteScreen(
    onOpenMehr: () -> Unit,
    onZiel: (HeuteZiel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HeuteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    // Die Seite lädt beim Sichtbarwerden (bei Bedarf) und hält sich aktuell, solange sie sichtbar ist.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.watch() }
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                try {
                    viewModel.load()
                } finally {
                    refreshing = false
                }
            }
        },
        modifier = modifier,
    ) {
        HeuteContentView(
            state = state,
            onOpenMehr = onOpenMehr,
            onZiel = onZiel,
            onSelectVenue = viewModel::selectVenue,
            onDismissNotice = viewModel::dismissNotice,
        )
    }
}

/** Oberfläche ohne ViewModel, damit sie sich in Vorschauen in jeder Tagesphase zeigen lässt. */
@Composable
fun HeuteContentView(
    state: HeuteUiState,
    onOpenMehr: () -> Unit,
    onZiel: (HeuteZiel) -> Unit,
    onSelectVenue: (MensaVenue) -> Unit,
    onDismissNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state.content
    var showVenueSheet by rememberSaveable { mutableStateOf(false) }
    val card = content.currentCard
    val showChange = card?.changeVenueVisible ?: (content.phase != Tagesphase.Nacht)

    val evening: @Composable () -> Unit = {
        HeuteAbendKarte(
            events = content.eveningEvents,
            now = state.now,
            onAlle = { onZiel(HeuteZiel.AlleEvents) },
            onOeffne = { event -> onZiel(HeuteZiel.Event(event.id)) },
        )
    }
    val upcoming: @Composable () -> Unit = {
        HeuteAlsNaechstesView(
            appointments = content.upcoming,
            onZiel = onZiel,
            modifier = Modifier.padding(horizontal = Spacing.ScreenEdge),
        )
    }

    val notices: @Composable () -> Unit = {
        val active = content.activeNotices
        if (active.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.ScreenEdge),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionTitle(stringResource(R.string.heute_hinweise))
                active.forEach { notice ->
                    HeuteHinweisKarte(notice = notice, onDismiss = { onDismissNotice(notice.id) })
                }
            }
        }
    }
    val now: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.ScreenEdge),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(content.phase.heading.asString(), modifier = Modifier.weight(1f))
                if (showChange) ChangeVenueButton(onClick = { showVenueSheet = true })
            }
            if (!state.loaded) {
                HeuteJetztPlatzhalter()
            } else {
                HeuteJetztKarteView(card = card ?: content.errorCard, onZiel = onZiel)
            }
        }
    }
    // Ab 15:00 (nicht nachts) steht „Heute Abend“ vor „Als Nächstes“.
    val later: @Composable () -> Unit = {
        if (content.eventsFirst) {
            evening()
            upcoming()
        } else {
            upcoming()
            evening()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Breit genug für zwei Spalten: links Hinweise und „Jetzt“, rechts, was danach kommt (iPad: `HeutePad`).
        val columns = AdaptiveColumns.count(availableDp = maxWidth.value.toInt(), minColumnDp = ColumnMinDp, maxColumns = 2)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 8.dp, bottom = Spacing.Xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.SectionGap),
        ) {
            Header(state = state, onOpenMehr = onOpenMehr, modifier = Modifier.padding(horizontal = Spacing.ScreenEdge))
            if (columns >= 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.SectionGap)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.SectionGap)) {
                        notices()
                        now()
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.SectionGap)) {
                        later()
                    }
                }
            } else {
                notices()
                now()
                later()
            }
        }
    }

    if (showVenueSheet) {
        HeuteMensaSheet(
            options = content.venueOptions,
            selected = state.venue,
            onSelect = onSelectVenue,
            onDismiss = { showVenueSheet = false },
        )
    }
}

@Composable
private fun Header(state: HeuteUiState, onOpenMehr: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val date = BerlinTime.date(state.now)
    val dateLine = stringResource(
        R.string.heute_datum_zeile,
        MensaFormatting.spokenDay(date, locale),
        HeuteClock.time(state.now),
    )
    val spoken = stringResource(
        R.string.heute_datum_zeile_sprechen,
        MensaFormatting.spokenDay(date, locale),
        HeuteClock.time(state.now),
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = dateLine,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary,
                modifier = Modifier.semantics { contentDescription = spoken },
            )
            Text(
                text = state.content.greeting.text.asString(),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
        }
        Box(
            modifier = Modifier
                .size(Spacing.MinTouchTarget)
                .clickable(role = Role.Button, onClick = onOpenMehr),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(44.dp).background(colors.surfaceSecondary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = stringResource(R.string.mehr_oeffnen),
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun ChangeVenueButton(onClick: () -> Unit) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier
            .heightIn(min = Spacing.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.heute_mensa_aendern),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accent,
        )
        Icon(imageVector = Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
    }
}

// MARK: - Vorschauen

private val PreviewDay = LocalDate.of(2026, 11, 24)

private fun previewState(hour: Int, minute: Int = 0): HeuteUiState {
    val hours = listOf("mon", "tue", "wed", "thu", "fri").map { OpeningHour(it, "11:00", "14:00") }
    val cafeteria = listOf("mon", "tue", "wed", "thu", "fri").map { OpeningHour(it, "08:00", "20:00") }
    val dinner = MensaPreviewData.sample.copy(
        id = "d1", name = "Burger mit Pommes", canteenName = "Cafeteria Mensa Ehrenberg", category = "Abendessen",
    )
    val start = BerlinTime.at(PreviewDay, 20 * 60)
    return HeuteUiState(
        now = BerlinTime.at(PreviewDay, hour * 60 + minute),
        canteens = listOf(
            Canteen(null, "Mensa Ehrenberg", true, null, null, hours),
            Canteen(null, "Cafeteria Mensa Ehrenberg", true, null, null, cafeteria),
        ),
        mealsToday = listOf(MensaPreviewData.sample, MensaPreviewData.minimal, dinner),
        mealsTomorrow = listOf(MensaPreviewData.sample.copy(id = "t1", name = "Reispfanne")),
        eventsEvening = listOf(
            HeuteEvent("e1", "Konzert im Club", start, null, false, false, "Club Ilmenau", "Studentenclub"),
            HeuteEvent("e2", "Spieleabend", start.plusSeconds(7200), null, false, false, "Mensa", null),
        ),
        notices = listOf(Hinweis("n1", "Studium", "ab Mo, 30. Nov.", "Rückmeldung", "Die Rückmeldefrist endet bald.")),
        loaded = true,
    )
}

@Composable
private fun PreviewHost(dark: Boolean, state: HeuteUiState) {
    CampusIlmenauTheme(darkTheme = dark) {
        HeuteContentView(state, onOpenMehr = {}, onZiel = {}, onSelectVenue = {}, onDismissNotice = {})
    }
}

@Preview(name = "Morgen", showBackground = true, heightDp = 1000)
@Composable
private fun HeuteMorgenPreview() = PreviewHost(false, previewState(9, 15))

@Preview(name = "Mittag Dark", showBackground = true, heightDp = 1000, backgroundColor = 0xFF0E1A2E)
@Composable
private fun HeuteMittagPreview() = PreviewHost(true, previewState(12, 10))

@Preview(name = "Nachmittag", showBackground = true, heightDp = 1000)
@Composable
private fun HeuteNachmittagPreview() = PreviewHost(false, previewState(15, 30))

@Preview(name = "Nacht", showBackground = true, heightDp = 1000)
@Composable
private fun HeuteNachtPreview() = PreviewHost(false, previewState(20, 30))

@Preview(name = "Nacht Dark", showBackground = true, heightDp = 1000, backgroundColor = 0xFF0E1A2E)
@Composable
private fun HeuteNachtDarkPreview() = PreviewHost(true, previewState(20, 30))
