package de.kaigothe.campusilmenau.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Alle Farben der App, Werte aus `Design/Logo 2026/Farben/tokens.json` (Light und Dark).
 *
 * Screens lesen Farben nie als lose Konstanten, sondern über `MaterialTheme.colorScheme` oder
 * [CampusTheme.colors]. Regeln aus dem Design:
 * - Grün und Blau nur als Flächen (Bänder, Badges), nie als Text oder Button.
 * - Buttons neutral (`buttonPrimary`, `buttonSecondary`), Rot nur für kritische Aktionen.
 * - `accent` (Orange) für Links, aktiven Tab und Auswahl, nicht für Buttons.
 * - Dark Mode ist Nachtblau `#0E1A2E`, nicht Schwarz.
 */
@Immutable
class CampusColors(
    // Flächen
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val separator: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    // Ilm-Orange
    val accent: Color,
    val accentSoft: Color,
    // Grün und Blau (nur Flächen)
    val green: Color,
    val onGreen: Color,
    val greenSoft: Color,
    val blue: Color,
    val onBlue: Color,
    val blueSoft: Color,
    // Buttons
    val buttonPrimary: Color,
    val onButtonPrimary: Color,
    val buttonSecondary: Color,
    val onButtonSecondary: Color,
    val buttonDestructive: Color,
    val onButtonDestructive: Color,
    // Wischtasten in Listen (in Light und Dark gleich)
    val swipeFound: Color,
    val swipeNeutral: Color,
    val swipeDelete: Color,
    val onSwipe: Color,
    // Status
    val warning: Color,
    val warningFill: Color,
    val onWarningFill: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    // Marke (ohne Dark-Variante: Icon, Abendsonne, Start-Animation)
    val brandOrange: Color,
    val brandYellow: Color,
    val brandNight: Color,
    val brandDusk: Color,
)

internal val LightCampusColors = CampusColors(
    background = Color(0xFFF4F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceSecondary = Color(0xFFEAEDF1),
    separator = Color(0xFFD8DBDF),
    textPrimary = Color(0xFF171B21),
    textSecondary = Color(0xFF5E6064),
    textTertiary = Color(0xFF75777B),
    accent = Color(0xFFCE3F00),
    accentSoft = Color(0xFFFFF7F4),
    green = Color(0xFF72B54D),
    onGreen = Color(0xFF0E1A2E),
    greenSoft = Color(0xFFE9FEE0),
    blue = Color(0xFF2B497F),
    onBlue = Color(0xFFFFFFFF),
    blueSoft = Color(0xFFF2F7FF),
    buttonPrimary = Color(0xFF171B21),
    onButtonPrimary = Color(0xFFFFFFFF),
    buttonSecondary = Color(0xFFEAEDF1),
    onButtonSecondary = Color(0xFF171B21),
    buttonDestructive = Color(0xFFD12E54),
    onButtonDestructive = Color(0xFFFFFFFF),
    swipeFound = Color(0xFF2E720F),
    swipeNeutral = Color(0xFF5E6064),
    swipeDelete = Color(0xFFD12E54),
    onSwipe = Color(0xFFFFFFFF),
    warning = Color(0xFF845500),
    warningFill = Color(0xFFFFB121),
    onWarningFill = Color(0xFF0E1A2E),
    warningSoft = Color(0xFFFFF5E8),
    danger = Color(0xFFD12E54),
    dangerSoft = Color(0xFFFFF4F4),
    brandOrange = Color(0xFFFF5C1A),
    brandYellow = Color(0xFFFFB121),
    brandNight = Color(0xFF0E1A2E),
    brandDusk = Color(0xFF1D2F4F),
)

internal val DarkCampusColors = CampusColors(
    background = Color(0xFF0E1A2E),
    surface = Color(0xFF17253D),
    surfaceSecondary = Color(0xFF1D2F4F),
    separator = Color(0xFF2F405D),
    textPrimary = Color(0xFFF4F7FB),
    textSecondary = Color(0xFFACAEB2),
    textTertiary = Color(0xFF96989C),
    accent = Color(0xFFFF8760),
    accentSoft = Color(0xFF412F37),
    green = Color(0xFF72B54D),
    onGreen = Color(0xFF0E1A2E),
    greenSoft = Color(0xFF375743),
    blue = Color(0xFF2B497F),
    onBlue = Color(0xFFFFFFFF),
    blueSoft = Color(0xFF1E3254),
    buttonPrimary = Color(0xFFF4F7FB),
    onButtonPrimary = Color(0xFF0E1A2E),
    buttonSecondary = Color(0xFF1D2F4F),
    onButtonSecondary = Color(0xFFF4F7FB),
    buttonDestructive = Color(0xFFFF8190),
    onButtonDestructive = Color(0xFF0E1A2E),
    swipeFound = Color(0xFF2E720F),
    swipeNeutral = Color(0xFF5E6064),
    swipeDelete = Color(0xFFD12E54),
    onSwipe = Color(0xFFFFFFFF),
    warning = Color(0xFFFFB121),
    warningFill = Color(0xFFFFB121),
    onWarningFill = Color(0xFF0E1A2E),
    warningSoft = Color(0xFF413E38),
    danger = Color(0xFFFF8190),
    dangerSoft = Color(0xFF41364C),
    brandOrange = Color(0xFFFF5C1A),
    brandYellow = Color(0xFFFFB121),
    brandNight = Color(0xFF0E1A2E),
    brandDusk = Color(0xFF1D2F4F),
)

/**
 * Farben der Bildkarten zum Teilen. Die Karten sehen auf jedem Gerät gleich aus (hell oder nachtblau, unabhängig vom
 * Modus), deshalb stehen die Werte hier fest: orange.200, blue.200 und blue.100 der Palette für den hellen Verlauf,
 * orange.900 für den Schein auf dem Nachtblau.
 */
object ShareCardColors {
    val Backdrop = Color(0xFFE3EEFF)
    val GlowOrange = Color(0xFFFFCEBE)
    val GlowBlue = Color(0xFFC6DCFF)
    val NightGlowOrange = Color(0xFF571808)

    /** Nachtblauer Grund und Schein unten links: `background` und `surfaceSecondary` des dunklen Themes. */
    val NightBackground = DarkCampusColors.background
    val NightGlowBlue = DarkCampusColors.surfaceSecondary
}
