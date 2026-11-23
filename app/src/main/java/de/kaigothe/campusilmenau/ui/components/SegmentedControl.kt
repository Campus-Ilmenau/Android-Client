package de.kaigothe.campusilmenau.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.SegmentShape

@Immutable
data class SegmentOption<T>(val value: T, val title: String)

/**
 * Segment-Control im Stil des Designs: Hintergrund `surfaceSecondary`, Ecken 10, die Auswahl liegt auf `surface`.
 * [singleLine]: Die Titel bleiben einzeilig (für schmale Popover), sonst dürfen sie umbrechen.
 */
@Composable
fun <T> SegmentedControl(
    options: List<SegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
) {
    val colors = CampusTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceSecondary, SegmentShape)
            .padding(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            val container by animateColorAsState(
                targetValue = if (isSelected) colors.surface else colors.surfaceSecondary,
                animationSpec = tween(200),
                label = "segment",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(container)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option.value) })
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
