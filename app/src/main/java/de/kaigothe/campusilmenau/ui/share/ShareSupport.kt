package de.kaigothe.campusilmenau.ui.share

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.mensa.MealChip
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.share.ShareContent
import de.kaigothe.campusilmenau.domain.model.share.ShareStrings
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import java.util.Locale

/** Die Texte zum Teilen aus den String-Ressourcen, in der Sprache, in der die App gerade läuft. */
class ResourceShareStrings(private val resources: Resources, override val locale: Locale) : ShareStrings {
    private fun s(id: Int, vararg args: Any): String = resources.getString(id, *args)

    override fun brand(domain: String) = s(R.string.share_brand, domain)

    override fun groupName(group: MensaPriceGroup) = s(
        when (group) {
            MensaPriceGroup.Studierende -> R.string.share_group_students
            MensaPriceGroup.Bedienstete -> R.string.share_group_staff
            MensaPriceGroup.Gaeste -> R.string.share_group_guests
        },
    )

    override fun planOffer(dinner: Boolean) = s(if (dinner) R.string.share_plan_dinner else R.string.share_plan_lunch)

    override fun hours(range: String) = s(R.string.share_hours, range)

    override fun priceNote(group: MensaPriceGroup) = s(
        when (group) {
            MensaPriceGroup.Studierende -> R.string.share_prices_students
            MensaPriceGroup.Bedienstete -> R.string.share_prices_staff
            MensaPriceGroup.Gaeste -> R.string.share_prices_guests
        },
    )

    override fun chipTitle(chip: MealChip) = when (chip) {
        MealChip.Vegan -> s(R.string.share_tag_vegan)
        MealChip.Vegetarian -> s(R.string.share_tag_vegetarian)
        MealChip.Bio -> s(R.string.share_tag_organic)
        MealChip.Regional -> s(R.string.share_tag_regional)
        is MealChip.Other -> chip.name
    }

    override fun fallbackCategory() = s(R.string.share_meal_fallback_category)
    override fun allergens(list: String) = s(R.string.share_meal_allergens, list)
    override fun additives(list: String) = s(R.string.share_meal_additives, list)
    override fun co2(grams: String) = s(R.string.share_meal_co2, grams)

    override fun eventCancelled(title: String) = s(R.string.share_event_cancelled, title)
    override fun eventTentative() = s(R.string.share_event_tentative)
    override fun eventOrganizer(name: String) = s(R.string.share_event_organizer, name)
    override fun eventTickets(url: String) = s(R.string.share_event_tickets, url)
    override fun eventInfo(url: String) = s(R.string.share_event_info, url)
    override fun eventAllDay() = s(R.string.share_event_all_day)
    override fun eventRange(from: String, to: String) = s(R.string.share_event_range, from, to)
    override fun eventFrom(from: String) = s(R.string.share_event_from, from)

    override fun mediumIntro() = s(R.string.share_medium_intro)
    override fun mediumOnline() = s(R.string.share_medium_online)

    override fun cardDinnerCafeteria() = s(R.string.share_card_dinner_cafeteria)
    override fun cardDisclaimer() = s(R.string.share_card_disclaimer)
    override fun cardMealDisclaimer() = s(R.string.share_card_meal_disclaimer)
    override fun cardAllergens() = s(R.string.share_card_allergens)
    override fun cardAdditives() = s(R.string.share_card_additives)
    override fun cardEnvironment() = s(R.string.share_card_environment)
    override fun cardEventAllDay() = s(R.string.share_card_event_all_day)
    override fun cardEventCancelledChip() = s(R.string.share_card_event_cancelled_chip)
    override fun cardEventCancelledNote() = s(R.string.share_card_event_cancelled_note)
    override fun cardEventFallbackOrganizer() = s(R.string.share_card_event_fallback_organizer)
    override fun cardEventNoEnd() = s(R.string.share_card_event_no_end)
    override fun cardEventTentativeChip() = s(R.string.share_card_event_tentative_chip)
    override fun cardEventTentativeNote() = s(R.string.share_card_event_tentative_note)
    override fun cardEventUntil(time: String) = s(R.string.share_card_event_until, time)
}

/** Die Teilen-Texte in der aktuellen Sprache; wechselt mit der Konfiguration. */
@Composable
fun rememberShareStrings(): ShareStrings {
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(resources, locale) { ResourceShareStrings(resources, locale) }
}

/** Öffnet das Teilen-Blatt des Systems mit dem Text. Der Titel dient Mail-Apps als Betreff. */
fun Context.shareText(content: ShareContent) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, content.title)
        .putExtra(Intent.EXTRA_TEXT, content.text)
    val chooser = Intent.createChooser(send, getString(R.string.share_chooser))
    // Ohne Activity (selten) braucht der Start eine neue Aufgabe.
    if (findActivityContext() == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(chooser)
}

internal fun Context.findActivityContext(): android.app.Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

/** Die Teilen-Taste für die Kopfleiste einer Detailansicht (48 dp). */
@Composable
fun ShareButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(Spacing.MinTouchTarget)) {
        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share_button), tint = CampusTheme.colors.textPrimary)
    }
}
