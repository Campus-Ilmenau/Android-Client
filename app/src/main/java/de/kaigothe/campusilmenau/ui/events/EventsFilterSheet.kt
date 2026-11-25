package de.kaigothe.campusilmenau.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.events.EventKind
import de.kaigothe.campusilmenau.domain.model.events.EventsSelection
import de.kaigothe.campusilmenau.ui.components.CampusSheet
import de.kaigothe.campusilmenau.ui.components.NeutralSwitchVisual
import de.kaigothe.campusilmenau.ui.components.SegmentOption
import de.kaigothe.campusilmenau.ui.components.SegmentedControl
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.CardShape

/** Sheet „Filter“: „Art“ als Segment und „Abgesagte ausblenden“. Tag, „Heute Abend“ und Veranstalter wählt man über der Liste. */
@Composable
fun EventsFilterSheet(
    selection: EventsSelection,
    onChange: ((EventsSelection) -> EventsSelection) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = CampusTheme.colors
    CampusSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.filter_titel),
        startAction = {
            TextButton(onClick = { onChange { it.resetSheet() } }, enabled = selection.sheetCount > 0) {
                Text(
                    text = stringResource(R.string.filter_zuruecksetzen_kurz),
                    color = if (selection.sheetCount > 0) colors.accent else colors.textTertiary,
                )
            }
        },
        endAction = { dismiss ->
            TextButton(onClick = dismiss) {
                Text(text = stringResource(R.string.fertig), color = colors.accent, fontWeight = FontWeight.SemiBold)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 34.dp),
        ) {
            Text(
                text = stringResource(R.string.events_filter_art).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.65.sp),
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 8.dp).semantics { heading() },
            )
            SegmentedControl(
                options = listOf(
                    SegmentOption(EventKind.All, stringResource(R.string.events_art_alle)),
                    SegmentOption(EventKind.Event, stringResource(R.string.events_art_veranstaltungen)),
                    SegmentOption(EventKind.Deadline, stringResource(R.string.events_art_fristen)),
                ),
                selected = selection.kind,
                onSelect = { kind -> onChange { it.copy(kind = kind) } },
            )

            Row(
                modifier = Modifier
                    .padding(top = 22.dp)
                    .fillMaxWidth()
                    .background(colors.surface, CardShape)
                    .heightIn(min = 52.dp)
                    .toggleable(
                        value = selection.hideCancelled,
                        role = Role.Switch,
                        onValueChange = { hide -> onChange { it.copy(hideCancelled = hide) } },
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.events_abgesagte_ausblenden),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.events_abgesagte_hinweis),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary,
                    )
                }
                NeutralSwitchVisual(checked = selection.hideCancelled)
            }

            Text(
                text = stringResource(R.string.events_filter_hinweis),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp),
            )
        }
    }
}
