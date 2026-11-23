package de.kaigothe.campusilmenau.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing

/**
 * Geometrie und Bewegung der rennenden Ziege. Reine Mathematik, ohne Compose-UI, damit sie sich testen lässt.
 * Koordinaten sind die des SVG `Ziege rennt.svg` (`viewBox -80 -40 1320 1100`), Winkel in Grad.
 */
internal object GoatGeometry {
    const val VIEW_BOX_X = -80f
    const val VIEW_BOX_Y = -40f
    const val VIEW_BOX_WIDTH = 1320f
    const val VIEW_BOX_HEIGHT = 1100f

    /** Höhe im Verhältnis zur Breite. */
    const val ASPECT = VIEW_BOX_HEIGHT / VIEW_BOX_WIDTH

    const val BODY_CENTER_X = 450f
    const val BODY_CENTER_Y = 620f
    const val HEAD_JOINT_X = 768.9f
    const val HEAD_JOINT_Y = 420f
}

/** Die sechs Teile der Ziege. Pfaddaten unverändert aus `Ziege rennt.svg`. */
internal enum class GoatPart(val pathData: String) {
    HindFar("M104,790 L163.5,996 L224,996 L180,790 A38,38 0 0 0 104,790 Z"),
    ForeFar("M595.9,753.7 L480,1000 L541.7,999.9 L660.1,786.3 A36,36 0 0 0 595.9,753.7 Z"),
    Body(
        "M171.612,741.601 L170.536,741.609 C131.224,742.198 113,756 110,775 A55,55 0 0 1 0,775 L0,741.355 " +
            "L1.718,741.359 L2.519,741.027 C48.026,721.783 79.981,676.638 80.030,624.000 L80.274,624 " +
            "L80.274,487.2 L539.654,487.2 L539.654,486.954 L541.627,486.938 C610.628,485.816 664.5,446 666.219,408.283 " +
            "L871.595,408.283 C871.595,458.092 860.683,505.344 841.126,547.765 L842,690 A44,44 0 0 1 754,690 " +
            "L753.771,662.597 C696.611,711.005 622.907,740.376 542.379,740.958 L171.612,741.601 Z " +
            "M206.960354,360 C216.301884,360 225.407266,361.014198 234.173036,362.938812 " +
            "C201.280723,372.246418 177.174691,402.568236 177.174691,438.53772 " +
            "C177.174691,456.916579 183.468222,473.820961 194.011438,487.200704 L80.274179,487.199154 " +
            "L80.274179,487.07772 C80.274179,416.894633 136.993511,360 206.960354,360 Z",
    ),
    Head(
        "M871.595,430 L871.595,359.878 C871.595,331.736 880.715,305.731 896.151,284.674 L1026.633,360.241 " +
            "L1159.760,282.599 L884.096,53.186 C847.723,20.134 799.466,0 746.522,0 " +
            "C636.899,0 547.375,86.318 541.814,194.891 C546.989,129.472 601.553,78 668.102,78 " +
            "C713.639,78 753.564,102.099 775.893,138.277 C702.777,264.281 666.219,354.283 666.219,408.283 " +
            "L666.219,430 Z",
    ),
    HindNear("M0,775 L0.12,996 L59.94,996 L110,775 A55,55 0 0 0 0,775 Z"),
    ForeNear("M754,690 L781.18,996 L841.12,995.88 L842,690 A44,44 0 0 0 754,690 Z"),
}

/** CSS-Kurven aus dem Mockup (`ease-in`, `ease-out`, `ease-in-out`). */
internal object GoatEase {
    val Linear: Easing = LinearEasing
    val EaseIn: Easing = CubicBezierEasing(0.42f, 0f, 1f, 1f)
    val EaseOut: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
    val EaseInOut: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
}

/** Keyframe bei Phase [at]. [ease] gilt für das Stück bis zum nächsten Keyframe (wie `animation-timing-function`). */
internal class GoatKey(val at: Double, val value: Double, val ease: Easing = GoatEase.Linear)

internal fun interpolate(keys: List<GoatKey>, phase: Double): Double {
    var previous = keys.first()
    for (key in keys.drop(1)) {
        if (phase <= key.at) {
            val span = key.at - previous.at
            val t = if (span > 0) (phase - previous.at) / span else 1.0
            return previous.value + (key.value - previous.value) * previous.ease.transform(t.toFloat())
        }
        previous = key
    }
    return previous.value
}

internal object GoatMotion {
    /** Dauer eines Sprungs in Sekunden. */
    const val JUMP_SECONDS = 0.56
    private const val LEG_SQUASH = 0.84

    /** Winkel a → b → m (Grad) und Phasenversatz je Bein. [jointX]/[jointY] sind das Gelenk im Koordinatenraum. */
    class Leg(
        val part: GoatPart,
        val jointX: Float,
        val jointY: Float,
        val a: Double,
        val b: Double,
        val m: Double,
        val offset: Double,
    )

    val HindFar = Leg(GoatPart.HindFar, 142f, 790f, a = -36.0, b = 16.0, m = -14.0, offset = 0.4)
    val ForeFar = Leg(GoatPart.ForeFar, 628f, 770f, a = -42.0, b = 10.0, m = -22.0, offset = 0.9)
    val HindNear = Leg(GoatPart.HindNear, 55f, 775f, a = -26.0, b = 28.0, m = 2.0, offset = 0.5)
    val ForeNear = Leg(GoatPart.ForeNear, 798f, 690f, a = -32.0, b = 22.0, m = -8.0, offset = 0.0)

    /** Phase 0…1 innerhalb eines Sprungs. */
    fun phase(timeSeconds: Double, offset: Double = 0.0): Double {
        val raw = timeSeconds / JUMP_SECONDS + offset
        return raw - kotlin.math.floor(raw)
    }

    fun legAngle(leg: Leg, phase: Double): Double = interpolate(
        listOf(
            GoatKey(0.0, leg.a),
            GoatKey(0.55, leg.b, GoatEase.EaseIn),
            GoatKey(0.78, leg.m, GoatEase.EaseOut),
            GoatKey(1.0, leg.a),
        ),
        phase,
    )

    /** Das Bein wird beim Anziehen am Gelenk gestaucht. */
    fun legScale(phase: Double): Double = interpolate(
        listOf(
            GoatKey(0.0, 1.0),
            GoatKey(0.55, 1.0, GoatEase.EaseIn),
            GoatKey(0.78, LEG_SQUASH, GoatEase.EaseOut),
            GoatKey(1.0, 1.0),
        ),
        phase,
    )

    fun headAngle(phase: Double): Double = interpolate(
        listOf(
            GoatKey(0.0, 0.0, GoatEase.EaseInOut),
            GoatKey(0.3, 4.0, GoatEase.EaseInOut),
            GoatKey(0.55, -1.0, GoatEase.EaseInOut),
            GoatKey(0.8, 2.0, GoatEase.EaseInOut),
            GoatKey(1.0, 0.0),
        ),
        phase,
    )

    /** Hub in Einheiten des Koordinatenraums (negativ = nach oben). */
    fun bobLift(phase: Double): Double = interpolate(
        listOf(
            GoatKey(0.0, 0.0, GoatEase.EaseInOut),
            GoatKey(0.3, -24.0, GoatEase.EaseInOut),
            GoatKey(0.55, 0.0, GoatEase.EaseInOut),
            GoatKey(0.8, -8.0, GoatEase.EaseInOut),
            GoatKey(1.0, 0.0),
        ),
        phase,
    )

    fun bobAngle(phase: Double): Double = interpolate(
        listOf(
            GoatKey(0.0, 0.0, GoatEase.EaseInOut),
            GoatKey(0.3, -2.0, GoatEase.EaseInOut),
            GoatKey(0.55, 0.5, GoatEase.EaseInOut),
            GoatKey(0.8, 1.5, GoatEase.EaseInOut),
            GoatKey(1.0, 0.0),
        ),
        phase,
    )
}
