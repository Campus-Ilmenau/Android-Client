package de.kaigothe.campusilmenau.ui.navigation

/**
 * Tastenkürzel der Shell für Tablets und Chromebooks mit Tastatur. Die Zuordnung steht hier ohne Compose, damit sie sich
 * testen lässt; die Shell übersetzt die Tasten in diese Fälle (`AppShell`).
 *
 * - **Strg + 1 … 5**: Tab in der Reihenfolge der Leiste (ohne NFC fehlt Thoska, die Zahlen rücken nach).
 * - **Strg + ,**: „Mehr“.
 * - **Strg + F**: Katalogsuche.
 * - **Esc**: eine Seite zurück, solange der Tab mehr als seine Wurzel zeigt.
 */
sealed interface ShortcutAction {
    data class SelectTab(val tab: AppTab) : ShortcutAction
    data object OpenMehr : ShortcutAction
    data object OpenCatalog : ShortcutAction
    data object Back : ShortcutAction
}

object AppShortcuts {
    /** Die Zahl des Kürzels eines Tabs (für Tooltips „Strg+2“, die Taste heißt je Sprache anders), `null` ohne Zahl. */
    fun numberFor(tab: AppTab, tabs: List<AppTab>): Int? = tabs.indexOf(tab).takeIf { it in 0..8 }?.let { it + 1 }

    /** Die Aktion zu einer Zifferntaste mit gedrückter Strg-Taste; `null`, wenn es den Tab nicht gibt. */
    fun forDigit(digit: Int, tabs: List<AppTab>): ShortcutAction? =
        tabs.getOrNull(digit - 1)?.let(ShortcutAction::SelectTab)
}
