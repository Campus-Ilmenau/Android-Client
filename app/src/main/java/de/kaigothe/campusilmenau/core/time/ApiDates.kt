package de.kaigothe.campusilmenau.core.time

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Datumsformate der Schnittstellen. Alle drei kommen vor:
 * - ISO 8601 mit Millisekunden (`2026-10-29T00:06:54.519Z`)
 * - ISO 8601 ohne Millisekunden (`2026-10-29T00:06:54Z`, auch mit Offset wie `+01:00`)
 * - reines Datum (`2026-11-05`), gelesen als 12:00 UTC, damit der Tag in jeder Zeitzone derselbe bleibt
 */
object ApiDates {
    private const val DATE_ONLY_LENGTH = 10

    /** Der Zeitpunkt, oder `null`, wenn der Text kein bekanntes Datum ist. */
    fun parse(text: String): Instant? {
        val value = text.trim()
        if (value.isEmpty()) return null
        return try {
            if (value.length == DATE_ONLY_LENGTH) {
                LocalDate.parse(value).atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC)
            } else {
                OffsetDateTime.parse(value).toInstant()
            }
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

/** Liest die Datumsformate der API (siehe [ApiDates]). Ein ungültiger Wert ist ein Dekodierfehler. */
object ApiInstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ApiInstant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        val text = decoder.decodeString()
        return ApiDates.parse(text) ?: throw SerializationException("Ungültiges Datum: $text")
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }
}
