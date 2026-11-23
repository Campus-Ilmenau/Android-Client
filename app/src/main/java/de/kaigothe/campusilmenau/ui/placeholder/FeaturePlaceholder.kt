package de.kaigothe.campusilmenau.ui.placeholder

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing

/**
 * Platzhalter für Ansichten, die erst mit ihrem Meilenstein entstehen. Jede Ansicht ersetzt ihren Platzhalter
 * (Heute in M4, Mensa in M3, Thoska in M6, Bibliothek in M7, Events in M5, Mehr in M8).
 */
@Composable
fun FeaturePlaceholder(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = Spacing.ScreenEdge, vertical = Spacing.Lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.Md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(content = actions)
        }
        Text(
            text = stringResource(R.string.platzhalter_text),
            style = MaterialTheme.typography.bodyLarge,
            color = CampusTheme.colors.textSecondary,
        )
    }
}

/** Der Personen-Button oben rechts auf „Heute“, öffnet „Mehr“. */
@Composable
fun MehrButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.PersonOutline,
            contentDescription = stringResource(R.string.mehr_oeffnen),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}
