package de.kaigothe.campusilmenau.domain.model.mensa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.Collator
import java.util.Locale

/** Ernährungsstufe des Filters. „Vegetarisch“ lässt auch vegane Gerichte durch. Die Namen werden gespeichert. */
@Serializable
enum class MensaDiet {
    @SerialName("alle")
    Alle,

    @SerialName("vegetarisch")
    Vegetarisch,

    @SerialName("vegan")
    Vegan,
}

/**
 * Filter für den Speiseplan: Ernährungsstufe, „Nur Bio“ und ausgeschlossene Allergene, alles UND-verknüpft.
 * Er gilt für alle Mensen und Tage und wird für die Mensa-Ansicht, die Mensa-Karte auf „Heute“ und das Widget gleich
 * verwendet. Das gespeicherte Format (`diet`, `onlyBio`, `excludedAllergens`) ist das der iOS-App.
 */
@Serializable
data class MensaFilter(
    val diet: MensaDiet = MensaDiet.Alle,
    val onlyBio: Boolean = false,
    val excludedAllergens: Set<String> = emptySet(),
) {
    /** Die eine Prüfung, auf der alles andere aufbaut. */
    fun matches(badges: Set<MealBadge>, allergies: List<String>): Boolean {
        when (diet) {
            MensaDiet.Alle -> Unit
            MensaDiet.Vegetarisch -> if (MealBadge.Vegetarian !in badges && MealBadge.Vegan !in badges) return false
            MensaDiet.Vegan -> if (MealBadge.Vegan !in badges) return false
        }
        if (onlyBio && MealBadge.Bio !in badges) return false
        return allergies.none { it in excludedAllergens }
    }

    fun matches(meal: MensaMeal): Boolean = matches(meal.badges, meal.allergies)

    fun apply(meals: List<MensaMeal>): List<MensaMeal> = if (isActive) meals.filter(::matches) else meals

    /** Irgendetwas ist gesetzt. */
    val isActive: Boolean get() = diet != MensaDiet.Alle || extraCount > 0

    /** Anzahl der Einstellungen jenseits der Kapseln: „Nur Bio“ und ausgeschlossene Allergene (Zahl an der Filter-Taste). */
    val extraCount: Int get() = (if (onlyBio) 1 else 0) + excludedAllergens.size

    /** Die ausgeschlossenen Allergene in der Reihenfolge der Anzeige (alphabetisch nach deutscher Sortierung). */
    val sortedExcludedAllergens: List<String> get() = sortAllergens(excludedAllergens)

    companion object {
        /**
         * Alle 26 Allergene, die die Mensa-API kennt (`/v1/mensa/codes`, Kategorie `allergen`), mit den Namen der API.
         * Das Sheet zeigt sie immer, auch wenn der Speiseplan des Tages sie nicht enthält: Wer etwas meiden muss, soll
         * den Filter an jedem Tag im Voraus einstellen können.
         */
        val knownAllergens: List<String> = listOf(
            "Weizen", "Roggen", "Gerste", "Hafer", "Dinkel", "Kamut", "Krebstiere", "Hühnerei", "Fisch", "Erdnüsse",
            "Soja", "Milch und Milchzucker", "Mandeln", "Haselnüsse", "Walnüsse", "Cashewnüsse", "Pekannüsse",
            "Paranüsse", "Pistazien", "Macadamianüsse", "Sellerie", "Senf", "Sesam", "Schwefeloxid/Sulfite", "Lupine",
            "Weichtiere",
        )

        /**
         * Allergene für das Sheet, alphabetisch: die feste Liste, dazu alles, was ein geladenes Gericht nennt (falls die
         * API ein neues Allergen einführt), und ausgeschlossene, die nicht mehr in der Liste stehen.
         */
        fun availableAllergens(meals: List<MensaMeal>, keeping: Set<String>): List<String> =
            sortAllergens(knownAllergens.toSet() + meals.flatMap { it.allergies } + keeping)

        /** Nur die Allergene, die ein Gericht des geladenen Tages nennt, dazu die ausgeschlossenen (Popover auf dem Tablet). */
        fun allergensOnDay(meals: List<MensaMeal>, keeping: Set<String>): List<String> =
            sortAllergens(meals.flatMap { it.allergies }.toSet() + keeping)

        private fun sortAllergens(names: Set<String>): List<String> {
            val collator = Collator.getInstance(Locale.GERMAN)
            return names.sortedWith { a, b -> collator.compare(a, b) }
        }
    }
}
