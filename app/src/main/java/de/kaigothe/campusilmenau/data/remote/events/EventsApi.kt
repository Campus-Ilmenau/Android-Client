package de.kaigothe.campusilmenau.data.remote.events

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

/** Events-Teil der Campus-Ilmenau-API. Ohne Anmeldung, nur lesend. */
interface EventsApi {

    /**
     * Eine Seite der Liste, nach Beginn aufsteigend. [params] sind die Parameter aus `EventFilter.query` (Zeitfilter,
     * Veranstalter, Kategorie, `include_cancelled`), dazu `page` und `per_page`.
     */
    @GET("v1/events")
    suspend fun list(@QueryMap params: Map<String, String>): EventListDto

    @GET("v1/events/{id}")
    suspend fun detail(@Path("id") id: String): EventDto

    @GET("v1/events/organizers")
    suspend fun organizers(): OrganizerListDto

    companion object {
        const val PAGE_SIZE = 100
    }
}
