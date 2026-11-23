package de.kaigothe.campusilmenau.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import de.kaigothe.campusilmenau.ui.theme.CampusTheme

/**
 * Abendsonne: Kreis im senkrechten Verlauf `brandOrange` → `brandYellow`. Erscheint abends (Club, Start im
 * Dark Mode) hinter dem Inhalt.
 */
@Composable
fun EveningSun(size: Dp, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .background(Brush.verticalGradient(listOf(colors.brandOrange, colors.brandYellow)), CircleShape),
    )
}
