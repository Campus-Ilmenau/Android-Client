package de.kaigothe.campusilmenau.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.ui.theme.CampusTheme

/**
 * Steht ein Detail neben seiner Liste? Dann zeigt das Detail keine Zurück-Taste (die Liste ist ja zu sehen) und der
 * Tipp auf einen anderen Eintrag ersetzt das Detail. Die Shell setzt den Wert; ohne Shell (Vorschau) gilt „nein“.
 */
val LocalSplitView = staticCompositionLocalOf { false }

/** Was rechts steht, solange nichts gewählt ist: die Ziege und eine Aufforderung. */
@Composable
fun DetailPlaceholder(text: String, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(R.drawable.ic_symbol_goat), contentDescription = null, tint = colors.separator, modifier = Modifier.size(72.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
