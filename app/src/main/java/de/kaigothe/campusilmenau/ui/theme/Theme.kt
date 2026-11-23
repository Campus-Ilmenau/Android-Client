package de.kaigothe.campusilmenau.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** Farben ohne Material-Gegenstück (Chips, Bänder, Wischtasten, Marke). Zugriff über [CampusTheme.colors]. */
val LocalCampusColors = staticCompositionLocalOf { LightCampusColors }

object CampusTheme {
    val colors: CampusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCampusColors.current
}

/**
 * Belegung der Material-Slots aus den Design-Tokens.
 * - `primary` ist der neutrale Hauptbutton (`buttonPrimary`), nicht das Orange.
 * - `tertiary` ist `accent` (Links, aktiver Tab, Auswahl).
 * - `error` ist `danger`, nur für Fehler und kritische Aktionen.
 */
private fun CampusColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = buttonPrimary,
        onPrimary = onButtonPrimary,
        primaryContainer = buttonSecondary,
        onPrimaryContainer = onButtonSecondary,
        secondary = buttonSecondary,
        onSecondary = onButtonSecondary,
        secondaryContainer = surfaceSecondary,
        onSecondaryContainer = textPrimary,
        tertiary = accent,
        onTertiary = background,
        tertiaryContainer = accentSoft,
        onTertiaryContainer = accent,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceSecondary,
        onSurfaceVariant = textSecondary,
        surfaceTint = surface,
        inverseSurface = buttonPrimary,
        inverseOnSurface = onButtonPrimary,
        inversePrimary = accent,
        error = danger,
        onError = onButtonDestructive,
        errorContainer = dangerSoft,
        onErrorContainer = danger,
        outline = separator,
        outlineVariant = separator,
        surfaceBright = surface,
        surfaceDim = background,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceSecondary,
        surfaceContainerHighest = surfaceSecondary,
    )
}

/**
 * Theme der App. Dynamische Farben (Material You) sind bewusst aus: Die Marke bestimmt die Farben.
 * `darkTheme` kommt aus der Einstellung „Darstellung“ (System, Hell, Dunkel).
 */
@Composable
fun CampusIlmenauTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val campusColors = if (darkTheme) DarkCampusColors else LightCampusColors
    CompositionLocalProvider(LocalCampusColors provides campusColors) {
        MaterialTheme(
            colorScheme = campusColors.toColorScheme(dark = darkTheme),
            typography = CampusTypography,
            shapes = CampusShapes,
            content = content,
        )
    }
}
