package de.kaigothe.campusilmenau.core.time

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class ApiDatesTest {

    @Test
    fun `liest ISO 8601 mit Millisekunden`() {
        assertEquals(Instant.parse("2026-10-29T00:06:54.519Z"), ApiDates.parse("2026-10-29T00:06:54.519Z"))
    }

    @Test
    fun `liest ISO 8601 ohne Millisekunden`() {
        assertEquals(Instant.parse("2026-10-29T00:06:54Z"), ApiDates.parse("2026-10-29T00:06:54Z"))
    }

    @Test
    fun `liest ISO 8601 mit Offset`() {
        assertEquals(Instant.parse("2026-11-05T17:00:00Z"), ApiDates.parse("2026-11-05T18:00:00+01:00"))
    }

    @Test
    fun `liest reines Datum als zwoelf Uhr UTC`() {
        assertEquals(Instant.parse("2026-11-05T12:00:00Z"), ApiDates.parse("2026-11-05"))
    }

    @Test
    fun `ignoriert Leerraum am Rand`() {
        assertEquals(Instant.parse("2026-11-05T12:00:00Z"), ApiDates.parse("  2026-11-05 "))
    }

    @Test
    fun `ungueltige Texte ergeben null`() {
        assertNull(ApiDates.parse(""))
        assertNull(ApiDates.parse("   "))
        assertNull(ApiDates.parse("morgen"))
        assertNull(ApiDates.parse("2026-13-45"))
        assertNull(ApiDates.parse("2026-11-05T25:00:00Z"))
    }

    @Serializable
    private data class Holder(
        @Serializable(with = ApiInstantSerializer::class) val at: Instant,
    )

    @Test
    fun `Serializer liest alle Formate`() {
        val holder = Json.decodeFromString<Holder>("""{"at":"2026-11-05"}""")
        assertEquals(Instant.parse("2026-11-05T12:00:00Z"), holder.at)
    }

    @Test
    fun `Serializer meldet ungueltiges Datum als Dekodierfehler`() {
        assertThrows(SerializationException::class.java) {
            Json.decodeFromString<Holder>("""{"at":"kein Datum"}""")
        }
    }
}
