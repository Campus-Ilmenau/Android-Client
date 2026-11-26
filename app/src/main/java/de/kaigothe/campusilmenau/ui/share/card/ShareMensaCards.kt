package de.kaigothe.campusilmenau.ui.share.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.domain.model.events.EventTimes
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealtime
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.detailChips
import de.kaigothe.campusilmenau.domain.model.mensa.listChips
import de.kaigothe.campusilmenau.domain.model.mensa.uniqueAdditives
import de.kaigothe.campusilmenau.domain.model.mensa.uniqueAllergies
import de.kaigothe.campusilmenau.domain.model.share.MensaMealShare
import de.kaigothe.campusilmenau.domain.model.share.MensaPlanShare
import de.kaigothe.campusilmenau.domain.model.share.ShareStrings
import de.kaigothe.campusilmenau.domain.model.share.ShareText
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import kotlin.math.roundToLong

/** Ziffern gleicher Breite (OpenType `tnum`), damit Preise bündig stehen. */
private const val TabularFigures = "tnum"

/**
 * Speiseplan einer Mensa an einem Tag, aufgebaut wie das große Mensa-Widget: blauer Kopf mit grünem Band, eine Zeile je
 * Gericht (alle, Namen brechen um), Hinweis, Marke.
 */
@Composable
fun SharePlanCard(share: MensaPlanShare, strings: ShareStrings, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    Column(modifier = modifier.shareCardSurface()) {
        PlanHeader(share, strings)
        Column(modifier = Modifier.padding(start = 18.dp, top = 2.dp, end = 18.dp)) {
            share.meals.forEachIndexed { index, meal ->
                PlanRow(meal, share, strings, modifier = Modifier.shareTopRule(hidden = index == 0).padding(vertical = 11.dp))
            }
        }
        Text(
            text = strings.priceNote(share.priceGroup) + " · " + strings.cardDisclaimer(),
            style = TextStyle(fontSize = 12.sp, color = colors.textTertiary),
            modifier = Modifier
                .fillMaxWidth()
                .shareTopRule()
                .padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 14.dp),
        )
        ShareBrandFooter()
    }
}

@Composable
private fun PlanHeader(share: MensaPlanShare, strings: ShareStrings) {
    val colors = CampusTheme.colors
    val offer = when (share.mealtime) {
        MensaMealtime.Lunch -> strings.planOffer(false)
        MensaMealtime.Dinner -> if (share.venue.hasMealtimes) strings.cardDinnerCafeteria() else strings.planOffer(true)
    }
    val subtitle = listOfNotNull(offer, share.hours?.let(strings::hours)).joinToString(" · ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(colors.blue)
                drawUnitPolygon(listOf(0.8f to 0f, 1f to 0f, 1f to 1f, 0.66f to 1f), colors.green)
            }
            .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 15.dp),
    ) {
        Text(
            text = EventTimes.fullDay(share.day, strings.locale).uppercase(strings.locale),
            style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.46.sp, color = colors.onBlue.copy(alpha = 0.9f)),
        )
        Text(
            text = share.venue.displayName,
            style = displayStyle(size = 23, lineHeight = 27, color = colors.onBlue),
            modifier = Modifier.padding(top = 3.dp, end = 56.dp),
        )
        Text(
            text = subtitle,
            style = TextStyle(fontSize = 13.sp, color = colors.onBlue.copy(alpha = 0.88f)),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun PlanRow(meal: MensaMeal, share: MensaPlanShare, strings: ShareStrings, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val chips = meal.listChips
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(meal.name, style = TextStyle(fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary))
            ShareText.clean(meal.description)?.let {
                Text(it, style = TextStyle(fontSize = 12.5.sp, color = colors.textSecondary), modifier = Modifier.padding(top = 1.dp))
            }
            if (chips.isNotEmpty()) ShareChips(chips, strings, modifier = Modifier.padding(top = 5.dp))
        }
        Text(
            text = meal.price.formatted(share.priceGroup, strings.locale),
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontFeatureSettings = TabularFigures),
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * Ein Gericht mit allen Angaben: Kopf mit Diagonalbändern wie in der Gericht-Ansicht, die eigene Preisgruppe groß, dann
 * die übrigen Zeilen. Felder ohne Daten entfallen.
 */
@Composable
fun ShareMealCard(share: MensaMealShare, strings: ShareStrings, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    Column(modifier = modifier.shareCardSurface()) {
        MealHead(share.kicker)
        MealDetails(share, strings)
        Text(
            text = strings.cardMealDisclaimer(),
            style = TextStyle(fontSize = 12.sp, color = colors.textTertiary),
            modifier = Modifier
                .fillMaxWidth()
                .shareTopRule()
                .padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 14.dp),
        )
        ShareBrandFooter()
    }
}

/** 104 dp hoch: Grün hinten, Blau vorn, der Kicker in Weiß. */
@Composable
private fun MealHead(kicker: String) {
    val colors = CampusTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .drawBehind {
                drawRect(colors.surface)
                drawUnitPolygon(listOf(0f to 0f, 1f to 0f, 1f to 0.5f, 0f to 1f), colors.green)
                drawUnitPolygon(listOf(0f to 0f, 1f to 0f, 1f to 0.26f, 0f to 0.78f), colors.blue)
            },
    ) {
        Text(
            text = kicker.uppercase(),
            style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.46.sp, color = colors.onBlue),
            modifier = Modifier.padding(start = 18.dp, top = 16.dp, end = 18.dp),
        )
    }
}

@Composable
private fun MealDetails(share: MensaMealShare, strings: ShareStrings) {
    val colors = CampusTheme.colors
    val meal = share.meal
    val chips = meal.detailChips
    Column(modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 4.dp, end = 18.dp, bottom = 4.dp)) {
        Text(meal.name, style = displayStyle(size = 25, lineHeight = 28, color = colors.textPrimary))
        ShareText.clean(meal.description)?.let {
            Text(it, style = TextStyle(fontSize = 14.sp, color = colors.textSecondary), modifier = Modifier.padding(top = 4.dp))
        }
        if (chips.isNotEmpty()) ShareChips(chips, strings, modifier = Modifier.padding(top = 10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(strings.groupName(share.priceGroup), style = TextStyle(fontSize = 13.sp, color = colors.textTertiary))
            Text(
                text = meal.price.formatted(share.priceGroup, strings.locale),
                style = displayStyle(size = 36, lineHeight = 40, color = colors.textPrimary).copy(fontFeatureSettings = TabularFigures),
                maxLines = 1,
                softWrap = false,
            )
        }

        mealLines(meal, share.priceGroup, strings).forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().shareTopRule().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(label, style = TextStyle(fontSize = 13.5.sp, color = colors.textSecondary), softWrap = false)
                Text(
                    text = value,
                    style = TextStyle(fontSize = 13.5.sp, color = colors.textPrimary, textAlign = TextAlign.End),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Die anderen Preisgruppen (in der Reihenfolge Studierende, Bedienstete, Gäste), Allergene, Zusatzstoffe, Umwelt. */
internal fun mealLines(meal: MensaMeal, own: MensaPriceGroup, strings: ShareStrings): List<Pair<String, String>> = buildList {
    MensaPriceGroup.entries.filter { it != own }.forEach { add(strings.groupName(it) to meal.price.formatted(it, strings.locale)) }
    if (meal.uniqueAllergies.isNotEmpty()) add(strings.cardAllergens() to meal.uniqueAllergies.joinToString(", "))
    if (meal.uniqueAdditives.isNotEmpty()) add(strings.cardAdditives() to meal.uniqueAdditives.joinToString(", "))
    meal.co2GramPerPortion?.let { add(strings.cardEnvironment() to strings.co2(it.roundToLong().toString())) }
}
