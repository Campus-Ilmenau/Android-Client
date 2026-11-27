package de.kaigothe.campusilmenau.ui.bibliothek.merkliste

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import de.kaigothe.campusilmenau.domain.model.library.merkliste.MerklisteSelection
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.merkliste.GemerkterTitel
import de.kaigothe.campusilmenau.domain.model.library.merkliste.MerklisteExemplar
import de.kaigothe.campusilmenau.ui.bibliothek.katalog.chipVariant
import de.kaigothe.campusilmenau.ui.bibliothek.katalog.displayName
import de.kaigothe.campusilmenau.ui.bibliothek.katalog.LibraryCover
import de.kaigothe.campusilmenau.ui.components.BackBar
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.components.ChipVariant
import de.kaigothe.campusilmenau.ui.components.PrimaryButton
import de.kaigothe.campusilmenau.ui.components.SoftDestructiveButton
import de.kaigothe.campusilmenau.ui.navigation.LocalToastState
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing

/** Die Aktionen der Oberfläche, damit sie sich ohne ViewModel in Vorschauen zeigen lässt. */
data class MerklisteActions(
    val onOpen: (GemerkterTitel) -> Unit = {},
    val onFound: (GemerkterTitel, Boolean) -> Unit = { _, _ -> },
    val onRemove: (GemerkterTitel) -> Unit = {},
    val onClear: () -> Unit = {},
    val onConfirmClear: () -> Unit = {},
    val onDismissClear: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onSearch: () -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
)

/** Die Merkliste: Titel aus dem Bibliothekskatalog an einem Ort. Keine Anmeldung nötig, den Status lädt die Ansicht frisch. */
@Composable
fun MerklisteScreen(
    onBack: () -> Unit,
    onOpenTitle: (id: String) -> Unit,
    onOpenCatalog: () -> Unit,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
    viewModel: MerklisteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = LocalToastState.current
    val uriHandler = LocalUriHandler.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val undo = stringResource(R.string.bib_merkliste_widerrufen)

    LaunchedEffect(Unit) { viewModel.ladeStatus() }
    LaunchedEffect(viewModel) {
        viewModel.meldungen.collect { meldung ->
            val action = meldung.widerruf
            toast.show(
                text = meldung.text.resolve(context),
                actionLabel = if (action != null) undo else null,
                onAction = action?.let { { viewModel.widerrufen(it) } },
            )
        }
    }

    val actions = remember(viewModel) {
        MerklisteActions(
            onOpen = { onOpenTitle(it.id) },
            onFound = viewModel::setzeGefunden,
            onRemove = viewModel::entfernen,
            onClear = viewModel::zeigeLeerenDialog,
            onConfirmClear = viewModel::leeren,
            onDismissClear = viewModel::schliesseLeerenDialog,
            onRefresh = viewModel::ladeStatus,
            onSearch = onOpenCatalog,
            onOpenUrl = { url -> if (url.startsWith("http://", true) || url.startsWith("https://", true)) runCatching { uriHandler.openUri(url) } },
        )
    }
    MerklisteContent(state = state, actions = actions, coverFor = viewModel::cover, onBack = onBack, modifier = modifier, selectedId = selectedId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerklisteContent(
    state: MerklisteUiState,
    actions: MerklisteActions,
    coverFor: suspend (GemerkterTitel) -> Bitmap?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
) {
    val colors = CampusTheme.colors
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        BackBar(onBack = onBack, description = stringResource(R.string.bib_merkliste_zurueck))
        Column(modifier = Modifier.padding(horizontal = Spacing.ScreenEdge).padding(bottom = 14.dp)) {
            Text(
                text = stringResource(R.string.bib_merkliste_titel),
                style = MaterialTheme.typography.displayMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = state.zusammenfassung.asString(), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        PullToRefreshBox(isRefreshing = false, onRefresh = actions.onRefresh, modifier = Modifier.weight(1f)) {
            if (state.liste.isEmpty) {
                EmptyState(onSearch = actions.onSearch)
            } else {
                MerklisteList(state, actions, coverFor, selectedId)
            }
        }
    }

    if (state.clearDialogVisible) {
        val count = state.liste.anzahl
        AlertDialog(
            onDismissRequest = actions.onDismissClear,
            title = { Text(stringResource(R.string.bib_merkliste_leeren_frage)) },
            text = {
                Text(if (count == 1) stringResource(R.string.bib_merkliste_leeren_text_eins) else stringResource(R.string.bib_merkliste_leeren_text_n, count))
            },
            confirmButton = { TextButton(onClick = actions.onConfirmClear) { Text(stringResource(R.string.bib_merkliste_leeren), color = colors.danger) } },
            dismissButton = { TextButton(onClick = actions.onDismissClear) { Text(stringResource(R.string.bib_merkliste_abbrechen), color = colors.accent) } },
        )
    }
}

@Composable
private fun MerklisteList(state: MerklisteUiState, actions: MerklisteActions, coverFor: suspend (GemerkterTitel) -> Bitmap?, selectedId: String?) {
    val colors = CampusTheme.colors
    val liste = state.liste
    // Pfeiltasten ↑/↓ wechseln den gewählten Titel (Liste neben dem Titel, Tastatur am Tablet oder Chromebook).
    val order = remember(liste) { liste.offen + liste.gefunden }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val step = when (event.key) {
                    Key.DirectionDown -> 1
                    Key.DirectionUp -> -1
                    else -> return@onPreviewKeyEvent false
                }
                val next = MerklisteSelection.move(order.map { it.id }, selectedId, step)
                order.firstOrNull { it.id == next }?.let(actions.onOpen)
                next != null
            },
        contentPadding = PaddingValues(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.CardGap),
    ) {
        items(liste.offen, key = { it.id }) { titel -> Zeile(titel, state, actions, coverFor, titel.id == selectedId) }
        if (liste.gefunden.isNotEmpty()) {
            item(key = "gefunden-kopf") {
                Text(
                    text = stringResource(R.string.bib_merkliste_abschnitt_gefunden, liste.gefunden.size).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 10.dp, start = 2.dp).semantics { heading() },
                )
            }
            items(liste.gefunden, key = { it.id }) { titel -> Zeile(titel, state, actions, coverFor, titel.id == selectedId) }
        }
        item(key = "fussnote") {
            Text(
                text = stringResource(R.string.bib_merkliste_fussnote),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            )
        }
        item(key = "leeren") {
            SoftDestructiveButton(text = stringResource(R.string.bib_merkliste_leeren_button), onClick = actions.onClear, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}

/**
 * Wischen ist bewusst umgekehrt zu Mail: nach rechts „Entfernen“, nach links „Gefunden“. Für TalkBack und Tastatur gibt es
 * dieselben Aktionen als Zusatzaktionen und im Menü beim langen Drücken.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun Zeile(titel: GemerkterTitel, state: MerklisteUiState, actions: MerklisteActions, coverFor: suspend (GemerkterTitel) -> Bitmap?, selected: Boolean) {
    val colors = CampusTheme.colors
    val swipe = rememberSwipeToDismissBoxState()
    val foundLabel = stringResource(if (titel.istGefunden) R.string.bib_merkliste_nicht_mehr_gefunden else R.string.bib_merkliste_als_gefunden)
    val removeLabel = stringResource(R.string.bib_merkliste_entfernen)
    var menuOpen by remember { mutableStateOf(false) }

    SwipeToDismissBox(
        state = swipe,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> actions.onRemove(titel)
                SwipeToDismissBoxValue.EndToStart -> actions.onFound(titel, !titel.istGefunden)
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        backgroundContent = {
            val toRight = swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val (container, content, icon, label) = if (toRight) {
                SwipeStyle(colors.swipeDelete, colors.onSwipe, Icons.Outlined.Delete, stringResource(R.string.bib_merkliste_wisch_entfernen))
            } else if (titel.istGefunden) {
                SwipeStyle(colors.swipeNeutral, colors.onSwipe, Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.bib_merkliste_wisch_nicht_gefunden))
            } else {
                SwipeStyle(colors.swipeFound, colors.onSwipe, Icons.Outlined.Check, stringResource(R.string.bib_merkliste_wisch_gefunden))
            }
            Row(
                modifier = Modifier.fillMaxSize().background(container).padding(horizontal = 20.dp),
                horizontalArrangement = if (toRight) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = content)
                Text(label, color = content, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
            }
        },
    ) {
        Box {
            val status = state.status[titel.id]
            val exemplar = state.exemplare[titel.id]
            val description = zeilenBeschreibung(titel, status, exemplar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selected) colors.accentSoft else colors.surface)
                    .combinedClickable(role = Role.Button, onClick = { actions.onOpen(titel) }, onLongClick = { menuOpen = true })
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = description
                        customActions = buildList {
                            add(CustomAccessibilityAction(foundLabel) { actions.onFound(titel, !titel.istGefunden); true })
                            add(CustomAccessibilityAction(removeLabel) { actions.onRemove(titel); true })
                            exemplar?.lageplan?.let { plan -> add(CustomAccessibilityAction("Lageplan") { actions.onOpenUrl(plan); true }) }
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                val cover by produceState<ImageBitmap?>(null, titel.id, titel.coverUrl) { value = coverFor(titel)?.asImageBitmap() }
                LibraryCover(media = titel.alsMedia, image = cover, modifier = Modifier.alpha(if (titel.istGefunden) 0.45f else 1f))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titel.anzeigeTitel.asString(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (titel.istGefunden) FontWeight.Medium else FontWeight.SemiBold,
                            textDecoration = if (titel.istGefunden) TextDecoration.LineThrough else TextDecoration.None,
                        ),
                        color = if (titel.istGefunden) colors.textSecondary else colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (titel.untertitel.isNotEmpty()) {
                        Text(titel.untertitel, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
                    }
                    FlowRow(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusChip(titel = titel, status = status)
                        Text(titel.mediaType.displayName(), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                    }
                    if (exemplar != null) ExemplarZeilen(exemplar, onOpenUrl = actions.onOpenUrl, modifier = Modifier.padding(top = 6.dp))
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.padding(top = 20.dp).size(20.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(foundLabel) }, onClick = { menuOpen = false; actions.onFound(titel, !titel.istGefunden) })
                DropdownMenuItem(text = { Text(removeLabel, color = colors.danger) }, onClick = { menuOpen = false; actions.onRemove(titel) })
            }
        }
    }
}

private data class SwipeStyle(val container: androidx.compose.ui.graphics.Color, val content: androidx.compose.ui.graphics.Color, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

@Composable
private fun StatusChip(titel: GemerkterTitel, status: LibraryAvailabilityStatus?) {
    when {
        titel.istGefunden -> CampusChip(text = stringResource(R.string.bib_merkliste_chip_gefunden), variant = ChipVariant.Green, icon = Icons.Outlined.Check)
        status != null -> CampusChip(text = status.chipText(), variant = status.chipVariant())
        else -> CampusChip(text = stringResource(R.string.bib_merkliste_chip_laedt), variant = ChipVariant.Neutral)
    }
}

/** Kurzer Text im Status-Chip der Listen: Ausleihbar, Online, Ausgeliehen, Nur vor Ort, Unbekannt. */
@Composable
fun LibraryAvailabilityStatus.chipText(): String = stringResource(
    when (this) {
        LibraryAvailabilityStatus.Available -> R.string.bib_status_verfuegbar
        LibraryAvailabilityStatus.OnLoan -> R.string.bib_status_ausgeliehen
        LibraryAvailabilityStatus.ReferenceOnly -> R.string.bib_status_vor_ort
        LibraryAvailabilityStatus.Online -> R.string.bib_merkliste_chip_online
        LibraryAvailabilityStatus.Unknown -> R.string.bib_merkliste_chip_unbekannt
    },
)

/** „Signatur …“ und „Standort …“ mit dem Link zum Lageplan. */
@Composable
private fun ExemplarZeilen(exemplar: MerklisteExemplar, onOpenUrl: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    Column(modifier = modifier.clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        exemplar.signatur?.let { signatur ->
            Angabe(label = stringResource(R.string.bib_merkliste_signatur), wert = signatur, monospace = true)
        }
        exemplar.standort?.let { standort ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Angabe(label = stringResource(R.string.bib_merkliste_standort), wert = standort, zusatz = if (exemplar.weitere > 0) "+${exemplar.weitere}" else null)
                }
                exemplar.lageplan?.let { plan ->
                    Row(
                        modifier = Modifier.heightIn(min = Spacing.MinTouchTarget).clickable(role = Role.Button, onClick = { onOpenUrl(plan) }),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.bib_titel_lageplan), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.accent)
                        Icon(Icons.Outlined.Map, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

/** „Signatur  ST 130 R434“: blasse Bezeichnung, danach der Wert. Läuft bei Bedarf in die nächste Zeile. */
@Composable
private fun Angabe(label: String, wert: String, monospace: Boolean = false, zusatz: String? = null) {
    val colors = CampusTheme.colors
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = colors.textTertiary)) { append("$label  ") }
        withStyle(
            SpanStyle(
                color = colors.textPrimary,
                fontFamily = if (monospace) FontFamily.Monospace else null,
                fontWeight = if (monospace) FontWeight.SemiBold else null,
            ),
        ) { append(wert) }
        if (zusatz != null) withStyle(SpanStyle(color = colors.textTertiary)) { append("  $zusatz") }
    }
    Text(text = text, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun zeilenBeschreibung(titel: GemerkterTitel, status: LibraryAvailabilityStatus?, exemplar: MerklisteExemplar?): String {
    val statusText = when {
        titel.istGefunden -> stringResource(R.string.bib_merkliste_chip_gefunden)
        status != null -> status.chipText()
        else -> stringResource(R.string.bib_status_laedt)
    }
    val parts = buildList {
        add(titel.anzeigeTitel.asString())
        add(titel.untertitel)
        add(statusText)
        add(titel.mediaType.displayName())
        exemplar?.signatur?.let { add(stringResource(R.string.bib_merkliste_signatur_cd, it)) }
        exemplar?.standort?.let {
            add(if (exemplar.weitere > 0) stringResource(R.string.bib_merkliste_standort_weitere_cd, it, exemplar.weitere) else stringResource(R.string.bib_merkliste_standort_cd, it))
        }
    }
    return parts.filter { it.isNotEmpty() }.joinToString(", ")
}

@Composable
private fun EmptyState(onSearch: () -> Unit) {
    val colors = CampusTheme.colors
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.ScreenEdge)) {
        CampusCard(verticalPadding = 32.dp, horizontalPadding = 20.dp) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Bookmark, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(40.dp))
                Text(
                    stringResource(R.string.bib_merkliste_leer_titel),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    stringResource(R.string.bib_merkliste_leer_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
                )
                PrimaryButton(text = stringResource(R.string.bib_merkliste_im_katalog), onClick = onSearch)
            }
        }
    }
}
