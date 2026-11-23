package de.kaigothe.campusilmenau.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.ui.theme.CampusTheme

/** Die drei Anordnungen aus dem Mockup. */
enum class BandsStyle {
    /** Gericht-Detail: zwei Bänder von links oben nach rechts unten auf `background`. */
    Header,

    /**
     * Veranstaltung: blaue Fläche, grünes Band rechts, am Rand läuft es in `background` aus.
     * Text gehört in die linke Hälfte (im Mockup rechts 140 dp Abstand), sonst liegt er auf Grün.
     */
    Event,

    /** „Heute“-Karte: blaue Fläche mit schmalem grünem Band rechts. */
    Hero,
}

/**
 * Welche Ebenen gezeichnet werden. Nur [BandsStyle.Header] kennt zwei Ebenen: Mit [Behind] (Hintergrund und grünes
 * Band) und [Front] (blaues Band mit Schatten) lässt sich Inhalt zwischen die beiden legen, sodass er über dem
 * Grünen liegt und genau an der blauen Schräge endet.
 */
enum class BandsLayer { All, Behind, Front }

private typealias Points = List<Pair<Float, Float>>

private val HeaderGreen: Points = listOf(0f to 0f, 1f to 0f, 1f to 0.46f, 0f to 1f)
private val HeaderBlue: Points = listOf(0f to 0f, 1f to 0f, 1f to 0.22f, 0f to 0.76f)
private val EventGreen: Points = listOf(0.58f to 0f, 1f to 0f, 1f to 1f, 0.30f to 1f)
private val EventBackground: Points = listOf(0.84f to 0f, 1f to 0f, 1f to 1f, 0.56f to 1f)
private val HeroGreen: Points = listOf(0.78f to 0f, 1f to 0f, 1f to 1f, 0.62f to 1f)

/**
 * Kopfbereich mit zwei diagonalen Flächen (Blau vorne, Grün dahinter) und weichem Schatten an der Kante.
 * Der Inhalt liegt darüber, unten links ausgerichtet. Schrift auf Blau ist `onBlue`.
 * Die Punkte stehen in Einheitskoordinaten (0…1), damit die Flächen mit der Größe mitwachsen.
 */
@Composable
fun DiagonalBands(
    modifier: Modifier = Modifier,
    style: BandsStyle = BandsStyle.Header,
    layer: BandsLayer = BandsLayer.All,
    height: Dp? = 250.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = CampusTheme.colors
    val background = MaterialTheme.colorScheme.background
    val shadow = colors.brandNight.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier)
            .clipToBounds(),
        contentAlignment = Alignment.BottomStart,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            when (style) {
                BandsStyle.Header -> {
                    if (layer != BandsLayer.Front) {
                        drawRect(background)
                        band(HeaderGreen, colors.green)
                    }
                    if (layer != BandsLayer.Behind) {
                        band(HeaderBlue, colors.blue, shadow)
                    }
                }

                BandsStyle.Event -> {
                    drawRect(colors.blue)
                    band(EventGreen, colors.green, shadow)
                    band(EventBackground, background)
                }

                BandsStyle.Hero -> {
                    drawRect(colors.blue)
                    band(HeroGreen, colors.green, shadow)
                }
            }
        }
        content()
    }
}

private fun DrawScope.band(points: Points, color: Color, shadow: Color? = null) {
    val path = Path().apply {
        points.forEachIndexed { index, (x, y) ->
            if (index == 0) moveTo(x * size.width, y * size.height) else lineTo(x * size.width, y * size.height)
        }
        close()
    }
    if (shadow == null) {
        drawPath(path, color)
    } else {
        // Weicher Schatten an der Kante. Der Schattenradius wirkt auf dem Hardware-Canvas ab Android 9,
        // auf älteren Versionen fehlt nur der Schatten.
        drawIntoCanvas { canvas ->
            val paint = Paint().apply { this.color = color }
            paint.asFrameworkPaint().setShadowLayer(8.dp.toPx(), 0f, 4.dp.toPx(), shadow.toArgb())
            canvas.drawPath(path, paint)
        }
    }
}
