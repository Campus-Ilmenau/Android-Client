package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.core.common.IoDispatcher
import de.kaigothe.campusilmenau.core.network.ApiCaller
import de.kaigothe.campusilmenau.data.remote.mensa.MensaApi
import de.kaigothe.campusilmenau.data.remote.mensa.toDomain
import de.kaigothe.campusilmenau.domain.model.mensa.Canteen
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Speiseplan und Öffnungszeiten. Fehler kommen als `ApiError`, ein abgebrochener Aufruf als Abbruch der Coroutine. */
interface MensaRepository {

    /** Alle Gerichte aller Mensen für den Tag. [forceRefresh] umgeht den Zwischenspeicher („Erneut versuchen“). */
    suspend fun meals(day: LocalDate, forceRefresh: Boolean = false): List<MensaMeal>

    /** Alle Mensen mit Öffnungszeiten. */
    suspend fun canteens(forceRefresh: Boolean = false): List<Canteen>

    /**
     * Ein Gericht. Die IDs der API gelten nur für einen Tag und lassen sich nicht einzeln abfragen: Gesucht wird im
     * Speiseplan des Tages. `null`, wenn es dort nicht (mehr) steht.
     */
    suspend fun meal(id: String, day: LocalDate): MensaMeal?
}

/**
 * Lädt von der API und hält die Antworten kurz im Speicher, damit Tagwechsel und das Öffnen eines Gerichts nicht
 * jedes Mal ins Netz gehen. Der Speiseplan ändert sich im Lauf des Tages selten, fünf Minuten genügen.
 */
@Singleton
class NetworkMensaRepository @Inject constructor(
    private val api: MensaApi,
    private val caller: ApiCaller,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : MensaRepository {

    private class Cached<T>(val value: T, val loadedAt: Instant)

    private val mutex = Mutex()
    private val mealCache = HashMap<LocalDate, Cached<List<MensaMeal>>>()
    private var canteenCache: Cached<List<Canteen>>? = null

    override suspend fun meals(day: LocalDate, forceRefresh: Boolean): List<MensaMeal> {
        if (!forceRefresh) {
            mutex.withLock { mealCache[day]?.takeIf(::isFresh)?.value }?.let { return it }
        }
        val loaded = withContext(io) {
            caller.call { api.meals(date = day.toString()) }.data.map { it.toDomain() }
        }
        mutex.withLock { mealCache[day] = Cached(loaded, clock.instant()) }
        return loaded
    }

    override suspend fun canteens(forceRefresh: Boolean): List<Canteen> {
        if (!forceRefresh) {
            mutex.withLock { canteenCache?.takeIf(::isFresh)?.value }?.let { return it }
        }
        val loaded = withContext(io) {
            caller.call { api.canteens() }.data.map { it.toDomain() }
        }
        mutex.withLock { canteenCache = Cached(loaded, clock.instant()) }
        return loaded
    }

    override suspend fun meal(id: String, day: LocalDate): MensaMeal? = meals(day).firstOrNull { it.id == id }

    private fun isFresh(cached: Cached<*>): Boolean =
        Duration.between(cached.loadedAt, clock.instant()) < CACHE_TTL

    private companion object {
        val CACHE_TTL: Duration = Duration.ofMinutes(5)
    }
}
