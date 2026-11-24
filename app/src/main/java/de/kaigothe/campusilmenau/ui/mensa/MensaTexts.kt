package de.kaigothe.campusilmenau.ui.mensa

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.domain.model.mensa.ChipTone
import de.kaigothe.campusilmenau.domain.model.mensa.MealChip
import de.kaigothe.campusilmenau.domain.model.mensa.MensaComponentInfo
import de.kaigothe.campusilmenau.domain.model.mensa.MensaDiet
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFormatting
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaStatus
import de.kaigothe.campusilmenau.domain.model.mensa.StatusTone
import de.kaigothe.campusilmenau.ui.components.ChipVariant
import java.time.LocalDate

/** Die Texte der Mensa aus den String-Ressourcen. Die Domäne liefert nur Fälle und Zahlen. */

@Composable
fun MealChip.title(): String = when (this) {
    MealChip.Vegan -> stringResource(R.string.chip_vegan)
    MealChip.Vegetarian -> stringResource(R.string.chip_vegetarisch)
    MealChip.Bio -> stringResource(R.string.chip_bio)
    MealChip.Regional -> stringResource(R.string.chip_regional)
    is MealChip.Other -> name
}

fun ChipTone.variant(): ChipVariant = when (this) {
    ChipTone.Green -> ChipVariant.Green
    ChipTone.Blue -> ChipVariant.Blue
    ChipTone.Neutral -> ChipVariant.Neutral
}

fun StatusTone.variant(): ChipVariant = when (this) {
    StatusTone.Danger -> ChipVariant.DangerSoft
    StatusTone.Neutral -> ChipVariant.Neutral
    StatusTone.Warning -> ChipVariant.Warning
    StatusTone.Green -> ChipVariant.Green
}

@Composable
fun MensaStatus.text(): String = when (this) {
    MensaStatus.Closed -> stringResource(R.string.status_geschlossen)
    is MensaStatus.UntilClock -> stringResource(R.string.status_bis, clock)
    is MensaStatus.Range -> stringResource(R.string.status_zeitraum, from, to)
    is MensaStatus.FromClock -> stringResource(R.string.status_ab, clock)
    MensaStatus.ClosingSoon -> stringResource(R.string.status_gleich_zu)
    is MensaStatus.Open -> stringResource(R.string.status_geoeffnet_zeitraum, from, to)
}

@Composable
fun MensaPriceGroup.title(): String = when (this) {
    MensaPriceGroup.Studierende -> stringResource(R.string.preisgruppe_studierende)
    MensaPriceGroup.Bedienstete -> stringResource(R.string.preisgruppe_bedienstete)
    MensaPriceGroup.Gaeste -> stringResource(R.string.preisgruppe_gaeste)
}

@Composable
fun MensaDiet.title(): String = when (this) {
    MensaDiet.Alle -> stringResource(R.string.diaet_alle)
    MensaDiet.Vegetarisch -> stringResource(R.string.diaet_vegetarisch)
    MensaDiet.Vegan -> stringResource(R.string.diaet_vegan)
}

/** „Heute“, „Morgen“, sonst „Do., 29. Okt.“ */
@Composable
fun dayLabel(day: LocalDate, today: LocalDate): String = when (MensaFormatting.relative(day, today)) {
    MensaFormatting.RelativeDay.Today -> stringResource(R.string.tag_heute)
    MensaFormatting.RelativeDay.Tomorrow -> stringResource(R.string.tag_morgen)
    MensaFormatting.RelativeDay.Other -> MensaFormatting.shortDate(day)
}

/** „Nur Bio · ohne Gluten, Ei“, `null` ohne zusätzliche Einstellungen. Die Ernährungsstufe steht in den Kapseln. */
@Composable
fun MensaFilter.summary(): String? = summaryParts().takeIf { it.isNotEmpty() }?.joinToString(" · ")

/** Für den leeren Zustand: „Vegetarisch · Nur Bio · ohne Gluten“. */
@Composable
fun MensaFilter.activeDescription(): String {
    val dietName = if (diet == MensaDiet.Alle) emptyList() else listOf(diet.title())
    return (dietName + summaryParts()).joinToString(" · ")
}

@Composable
private fun MensaFilter.summaryParts(): List<String> = buildList {
    if (onlyBio) add(stringResource(R.string.filter_nur_bio))
    if (excludedAllergens.isNotEmpty()) {
        add(stringResource(R.string.filter_ohne_kurz, sortedExcludedAllergens.joinToString(", ")))
    }
}

/** „Allergene: Sellerie · Zusatzstoffe: Antioxidationsmittel“ oder „Keine weiteren Angaben“. */
@Composable
fun MensaComponentInfo.infoText(): String {
    val parts = buildList {
        if (allergens.isNotEmpty()) add(stringResource(R.string.gericht_komponente_allergene, allergens.joinToString(", ")))
        if (additives.isNotEmpty()) add(stringResource(R.string.gericht_komponente_zusatzstoffe, additives.joinToString(", ")))
    }
    return if (parts.isEmpty()) stringResource(R.string.gericht_keine_angaben) else parts.joinToString(" · ")
}

/** Der Fehlertext zum Fehler: „Keine Verbindung“ gegen „Dienst nicht erreichbar“. */
@Composable
fun ApiError.mensaMessage(): String =
    if (this is ApiError.Http && isUnavailable) stringResource(R.string.mensa_fehler_dienst) else stringResource(R.string.mensa_fehler_netz)
