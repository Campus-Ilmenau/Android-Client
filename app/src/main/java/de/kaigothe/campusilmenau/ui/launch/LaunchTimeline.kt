package de.kaigothe.campusilmenau.ui.launch

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Zeitachse und Geometrie der Start-Animation „Durch die Ziege“. Werte aus
 * `Design/Logo 2026/Launch/Launch-Animation.md`. Koordinaten der Ziege sind Pfad-Einheiten der Maske
 * (viewBox 996 × 1000, siehe [GoatMask]). Reine Mathematik, ohne Compose, damit sie sich testen lässt.
 */
internal object LaunchTimeline {
    /** Gesamtdauer in Millisekunden. */
    const val DURATION_MILLIS = 1_100

    /**
     * Größe einer Pfad-Einheit der Maske in dp im ersten Frame. Sie ergibt sich aus dem Splash-Symbol: Das
     * System zeigt es in 288 dp, das Symbol ist auf einem 1024er-Raster mit Skalierung 0,47 gezeichnet
     * (`drawable/launch_goat.xml`). So sitzt die Ziege im ersten Frame genau auf dem Splash des Systems.
     */
    const val UNIT_DP = 0.47f * 288f / 1024f

    /**
     * Dieser Punkt der Maske liegt im ersten Frame in der Bildschirmmitte: der Mittelpunkt des kleinsten Kreises,
     * der die ganze Ziege umschließt. Kreisförmig zugeschnittene Splash-Symbole schneiden die Ziege so nicht an.
     */
    const val START_X = 420f
    const val START_Y = 500f

    /** Zoom-Anker: dickste Stelle der Silhouette, Übergang von Brust zu Hals. */
    const val ANCHOR_X = 602f
    const val ANCHOR_Y = 608f

    /** Radius des Innenkreises um den Anker. Ab dort ist der Bildschirm sicher Loch. */
    const val ANCHOR_INNER_RADIUS = 129f

    const val ANTICIPATION_SCALE = 0.88f
    const val MINIMUM_END_SCALE = 30f

    /** Abendsonne im Dark Mode (wie App-Icon Dark), in Pfad-Einheiten. */
    const val SUN_X = 527f
    const val SUN_Y = 443f
    const val SUN_RADIUS = 587f
    const val SUN_HORIZON = 996f

    val RING_RADII = floatArrayOf(900f, 1300f)
    const val RING_LINE_WIDTH_DP = 3f

    /** Kantenlicht: konstante Breite und Weichzeichnung, unabhängig vom Zoom. */
    const val EDGE_STROKE_DP = 10f
    const val EDGE_BLUR_DP = 4f

    /** Skalierung der Startansicht von 1,08 auf 1,0, solange die Wand wegfliegt. */
    fun contentScale(progress: Float): Float =
        1.08f + (1f - 1.08f) * Ease.outCubic(Ease.clamp(progress, 0.32f, 1f))
}

/** Alle Größen eines Frames für eine gegebene Bildschirmgröße. */
internal class LaunchFrame(progress: Float, width: Float, height: Float, unit: Float) {
    /** Skalierung der Ziege relativ zur Startgröße. */
    val scale: Float

    /** Bildschirmposition des Zoom-Ankers. */
    val centerX: Float
    val centerY: Float

    /** Deckkraft der Ziegenfüllung: 1 beim Start, 0 sobald die Ziege zum Loch geworden ist. */
    val coverOpacity: Float

    /** Deckkraft des Kantenlichts: ab 1,3× ein, ab 8–16× aus. */
    val edgeOpacity: Float

    /** Skalierung der Ringe. Sie wachsen langsamer als das Loch (Parallaxe). */
    val ringScale: Float

    /** 0 → 1 in der Haltephase: Die Vollfarbe des Splash geht in den Wandverlauf über. */
    val wallBlend: Float

    /** Pro Pfad-Einheit in Pixeln, mit Skalierung. */
    val unitScaled: Float

    /** Linke obere Ecke der Maske in Bildschirmkoordinaten. */
    val originX: Float
    val originY: Float

    /** Skalierung am Ende, bei der der Innenkreis um den Anker die am weitesten entfernte Ecke abdeckt. */
    val endScale: Float

    init {
        val screenCenterX = width / 2f
        val screenCenterY = height / 2f
        // Der Anker liegt im ersten Frame so weit von der Bildschirmmitte, wie er in der Maske vom Startpunkt liegt.
        val startAnchorX = screenCenterX + (LaunchTimeline.ANCHOR_X - LaunchTimeline.START_X) * unit
        val startAnchorY = screenCenterY + (LaunchTimeline.ANCHOR_Y - LaunchTimeline.START_Y) * unit

        val farthestCorner = hypot(width / 2f, height / 2f)
        endScale = max(
            LaunchTimeline.MINIMUM_END_SCALE,
            1.1f * farthestCorner / (LaunchTimeline.ANCHOR_INNER_RADIUS * unit),
        )

        val anticipation = Ease.inOut(Ease.clamp(progress, 0.10f, 0.32f))
        val fly = Ease.inCubic(Ease.clamp(progress, 0.32f, 1f))
        val anticipationFactor = 1f + (LaunchTimeline.ANTICIPATION_SCALE - 1f) * anticipation
        scale = if (fly > 0f) {
            anticipationFactor * exp(ln(endScale / LaunchTimeline.ANTICIPATION_SCALE) * fly)
        } else {
            anticipationFactor
        }

        centerX = startAnchorX + (screenCenterX - startAnchorX) * fly
        centerY = startAnchorY + (screenCenterY - startAnchorY) * fly
        coverOpacity = 1f - Ease.clamp(progress, 0.26f, 0.40f)
        edgeOpacity = Ease.clamp(scale, 1.3f, 3f) * (1f - Ease.clamp(scale, 8f, 16f))
        ringScale = scale.pow(0.6f)
        wallBlend = Ease.clamp(progress, 0f, 0.10f)
        unitScaled = unit * scale
        originX = centerX - LaunchTimeline.ANCHOR_X * unitScaled
        originY = centerY - LaunchTimeline.ANCHOR_Y * unitScaled
    }
}

internal object Ease {
    /** Anteil von [t] zwischen [a] und [b], begrenzt auf 0…1. */
    fun clamp(t: Float, a: Float, b: Float): Float = min(1f, max(0f, (t - a) / (b - a)))

    fun inOut(t: Float): Float = if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f

    fun inCubic(t: Float): Float = t * t * t

    fun outCubic(t: Float): Float = 1f - (1f - t).pow(3)
}

/**
 * Silhouette der Ziege als Maske: ein geschlossener Pfad, viewBox `0 0 996 1000`
 * (`Design/Logo 2026/Launch/Goat Mask.svg`). Sie wird aus der Wand gestanzt.
 */
internal const val GoatMask =
    "M172.422,741.600 L179.898,778.452 L204.616,900.304 L224.000,996.000 L163.525,996.000 L97.442,791.714 " +
        "L59.937,996 L0.122,996 L0,741.355 L2.519,741.027 C48.026,721.783 79.981,676.638 80.030,624.000 " +
        "L80.274,624 L80.274,487.078 C80.274,416.895 136.994,360 206.960,360 C216.302,360 225.407,361.014 " +
        "234.173,362.939 C201.281,372.246 177.175,402.568 177.175,438.538 C177.175,456.917 183.468,473.821 " +
        "194.011,487.201 L541.627,486.938 C610.628,485.816 666.219,429.359 666.219,359.878 " +
        "C666.219,331.736 657.099,305.731 641.663,284.674 L511.181,360.241 L378.054,282.599 L653.718,53.186 " +
        "C690.091,20.134 738.348,0 791.292,0 C900.915,0 990.439,86.318 996,194.891 " +
        "C990.825,129.472 936.261,78 869.712,78 C824.175,78 784.250,102.099 761.921,138.277 " +
        "C835.037,264.281 871.595,354.283 871.595,408.283 C871.595,458.092 860.683,505.344 841.126,547.765 " +
        "L841.123,995.875 L781.183,996 L753.771,662.597 C733.321,679.915 710.753,694.798 686.514,706.796 " +
        "L541.678,999.875 L480.000,1000.000 L587.826,737.524 C572.973,739.679 557.801,740.847 542.379,740.958 Z"
