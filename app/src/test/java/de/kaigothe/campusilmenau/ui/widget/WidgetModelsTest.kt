package de.kaigothe.campusilmenau.ui.widget

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.heute.HeuteContent
import de.kaigothe.campusilmenau.domain.model.heute.HeuteEvent
import de.kaigothe.campusilmenau.domain.model.mensa.MensaDiet
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.domain.model.mensa.OpeningHour
import de.kaigothe.campusilmenau.testing.testCanteen
import de.kaigothe.campusilmenau.testing.testMeal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests für `WidgetModelsTest.kt`-Abbildung: Was das Widget zeigt, folgt der Jetzt-Karte und den Abend-Veranstaltungen der Startseite. */
class WidgetModelsTest {
    /** Dienstag, 24.11.2026. */
    private val day = LocalDate.of(2026, 11, 24)

    private fun at(hour: Int, minute: Int = 0): Instant = BerlinTime.at(day, hour * 60 + minute)

    private val canteens = listOf(
        testCanteen("Mensa Ehrenberg", *listOf("mon", "tue", "wed", "thu", "fri").map { OpeningHour(it, "11:00", "14:00") }.toTypedArray()),
    )

    private val meals = listOf(
        testMeal("m1", "Pasta"),
        testMeal("m2", "Schnitzel"),
        testMeal("m3", "Linsen"),
        testMeal("m4", "Suppe"),
    )

    /** Ressourcen bleiben als Nummer stehen, fertige Texte als sie selbst: So prüft der Test Auswahl und Reihenfolge. */
    private val resolve: (UiText) -> String = { text -> if (text.id == R.string.plain) text.args.single().toString() else "res:${text.id}" }

    private fun content(now: Instant, filter: MensaFilter = MensaFilter(), withMeals: Boolean = true) = HeuteContent(
        now = now,
        venue = MensaVenue.MensaEhrenberg,
        filter = filter,
        canteens = canteens,
        mealsToday = if (withMeals) meals else null,
        mealsTomorrow = emptyList(),
        eventsEvening = emptyList(),
    )

    @Test
    fun `at lunch the widget shows the venue with the first three dishes`() {
        val data = content(at(12)).toMensaWidgetData(resolve)
        assertEquals("Mensa Ehrenberg", data.title)
        assertEquals(listOf("Pasta", "Schnitzel", "Linsen"), data.rows.map { it.text })
        assertTrue(data.rows.all { it.trailing != null })
    }

    @Test
    fun `a tap on the header opens the menu of the venue and a tap on a row opens the dish`() {
        val data = content(at(12)).toMensaWidgetData(resolve)
        assertEquals("campusilmenau://mensa?mensa=mensaEhrenberg", data.url)
        assertEquals("campusilmenau://mensa/gericht/m1", data.rows.first().url)
    }

    @Test
    fun `the filter of the app applies to the widget`() {
        val vegan = testMeal("v1", "Linsen-Dal", badges = setOf(de.kaigothe.campusilmenau.domain.model.mensa.MealBadge.Vegan))
        val data = HeuteContent(
            now = at(12), venue = MensaVenue.MensaEhrenberg, filter = MensaFilter(diet = MensaDiet.Vegan),
            canteens = canteens, mealsToday = meals + vegan, mealsTomorrow = emptyList(), eventsEvening = emptyList(),
        ).toMensaWidgetData(resolve)
        assertEquals(listOf("Linsen-Dal"), data.rows.map { it.text })
    }

    @Test
    fun `without data the widget shows the error card of the start page`() {
        val data = content(at(12), withMeals = false).toMensaWidgetData(resolve)
        assertEquals("Mensa Ehrenberg", data.title)
        assertTrue(data.rows.isEmpty())
        assertEquals("res:${R.string.heute_speiseplan_nicht_verfuegbar}", data.subtitle)
        assertEquals("campusilmenau://mensa?mensa=mensaEhrenberg", data.url)
    }

    // --- Heute Abend

    private fun event(id: String, hour: Int, place: String? = "Club", allDay: Boolean = false) = HeuteEvent(
        id = id, title = "Event $id", start = at(hour), end = null, allDay = allDay, cancelled = false, location = place, organizer = null,
    )

    private fun evening(now: Instant, vararg events: HeuteEvent) = HeuteContent(
        now = now, venue = MensaVenue.MensaEhrenberg, canteens = canteens, mealsToday = meals, mealsTomorrow = emptyList(),
        eventsEvening = events.toList(),
    )

    @Test
    fun `the evening widget lists the events in time order with time and place`() {
        val data = evening(at(16), event("b", 21), event("a", 19, place = "  Mensa ")).toAbendWidgetData()
        assertEquals(listOf("a", "b"), data.rows.map { it.title.removePrefix("Event ") })
        assertEquals(listOf("19:00", "21:00"), data.rows.map { it.time })
        assertEquals(listOf("Mensa", "Club"), data.rows.map { it.place })
        assertEquals("campusilmenau://events/a", data.rows.first().url)
        assertEquals("campusilmenau://events?filter=heute-abend", data.url)
    }

    @Test
    fun `the evening widget has at most three events and skips all day and finished ones`() {
        val data = evening(at(16), event("1", 18), event("2", 19), event("3", 20), event("4", 21), event("x", 10, allDay = true)).toAbendWidgetData()
        assertEquals(3, data.rows.size)
        assertTrue(data.rows.none { it.title == "Event x" })
    }

    @Test
    fun `an evening without events gives an empty list`() {
        assertTrue(evening(at(16)).toAbendWidgetData().rows.isEmpty())
    }
}
