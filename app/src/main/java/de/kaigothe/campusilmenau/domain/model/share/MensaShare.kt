package de.kaigothe.campusilmenau.domain.model.share

import de.kaigothe.campusilmenau.domain.model.events.EventTimes
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealtime
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.domain.model.mensa.detailChips
import de.kaigothe.campusilmenau.domain.model.mensa.listChips
import de.kaigothe.campusilmenau.domain.model.mensa.uniqueAdditives
import de.kaigothe.campusilmenau.domain.model.mensa.uniqueAllergies
import java.time.LocalDate
import kotlin.math.roundToLong

/**
 * Das gerade gezeigte Angebot einer Mensa: alle Gerichte, **ohne** Filter (die sind persönlich), mit dem Preis der
 * eigenen Preisgruppe.
 *
 * @param allMeals alle Gerichte des Tages; die Auswahl für Mensa und Mittag/Abendessen trifft diese Klasse wie der Speiseplan.
 * @param hours feste Zeit, z. B. „11:00–14:00“; ohne Angabe steht nur „Mittagsangebot“ bzw. „Abendessen“.
 */
class MensaPlanShare(
    val venue: MensaVenue,
    val day: LocalDate,
    val mealtime: MensaMealtime = MensaMealtime.Lunch,
    allMeals: List<MensaMeal>,
    val priceGroup: MensaPriceGroup,
    val hours: String? = null,
    private val strings: ShareStrings,
) : ShareContent {
    val meals: List<MensaMeal> = allMeals.filter { meal ->
        if (!venue.matches(meal.canteenName)) return@filter false
        if (!venue.hasMealtimes) return@filter true
        (mealtime == MensaMealtime.Dinner) == MensaMealtime.isDinner(meal)
    }

    override val title: String get() = "${venue.displayName} · ${EventTimes.short(day, strings.locale)}"

    override val text: String
        get() {
            val offerLine = listOfNotNull(strings.planOffer(mealtime == MensaMealtime.Dinner), hours?.let(strings::hours)).joinToString(", ")
            val header = listOf("${venue.displayName} · ${EventTimes.fullDay(day, strings.locale)}", offerLine)
            val lines = meals.map { meal ->
                buildString {
                    append("• ").append(meal.name)
                    meal.price.shareText(priceGroup, strings.locale)?.let { append(" – ").append(it) }
                    val tags = meal.listChips.map { strings.chipTitle(it).lowercase(strings.locale) }
                    if (tags.isNotEmpty()) append(" (").append(tags.joinToString(", ")).append(")")
                }
            }
            return ShareText.compose(listOf(header, lines, listOf(strings.priceNote(priceGroup))), strings)
        }
}

/** Ein Gericht mit allen Angaben: alle drei Preisgruppen (die eigene zuerst), Allergene, Zusatzstoffe, CO₂. */
class MensaMealShare(
    val meal: MensaMeal,
    val priceGroup: MensaPriceGroup,
    private val strings: ShareStrings,
) : ShareContent {
    val venueName: String get() = MensaVenue.entries.firstOrNull { it.matches(meal.canteenName) }?.displayName ?: meal.canteenName

    /** „Mensa Ehrenberg · Essen 1 · Do 29. Okt.“ */
    val kicker: String
        get() {
            val category = ShareText.clean(meal.category) ?: strings.fallbackCategory()
            return "$venueName · $category · ${EventTimes.short(meal.date, strings.locale)}"
        }

    override val title: String get() = "${meal.name} · $venueName"

    override val text: String
        get() {
            val head = listOf(
                meal.name,
                ShareText.clean(meal.description).orEmpty(),
                kicker,
                meal.detailChips.joinToString(" · ") { strings.chipTitle(it) },
            )
            val prices = priceGroup.ownFirst()
                .mapNotNull { group -> meal.price.shareText(group, strings.locale)?.let { "${strings.groupName(group)} $it" } }
                .joinToString(" · ")
            val details = buildList {
                add(prices)
                if (meal.uniqueAllergies.isNotEmpty()) add(strings.allergens(meal.uniqueAllergies.joinToString(", ")))
                if (meal.uniqueAdditives.isNotEmpty()) add(strings.additives(meal.uniqueAdditives.joinToString(", ")))
                meal.co2GramPerPortion?.let { add(strings.co2(it.roundToLong().toString())) }
            }
            return ShareText.compose(listOf(head, details), strings)
        }
}
