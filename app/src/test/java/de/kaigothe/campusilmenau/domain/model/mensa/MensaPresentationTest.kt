package de.kaigothe.campusilmenau.domain.model.mensa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/** Testfälle aus `MensaDetailTests` der iOS-App (Preise, Chips, Bestandteile, Favoriten). */
class MensaPresentationTest {

    private fun meal(
        name: String = "Linsen-Dal",
        badges: Set<MealBadge> = emptySet(),
        categories: List<String> = emptyList(),
        allergies: List<String> = emptyList(),
        additives: List<String> = emptyList(),
        components: List<MensaMealComponent> = emptyList(),
        price: MensaMealPrice = MensaMealPrice(2.9, 4.4, null, "€"),
    ) = MensaMeal(
        id = "x",
        category = "Mittagessen",
        name = name,
        description = null,
        price = price,
        allergies = allergies,
        badges = badges,
        additives = additives,
        categories = categories,
        co2GramPerPortion = null,
        components = components,
        canteenName = "Mensa Ehrenberg",
        date = LocalDate.of(2026, 11, 24),
    )

    private fun String.plainSpaces() = replace(' ', ' ')

    // Preise

    @Test
    fun `Preise im deutschen Format, fehlender Preis als Strich`() {
        val price = MensaMealPrice(2.9, 4.4, null, "€")
        assertEquals("2,90 €", price.formatted(MensaPriceGroup.Studierende, Locale.GERMANY).plainSpaces())
        assertEquals("4,40 €", price.formatted(MensaPriceGroup.Bedienstete, Locale.GERMANY).plainSpaces())
        assertEquals("–", price.formatted(MensaPriceGroup.Gaeste, Locale.GERMANY))
    }

    @Test
    fun `ein dreistelliger Waehrungscode gilt als Code, alles andere als Euro`() {
        assertEquals("2,00 €", MensaMealPrice.format(2.0, "EUR", Locale.GERMANY).plainSpaces())
        assertEquals("2,00 €", MensaMealPrice.format(2.0, "eur", Locale.GERMANY).plainSpaces())
        assertEquals("2,00 €", MensaMealPrice.format(2.0, "€", Locale.GERMANY).plainSpaces())
        assertEquals("2,00 €", MensaMealPrice.format(2.0, "kein Code", Locale.GERMANY).plainSpaces())
    }

    @Test
    fun `die Preisgruppe kommt aus dem Schluessel, unbekannt gilt als Studierende`() {
        assertEquals(MensaPriceGroup.Gaeste, MensaPriceGroup.fromKey("gaeste"))
        assertEquals(MensaPriceGroup.Studierende, MensaPriceGroup.fromKey("rentner"))
        assertEquals(MensaPriceGroup.Studierende, MensaPriceGroup.fromKey(null))
        // Die Schlüssel sind die der iOS-App.
        assertEquals(listOf("studierende", "bedienstete", "gaeste"), MensaPriceGroup.entries.map { it.key })
    }

    // Chips

    @Test
    fun `ein veganes Gericht zeigt in der Liste nur Vegan`() {
        val chips = meal(badges = setOf(MealBadge.Vegan, MealBadge.Vegetarian)).listChips
        assertEquals(listOf<MealChip>(MealChip.Vegan), chips)
    }

    @Test
    fun `vegetarisch, Bio und Regional in fester Reihenfolge`() {
        val chips = meal(
            badges = setOf(MealBadge.Vegetarian, MealBadge.Bio),
            categories = listOf("Regionale Spezialität"),
        ).listChips
        assertEquals(listOf(MealChip.Vegetarian, MealChip.Bio, MealChip.Regional), chips)
    }

    @Test
    fun `ohne Eigenschaften gibt es keine Chips`() {
        assertTrue(meal(categories = listOf("Knoblauch", "Schweinefleisch")).listChips.isEmpty())
    }

    @Test
    fun `das Detail ergaenzt weitere Kategorien als neutrale Chips ohne Doppelte`() {
        val chips = meal(
            badges = setOf(MealBadge.Vegan),
            categories = listOf("Regional", "Kinderessen", "Vegan", "Kinderessen"),
        ).detailChips
        assertEquals(listOf(MealChip.Vegan, MealChip.Regional, MealChip.Other("Kinderessen")), chips)
        assertEquals(ChipTone.Neutral, chips.last().tone)
    }

    @Test
    fun `Farben der Chips`() {
        assertEquals(ChipTone.Green, MealChip.Vegan.tone)
        assertEquals(ChipTone.Neutral, MealChip.Vegetarian.tone)
        assertEquals(ChipTone.Green, MealChip.Bio.tone)
        assertEquals(ChipTone.Blue, MealChip.Regional.tone)
    }

    @Test
    fun `doppelte Allergene und Zusatzstoffe erscheinen einmal`() {
        val m = meal(allergies = listOf("Sellerie", "Sellerie", "Weizen"), additives = listOf("Farbstoff", "Farbstoff"))
        assertEquals(listOf("Sellerie", "Weizen"), m.uniqueAllergies)
        assertEquals(listOf("Farbstoff"), m.uniqueAdditives)
    }

    // Bestandteile

    @Test
    fun `Codes einer Komponente werden in Allergene und Zusatzstoffe getrennt`() {
        val m = meal(
            allergies = listOf("Sellerie"),
            additives = listOf("Antioxidationsmittel"),
            components = listOf(
                MensaMealComponent("Mango-Chutney", listOf("Senf", "Antioxidationsmittel", "Vegan", "Knoblauch")),
                MensaMealComponent("Basmatireis", listOf("Ohne Zusatzstoffe", "Vegan")),
            ),
        )
        val infos = m.componentInfos
        assertEquals(listOf("Senf"), infos[0].allergens)
        assertEquals(listOf("Antioxidationsmittel"), infos[0].additives)
        // Symbole und „Ohne Zusatzstoffe“ zählen nicht.
        assertTrue(infos[1].isEmpty)
    }

    @Test
    fun `ein Allergen, das nur das Gericht nennt, wird in der Komponente erkannt`() {
        val m = meal(
            allergies = listOf("Neuallergen"),
            components = listOf(MensaMealComponent("Soße", listOf("Neuallergen", "Neuallergen"))),
        )
        assertEquals(listOf("Neuallergen"), m.componentInfos.single().allergens)
    }

    // Favoriten

    @Test
    fun `Favoriten zaehlen ueber den Namen, getrimmt und ohne Gross- und Kleinschreibung`() {
        assertEquals(MensaFavorites.key(meal(name = "Linsen-Dal")), MensaFavorites.key(meal(name = "  linsen-dal ")))
        assertTrue(MensaFavorites.key(meal(name = "Linsen-Dal")) != MensaFavorites.key(meal(name = "Pasta")))
        assertEquals("linsen-dal", MensaFavorites.key("  Linsen-DAL "))
    }
}
