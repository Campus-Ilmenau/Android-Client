package de.kaigothe.campusilmenau.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.ui.theme.CampusTheme

/** Zurück-Pfeil in `accent` für Ansichten ohne eigenen Kopf (Pendant zum Zurück-Pfeil der Navigationsleiste in iOS). */
@Composable
fun BackBar(onBack: () -> Unit, modifier: Modifier = Modifier, description: String = stringResource(R.string.zurueck)) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = description,
                tint = CampusTheme.colors.accent,
            )
        }
    }
}
