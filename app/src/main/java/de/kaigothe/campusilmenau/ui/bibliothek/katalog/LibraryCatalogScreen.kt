package de.kaigothe.campusilmenau.ui.bibliothek.katalog

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaFilter
import de.kaigothe.campusilmenau.ui.components.BackBar
import de.kaigothe.campusilmenau.ui.components.GoatLoader
import de.kaigothe.campusilmenau.ui.components.PrimaryButton
import de.kaigothe.campusilmenau.ui.components.SecondaryButton
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Die Aktionen der Oberfläche, damit sie sich ohne ViewModel in Vorschauen zeigen lässt. */
data class CatalogActions(
    val onTextChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onClear: () -> Unit = {},
    val onSelectKind: (LibraryMediaFilter) -> Unit = {},
    val onToggleAdvanced: () -> Unit = {},
    val onFields: ((AdvancedFields) -> AdvancedFields) -> Unit = {},
    val onSubmitAdvanced: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onLoadMoreInCatalog: () -> Unit = {},
    val onContinueInCatalog: () -> Unit = {},
    val onVisible: (LibraryMedia) -> Unit = {},
    val onOpen: (LibraryMedia) -> Unit = {},
)

/**
 * Katalog der UB Ilmenau: Suche ohne Anmeldung, nur lesend. Das Feld ist beim Öffnen fokussiert, außer wenn ein
 * Begriff von außen kommt ([term], Link `katalog/suche?q=`): Dann stehen die Treffer sofort da und die Tastatur bleibt zu.
 */
@Composable
fun LibraryCatalogScreen(
    onBack: () -> Unit,
    onOpenTitle: (media: LibraryMedia, searchId: String?) -> Unit,
    modifier: Modifier = Modifier,
    term: String? = null,
    viewModel: LibrarySearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onAppear()
        if (!term.isNullOrBlank()) viewModel.searchFromOutside(term)
    }
    val actions = remember(viewModel) {
        CatalogActions(
            onTextChange = viewModel::onTextChange,
            onSubmit = viewModel::submit,
            onClear = viewModel::clearText,
            onSelectKind = viewModel::selectKind,
            onToggleAdvanced = viewModel::toggleAdvanced,
            onFields = viewModel::updateFields,
            onSubmitAdvanced = viewModel::submitAdvanced,
            onRetry = viewModel::retry,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onLoadMoreInCatalog = viewModel::loadMoreInCatalog,
            onContinueInCatalog = viewModel::continueInCatalog,
            onVisible = { viewModel.requestAvailability(it.id) },
            onOpen = { onOpenTitle(it, viewModel.searchId(it)) },
        )
    }
    LibraryCatalogContent(
        state = state,
        actions = actions,
        coverFor = viewModel::cover,
        onBack = onBack,
        autofocus = term.isNullOrBlank(),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryCatalogContent(
    state: LibrarySearchState,
    actions: CatalogActions,
    coverFor: suspend (LibraryMedia) -> Bitmap?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    autofocus: Boolean = false,
) {
    val colors = CampusTheme.colors
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        BackBar(onBack = onBack)
        Text(
            text = stringResource(R.string.bib_kat_oeffnen),
            style = MaterialTheme.typography.displayMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.ScreenEdge).semantics { heading() },
        )
        SearchRow(state = state, actions = actions, autofocus = autofocus)
        MediaPills(selected = state.kind, onSelect = actions.onSelectKind)
        if (state.advancedOpen) AdvancedPanel(state = state, actions = actions)
        PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = actions.onRefresh, modifier = Modifier.weight(1f)) {
            Results(state = state, actions = actions, coverFor = coverFor)
        }
    }
}

// region Kopf

@Composable
private fun SearchRow(state: LibrarySearchState, actions: CatalogActions, autofocus: Boolean) {
    val colors = CampusTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(autofocus) {
        if (autofocus) {
            // Erst nach dem Einblenden der Ansicht, sonst kommt die Tastatur zu früh.
            delay(350L)
            runCatching { focus.requestFocus() }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val searchDescription = stringResource(R.string.bib_kat_suchfeld_cd)
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = Spacing.MinTouchTarget)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(22.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (state.text.isEmpty()) {
                    Text(stringResource(R.string.bib_kat_suchfeld_hint), style = MaterialTheme.typography.bodyLarge, color = colors.textTertiary, maxLines = 1)
                }
                BasicTextField(
                    value = state.text,
                    onValueChange = actions.onTextChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { actions.onSubmit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = searchDescription },
                )
            }
            if (state.text.isNotEmpty()) {
                IconButton(onClick = {
                    actions.onClear()
                    runCatching { focus.requestFocus() }
                }, modifier = Modifier.size(Spacing.MinTouchTarget)) {
                    Icon(Icons.Filled.Cancel, contentDescription = stringResource(R.string.bib_kat_loeschen), tint = colors.textTertiary)
                }
            }
        }
        val open = state.advancedOpen
        val stateText = stringResource(if (open) R.string.bib_kat_geoeffnet else R.string.bib_kat_geschlossen)
        Box(
            modifier = Modifier
                .size(Spacing.MinTouchTarget)
                .clip(RoundedCornerShape(12.dp))
                .background(if (open) colors.buttonPrimary else MaterialTheme.colorScheme.surfaceVariant)
                .selectable(selected = open, role = Role.Button, onClick = actions.onToggleAdvanced)
                .semantics { stateDescription = stateText },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Tune,
                contentDescription = stringResource(R.string.bib_kat_erweitert),
                tint = if (open) colors.onButtonPrimary else colors.textPrimary,
            )
        }
    }
}

@Composable
private fun MediaPills(selected: LibraryMediaFilter, onSelect: (LibraryMediaFilter) -> Unit) {
    val description = stringResource(R.string.bib_kat_medienart)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.ScreenEdge)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LibraryMediaFilter.entries.forEach { kind ->
            val isSelected = kind == selected
            val colors = CampusTheme.colors
            // Sichtbar 34 dp hoch, die Tippfläche misst 48 dp.
            Box(
                modifier = Modifier
                    .heightIn(min = Spacing.MinTouchTarget)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(kind) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(kind.pillTitle()),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) colors.onButtonPrimary else colors.textPrimary,
                    modifier = Modifier
                        .heightIn(min = 34.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.buttonPrimary else colors.surface)
                        .border(BorderStroke(1.dp, if (isSelected) colors.buttonPrimary else colors.separator), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

// endregion

// region Erweiterte Suche

@Composable
private fun AdvancedPanel(state: LibrarySearchState, actions: CatalogActions) {
    val colors = CampusTheme.colors
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val fields = state.fields
    Column(
        modifier = Modifier
            .padding(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, top = 10.dp, bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 14.dp),
    ) {
        FieldRow(R.string.bib_kat_f_titel, R.string.bib_kat_h_titel, fields.title, onSubmit = actions.onSubmit) { v -> actions.onFields { it.copy(title = v) } }
        FieldRow(R.string.bib_kat_f_person, R.string.bib_kat_h_person, fields.author, onSubmit = actions.onSubmit) { v -> actions.onFields { it.copy(author = v) } }
        FieldRow(R.string.bib_kat_f_isbn, R.string.bib_kat_h_isbn, fields.isbn, keyboardType = KeyboardType.Number, onSubmit = actions.onSubmit) { v ->
            actions.onFields { it.copy(isbn = v) }
        }
        FieldRow(R.string.bib_kat_f_schlagwort, R.string.bib_kat_h_schlagwort, fields.subject, onSubmit = actions.onSubmit) { v -> actions.onFields { it.copy(subject = v) } }
        YearRow(fields = fields, onFields = actions.onFields)

        state.formError?.let {
            Text(
                text = it.asString(),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.danger,
                modifier = Modifier.padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Text(
            text = stringResource(R.string.bib_kat_feldsuche_hinweis),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        PrimaryButton(
            text = stringResource(R.string.bib_kat_suchen),
            onClick = {
                focus.clearFocus()
                keyboard?.hide()
                actions.onSubmitAdvanced()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FieldRow(
    label: Int,
    hint: Int,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    showsSeparator: Boolean = true,
    onSubmit: () -> Unit,
    onChange: (String) -> Unit,
) {
    RowLayout(label = stringResource(label), showsSeparator = showsSeparator, modifier = modifier) {
        InlineField(value = value, hint = stringResource(hint), onChange = onChange, keyboardType = keyboardType, onSubmit = onSubmit, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun YearRow(fields: AdvancedFields, onFields: ((AdvancedFields) -> AdvancedFields) -> Unit) {
    val colors = CampusTheme.colors
    val fromDescription = stringResource(R.string.bib_kat_jahr_von)
    val toDescription = stringResource(R.string.bib_kat_jahr_bis)
    RowLayout(label = stringResource(R.string.bib_kat_f_jahr), showsSeparator = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            InlineField(
                value = fields.yearFrom,
                hint = stringResource(R.string.bib_kat_h_von),
                onChange = { v -> onFields { it.copy(yearFrom = v.filter(Char::isDigit)) } },
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f).semantics { contentDescription = fromDescription },
            )
            Text("–", color = colors.textTertiary, modifier = Modifier.clearAndSetSemantics { })
            InlineField(
                value = fields.yearTo,
                hint = stringResource(R.string.bib_kat_h_bis),
                onChange = { v -> onFields { it.copy(yearTo = v.filter(Char::isDigit)) } },
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f).semantics { contentDescription = toDescription },
            )
        }
    }
}

/** Zeile mit Beschriftung links und Eingabe rechts; bei großer Schrift steht die Beschriftung darüber. */
@Composable
private fun RowLayout(label: String, showsSeparator: Boolean = true, modifier: Modifier = Modifier, field: @Composable () -> Unit) {
    val colors = CampusTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.BoxWithConstraints {
            // Bei schmalem Platz (große Schrift, kleines Fenster) steht die Beschriftung über dem Feld.
            val stacked = maxWidth < 320.dp
            if (stacked) {
                Column(modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.MinTouchTarget).padding(top = 8.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    Box(modifier = Modifier.heightIn(min = Spacing.MinTouchTarget), contentAlignment = Alignment.CenterStart) { field() }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.MinTouchTarget),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.width(96.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { field() }
                }
            }
        }
        if (showsSeparator) Box(modifier = Modifier.fillMaxWidth().heightIn(min = 1.dp).background(colors.separator))
    }
}

@Composable
private fun InlineField(
    value: String,
    hint: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onSubmit: (() -> Unit)? = null,
) {
    val colors = CampusTheme.colors
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = colors.textTertiary, maxLines = 1)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = keyboardType,
                imeAction = if (onSubmit != null) ImeAction.Search else ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onSearch = { onSubmit?.invoke() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// endregion

// region Ergebnisse

@Composable
private fun Results(state: LibrarySearchState, actions: CatalogActions, coverFor: suspend (LibraryMedia) -> Bitmap?) {
    val colors = CampusTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.CardGap),
    ) {
        when (state.phase) {
            SearchPhase.Start -> item(key = "start") {
                StateBlock(title = stringResource(R.string.bib_kat_start_titel), text = stringResource(R.string.bib_kat_start_text))
            }
            SearchPhase.Loading -> {
                item(key = "loading") { LoadingNote(withCatalogHint = state.showsCatalogHint) }
                items(4, key = { "skeleton$it" }) { SkeletonCard() }
            }
            is SearchPhase.Failed -> item(key = "failed") {
                StateBlock(title = stringResource(R.string.bib_kat_fehler_titel), text = stringResource(R.string.bib_kat_fehler_text), buttonText = stringResource(R.string.bib_kat_erneut), onButton = actions.onRetry)
            }
            SearchPhase.Empty -> item(key = "empty") {
                StateBlock(
                    title = stringResource(R.string.bib_kat_leer_titel, state.emptyQueryText.asString()),
                    text = stringResource(if (state.catalogTried) R.string.bib_kat_leer_text_katalog else R.string.bib_kat_leer_text),
                    buttonText = if (state.offersCatalogForEmpty) stringResource(R.string.bib_kat_suchen_gesamt) else null,
                    onButton = actions.onContinueInCatalog,
                    buttonLoading = state.isContinuing,
                    error = state.loadMoreError,
                )
            }
            SearchPhase.Results -> resultsBlock(state, actions, coverFor, colors)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultsBlock(
    state: LibrarySearchState,
    actions: CatalogActions,
    coverFor: suspend (LibraryMedia) -> Bitmap?,
    colors: de.kaigothe.campusilmenau.ui.theme.CampusColors,
) {
    state.header?.let { (line1, line2) ->
        item(key = "header") {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp).semantics(mergeDescendants = true) { heading() }) {
                Text(line1.asString(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                if (line2 != null) Text(line2.asString(), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
        }
    }
    state.staleNotice?.let { notice -> item(key = "stale") { InfoRow(text = notice.asString(), icon = Icons.Outlined.Schedule, warning = true) } }

    state.sections.forEach { section ->
        section.title?.let { title ->
            item(key = "title-${section.id}") {
                Text(
                    text = title.asString().uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp),
                    color = colors.textTertiary,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 2.dp, end = 2.dp).semantics { heading() },
                )
            }
        }
        items(section.items, key = { "${section.id}-${it.id}" }) { media ->
            LaunchedEffect(media.id) { actions.onVisible(media) }
            val cover by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, media.id) { value = coverFor(media)?.asImageBitmap() }
            LibraryResultCard(
                media = media,
                availability = state.availability[media.id],
                fromCatalog = section.fromCatalog,
                searchText = state.text,
                cover = cover,
                onClick = { actions.onOpen(media) },
            )
        }
    }

    state.loadMoreError?.let { error ->
        item(key = "error") {
            Text(
                text = error.catalogMessage(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.danger,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    state.loadMoreCount?.let { count -> item(key = "more") { LoadMoreRow(count, state.isLoadingMore, actions.onLoadMore) } }
    if (state.offersCatalogContinuation) {
        item(key = "offer-continue") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.CardGap)) {
                InfoRow(text = stringResource(R.string.bib_kat_hinweis_wenige))
                SecondaryButton(
                    text = stringResource(R.string.bib_kat_weitersuchen),
                    onClick = actions.onContinueInCatalog,
                    isLoading = state.isContinuing,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (state.offersMixedEnd) {
        item(key = "offer-mixed") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.CardGap)) {
                InfoRow(text = stringResource(R.string.bib_kat_hinweis_alle))
                SecondaryButton(
                    text = stringResource(R.string.bib_kat_suchen_gesamt),
                    onClick = actions.onContinueInCatalog,
                    isLoading = state.isContinuing,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    state.loadMoreCountInCatalog?.let { count -> item(key = "more-catalog") { LoadMoreRow(count, state.isLoadingMore, actions.onLoadMoreInCatalog) } }
}

/** „Weitere N Treffer laden“; beim Nachladen steht die kleine Ziege an der Stelle des Buttons. */
@Composable
private fun LoadMoreRow(count: Int, isLoading: Boolean, onLoad: () -> Unit) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), contentAlignment = Alignment.Center) { GoatLoader(size = 40.dp) }
    } else {
        SecondaryButton(text = stringResource(R.string.bib_kat_mehr_laden, count), onClick = onLoad, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LoadingNote(withCatalogHint: Boolean) {
    val colors = CampusTheme.colors
    val text = stringResource(if (withCatalogHint) R.string.bib_kat_laedt_katalog else R.string.bib_kat_laedt)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonPill(width = 18.dp, height = 18.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun InfoRow(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, warning: Boolean = false) {
    val colors = CampusTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (warning) colors.warningSoft else colors.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = colors.warning, modifier = Modifier.padding(top = 2.dp).size(18.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (warning) FontWeight.SemiBold else FontWeight.Normal),
            color = if (warning) colors.warning else colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StateBlock(
    title: String,
    text: String,
    buttonText: String? = null,
    onButton: () -> Unit = {},
    buttonLoading: Boolean = false,
    error: ApiError? = null,
) {
    val colors = CampusTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (buttonText != null) {
            SecondaryButton(text = buttonText, onClick = onButton, isLoading = buttonLoading, modifier = Modifier.padding(top = 10.dp))
        }
        if (error != null) {
            Text(error.catalogMessage(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.danger, textAlign = TextAlign.Center)
        }
    }
}

/** Fehlertext: „Keine Verbindung“ gegen „Katalog nicht erreichbar“. */
@Composable
fun ApiError.catalogMessage(): String =
    if (this is ApiError.Http) stringResource(R.string.bib_kat_fehler_dienst) else stringResource(R.string.bib_kat_fehler_netz)

// endregion
