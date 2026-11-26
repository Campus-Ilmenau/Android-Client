package de.kaigothe.campusilmenau.ui.mehr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.Darstellung
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaFormatting
import de.kaigothe.campusilmenau.ui.components.BackBar
import de.kaigothe.campusilmenau.ui.components.DestructiveButton
import de.kaigothe.campusilmenau.ui.mensa.activeDescription
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** „Gespeicherte Daten“: alles, was die App auf diesem Gerät ablegt, und auch, was bewusst nur im Speicher bleibt. */
@Composable
fun GespeicherteDatenScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: GespeicherteDatenViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GespeicherteDatenContent(
        state = state,
        onBack = onBack,
        onLoeschen = viewModel::zeigeLoeschen,
        onLoeschenBestaetigt = viewModel::alleLoeschen,
        onLoeschenAbbrechen = viewModel::schliesseLoeschen,
        modifier = modifier,
    )
}

@Composable
fun GespeicherteDatenContent(
    state: GespeicherteDatenUiState,
    onBack: () -> Unit,
    onLoeschen: () -> Unit,
    onLoeschenBestaetigt: () -> Unit,
    onLoeschenAbbrechen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CampusTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val datum = remember(locale) {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        val format: (Instant) -> String = { formatter.format(BerlinTime.date(it)) }
        format
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        BackBar(onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item(key = "titel") {
                Column(modifier = Modifier.padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.gd_titel), style = MaterialTheme.typography.displaySmall, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.gd_intro), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
            }
            item(key = "thoska") {
                MehrAbschnitt(stringResource(R.string.gd_thoska), fuss = stringResource(R.string.gd_thoska_fuss, state.kartenNamen)) {
                    if (state.karten.isEmpty()) {
                        Leer(stringResource(R.string.gd_thoska_leer))
                    } else {
                        state.karten.forEachIndexed { index, karte ->
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) {},
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(karte.name.asString(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                Text(
                                    stringResource(R.string.gd_thoska_karte, karte.anzahl, datum(karte.erste), datum(karte.letzte)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                )
                                Text(
                                    stringResource(R.string.gd_thoska_je, ThoskaFormatting.euro(karte.letzterStandMilli, locale)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textTertiary,
                                )
                            }
                            if (index < state.karten.lastIndex) MehrTrenner()
                        }
                    }
                }
            }
            item(key = "merkliste") {
                MehrAbschnitt(stringResource(R.string.gd_merkliste), fuss = stringResource(R.string.gd_merkliste_fuss)) {
                    if (state.merkliste.isEmpty()) {
                        Leer(stringResource(R.string.gd_merkliste_leer))
                    } else {
                        state.merkliste.forEachIndexed { index, titel ->
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) {},
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(titel.anzeigeTitel.asString(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                                if (titel.untertitel.isNotEmpty()) Text(titel.untertitel, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                                Text(stringResource(R.string.gd_gemerkt_am, datum(titel.gemerktAm)), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                            }
                            if (index < state.merkliste.lastIndex) MehrTrenner()
                        }
                    }
                }
            }
            item(key = "events") {
                MehrAbschnitt(stringResource(R.string.gd_events), fuss = stringResource(R.string.gd_events_fuss)) {
                    MehrWertZeile(stringResource(R.string.gd_events_zeile), state.gemerkteVeranstaltungen.toString())
                }
            }
            item(key = "einstellungen") {
                MehrAbschnitt(stringResource(R.string.gd_einstellungen), fuss = stringResource(R.string.gd_einstellungen_fuss)) {
                    MehrWertZeile(
                        stringResource(R.string.mehr_dar_titel),
                        stringResource(
                            when (state.darstellung) {
                                Darstellung.System -> R.string.mehr_dar_system
                                Darstellung.Hell -> R.string.mehr_dar_hell
                                Darstellung.Dunkel -> R.string.mehr_dar_dunkel
                            },
                        ),
                    )
                    MehrTrenner()
                    MehrWertZeile(stringResource(R.string.mehr_mensa_start), state.heuteMensa.displayName)
                    MehrTrenner()
                    MehrWertZeile(
                        stringResource(R.string.mehr_preisgruppe),
                        stringResource(
                            when (state.priceGroup) {
                                MensaPriceGroup.Studierende -> R.string.preisgruppe_studierende
                                MensaPriceGroup.Bedienstete -> R.string.preisgruppe_bedienstete
                                MensaPriceGroup.Gaeste -> R.string.preisgruppe_gaeste
                            },
                        ),
                    )
                    MehrTrenner()
                    MehrWertZeile(
                        stringResource(R.string.gd_mensa_filter),
                        if (state.mensaFilter.isActive) state.mensaFilter.activeDescription() else stringResource(R.string.gd_aus),
                    )
                    MehrTrenner()
                    MehrWertZeile(stringResource(R.string.gd_favoriten), state.gemerkteGerichte.toString())
                    MehrTrenner()
                    MehrWertZeile(stringResource(R.string.gd_hinweise), state.ausgeblendeteHinweise.toString())
                }
            }
            item(key = "nicht") {
                MehrAbschnitt(stringResource(R.string.gd_nicht)) {
                    MehrHinweisZeile(
                        stringResource(R.string.mehr_konto_titel),
                        stringResource(if (state.speichertZugangsdaten) R.string.gd_konto_mit else R.string.gd_konto_ohne),
                    )
                    MehrTrenner()
                    MehrHinweisZeile(stringResource(R.string.gd_kamera), stringResource(R.string.gd_kamera_text))
                    MehrTrenner()
                    MehrHinweisZeile(stringResource(R.string.gd_ausweis), stringResource(R.string.gd_ausweis_text))
                }
            }
            item(key = "loeschen") {
                DestructiveButton(text = stringResource(R.string.gd_loeschen_button), onClick = onLoeschen, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (state.loeschenDialog) {
        AlertDialog(
            onDismissRequest = onLoeschenAbbrechen,
            title = { Text(stringResource(R.string.gd_loeschen_frage)) },
            text = { Text(stringResource(R.string.gd_loeschen_text)) },
            confirmButton = { TextButton(onClick = onLoeschenBestaetigt) { Text(stringResource(R.string.gd_loeschen_ok), color = colors.danger) } },
            dismissButton = { TextButton(onClick = onLoeschenAbbrechen) { Text(stringResource(R.string.mehr_abbrechen), color = colors.accent) } },
        )
    }
}

@Composable
private fun Leer(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = CampusTheme.colors.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
    )
}
