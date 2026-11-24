package de.kaigothe.campusilmenau.ui.mensa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.mensa.MensaDiet
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.ui.components.CampusSheet
import de.kaigothe.campusilmenau.ui.components.NeutralSwitchRow
import de.kaigothe.campusilmenau.ui.components.SegmentOption
import de.kaigothe.campusilmenau.ui.components.SegmentedControl
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.CardShape

/**
 * Sheet „Filter“: Ernährung als Segment (immer gleich wie die Kapseln), „Nur Bio“ und ein Schalter je Allergen.
 * Änderungen gelten sofort, gelten für alle Mensen und Tage und bleiben gespeichert.
 * [allergens] sind alle bekannten Allergene, dazu ausgeschlossene und neue aus dem Speiseplan.
 */
@Composable
fun MensaFilterSheet(
    filter: MensaFilter,
    allergens: List<String>,
    onFilterChange: (MensaFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = CampusTheme.colors
    CampusSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.filter_titel),
        startAction = {
            TextButton(onClick = { onFilterChange(MensaFilter()) }, enabled = filter.isActive) {
                Text(text = stringResource(R.string.filter_zuruecksetzen_kurz), color = colors.accent.takeIf { filter.isActive } ?: colors.textTertiary)
            }
        },
        endAction = { dismiss ->
            TextButton(onClick = dismiss) {
                Text(
                    text = stringResource(R.string.fertig),
                    color = colors.accent,
                    fontWeight = FontWeight.SemiBold,
                )
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
            SectionTitle(stringResource(R.string.filter_abschnitt_ernaehrung))
            Group {
                SegmentedControl(
                    options = MensaDiet.entries.map { SegmentOption(it, it.title()) },
                    selected = filter.diet,
                    onSelect = { onFilterChange(filter.copy(diet = it)) },
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                HorizontalDivider(color = colors.separator)
                NeutralSwitchRow(
                    label = stringResource(R.string.filter_nur_bio),
                    checked = filter.onlyBio,
                    onCheckedChange = { onFilterChange(filter.copy(onlyBio = it)) },
                )
            }
            Note(stringResource(R.string.filter_hinweis_vegetarisch))

            SectionTitle(stringResource(R.string.filter_abschnitt_allergene), modifier = Modifier.padding(top = 22.dp))
            Group {
                allergens.forEachIndexed { index, allergen ->
                    if (index > 0) HorizontalDivider(color = colors.separator)
                    NeutralSwitchRow(
                        label = stringResource(R.string.filter_ohne_allergen, allergen),
                        checked = allergen in filter.excludedAllergens,
                        onCheckedChange = { excluded ->
                            val updated = if (excluded) filter.excludedAllergens + allergen else filter.excludedAllergens - allergen
                            onFilterChange(filter.copy(excludedAllergens = updated))
                        },
                    )
                }
            }
            Note(stringResource(R.string.filter_hinweis_allergene))
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.65.sp,
        ),
        color = CampusTheme.colors.textTertiary,
        modifier = modifier
            .padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CampusTheme.colors.surface, CardShape)
            .padding(horizontal = 16.dp),
    ) {
        content()
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = CampusTheme.colors.textTertiary,
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp),
    )
}
