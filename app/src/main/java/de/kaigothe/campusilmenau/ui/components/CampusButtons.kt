package de.kaigothe.campusilmenau.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.ui.theme.ButtonShape
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.SegmentShape

/**
 * Gemeinsame Gestalt der Buttons: Höhe 50, Ecken 12, Semibold 16.
 * [compact] ist die kleine Variante in Karten: 36 dp hoch, Ecken 10, Semibold 14, so breit wie der Titel.
 * Die Tippfläche bleibt dabei 48 dp hoch. Ein Button mit [isLoading] zeigt einen Ladekreis und reagiert nicht.
 * Der Text bleibt einzeilig, damit er auf schmalen Geräten nicht Zeichen für Zeichen umbricht.
 */
@Composable
private fun DesignButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    container: Color,
    content: Color,
    icon: ImageVector?,
    trailingIcon: ImageVector?,
    isLoading: Boolean,
    compact: Boolean,
    enabled: Boolean,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = if (compact) 14.sp else 16.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Surface(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = if (compact) SegmentShape else ButtonShape,
        color = container,
        contentColor = content,
        modifier = modifier
            .then(if (compact) Modifier.minimumInteractiveComponentSize() else Modifier.fillMaxWidth())
            .alpha(if (enabled) 1f else 0.4f),
    ) {
        Row(
            modifier = if (compact) {
                Modifier.heightIn(min = 36.dp).padding(horizontal = 14.dp)
            } else {
                Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(horizontal = 16.dp)
            },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = content)
            } else if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(text = text, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (trailingIcon != null) {
                Icon(imageVector = trailingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Hauptbutton, neutral (`buttonPrimary`). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = CampusTheme.colors
    DesignButton(
        text, onClick, modifier, colors.buttonPrimary, colors.onButtonPrimary,
        icon, trailingIcon, isLoading, compact = false, enabled = enabled,
    )
}

/** Nebenbutton, neutral (`buttonSecondary`). [compact] ist die kleine Variante für Karten. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    compact: Boolean = false,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = CampusTheme.colors
    DesignButton(
        text, onClick, modifier, colors.buttonSecondary, colors.onButtonSecondary,
        icon, trailingIcon, isLoading, compact, enabled,
    )
}

/** Nur für kritische Aktionen (Löschen, Stornieren). */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = CampusTheme.colors
    DesignButton(
        text, onClick, modifier, colors.buttonDestructive, colors.onButtonDestructive,
        icon, trailingIcon = null, isLoading = false, compact = false, enabled = enabled,
    )
}

/** Zurückhaltende Variante für kritische Aktionen ohne Rückfrage (Abmelden): `dangerSoft` auf `danger`. */
@Composable
fun SoftDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = CampusTheme.colors
    DesignButton(
        text, onClick, modifier, colors.dangerSoft, colors.danger,
        icon, trailingIcon = null, isLoading = false, compact = false, enabled = enabled,
    )
}
