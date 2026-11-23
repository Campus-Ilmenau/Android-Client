package de.kaigothe.campusilmenau.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.ui.theme.CampusTheme

/**
 * Tab-Leiste im Stil des Designs: aktiver Tab in `accent`, sonst `textTertiary`, Hintergrund `surface`
 * leicht durchscheinend, oben eine Trennlinie. Sie liegt unter der Navigationsleiste des Systems (Edge-to-Edge).
 * Ein erneutes Tippen auf den gewählten Tab löst [onReselect] aus (zum Beispiel „nach oben scrollen“).
 */
@Composable
fun CampusTabBar(
    tabs: List<AppTab>,
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    onReselect: (AppTab) -> Unit = {},
) {
    val colors = CampusTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.94f)),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selected
                val tint = if (isSelected) colors.accent else colors.textTertiary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { if (isSelected) onReselect(tab) else onSelect(tab) },
                        )
                        .padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.height(26.dp).size(22.dp),
                    )
                    Text(
                        text = stringResource(tab.title),
                        color = tint,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
