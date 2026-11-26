package de.kaigothe.campusilmenau.domain.model.library.ausweis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Code39Test {
    @Test
    fun `every pattern has nine elements and exactly three wide ones`() {
        Code39.patterns.forEach { (char, pattern) ->
            assertEquals("Länge von $char", 9, pattern.length)
            assertEquals("breite Elemente von $char", 3, pattern.count { it == 'w' })
        }
    }

    @Test
    fun `text gets start and stop characters with narrow gaps between characters`() {
        val elements = Code39.elements("A")!!
        // 3 Zeichen (*, A, *) mit je 9 Elementen und 2 Lücken dazwischen.
        assertEquals(29, elements.size)
        assertTrue(elements.first().isBar)
        assertTrue(elements.last().isBar)
        assertEquals(false, elements[9].isBar)
        assertEquals(false, elements[9].isWide)
        assertEquals(9, elements.count { it.isWide })
    }

    @Test
    fun `lowercase becomes uppercase`() {
        assertEquals(Code39.elements("ab12"), Code39.elements("AB12"))
    }

    @Test
    fun `unknown characters, an asterisk and empty text give no image`() {
        assertNull(Code39.elements(""))
        assertNull(Code39.elements("12*3"))
        assertNull(Code39.elements("12ä"))
        assertNull(Code39.layout("12ä", 1000, 2f))
    }

    @Test
    fun `wide bars are about two and a half times the narrow ones`() {
        // 2,5 rundet auf 3, wie in der iOS-App.
        assertEquals(3, Code39.wideFor(1))
        assertEquals(5, Code39.wideFor(2))
        assertEquals(8, Code39.wideFor(3))
    }

    @Test
    fun `layout fits, uses whole pixels and keeps bars apart`() {
        val layout = Code39.layout("3400056749X", availablePixels = 1000, density = 2.75f)!!
        assertTrue(layout.width <= 1000)
        assertEquals(Code39.widthInPixels(Code39.elements("3400056749X")!!, layout.narrow), layout.width)
        layout.bars.zipWithNext().forEach { (a, b) -> assertTrue(a.x + a.width < b.x) }
        assertTrue(layout.bars.all { it.width == layout.narrow || it.width == Code39.wideFor(layout.narrow) })
        assertEquals(0, layout.bars.first().x)
    }

    @Test
    fun `narrow bar is at most 1_8 pixels per dp so the quiet zone fits`() {
        // Viel Platz, aber geringe Dichte: schmaler Strich höchstens 1 (1,8 * 1,0 abgerundet).
        assertEquals(1, Code39.layout("12345678", 5000, 1f)!!.narrow)
        // Bei 3,0 Pixeln pro dp sind es höchstens 5.
        assertEquals(5, Code39.layout("12345678", 5000, 3f)!!.narrow)
    }

    @Test
    fun `layout shrinks the bars when space is short and fails without any`() {
        val elements = Code39.elements("3400056749X")!!
        val tight = Code39.widthInPixels(elements, 2)
        assertEquals(2, Code39.layout("3400056749X", tight, 3f)!!.narrow)
        assertEquals(1, Code39.layout("3400056749X", tight - 1, 3f)!!.narrow)
        assertNull(Code39.layout("3400056749X", 0, 3f))
        assertNull(Code39.layout("3400056749X", 100, 0f))
        assertNotNull(Code39.layout("1", 1000, 3f))
    }
}
