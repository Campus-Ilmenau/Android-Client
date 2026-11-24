package de.kaigothe.campusilmenau.testing

import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.data.repository.MensaRepository
import de.kaigothe.campusilmenau.domain.model.mensa.Canteen
import de.kaigothe.campusilmenau.domain.model.mensa.MealBadge
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealPrice
import de.kaigothe.campusilmenau.domain.model.mensa.OpeningHour
import java.io.IOException
import java.time.LocalDate

fun testMeal(
    id: String,
    name: String = id,
    canteen: String = "Mensa Ehrenberg",
    category: String = "Mittagessen",
    badges: Set<MealBadge> = emptySet(),
    allergies: List<String> = emptyList(),
    date: LocalDate = LocalDate.of(2026, 11, 24),
) = MensaMeal(
    id = id,
    category = category,
    name = name,
    description = null,
    price = MensaMealPrice(2.9, 4.4, 5.8, "€"),
    allergies = allergies,
    badges = badges,
    additives = emptyList(),
    categories = emptyList(),
    co2GramPerPortion = null,
    components = emptyList(),
    canteenName = canteen,
    date = date,
)

fun testCanteen(name: String, vararg hours: OpeningHour) = Canteen(
    rscId = null,
    name = name,
    hasMenu = true,
    city = null,
    address = null,
    openingHours = hours.toList(),
)

/**
 * Repository mit festen Antworten. [mealsByDay] fehlt der Tag, antwortet es mit einem Netzfehler.
 * [gate] hält Anfragen an, bis der Test sie freigibt (Ladevorgänge, die sich überholen).
 */
class FakeMensaRepository(
    var mealsByDay: Map<LocalDate, List<MensaMeal>> = emptyMap(),
    var canteens: List<Canteen>? = emptyList(),
    var gate: (suspend (LocalDate) -> Unit)? = null,
) : MensaRepository {

    val requestedDays = mutableListOf<LocalDate>()
    val forced = mutableListOf<Boolean>()

    override suspend fun meals(day: LocalDate, forceRefresh: Boolean): List<MensaMeal> {
        requestedDays += day
        forced += forceRefresh
        gate?.invoke(day)
        return mealsByDay[day] ?: throw ApiError.Network(IOException("offline"))
    }

    override suspend fun canteens(forceRefresh: Boolean): List<Canteen> =
        canteens ?: throw ApiError.Network(IOException("offline"))

    override suspend fun meal(id: String, day: LocalDate): MensaMeal? = meals(day).firstOrNull { it.id == id }
}
