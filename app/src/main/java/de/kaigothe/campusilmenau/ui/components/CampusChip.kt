package de.kaigothe.campusilmenau.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.ChipShape

/** Varianten des Chips. Grün und Blau sind Flächen mit eigener Schriftfarbe (`onGreen`, `onBlue`). */
enum class ChipVariant { Green, Blue, Warning, AccentSoft, DangerSoft, Neutral }

/** Kapsel mit kurzer Aussage, 12 sp Semibold. Optional mit kleinem Symbol vor dem Text. */
@Composable
fun CampusChip(
    text: String,
    modifier: Modifier = Modifier,
    variant: ChipVariant = ChipVariant.Neutral,
    icon: ImageVector? = null,
) {
    val colors = CampusTheme.colors
    val (container, content) = when (variant) {
        ChipVariant.Green -> colors.green to colors.onGreen
        ChipVariant.Blue -> colors.blue to colors.onBlue
        ChipVariant.Warning -> colors.warningFill to colors.onWarningFill
        ChipVariant.AccentSoft -> colors.accentSoft to colors.accent
        ChipVariant.DangerSoft -> colors.dangerSoft to colors.danger
        ChipVariant.Neutral -> MaterialTheme.colorScheme.surfaceVariant to colors.textSecondary
    }
    ChipContent(text, modifier, container, content, icon)
}

@Composable
private fun ChipContent(
    text: String,
    modifier: Modifier,
    container: Color,
    content: Color,
    icon: ImageVector?,
) {
    Row(
        modifier = modifier
            .background(container, ChipShape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
        }
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
