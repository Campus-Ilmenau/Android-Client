package de.kaigothe.campusilmenau.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class ApiCallerTest {

    private val caller = ApiCaller(ApiJson.create())

    private fun httpException(status: Int, body: String, vararg headers: Pair<String, String>): HttpException {
        val errorBody = body.toResponseBody("application/json".toMediaType())
        val raw = okhttp3.Response.Builder()
            .code(status)
            .message("Fehler")
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .request(okhttp3.Request.Builder().url("https://example.test/").build())
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        return HttpException(Response.error<Any>(errorBody, raw))
    }

    private suspend inline fun <reified E : Throwable> failure(noinline block: suspend () -> Unit): E {
        try {
            block()
        } catch (e: Throwable) {
            if (e is E) return e
            throw e
        }
        fail("Erwartet: ${E::class.simpleName}")
        error("unreachable")
    }

    @Test
    fun `Erfolg gibt den Wert zurueck`() = runTest {
        assertEquals(42, caller.call { 42 })
    }

    @Test
    fun `Fehlerhuelle der API wird gelesen`() = runTest {
        val body = """{"error":{"type":"invalid_request","code":"invalid_parameter_format","message":"Ungueltig","param":"organizer","request_id":"req_1"}}"""
        val error = failure<ApiError.Http> { caller.call { throw httpException(400, body) } }
        assertEquals(400, error.status)
        assertEquals("invalid_parameter_format", error.code)
        assertEquals("Ungueltig", error.serverMessage)
        assertEquals("organizer", error.param)
        assertEquals("req_1", error.requestId)
    }

    @Test
    fun `404 ohne Huelle ist nicht gefunden`() = runTest {
        val error = failure<ApiError.Http> { caller.call { throw httpException(404, "") } }
        assertTrue(error.isNotFound)
        assertNull(error.code)
    }

    @Test
    fun `404 mit Cover-Code behaelt den Code`() = runTest {
        val body = """{"error":{"code":"cover_pending","message":"noch nicht da"}}"""
        val error = failure<ApiError.Http> {
            caller.call { throw httpException(404, body, "Retry-After" to "30") }
        }
        assertEquals("cover_pending", error.code)
        assertEquals(30.seconds, error.retryAfter)
    }

    @Test
    fun `503 ist wiederholbar, auch ohne lesbaren Body`() = runTest {
        val error = failure<ApiError.Http> { caller.call { throw httpException(503, "<html>") } }
        assertTrue(error.isUnavailable)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `Request-Id kommt auch aus der Kopfzeile`() = runTest {
        val error = failure<ApiError.Http> {
            caller.call { throw httpException(500, "", "X-Request-Id" to "abc") }
        }
        assertEquals("abc", error.requestId)
    }

    @Test
    fun `Netzfehler werden zu Network und sind wiederholbar`() = runTest {
        val error = failure<ApiError.Network> { caller.call { throw IOException("offline") } }
        assertTrue(error.isRetryable)
    }

    @Test
    fun `Dekodierfehler werden zu Decoding und sind nicht wiederholbar`() = runTest {
        val error = failure<ApiError.Decoding> { caller.call { throw SerializationException("kaputt") } }
        assertFalse(error.isRetryable)
    }

    @Test
    fun `Abbruch der Coroutine wird nicht verschluckt`() = runTest {
        failure<CancellationException> { caller.call { throw CancellationException("abgebrochen") } }
    }
}
