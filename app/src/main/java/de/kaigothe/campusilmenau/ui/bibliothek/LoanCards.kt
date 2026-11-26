package de.kaigothe.campusilmenau.ui.bibliothek

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.domain.model.library.HoldInfo
import de.kaigothe.campusilmenau.domain.model.library.LoanInfo
import de.kaigothe.campusilmenau.domain.model.library.LoanUrgency
import de.kaigothe.campusilmenau.domain.model.library.PaiaLoanItem
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.components.SecondaryButton
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.CardShape
import java.time.LocalDate

/** Titel (höchstens 2 Zeilen) und Signatur links, Chip rechts. */
@Composable
private fun ItemTitleRow(title: String, signature: String?, chip: @Composable () -> Unit) {
    val colors = CampusTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (signature != null) {
                Text(text = signature, style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
            }
        }
        Box(modifier = Modifier.padding(top = 2.dp)) { chip() }
    }
}

/**
 * Karte einer laufenden Ausleihe: Titel, Signatur, Fälligkeits-Chip, Zeitleiste und Fußzeile mit „Verlängern“.
 * [onOpen] öffnet die Details des Mediums, `null`, wenn der Titel im Katalog nicht auffindbar ist. Das Wischen zum
 * Verlängern der iOS-App gibt es nicht: Der Button mit Rückfrage ist der Weg (auch für TalkBack).
 */
@Composable
fun LoanCard(item: PaiaLoanItem, today: LocalDate, onRenew: () -> Unit, onOpen: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val info = LoanInfo(item, today)
    var confirming by rememberSaveable { mutableStateOf(false) }
    val title = item.displayTitle ?: stringResource(R.string.bib_unbekanntes_medium)
    val timeline = info.timelineLabel.asString()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        ItemTitleRow(title = title, signature = item.signature) {
            info.dueText?.let { CampusChip(text = it.asString(), variant = info.urgency.chipVariant()) }
        }
        if (info.days != null) {
            LoanTimeline(info = info, description = timeline, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp))
        } else {
            Box(modifier = Modifier.height(10.dp))
        }
        Row(
            modifier = Modifier.heightIn(min = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = info.metaText.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.weight(1f),
            )
            if (info.canRenew) {
                SecondaryButton(text = stringResource(R.string.bib_verlaengern), onClick = { confirming = true }, compact = true)
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.bib_verlaengern_titel)) },
            text = { Text(stringResource(R.string.bib_verlaengern_text, title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onRenew()
                }) { Text(stringResource(R.string.bib_verlaengern), color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.abbrechen), color = colors.accent) }
            },
        )
    }
}

/**
 * Zeitleiste einer laufenden Ausleihe (Standard-Leihfrist 3 Wochen pro Periode). Links ein Punkt für die Erstausleihe und je
 * einer pro Verlängerung, dann der Strich mit dem Fortschritt der aktuellen Leihperiode, rechts der Fälligkeitspunkt (bei
 * überfällig gefüllt). Strich und Fälligkeitspunkt haben die Farbe der Dringlichkeit.
 */
@Composable
fun LoanTimeline(info: LoanInfo, description: String, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val tint = when (info.urgency) {
        LoanUrgency.Normal -> colors.textSecondary
        LoanUrgency.Soon -> colors.warningFill
        LoanUrgency.Urgent -> colors.danger
    }
    Row(
        modifier = modifier.fillMaxWidth().height(10.dp).semantics { contentDescription = description; role = Role.Image },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(info.renewals + 1) {
                Box(modifier = Modifier.size(6.dp).background(colors.textTertiary, CircleShape))
            }
        }
        Canvas(modifier = Modifier.weight(1f).height(4.dp).clearAndSetSemantics {}) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(colors.surfaceSecondary, size = size, cornerRadius = radius)
            drawRoundRect(tint, size = Size(size.width * info.progress.toFloat(), size.height), cornerRadius = radius)
        }
        Canvas(modifier = Modifier.size(10.dp).clearAndSetSemantics {}) {
            val stroke = 2.dp.toPx()
            if (info.isOverdue) drawCircle(tint)
            drawCircle(tint, radius = (size.minDimension - stroke) / 2f, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(stroke))
        }
    }
}

/** Karte für Vormerkungen, Bestellungen und Bereitstellungen: Titel, Signatur, Status-Chip und Zusatzzeile, wenn der Server Daten liefert. */
@Composable
fun HoldCard(item: PaiaLoanItem, onOpen: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val info = HoldInfo(item)
    val title = item.displayTitle ?: stringResource(R.string.bib_unbekanntes_medium)
    val separator = stringResource(R.string.bib_zusammenfassung_trenner)
    val detail = info.detail.map { it.asString() }.joinToString(separator)
    CampusCard(modifier = modifier.semantics(mergeDescendants = true) {}, onClick = onOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ItemTitleRow(title = title, signature = item.signature) {
                CampusChip(text = info.statusText.asString(), variant = info.tone.chipVariant())
            }
            if (detail.isNotEmpty()) {
                Text(text = detail, style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
            }
        }
    }
}
