package de.kaigothe.campusilmenau.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.ui.theme.CampusTheme

/**
 * Ladebereich für Ansichten ohne Daten: Ziege (96 dp) mittig im freien Bereich, darunter der Text. Füllt den
 * verfügbaren Platz, Kopf, Leisten und Filter bleiben darüber stehen. Das Zeitverhalten (erst nach 0,4 s sichtbar,
 * Hinweis ab 8 s) bestimmt das [LoadingGate].
 */
@Composable
fun GoatLoadingBlock(message: String, gate: LoadingGate, modifier: Modifier = Modifier) {
    val visible by gate.isVisible.collectAsStateWithLifecycle()
    val slow by gate.isSlow.collectAsStateWithLifecycle()
    GoatLoadingBlock(
        message = message,
        isVisible = visible,
        isSlow = slow,
        fadeMillis = gate.timing.fade.inWholeMilliseconds.toInt(),
        modifier = modifier,
    )
}

/** Zustandslose Variante für Vorschauen und Tests. */
@Composable
fun GoatLoadingBlock(
    message: String,
    isVisible: Boolean,
    isSlow: Boolean,
    modifier: Modifier = Modifier,
    fadeMillis: Int = 200,
) {
    val alpha by animateFloatAsState(if (isVisible) 1f else 0f, tween(fadeMillis), label = "loadingAlpha")
    val slowAlpha by animateFloatAsState(if (isSlow) 1f else 0f, tween(fadeMillis), label = "slowAlpha")
    val colors = CampusTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, bottom = 40.dp)
            .graphicsLayer { this.alpha = alpha }
            // Ein Element: Der Text ist das Label, die Ziege selbst ist für TalkBack unsichtbar. Solange sie nicht
            // zu sehen ist, gibt es auch kein Label.
            .semantics(mergeDescendants = true) { if (!isVisible) hideFromAccessibility() },
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GoatLoader(size = 96.dp)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (isSlow || slowAlpha > 0f) {
            Text(
                text = stringResource(R.string.laedt_lange),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { this.alpha = slowAlpha }.padding(top = 0.dp),
            )
        }
    }
}
