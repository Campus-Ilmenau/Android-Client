package de.kaigothe.campusilmenau.core.network

import java.time.Duration as JavaDuration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

/** `Retry-After` als Wartezeit. Der Header ist eine Sekundenzahl oder ein HTTP-Datum. */
object RetryAfter {
    fun parse(header: String?, now: Instant = Instant.now()): Duration? {
        val value = header?.trim().orEmpty()
        if (value.isEmpty()) return null
        value.toLongOrNull()?.let { return it.coerceAtLeast(0).seconds }
        return try {
            val target = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
            JavaDuration.between(now, target).takeIf { !it.isNegative }?.toKotlinDuration() ?: Duration.ZERO
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
