package de.kaigothe.campusilmenau.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Abstände im 4-dp-Raster (Design „Logo 2026“). */
object Spacing {
    val Xs: Dp = 4.dp
    val Sm: Dp = 8.dp
    val Md: Dp = 12.dp
    val Lg: Dp = 16.dp
    val Xl: Dp = 24.dp

    /** Abstand zum Bildschirmrand. */
    val ScreenEdge: Dp = 16.dp

    /** Abstand zwischen Karten. */
    val CardGap: Dp = 10.dp

    /** Abstand zwischen Abschnitten. */
    val SectionGap: Dp = 22.dp

    /** Mindestgröße für Tippziele (Material-Richtlinie, größer als die 44 pt von iOS). */
    val MinTouchTarget: Dp = 48.dp
}
