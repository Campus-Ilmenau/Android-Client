package de.kaigothe.campusilmenau.ui.heute

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.heute.HeuteClock
import de.kaigothe.campusilmenau.domain.model.heute.HeuteContent
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.ui.components.CampusSheet
import de.kaigothe.campusilmenau.ui.theme.CampusTheme

/**
 * Sheet „Mensa auf der Startseite“: Liste der vier Mensen mit den heutigen Öffnungszeiten. Die Wahl wird sofort
 * gespeichert, „Fertig“ schließt nur.
 */
@Composable
fun HeuteMensaSheet(
    options: List<HeuteContent.MensaOption>,
    selected: MensaVenue,
    onSelect: (MensaVenue) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = CampusTheme.colors
    CampusSheet(
        onDismissRequest = onDismiss,
        startAction = {},
        endAction = { dismiss ->
            TextButton(onClick = dismiss) {
                Text(text = stringResource(R.string.fertig), color = colors.accent, fontWeight = FontWeight.SemiBold)
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.heute_mensa_sheet_titel),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 22.sp, lineHeight = 28.sp),
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.heute_mensa_sheet_text),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
            )
            options.forEachIndexed { index, option ->
                if (index > 0) HorizontalDivider(color = colors.separator)
                OptionRow(option = option, isSelected = option.venue == selected, onClick = { onSelect(option.venue) })
            }
        }
    }
}

@Composable
private fun OptionRow(option: HeuteContent.MensaOption, isSelected: Boolean, onClick: () -> Unit) {
    val colors = CampusTheme.colors
    val info = option.info?.let { infoText(it) }
    val description = listOfNotNull(option.venue.displayName, info).joinToString(", ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = option.venue.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary,
            )
            if (info != null) {
                Text(text = info, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        // Die leere Fläche hält den Platz des Häkchens, damit nichts springt.
        Box(modifier = Modifier.size(24.dp).alpha(if (isSelected) 1f else 0f), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
        }
    }
}

/** „11:00–14:00 Uhr“, bei Cafeteria Ehrenberg mit Abendessen; „Heute geschlossen“ ohne Zeit. */
@Composable
private fun infoText(info: HeuteContent.MensaInfo): String = when (info) {
    HeuteContent.MensaInfo.ClosedToday -> stringResource(R.string.heute_geschlossen)
    is HeuteContent.MensaInfo.Hours -> {
        val range = stringResource(R.string.heute_oeffnung_zeitraum, HeuteClock.time(info.opens), HeuteClock.time(info.closes))
        if (info.dinner) stringResource(R.string.heute_mit_abendessen, range, HeuteClock.time(HeuteContent.DinnerStart)) else range
    }
}
