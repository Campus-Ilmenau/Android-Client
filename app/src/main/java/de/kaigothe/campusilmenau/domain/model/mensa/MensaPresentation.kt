package de.kaigothe.campusilmenau.domain.model.mensa

/** Farbfamilie eines Chips. Die Oberfläche bildet sie auf die Chip-Varianten des Design-Systems ab. */
enum class ChipTone { Green, Blue, Neutral }

/** Ein Chip in Liste und Detail. Die Titel der vier festen Chips kommen aus den String-Ressourcen. */
sealed interface MealChip {
    val tone: ChipTone

    data object Vegan : MealChip {
        override val tone = ChipTone.Green
    }

    data object Vegetarian : MealChip {
        override val tone = ChipTone.Neutral
    }

    data object Bio : MealChip {
        override val tone = ChipTone.Green
    }

    data object Regional : MealChip {
        override val tone = ChipTone.Blue
    }

    /** Eine weitere Kategorie der API (zum Beispiel „Kinderessen“), nur im Detail, mit dem Namen der API. */
    data class Other(val name: String) : MealChip {
        override val tone = ChipTone.Neutral
    }
}

/** Angaben zu einer Komponente („Bestandteile“), getrennt nach Allergenen und Zusatzstoffen. */
data class MensaComponentInfo(
    val name: String,
    val allergens: List<String>,
    val additives: List<String>,
) {
    /** Weder Allergene noch Zusatzstoffe: Die Oberfläche zeigt „Keine weiteren Angaben“. */
    val isEmpty: Boolean get() = allergens.isEmpty() && additives.isEmpty()
}

/**
 * Zusatzstoffe, wie die API sie kennt (`/v1/mensa/codes`, Kategorie `additive`). Die Codes einer Komponente kommen
 * ohne Kategorie, die Zuordnung läuft deshalb über die Namen.
 */
private val KnownAdditives: Set<String> = setOf(
    "Farbstoff", "Konservierungsstoff", "Antioxidationsmittel", "Geschmacksverstärker", "Geschwärzt", "Gewachst",
    "Phosphat", "Süßungsmittel", "Phenylalaninquelle", "Kakaohaltige Fettglasur", "Koffeinhaltig", "Chininhaltig",
    "Tierische Gelatine", "Tierisches Lab", "Karmin", "Sepia", "Honig",
)

/**
 * Die Chips für Liste und Widget: nur Vegan, Vegetarisch, Bio und Regional. Ein veganes Gericht zeigt nur „Vegan“
 * (das Label „vegetarisch“ gilt auch für vegane, deshalb erscheint es dort nicht zusätzlich).
 */
val MensaMeal.listChips: List<MealChip>
    get() = buildList {
        val isVegan = MealBadge.Vegan in badges
        if (isVegan) add(MealChip.Vegan)
        if (!isVegan && MealBadge.Vegetarian in badges) add(MealChip.Vegetarian)
        if (MealBadge.Bio in badges) add(MealChip.Bio)
        if (categories.any { it.contains("regional", ignoreCase = true) }) add(MealChip.Regional)
    }

/** Wie in der Liste, dazu weitere Kategorien der API (zum Beispiel „Kinderessen“) als neutrale Chips, ohne Doppelte. */
val MensaMeal.detailChips: List<MealChip>
    get() {
        val covered = setOf("vegan", "vegetarisch", "bio", "regional")
        val others = categories
            .filter { it.lowercase() !in covered }
            .distinct()
            .map(MealChip::Other)
        return listChips + others
    }

/** Die API liefert manche Allergene doppelt. */
val MensaMeal.uniqueAllergies: List<String> get() = allergies.distinct()

val MensaMeal.uniqueAdditives: List<String> get() = additives.distinct()

/**
 * Allergene und Zusatzstoffe je Komponente. Die Codes einer Komponente tragen keine Kategorie: Allergene stammen
 * aus der festen Liste und dem Gericht, Zusatzstoffe aus der Liste der API und dem Gericht. Symbole wie „Vegan“ oder
 * „Knoblauch“ und „Ohne Zusatzstoffe“ gehören zu keinem von beiden.
 */
val MensaMeal.componentInfos: List<MensaComponentInfo>
    get() {
        val allergenNames = MensaFilter.knownAllergens.toSet() + allergies
        val additiveNames = KnownAdditives + additives
        return components.map { component ->
            MensaComponentInfo(
                name = component.name,
                allergens = component.codes.filter { it in allergenNames }.distinct(),
                additives = component.codes.filter { it in additiveNames }.distinct(),
            )
        }
    }

/** Favoriten zählen über den Namen, weil die ID der API pro Tag wechselt: getrimmt und klein geschrieben. */
object MensaFavorites {
    fun key(meal: MensaMeal): String = key(meal.name)

    fun key(name: String): String = name.trim().lowercase()
}
