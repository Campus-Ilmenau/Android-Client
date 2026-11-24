package de.kaigothe.campusilmenau.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.ui.theme.CampusTheme

private val TrackWidth = 51.dp
private val TrackHeight = 31.dp
private val ThumbSize = 27.dp
private val ThumbInset = 2.dp

/**
 * Neutraler Schalter für Einstellungen: an = `buttonPrimary`, aus = `surfaceSecondary`. Kein Rot, kein Grün.
 * Der Schaltknopf bleibt auch im Dark Mode sichtbar (an: Farbe der Fläche, aus: Weiß).
 * Der Schalter allein ist reine Anzeige. Bedient wird er über [NeutralSwitchRow], damit die ganze Zeile tippbar ist.
 */
@Composable
fun NeutralSwitchVisual(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val track by animateColorAsState(
        targetValue = if (checked) colors.buttonPrimary else colors.surfaceSecondary,
        animationSpec = tween(200),
        label = "track",
    )
    val thumb by animateColorAsState(
        targetValue = if (checked) colors.surface else colors.onBlue,
        animationSpec = tween(200),
        label = "thumb",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - ThumbSize - ThumbInset else ThumbInset,
        animationSpec = tween(200),
        label = "thumbOffset",
    )
    Box(modifier = modifier.size(TrackWidth, TrackHeight).background(track, CircleShape)) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(x = thumbOffset.roundToPx(), y = 0) }
                .size(ThumbSize)
                .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                .background(thumb, CircleShape),
        )
    }
}

/**
 * Zeile mit Beschriftung links und Schalter rechts. Die ganze Zeile ist tippbar (mindestens 52 dp hoch),
 * TalkBack liest sie als Schalter mit Beschriftung.
 */
@Composable
fun NeutralSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        NeutralSwitchVisual(checked = checked)
    }
}
