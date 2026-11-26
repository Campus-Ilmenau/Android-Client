package de.kaigothe.campusilmenau.ui.mehr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing

/** Ein Abschnitt der Listen unter „Mehr“: Kopf in Großbuchstaben, Zeilen auf einer Fläche, optional ein Fuß. */
@Composable
fun MehrAbschnitt(
    titel: String?,
    modifier: Modifier = Modifier,
    fuss: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CampusTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        if (titel != null) {
            Text(
                text = titel.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp).semantics { heading() },
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(horizontal = 16.dp),
            content = content,
        )
        if (fuss != null) {
            Text(
                text = fuss,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
            )
        }
    }
}

@Composable
fun MehrTrenner() {
    HorizontalDivider(color = CampusTheme.colors.separator)
}

/** Zeile mit Beschriftung links und Wert rechts, nicht tippbar (zum Beispiel in „Gespeicherte Daten“). */
@Composable
fun MehrWertZeile(titel: String, wert: String) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.MinTouchTarget)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(titel, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
        Text(wert, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

/** Zeile, die ein Menü mit Auswahlmöglichkeiten öffnet. Rechts steht der aktuelle Wert. */
@Composable
fun <T> MehrAuswahlZeile(
    titel: String,
    wert: String,
    optionen: List<T>,
    gewaehlt: T,
    beschriftung: @Composable (T) -> String,
    onWahl: (T) -> Unit,
) {
    val colors = CampusTheme.colors
    var offen by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.MinTouchTarget)
                .clickable(role = Role.DropdownList) { offen = true }
                .padding(vertical = 4.dp)
                .semantics(mergeDescendants = true) { stateDescription = wert },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(titel, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            Text(
                wert,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 6.dp),
            )
            Icon(Icons.Outlined.UnfoldMore, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            optionen.forEach { option ->
                val text = beschriftung(option)
                DropdownMenuItem(
                    text = {
                        Text(text, fontWeight = if (option == gewaehlt) FontWeight.Bold else FontWeight.Normal)
                    },
                    onClick = {
                        offen = false
                        onWahl(option)
                    },
                    modifier = Modifier.semantics { if (option == gewaehlt) stateDescription = "✓" },
                )
            }
        }
    }
}

/** Zeile, die eine andere Ansicht öffnet (Pfeil rechts). */
@Composable
fun MehrLinkZeile(titel: String, onClick: () -> Unit, wert: String? = null, extern: Boolean = false) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(titel, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
        if (wert != null) Text(wert, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
        // Externe Seiten öffnen im Browser: anderes Symbol als der Pfeil für Ansichten der App.
        Icon(
            if (extern) Icons.AutoMirrored.Outlined.OpenInNew else Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Zeile mit Überschrift und Erklärung, für „Wird nicht gespeichert“. */
@Composable
fun MehrHinweisZeile(titel: String, text: String) {
    val colors = CampusTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titel, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
    }
}
