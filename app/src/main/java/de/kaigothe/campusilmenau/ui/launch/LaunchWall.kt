package de.kaigothe.campusilmenau.ui.launch

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser

/** Farben der Start-Animation. Sie folgen dem **System**, nicht der Darstellung der App: Der Splash tut es auch. */
internal class LaunchColors(
    val wallSolid: Color,
    val wallStart: Color,
    val wallEnd: Color,
    val goatFill: Color,
    val edge: Color,
    val ringInner: Color,
    val ringOuter: Color,
    val dark: Boolean,
)

internal val LightLaunchColors = LaunchColors(
    wallSolid = Color(0xFFFF861E),
    wallStart = Color(0xFFFF5C1A),
    wallEnd = Color(0xFFFFB121),
    goatFill = Color.White,
    edge = Color(0xFFA33A00).copy(alpha = 0.30f),
    ringInner = Color.White.copy(alpha = 0.16f),
    ringOuter = Color.White.copy(alpha = 0.09f),
    dark = false,
)

internal val DarkLaunchColors = LaunchColors(
    wallSolid = Color(0xFF0E1A2E),
    wallStart = Color(0xFF0E1A2E),
    wallEnd = Color(0xFF1D2F4F),
    goatFill = Color(0xFF0E1A2E),
    edge = Color(0xFFFF7A1F).copy(alpha = 0.70f),
    ringInner = Color(0xFFFF5C1A).copy(alpha = 0.24f),
    ringOuter = Color(0xFFFF5C1A).copy(alpha = 0.12f),
    dark = true,
)

private val BrandOrange = Color(0xFFFF5C1A)
private val BrandYellow = Color(0xFFFFB121)

/**
 * Die Wand der Start-Animation mit der Ziege als Loch. Liegt als Overlay über der echten Startansicht.
 * [progress] liefert den Fortschritt 0…1 über die ganze Dauer. Er wird nur beim Zeichnen gelesen, die Wand wird
 * also pro Bild neu gezeichnet, aber nie neu zusammengesetzt.
 */
@Composable
internal fun LaunchWall(progress: () -> Float, modifier: Modifier = Modifier) {
    val colors = if (isSystemInDarkTheme()) DarkLaunchColors else LightLaunchColors
    val goat = remember { PathParser().parsePathString(GoatMask).toPath() }
    val scratch = remember { Path() }
    val matrix = remember { Matrix() }

    // Die Ebene wird einzeln gezeichnet, damit das Stanzen (BlendMode.Clear) nur die Wand trifft, nicht den Inhalt darunter.
    Canvas(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val unit = LaunchTimeline.UNIT_DP * density
        val frame = LaunchFrame(progress(), size.width, size.height, unit)

        // Die Maske in Bildschirmkoordinaten. Alles Weitere arbeitet in Pixeln, damit Linienbreiten und
        // Weichzeichnung nicht mit dem Zoom wachsen.
        matrix.reset()
        matrix.translate(frame.originX, frame.originY)
        matrix.scale(frame.unitScaled, frame.unitScaled)
        scratch.rewind()
        scratch.addPath(goat)
        scratch.transform(matrix)

        drawWall(colors, frame)
        if (colors.dark) drawSun(frame)
        drawRings(colors, frame)
        drawPath(scratch, Color.Black, blendMode = BlendMode.Clear)

        if (frame.edgeOpacity > 0.001f) drawEdgeGlow(colors, frame, scratch)

        if (frame.coverOpacity > 0.001f) {
            drawPath(scratch, colors.goatFill.copy(alpha = colors.goatFill.alpha * frame.coverOpacity))
        }
    }
}

/**
 * Hell: Diagonale von links oben nach rechts unten. Dunkel: senkrecht. In der Haltephase geht die Vollfarbe des
 * Splash in den Verlauf über.
 */
private fun DrawScope.drawWall(colors: LaunchColors, frame: LaunchFrame) {
    val start = lerp(colors.wallSolid, colors.wallStart, frame.wallBlend)
    val end = lerp(colors.wallSolid, colors.wallEnd, frame.wallBlend)
    val brush = if (colors.dark) {
        Brush.linearGradient(listOf(start, end), start = Offset.Zero, end = Offset(0f, size.height))
    } else {
        Brush.linearGradient(listOf(start, end), start = Offset.Zero, end = Offset(size.width, size.height))
    }
    drawRect(brush)
}

/** Abendsonne hinter der Ziege, Horizont an den Hufen. Gehört zur Wand und wird mit ausgestanzt. */
private fun DrawScope.drawSun(frame: LaunchFrame) {
    val k = frame.unitScaled
    val center = Offset(frame.originX + LaunchTimeline.SUN_X * k, frame.originY + LaunchTimeline.SUN_Y * k)
    val radius = LaunchTimeline.SUN_RADIUS * k
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(BrandOrange, BrandYellow),
            startY = center.y - radius,
            endY = center.y + radius,
        ),
        radius = radius,
        center = center,
    )
    val horizon = frame.originY + LaunchTimeline.SUN_HORIZON * k
    if (horizon < size.height) {
        drawRect(
            color = DarkLaunchColors.wallStart,
            topLeft = Offset(-1f, horizon),
            size = androidx.compose.ui.geometry.Size(size.width + 2f, size.height - horizon + 1f),
        )
    }
}

/** Zwei Ringe um den Anker, wachsen langsamer als das Loch (Parallaxe). */
private fun DrawScope.drawRings(colors: LaunchColors, frame: LaunchFrame) {
    val unit = LaunchTimeline.UNIT_DP * density
    val ringColors = listOf(colors.ringInner, colors.ringOuter)
    LaunchTimeline.RING_RADII.forEachIndexed { index, radiusUnits ->
        val color = ringColors[index]
        drawCircle(
            color = color.copy(alpha = color.alpha * frame.wallBlend),
            radius = radiusUnits * unit * frame.ringScale,
            center = Offset(frame.centerX, frame.centerY),
            style = Stroke(width = LaunchTimeline.RING_LINE_WIDTH_DP * density),
        )
    }
}

/** Kantenlicht: weiche Zeichnung der Lochkante nach innen (Wand wirft Schatten bzw. Sonnenlicht scheint durch). */
private fun DrawScope.drawEdgeGlow(colors: LaunchColors, frame: LaunchFrame, hole: Path) {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            style = PaintingStyle.Stroke
            strokeWidth = LaunchTimeline.EDGE_STROKE_DP * density
            color = colors.edge.copy(alpha = colors.edge.alpha * frame.edgeOpacity)
            asFrameworkPaint().maskFilter =
                BlurMaskFilter(LaunchTimeline.EDGE_BLUR_DP * density, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.save()
        canvas.clipPath(hole)
        canvas.drawPath(hole, paint)
        canvas.restore()
    }
}
