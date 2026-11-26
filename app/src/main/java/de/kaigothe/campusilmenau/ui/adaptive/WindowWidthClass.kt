package de.kaigothe.campusilmenau.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Breitenklasse des Fensters nach den Material-Grenzen (600 und 840 dp). Sie bestimmt die Hülle: Compact hat die
 * Tab-Leiste unten, Medium eine Navigation Rail, Expanded eine Seitenleiste mit Beschriftung (iPad: `PadRootView`).
 * Maßgeblich ist das Fenster, nicht das Gerät: Split-Screen und freie Fenster wechseln die Klasse.
 */
enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded,
    ;

    /** Mehr als eine Spalte Platz: Tab-Leiste weicht der Seitenleiste. */
    val usesSideNavigation: Boolean get() = this != Compact

    companion object {
        const val MediumMinDp = 600
        const val ExpandedMinDp = 840

        fun of(widthDp: Int): WindowWidthClass = when {
            widthDp >= ExpandedMinDp -> Expanded
            widthDp >= MediumMinDp -> Medium
            else -> Compact
        }
    }
}

/** Die Breitenklasse des Fensters, wechselt mit Drehen, Falten und Größenänderung. */
@Composable
fun currentWindowWidthClass(): WindowWidthClass {
    val width = LocalWindowInfo.current.containerSize.width
    val density = LocalDensity.current
    return WindowWidthClass.of(with(density) { width.toDp() }.value.toInt())
}
