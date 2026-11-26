package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.core.network.ApiJson
import de.kaigothe.campusilmenau.core.network.CamelCaseKeysInterceptor
import de.kaigothe.campusilmenau.data.remote.paia.PaiaApi
import de.kaigothe.campusilmenau.data.security.CredentialStore
import de.kaigothe.campusilmenau.data.security.StoredCredentials
import de.kaigothe.campusilmenau.domain.model.library.PaiaError
import de.kaigothe.campusilmenau.domain.model.library.PaiaItemStatus
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Sitzung und Daten des PAIA-Kontos, vom MockWebServer über die Schlüssel-Umwandlung bis zum Zustand. */
class PaiaRepositoryTest {

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
    }

    private class FakeCredentials(var stored: StoredCredentials? = null) : CredentialStore {
        override fun save(credentials: StoredCredentials) {
            stored = credentials
        }

        override fun load(): StoredCredentials? = stored
        override fun clear() {
            stored = null
        }
    }

    private val clock = MutableClock(Instant.parse("2026-11-26T10:00:00Z"))
    private val server = MockWebServer()
    private val requests = mutableListOf<RecordedRequest>()
    private var handler: (RecordedRequest) -> MockResponse = { MockResponse().setResponseCode(500) }
    private val credentials = FakeCredentials()
    private lateinit var repository: PaiaRepository

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private val loginOk = """{"access_token":"tok","token_type":"Bearer","patron":"123456","scope":"read_items","expires_in":3600}"""

    private fun build(creds: FakeCredentials = credentials): PaiaRepository {
        val parser = ApiJson.create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(CamelCaseKeysInterceptor(parser)).build())
            .addConverterFactory(parser.asConverterFactory("application/json".toMediaType()))
            .build()
        return PaiaRepository(retrofit.create<PaiaApi>(), parser, creds, clock)
    }

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return handler(request)
            }
        }
        server.start()
        repository = build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private suspend fun signIn(remember: Boolean = false) {
        handler = { json(loginOk) }
        repository.login("12345678", "geheim", remember)
        requests.clear()
    }

    private val itemsBody = """{"doc":[
        {"item":"urn:x:1","edition":"http://uri.gbv.de/document/opac-de-ilm1:ppn:1009895648","about":"Android Programmierung","label":"ZI 4711",
         "endtime":"2026-12-03","renewals":1,"canrenew":true,"status":3},
        {"about":"Vorgemerktes Buch","status":1,"queue":2,"storage":"Theke"}
    ]}"""

    // MARK: Login

    @Test
    fun `der Login schickt den Password-Grant und merkt sich die Sitzung`() = runTest {
        handler = { json(loginOk) }

        repository.login("12345678", "geheim", remember = false)

        val request = requests.single()
        assertEquals("POST", request.method)
        assertEquals("/auth/login", request.path)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/x-www-form-urlencoded"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("grant_type=password"))
        assertTrue(body.contains("username=12345678"))
        assertTrue(body.contains("password=geheim"))
        assertTrue(body.contains("scope=read_patron+read_fees+read_items+write_items+read_notifications") ||
            body.contains("scope=read_patron%20read_fees%20read_items%20write_items%20read_notifications"))
        val state = repository.state.value
        assertTrue(state.isAuthenticated)
        assertEquals("12345678", state.lastUsername)
        assertFalse(state.sessionExpired)
        assertFalse(state.isLoading)
    }

    @Test
    fun `Passwort mit Sonderzeichen wird kodiert uebertragen`() = runTest {
        handler = { json(loginOk) }

        repository.login("12345678", "p&ss=wort+1%", remember = false)

        val body = requests.single().body.readUtf8()
        assertTrue(body.contains("password=p%26ss%3Dwort%2B1%25"))
    }

    @Test
    fun `Angemeldet bleiben speichert die Zugangsdaten, ohne den Schalter werden sie geloescht`() = runTest {
        credentials.stored = StoredCredentials("alt", "alt")
        handler = { json(loginOk) }

        repository.login("12345678", "geheim", remember = true)
        assertEquals(StoredCredentials("12345678", "geheim"), credentials.stored)
        assertTrue(repository.state.value.remembersCredentials)

        repository.login("12345678", "geheim", remember = false)
        assertNull(credentials.stored)
        assertFalse(repository.state.value.remembersCredentials)
    }

    @Test
    fun `ein abgelehnter Login nennt die Meldung des Servers`() = runTest {
        handler = { json("""{"error":"access_denied","error_description":"Benutzernummer oder Passwort falsch"}""", 401) }

        try {
            repository.login("12345678", "falsch", remember = true)
            fail("Erwartet: AuthenticationFailed")
        } catch (e: PaiaError.AuthenticationFailed) {
            assertEquals("Benutzernummer oder Passwort falsch", e.serverMessage)
        }

        val state = repository.state.value
        assertFalse(state.isAuthenticated)
        assertTrue(state.error is PaiaError.AuthenticationFailed)
        assertNull(credentials.stored)
    }

    @Test
    fun `ohne Beschreibung gilt der Fehlercode, ohne lesbaren Body der Status`() = runTest {
        handler = { json("""{"error":"invalid_grant"}""", 400) }
        try {
            repository.login("1", "x", false)
            fail()
        } catch (e: PaiaError.AuthenticationFailed) {
            assertEquals("invalid_grant", e.serverMessage)
        }

        handler = { MockResponse().setResponseCode(503).setBody("<html>down</html>") }
        try {
            repository.login("1", "x", false)
            fail()
        } catch (e: PaiaError.Http) {
            assertEquals(503, e.status)
        }
    }

    // MARK: Ausleihen

    @Test
    fun `die Ausleihen kommen mit Token und Patron im Pfad`() = runTest {
        signIn()
        handler = { json(itemsBody) }

        repository.fetchItems()

        val request = requests.single()
        assertEquals("/core/123456/items", request.path)
        assertEquals("Bearer tok", request.getHeader("Authorization"))
        val items = repository.state.value.items
        assertEquals(2, items.size)
        val loan = items[0]
        assertEquals("Android Programmierung", loan.displayTitle)
        assertEquals("ZI 4711", loan.signature)
        assertEquals(PaiaItemStatus.Held, loan.status)
        assertEquals(Instant.parse("2026-12-03T12:00:00Z"), loan.end)
        assertEquals("1009895648", loan.catalogPpn)
        assertEquals(PaiaItemStatus.Reserved, items[1].status)
        assertNull(repository.state.value.error)
    }

    @Test
    fun `ein Status ausserhalb der Liste gilt als Kein Status`() = runTest {
        signIn()
        handler = { json("""{"doc":[{"about":"X","status":9}]}""") }

        repository.fetchItems()

        assertEquals(PaiaItemStatus.None, repository.state.value.items.single().status)
    }

    @Test
    fun `ohne Anmeldung gibt es keine Anfrage`() = runTest {
        try {
            repository.fetchItems()
            fail("Erwartet: NotAuthenticated")
        } catch (_: PaiaError.NotAuthenticated) {
            // erwartet
        }
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `ein Serverfehler steht im Zustand`() = runTest {
        signIn()
        handler = { MockResponse().setResponseCode(500) }

        try {
            repository.fetchItems()
            fail()
        } catch (e: PaiaError.Http) {
            assertEquals(500, e.status)
        }

        assertTrue(repository.state.value.error is PaiaError.Http)
        assertTrue(repository.state.value.isAuthenticated)
        assertFalse(repository.state.value.isLoading)
    }

    // MARK: Ablauf der Sitzung

    @Test
    fun `401 beendet die Sitzung, behaelt die Benutzernummer und setzt den Ablauf`() = runTest {
        signIn()
        handler = { MockResponse().setResponseCode(401) }

        try {
            repository.fetchItems()
            fail("Erwartet: SessionExpired")
        } catch (_: PaiaError.SessionExpired) {
            // erwartet
        }

        val state = repository.state.value
        assertFalse(state.isAuthenticated)
        assertTrue(state.sessionExpired)
        assertEquals("12345678", state.lastUsername)
        assertTrue(state.items.isEmpty())
        // Der Ablauf ist keine normale Fehlermeldung.
        assertNull(state.error)
    }

    @Test
    fun `403 gilt wie 401 und ein neuer Login setzt den Ablauf zurueck`() = runTest {
        signIn()
        handler = { MockResponse().setResponseCode(403) }
        try {
            repository.fetchNotifications()
            fail()
        } catch (_: PaiaError.SessionExpired) {
            // erwartet
        }
        assertTrue(repository.state.value.sessionExpired)

        handler = { json(loginOk) }
        repository.login("12345678", "geheim", false)

        assertFalse(repository.state.value.sessionExpired)
        assertTrue(repository.state.value.isAuthenticated)
    }

    @Test
    fun `ein lokal abgelaufenes Token fragt gar nicht erst nach`() = runTest {
        signIn()
        clock.now = clock.now.plusSeconds(3601)

        try {
            repository.fetchItems()
            fail("Erwartet: SessionExpired")
        } catch (_: PaiaError.SessionExpired) {
            // erwartet
        }

        assertTrue(requests.isEmpty())
        assertTrue(repository.state.value.sessionExpired)
    }

    // MARK: Mitteilungen

    @Test
    fun `Mitteilungen werden gelesen`() = runTest {
        signIn()
        handler = {
            json("""{"notification":[
                {"id":"n1","about":"Fristablauf","date":"2026-11-25T08:00:00+01:00","item":"urn:x:1"},
                {"id":"n2","about":"Kaputt","date":"kein datum"}
            ]}""")
        }

        repository.fetchNotifications()

        assertEquals("/core/123456/notifications", requests.single().path)
        val list = repository.state.value.notifications
        // Eine Mitteilung ohne lesbares Datum lässt sich nicht einordnen.
        assertEquals(listOf("n1"), list.map { it.id })
        assertEquals(Instant.parse("2026-11-25T07:00:00Z"), list.single().date)
    }

    @Test
    fun `404 bei den Mitteilungen ist eine leere Liste und kein Fehler`() = runTest {
        signIn()
        handler = { MockResponse().setResponseCode(404) }

        repository.fetchNotifications()

        assertTrue(repository.state.value.notifications.isEmpty())
        assertNull(repository.state.value.error)
        assertTrue(repository.state.value.isAuthenticated)
    }

    // MARK: Konto

    @Test
    fun `das Konto liefert nur die Felder fuer den Ausweis`() = runTest {
        signIn()
        handler = {
            json(
                """{"name":"Erika Muster","expires":"2027-03-31","status":0,"type":["urn:mace:x:student"],
                    "email":"erika@example.org","address":"Musterweg 1"}""",
            )
        }

        val patron = repository.fetchPatron()

        assertEquals("/core/123456", requests.single().path)
        assertEquals("Erika Muster", patron.name)
        assertEquals(Instant.parse("2027-03-31T12:00:00Z"), patron.expires)
        assertEquals(listOf("urn:mace:x:student"), patron.types)
        // Die Tabelle der Nutzergruppen ist noch leer: kein Chip.
        assertNull(patron.userGroup)
        assertEquals(patron, repository.state.value.patron)
    }

    // MARK: Verlängern

    @Test
    fun `Verlaengern aktualisiert Faelligkeit und Zaehler`() = runTest {
        signIn()
        handler = { json(itemsBody) }
        repository.fetchItems()
        requests.clear()
        handler = { json("""{"doc":[{"item":"urn:x:1","endtime":"2026-12-24T00:00:00Z","status":3}]}""") }

        repository.renew(repository.state.value.items[0])

        val request = requests.single()
        assertEquals("POST", request.method)
        assertEquals("/core/123456/renew", request.path)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals("""{"doc":[{"item":"urn:x:1"}]}""", request.body.readUtf8())
        val renewed = repository.state.value.items[0]
        assertEquals(2, renewed.renewals)
        assertEquals(Instant.parse("2026-12-24T00:00:00Z"), renewed.end)
        // Andere Einträge bleiben unberührt.
        assertEquals(PaiaItemStatus.Reserved, repository.state.value.items[1].status)
    }

    @Test
    fun `eine abgelehnte Verlaengerung nennt den Grund und aendert nichts`() = runTest {
        signIn()
        handler = { json(itemsBody) }
        repository.fetchItems()
        handler = { json("""{"doc":[{"item":"urn:x:1","error":"Vorgemerkt von anderer Person"}]}""") }

        try {
            repository.renew(repository.state.value.items[0])
            fail("Erwartet: RenewalFailed")
        } catch (e: PaiaError.RenewalFailed) {
            assertEquals("Vorgemerkt von anderer Person", e.serverMessage)
        }

        assertEquals(1, repository.state.value.items[0].renewals)
        assertTrue(repository.state.value.error is PaiaError.RenewalFailed)
    }

    @Test
    fun `ohne URI des Exemplars wird nichts gesendet`() = runTest {
        signIn()
        handler = { json(itemsBody) }
        repository.fetchItems()
        requests.clear()

        try {
            repository.renew(repository.state.value.items[1])
            fail("Erwartet: MissingItemIdentifier")
        } catch (_: PaiaError.MissingItemIdentifier) {
            // erwartet
        }
        assertTrue(requests.isEmpty())
    }

    // MARK: Abmelden und Auto-Login

    @Test
    fun `Abmelden loescht alles, auch Benutzernummer und gespeicherte Zugangsdaten`() = runTest {
        signIn(remember = true)
        handler = { json(itemsBody) }
        repository.fetchItems()

        repository.logout()

        val state = repository.state.value
        assertFalse(state.isAuthenticated)
        assertTrue(state.items.isEmpty())
        assertNull(state.lastUsername)
        assertFalse(state.sessionExpired)
        assertFalse(state.remembersCredentials)
        assertNull(credentials.stored)
        try {
            repository.fetchItems()
            fail()
        } catch (_: PaiaError.NotAuthenticated) {
            // erwartet
        }
    }

    @Test
    fun `Auto-Login meldet mit den gespeicherten Daten an`() = runTest {
        credentials.stored = StoredCredentials("12345678", "geheim")
        repository = build()
        assertTrue(repository.state.value.remembersCredentials)
        handler = { json(loginOk) }

        assertTrue(repository.autoLogin())

        assertTrue(repository.state.value.isAuthenticated)
        assertTrue(requests.single().body.readUtf8().contains("username=12345678"))
        // Ein zweiter Aufruf tut nichts.
        requests.clear()
        assertTrue(repository.autoLogin())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `Auto-Login ohne gespeicherte Daten tut nichts`() = runTest {
        assertFalse(repository.autoLogin())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `lehnt der Server die gespeicherten Daten ab, werden sie geloescht`() = runTest {
        credentials.stored = StoredCredentials("12345678", "altes-passwort")
        repository = build()
        handler = { json("""{"error":"access_denied","error_description":"falsch"}""", 401) }

        assertFalse(repository.autoLogin())

        assertNull(credentials.stored)
        assertFalse(repository.state.value.remembersCredentials)
    }

    @Test
    fun `bei einem Netzfehler bleiben die gespeicherten Daten fuer den naechsten Versuch`() = runTest {
        credentials.stored = StoredCredentials("12345678", "geheim")
        repository = build()
        server.shutdown()

        assertFalse(repository.autoLogin())

        assertEquals(StoredCredentials("12345678", "geheim"), credentials.stored)
    }

    @Test
    fun `nach einem Ablauf meldet Auto-Login wieder an und behaelt die Zugangsdaten`() = runTest {
        signIn(remember = true)
        handler = { MockResponse().setResponseCode(401) }
        try {
            repository.fetchItems()
            fail()
        } catch (_: PaiaError.SessionExpired) {
            // erwartet
        }
        assertTrue(repository.state.value.remembersCredentials)

        handler = { json(loginOk) }
        assertTrue(repository.autoLogin())

        assertTrue(repository.state.value.isAuthenticated)
        assertFalse(repository.state.value.sessionExpired)
    }
}
