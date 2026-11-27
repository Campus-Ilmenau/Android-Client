package de.kaigothe.campusilmenau.ui.adaptive

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveColumnsTest {
    @Test
    fun `narrow content has one column`() {
        assertEquals(1, AdaptiveColumns.count(availableDp = 360, minColumnDp = 320, maxColumns = 4))
        assertEquals(1, AdaptiveColumns.count(availableDp = 0, minColumnDp = 320, maxColumns = 4))
    }

    @Test
    fun `columns grow with the width up to the maximum`() {
        assertEquals(2, AdaptiveColumns.count(availableDp = 640, minColumnDp = 320, maxColumns = 4))
        assertEquals(3, AdaptiveColumns.count(availableDp = 960, minColumnDp = 320, maxColumns = 4))
        assertEquals(4, AdaptiveColumns.count(availableDp = 1600, minColumnDp = 320, maxColumns = 4))
        assertEquals(2, AdaptiveColumns.count(availableDp = 1600, minColumnDp = 320, maxColumns = 2))
    }

    @Test
    fun `gaps count against the width`() {
        // Zwei Spalten à 320 dp und 16 dp Abstand brauchen 656 dp.
        assertEquals(1, AdaptiveColumns.count(availableDp = 655, minColumnDp = 320, maxColumns = 4, gapDp = 16))
        assertEquals(2, AdaptiveColumns.count(availableDp = 656, minColumnDp = 320, maxColumns = 4, gapDp = 16))
    }

    @Test
    fun `invalid input falls back to one column`() {
        assertEquals(1, AdaptiveColumns.count(availableDp = 900, minColumnDp = 0, maxColumns = 3))
        assertEquals(1, AdaptiveColumns.count(availableDp = 900, minColumnDp = 300, maxColumns = 1))
    }

    @Test
    fun `column width subtracts the gaps`() {
        assertEquals(320, AdaptiveColumns.columnWidthDp(availableDp = 656, columns = 2, gapDp = 16))
        assertEquals(500, AdaptiveColumns.columnWidthDp(availableDp = 500, columns = 1, gapDp = 16))
    }
}
