package de.kaigothe.campusilmenau.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RetryAfterTest {

    private val now = Instant.parse("2026-10-21T07:28:20Z")

    @Test
    fun `Sekundenzahl`() {
        assertEquals(30.seconds, RetryAfter.parse("30", now))
    }

    @Test
    fun `negative Sekunden werden zu null Sekunden`() {
        assertEquals(Duration.ZERO, RetryAfter.parse("-5", now))
    }

    @Test
    fun `HTTP-Datum in der Zukunft`() {
        assertEquals(10.seconds, RetryAfter.parse("Wed, 21 Oct 2026 07:28:30 GMT", now))
    }

    @Test
    fun `HTTP-Datum in der Vergangenheit`() {
        assertEquals(Duration.ZERO, RetryAfter.parse("Wed, 21 Oct 2026 07:00:00 GMT", now))
    }

    @Test
    fun `fehlender oder unlesbarer Header`() {
        assertNull(RetryAfter.parse(null, now))
        assertNull(RetryAfter.parse("", now))
        assertNull(RetryAfter.parse("bald", now))
    }
}
