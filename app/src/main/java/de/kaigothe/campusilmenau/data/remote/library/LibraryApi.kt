package de.kaigothe.campusilmenau.data.remote.library

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/** Katalog-Teil der Campus-Ilmenau-API. Ohne Anmeldung, nur lesend. */
interface LibraryApi {

    /** [params] kommen aus `LibrarySearchQuery.params()`. Die Antwort bleibt eine `Response`, weil `X-Cache-Status` zählt. */
    @GET("v1/library/search")
    suspend fun search(@QueryMap params: Map<String, String>): Response<LibrarySearchDto>

    /** Höchstens 20 IDs, durch Komma getrennt. */
    @GET("v1/library/availability")
    suspend fun availability(@Query("ids") ids: String): AvailabilityListDto

    /** [searchId] aus der Suche, wenn man von dort kam; [track] `false` beim Neuladen einer offenen Seite und bei der Merkliste. */
    @GET("v1/library/media/{id}")
    suspend fun media(
        @Path("id") id: String,
        @Query("search_id") searchId: String? = null,
        @Query("track") track: Boolean? = null,
    ): Response<MediaDetailDto>

    @GET("v1/library/features")
    suspend fun features(): FeaturesDto
}
