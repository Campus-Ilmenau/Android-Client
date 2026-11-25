package de.kaigothe.campusilmenau.ui.mensa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.mensa.MensaDiet
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFormatting
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealtime
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.components.GoatLoadingBlock
import de.kaigothe.campusilmenau.ui.components.MessageCard
import de.kaigothe.campusilmenau.ui.components.SegmentOption
import de.kaigothe.campusilmenau.ui.components.SegmentedControl
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.floor
import kotlin.math.roundToInt

/** Wie der Ladebereich gerade aussieht (aus dem `LoadingGate` des ViewModels). */
data class LoadingUi(val isActive: Boolean = false, val isVisible: Boolean = false, val isSlow: Boolean = false)

/**
 * Der Tab „Mensa“: feststehender Kopf, Namensleiste, Tagesleiste, Filter und darunter eine Seite pro Mensa zum
 * horizontalen Wischen. [onOpenMeal] bekommt das Gericht und seinen Tag.
 */
@Composable
fun MensaScreen(
    onOpenMeal: (MensaMeal, LocalDate) -> Unit,
    onOpenHours: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MensaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val isActive by viewModel.loadingGate.isActive.collectAsStateWithLifecycle()
    val isVisible by viewModel.loadingGate.isVisible.collectAsStateWithLifecycle()
    val isSlow by viewModel.loadingGate.isSlow.collectAsStateWithLifecycle()

    MensaContent(
        state = state,
        now = now,
        loading = LoadingUi(isActive, isVisible, isSlow),
        onSelectDay = viewModel::selectDay,
        onSelectVenue = viewModel::selectVenue,
        onSelectMealtime = viewModel::selectMealtime,
        onSetFilter = viewModel::setFilter,
        onResetFilter = viewModel::resetFilter,
        onRetry = viewModel::retry,
        onOpenMeal = { meal -> onOpenMeal(meal, state.selectedDay) },
        onOpenHours = onOpenHours,
        modifier = modifier,
    )
}

/** Oberfläche ohne ViewModel, damit sie sich in Vorschauen in jedem Zustand zeigen lässt. */
@Composable
fun MensaContent(
    state: MensaUiState,
    now: LocalDateTime,
    loading: LoadingUi,
    onSelectDay: (Int) -> Unit,
    onSelectVenue: (MensaVenue) -> Unit,
    onSelectMealtime: (MensaMealtime) -> Unit,
    onSetFilter: (MensaFilter) -> Unit,
    onResetFilter: () -> Unit,
    onRetry: () -> Unit,
    onOpenMeal: (MensaMeal) -> Unit,
    onOpenHours: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilter by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Header(onOpenHours = onOpenHours)

        // Erst wenn gewählte Mensa und Filter gelesen sind, damit nichts springt.
        if (state.ready) {
            val pagerState = rememberPagerState(initialPage = state.venue.ordinal) { MensaVenue.entries.size }

            // Wahl in der Namensleiste (oder von außen) bewegt den Pager, Wischen wählt die Mensa nach dem Einrasten.
            LaunchedEffect(state.venue) {
                if (pagerState.currentPage != state.venue.ordinal) pagerState.animateScrollToPage(state.venue.ordinal)
            }
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }.collect { page -> onSelectVenue(MensaVenue.entries[page]) }
            }

            VenueStrip(state = state, pagerState = pagerState, onSelect = onSelectVenue)
            DayStrip(state = state, today = now.toLocalDate(), onSelect = onSelectDay)
            FilterBar(filter = state.filter, onSetFilter = onSetFilter, onOpenFilter = { showFilter = true })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                key = { MensaVenue.entries[it].key },
            ) { page ->
                MensaVenuePage(
                    venue = MensaVenue.entries[page],
                    state = state,
                    now = now,
                    loading = loading,
                    onSelectMealtime = onSelectMealtime,
                    onRetry = onRetry,
                    onResetFilter = onResetFilter,
                    onOpenMeal = onOpenMeal,
                )
            }
        }
    }

    if (showFilter) {
        MensaFilterSheet(
            filter = state.filter,
            allergens = state.allergenOptions,
            onFilterChange = onSetFilter,
            onDismiss = { showFilter = false },
        )
    }
}

// MARK: - Kopf

@Composable
private fun Header(onOpenHours: () -> Unit) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.ScreenEdge, end = Spacing.Sm, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.tab_mensa),
            style = MaterialTheme.typography.displayMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        // Das Teilen des Speiseplans kommt mit M8 hierher, links neben die Uhr.
        Box(
            modifier = Modifier
                .size(Spacing.MinTouchTarget)
                .clickable(role = Role.Button, onClick = onOpenHours),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(colors.surfaceSecondary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = stringResource(R.string.mensa_zeiten_oeffnen),
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

// MARK: - Namensleiste

private class TabFrame(val x: Float, val width: Float)

@Composable
private fun VenueStrip(state: MensaUiState, pagerState: PagerState, onSelect: (MensaVenue) -> Unit) {
    val colors = CampusTheme.colors
    val scrollState = rememberScrollState()
    val frames = remember { mutableStateMapOf<Int, TabFrame>() }
    var viewportWidth by remember { mutableStateOf(0) }
    val showCounts = state.filter.isActive && state.loadState == MensaLoadState.Loaded

    // Die gewählte Mensa bleibt in der Mitte der Leiste, wenn sie breiter ist als der Bildschirm.
    val selectedFrame = frames[state.venue.ordinal]
    LaunchedEffect(state.venue, selectedFrame, viewportWidth) {
        selectedFrame?.let { frame ->
            val target = (frame.x + frame.width / 2f - viewportWidth / 2f).roundToInt().coerceAtLeast(0)
            scrollState.animateScrollTo(target)
        }
    }

    val underlineHeight = with(LocalDensity.current) { 3.dp.roundToPx() }
    val stripDescription = stringResource(R.string.mensa_waehlen)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .onGloballyPositioned { viewportWidth = it.size.width }
            .horizontalScroll(scrollState)
            .padding(horizontal = 10.dp)
            .semantics { contentDescription = stripDescription },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MensaVenue.entries.forEach { venue ->
                val count = if (showCounts) state.mealsFor(venue).size else null
                VenueTab(
                    venue = venue,
                    isSelected = venue == state.venue,
                    count = count,
                    onClick = { onSelect(venue) },
                    modifier = Modifier.onGloballyPositioned {
                        frames[venue.ordinal] = TabFrame(it.positionInParent().x, it.size.width.toFloat())
                    },
                )
            }
        }
        // 3 dp in accent unter dem aktiven Namen. Beim Wischen wandert sie mit dem Finger: Breite und Lage
        // wechseln fließend zwischen den beiden Namen. Das passiert erst beim Platzieren, ohne neu zu komponieren.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .layout { measurable, constraints ->
                    val progress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        .coerceIn(0f, (MensaVenue.entries.size - 1).toFloat())
                    val lower = floor(progress).toInt()
                    val upper = minOf(lower + 1, MensaVenue.entries.size - 1)
                    val fraction = progress - lower
                    val a = frames[lower]
                    val b = frames[upper]
                    if (a == null || b == null) {
                        layout(0, 0) {}
                    } else {
                        val width = (a.width + (b.width - a.width) * fraction).roundToInt()
                        val x = (a.x + (b.x - a.x) * fraction).roundToInt()
                        val placeable = measurable.measure(Constraints.fixed(width, underlineHeight))
                        layout(constraints.minWidth, underlineHeight) { placeable.place(x, 0) }
                    }
                }
                .background(colors.accent),
        )
    }
}

@Composable
private fun VenueTab(
    venue: MensaVenue,
    isSelected: Boolean,
    count: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CampusTheme.colors
    val countText = count?.let {
        if (it == 0) stringResource(R.string.mensa_keins_passend) else stringResource(R.string.mensa_passend, it)
    }
    val description = when (count) {
        null -> venue.displayName
        0 -> stringResource(R.string.mensa_tab_keins_passend, venue.displayName)
        else -> stringResource(R.string.mensa_tab_passend, venue.displayName, count)
    }
    Column(
        modifier = modifier
            .heightIn(min = Spacing.MinTouchTarget)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 9.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = venue.shortName,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isSelected) colors.textPrimary else colors.textTertiary,
            maxLines = 1,
        )
        if (countText != null) {
            Text(text = countText, style = MaterialTheme.typography.labelSmall, color = colors.textTertiary, maxLines = 1)
        }
    }
}

// MARK: - Tagesleiste

@Composable
private fun DayStrip(state: MensaUiState, today: LocalDate, onSelect: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, top = 10.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(count = state.days.size, key = { state.days[it].toString() }) { index ->
            DayButton(
                day = state.days[index],
                isToday = state.days[index] == today,
                isSelected = index == state.selectedDayIndex,
                startsWeek = index > 0 && state.days[index].dayOfWeek == java.time.DayOfWeek.MONDAY,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun DayButton(day: LocalDate, isToday: Boolean, isSelected: Boolean, startsWeek: Boolean, onClick: () -> Unit) {
    val colors = CampusTheme.colors
    val spoken = MensaFormatting.spokenDay(day)
    val description = if (isToday) stringResource(R.string.mensa_tag_heute_sprechen, spoken) else spoken
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .widthIn(min = 52.dp)
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(if (isSelected) colors.buttonPrimary else colors.surface, shape)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = if (isToday) stringResource(R.string.tag_heute) else MensaFormatting.weekdayShort(day),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            color = when {
                isSelected -> colors.onButtonPrimary
                startsWeek -> colors.textTertiary
                else -> colors.textPrimary
            },
        )
        Text(
            text = MensaFormatting.dayOfMonth(day),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            color = if (isSelected) colors.onButtonPrimary else colors.textPrimary,
        )
    }
}

// MARK: - Filter

@Composable
private fun FilterBar(filter: MensaFilter, onSetFilter: (MensaFilter) -> Unit, onOpenFilter: () -> Unit) {
    val colors = CampusTheme.colors
    val summary = filter.summary()
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.ScreenEdge).padding(bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MensaDiet.entries.forEach { diet ->
                    DietPill(diet = diet, isSelected = filter.diet == diet, onClick = { onSetFilter(filter.copy(diet = diet)) })
                }
            }
            FilterButton(count = filter.extraCount, onClick = onOpenFilter)
        }
        if (summary != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpenFilter) {
                    Text(
                        text = stringResource(R.string.filter_aendern),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun DietPill(diet: MensaDiet, isSelected: Boolean, onClick: () -> Unit) {
    val colors = CampusTheme.colors
    Box(
        modifier = Modifier
            .heightIn(min = Spacing.MinTouchTarget)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = diet.title(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isSelected) colors.onButtonPrimary else colors.textSecondary,
            maxLines = 1,
            modifier = Modifier
                .background(if (isSelected) colors.buttonPrimary else colors.surfaceSecondary, CircleShape)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

/** Runde Taste (36 dp, Tippfläche 48 dp). Sind „Nur Bio“ oder Allergene gesetzt, ist sie gefüllt und zeigt die Anzahl. */
@Composable
private fun FilterButton(count: Int, onClick: () -> Unit) {
    val colors = CampusTheme.colors
    val description = if (count > 0) stringResource(R.string.filter_aktiv, count) else stringResource(R.string.filter_titel)
    Box(
        modifier = Modifier
            .size(Spacing.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(if (count > 0) colors.buttonPrimary else colors.surfaceSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = null,
                tint = if (count > 0) colors.onButtonPrimary else colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (count > 0) {
            FilterCountBadge(count = count, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp))
        }
    }
}

/** Zahl in `accent` an der Filter-Taste. Im Dark Mode ist `accent` hell, deshalb steht dort dunkle Schrift darauf. */
@Composable
internal fun FilterCountBadge(count: Int, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = if (isDark) colors.brandNight else colors.onBlue,
        modifier = modifier
            .widthIn(min = 18.dp)
            .heightIn(min = 18.dp)
            .background(colors.accent, CircleShape)
            .padding(horizontal = 5.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

// MARK: - Seite einer Mensa

@Composable
private fun MensaVenuePage(
    venue: MensaVenue,
    state: MensaUiState,
    now: LocalDateTime,
    loading: LoadingUi,
    onSelectMealtime: (MensaMealtime) -> Unit,
    onRetry: () -> Unit,
    onResetFilter: () -> Unit,
    onOpenMeal: (MensaMeal) -> Unit,
) {
    // Erstes Laden ohne Daten: nur die Ziege mittig im freien Bereich unter den Filtern. Die Wahl Mittag/Abendessen
    // bleibt erreichbar, Name und Status der Mensa kommen mit den Daten.
    if (loading.isActive) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (venue.hasMealtimes) {
                MealtimeControl(
                    selected = state.mealtime,
                    onSelect = onSelectMealtime,
                    modifier = Modifier.padding(horizontal = Spacing.ScreenEdge).padding(bottom = 12.dp),
                )
            }
            GoatLoadingBlock(
                message = stringResource(R.string.mensa_laedt),
                isVisible = loading.isVisible,
                isSlow = loading.isSlow,
            )
        }
        return
    }

    val colors = CampusTheme.colors
    val meals = state.mealsFor(venue)
    val isToday = state.selectedDay == now.toLocalDate()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, bottom = Spacing.Xl),
    ) {
        item(key = "header") { PageHeader(venue = venue, state = state, now = now) }
        if (venue.hasMealtimes) {
            item(key = "mealtime") {
                MealtimeControl(
                    selected = state.mealtime,
                    onSelect = onSelectMealtime,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }

        when (val load = state.loadState) {
            // Zeigt die Ziege oben; dieser Zweig gilt nur für den einen Frame, bis das `LoadingGate` nachzieht.
            MensaLoadState.Loading -> Unit

            is MensaLoadState.Failed -> item(key = "failed") {
                MessageCard(
                    title = stringResource(R.string.mensa_fehler_titel),
                    text = load.error.mensaMessage(),
                    buttonText = stringResource(R.string.erneut_versuchen),
                    onButtonClick = onRetry,
                )
            }

            MensaLoadState.Loaded -> when {
                meals.isNotEmpty() -> {
                    items(meals, key = { it.id }) { meal ->
                        MensaMealCard(
                            meal = meal,
                            priceGroup = state.priceGroup,
                            onClick = { onOpenMeal(meal) },
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    item(key = "footnote") {
                        Text(
                            text = stringResource(R.string.mensa_preishinweis, state.priceGroup.title()),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 4.dp),
                        )
                    }
                }

                state.mealsFor(venue, filtered = false).isNotEmpty() -> item(key = "filterEmpty") {
                    MessageCard(
                        title = stringResource(R.string.mensa_filter_leer_titel),
                        text = stringResource(
                            if (isToday) R.string.mensa_filter_leer_heute else R.string.mensa_filter_leer_tag,
                            state.filter.activeDescription(),
                        ),
                        buttonText = stringResource(R.string.filter_zuruecksetzen),
                        onButtonClick = onResetFilter,
                    )
                }

                else -> item(key = "empty") {
                    CampusCard {
                        Text(
                            text = stringResource(if (isToday) R.string.mensa_leer_heute else R.string.mensa_leer_tag),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealtimeControl(selected: MensaMealtime, onSelect: (MensaMealtime) -> Unit, modifier: Modifier = Modifier) {
    SegmentedControl(
        options = listOf(
            SegmentOption(MensaMealtime.Lunch, stringResource(R.string.mittag)),
            SegmentOption(MensaMealtime.Dinner, stringResource(R.string.abendessen)),
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
private fun PageHeader(venue: MensaVenue, state: MensaUiState, now: LocalDateTime) {
    val colors = CampusTheme.colors
    val status = state.status(venue, now)
    val dayText = dayLabel(state.selectedDay, now.toLocalDate())
    val subtitle = when {
        !venue.hasMealtimes -> dayText
        state.mealtime == MensaMealtime.Dinner -> stringResource(R.string.mensa_untertitel_abend, dayText)
        else -> stringResource(R.string.mensa_untertitel_mittag, dayText)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = venue.displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        if (status != null) {
            CampusChip(text = status.text(), variant = status.tone.variant())
        }
    }
}

// MARK: - Vorschauen

private val PreviewDays = MensaFormatting.workdays(LocalDate.of(2026, 11, 24))
private val PreviewNow: LocalDateTime = LocalDateTime.of(2026, 11, 24, 12, 15)

private fun previewState(
    loadState: MensaLoadState = MensaLoadState.Loaded,
    filter: MensaFilter = MensaFilter(),
    meals: List<MensaMeal> = listOf(MensaPreviewData.sample, MensaPreviewData.minimal),
) = MensaUiState(
    ready = true,
    days = PreviewDays,
    loadState = loadState,
    meals = meals,
    filter = filter,
)

@Composable
private fun PreviewHost(darkTheme: Boolean, state: MensaUiState, loading: LoadingUi = LoadingUi()) {
    CampusIlmenauTheme(darkTheme = darkTheme) {
        MensaContent(
            state = state, now = PreviewNow, loading = loading,
            onSelectDay = {}, onSelectVenue = {}, onSelectMealtime = {}, onSetFilter = {}, onResetFilter = {},
            onRetry = {}, onOpenMeal = {}, onOpenHours = {},
        )
    }
}

@Preview(name = "Mensa Light", showBackground = true, heightDp = 780)
@Composable
private fun MensaLightPreview() = PreviewHost(darkTheme = false, state = previewState())

@Preview(name = "Mensa Dark", showBackground = true, heightDp = 780, backgroundColor = 0xFF0E1A2E)
@Composable
private fun MensaDarkPreview() = PreviewHost(darkTheme = true, state = previewState())

@Preview(name = "Mensa Filter aktiv", showBackground = true, heightDp = 780)
@Composable
private fun MensaFilterPreview() = PreviewHost(
    darkTheme = false,
    state = previewState(filter = MensaFilter(MensaDiet.Vegetarisch, onlyBio = true, excludedAllergens = setOf("Gluten"))),
)

@Preview(name = "Mensa Laden", showBackground = true, heightDp = 780)
@Composable
private fun MensaLoadingPreview() = PreviewHost(
    darkTheme = false,
    state = previewState(loadState = MensaLoadState.Loading, meals = emptyList()),
    loading = LoadingUi(isActive = true, isVisible = true),
)
