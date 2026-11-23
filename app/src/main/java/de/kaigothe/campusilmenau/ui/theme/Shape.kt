package de.kaigothe.campusilmenau.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Ecken laut Design: Karten 16, Buttons 12, Segment 10, Chips rund (Kapsel), Sheets 14 oben.
 * Die Material-Slots sind so belegt, dass Standard-Komponenten die Werte ohne Zutun übernehmen.
 */
val CampusShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

/** Karten (`Card`). */
val CardShape = RoundedCornerShape(16.dp)

/** Buttons (`PrimaryButton`, `SecondaryButton`). */
val ButtonShape = RoundedCornerShape(12.dp)

/** Segmentierte Auswahl. */
val SegmentShape = RoundedCornerShape(10.dp)

/** Chips sind Kapseln. */
val ChipShape = CircleShape

/** Sheets: nur die oberen Ecken sind gerundet. */
val SheetShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
