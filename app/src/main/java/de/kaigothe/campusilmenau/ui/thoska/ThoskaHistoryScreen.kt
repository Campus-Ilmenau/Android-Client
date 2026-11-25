package de.kaigothe.campusilmenau.ui.thoska

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaCardNaming
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaCardOption
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaChartBar
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaFormatting
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaHistoryEntry
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaHistoryRange
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaHistorySummary
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaReading
import de.kaigothe.campusilmenau.ui.components.BackBar
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.SegmentOption
import de.kaigothe.campusilmenau.ui.components.SegmentedControl
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate

/** Verlauf der Thoska-Auslesungen: Ausgaben, Balkendiagramm und Liste. Die Daten liegen nur auf dem Gerät. */
@Composable
fun ThoskaHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThoskaHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ThoskaHistoryContent(
        state = state,
        onBack = onBack,
        onSelectCard = viewModel::selectCard,
        onRename = viewModel::rename,
        modifier = modifier,
    )
}

/** Oberfläche ohne ViewModel, damit sie sich in Vorschauen mit Beispieldaten zeigen lässt. */
@Composable
fun ThoskaHistoryContent(
    state: ThoskaHistoryState,
    onBack: () -> Unit,
    onSelectCard: (String) -> Unit,
    onRename: (cardId: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
    initialRange: ThoskaHistoryRange = ThoskaHistoryRange.TwoWeeks,
) {
    val colors = CampusTheme.colors
    var range by rememberSaveable { mutableStateOf(initialRange) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    val summary = remember(state.readings, range, state.now) { ThoskaHistorySummary(state.readings, range, state.now) }
    val selected = state.cards.firstOrNull { it.id == state.selectedCardId }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
    ) {
        BackBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.ScreenEdge)
                .padding(bottom = Spacing.Xl),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.thoska_verlauf),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp, lineHeight = 34.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() },
            )

            if (selected != null) {
                CardMenu(
                    cards = state.cards, selected = selected,
                    onSelect = onSelectCard, onRename = { renaming = true },
                )
            }

            SegmentedControl(
                options = listOf(
                    SegmentOption(ThoskaHistoryRange.TwoWeeks, stringResource(R.string.thoska_zeitraum_2w)),
                    SegmentOption(ThoskaHistoryRange.ThreeMonths, stringResource(R.string.thoska_zeitraum_3m)),
                ),
                selected = range,
                onSelect = { range = it },
            )

            if (summary.entries.isEmpty()) {
                CampusCard {
                    Text(
                        text = stringResource(R.string.thoska_verlauf_leer),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    )
                }
            } else {
                SpentCard(summary)
                Text(
                    text = stringResource(R.string.thoska_ausgelesen),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 6.dp).semantics { heading() },
                )
                ReadingsCard(summary, state.now)
            }
        }
    }

    if (renaming && selected != null) {
        RenameDialog(
            initial = selected.customName.orEmpty(),
            onSave = { name ->
                onRename(selected.id, name)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
}

// MARK: - Karte wählen und benennen

/** Bei mehreren Karten wechselt das Menü die Karte, immer kann die gewählte benannt werden. */
@Composable
private fun CardMenu(
    cards: List<ThoskaCardOption>,
    selected: ThoskaCardOption,
    onSelect: (String) -> Unit,
    onRename: () -> Unit,
) {
    val colors = CampusTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val name = displayName(selected)
    val description = stringResource(R.string.thoska_karte_beschreibung, name)
    val hint = stringResource(if (cards.size > 1) R.string.thoska_karte_hinweis_wechseln else R.string.thoska_karte_hinweis_benennen)
    Box {
        Row(
            modifier = Modifier
                .heightIn(min = Spacing.MinTouchTarget)
                .clickable(role = Role.Button) { expanded = true }
                .semantics { contentDescription = description; stateDescription = hint },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (cards.size > 1) {
                cards.forEach { card ->
                    DropdownMenuItem(
                        text = { Text(displayName(card)) },
                        trailingIcon = { if (card.id == selected.id) Icon(Icons.Outlined.Check, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onSelect(card.id)
                        },
                    )
                }
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.thoska_karte_benennen)) },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRename()
                },
            )
        }
    }
}

@Composable
private fun displayName(card: ThoskaCardOption): String = card.displayName.ifEmpty { ThoskaCardNaming.fallbackName(card.id).asString() }

// MARK: - Ausgaben und Diagramm

@Composable
private fun SpentCard(summary: ThoskaHistorySummary) {
    val colors = CampusTheme.colors
    CampusCard {
        Text(text = stringResource(R.string.thoska_ausgegeben), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
        Text(
            text = ThoskaFormatting.euro(summary.spentMilli),
            style = MaterialTheme.typography.displaySmall,
            color = colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
        BalanceChart(bars = summary.bars, description = chartDescription(summary), modifier = Modifier.padding(top = 14.dp))
        Row(modifier = Modifier.padding(top = 10.dp).clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendItem(stringResource(R.string.thoska_legende_guthaben), colors.blue)
            LegendItem(stringResource(R.string.thoska_legende_aufgeladen), colors.green)
        }
    }
}

@Composable
private fun LegendItem(title: String, color: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Text(text = title, style = MaterialTheme.typography.bodySmall, color = CampusTheme.colors.textSecondary)
    }
}

/** Beschreibung des Diagramms für TalkBack in Worten. */
@Composable
private fun chartDescription(summary: ThoskaHistorySummary): String {
    val first = summary.entries.firstOrNull() ?: return ""
    val last = summary.entries.last()
    val topUps = summary.entries.count { it.isTopUp }
    val topUpText = if (topUps == 0) stringResource(R.string.thoska_chart_keine_aufladung) else stringResource(R.string.thoska_chart_aufladungen, topUps)
    return stringResource(
        R.string.thoska_chart_beschreibung,
        summary.entries.size,
        ThoskaFormatting.spokenEuro(first.balanceMilli).asString(),
        ThoskaFormatting.spokenEuro(last.balanceMilli).asString(),
        ThoskaFormatting.spokenEuro(summary.spentMilli).asString(),
        topUpText,
    )
}

/** Balken mit dem Guthaben, grün bei Aufladung. Ohne Achsen und Raster, 120 dp hoch. */
@Composable
private fun BalanceChart(bars: List<ThoskaChartBar>, description: String, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val maxBalance = maxOf(bars.maxOfOrNull { it.balanceMilli } ?: 0L, 1_000L).toFloat()
    // Mindestens so viele Plätze, damit einzelne Balken nicht über die ganze Breite laufen; neue rechts.
    val slots = maxOf(MINIMUM_SLOTS, bars.size)
    val offset = slots - bars.size
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .semantics { contentDescription = description },
    ) {
        val slotWidth = size.width / slots
        val barWidth = slotWidth * 0.78f
        val corner = 4.dp.toPx()
        bars.forEachIndexed { index, bar ->
            // Mindesthöhe, damit auch ein leeres Guthaben als Balken sichtbar bleibt.
            val value = maxOf(bar.balanceMilli.toFloat(), maxBalance * 0.03f)
            val barHeight = size.height * (value / maxBalance)
            val left = (offset + index) * slotWidth + (slotWidth - barWidth) / 2f
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = left, top = size.height - barHeight, right = left + barWidth, bottom = size.height,
                        topLeftCornerRadius = CornerRadius(corner), topRightCornerRadius = CornerRadius(corner),
                    ),
                )
            }
            drawPath(path, if (bar.isTopUp) colors.green else colors.blue)
        }
    }
}

private const val MINIMUM_SLOTS = 6

// MARK: - Liste

@Composable
private fun ReadingsCard(summary: ThoskaHistorySummary, now: Instant) {
    val colors = CampusTheme.colors
    val entries = summary.entries.reversed()
    CampusCard(verticalPadding = 2.dp) {
        entries.forEachIndexed { index, entry ->
            ReadingRow(entry, now)
            if (index < entries.lastIndex) HorizontalDivider(color = colors.separator)
        }
    }
}

@Composable
private fun ReadingRow(entry: ThoskaHistoryEntry, now: Instant) {
    val colors = CampusTheme.colors
    val note = if (entry.isTopUp) {
        stringResource(R.string.thoska_notiz_aufgeladen, ThoskaFormatting.plusEuro(entry.lastTransactionMilli))
    } else {
        stringResource(R.string.thoska_notiz_buchung, ThoskaFormatting.minusEuro(entry.lastTransactionMilli))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ThoskaFormatting.lastRead(entry.date, now).asString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Text(text = note, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Text(
            text = ThoskaFormatting.euro(entry.balanceMilli),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
    }
}

// MARK: - Vorschauen

private val PreviewDay = LocalDate.of(2026, 11, 25)
private val PreviewNow: Instant = BerlinTime.at(PreviewDay, 12 * 60)

/** Werte und Aufladungen aus dem Mockup. */
private val ShortValues = listOf(18_200L, 15_300, 12_500, 12_500, 9_700, 6_800, 26_800, 24_000, 21_200, 21_200, 18_400, 15_500, 12_700, 12_400)

private fun twoWeeks(): List<ThoskaReading> = ShortValues.mapIndexed { index, balance ->
    val previous = if (index > 0) ShortValues[index - 1] else balance + 2_900
    ThoskaReading(
        date = BerlinTime.at(PreviewDay.minusDays((ShortValues.size - 1 - index).toLong()), 9 * 60),
        balanceMilli = balance,
        lastTransactionMilli = if (balance > previous) 20_000 else maxOf(previous - balance, 2_800),
    )
}

private val Cards = listOf(
    ThoskaCardOption("04a1b2c3d4", "Mensakarte", "Mensakarte"),
    ThoskaCardOption("aa11bb22cc", "", null),
)

@Composable
private fun PreviewHost(dark: Boolean, state: ThoskaHistoryState, range: ThoskaHistoryRange = ThoskaHistoryRange.TwoWeeks) {
    CampusIlmenauTheme(darkTheme = dark) {
        ThoskaHistoryContent(state, onBack = {}, onSelectCard = {}, onRename = { _, _ -> }, initialRange = range)
    }
}

@Preview(name = "Verlauf 2 Wochen", showBackground = true, heightDp = 1000)
@Composable
private fun HistoryTwoWeeksPreview() = PreviewHost(false, ThoskaHistoryState(Cards, Cards[0].id, twoWeeks(), PreviewNow))

@Preview(name = "Verlauf Dark", showBackground = true, heightDp = 1000, backgroundColor = 0xFF0E1A2E)
@Composable
private fun HistoryDarkPreview() = PreviewHost(true, ThoskaHistoryState(Cards, Cards[0].id, twoWeeks(), PreviewNow))

@Preview(name = "Verlauf 3 Monate", showBackground = true, heightDp = 1000)
@Composable
private fun HistoryThreeMonthsPreview() =
    PreviewHost(false, ThoskaHistoryState(Cards, Cards[0].id, twoWeeks(), PreviewNow), ThoskaHistoryRange.ThreeMonths)

@Preview(name = "Verlauf leer", showBackground = true, heightDp = 600)
@Composable
private fun HistoryEmptyPreview() = PreviewHost(false, ThoskaHistoryState(now = PreviewNow))
