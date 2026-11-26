package de.kaigothe.campusilmenau.ui.adaptive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowWidthClassTest {
    @Test
    fun `phones are compact`() {
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.of(360))
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.of(599))
    }

    @Test
    fun `boundaries at 600 and 840 dp`() {
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.of(600))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.of(839))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.of(840))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.of(1280))
    }

    @Test
    fun `only compact keeps the bottom bar`() {
        assertFalse(WindowWidthClass.Compact.usesSideNavigation)
        assertTrue(WindowWidthClass.Medium.usesSideNavigation)
        assertTrue(WindowWidthClass.Expanded.usesSideNavigation)
    }
}
