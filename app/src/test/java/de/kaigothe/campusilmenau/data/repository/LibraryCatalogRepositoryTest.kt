package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.core.network.ApiCaller
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.network.ApiJson
import de.kaigothe.campusilmenau.core.network.CamelCaseKeysInterceptor
import de.kaigothe.campusilmenau.data.remote.library.LibraryApi
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryCoverStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaType
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchQuery
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchScope
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchSource
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** Vom MockWebServer über die Schlüssel-Umwandlung bis zum Repository, mit Antworten im Format der Katalog-API. */
class LibraryCatalogRepositoryTest {
    private val server = MockWebServer()
    private val requests = mutableListOf<RecordedRequest>()
    private var handler: (RecordedRequest) -> MockResponse = { MockResponse().setResponseCode(500) }
    private lateinit var repository: NetworkLibraryCatalogRepository

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                synchronized(requests) { requests += request }
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
        repository = NetworkLibraryCatalogRepository(retrofit.create<LibraryApi>(), ApiCaller(parser), Dispatchers.IO)
    }

    @After
    fun tearDown() = server.shutdown()

    private val searchBody = """
        {"object":"list","has_more":true,"total_count":33,"page":1,"per_page":20,"source":"mixed","search_id":"s1",
         "catalog_suggested":true,
         "data":[{"id":"med_1","ppn":"1","mediaType":"print_book","title":" Java ","authors":["Kofler, Michael"," "],
                  "onlineUrl":"not a url","catalogUrl":"https://opac.example/1","fetchedAt":"2026-11-04T16:11:35.244Z",
                  "cover":{"status":"found","url":"/v1/library/media/med_1/cover"},
                  "match":{"fields":["title","toc","weird"],"stage":"exact"},
                  "links":[{"kind":"blurb","url":"https://x.example/b","mimeType":"text/html"}]},
                 {"id":"med_2","mediaType":"hologram","title":"Zwei"}]}
    """.trimIndent()

    @Test
    fun `die Suche sendet die Parameter und liest die Antwort tolerant`() = runTest {
        handler = { json(searchBody) }
        val result = repository.search(LibrarySearchQuery(text = "java", scope = LibrarySearchScope.Local, page = 2))

        val url = requests.single().requestUrl!!
        assertEquals("/v1/library/search", url.encodedPath)
        assertEquals("java", url.queryParameter("q"))
        assertEquals("local", url.queryParameter("in"))
        assertEquals("2", url.queryParameter("page"))
        assertEquals("20", url.queryParameter("per_page"))

        assertEquals(LibrarySearchSource.Mixed, result.source)
        assertTrue(result.hasMore)
        assertTrue(result.catalogSuggested)
        assertEquals("s1", result.searchId)
        assertFalse(result.isStale)
        val first = result.items[0]
        assertEquals("Java", first.title)
        assertEquals(listOf("Kofler, Michael"), first.authors)
        assertNull(first.onlineUrl)
        assertEquals("https://opac.example/1", first.catalogUrl)
        assertEquals(LibraryCoverStatus.Found, first.cover.status)
        assertTrue(first.cover.isFetchable)
        assertTrue(first.match!!.isInTableOfContents)
        assertEquals(LibraryMediaType.Other, result.items[1].mediaType)
        assertEquals(LibraryCoverStatus.Absent, result.items[1].cover.status)
    }

    @Test
    fun `X-Cache-Status stale markiert die Antwort`() = runTest {
        handler = { json(searchBody).setHeader("X-Cache-Status", "stale") }
        assertTrue(repository.search(LibrarySearchQuery(text = "java")).isStale)
    }

    @Test
    fun `503 ohne Koerper ist wiederholbar`() = runTest {
        handler = { MockResponse().setResponseCode(503) }
        try {
            repository.search(LibrarySearchQuery(text = "java"))
            fail()
        } catch (e: ApiError) {
            assertTrue(e.isRetryable)
        }
    }

    @Test
    fun `die Verfuegbarkeit wird dedupliziert und in Paketen zu 20 geholt`() = runTest {
        handler = { request ->
            val ids = request.requestUrl!!.queryParameter("ids")!!.split(",")
            val data = ids.joinToString(",") { """{"id":"$it","availabilityStatus":"available","items":[]}""" }
            json("""{"data":[$data]}""")
        }
        val ids = (1..45).map { "med_$it" } + "med_1" + "med_2"
        val result = repository.availability(ids)

        assertEquals(45, result.size)
        assertEquals(3, requests.size)
        val sizes = requests.map { it.requestUrl!!.queryParameter("ids")!!.split(",").size }.sorted()
        assertEquals(listOf(5, 20, 20), sizes)
        assertEquals(LibraryAvailabilityStatus.Available, result.getValue("med_7").status)
    }

    @Test
    fun `ohne IDs wird nichts gefragt`() = runTest {
        assertTrue(repository.availability(emptyList()).isEmpty())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `Exemplare lesen Status Rueckgabedatum und Standort`() = runTest {
        handler = {
            json(
                """{"data":[{"id":"med_1","availabilityStatus":"on_loan","items":[
                    {"label":"INF A 1","location":{"id":"inf","code":"52","name":"52/53 INF","floorPlanUrl":"https://x.example/p.png"},
                     "status":"on_loan","dueDate":"2026-12-01T00:00:00Z","reserveUrl":"https://x.example/r","queue":2},
                    {"label":"INF A 2","status":"on_loan","dueDate":"2026-11-30","queue":1}]}]}""",
            )
        }
        val availability = repository.availability(listOf("med_1")).getValue("med_1")
        assertEquals(3, availability.totalQueue)
        assertEquals(0, availability.availableCount)
        assertEquals("52/53 INF", availability.items[0].location?.name)
        assertEquals("https://x.example/p.png", availability.items[0].location?.floorPlanUrl)
        // Der reine Tag 2026-11-30 (Berliner Mitternacht) ist früher als der 1. Dezember.
        assertEquals(availability.items[1].dueDate, availability.earliestDueDate)
    }

    @Test
    fun `das Detail sendet search_id und track und liest Inhalte`() = runTest {
        handler = {
            json(
                """{"id":"med_1","title":"Java","items":[],"availability_status":"unknown","contents_status":"available",
                    "contents":{"table_of_contents":{"entries":[{"title":"Kapitel 1","level":1}]},"blurb":{"text":"Ein Buch."}}}""",
            )
        }
        val detail = repository.media("med_1", searchId = "s1", track = false)
        val url = requests.single().requestUrl!!
        assertEquals("/v1/library/media/med_1", url.encodedPath)
        assertEquals("s1", url.queryParameter("search_id"))
        assertEquals("false", url.queryParameter("track"))
        assertTrue(detail.hasContents)
        assertEquals("Ein Buch.", detail.contents?.blurb)
        assertEquals("Kapitel 1", detail.contents?.tableOfContents?.entries?.single()?.title)
    }

    @Test
    fun `ein fehlender Titel ist ein 404`() = runTest {
        handler = { json("""{"error":{"code":"not_found","message":"weg"}}""", code = 404) }
        try {
            repository.media("med_x")
            fail()
        } catch (e: ApiError.Http) {
            assertTrue(e.isNotFound)
        }
    }

    @Test
    fun `die Features sagen, ob Inhalte angeboten werden`() = runTest {
        handler = { json("""{"object":"library_features","contents":true}""") }
        assertTrue(repository.contentsAvailable())
    }
}
