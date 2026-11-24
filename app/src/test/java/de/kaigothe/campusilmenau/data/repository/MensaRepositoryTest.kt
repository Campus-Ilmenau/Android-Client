package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.core.network.ApiCaller
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.network.ApiJson
import de.kaigothe.campusilmenau.core.network.CamelCaseKeysInterceptor
import de.kaigothe.campusilmenau.data.remote.mensa.MensaApi
import de.kaigothe.campusilmenau.domain.model.mensa.MealBadge
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealtime
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.domain.model.mensa.detailChips
import de.kaigothe.campusilmenau.domain.model.mensa.listChips
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Der ganze Weg von der API bis zum Modell, mit echten Antworten der Dev-API (`src/test/resources/mensa`):
 * MockWebServer → OkHttp mit Schlüssel-Umwandlung → Retrofit → DTO → Mapper → Repository.
 */
class MensaRepositoryTest {

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant(): Instant = now
    }

    private val day = LocalDate.of(2026, 11, 24)
    private val server = MockWebServer()
    private val clock = MutableClock(Instant.parse("2026-11-24T08:00:00Z"))
    private lateinit var repository: NetworkMensaRepository

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/mensa/$name")) { "Fixture fehlt: $name" }
            .bufferedReader().use { it.readText() }

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json; charset=utf-8").setBody(body)

    @Before
    fun setUp() {
        server.start()
        val parser = ApiJson.create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(CamelCaseKeysInterceptor(parser)).build())
            .addConverterFactory(parser.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = NetworkMensaRepository(retrofit.create<MensaApi>(), ApiCaller(parser), clock, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `der Speiseplan des Tages wird mit den richtigen Parametern geladen`() = runTest {
        server.enqueue(json(fixture("meals-2026-11-24.json")))

        val meals = repository.meals(day)

        assertEquals(12, meals.size)
        val request = server.takeRequest()
        assertEquals("/v1/mensa?date=2026-11-24&per_page=100", request.path)
        assertEquals("GET", request.method)
    }

    @Test
    fun `die Gerichte verteilen sich wie in der Antwort auf die vier Mensen`() = runTest {
        server.enqueue(json(fixture("meals-2026-11-24.json")))

        val meals = repository.meals(day)

        val perVenue = MensaVenue.entries.associateWith { venue -> meals.count { venue.matches(it.canteenName) } }
        assertEquals(4, perVenue.getValue(MensaVenue.MensaEhrenberg))
        assertEquals(3, perVenue.getValue(MensaVenue.CafeteriaEhrenberg))
        assertEquals(2, perVenue.getValue(MensaVenue.Nanoteria))
        assertEquals(3, perVenue.getValue(MensaVenue.Roentgenbau))
        assertEquals(12, perVenue.values.sum())
        assertEquals(12, meals.map { it.id }.toSet().size)
    }

    @Test
    fun `die Cafeteria Ehrenberg hat Mittag und Abendessen`() = runTest {
        server.enqueue(json(fixture("meals-2026-11-24.json")))

        val cafeteria = repository.meals(day).filter { MensaVenue.CafeteriaEhrenberg.matches(it.canteenName) }

        assertEquals(1, cafeteria.count { !MensaMealtime.isDinner(it) })
        assertEquals(2, cafeteria.count { MensaMealtime.isDinner(it) })
    }

    @Test
    fun `das erste Gericht ist vollstaendig gelesen`() = runTest {
        server.enqueue(json(fixture("meals-2026-11-24.json")))

        val meal = repository.meals(day).first()

        assertEquals("meal_09eb9245-8e55-4754-bd60-b8384f16caad", meal.id)
        assertEquals("Mittagessen", meal.category)
        assertEquals("Rahm-Pfannengyros mit Paprika,Tomate und Lauch , dazu Kräuterreis", meal.name)
        assertEquals("Mensa Ehrenberg", meal.canteenName)
        assertEquals(day, meal.date)
        // Beschreibung = Namen der Komponenten.
        assertEquals("Kräuterreis, Rahm-Pfannengyros mit Paprika", meal.description)
        assertEquals(3.7, meal.price.student!!, 0.0)
        assertEquals(5.9, meal.price.staff!!, 0.0)
        assertEquals(7.3, meal.price.guest!!, 0.0)
        assertEquals("€", meal.price.currency)
        assertEquals(listOf("Milch und Milchzucker", "Sellerie", "Soja"), meal.allergies)
        assertEquals(listOf("Knoblauch", "Schweinefleisch"), meal.categories)
        assertTrue(meal.additives.isEmpty())
        assertEquals(1475.0, meal.co2GramPerPortion!!, 0.0)
        assertTrue(meal.badges.isEmpty())
        assertEquals(listOf("Kräuterreis", "Rahm-Pfannengyros mit Paprika"), meal.components.map { it.name })
        assertEquals(listOf("Ohne Zusatzstoffe", "Vegetarisch", "Vegan"), meal.components.first().codes)
    }

    @Test
    fun `Vegan und Vegetarisch kommen aus den Flags`() = runTest {
        server.enqueue(json(fixture("meals-2026-11-24.json")))

        val meals = repository.meals(day)

        val vegan = meals.filter { MealBadge.Vegan in it.badges }
        val vegetarianOnly = meals.filter { MealBadge.Vegetarian in it.badges && MealBadge.Vegan !in it.badges }
        assertEquals(4, vegan.size)
        assertEquals(3, vegetarianOnly.size)
        assertTrue(vegan.all { MealBadge.Vegetarian in it.badges })
        // Ein veganes Gericht zeigt in der Liste nur „Vegan“, im Detail dazu keine doppelten Chips.
        assertEquals(1, vegan.first().listChips.size)
        assertEquals(vegan.first().listChips, vegan.first().detailChips.filter { it in vegan.first().listChips })
        // Der Bio-Badge kommt nur aus einem Symbol mit „bio“. Die Beispieldaten haben keins.
        assertTrue(meals.none { MealBadge.Bio in it.badges })
    }

    @Test
    fun `die Mensen werden mit Oeffnungszeiten gelesen`() = runTest {
        server.enqueue(json(fixture("canteens.json")))

        val canteens = repository.canteens()

        assertEquals("/v1/mensa/canteens", server.takeRequest().path)
        assertEquals(5, canteens.size)
        val ehrenberg = canteens.first { MensaVenue.MensaEhrenberg.matches(it.name) }
        assertEquals(46, ehrenberg.rscId)
        assertEquals(5, ehrenberg.openingHours.size)
        assertEquals("11:00", ehrenberg.openingHours.first().opensAt)
        assertEquals("14:00", ehrenberg.openingHours.first().closesAt)
        // Die Cafeteria Ehrenberg öffnet auch samstags.
        val cafeteria = canteens.first { MensaVenue.CafeteriaEhrenberg.matches(it.name) }
        assertEquals("sat", cafeteria.openingHours.last().weekday)
        // Die Cafeteria Mini hat keinen Speiseplan und keine ID.
        val mini = canteens.first { it.name == "Cafeteria Mini" }
        assertFalse(mini.hasMenu)
        assertNull(mini.rscId)
        // Jede der vier Mensen der App kommt in der Antwort vor.
        assertTrue(MensaVenue.entries.all { venue -> canteens.any { venue.matches(it.name) } })
    }

    @Test
    fun `ein zweiter Aufruf nutzt den Zwischenspeicher`() = runTest {
        server.enqueue(json(fixture("meals-2026-11-24.json")))

        repository.meals(day)
        val again = repository.meals(day)

        assertEquals(12, again.size)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `nach fuenf Minuten wird neu geladen`() = runTest {
        server.enqueue(json(fixture("meals-2026-11-24.json")))
        server.enqueue(json(fixture("meals-2026-11-24.json")))

        repository.meals(day)
        clock.now = clock.now.plus(Duration.ofMinutes(4))
        repository.meals(day)
        assertEquals(1, server.requestCount)

        clock.now = clock.now.plus(Duration.ofMinutes(2))
        repository.meals(day)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `Erneut versuchen umgeht den Zwischenspeicher`() = runTest {
        server.enqueue(json(fixture("meals-2026-11-24.json")))
        server.enqueue(json(fixture("meals-2026-11-24.json")))

        repository.meals(day)
        repository.meals(day, forceRefresh = true)

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `jeder Tag hat seinen eigenen Zwischenspeicher`() = runTest {
        server.enqueue(json(fixture("meals-2026-11-24.json")))
        server.enqueue(json("""{"object":"list","has_more":false,"total_count":0,"data":[]}"""))

        assertEquals(12, repository.meals(day).size)
        assertTrue(repository.meals(day.plusDays(1)).isEmpty())
        assertEquals("/v1/mensa?date=2026-11-24&per_page=100", server.takeRequest().path)
        assertEquals("/v1/mensa?date=2026-11-25&per_page=100", server.takeRequest().path)
    }

    @Test
    fun `ein Gericht wird im Speiseplan des Tages gefunden`() = runTest {
        server.enqueue(json(fixture("meals-2026-11-24.json")))

        val found = repository.meal("meal_09eb9245-8e55-4754-bd60-b8384f16caad", day)
        assertNotNull(found)
        assertEquals("Mensa Ehrenberg", found!!.canteenName)
        // Eine unbekannte ID, zum Beispiel von einem alten Link, ergibt null (aus dem Zwischenspeicher).
        assertNull(repository.meal("meal_gibtsnicht", day))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `ein leerer Tag ist eine leere Liste, kein Fehler`() = runTest {
        server.enqueue(json("""{"object":"list","url":"/v1/mensa","has_more":false,"total_count":0,"data":[]}"""))
        assertTrue(repository.meals(day).isEmpty())
    }

    @Test
    fun `ein Serverfehler wird zu ApiError mit Status`() = runTest {
        server.enqueue(json("""{"error":{"type":"server","code":"internal","message":"kaputt","request_id":"req_9"}}""", code = 500))

        val error = runCatching { repository.meals(day) }.exceptionOrNull()

        assertTrue(error is ApiError.Http)
        error as ApiError.Http
        assertEquals(500, error.status)
        assertEquals("internal", error.code)
        assertEquals("req_9", error.requestId)
    }

    @Test
    fun `ein Fehler wird nicht zwischengespeichert`() = runTest {
        server.enqueue(json("{}", code = 503))
        server.enqueue(json(fixture("meals-2026-11-24.json")))

        assertTrue(runCatching { repository.meals(day) }.exceptionOrNull() is ApiError.Http)
        assertEquals(12, repository.meals(day).size)
    }

    @Test
    fun `unlesbare Antworten sind ein Dekodierfehler`() = runTest {
        server.enqueue(json("""{"data":[{"id":"x"}]}"""))

        val error = runCatching { repository.meals(day) }.exceptionOrNull()

        assertTrue("war: $error", error is ApiError.Decoding)
    }

    @Test
    fun `die Antwort darf wachsen, unbekannte Felder stoeren nicht`() = runTest {
        server.enqueue(
            json(
                """{"has_more":false,"total_count":1,"data":[{"id":"m1","canteen":{"name":"Mensa Ehrenberg","neu":1},
                    "date":"2026-11-24T00:00:00.000Z","title":"Suppe","irgendwas":{"a":[1,2]}}]}""",
            ),
        )

        val meal = repository.meals(day).single()

        assertEquals("Suppe", meal.name)
        assertEquals("", meal.category)
        assertNull(meal.description)
        assertNull(meal.price.student)
        assertTrue(meal.allergies.isEmpty())
    }

    @Test
    fun `Bio kommt aus einem Symbol mit bio im Code`() = runTest {
        server.enqueue(
            json(
                """{"data":[{"id":"m1","canteen":{"name":"Mensa Ehrenberg"},"date":"2026-11-24T00:00:00.000Z","title":"Salat",
                    "symbolCodes":[{"code":"Bio","name":"Aus biologischem Anbau"}],"flags":{"vegetarian":true}}]}""",
            ),
        )

        val meal = repository.meals(day).single()

        assertEquals(setOf(MealBadge.Vegetarian, MealBadge.Bio), meal.badges)
    }

    @Test
    fun `der Tag ist der UTC-Tag der API-Zeit`() = runTest {
        server.enqueue(
            json(
                """{"data":[{"id":"m1","canteen":{"name":"Mensa Ehrenberg"},"date":"2026-11-25T00:00:00.000Z","title":"Suppe"}]}""",
            ),
        )
        assertEquals(LocalDate.of(2026, 11, 25), repository.meals(day).single().date)
    }
}
