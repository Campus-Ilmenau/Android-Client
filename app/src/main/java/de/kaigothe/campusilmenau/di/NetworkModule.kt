package de.kaigothe.campusilmenau.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.kaigothe.campusilmenau.BuildConfig
import de.kaigothe.campusilmenau.core.network.ApiJson
import de.kaigothe.campusilmenau.core.network.CamelCaseKeysInterceptor
import de.kaigothe.campusilmenau.data.remote.paia.PaiaApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Basis-URL der Campus-Ilmenau-API. Endet immer auf `/`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiBaseUrl

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @ApiBaseUrl
    fun provideApiBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    fun provideJson(): Json = ApiJson.create()

    @Provides
    @Singleton
    fun provideOkHttpClient(json: Json): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("Accept", "application/json").build())
            }
            // Schlüssel der Antworten in camelCase vereinheitlichen (die API mischt snake_case und camelCase).
            .addInterceptor(CamelCaseKeysInterceptor(json))
        if (BuildConfig.DEBUG) {
            // Nur Kopfzeilen und Status, nie Inhalte. Zugangsdaten gehören nicht in Logs.
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactHeader("Authorization")
                },
            )
        }
        return builder.build()
    }

    /** Der PAIA-Server der UB (fremder Host, eigene Basis-URL), über denselben HTTP-Client. */
    @Provides
    @Singleton
    fun providePaiaApi(client: OkHttpClient, json: Json): PaiaApi = Retrofit.Builder()
        .baseUrl(PaiaApi.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(PaiaApi::class.java)

    @Provides
    @Singleton
    fun provideRetrofit(
        @ApiBaseUrl baseUrl: String,
        client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
