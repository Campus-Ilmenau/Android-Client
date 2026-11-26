package de.kaigothe.campusilmenau.ui.bibliothek.ausweis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.domain.model.library.ausweis.Code39

/**
 * Zeichnet den Code 39 der [nummer]: schwarze Striche mit ganzzahligen Pixelbreiten auf der Fläche, zentriert. Die Farbe ist
 * fest Schwarz, der Hintergrund gehört dem Aufrufer (immer Weiß, auch im Dark Mode, damit Scanner ihn lesen).
 * Ist die Nummer nicht darstellbar, bleibt die Fläche leer.
 */
@Composable
fun Code39View(nummer: String, modifier: Modifier = Modifier, height: Dp = 84.dp) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(height)) {
        val density = LocalDensity.current
        val availablePx = with(density) { maxWidth.roundToPx() }
        val layout = remember(nummer, availablePx, density.density) { Code39.layout(nummer, availablePx, density.density) }
        if (layout != null) {
            Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
                val left = ((size.width - layout.width) / 2f).toInt().toFloat()
                for (bar in layout.bars) {
                    // Kein Anti-Aliasing nötig: Rechtecke liegen auf ganzen Pixeln.
                    drawRect(Color.Black, topLeft = Offset(left + bar.x, 0f), size = Size(bar.width.toFloat(), size.height))
                }
            }
        }
    }
}
