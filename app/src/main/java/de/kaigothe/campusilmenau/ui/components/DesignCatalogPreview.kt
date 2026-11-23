package de.kaigothe.campusilmenau.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.ui.navigation.AppTab
import de.kaigothe.campusilmenau.ui.navigation.CampusTabBar
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing

// Katalog aller Bausteine in Light und Dark. Pendant zu `DesignFoundationPreview.swift`.
// Texte in Previews sind von der Regel „alles aus strings.xml“ ausgenommen.

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = CampusTheme.colors.textTertiary,
    )
}

@Composable
private fun ComponentsCatalog() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.ScreenEdge),
        verticalArrangement = Arrangement.spacedBy(Spacing.SectionGap),
    ) {
        Text("Bausteine", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)

        SectionTitle("Card")
        CampusCard {
            Text("Linsen-Dal mit Basmatireis", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("Rote Linsen, Kokosmilch, Tomate, frischer Koriander", color = CampusTheme.colors.textSecondary)
        }

        SectionTitle("Chip")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CampusChip("Vegan", variant = ChipVariant.Green)
            CampusChip("Regional", variant = ChipVariant.Blue)
            CampusChip("Vegetarisch")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CampusChip("Nur noch wenige", variant = ChipVariant.Warning)
            CampusChip("Tagesangebot", variant = ChipVariant.AccentSoft)
            CampusChip("Geschlossen", variant = ChipVariant.DangerSoft)
        }

        SectionTitle("Buttons")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Als Favorit merken", onClick = {})
            PrimaryButton("Thoska auslesen", onClick = {}, icon = Icons.Outlined.Nfc)
            PrimaryButton("Anmelden", onClick = {}, enabled = false)
            PrimaryButton("Wird geladen", onClick = {}, isLoading = true)
            SecondaryButton("Zum Kalender hinzufügen", onClick = {}, icon = Icons.Outlined.CalendarMonth)
            SecondaryButton("Verlängern", onClick = {}, compact = true)
            DestructiveButton("Löschen", onClick = {}, icon = Icons.Outlined.Delete)
            SoftDestructiveButton("Abmelden", onClick = {})
        }

        SectionTitle("Schalter")
        CampusCard(verticalPadding = 4.dp) {
            var first by remember { mutableStateOf(true) }
            var second by remember { mutableStateOf(false) }
            NeutralSwitchRow("Nur Bio", first, { first = it })
            HorizontalDivider(color = CampusTheme.colors.separator)
            NeutralSwitchRow("Ohne Gluten", second, { second = it })
        }

        SectionTitle("Segment")
        var range by remember { mutableIntStateOf(0) }
        SegmentedControl(
            options = listOf(SegmentOption(0, "Studierende"), SegmentOption(1, "Bedienstete"), SegmentOption(2, "Gäste")),
            selected = range,
            onSelect = { range = it },
        )

        SectionTitle("Toast")
        Toast(text = "Im Kalender eingetragen")
        Toast(text = "Von der Merkliste entfernt", actionLabel = "Widerrufen", onAction = {})

        SectionTitle("Abendsonne")
        EveningSun(size = 64.dp)
    }
}

@Composable
private fun BandsCatalog() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        DiagonalBands(style = BandsStyle.Header, height = 250.dp) {
            Text(
                "MENSA EHRENBERG · ESSEN 1 · HEUTE",
                style = MaterialTheme.typography.labelMedium,
                color = CampusTheme.colors.onBlue,
                modifier = Modifier.padding(start = 20.dp, bottom = 54.dp),
            )
        }
        DiagonalBands(style = BandsStyle.Event, height = 270.dp) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 140.dp, bottom = 28.dp)) {
                Text("STUDI-CLUB", style = MaterialTheme.typography.labelMedium, color = CampusTheme.colors.onBlue)
                Text("Halloween-Party", style = MaterialTheme.typography.displaySmall, color = CampusTheme.colors.onBlue)
            }
        }
        DiagonalBands(
            style = BandsStyle.Hero,
            height = 110.dp,
            modifier = Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(22.dp)),
        ) {
            Column(modifier = Modifier.padding(start = 18.dp, bottom = 18.dp)) {
                Text("JETZT GEÖFFNET · BIS 14:00", style = MaterialTheme.typography.labelMedium, color = CampusTheme.colors.onBlue)
                Text("Mensa Ehrenberg", style = MaterialTheme.typography.headlineSmall, color = CampusTheme.colors.onBlue)
            }
        }
    }
}

@Composable
private fun TabBarCatalog() {
    var selected by remember { mutableStateOf(AppTab.Heute) }
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tab: $selected", color = CampusTheme.colors.textSecondary)
        }
        CampusTabBar(tabs = AppTab.visible(hasNfc = true), selected = selected, onSelect = { selected = it })
    }
}

@Composable
private fun GoatCatalog(reduced: Boolean) {
    WithReducedMotion(reduced) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GoatLoader(size = 96.dp)
            GoatLoader(size = 44.dp)
            GoatLoader(size = 28.dp)
            Box(modifier = Modifier.size(1.dp))
        }
    }
}

private const val CATALOG_HEIGHT = 1500

@Preview(name = "Bausteine – Hell", showBackground = true, heightDp = CATALOG_HEIGHT)
@Composable
private fun ComponentsLightPreview() {
    CampusIlmenauTheme(darkTheme = false) { ComponentsCatalog() }
}

@Preview(name = "Bausteine – Dunkel", showBackground = true, heightDp = CATALOG_HEIGHT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComponentsDarkPreview() {
    CampusIlmenauTheme(darkTheme = true) { ComponentsCatalog() }
}

@Preview(name = "Bänder – Hell", showBackground = true, heightDp = 760)
@Composable
private fun BandsLightPreview() {
    CampusIlmenauTheme(darkTheme = false) { BandsCatalog() }
}

@Preview(name = "Bänder – Dunkel", showBackground = true, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BandsDarkPreview() {
    CampusIlmenauTheme(darkTheme = true) { BandsCatalog() }
}

@Preview(name = "Tab-Leiste – Hell", showBackground = true, heightDp = 240)
@Composable
private fun TabBarLightPreview() {
    CampusIlmenauTheme(darkTheme = false) { TabBarCatalog() }
}

@Preview(name = "Tab-Leiste – Dunkel", showBackground = true, heightDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TabBarDarkPreview() {
    CampusIlmenauTheme(darkTheme = true) { TabBarCatalog() }
}

@Preview(name = "Ziege – Hell", showBackground = true, heightDp = 400)
@Composable
private fun GoatLightPreview() {
    CampusIlmenauTheme(darkTheme = false) { GoatCatalog(reduced = false) }
}

@Preview(name = "Ziege – Dunkel", showBackground = true, heightDp = 400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoatDarkPreview() {
    CampusIlmenauTheme(darkTheme = true) { GoatCatalog(reduced = false) }
}

@Preview(name = "Ziege – Bewegung reduziert", showBackground = true, heightDp = 400)
@Composable
private fun GoatReducedPreview() {
    CampusIlmenauTheme(darkTheme = false) { GoatCatalog(reduced = true) }
}
