package de.kaigothe.campusilmenau.data.remote.mensa

import de.kaigothe.campusilmenau.domain.model.mensa.Canteen
import de.kaigothe.campusilmenau.domain.model.mensa.MealBadge
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealComponent
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealPrice
import de.kaigothe.campusilmenau.domain.model.mensa.OpeningHour
import java.time.ZoneOffset

/**
 * Übersetzt die Antworten der API in die Modelle der App. Die Regeln stammen aus `MensaService` der iOS-App:
 * - Gruppe: `mealGroup`, sonst leer (die Oberfläche zeigt dann „Gericht“).
 * - Beschreibung: die Namen der Komponenten, mit „, “ verbunden.
 * - Badges: Vegan und Vegetarisch aus den Flags, Bio, wenn ein Symbol-Code „bio“ enthält.
 * - Der Tag ist der Tag der API-Zeit in UTC (`2026-11-24T00:00:00.000Z` ist der 24.11.).
 */
internal fun MealDto.toDomain(): MensaMeal = MensaMeal(
    id = id,
    category = mealGroup.orEmpty(),
    name = title,
    description = components.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name },
    price = MensaMealPrice(
        student = price?.student,
        staff = price?.staff,
        guest = price?.guest,
        currency = price?.currency ?: "€",
    ),
    allergies = allergenCodes.mapNotNull { it.name },
    badges = buildSet {
        if (flags.vegan) add(MealBadge.Vegan)
        if (flags.vegetarian) add(MealBadge.Vegetarian)
        if (symbolCodes.any { it.code.lowercase().contains("bio") }) add(MealBadge.Bio)
    },
    additives = additiveCodes.mapNotNull { it.name },
    categories = symbolCodes.mapNotNull { it.name },
    co2GramPerPortion = co2GramPerPortion,
    components = components.map { component ->
        MensaMealComponent(name = component.name, codes = component.codes.mapNotNull { it.name })
    },
    canteenName = canteen.name,
    date = date.atOffset(ZoneOffset.UTC).toLocalDate(),
)

internal fun CanteenDto.toDomain(): Canteen = Canteen(
    rscId = rscId,
    name = name,
    hasMenu = hasMenu,
    city = city,
    address = address,
    openingHours = openingHours.map { OpeningHour(weekday = it.weekday, opensAt = it.opensAt, closesAt = it.closesAt) },
)
