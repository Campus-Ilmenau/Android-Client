package de.kaigothe.campusilmenau.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.ui.theme.CampusTheme

/**
 * Karte für Zustände ohne Inhalt (Fehler, leerer Filter): fette Überschrift, Text, optional eine Taste.
 * Pendant zur `messageCard` der iOS-App.
 */
@Composable
fun MessageCard(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    buttonText: String? = null,
    onButtonClick: () -> Unit = {},
) {
    CampusCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CampusTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = CampusTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (buttonText != null) {
                SecondaryButton(text = buttonText, onClick = onButtonClick, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}
