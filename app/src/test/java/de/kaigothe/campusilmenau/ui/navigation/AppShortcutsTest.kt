package de.kaigothe.campusilmenau.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppShortcutsTest {
    private val all = AppTab.visible(hasNfc = true)
    private val noNfc = AppTab.visible(hasNfc = false)

    @Test
    fun `digits select the tabs in bar order`() {
        assertEquals(ShortcutAction.SelectTab(AppTab.Heute), AppShortcuts.forDigit(1, all))
        assertEquals(ShortcutAction.SelectTab(AppTab.Thoska), AppShortcuts.forDigit(3, all))
        assertEquals(ShortcutAction.SelectTab(AppTab.Events), AppShortcuts.forDigit(5, all))
    }

    @Test
    fun `without nfc the numbers move up`() {
        assertEquals(ShortcutAction.SelectTab(AppTab.Bibliothek), AppShortcuts.forDigit(3, noNfc))
        assertEquals(ShortcutAction.SelectTab(AppTab.Events), AppShortcuts.forDigit(4, noNfc))
        assertNull(AppShortcuts.forDigit(5, noNfc))
    }

    @Test
    fun `digits outside the tabs do nothing`() {
        assertNull(AppShortcuts.forDigit(0, all))
        assertNull(AppShortcuts.forDigit(6, all))
        assertNull(AppShortcuts.forDigit(9, all))
    }

    @Test
    fun `tooltip numbers follow the same numbering`() {
        assertEquals(1, AppShortcuts.numberFor(AppTab.Heute, all))
        assertEquals(3, AppShortcuts.numberFor(AppTab.Bibliothek, noNfc))
        assertNull(AppShortcuts.numberFor(AppTab.Thoska, noNfc))
    }
}
