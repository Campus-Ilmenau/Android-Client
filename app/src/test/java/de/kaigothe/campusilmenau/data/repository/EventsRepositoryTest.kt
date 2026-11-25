package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.core.network.ApiCaller
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.network.ApiJson
import de.kaigothe.campusilmenau.core.network.CamelCaseKeysInterceptor
import de.kaigothe.campusilmenau.data.remote.events.EventsApi
import de.kaigothe.campusilmenau.domain.model.events.EventFilter
import de.kaigothe.campusilmenau.domain.model.events.EventStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Vom MockWebServer über die Schlüssel-Umwandlung bis zum Repository, mit Antworten im Format der Events-API. */
class EventsRepositoryTest {

    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        // 11:00 Uhr Berliner Zeit am 24.11.2026
        override fun instant(): Instant = Instant.parse("2026-11-24T10:00:00Z")
    }

    private val server = MockWebServer()
    private val requests = mutableListOf<RecordedRequest>()
    private var handler: (RecordedRequest) -> MockResponse = { MockResponse().setResponseCode(500) }
    private lateinit var repository: NetworkEventsRepository

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun event(
        id: String,
        start: String,
        end: String? = null,
        extra: String = "",
    ) = """{"id":"$id","title":"Event $id","start_at":"$start"${end?.let { ""","end_at":"$it"""" } ?: ""}$extra}"""

    private fun list(vararg events: String, hasMore: Boolean = false, total: Int = events.size) =
        """{"data":[${events.joinToString(",")}],"has_more":$hasMore,"total_count":$total,"page":1}"""

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return handler(request)
            }
        }
        server.start()
        val parser = ApiJson.create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(CamelCaseKeysInterceptor(parser)).build())
            .addConverterFactory(parser.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = NetworkEventsRepository(retrofit.create<EventsApi>(), ApiCaller(parser), clock, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `die Liste fragt mit verbreitertem Fenster, Seite und Seitengroesse`() = runTest {
        handler = { json(list()) }

        repository.list(EventFilter())

        val first = requests.first().requestUrl!!
        assertEquals("/v1/events", first.encodedPath)
        assertEquals("2026-11-23", first.queryParameter("from"))
        assertEquals("true", first.queryParameter("include_cancelled"))
        assertEquals("1", first.queryParameter("page"))
        assertEquals("100", first.queryParameter("per_page"))
    }

    @Test
    fun `Treffer vor dem Berliner Tagesbeginn werden abgeschnitten`() = runTest {
        // 23:30 Berliner Zeit am 23.11. liegt im verbreiterten UTC-Fenster, aber vor dem Berliner Tag.
        handler = { request ->
            if (request.requestUrl!!.queryParameter("date") != null) {
                json(list())
            } else {
                json(
                    list(
                        event("zu-frueh", "2026-11-23T22:30:00.000Z"),
                        event("heute-0", "2026-11-23T23:00:00.000Z"),
                        event("spaeter", "2026-11-25T18:00:00Z"),
                    ),
                )
            }
        }

        val page = repository.list(EventFilter())

        assertEquals(listOf("heute-0", "spaeter"), page.events.map { it.id })
    }

    @Test
    fun `mit Zeitraum schneidet auch das Ende ab`() = runTest {
        handler = {
            json(
                list(
                    event("drin", "2026-11-26T18:00:00Z"),
                    // Ab 28.11. 00:00 Berliner Zeit ist es außerhalb von „bis 28.11.“ (exklusiv).
                    event("draussen", "2026-11-27T23:00:00Z"),
                ),
            )
        }

        val page = repository.list(EventFilter(period = EventFilter.Period(LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 28))))

        assertEquals(listOf("drin"), page.events.map { it.id })
        // Der Tag nach „bis“ beginnt um 00:00 Berliner Zeit, das ist der 28.11. in UTC.
        assertEquals("2026-11-28", requests.first().requestUrl!!.queryParameter("to"))
    }

    @Test
    fun `laufende mehrtaegige Veranstaltungen kommen dazu, ohne Doppelte, nach Beginn sortiert`() = runTest {
        handler = { request ->
            if (request.requestUrl!!.queryParameter("date") == "2026-11-24") {
                json(
                    list(
                        // läuft noch (bis 26.11.)
                        event("laufend", "2026-11-22T10:00:00Z", end = "2026-11-26T10:00:00Z"),
                        // schon vorbei
                        event("vorbei", "2026-11-20T10:00:00Z", end = "2026-11-22T10:00:00Z"),
                        // beginnt heute: gehört schon zur Liste
                        event("heute", "2026-11-24T17:00:00Z"),
                    ),
                )
            } else {
                json(list(event("heute", "2026-11-24T17:00:00Z"), event("morgen", "2026-11-25T17:00:00Z")))
            }
        }

        val page = repository.list(EventFilter())

        assertEquals(listOf("laufend", "heute", "morgen"), page.events.map { it.id })
        val running = requests.last().requestUrl!!
        assertEquals("2026-11-24", running.queryParameter("date"))
        assertEquals("true", running.queryParameter("include_cancelled"))
        assertNull(running.queryParameter("from"))
    }

    @Test
    fun `schlaegt die Abfrage der laufenden fehl, bleibt die Liste`() = runTest {
        handler = { request ->
            if (request.requestUrl!!.queryParameter("date") != null) json("{}", 500) else json(list(event("a", "2026-11-25T17:00:00Z")))
        }

        assertEquals(listOf("a"), repository.list(EventFilter()).events.map { it.id })
    }

    @Test
    fun `ein einzelner Tag und Heute Abend fragen nicht nach laufenden und nicht nach Seite zwei`() = runTest {
        handler = { json(list(event("a", "2026-11-24T18:00:00Z"))) }

        repository.list(EventFilter(day = LocalDate.of(2026, 11, 24)))
        repository.list(EventFilter(eveningOnly = true))
        repository.list(EventFilter(), page = 2)

        // Je eine Abfrage: Tag, Heute Abend, Seite 2 der Liste ab heute (ohne laufende).
        assertEquals(3, requests.size)
        assertEquals("true", requests[1].requestUrl!!.queryParameter("evening"))
        assertEquals("2", requests[2].requestUrl!!.queryParameter("page"))
    }

    @Test
    fun `die Seite traegt Weitere und Gesamtzahl der API`() = runTest {
        handler = { json(list(event("a", "2026-11-25T17:00:00Z"), hasMore = true, total = 130)) }

        val page = repository.list(EventFilter(day = LocalDate.of(2026, 11, 25)))

        assertTrue(page.hasMore)
        assertEquals(130, page.total)
        assertEquals(1, page.page)
    }

    @Test
    fun `fehlende Felder sind kein Fehler und Texte werden aufgeraeumt`() = runTest {
        handler = {
            json(
                list(
                    event(
                        "voll", "2026-11-25T17:00:00Z", end = "kaputt",
                        extra = """,
                            "category":"FRIST","status":"cancelled","all_day":true,
                            "location":"  Club  ","location_address":"  ",
                            "organizer":{"id":"cafe-bohne","name":" Café Bohne "},
                            "description":"Zeile 1\n\nZeile 2",
                            "image_url":"ftp://x/y.png","source_url":"https://example.org/a","ticket_url":"kein link"
                        """.trimIndent(),
                    ),
                ),
            )
        }

        val e = repository.list(EventFilter(day = LocalDate.of(2026, 11, 25))).events.single()

        assertNull(e.end)
        assertEquals("Frist", e.category)
        assertTrue(e.isDeadline)
        assertEquals(EventStatus.Cancelled, e.status)
        assertTrue(e.allDay)
        assertEquals("Club", e.location)
        assertNull(e.address)
        assertEquals("Café Bohne", e.organizer)
        assertEquals("cafe-bohne", e.organizerId)
        assertEquals("Zeile 1\n\nZeile 2", e.description)
        assertNull(e.imageUrl)
        assertEquals("https://example.org/a", e.infoUrl)
        assertNull(e.ticketUrl)
    }

    @Test
    fun `ein nicht lesbarer Beginn ist ein Dekodierfehler`() = runTest {
        handler = { json(list(event("kaputt", "morgen frueh"))) }

        try {
            repository.list(EventFilter(day = LocalDate.of(2026, 11, 25)))
            fail("Erwartet: ApiError.Decoding")
        } catch (e: ApiError.Decoding) {
            // erwartet
        }
    }

    @Test
    fun `das Detail kodiert die ID im Pfad, ein 404 ist nicht gefunden`() = runTest {
        handler = { request ->
            if (request.path!!.endsWith("/a%2Fb%20c")) json(event("a/b c", "2026-11-25T17:00:00Z")) else json("""{"error":{"code":"not_found"}}""", 404)
        }

        assertEquals("a/b c", repository.detail("a/b c").id)
        try {
            repository.detail("gibt-es-nicht")
            fail("Erwartet: 404")
        } catch (e: ApiError.Http) {
            assertTrue(e.isNotFound)
        }
    }

    @Test
    fun `Veranstalter ohne kommende Termine entfallen, der Rest ist alphabetisch`() = runTest {
        handler = {
            json(
                """{"data":[
                    {"id":"z","name":"Zebra","upcoming_event_count":2},
                    {"id":"a","name":"Ärzte","upcoming_event_count":1},
                    {"id":"b","name":"Bohne","upcoming_event_count":0},
                    {"name":"Club","upcoming_event_count":5}
                ]}""",
            )
        }

        val organizers = repository.organizers()

        assertEquals(listOf("Ärzte", "Club", "Zebra"), organizers.map { it.name })
        // Ohne ID dient der Name als ID.
        assertEquals("Club", organizers[1].id)
    }
}
