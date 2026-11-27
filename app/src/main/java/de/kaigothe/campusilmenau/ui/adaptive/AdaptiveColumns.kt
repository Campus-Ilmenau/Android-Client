package de.kaigothe.campusilmenau.ui.adaptive

/**
 * Wie viele Spalten passen in die verfügbare Breite? Jede Spalte braucht mindestens [minColumnDp], dazwischen liegt je
 * [gapDp] Abstand; es sind höchstens [maxColumns] und mindestens eine. Die Breite ist die des Inhalts (ohne Seitenleiste),
 * nicht die des Fensters. Pendant zu den Rastern in `PadRaster` der iOS-App.
 */
object AdaptiveColumns {
    fun count(availableDp: Int, minColumnDp: Int, maxColumns: Int, gapDp: Int = 0): Int {
        if (availableDp <= 0 || minColumnDp <= 0 || maxColumns <= 1) return 1
        // n Spalten brauchen n * min + (n - 1) * gap  <=  verfügbar
        val fitting = (availableDp + gapDp) / (minColumnDp + gapDp)
        return fitting.coerceIn(1, maxColumns)
    }

    /** Breite einer Spalte bei [columns] Spalten in [availableDp], nach Abzug der Abstände. */
    fun columnWidthDp(availableDp: Int, columns: Int, gapDp: Int = 0): Int =
        if (columns <= 1) availableDp else (availableDp - gapDp * (columns - 1)) / columns
}
