package de.kaigothe.campusilmenau.domain.model.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BenutzernummerFormatTest {
    private fun n(raw: String, codabar: Boolean = false) = BenutzernummerFormat.normalize(raw, codabar)

    @Test fun acceptsLengthsFromEightToFourteen() {
        assertEquals("0001234567", n("0001234567"))
        assertEquals("12345678", n("12345678"))
        assertEquals("12345678901234", n("12345678901234"))
        assertNull(n("1234567"))
        assertNull(n("123456789012345"))
        assertNull(n(""))
    }

    @Test fun removesWhitespace() {
        assertEquals("0001234567", n(" 0001 234567 "))
        assertEquals("0001234567", n("0001\n234567"))
    }

    @Test fun codabarStripsStartAndStopMarks() {
        assertEquals("0001234567", n("A0001234567B", codabar = true))
        assertEquals("0001234567", n("c0001234567d", codabar = true))
        assertEquals("0001234567", n("D0001234567D", codabar = true))
        assertEquals("0001234567", n("A0001234567", codabar = true))
        assertEquals("0001234567", n("0001234567B", codabar = true))
    }

    @Test fun otherCodesKeepTrailingLetters() {
        assertEquals("0001234567B", n("0001234567B"))
        assertEquals("0001234567X", n("0001234567X"))
        assertEquals("000123456X", n("000123456X"))
        assertEquals("0001234567X", n("0001234567X", codabar = true))
    }

    @Test fun rejectsEverythingElse() {
        assertNull(n("ABC12345678"))
        assertNull(n("0001-234567"))
        assertNull(n("0001234567ä"))
        assertNull(n("https://example.org/00012345678"))
        assertNull(n("٠٠٠١٢٣٤٥٦٧"))
        assertNull(n("ABCD", codabar = true))
    }
}
