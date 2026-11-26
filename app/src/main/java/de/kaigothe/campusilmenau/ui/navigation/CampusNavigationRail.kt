package de.kaigothe.campusilmenau.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.ui.adaptive.WindowWidthClass
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.PlusJakartaSans

private val RailWidthMedium = 88.dp
private val SidebarWidthExpanded = 248.dp

/**
 * Navigation am Rand für Fenster ab 600 dp Breite. Medium: schmale Rail mit Symbol über der Beschriftung. Expanded:
 * Seitenleiste mit Kopf (Ziege und „Campus Ilmenau“) und Zeilen. Beide haben dieselben Ziele wie die Tab-Leiste und
 * dazu „Mehr“, das es auf dem Handy nur als Personen-Button auf „Heute“ gibt. „Mehr“ steht unten, getrennt von den
 * Tabs (iPad: eigener Abschnitt „App“).
 *
 * Aktiver Eintrag in `accent`, sonst `textTertiary`; Hintergrund `surface`, rechts eine Trennlinie. Die Rail liegt
 * innerhalb der Systemleisten (Statusleiste, Navigationsleiste, Display-Aussparung).
 */
@Composable
fun CampusNavigationRail(
    tabs: List<AppTab>,
    selected: AppTab,
    mehrSelected: Boolean,
    onSelect: (AppTab) -> Unit,
    onReselect: (AppTab) -> Unit,
    onOpenMehr: () -> Unit,
    widthClass: WindowWidthClass,
    modifier: Modifier = Modifier,
) {
    val colors = CampusTheme.colors
    val expanded = widthClass == WindowWidthClass.Expanded
    val width: Dp = if (expanded) SidebarWidthExpanded else RailWidthMedium
    Row(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(width)
                .fillMaxHeight()
                .background(colors.surface)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical))
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                .padding(horizontal = if (expanded) 12.dp else 4.dp, vertical = 12.dp),
            horizontalAlignment = if (expanded) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (expanded) SidebarHeader()
            tabs.forEach { tab ->
                val isSelected = tab == selected && !mehrSelected
                RailItem(
                    icon = tab.icon,
                    label = stringResource(tab.title),
                    selected = isSelected,
                    expanded = expanded,
                    onClick = { if (tab == selected && !mehrSelected) onReselect(tab) else onSelect(tab) },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            RailItem(
                icon = Icons.Outlined.AccountCircle,
                label = stringResource(R.string.mehr_titel),
                selected = mehrSelected,
                expanded = expanded,
                onClick = onOpenMehr,
            )
        }
        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(colors.separator))
    }
}

/** Kopf der Seitenleiste: Ziege (`brandOrange`) links neben „Campus Ilmenau“. Nur Dekor, der Schriftzug trägt den Namen. */
@Composable
private fun SidebarHeader() {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_symbol_goat), contentDescription = null, tint = colors.brandOrange, modifier = Modifier.size(22.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RailItem(icon: ImageVector, label: String, selected: Boolean, expanded: Boolean, onClick: () -> Unit) {
    val colors = CampusTheme.colors
    val tint = if (selected) colors.accent else colors.textTertiary
    val shape = RoundedCornerShape(14.dp)
    val background = if (selected) colors.accentSoft else colors.surface
    val select = Modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick)
    if (expanded) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(background)
                .then(select)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Text(
                text = label,
                color = if (selected) colors.accent else colors.textPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(shape)
                .background(background)
                .then(select)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Text(
                text = label,
                color = tint,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
