package de.kaigothe.campusilmenau.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.R

/**
 * Plus Jakarta Sans (SIL Open Font License, siehe `docs/licenses/PlusJakartaSans-OFL.txt`).
 * Nur für große Titel und Zahlen. Die Oberfläche selbst nutzt die System-Schrift.
 */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

/** Laufweite großer Titel: −2 % (Design „Logo 2026“). */
private val DisplayTracking = (-0.02).em

private fun display(sizeSp: Int, lineHeightSp: Int) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.ExtraBold,
    fontSize = sizeSp.sp,
    lineHeight = lineHeightSp.sp,
    letterSpacing = DisplayTracking,
)

/**
 * Material-Typografie. Die drei `display*`-Stile sind die großen Titel und Zahlen in Plus Jakarta Sans
 * ExtraBold (Seitentitel 38, Titel 34, Begrüßung und Guthaben 32). Alle übrigen Stile bleiben bei der
 * System-Schrift und skalieren mit der Schriftgröße des Systems (sp).
 */
val CampusTypography = Typography(
    displayLarge = display(sizeSp = 38, lineHeightSp = 44),
    displayMedium = display(sizeSp = 34, lineHeightSp = 40),
    displaySmall = display(sizeSp = 32, lineHeightSp = 38),
)
