package de.kaigothe.campusilmenau.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventStatus
import de.kaigothe.campusilmenau.domain.model.events.EventTimes
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.components.ChipVariant
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import java.time.LocalDate

/**
 * Karte einer Veranstaltung in der Liste: ohne Bild und Logo, nur Text. Tippen öffnet das Detail, der Stern rechts merkt
 * die Veranstaltung. Für TalkBack ist die Karte ein Element, „Merken“ bzw. „Nicht mehr merken“ ist eine zusätzliche Aktion.
 * [referenceDay] ist der Tag, unter dem die Karte steht: Mehrtägige, die vorher begonnen haben, „laufen seit“.
 */
@Composable
fun EventCard(
    event: CampusEvent,
    referenceDay: LocalDate,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CampusTheme.colors
    val cancelled = event.status == EventStatus.Cancelled
    val timeLine = EventTimes.timeLine(event, referenceDay).asString()
    val place = listOfNotNull(event.location, event.organizerForDisplay).filter { it.isNotEmpty() }.joinToString(" · ").takeIf { it.isNotEmpty() }

    val cancelledText = stringResource(R.string.events_sprech_faellt_aus)
    val tentativeText = stringResource(R.string.events_sprech_unbestaetigt)
    val deadlineText = stringResource(R.string.events_chip_frist)
    val bookmarkedText = stringResource(R.string.events_sprech_gemerkt)
    val spoken = buildList {
        add(event.title)
        add(timeLine)
        place?.let(::add)
        when (event.status) {
            EventStatus.Cancelled -> add(cancelledText)
            EventStatus.Tentative -> add(tentativeText)
            EventStatus.Scheduled -> Unit
        }
        if (event.isDeadline) add(deadlineText)
        if (isBookmarked) add(bookmarkedText)
    }.joinToString(", ")
    val actionLabel = stringResource(if (isBookmarked) R.string.events_nicht_mehr_merken else R.string.events_merken)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = spoken
                role = Role.Button
                customActions = listOf(CustomAccessibilityAction(actionLabel) { onToggleBookmark(); true })
            },
    ) {
        CampusCard(horizontalPadding = 0.dp, verticalPadding = 12.dp, onClick = onClick) {
            Column(
                // Platz für den Stern (48 dp) rechts.
                modifier = Modifier.padding(start = 16.dp, end = 52.dp).clearAndSetSemantics {},
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = timeLine,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary,
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (cancelled) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    color = if (cancelled) colors.textTertiary else colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (place != null) {
                    Text(
                        text = place,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val chips = statusChips(event)
                if (chips.isNotEmpty()) {
                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        chips.forEach { (text, variant) -> CampusChip(text = text, variant = variant) }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 2.dp)
                .size(48.dp)
                .clickable(role = Role.Checkbox, onClick = onToggleBookmark)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (isBookmarked) colors.accent else colors.textTertiary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Chips nur bei Abweichung. „Veranstaltung“ bekommt keinen. */
@Composable
private fun statusChips(event: CampusEvent): List<Pair<String, ChipVariant>> = buildList {
    when (event.status) {
        EventStatus.Cancelled -> add(stringResource(R.string.events_chip_faellt_aus) to ChipVariant.DangerSoft)
        EventStatus.Tentative -> add(stringResource(R.string.events_chip_unbestaetigt) to ChipVariant.Neutral)
        EventStatus.Scheduled -> Unit
    }
    if (event.isDeadline) add(stringResource(R.string.events_chip_frist) to ChipVariant.Blue)
}
