package de.kaigothe.campusilmenau.data.remote.mensa

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Mensa-Teil der Campus-Ilmenau-API. Ohne Anmeldung, nur lesend. Pfade relativ zur Basis-URL
 * (`BuildConfig.API_BASE_URL`, endet auf `/`).
 */
interface MensaApi {

    /** Speiseplan aller Mensen für einen Berliner Tag (`yyyy-MM-dd`). Die Seitengröße 100 ist das Maximum der API. */
    @GET("v1/mensa")
    suspend fun meals(
        @Query("date") date: String,
        @Query("per_page") perPage: Int = PAGE_SIZE,
    ): MensaListDto<MealDto>

    /** Alle Mensen und Cafeterien mit Öffnungszeiten. */
    @GET("v1/mensa/canteens")
    suspend fun canteens(): MensaListDto<CanteenDto>

    companion object {
        const val PAGE_SIZE = 100
    }
}
