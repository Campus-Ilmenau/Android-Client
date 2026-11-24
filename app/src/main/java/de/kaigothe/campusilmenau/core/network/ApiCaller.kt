package de.kaigothe.campusilmenau.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Führt Retrofit-Aufrufe aus und übersetzt jeden Fehler in einen [ApiError]. Abbrüche der Coroutine
 * werden nicht verschluckt.
 */
@Singleton
class ApiCaller @Inject constructor(private val json: Json) {

    suspend fun <T> call(block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw toHttpError(e)
        } catch (e: SerializationException) {
            throw ApiError.Decoding(e)
        } catch (e: IOException) {
            throw ApiError.Network(e)
        }

    private fun toHttpError(e: HttpException): ApiError.Http {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        // Auch die Fehlerhülle kommt je nach Quelle in snake_case oder camelCase: vor dem Lesen vereinheitlichen.
        val detail = body?.takeIf { it.isNotBlank() }?.let {
            runCatching { json.decodeFromString<ApiErrorEnvelope>(CamelCaseKeys.normalize(it, json)).error }.getOrNull()
        }
        return ApiError.Http(
            status = e.code(),
            code = detail?.code,
            serverMessage = detail?.message,
            param = detail?.param,
            requestId = detail?.requestId ?: e.response()?.headers()?.get("X-Request-Id"),
            retryAfter = RetryAfter.parse(e.response()?.headers()?.get("Retry-After")),
        )
    }
}
