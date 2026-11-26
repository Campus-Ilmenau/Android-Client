package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.core.common.IoDispatcher
import de.kaigothe.campusilmenau.core.network.ApiCaller
import de.kaigothe.campusilmenau.data.remote.library.LibraryApi
import de.kaigothe.campusilmenau.data.remote.library.toDomain
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailability
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaDetail
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchQuery
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Katalog der UB Ilmenau. Fehler kommen als `ApiError` (503 und Netzfehler sind wiederholbar). */
interface LibraryCatalogRepository {
    /** Sendet die Anfrage. Die Eingabe prüft der Aufrufer vorher mit `LibrarySearchQuery.validate()`. */
    suspend fun search(query: LibrarySearchQuery): LibrarySearchResponse

    /**
     * Verfügbarkeit gebündelt: IDs ohne Dopplungen, in Paketen zu höchstens 20, parallel. Unbekannte IDs kommen mit
     * Status `Unknown`. Wird nie gespeichert.
     */
    suspend fun availability(ids: Collection<String>): Map<String, LibraryAvailability>

    /** [searchId] aus der Suche; [track] `false` beim Neuladen einer offenen Seite und bei der Merkliste. */
    suspend fun media(id: String, searchId: String? = null, track: Boolean? = null): LibraryMediaDetail

    /** Ob Inhaltsverzeichnis, Klappentext und Inhaltssuche angeboten werden. Wird nicht gespeichert. */
    suspend fun contentsAvailable(): Boolean

    companion object {
        const val AVAILABILITY_BATCH = 20
    }
}

@Singleton
class NetworkLibraryCatalogRepository @Inject constructor(
    private val api: LibraryApi,
    private val caller: ApiCaller,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LibraryCatalogRepository {

    override suspend fun search(query: LibrarySearchQuery): LibrarySearchResponse = withContext(io) {
        caller.call {
            val response = api.search(query.params()).checked()
            response.dto.toDomain(isStale = response.isStale)
        }
    }

    override suspend fun availability(ids: Collection<String>): Map<String, LibraryAvailability> = withContext(io) {
        val unique = ids.distinct()
        if (unique.isEmpty()) return@withContext emptyMap()
        coroutineScope {
            unique.chunked(LibraryCatalogRepository.AVAILABILITY_BATCH)
                .map { batch -> async { caller.call { api.availability(batch.joinToString(",")).data.map { it.toDomain() } } } }
                .awaitAll()
                .flatten()
                .associateBy { it.id }
        }
    }

    override suspend fun media(id: String, searchId: String?, track: Boolean?): LibraryMediaDetail = withContext(io) {
        caller.call { api.media(id, searchId, track).checked().dto.toDomain() }
    }

    override suspend fun contentsAvailable(): Boolean = withContext(io) {
        caller.call { api.features().contents }
    }

    private class Body<T>(val dto: T, val isStale: Boolean)

    /** Erfolg liefert den Körper samt Cache-Hinweis, sonst die `HttpException`, die `ApiCaller` übersetzt. */
    private fun <T> Response<T>.checked(): Body<T> {
        if (!isSuccessful) throw HttpException(this)
        val dto = body() ?: throw HttpException(this)
        return Body(dto, headers()["X-Cache-Status"].equals("stale", ignoreCase = true))
    }
}
