package de.kaigothe.campusilmenau.ui.share.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventStatus
import de.kaigothe.campusilmenau.domain.model.mensa.MealBadge
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealPrice
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealtime
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.domain.model.share.EventShare
import de.kaigothe.campusilmenau.domain.model.share.MensaMealShare
import de.kaigothe.campusilmenau.domain.model.share.MensaPlanShare
import de.kaigothe.campusilmenau.ui.share.rememberShareStrings
import java.time.Instant
import java.time.LocalDate

// Die Karten sehen immer gleich aus, egal ob das Gerät hell oder dunkel ist; deshalb gibt es je Karte eine Vorschau.
// Das Bild ist 1080 px breit. Die Vorschau hat Dichte 2,625, 411 dp sind dafür rund 1080 px.

private val Day = LocalDate.of(2026, 10, 29)

private fun meal(name: String, description: String?, vegan: Boolean = false, price: Double = 2.9) = MensaMeal(
    id = name, category = "Essen 1", name = name, description = description,
    price = MensaMealPrice(price, price + 1.2, price + 2.4),
    allergies = listOf("Sellerie", "Senf"), badges = if (vegan) setOf(MealBadge.Vegan, MealBadge.Bio) else emptySet(),
    additives = listOf("Antioxidationsmittel"), categories = listOf("Regional"), co2GramPerPortion = 412.0,
    components = emptyList(), canteenName = "Mensa Ehrenberg", date = Day,
)

private val Meals = listOf(
    meal("Linsen-Dal", "mit Basmatireis und Koriander", vegan = true),
    meal("Hähnchenbrust in Rahmsoße", "dazu Spätzle und Erbsen", price = 4.5),
    meal("Gemüsepfanne", null, vegan = true, price = 3.2),
)

private fun event(status: EventStatus = EventStatus.Scheduled, allDay: Boolean = false) = CampusEvent(
    id = "e", title = "Halloween-Party im Club C", start = Instant.parse("2026-10-30T19:00:00Z"),
    end = Instant.parse("2026-10-31T01:00:00Z"), allDay = allDay, status = status,
    location = "Club C", address = "Ehrenbergstraße 1, 98693 Ilmenau", organizer = "StuRa",
    description = "Verkleidet kommen lohnt sich: Es gibt Preise für die besten Kostüme.\n\nEinlass ab 20 Uhr.",
)

@Composable
private fun PreviewHost(style: ShareCardStyle, content: @Composable () -> Unit) = ShareCardTheme(style, content)

@Preview(name = "Speiseplan", widthDp = 411, showBackground = true)
@Composable
private fun PlanPreview() = PreviewHost(ShareCardStyle.Light) {
    val strings = rememberShareStrings()
    SharePlanImage(MensaPlanShare(MensaVenue.MensaEhrenberg, Day, MensaMealtime.Lunch, Meals, MensaPriceGroup.Studierende, "10:45–14:00", strings), strings)
}

@Preview(name = "Gericht", widthDp = 411, showBackground = true)
@Composable
private fun MealPreview() = PreviewHost(ShareCardStyle.Light) {
    val strings = rememberShareStrings()
    ShareMealImage(MensaMealShare(Meals[0], MensaPriceGroup.Studierende, strings), strings)
}

@Preview(name = "Veranstaltung", widthDp = 411, showBackground = true)
@Composable
private fun EventPreview() = PreviewHost(ShareCardStyle.Night) {
    val strings = rememberShareStrings()
    ShareEventImage(EventShare(event(), strings), strings)
}

@Preview(name = "Veranstaltung abgesagt", widthDp = 411, showBackground = true)
@Composable
private fun EventCancelledPreview() = PreviewHost(ShareCardStyle.Night) {
    val strings = rememberShareStrings()
    ShareEventImage(EventShare(event(status = EventStatus.Cancelled), strings), strings)
}

@Preview(name = "Veranstaltung unbestätigt ganztägig", widthDp = 411, showBackground = true)
@Composable
private fun EventTentativePreview() = PreviewHost(ShareCardStyle.Night) {
    val strings = rememberShareStrings()
    ShareEventImage(EventShare(event(status = EventStatus.Tentative, allDay = true), strings), strings)
}
