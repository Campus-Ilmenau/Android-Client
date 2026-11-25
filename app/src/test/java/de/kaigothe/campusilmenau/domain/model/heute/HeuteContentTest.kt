package de.kaigothe.campusilmenau.domain.model.heute

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.mensa.MealBadge
import de.kaigothe.campusilmenau.domain.model.mensa.MensaDiet
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.domain.model.mensa.OpeningHour
import de.kaigothe.campusilmenau.testing.testCanteen
import de.kaigothe.campusilmenau.testing.testMeal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Testfälle aus `HeuteModelTests` der iOS-App (Phasen, Begrüßung, Karten, Hinweise, Termine). */
class HeuteContentTest {

    /** Dienstag, 24.11.2026. */
    private val day = LocalDate.of(2026, 11, 24)

    private fun at(hour: Int, minute: Int = 0, date: LocalDate = day): Instant = BerlinTime.at(date, hour * 60 + minute)

    private fun weekdays(opens: String, closes: String) =
        listOf("mon", "tue", "wed", "thu", "fri").map { OpeningHour(it, opens, closes) }.toTypedArray()

    private val canteens = listOf(
        testCanteen("Mensa Ehrenberg", *weekdays("11:00", "14:00")),
        testCanteen("Cafeteria Mensa Ehrenberg", *weekdays("08:00", "20:00")),
        testCanteen("Cafeteria NANOteria", *weekdays("11:00", "14:00")),
    )

    private val lunch = listOf(
        testMeal("m1", "Pasta", badges = setOf(MealBadge.Vegetarian)),
        testMeal("m2", "Schnitzel"),
        testMeal("m3", "Linsen", badges = setOf(MealBadge.Vegan, MealBadge.Vegetarian)),
        testMeal("m4", "Suppe"),
    )
    private val dinner = listOf(
        testMeal("d1", "Burger", canteen = "Cafeteria Mensa Ehrenberg", category = "Abendessen"),
        testMeal("d2", "Salat", canteen = "Cafeteria Mensa Ehrenberg", category = "Abendessen"),
    )
    private val tomorrow = listOf(testMeal("t1", "Reis", date = day.plusDays(1)))

    private fun content(
        now: Instant,
        venue: MensaVenue = MensaVenue.MensaEhrenberg,
        filter: MensaFilter = MensaFilter(),
        meals: List<de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal>? = lunch + dinner,
        mealsTomorrow: List<de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal>? = tomorrow,
        withCanteens: Boolean = true,
        eventsEvening: List<HeuteEvent>? = emptyList(),
        eventsTomorrow: List<HeuteEvent>? = emptyList(),
        notices: List<Hinweis> = emptyList(),
        dismissed: Set<String> = emptySet(),
    ) = HeuteContent(
        now = now,
        venue = venue,
        filter = filter,
        canteens = if (withCanteens) canteens else null,
        mealsToday = meals,
        mealsTomorrow = mealsTomorrow,
        eventsEvening = eventsEvening,
        eventsTomorrow = eventsTomorrow,
        notices = notices,
        dismissedNotices = dismissed,
    )

    private fun event(id: String, startHour: Int, startMinute: Int = 0, endHour: Int? = null, allDay: Boolean = false, date: LocalDate = day) =
        HeuteEvent(
            id = id, title = "Event $id", start = at(startHour, startMinute, date),
            end = endHour?.let { at(it, 0, date) }, allDay = allDay, cancelled = false, location = "Club", organizer = "Verein",
        )

    private fun plain(text: String) = UiText.plain(text)

    // MARK: Phasen und Begrüßung

    @Test
    fun Phasengrenzen() {
        assertEquals(Tagesphase.Morgen, Tagesphase.of(at(10, 29)))
        assertEquals(Tagesphase.Mittag, Tagesphase.of(at(10, 30)))
        assertEquals(Tagesphase.Mittag, Tagesphase.of(at(13, 59)))
        assertEquals(Tagesphase.Nachmittag, Tagesphase.of(at(14)))
        assertEquals(Tagesphase.Nachmittag, Tagesphase.of(at(16, 29)))
        assertEquals(Tagesphase.Abend, Tagesphase.of(at(16, 30)))
        assertEquals(Tagesphase.Abend, Tagesphase.of(at(19, 29)))
        assertEquals(Tagesphase.Nacht, Tagesphase.of(at(19, 30)))
        assertEquals(Tagesphase.Morgen, Tagesphase.of(at(0)))
    }

    @Test
    fun Begruessungsgrenzen() {
        assertEquals(Begruessung.GutenMorgen, Begruessung.of(at(10, 59)))
        assertEquals(Begruessung.Mahlzeit, Begruessung.of(at(11)))
        assertEquals(Begruessung.Mahlzeit, Begruessung.of(at(13, 59)))
        assertEquals(Begruessung.Hallo, Begruessung.of(at(14)))
        assertEquals(Begruessung.Hallo, Begruessung.of(at(17, 59)))
        assertEquals(Begruessung.GutenAbend, Begruessung.of(at(18)))
    }

    @Test
    fun `naechster Wechsel von Phase oder Begruessung`() {
        assertEquals(at(10, 30), Tagesphase.nextChange(at(10, 0)))
        assertEquals(at(11), Tagesphase.nextChange(at(10, 30)))
        assertEquals(at(18), Tagesphase.nextChange(at(17, 0)))
        assertEquals(at(19, 30), Tagesphase.nextChange(at(18, 0)))
        // Nach 19:30 ist der nächste Wechsel Mitternacht.
        assertEquals(BerlinTime.startOfDay(day.plusDays(1)), Tagesphase.nextChange(at(20)))
    }

    @Test
    fun `Ueberschrift der Karte je Phase`() {
        assertEquals(UiText(R.string.heute_ueberschrift_heute), Tagesphase.Morgen.heading)
        assertEquals(UiText(R.string.heute_ueberschrift_jetzt), Tagesphase.Mittag.heading)
        assertEquals(UiText(R.string.heute_ueberschrift_gleich), Tagesphase.Nachmittag.heading)
    }

    @Test
    fun `Events stehen ab 15 Uhr und nicht nachts zuerst`() {
        assertFalse(content(at(14, 59)).eventsFirst)
        assertTrue(content(at(15)).eventsFirst)
        assertTrue(content(at(19, 29)).eventsFirst)
        assertFalse(content(at(19, 30)).eventsFirst)
    }

    // MARK: Mensa-Karte

    @Test
    fun `morgens zeigt die Karte zwei Gerichte und den Beginn`() {
        val card = content(at(9)).currentCard!!
        assertEquals(plain("Mensa Ehrenberg"), card.title)
        assertEquals(UiText(R.string.heute_kicker_ab, plain("11:00")), card.kicker)
        assertEquals(UiText(R.string.heute_vorschau_mittag), card.subtitle)
        assertEquals(listOf("m1", "m2"), card.rows.map { it.id })
        assertEquals(HeuteZiel.Speiseplan(MensaVenue.MensaEhrenberg), card.ziel)
        assertTrue(card.changeVenueVisible)
        assertEquals(HeuteJetztKarte.Style.Mensa, card.style)
    }

    @Test
    fun `mittags zeigt die Karte drei Gerichte und das Ende`() {
        val card = content(at(12)).currentCard!!
        assertEquals(UiText(R.string.heute_kicker_offen_bis, plain("14:00")), card.kicker)
        assertEquals(listOf("m1", "m2", "m3"), card.rows.map { it.id })
        assertEquals(HeuteZiel.Gericht("m1", null), card.rows.first().ziel)
        assertNotNull(card.rows.first().right)
    }

    @Test
    fun `der Mensa-Filter gilt auch fuer die Karte`() {
        val card = content(at(12), filter = MensaFilter(MensaDiet.Vegan)).currentCard!!
        assertEquals(listOf("m3"), card.rows.map { it.id })
    }

    @Test
    fun `ohne Speiseplan oder Oeffnungszeiten entfaellt die Karte`() {
        assertNull(content(at(12), meals = null).currentCard)
        assertNull(content(at(12), withCanteens = false).currentCard)
    }

    @Test
    fun `ohne Angebot sagt die Karte es ausdruecklich`() {
        assertEquals(
            UiText(R.string.heute_keine_angebote),
            content(at(12), meals = emptyList()).currentCard!!.subtitle,
        )
        // Nanoteria hat heute keine Gerichte und nach 14 Uhr zu.
        val closed = content(at(13), venue = MensaVenue.Roentgenbau).currentCard!!
        assertEquals(UiText(R.string.heute_geschlossen), closed.kicker)
        assertEquals(UiText(R.string.heute_mensa_geschlossen), closed.subtitle)
    }

    @Test
    fun `die Fehlerkarte nennt die gewaehlte Mensa`() {
        val card = content(at(12), meals = null).errorCard
        assertEquals(plain("Mensa Ehrenberg"), card.title)
        assertEquals(UiText(R.string.heute_speiseplan_nicht_verfuegbar), card.subtitle)
        assertTrue(card.rows.isEmpty())
    }

    // MARK: Abendessen

    @Test
    fun `nachmittags kommt das Abendessen als Karte`() {
        val card = content(at(15)).currentCard!!
        assertEquals(UiText(R.string.heute_kicker_heute_ab, plain("16:30")), card.kicker)
        assertEquals(UiText(R.string.heute_abend_in, "Mensa Ehrenberg"), card.title)
        assertEquals(listOf("d1", "d2"), card.rows.map { it.id })
        assertEquals(HeuteZiel.Speiseplan(MensaVenue.CafeteriaEhrenberg), card.ziel)
    }

    @Test
    fun `abends ist das Abendessen offen`() {
        val card = content(at(17)).currentCard!!
        assertEquals(UiText(R.string.heute_abend_jetzt_offen), card.kicker)
        assertEquals(plain("Mensa Ehrenberg"), card.title)
    }

    @Test
    fun `eine Mensa in einem anderen Haus bekommt das Abendessen als Tipp`() {
        val card = content(at(17), venue = MensaVenue.Nanoteria).currentCard!!
        assertEquals(UiText(R.string.heute_hat_geschlossen, MensaVenue.Nanoteria.displayName), card.kicker)
        assertEquals(UiText(R.string.heute_abend_tipp_jetzt), card.subtitle)
        val earlier = content(at(15), venue = MensaVenue.Nanoteria).currentCard!!
        assertEquals(
            UiText(R.string.heute_tipp_ab, plain("16:30"), UiText(R.string.heute_ort_cafeteria)),
            earlier.subtitle,
        )
    }

    @Test
    fun `ohne Abendessen bleibt die gewaehlte Mensa`() {
        val card = content(at(15), meals = lunch).currentCard!!
        assertEquals(plain("Mensa Ehrenberg"), card.title)
        assertEquals(UiText(R.string.heute_mittagsangebot), card.subtitle)
    }

    // MARK: Nacht und Events

    @Test
    fun `nachts zeigt die Karte das laufende Event mit dem naechsten danach`() {
        val running = event("a", 19, 0, endHour = 23)
        val later = event("b", 22)
        val card = content(at(20), eventsEvening = listOf(running, later)).currentCard!!
        assertEquals(HeuteJetztKarte.Style.Event, card.style)
        assertEquals(plain("Event a"), card.title)
        assertEquals(UiText(R.string.heute_laeuft_seit, plain("19:00")), card.kicker)
        assertEquals(plain("Club · Verein"), card.subtitle)
        assertEquals(listOf("b"), card.rows.map { it.id })
        assertEquals("22:00", card.rows.single().right)
        assertFalse(card.changeVenueVisible)
        assertEquals(HeuteZiel.AlleEvents, card.ziel)
    }

    @Test
    fun `nachts ohne laufendes Event kommt das naechste`() {
        val card = content(at(19, 45), eventsEvening = listOf(event("c", 21))).currentCard!!
        assertEquals(UiText(R.string.heute_kicker_ab, plain("21:00")), card.kicker)
    }

    @Test
    fun `nachts ohne Event zeigt die Karte den Mittag von morgen`() {
        val card = content(at(21)).currentCard!!
        assertEquals(UiText(R.string.heute_mittag_morgen), card.subtitle)
        assertEquals(HeuteZiel.Gericht("t1", day.plusDays(1).toString()), card.rows.single().ziel)
        assertNull(content(at(21), mealsTomorrow = emptyList()).currentCard)
    }

    @Test
    fun `Heute Abend zeigt hoechstens drei Events und laesst Beendete weg`() {
        val events = listOf(
            event("1", 18, endHour = 19), event("2", 20), event("3", 21), event("4", 22), event("5", 23),
            event("ganztag", 0, allDay = true),
        )
        val list = content(at(19, 30), eventsEvening = events).eveningEvents
        // Event 1 ist vorbei, das laufende Event 2 steht in der Jetzt-Karte und fehlt hier.
        assertEquals(listOf("3", "4", "5"), list.map { it.id })
    }

    @Test
    fun `vor 19 Uhr 30 bleibt das erste Event in der Liste`() {
        val list = content(at(18), eventsEvening = listOf(event("2", 20), event("1", 19))).eveningEvents
        assertEquals(listOf("1", "2"), list.map { it.id })
    }

    // MARK: Hinweise

    private fun notice(id: String, priority: Int = 0, from: Instant? = null, until: Instant? = null) =
        Hinweis(id, "Studium", title = id, text = "Text", priority = priority, from = from, until = until)

    @Test
    fun `Hinweise gelten im Zeitraum von bis exklusiv`() {
        val notice = notice("a", from = at(10), until = at(12))
        assertFalse(notice.appliesAt(at(9, 59)))
        assertTrue(notice.appliesAt(at(10)))
        assertTrue(notice.appliesAt(at(11, 59)))
        assertFalse(notice.appliesAt(at(12)))
        assertTrue(notice("b").appliesAt(at(3)))
    }

    @Test
    fun `Hinweise nach Prioritaet, hoechstens zwei, ohne ausgeblendete`() {
        val all = listOf(notice("low", 1), notice("high", 9), notice("mid", 5), notice("expired", 99, until = at(8)))
        assertEquals(listOf("high", "mid"), content(at(12), notices = all).activeNotices.map { it.id })
        assertEquals(listOf("mid", "low"), content(at(12), notices = all, dismissed = setOf("high")).activeNotices.map { it.id })
        assertTrue(content(at(12)).activeNotices.isEmpty())
    }

    // MARK: Als Nächstes

    @Test
    fun `morgens oeffnet und schliesst die Mensa, danach Abendessen`() {
        val list = content(at(9)).upcoming
        assertEquals(listOf("mensa-oeffnet", "mensa-schliesst", "abendessen"), list.map { it.id })
        assertEquals(listOf("11:00", "14:00", "16:30"), list.map { it.time })
        assertEquals(HeuteZiel.Oeffnungszeiten(MensaVenue.MensaEhrenberg), list[1].ziel)
    }

    @Test
    fun `mittags faellt Oeffnet weg`() {
        assertEquals(listOf("mensa-schliesst", "abendessen"), content(at(12)).upcoming.map { it.id })
    }

    @Test
    fun `hoechstens drei Termine, Events nach dem Abendessen`() {
        val list = content(at(9), eventsEvening = listOf(event("x", 20))).upcoming
        assertEquals(3, list.size)
        val afternoon = content(at(15), eventsEvening = listOf(event("x", 20))).upcoming
        assertEquals(listOf("abendessen", "event-x"), afternoon.map { it.id })
        assertEquals(HeuteFarbe.Blue, afternoon.last().color)
        assertEquals(HeuteZiel.Event("x"), afternoon.last().ziel)
    }

    @Test
    fun `nachts kommt der Mittag von morgen, dann das naechste Event`() {
        val list = content(at(21), eventsTomorrow = listOf(event("m", 20, date = day.plusDays(1)))).upcoming
        assertEquals(listOf("mittag-morgen", "event-m"), list.map { it.id })
        assertEquals(MensaFormattingWeekday.of(day.plusDays(1)), list.last().time)
    }

    // MARK: Nächster Wechsel und Mensa-Wahl

    @Test
    fun `naechster Wechsel ist der fruehste Zeitpunkt`() {
        // 09:00: Phase 10:30, Mensa öffnet 11:00, Abendessen 16:30 → 10:30.
        assertEquals(at(10, 30), content(at(9)).nextChange)
        // Ein Event davor gewinnt.
        assertEquals(at(9, 30), content(at(9), eventsEvening = listOf(event("e", 9, 30))).nextChange)
        // Abends nach Eventende.
        assertEquals(at(20), content(at(19, 45), eventsEvening = listOf(event("e", 19, endHour = 20))).nextChange)
    }

    @Test
    fun `Mensa-Wahl zeigt Zeiten, Abendessen und geschlossene Mensen`() {
        val options = content(at(12)).venueOptions.associate { it.venue to it.info }
        assertEquals(HeuteContent.MensaInfo.Hours(660, 840, dinner = false), options[MensaVenue.MensaEhrenberg])
        assertEquals(HeuteContent.MensaInfo.Hours(480, 1200, dinner = true), options[MensaVenue.CafeteriaEhrenberg])
        assertEquals(HeuteContent.MensaInfo.ClosedToday, options[MensaVenue.Roentgenbau])
        assertNotNull(options[MensaVenue.Nanoteria])
        assertNull(content(at(12), withCanteens = false).venueOptions.first().info)
    }

    private object MensaFormattingWeekday {
        fun of(date: LocalDate) = de.kaigothe.campusilmenau.domain.model.mensa.MensaFormatting.weekdayShort(date)
    }
}
