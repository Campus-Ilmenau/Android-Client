package de.kaigothe.campusilmenau.ui.share.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.mensa.ChipTone
import de.kaigothe.campusilmenau.domain.model.mensa.MealChip
import de.kaigothe.campusilmenau.domain.model.share.ShareBranding
import de.kaigothe.campusilmenau.domain.model.share.ShareStrings
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.PlusJakartaSans
import de.kaigothe.campusilmenau.ui.theme.ShareCardColors

// Bildkarten zum Teilen nach `Uebergabe.md`, Kapitel „Teilen“, Artboard `TeilenKarte.dc.html`. Alle Maße sind feste dp
// bei Dichte 3 (360 dp = 1080 px): Das Bild sieht auf jedem Gerät gleich aus, egal ob Hell/Dunkel oder Schriftgröße.

/** Darstellung des Bildes: hell (Speiseplan, Gericht) oder nachtblau (Veranstaltung), unabhängig vom Gerät. */
enum class ShareCardStyle { Light, Night }

object ShareCardSpec {
    /** Breite in dp; mit [Scale] sind das 1080 px. */
    const val WidthDp = 360
    const val Scale = 3f
    const val MinHeightDp = 450
}

/** Das Bild gerendert: feste Dichte und Schriftgröße, fester Modus. Für Bild und Vorschau gleich. */
@Composable
fun ShareCardTheme(style: ShareCardStyle, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDensity provides Density(density = ShareCardSpec.Scale, fontScale = 1f)) {
        CampusIlmenauTheme(darkTheme = style == ShareCardStyle.Night, content = content)
    }
}

/**
 * Rahmen des Bildes: 360 dp breit, Höhe nach Inhalt (mindestens 450 dp), Verlauf als Hintergrund, 16 dp Rand, darin die
 * Karte. Gehört in [ShareCardTheme].
 */
@Composable
fun ShareCardFrame(style: ShareCardStyle, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .width(ShareCardSpec.WidthDp.dp)
            .heightIn(min = ShareCardSpec.MinHeightDp.dp)
            .shareBackdrop(style)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp),
    ) {
        content()
    }
}

/**
 * Ein elliptischer Schein: ein runder Verlauf, auf die beiden Radien gestreckt. [widthFactor] und [heightFactor] sind die
 * Radien als Anteil von Breite und Höhe der Fläche (CSS: Größe × Stopp-Position), [anchorX] und [anchorY] der Mittelpunkt.
 */
fun Modifier.shareGlow(color: Color, anchorX: Float, anchorY: Float, widthFactor: Float, heightFactor: Float): Modifier =
    clipToBounds().drawBehind { drawGlow(color, anchorX, anchorY, widthFactor, heightFactor) }

private fun DrawScope.drawGlow(color: Color, anchorX: Float, anchorY: Float, widthFactor: Float, heightFactor: Float) {
    val rx = size.width * widthFactor
    val ry = size.height * heightFactor
    if (rx <= 0f || ry <= 0f) return
    val center = Offset(size.width * anchorX, size.height * anchorY)
    scale(scaleX = 1f, scaleY = ry / rx, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = center, radius = rx),
            radius = rx,
            center = center,
        )
    }
}

/**
 * Verlauf. Hell: Orange oben rechts und Blau unten links auf hellem Blau. Nachtblau: dasselbe mit dunklem Orange und
 * `surfaceSecondary` auf `background`.
 */
private fun Modifier.shareBackdrop(style: ShareCardStyle): Modifier =
    drawBehind {
        when (style) {
            ShareCardStyle.Light -> {
                drawRect(ShareCardColors.Backdrop)
                // CSS: radial-gradient(120% 60% at 90% 0%, orange.200 0, transparent 60%)
                drawGlow(ShareCardColors.GlowOrange, 0.9f, 0f, 1.2f * 0.6f, 0.6f * 0.6f)
                // CSS: radial-gradient(110% 60% at 0% 100%, blue.200 0, transparent 65%)
                drawGlow(ShareCardColors.GlowBlue, 0f, 1f, 1.1f * 0.65f, 0.6f * 0.65f)
            }
            ShareCardStyle.Night -> {
                drawRect(ShareCardColors.NightBackground)
                // CSS: radial-gradient(90% 50% at 90% 0%, orange.900 0, transparent 70%)
                drawGlow(ShareCardColors.NightGlowOrange, 0.9f, 0f, 0.9f * 0.7f, 0.5f * 0.7f)
                // CSS: radial-gradient(110% 60% at 0% 100%, blue.900 0, transparent 65%)
                drawGlow(ShareCardColors.NightGlowBlue, 0f, 1f, 1.1f * 0.65f, 0.6f * 0.65f)
            }
        }
    }

/** Die weiße Karte: `surface`, Ecken 22, feiner Schatten. */
@Composable
fun Modifier.shareCardSurface(): Modifier {
    val colors = CampusTheme.colors
    val shape = RoundedCornerShape(22.dp)
    return fillMaxWidth()
        .shadow(1.dp, shape, ambientColor = colors.brandNight.copy(alpha = 0.06f), spotColor = colors.brandNight.copy(alpha = 0.06f))
        .clip(shape)
        .background(colors.surface)
}

/** Trennlinie oben, 1 dp `separator`; bei der ersten Zeile einer Liste [hidden]. */
@Composable
fun Modifier.shareTopRule(hidden: Boolean = false): Modifier {
    if (hidden) return this
    val color = CampusTheme.colors.separator
    return drawBehind { drawRect(color, size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx())) }
}

/** Chip auf dem Bild: 11 sp Semibold, 8/2 dp Innenabstand. Grün und Blau sind Flächen mit eigener Schriftfarbe. */
@Composable
fun ShareChip(title: String, tone: ChipTone, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val (background, foreground) = when (tone) {
        ChipTone.Green -> colors.green to colors.onGreen
        ChipTone.Blue -> colors.blue to colors.onBlue
        ChipTone.Neutral -> colors.surfaceSecondary to colors.textSecondary
    }
    ChipText(title, background, foreground, modifier)
}

@Composable
fun ChipText(title: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = foreground),
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ShareChips(chips: List<MealChip>, strings: ShareStrings, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        chips.forEach { ShareChip(strings.chipTitle(it), it.tone) }
    }
}

/** Plus Jakarta Sans ExtraBold für große Titel und Zahlen. */
fun displayStyle(size: Int, lineHeight: Int, color: Color) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.ExtraBold,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = (-0.02).sp,
    color = color,
)

/** Wo die Marke steht: letzte Zeile in der weißen Karte oder Fußzeile auf dem Hintergrund. */
enum class ShareBrandPlacement { InCard, OnBackground }

/** Marke: Ziege (`brandOrange`), „Campus Ilmenau“ und die Webadresse. */
@Composable
fun ShareBrandFooter(modifier: Modifier = Modifier, placement: ShareBrandPlacement = ShareBrandPlacement.InCard) {
    val colors = CampusTheme.colors
    val padding = when (placement) {
        ShareBrandPlacement.InCard -> Modifier.shareTopRule().padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 14.dp)
        ShareBrandPlacement.OnBackground -> Modifier.padding(start = 4.dp, top = 2.dp, end = 4.dp)
    }
    Row(
        modifier = modifier.fillMaxWidth().then(padding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_symbol_goat), contentDescription = null, tint = colors.brandOrange, modifier = Modifier.size(24.dp))
        Text(
            text = "Campus Ilmenau",
            style = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = (-0.15).sp, color = colors.textPrimary),
            maxLines = 1,
            softWrap = false,
        )
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            Text(
                text = ShareBranding.DOMAIN,
                style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary),
                maxLines = 1,
            )
        }
    }
}

/** Ein Polygon in Einheitskoordinaten (0…1) der Fläche, für die Diagonalbänder der Köpfe. */
fun DrawScope.drawUnitPolygon(points: List<Pair<Float, Float>>, color: Color) {
    val path = Path()
    points.forEachIndexed { index, (x, y) ->
        if (index == 0) path.moveTo(x * size.width, y * size.height) else path.lineTo(x * size.width, y * size.height)
    }
    path.close()
    drawPath(path, color)
}
