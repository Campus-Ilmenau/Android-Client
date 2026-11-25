package de.kaigothe.campusilmenau.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FilterList
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventKind
import de.kaigothe.campusilmenau.domain.model.events.EventOrganizer
import de.kaigothe.campusilmenau.domain.model.events.EventStatus
import de.kaigothe.campusilmenau.domain.model.events.EventTimes
import de.kaigothe.campusilmenau.domain.model.events.EventsSelection
import de.kaigothe.campusilmenau.ui.components.GoatLoader
import de.kaigothe.campusilmenau.ui.components.PrimaryButton
import de.kaigothe.campusilmenau.ui.components.SecondaryButton
import de.kaigothe.campusilmenau.ui.mensa.FilterCountBadge
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/** Der Tab „Events“: Kopf, Tagesleiste, Chipleiste und die nach Tagen gruppierte Liste. */
@Composable
fun EventsScreen(
    onOpenEvent: (String) -> Unit,
    onOpenPast: (EventsSelection) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel<EventsViewModel, EventsViewModel.Factory>(
        key = "events-upcoming",
        creationCallback = { factory -> factory.create(EventsMode.Upcoming, EventsSelection()) },
    ),
) {
    EventsHost(viewModel, onBack = null, onOpenEvent = onOpenEvent, onOpenPast = onOpenPast, modifier = modifier)
}

/** „Vergangene“: der letzte Monat bis gestern, neueste zuerst. Ohne Tages- und Chipleiste. */
@Composable
fun EventsPastScreen(
    initial: EventsSelection,
    onBack: () -> Unit,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel<EventsViewModel, EventsViewModel.Factory>(
        key = "events-past",
        creationCallback = { factory -> factory.create(EventsMode.Past, initial) },
    ),
) {
    EventsHost(viewModel, onBack = onBack, onOpenEvent = onOpenEvent, onOpenPast = null, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventsHost(
    viewModel: EventsViewModel,
    onBack: (() -> Unit)?,
    onOpenEvent: (String) -> Unit,
    onOpenPast: ((EventsSelection) -> Unit)?,
    modifier: Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                try {
                    viewModel.refresh()
                } finally {
                    refreshing = false
                }
            }
        },
        modifier = modifier,
    ) {
        EventsContent(
            state = state,
            onBack = onBack,
            onSelect = viewModel::select,
            onToggleEvening = viewModel::toggleEvening,
            onReset = viewModel::reset,
            onRetry = viewModel::retry,
            onLoadMore = viewModel::loadMore,
            onToggleBookmark = viewModel::toggleBookmark,
            onOpenEvent = onOpenEvent,
            onOpenPast = onOpenPast?.let { open -> { open(state.selection) } },
        )
    }
}

/** Oberfläche ohne ViewModel, damit sie sich in Vorschauen in jedem Zustand zeigen lässt. */
@Composable
fun EventsContent(
    state: EventsUiState,
    onBack: (() -> Unit)?,
    onSelect: ((EventsSelection) -> EventsSelection) -> Unit,
    onToggleEvening: () -> Unit,
    onReset: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleBookmark: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenPast: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var showFilter by rememberSaveable { mutableStateOf(false) }
    val upcoming = state.mode == EventsMode.Upcoming

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        EventsHeader(
            title = stringResource(if (upcoming) R.string.events_titel else R.string.events_vergangene),
            subtitle = if (upcoming) state.selection.subtitle(state.now).asString() else stringResource(R.string.events_unterzeile_vergangene),
            filterCount = state.selection.sheetCount,
            onBack = onBack,
            onFilter = { showFilter = true },
        )
        if (upcoming) {
            DayStrip(state = state, onSelect = { day -> onSelect { it.withDay(day) } })
            ChipBar(
                state = state,
                onToggleEvening = onToggleEvening,
                onToggleBookmarked = { onSelect { it.copy(onlyBookmarked = !it.onlyBookmarked) } },
                onToggleOrganizer = { organizer -> onSelect { it.toggleOrganizer(organizer.name, organizer.id) } },
            )
        }
        EventsList(
            state = state,
            onReset = onReset,
            onRetry = onRetry,
            onLoadMore = onLoadMore,
            onToggleBookmark = onToggleBookmark,
            onOpenEvent = onOpenEvent,
            onOpenPast = onOpenPast,
        )
    }

    if (showFilter) {
        EventsFilterSheet(selection = state.selection, onChange = onSelect, onDismiss = { showFilter = false })
    }
}

// MARK: - Kopf

@Composable
private fun EventsHeader(title: String, subtitle: String, filterCount: Int, onBack: (() -> Unit)?, onFilter: () -> Unit) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .heightIn(min = Spacing.MinTouchTarget)
                        .clickable(role = Role.Button, onClick = onBack),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(R.string.events_kommende),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.accent,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        FilterButton(count = filterCount, onClick = onFilter)
    }
}

/** Runde Taste. Ist im Sheet etwas gesetzt, ist sie gefüllt (`buttonPrimary`) und zeigt die Zahl in `accent`. */
@Composable
private fun FilterButton(count: Int, onClick: () -> Unit) {
    val colors = CampusTheme.colors
    val description = if (count > 0) stringResource(R.string.events_filter_aktiv, count) else stringResource(R.string.filter_titel)
    Box(
        modifier = Modifier
            .size(Spacing.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(if (count > 0) colors.buttonPrimary else colors.surfaceSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = null,
                tint = if (count > 0) colors.onButtonPrimary else colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        if (count > 0) FilterCountBadge(count = count, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp))
    }
}

// MARK: - Tagesleiste

/** Tagesleiste, 56 dp hoch: „Ab heute“, „Heute“, „Morgen“, „Sa 31“ … für 14 Tage. Ein Punkt zeigt, dass etwas stattfindet. */
@Composable
private fun DayStrip(state: EventsUiState, onSelect: (LocalDate?) -> Unit) {
    val stripDescription = stringResource(R.string.events_tag_leiste)
    LazyRow(
        modifier = Modifier.padding(bottom = 2.dp).semantics { contentDescription = stripDescription },
        contentPadding = PaddingValues(horizontal = Spacing.ScreenEdge),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "from-today") {
            DayButton(
                day = null, today = state.today, isSelected = state.selection.day == null, hasEvents = false,
                onClick = { onSelect(null) },
            )
        }
        items(state.days, key = { it.toString() }) { day ->
            DayButton(
                day = day, today = state.today, isSelected = state.selection.day == day,
                hasEvents = day in state.daysWithEvents, onClick = { onSelect(day) },
            )
        }
    }
}

@Composable
private fun DayButton(day: LocalDate?, today: LocalDate, isSelected: Boolean, hasEvents: Boolean, onClick: () -> Unit) {
    val colors = CampusTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val top = when {
        day == null -> stringResource(R.string.events_ab_heute)
        day == today -> stringResource(R.string.tag_heute)
        day == today.plusDays(1) -> stringResource(R.string.tag_morgen)
        else -> EventTimes.weekdayShort(day)
    }
    val base = if (day == null) {
        stringResource(R.string.events_alles_ab_heute)
    } else {
        EventTimes.sectionTitle(day, today).asString()
    }
    val description = if (hasEvents) stringResource(R.string.events_mit_veranstaltungen, base) else base
    val content = if (isSelected) colors.onButtonPrimary else colors.textPrimary

    Column(
        modifier = Modifier
            .widthIn(min = 52.dp)
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (isSelected) colors.buttonPrimary else colors.surface, shape)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = top,
            style = if (day == null) {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            },
            color = content,
            maxLines = 1,
        )
        if (day != null && day >= today.plusDays(2)) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 18.sp, lineHeight = 22.sp),
                color = content,
            )
        }
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(if (hasEvents) (if (isSelected) colors.onButtonPrimary else colors.accent) else androidx.compose.ui.graphics.Color.Transparent, CircleShape),
        )
    }
}

// MARK: - Chipleiste

/** Chipleiste: „Heute Abend“, „Gemerkt“, Trenner und je Veranstalter ein Chip mit der Zahl kommender Veranstaltungen. */
@Composable
private fun ChipBar(
    state: EventsUiState,
    onToggleEvening: () -> Unit,
    onToggleBookmarked: () -> Unit,
    onToggleOrganizer: (EventOrganizer) -> Unit,
) {
    val colors = CampusTheme.colors
    LazyRow(
        modifier = Modifier.padding(bottom = 4.dp),
        contentPadding = PaddingValues(horizontal = Spacing.ScreenEdge),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "evening") {
            Pill(
                title = stringResource(R.string.heute_abend_titel), icon = Icons.Outlined.DarkMode,
                isActive = state.selection.eveningOnly, onClick = onToggleEvening,
            )
        }
        item(key = "bookmarked") {
            Pill(
                title = stringResource(R.string.events_gemerkt), icon = Icons.Filled.Star,
                isActive = state.selection.onlyBookmarked, onClick = onToggleBookmarked,
            )
        }
        if (state.organizers.isNotEmpty()) {
            item(key = "divider") {
                Box(modifier = Modifier.padding(horizontal = 2.dp).width(1.dp).heightIn(min = 26.dp).background(colors.separator))
            }
        }
        items(state.organizers, key = { "o-" + it.id }) { organizer ->
            val description = stringResource(R.string.events_veranstalter_kommende, organizer.name, organizer.upcomingCount)
            Pill(
                title = organizer.name, count = organizer.upcomingCount,
                isActive = state.selection.organizerId == organizer.id,
                onClick = { onToggleOrganizer(organizer) },
                description = description,
            )
        }
    }
}

@Composable
private fun Pill(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    count: Int? = null,
    description: String? = null,
) {
    val colors = CampusTheme.colors
    Box(
        modifier = Modifier
            .heightIn(min = Spacing.MinTouchTarget)
            .selectable(selected = isActive, role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 34.dp)
                .background(if (isActive) colors.buttonPrimary else colors.surface, CircleShape)
                .border(1.dp, if (isActive) colors.buttonPrimary else colors.separator, CircleShape)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val content = if (isActive) colors.onButtonPrimary else colors.textPrimary
            if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = content,
                maxLines = 1,
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isActive) colors.onButtonPrimary.copy(alpha = 0.7f) else colors.textTertiary,
                )
            }
        }
    }
}

// MARK: - Liste

@Composable
private fun EventsList(
    state: EventsUiState,
    onReset: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleBookmark: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenPast: (() -> Unit)?,
) {
    val colors = CampusTheme.colors
    val sections = state.sections
    val lastId = sections.lastOrNull()?.events?.lastOrNull()?.id

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, bottom = Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (state.loadState) {
            EventsLoadState.Loading -> item(key = "placeholder") { Placeholder() }

            EventsLoadState.Failed -> item(key = "failed") {
                EventsMessage(
                    title = stringResource(R.string.events_fehler_titel),
                    text = stringResource(R.string.events_fehler_text),
                    actionText = stringResource(R.string.erneut_versuchen),
                    primary = true,
                    onAction = onRetry,
                )
            }

            EventsLoadState.Loaded -> {
                if (sections.isEmpty()) {
                    item(key = "empty") { EmptyMessage(state = state, onReset = onReset) }
                } else {
                    sections.forEach { section ->
                        item(key = "h-" + section.day) {
                            SectionHeader(section.day, state, count = if (onOpenPast != null) null else section.events.size)
                        }
                        items(section.events, key = { section.day.toString() + "/" + it.id }) { event ->
                            EventCard(
                                event = event,
                                referenceDay = section.day,
                                isBookmarked = event.id in state.bookmarks,
                                onClick = { onOpenEvent(event.id) },
                                onToggleBookmark = { onToggleBookmark(event.id) },
                            )
                            if (event.id == lastId) LaunchedEffect(event.id, state.events.size) { onLoadMore() }
                        }
                    }
                    item(key = "footer") {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            if (state.loadingMore) {
                                GoatLoader(size = 28.dp, modifier = Modifier.heightIn(min = 44.dp).padding(top = 6.dp))
                            } else if (state.hasMore && state.mode == EventsMode.Upcoming) {
                                Text(
                                    text = stringResource(R.string.events_weitere),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textTertiary,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                            if (onOpenPast != null) {
                                SecondaryButton(
                                    text = stringResource(R.string.events_vergangene_button),
                                    onClick = onOpenPast,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Titel des Tagesabschnitts, in „Vergangene“ mit der Zahl der Veranstaltungen. */
@Composable
private fun SectionHeader(day: LocalDate, state: EventsUiState, count: Int?) {
    val colors = CampusTheme.colors
    val title = if (state.mode == EventsMode.Past) EventTimes.pastSectionTitle(day, state.today) else EventTimes.sectionTitle(day, state.today)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 12.dp).semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.asString(),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        // Der Zähler gehört nur zur Liste von „Vergangene“ (dort ohne „Vergangene“-Link).
        if (count != null && state.mode == EventsMode.Past) {
            Text(
                text = pluralStringResource(R.plurals.events_anzahl, count, count),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textTertiary,
            )
        }
    }
}

/** Graue Platzhalter-Karten mit demselben Aufbau wie die echten. */
@Composable
private fun Placeholder() {
    val colors = CampusTheme.colors
    val description = stringResource(R.string.events_laedt)
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(4) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth(0.35f).heightIn(min = 12.dp).background(colors.surfaceSecondary, CircleShape))
                Box(modifier = Modifier.fillMaxWidth(0.8f).heightIn(min = 16.dp).background(colors.surfaceSecondary, CircleShape))
                Box(modifier = Modifier.fillMaxWidth(0.5f).heightIn(min = 12.dp).background(colors.surfaceSecondary, CircleShape))
            }
        }
    }
}

@Composable
private fun EmptyMessage(state: EventsUiState, onReset: () -> Unit) {
    when (val kind = state.selection.emptyKind(state.now)) {
        EventsSelection.EmptyKind.Bookmarked -> EventsMessage(
            title = stringResource(R.string.events_leer_gemerkt_titel),
            text = stringResource(R.string.events_leer_gemerkt_text),
            actionText = stringResource(R.string.events_leer_alle_anzeigen),
            primary = false,
            onAction = onReset,
        )

        is EventsSelection.EmptyKind.Evening -> EventsMessage(
            title = stringResource(if (kind.today) R.string.events_leer_abend_heute else R.string.events_leer_abend_tag),
            text = stringResource(R.string.events_leer_abend_text),
            actionText = stringResource(R.string.events_leer_abend_aktion),
            primary = false,
            onAction = onReset,
        )

        EventsSelection.EmptyKind.General -> EventsMessage(
            title = stringResource(R.string.events_leer_titel),
            text = stringResource(if (state.mode == EventsMode.Past) R.string.events_leer_vergangene_text else R.string.events_leer_text),
            actionText = if (state.mode == EventsMode.Past && state.selection.sheetCount == 0) null else stringResource(R.string.events_filter_zuruecksetzen),
            primary = false,
            onAction = onReset,
        )
    }
}

/** Mitteilung mitten in der Liste: Titel, Text und ein Button. Für Leer-Zustände und den Fehler. */
@Composable
private fun EventsMessage(title: String, text: String, actionText: String?, primary: Boolean, onAction: () -> Unit) {
    val colors = CampusTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (actionText != null) {
            Box(modifier = Modifier.padding(top = 12.dp)) {
                if (primary) PrimaryButton(text = actionText, onClick = onAction) else SecondaryButton(text = actionText, onClick = onAction)
            }
        }
    }
}

// MARK: - Vorschauen

private val PreviewDay = LocalDate.of(2026, 11, 24)
private val PreviewNow: Instant = BerlinTime.at(PreviewDay, 11 * 60)

private fun previewEvent(id: String, day: LocalDate, hour: Int, title: String, status: EventStatus = EventStatus.Scheduled, category: String? = null) =
    CampusEvent(
        id = id, title = title, start = BerlinTime.at(day, hour * 60), end = BerlinTime.at(day, (hour + 3) * 60),
        status = status, location = "Club Ilmenau", organizer = "Studentenclub", category = category,
    )

private fun previewState(mode: EventsMode = EventsMode.Upcoming, loadState: EventsLoadState = EventsLoadState.Loaded) = EventsUiState(
    mode = mode,
    loadState = loadState,
    events = listOf(
        previewEvent("a", PreviewDay, 19, "Konzert im Club"),
        previewEvent("b", PreviewDay, 21, "Spieleabend", EventStatus.Cancelled),
        previewEvent("c", PreviewDay.plusDays(1), 18, "Rückmeldefrist", EventStatus.Scheduled, "Frist"),
        previewEvent("d", PreviewDay.plusDays(3), 20, "Poetry Slam", EventStatus.Tentative),
    ),
    organizers = listOf(EventOrganizer("club", "Studentenclub", 3), EventOrganizer("bohne", "Café Bohne", 2)),
    daysWithEvents = setOf(PreviewDay, PreviewDay.plusDays(1), PreviewDay.plusDays(3)),
    bookmarks = setOf("a"),
    hasMore = true,
    now = PreviewNow,
)

@Composable
private fun PreviewHost(dark: Boolean, state: EventsUiState) {
    CampusIlmenauTheme(darkTheme = dark) {
        EventsContent(
            state = state, onBack = null, onSelect = {}, onToggleEvening = {}, onReset = {}, onRetry = {},
            onLoadMore = {}, onToggleBookmark = {}, onOpenEvent = {}, onOpenPast = {},
        )
    }
}

@Preview(name = "Events Light", showBackground = true, heightDp = 800)
@Composable
private fun EventsLightPreview() = PreviewHost(false, previewState())

@Preview(name = "Events Dark", showBackground = true, heightDp = 800, backgroundColor = 0xFF0E1A2E)
@Composable
private fun EventsDarkPreview() = PreviewHost(true, previewState())

@Preview(name = "Events Laden", showBackground = true, heightDp = 800)
@Composable
private fun EventsLoadingPreview() = PreviewHost(false, previewState(loadState = EventsLoadState.Loading))

@Preview(name = "Events Leer Heute Abend", showBackground = true, heightDp = 800)
@Composable
private fun EventsEmptyPreview() = PreviewHost(
    false,
    previewState().copy(events = emptyList(), selection = EventsSelection.evening(PreviewNow), hasMore = false),
)

@Preview(name = "Events Vergangene", showBackground = true, heightDp = 800)
@Composable
private fun EventsPastPreview() = PreviewHost(false, previewState(EventsMode.Past).copy(selection = EventsSelection(kind = EventKind.All)))
