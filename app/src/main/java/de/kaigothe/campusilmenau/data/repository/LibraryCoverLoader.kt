package de.kaigothe.campusilmenau.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryCoverResult
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Holt Cover und merkt sich die Bilder für die Dauer der Sitzung. Bei „wird noch geholt“ wartet er die genannte Zeit
 * (höchstens 30 s, ohne Angabe 30 s) und versucht es bis zu zweimal erneut.
 */
@Singleton
class LibraryCoverLoader @Inject constructor(private val repository: LibraryCatalogRepository) {
    // Erst beim ersten Cover anlegen: `LruCache` gibt es nur auf dem Gerät, nicht in reinen JVM-Tests.
    private val cache by lazy {
        object : LruCache<String, Bitmap>(CACHE_BYTES) {
            override fun sizeOf(key: String, value: Bitmap) = value.byteCount
        }
    }

    /** `null`: kein Cover oder nicht ladbar. Abbruch der Coroutine bricht auch das Warten ab. */
    suspend fun image(media: LibraryMedia): Bitmap? {
        if (!media.cover.isFetchable) return null
        cache.get(media.id)?.let { return it }
        repeat(MAX_ATTEMPTS) {
            val result = try {
                repository.cover(media.cover)
            } catch (_: ApiError) {
                return null
            }
            when (result) {
                is LibraryCoverResult.Image -> {
                    val bitmap = decode(result.bytes) ?: return null
                    cache.put(media.id, bitmap)
                    return bitmap
                }
                LibraryCoverResult.Missing -> return null
                is LibraryCoverResult.Pending -> delay(waitTime(result.retryAfter))
            }
        }
        return null
    }

    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= TARGET_WIDTH) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    companion object {
        const val MAX_ATTEMPTS = 3
        val DEFAULT_RETRY = 30.seconds
        private const val CACHE_BYTES = 8 * 1024 * 1024
        private const val TARGET_WIDTH = 192

        /** Wartezeit nach `cover_pending`: laut `Retry-After`, höchstens 30 s. */
        fun waitTime(retryAfter: Duration?): Duration = minOf(retryAfter ?: DEFAULT_RETRY, DEFAULT_RETRY)
    }
}
