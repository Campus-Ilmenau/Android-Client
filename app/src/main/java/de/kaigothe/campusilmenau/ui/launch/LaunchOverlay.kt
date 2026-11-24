package de.kaigothe.campusilmenau.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import de.kaigothe.campusilmenau.ui.components.rememberReduceMotion
import kotlinx.coroutines.delay

/**
 * Legt die Start-Animation „Durch die Ziege“ über den [content]. Solange [playing] gilt, liegt die Wand mit der
 * Ziege als Loch darüber und der Inhalt skaliert von 1,08 auf 1,0. Am Ende ruft [onFinished] auf, und die Wand
 * ist vollständig entfernt (sie blockiert nichts mehr).
 *
 * Der erste Frame entspricht dem Splash des Systems, die Animation läuft also ohne Sprung weiter. Bei „Bewegung
 * reduzieren“ steht der erste Frame kurz und wird dann überblendet.
 */
@Composable
fun LaunchOverlay(
    playing: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val progress = remember { Animatable(0f) }
    val wallAlpha = remember { Animatable(1f) }

    LaunchedEffect(playing, reduceMotion) {
        if (!playing) return@LaunchedEffect
        // Erst nach dem ersten gezeichneten Bild starten: Das Aufbauen der Ansicht darf die Animation nicht verschlucken.
        withFrameNanos { }
        if (reduceMotion) {
            delay(150)
            wallAlpha.animateTo(0f, tween(250))
        } else {
            progress.animateTo(1f, tween(LaunchTimeline.DURATION_MILLIS, easing = LinearEasing))
        }
        onFinished()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (playing && !reduceMotion) {
                        val scale = LaunchTimeline.contentScale(progress.value)
                        scaleX = scale
                        scaleY = scale
                    }
                },
        ) {
            content()
        }
        if (playing) {
            LaunchWall(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(wallAlpha.value)
                    // Die Wand schluckt Berührungen, bis sie weg ist.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown().consume()
                        }
                    },
            )
        }
    }
}
