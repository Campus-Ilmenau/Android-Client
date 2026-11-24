package de.kaigothe.campusilmenau.domain.model.mensa

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Testfälle aus `MensaFilterTests` der iOS-App. */
class MensaFilterTest {

    private fun meal(
        id: String,
        badges: Set<MealBadge> = emptySet(),
        allergies: List<String> = emptyList(),
        canteen: String = "Mensa Ehrenberg",
    ) = MensaMeal(
        id = id,
        category = "Mittagessen",
        name = id,
        description = null,
        price = MensaMealPrice(2.9, 4.4, 5.8, "€"),
        allergies = allergies,
        badges = badges,
        additives = emptyList(),
        categories = emptyList(),
        co2GramPerPortion = null,
        components = emptyList(),
        canteenName = canteen,
        date = LocalDate.of(2026, 11, 24),
    )

    private val meat = meal("meat", allergies = listOf("Gluten"))
    private val vegetarian = meal("vegetarian", setOf(MealBadge.Vegetarian), listOf("Milch"))
    private val vegan = meal("vegan", setOf(MealBadge.Vegan, MealBadge.Vegetarian))
    private val veganOnly = meal("veganOnly", setOf(MealBadge.Vegan), listOf("Soja"))
    private val bioVegan = meal("bioVegan", setOf(MealBadge.Vegan, MealBadge.Bio), listOf("Gluten"))
    private val bioVegetarian = meal("bioVegetarian", setOf(MealBadge.Vegetarian, MealBadge.Bio))

    private fun ids(meals: List<MensaMeal>) = meals.map { it.id }

    @Test
    fun `Standardfilter ist inaktiv und behaelt alles`() {
        val filter = MensaFilter()
        assertFalse(filter.isActive)
        assertEquals(0, filter.extraCount)
        assertEquals(listOf("meat", "vegetarian", "vegan"), ids(filter.apply(listOf(meat, vegetarian, vegan))))
    }

    @Test
    fun `vegetarisch zeigt auch vegane Gerichte`() {
        val filter = MensaFilter(diet = MensaDiet.Vegetarisch)
        // Auch ein Gericht, das nur „Vegan“ trägt, ist vegetarisch.
        assertEquals(
            listOf("vegetarian", "vegan", "veganOnly"),
            ids(filter.apply(listOf(meat, vegetarian, vegan, veganOnly))),
        )
    }

    @Test
    fun `vegan zeigt nur vegane Gerichte`() {
        val filter = MensaFilter(diet = MensaDiet.Vegan)
        assertEquals(listOf("vegan", "veganOnly"), ids(filter.apply(listOf(meat, vegetarian, vegan, veganOnly))))
    }

    @Test
    fun `Bio und Allergene sind UND-verknuepft`() {
        val filter = MensaFilter(onlyBio = true, excludedAllergens = setOf("Gluten"))
        // Bio: bioVegan, bioVegetarian. Ohne Gluten schließt bioVegan aus.
        assertEquals(listOf("bioVegetarian"), ids(filter.apply(listOf(meat, vegan, bioVegan, bioVegetarian))))
        assertEquals(2, filter.extraCount)
    }

    @Test
    fun `Ernaehrung, Bio und Allergen wirken zusammen`() {
        val filter = MensaFilter(diet = MensaDiet.Vegan, onlyBio = true, excludedAllergens = setOf("Soja"))
        assertEquals(listOf("bioVegan"), ids(filter.apply(listOf(bioVegan, bioVegetarian, veganOnly))))
    }

    @Test
    fun `ein ausgeschlossenes Allergen entfernt jedes Gericht, das eines davon nennt`() {
        val filter = MensaFilter(excludedAllergens = setOf("Milch", "Soja"))
        assertEquals(listOf("meat", "vegan"), ids(filter.apply(listOf(meat, vegetarian, veganOnly, vegan))))
    }

    @Test
    fun `der Filter kann alle Gerichte entfernen, die Ausgangsliste bleibt`() {
        val filter = MensaFilter(diet = MensaDiet.Vegan, onlyBio = true)
        val meals = listOf(meat, vegetarian, vegan)
        assertTrue(filter.apply(meals).isEmpty())
        assertEquals(3, meals.size)
        assertTrue(filter.isActive)
    }

    @Test
    fun `der Filter gilt je Mensa, ohne Mensen zu verlieren`() {
        val filter = MensaFilter(diet = MensaDiet.Vegan, onlyBio = true)
        val meals = listOf(
            meal("a", setOf(MealBadge.Vegan, MealBadge.Bio), canteen = "Cafeteria NANOteria"),
            meal("b", setOf(MealBadge.Vegetarian), canteen = "Mensa Ehrenberg"),
        )
        val filtered = filter.apply(meals)
        val byVenue = MensaVenue.entries.map { venue -> filtered.count { venue.matches(it.canteenName) } }
        // Reihenfolge: Mensa Ehrenberg, Cafeteria Ehrenberg, Nanoteria, Röntgenbau
        assertEquals(listOf(0, 0, 1, 0), byVenue)
    }

    @Test
    fun `ausgeschlossene Allergene stehen alphabetisch`() {
        val filter = MensaFilter(onlyBio = true, excludedAllergens = setOf("Gluten", "Ei", "Ärmel", "Zimt"))
        assertEquals(listOf("Ärmel", "Ei", "Gluten", "Zimt"), filter.sortedExcludedAllergens)
    }

    @Test
    fun `die feste Liste kennt alle 26 Allergene der API`() {
        val list = MensaFilter.availableAllergens(emptyList(), keeping = emptySet())
        assertEquals(26, MensaFilter.knownAllergens.size)
        assertEquals(26, MensaFilter.knownAllergens.toSet().size)
        assertEquals(26, list.size)
        // Auch Allergene, die selten im Speiseplan stehen.
        assertTrue("Walnüsse" in list)
        assertTrue("Hühnerei" in list)
        assertTrue("Milch und Milchzucker" in list)
        assertTrue("Weichtiere" in list)
    }

    @Test
    fun `die Liste ist sortiert und ohne Doppelte`() {
        val meals = listOf(meal("x", allergies = listOf("Sellerie", "Sellerie", "Hühnerei")))
        val list = MensaFilter.availableAllergens(meals, keeping = emptySet())
        assertEquals(26, list.size)
        assertEquals(list.sortedWith(java.text.Collator.getInstance(java.util.Locale.GERMAN)), list)
    }

    @Test
    fun `unbekannte Allergene aus Gerichten und ausgeschlossene bleiben sichtbar`() {
        val meals = listOf(meal("x", allergies = listOf("Sellerie", "Lupinenkaffee")))
        val list = MensaFilter.availableAllergens(meals, keeping = setOf("Alt-Allergen"))
        assertTrue("Lupinenkaffee" in list)
        assertTrue("Alt-Allergen" in list)
        assertEquals(28, list.size)
    }

    @Test
    fun `Tablet-Popover zeigt nur Allergene des Tages und ausgeschlossene`() {
        val meals = listOf(meal("x", allergies = listOf("Sellerie")))
        assertEquals(listOf("Sellerie", "Soja"), MensaFilter.allergensOnDay(meals, keeping = setOf("Soja")))
    }

    // Speicherformat: das der iOS-App (`diet`, `onlyBio`, `excludedAllergens`).

    @Test
    fun `Speicherformat entspricht dem der iOS-App`() {
        val json = Json
        val filter = MensaFilter(MensaDiet.Vegetarisch, onlyBio = true, excludedAllergens = setOf("Ei"))
        assertEquals(
            """{"diet":"vegetarisch","onlyBio":true,"excludedAllergens":["Ei"]}""",
            json.encodeToString(MensaFilter.serializer(), filter),
        )
        assertEquals(
            filter,
            json.decodeFromString(
                MensaFilter.serializer(),
                """{"diet":"vegetarisch","onlyBio":true,"excludedAllergens":["Ei"]}""",
            ),
        )
    }

    @Test
    fun `ein leeres Objekt ist der inaktive Filter`() {
        assertEquals(MensaFilter(), Json.decodeFromString(MensaFilter.serializer(), "{}"))
    }
}
