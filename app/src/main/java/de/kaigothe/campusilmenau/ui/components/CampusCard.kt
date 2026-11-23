package de.kaigothe.campusilmenau.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.ui.theme.CardShape

/**
 * Fläche für zusammengehörige Inhalte: `surface` (oder eine andere Fläche über [containerColor]), Ecken 16,
 * Innenabstand 14/16. Karten liegen flach, ohne Schatten. Mit [onClick] ist die ganze Karte tippbar.
 */
@Composable
fun CampusCard(
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 14.dp,
    horizontalPadding: Dp = 16.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickable = if (onClick != null) Modifier.clip(CardShape).clickable(role = Role.Button, onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, CardShape)
            .then(clickable)
            .padding(vertical = verticalPadding, horizontal = horizontalPadding),
        content = content,
    )
}
