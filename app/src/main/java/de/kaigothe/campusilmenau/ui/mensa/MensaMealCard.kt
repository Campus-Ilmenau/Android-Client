package de.kaigothe.campusilmenau.ui.mensa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.mensa.MealBadge
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealPrice
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.listChips
import de.kaigothe.campusilmenau.domain.model.mensa.uniqueAllergies
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import java.time.LocalDate

/**
 * Gericht in der Liste: Kategorie, Name mit Preis der Preisgruppe, Beschreibung, Chips, Allergene.
 * Die Karte ist reine Darstellung, das Öffnen übernimmt der Aufrufer über [onClick].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MensaMealCard(
    meal: MensaMeal,
    priceGroup: MensaPriceGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CampusTheme.colors
    val price = meal.price.formatted(priceGroup)
    val chips = meal.listChips
    val allergies = meal.uniqueAllergies

    CampusCard(modifier = modifier.semantics(mergeDescendants = true) {}, onClick = onClick) {
        Text(
            text = (meal.category.ifEmpty { stringResource(R.string.gericht) }).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            ),
            color = colors.textTertiary,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        // Der Name darf umbrechen, der Preis bleibt in einer Zeile.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = price,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 1,
                modifier = Modifier.wrapContentWidth(),
            )
        }

        meal.description?.takeIf { it.isNotEmpty() }?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        if (chips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                chips.forEach { chip -> CampusChip(text = chip.title(), variant = chip.tone.variant()) }
            }
        }

        if (allergies.isNotEmpty()) {
            Text(
                text = stringResource(R.string.gericht_enthaelt, allergies.joinToString(", ")),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

/** Beispielgerichte für Vorschauen. */
internal object MensaPreviewData {
    val sample = MensaMeal(
        id = "preview-1",
        category = "Mittagessen",
        name = "Linsen-Dal mit Basmatireis",
        description = "Basmatireis, Mango-Chutney",
        price = MensaMealPrice(2.9, 4.4, 5.8, "€"),
        allergies = listOf("Sellerie", "Senf"),
        badges = setOf(MealBadge.Vegan, MealBadge.Vegetarian),
        additives = emptyList(),
        categories = listOf("Regional"),
        co2GramPerPortion = 850.0,
        components = emptyList(),
        canteenName = "Mensa Ehrenberg",
        date = LocalDate.of(2026, 10, 29),
    )

    /** Ohne Beschreibung, Chips und Allergene, mit fehlendem Studierendenpreis. */
    val minimal = MensaMeal(
        id = "preview-2",
        category = "Mittagessen",
        name = "Pasta Arrabbiata",
        description = null,
        price = MensaMealPrice(null, 4.4, 5.8, "€"),
        allergies = emptyList(),
        badges = emptySet(),
        additives = emptyList(),
        categories = emptyList(),
        co2GramPerPortion = null,
        components = emptyList(),
        canteenName = "Mensa Ehrenberg",
        date = LocalDate.of(2026, 10, 29),
    )
}

@Preview(name = "Karte", showBackground = true)
@Composable
private fun MensaMealCardLightPreview() {
    CampusIlmenauTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MensaMealCard(MensaPreviewData.sample, MensaPriceGroup.Studierende, onClick = {})
            MensaMealCard(MensaPreviewData.minimal, MensaPriceGroup.Studierende, onClick = {})
        }
    }
}

@Preview(name = "Karte Dark", showBackground = true, backgroundColor = 0xFF0E1A2E)
@Composable
private fun MensaMealCardDarkPreview() {
    CampusIlmenauTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MensaMealCard(MensaPreviewData.sample, MensaPriceGroup.Studierende, onClick = {})
            MensaMealCard(MensaPreviewData.minimal, MensaPriceGroup.Studierende, onClick = {})
        }
    }
}
