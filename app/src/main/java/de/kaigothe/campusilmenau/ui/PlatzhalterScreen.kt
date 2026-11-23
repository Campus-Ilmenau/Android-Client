package de.kaigothe.campusilmenau.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing

/**
 * Platzhalter, bis die Shell mit den Tabs steht (M2). Zeigt Titel, Text und die Flächenfarben,
 * damit sich Theme (Light/Dark) und Schrift prüfen lassen.
 */
@Composable
fun PlatzhalterScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(Spacing.ScreenEdge),
        verticalArrangement = Arrangement.spacedBy(Spacing.Md),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.platzhalter_text),
            style = MaterialTheme.typography.bodyLarge,
            color = CampusTheme.colors.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
            Swatch(color = CampusTheme.colors.blue, modifier = Modifier.weight(1f))
            Swatch(color = CampusTheme.colors.green, modifier = Modifier.weight(1f))
            Swatch(color = CampusTheme.colors.accent, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Swatch(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.MinTouchTarget)
            .background(color, MaterialTheme.shapes.medium),
    )
}

@Preview(name = "Hell", showBackground = true)
@Composable
private fun PlatzhalterScreenLightPreview() {
    CampusIlmenauTheme(darkTheme = false) { PlatzhalterScreen() }
}

@Preview(name = "Dunkel", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlatzhalterScreenDarkPreview() {
    CampusIlmenauTheme(darkTheme = true) { PlatzhalterScreen() }
}
