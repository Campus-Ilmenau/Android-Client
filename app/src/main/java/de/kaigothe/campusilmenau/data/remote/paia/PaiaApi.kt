package de.kaigothe.campusilmenau.data.remote.paia

import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * PAIA-Server der Universitätsbibliothek Ilmenau (https://gbv.github.io/paia/paia.html). Die Basis-URL
 * `https://paia.gbv.de/DE-Ilm1/` ist fest und gehört nicht zur Campus-Ilmenau-API. Der `Authorization`-Header kommt mit
 * jedem Aufruf, das Token wird nirgends gespeichert.
 */
interface PaiaApi {

    /** Resource-Owner-Password-Grant. Ein Fehler kommt mit Status 4xx und `{ error, error_description }`. */
    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("grant_type") grantType: String,
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("scope") scope: String,
    ): AuthResponseDto

    @GET("core/{patron}/items")
    suspend fun items(@Path("patron") patron: String, @Header("Authorization") authorization: String): ItemsResponseDto

    /** Optional laut Spezifikation: Ein Server ohne diesen Endpunkt antwortet mit 404. */
    @GET("core/{patron}/notifications")
    suspend fun notifications(@Path("patron") patron: String, @Header("Authorization") authorization: String): NotificationsResponseDto

    @GET("core/{patron}")
    suspend fun patron(@Path("patron") patron: String, @Header("Authorization") authorization: String): PatronDto

    @POST("core/{patron}/renew")
    suspend fun renew(
        @Path("patron") patron: String,
        @Header("Authorization") authorization: String,
        @Body request: RenewRequestDto,
    ): RenewResponseDto

    companion object {
        const val BASE_URL = "https://paia.gbv.de/DE-Ilm1/"
        const val SCOPE = "read_patron read_fees read_items write_items read_notifications"
    }
}
