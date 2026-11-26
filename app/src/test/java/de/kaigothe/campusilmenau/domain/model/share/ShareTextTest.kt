package de.kaigothe.campusilmenau.domain.model.share

import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryItem
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryLocation
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.mensa.MealBadge
import de.kaigothe.campusilmenau.domain.model.mensa.MealChip
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealPrice
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealtime
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deutsche Texte wie in der App, ohne Ressourcen. */
private object GermanStrings : ShareStrings {
    override val locale: Locale = Locale.GERMANY
    override fun brand(domain: String) = "Geteilt mit Campus Ilmenau – $domain"
    override fun groupName(group: MensaPriceGroup) = when (group) {
        MensaPriceGroup.Studierende -> "Studierende"
        MensaPriceGroup.Bedienstete -> "Bedienstete"
        MensaPriceGroup.Gaeste -> "Gäste"
    }
    override fun planOffer(dinner: Boolean) = if (dinner) "Abendessen" else "Mittagsangebot"
    override fun hours(range: String) = "$range Uhr"
    override fun priceNote(group: MensaPriceGroup) = "Preise für ${groupName(group)}"
    override fun chipTitle(chip: MealChip) = when (chip) {
        MealChip.Vegan -> "Vegan"
        MealChip.Vegetarian -> "Vegetarisch"
        MealChip.Bio -> "Bio"
        MealChip.Regional -> "Regional"
        is MealChip.Other -> chip.name
    }
    override fun fallbackCategory() = "Gericht"
    override fun allergens(list: String) = "Allergene: $list"
    override fun additives(list: String) = "Zusatzstoffe: $list"
    override fun co2(grams: String) = "≈ $grams g CO₂ pro Portion"
    override fun eventCancelled(title: String) = "Fällt aus: $title"
    override fun eventTentative() = "(Termin noch nicht bestätigt)"
    override fun eventOrganizer(name: String) = "Veranstalter: $name"
    override fun eventTickets(url: String) = "Tickets: $url"
    override fun eventInfo(url: String) = "Infos: $url"
    override fun eventAllDay() = "ganztägig"
    override fun eventRange(from: String, to: String) = "$from–$to Uhr"
    override fun eventFrom(from: String) = "ab $from Uhr"
    override fun mediumIntro() = "Aus dem Katalog der UB Ilmenau:"
    override fun mediumOnline() = "Online im Campusnetz"
}

private const val BRAND = "Geteilt mit Campus Ilmenau – campus-ilmenau.de"

class ShareTextTest {
    // --- Bausteine

    @Test
    fun `compose lasts the brand, skips empty blocks and lines`() {
        val text = ShareText.compose(listOf(listOf("A", "", "B"), listOf("", ""), listOf("C")), GermanStrings)
        assertEquals("A\nB\n\nC\n\n$BRAND", text)
    }

    @Test
    fun `clean trims and turns blank into null`() {
        assertEquals("x", ShareText.clean("  x \n"))
        assertEquals(null, ShareText.clean("  \n"))
        assertEquals(null, ShareText.clean(null))
    }

    @Test
    fun `plainSpaces replaces non breaking spaces`() {
        assertEquals("2,90 €", ShareText.plainSpaces("2,90 €"))
        assertEquals("2,90 €", ShareText.plainSpaces("2,90 €"))
    }

    @Test
    fun `shortened keeps short text and collapses blank lines`() {
        assertEquals("Eins\n\nZwei", ShareText.shortened("Eins \n\n\n\nZwei  "))
    }

    @Test
    fun `shortened cuts at a word end and stays within the limit`() {
        val result = ShareText.shortened("alpha beta gamma delta epsilon", limit = 16)
        assertEquals("alpha beta…", result)
        assertTrue(result.length <= 16)
    }

    @Test
    fun `shortened keeps a whole word before the cut`() {
        // Der Schnitt liegt hinter „gamma“ im Leerzeichen; das Wort bleibt ganz, „delta“ fällt weg.
        assertEquals("alpha beta gamma…", ShareText.shortened("alpha beta gamma delta", limit = 18))
    }

    @Test
    fun `shortened strips trailing punctuation before the ellipsis`() {
        assertEquals("eins, zwei…", ShareText.shortened("eins, zwei, drei vier", limit = 14))
    }

    @Test
    fun `price text is null without a price and uses plain spaces`() {
        val price = MensaMealPrice(student = 2.9, staff = null, guest = 4.5)
        assertEquals("2,90 €", price.shareText(MensaPriceGroup.Studierende, Locale.GERMANY))
        assertEquals(null, price.shareText(MensaPriceGroup.Bedienstete, Locale.GERMANY))
        assertEquals("4,50 €", price.shareText(MensaPriceGroup.Gaeste, Locale.GERMANY))
    }

    @Test
    fun `own price group comes first`() {
        assertEquals(
            listOf(MensaPriceGroup.Gaeste, MensaPriceGroup.Studierende, MensaPriceGroup.Bedienstete),
            MensaPriceGroup.Gaeste.ownFirst(),
        )
    }

    // --- Gericht und Speiseplan

    private fun meal(
        name: String = "Linsen-Dal",
        category: String = "Essen 1",
        canteen: String = "Mensa Ehrenberg",
        badges: Set<MealBadge> = setOf(MealBadge.Vegan),
        allergies: List<String> = listOf("Sellerie", "Sellerie", "Senf"),
        additives: List<String> = emptyList(),
        co2: Double? = 412.4,
        description: String? = "mit Reis",
        price: MensaMealPrice = MensaMealPrice(2.9, 4.1, 5.3),
    ) = MensaMeal(
        id = "1", category = category, name = name, description = description, price = price,
        allergies = allergies, badges = badges, additives = additives, categories = emptyList(),
        co2GramPerPortion = co2, components = emptyList(), canteenName = canteen, date = LocalDate.of(2026, 10, 29),
    )

    @Test
    fun `meal text lists every price group and details`() {
        val share = MensaMealShare(meal(), MensaPriceGroup.Bedienstete, GermanStrings)
        assertEquals(
            "Linsen-Dal\nmit Reis\nMensa Ehrenberg · Essen 1 · Do 29. Okt.\nVegan\n\n" +
                "Bedienstete 4,10 € · Studierende 2,90 € · Gäste 5,30 €\nAllergene: Sellerie, Senf\n≈ 412 g CO₂ pro Portion\n\n$BRAND",
            share.text,
        )
        assertEquals("Linsen-Dal · Mensa Ehrenberg", share.title)
    }

    @Test
    fun `meal text drops missing prices and falls back for the category`() {
        val share = MensaMealShare(
            meal(category = " ", badges = emptySet(), allergies = emptyList(), co2 = null, description = null, price = MensaMealPrice(2.9, null, null)),
            MensaPriceGroup.Studierende,
            GermanStrings,
        )
        assertEquals("Linsen-Dal\nMensa Ehrenberg · Gericht · Do 29. Okt.\n\nStudierende 2,90 €\n\n$BRAND", share.text)
    }

    @Test
    fun `plan share takes only the venue and the mealtime and no filters`() {
        val lunch = meal(name = "Suppe")
        val dinner = meal(name = "Pizza", category = "Abendessen", canteen = "Cafeteria Mensa Ehrenberg")
        val cafeLunch = meal(name = "Salat", category = "Mittagessen", canteen = "Cafeteria Mensa Ehrenberg")
        val other = meal(name = "Burger", canteen = "Cafeteria NANOteria")
        val all = listOf(lunch, dinner, cafeLunch, other)
        val day = LocalDate.of(2026, 10, 29)

        val mensa = MensaPlanShare(MensaVenue.MensaEhrenberg, day, allMeals = all, priceGroup = MensaPriceGroup.Studierende, strings = GermanStrings)
        assertEquals(listOf("Suppe"), mensa.meals.map { it.name })

        val abend = MensaPlanShare(
            MensaVenue.CafeteriaEhrenberg, day, MensaMealtime.Dinner, all, MensaPriceGroup.Studierende, "16:30–19:30", GermanStrings,
        )
        assertEquals(listOf("Pizza"), abend.meals.map { it.name })
        assertEquals(
            "Cafeteria Ehrenberg · Donnerstag, 29. Oktober\nAbendessen, 16:30–19:30 Uhr\n\n• Pizza – 2,90 € (vegan)\n\n" +
                "Preise für Studierende\n\n$BRAND",
            abend.text,
        )
        assertEquals("Cafeteria Ehrenberg · Do 29. Okt.", abend.title)
    }

    // --- Veranstaltung

    private fun event(
        status: EventStatus = EventStatus.Scheduled,
        allDay: Boolean = false,
        start: String = "2026-10-30T19:00:00Z",
        end: String? = "2026-10-30T22:00:00Z",
        ticket: String? = null,
        info: String? = "https://example.org/info",
        description: String? = "Ein Abend mit Musik.",
    ) = CampusEvent(
        id = "e1", title = "Halloween-Party", start = Instant.parse(start), end = end?.let(Instant::parse), allDay = allDay,
        status = status, location = "Club C", address = "Ehrenbergstr. 1", organizer = "StuRa",
        description = description, infoUrl = info, ticketUrl = ticket,
    )

    @Test
    fun `event text has date time place organizer description and info link`() {
        val share = EventShare(event(), GermanStrings)
        assertEquals(
            "Halloween-Party\nFreitag, 30. Oktober · 20:00–23:00 Uhr\nClub C, Ehrenbergstr. 1\nVeranstalter: StuRa\n\n" +
                "Ein Abend mit Musik.\n\nInfos: https://example.org/info\n\n$BRAND",
            share.text,
        )
        assertEquals("Halloween-Party · Fr 30. Okt.", share.title)
    }

    @Test
    fun `tickets win over the info link`() {
        val text = EventShare(event(ticket = "https://example.org/t"), GermanStrings).text
        assertTrue(text.contains("Tickets: https://example.org/t"))
        assertFalse(text.contains("Infos:"))
    }

    @Test
    fun `cancelled event has no description and says so`() {
        val text = EventShare(event(status = EventStatus.Cancelled), GermanStrings).text
        assertTrue(text.startsWith("Fällt aus: Halloween-Party\n"))
        assertFalse(text.contains("Ein Abend mit Musik."))
    }

    @Test
    fun `tentative event notes it after the time`() {
        val text = EventShare(event(status = EventStatus.Tentative), GermanStrings).text
        assertTrue(text.contains("20:00–23:00 Uhr (Termin noch nicht bestätigt)"))
    }

    @Test
    fun `event without end says from and all day says all day`() {
        assertTrue(EventShare(event(end = null), GermanStrings).text.contains("Freitag, 30. Oktober · ab 20:00 Uhr"))
        assertTrue(
            EventShare(event(allDay = true, start = "2026-10-29T23:00:00Z", end = "2026-10-30T23:00:00Z"), GermanStrings)
                .text.contains("Freitag, 30. Oktober · ganztägig"),
        )
    }

    @Test
    fun `multi day event shows both days`() {
        val share = EventShare(event(allDay = true, start = "2026-10-27T23:00:00Z", end = "2026-11-01T23:00:00Z"), GermanStrings)
        assertEquals("Mittwoch, 28. Oktober – Sonntag, 1. November", share.dateText)
    }

    @Test
    fun `long description is shortened`() {
        val text = EventShare(event(description = "wort ".repeat(200)), GermanStrings).text
        assertTrue(text.contains("…"))
        assertFalse(text.contains("wort ".repeat(100)))
    }

    // --- Medium

    private val media = LibraryMedia(
        id = "med_1", title = "Analysis 1", subtitle = " Eine Einführung ", authors = listOf("Forster, Otto"),
        edition = "9. Aufl.", year = "2016", catalogUrl = "https://katalog.example/med_1",
    )

    private fun item(label: String, place: String?) = LibraryItem(
        label = label, location = place?.let { LibraryLocation(null, null, it, null) },
        status = LibraryAvailabilityStatus.Available, dueDate = null, reserveUrl = null, queue = null,
    )

    @Test
    fun `media text without detail has no place line`() {
        val text = LibraryMediaShare(media, GermanStrings, more = "u. a.").text
        assertEquals(
            "Aus dem Katalog der UB Ilmenau:\nAnalysis 1\nEine Einführung\nOtto Forster · 9. Aufl., 2016\n\n" +
                "https://katalog.example/med_1\n\n$BRAND",
            text,
        )
    }

    @Test
    fun `media text takes the place of the first item`() {
        val text = LibraryMediaShare(
            media, GermanStrings, "u. a.", items = listOf(item("ZQ 5000 R444(16)-1", "Lehrbuchsammlung, 2. OG"), item("x", "y")),
        ).text
        assertTrue(text.contains("\nLehrbuchsammlung, 2. OG · ZQ 5000 R444(16)\n"))
    }

    @Test
    fun `online media says so instead of a place`() {
        val text = LibraryMediaShare(media, GermanStrings, "u. a.", items = listOf(item("a", "b")), isOnline = true).text
        assertTrue(text.contains("\nOnline im Campusnetz\n"))
        assertFalse(text.contains("· a"))
    }

    @Test
    fun `signature loses the copy suffix`() {
        assertEquals("ZQ 5000 R444(16)", LibraryMediaShare.signature("ZQ 5000 R444(16)-1"))
        assertEquals("ZQ 5000 R444(16)", LibraryMediaShare.signature("ZQ 5000 R444(16)-12"))
        assertEquals("ZQ 5000", LibraryMediaShare.signature(" ZQ 5000 "))
        assertEquals(null, LibraryMediaShare.signature("  "))
    }

    @Test
    fun `media without catalog url ends with the brand only`() {
        val text = LibraryMediaShare(media.copy(catalogUrl = null), GermanStrings, "u. a.").text
        assertTrue(text.endsWith("2016\n\n$BRAND"))
    }
}
