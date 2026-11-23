package de.kaigothe.campusilmenau.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import androidx.compose.runtime.withFrameNanos

/** Ab dieser Breite gibt es Boden und Staub. */
private val GroundMinimumWidth = 60.dp

/** Pfade einmal geparst, im Koordinatenraum des SVG. */
@Composable
private fun rememberGoatPaths(): Map<GoatPart, Path> = remember {
    GoatPart.entries.associateWith { PathParser().parsePathString(it.pathData).toPath() }
}

/**
 * Ladeanimation: die Logo-Ziege rennt auf der Stelle und schaut dabei nach vorn.
 * Breite = [size], Höhe = [size] × 1100/1320. Größen laut Design: 96, 44 und 28 dp. Farbe überall `accent`.
 * Boden und Staub gibt es erst ab 60 dp Breite. Bei „Bewegung reduzieren“ steht die Ziege und pulsiert nur.
 * Die Animation hat keinen Text, sie ist für TalkBack ausgeblendet. Der Aufrufer beschriftet den Ladezustand.
 */
@Composable
fun GoatLoader(
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = CampusTheme.colors.accent,
) {
    val paths = rememberGoatPaths()
    val canvasModifier = modifier
        .size(width = size, height = size * GoatGeometry.ASPECT)
        .clearAndSetSemantics {}

    if (rememberReduceMotion()) {
        GoatReducedMotion(paths, color, canvasModifier)
    } else {
        GoatRunning(paths, color, size, canvasModifier)
    }
}

// MARK: - Rennende Ziege

@Composable
private fun GoatRunning(paths: Map<GoatPart, Path>, color: Color, size: Dp, modifier: Modifier) {
    // Die Zeit steht in einem State, der nur beim Zeichnen gelesen wird: Pro Bild wird neu gezeichnet,
    // aber nichts neu zusammengesetzt.
    val timeNanos = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { timeNanos.longValue = it - start }
        }
    }
    val showGround = size >= GroundMinimumWidth

    Canvas(modifier = modifier) {
        val time = timeNanos.longValue / 1_000_000_000.0
        val jump = GoatMotion.phase(time)
        val unit = this.size.width / GoatGeometry.VIEW_BOX_WIDTH
        scale(unit, pivot = Offset.Zero) {
            translate(-GoatGeometry.VIEW_BOX_X, -GoatGeometry.VIEW_BOX_Y) {
                if (showGround) {
                    drawGround(color, jump)
                    drawDust(color, time)
                }
                drawFigure(paths, color, jump, time)
            }
        }
    }
}

/** Körper, Kopf und alle Beine wippen gemeinsam um die Körpermitte. */
private fun DrawScope.drawFigure(paths: Map<GoatPart, Path>, color: Color, jump: Double, time: Double) {
    val bodyCenter = Offset(GoatGeometry.BODY_CENTER_X, GoatGeometry.BODY_CENTER_Y)
    translate(top = GoatMotion.bobLift(jump).toFloat()) {
        rotate(GoatMotion.bobAngle(jump).toFloat(), pivot = bodyCenter) {
            drawLeg(paths, color, GoatMotion.HindFar, time)
            drawLeg(paths, color, GoatMotion.ForeFar, time)
            drawPath(paths.getValue(GoatPart.Body), color)
            rotate(
                GoatMotion.headAngle(jump).toFloat(),
                pivot = Offset(GoatGeometry.HEAD_JOINT_X, GoatGeometry.HEAD_JOINT_Y),
            ) {
                drawPath(paths.getValue(GoatPart.Head), color)
            }
            drawLeg(paths, color, GoatMotion.HindNear, time)
            drawLeg(paths, color, GoatMotion.ForeNear, time)
        }
    }
}

private fun DrawScope.drawLeg(paths: Map<GoatPart, Path>, color: Color, leg: GoatMotion.Leg, time: Double) {
    val phase = GoatMotion.phase(time, leg.offset)
    val joint = Offset(leg.jointX, leg.jointY)
    rotate(GoatMotion.legAngle(leg, phase).toFloat(), pivot = joint) {
        scale(1f, GoatMotion.legScale(phase).toFloat(), pivot = joint) {
            drawPath(paths.getValue(leg.part), color)
        }
    }
}

/** Strichlinie bei y = 1018, läuft nach links; an den Enden ausgeblendet. */
private fun DrawScope.drawGround(color: Color, jump: Double) {
    val brush = Brush.horizontalGradient(
        colorStops = arrayOf(
            0f to color.copy(alpha = 0f),
            0.22f to color.copy(alpha = 0.38f),
            0.78f to color.copy(alpha = 0.38f),
            1f to color.copy(alpha = 0f),
        ),
        startX = -60f,
        endX = 1220f,
    )
    drawLine(
        brush = brush,
        start = Offset(-60f, 1018f),
        end = Offset(1220f, 1018f),
        strokeWidth = 14f,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(70f, 46f), (232 * jump).toFloat()),
    )
}

private val DustRadii = floatArrayOf(26f, 20f, 16f)
private const val DUST_PERIOD = GoatMotion.JUMP_SECONDS * 1.5

/** Drei Staubwolken am hinteren Huf, je ein Drittel Takt versetzt. */
private fun DrawScope.drawDust(color: Color, time: Double) {
    DustRadii.forEachIndexed { index, radius ->
        val raw = time / DUST_PERIOD + index / 3.0
        val p = raw - kotlin.math.floor(raw)
        val travel = GoatEase.EaseOut.transform(p.toFloat())
        val growth = 0.3f + (1.4f - 0.3f) * travel
        // Deckkraft: 0 → 0,28 (bis 15 %), dann → 0; je Stück ease-out.
        val alpha = if (p < 0.15) {
            0.28f * GoatEase.EaseOut.transform((p / 0.15).toFloat())
        } else {
            0.28f * (1f - GoatEase.EaseOut.transform(((p - 0.15) / 0.85).toFloat()))
        }
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius * growth,
            center = Offset(30f - 200f * travel, 990f - 46f * travel),
        )
    }
}

// MARK: - Bewegung reduziert

/** Ziege steht (Logo-Stellung), nur die Deckkraft pulsiert 1 → 0,45 in 1,2 s, hin und her. */
@Composable
private fun GoatReducedMotion(paths: Map<GoatPart, Path>, color: Color, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "goat-reduced")
    val alpha = transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = GoatEase.EaseInOut), RepeatMode.Reverse),
        label = "alpha",
    )
    Canvas(modifier = modifier.alpha(alpha.value)) {
        val unit = size.width / GoatGeometry.VIEW_BOX_WIDTH
        scale(unit, pivot = Offset.Zero) {
            translate(-GoatGeometry.VIEW_BOX_X, -GoatGeometry.VIEW_BOX_Y) {
                GoatPart.entries.forEach { drawPath(paths.getValue(it), color) }
            }
        }
    }
}
