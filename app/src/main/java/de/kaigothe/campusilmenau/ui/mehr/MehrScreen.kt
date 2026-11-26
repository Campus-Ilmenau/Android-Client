package de.kaigothe.campusilmenau.ui.mehr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import de.kaigothe.campusilmenau.domain.model.mehr.AppLinks
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.BuildConfig
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.Darstellung
import de.kaigothe.campusilmenau.domain.model.SpracheWahl
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.ui.bibliothek.ausweis.findFragmentActivity
import de.kaigothe.campusilmenau.ui.components.BackBar
import de.kaigothe.campusilmenau.ui.components.DestructiveButton
import de.kaigothe.campusilmenau.ui.components.PrimaryButton
import de.kaigothe.campusilmenau.ui.components.SegmentOption
import de.kaigothe.campusilmenau.ui.components.SegmentedControl
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing

/** Die Bereiche von „Mehr“. Der Wert ist zugleich der Wert im Link `campusilmenau://mehr?bereich=…`. */
enum class MehrBereich(val link: String) {
    Konto("konto"),
    Darstellung("darstellung"),
    Mensa("mensa"),
    Sprache("sprache"),
    Ueber("ueber"),
    ;

    companion object {
        /** Aus dem Wert des Links; unbekannte Werte ergeben `null` (dann bleibt die Liste oben). */
        fun ofLink(link: String?): MehrBereich? = link?.lowercase()?.let { wert -> entries.firstOrNull { it.link == wert } }
    }
}

/** „Mehr und Einstellungen“ (Personen-Button auf Heute). */
@Composable
fun MehrScreen(
    bereich: String?,
    onBack: () -> Unit,
    onOpenMensaZeiten: () -> Unit,
    onOpenGespeicherteDaten: () -> Unit,
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MehrViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var appIcon by remember { mutableStateOf(AppIcon.aktuell(context)) }
    MehrContent(
        state = state,
        bereich = MehrBereich.ofLink(bereich),
        sprache = AppSprache.aktuell(context),
        erscheintAls = AppSprache.erscheintAls(context),
        onBack = onBack,
        onDarstellung = viewModel::setDarstellung,
        onHeuteMensa = viewModel::setHeuteMensa,
        onPreisgruppe = viewModel::setPriceGroup,
        onSprache = { wahl -> context.findFragmentActivity()?.let { AppSprache.waehle(it, wahl) } },
        appIcon = appIcon,
        onAppIcon = { wahl ->
            AppIcon.waehle(context, wahl)
            appIcon = AppIcon.aktuell(context)
        },
        onOpenMensaZeiten = onOpenMensaZeiten,
        onOpenGespeicherteDaten = onOpenGespeicherteDaten,
        onAnmelden = onOpenLogin,
        onAbmelden = viewModel::logout,
        onOpenUrl = { url -> runCatching { uriHandler.openUri(url) } },
        version = BuildConfig.VERSION_NAME,
        build = BuildConfig.VERSION_CODE.toString(),
        modifier = modifier,
    )
}

/** Die Oberfläche ohne ViewModel, damit sie sich in Vorschauen in jedem Zustand zeigen lässt. */
@Composable
fun MehrContent(
    state: MehrUiState,
    bereich: MehrBereich?,
    sprache: SpracheWahl,
    erscheintAls: SpracheWahl,
    onBack: () -> Unit,
    onDarstellung: (Darstellung) -> Unit,
    onHeuteMensa: (MensaVenue) -> Unit,
    onPreisgruppe: (MensaPriceGroup) -> Unit,
    onSprache: (SpracheWahl) -> Unit,
    appIcon: AppIconWahl,
    onAppIcon: (AppIconWahl) -> Unit,
    onOpenMensaZeiten: () -> Unit,
    onOpenGespeicherteDaten: () -> Unit,
    onAnmelden: () -> Unit,
    onAbmelden: () -> Unit,
    onOpenUrl: (String) -> Unit,
    version: String,
    build: String,
    modifier: Modifier = Modifier,
) {
    val colors = CampusTheme.colors
    val list = rememberLazyListState()

    // Ein Bereich aus dem Link wird nach oben gescrollt. Die Reihenfolge: Titel, Darstellung, Mensa, Konto, Allgemein.
    LaunchedEffect(bereich) {
        val index = when (bereich) {
            null -> return@LaunchedEffect
            MehrBereich.Darstellung -> 1
            MehrBereich.Mensa -> 2
            MehrBereich.Konto -> 3
            MehrBereich.Sprache, MehrBereich.Ueber -> 4
        }
        list.animateScrollToItem(index)
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        BackBar(onBack = onBack)
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item(key = "titel") {
                Text(
                    text = stringResource(R.string.mehr_titel),
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(start = 6.dp).semantics { heading() },
                )
            }
            item(key = "darstellung") {
                MehrAbschnitt(stringResource(R.string.mehr_dar_titel)) {
                    SegmentedControl(
                        options = Darstellung.entries.map { SegmentOption(it, stringResource(it.titel())) },
                        selected = state.darstellung,
                        onSelect = onDarstellung,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
            }
            item(key = "mensa") {
                MehrAbschnitt(stringResource(R.string.mehr_mensa_titel)) {
                    MehrAuswahlZeile(
                        titel = stringResource(R.string.mehr_mensa_start),
                        wert = state.heuteMensa.displayName,
                        optionen = MensaVenue.entries.toList(),
                        gewaehlt = state.heuteMensa,
                        beschriftung = { it.displayName },
                        onWahl = onHeuteMensa,
                    )
                    MehrTrenner()
                    MehrAuswahlZeile(
                        titel = stringResource(R.string.mehr_preisgruppe),
                        wert = stringResource(state.priceGroup.titel()),
                        optionen = MensaPriceGroup.entries.toList(),
                        gewaehlt = state.priceGroup,
                        beschriftung = { stringResource(it.titel()) },
                        onWahl = onPreisgruppe,
                    )
                    MehrTrenner()
                    MehrLinkZeile(titel = stringResource(R.string.mensa_zeiten_oeffnen), onClick = onOpenMensaZeiten)
                }
            }
            item(key = "konto") {
                MehrAbschnitt(stringResource(R.string.mehr_konto_titel)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.paia.isAuthenticated) {
                            val nummer = state.paia.lastUsername
                            Text(
                                text = if (nummer != null) stringResource(R.string.mehr_konto_angemeldet_als, nummer) else stringResource(R.string.mehr_konto_angemeldet),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                                color = colors.textPrimary,
                            )
                            DestructiveButton(text = stringResource(R.string.mehr_abmelden), onClick = onAbmelden, modifier = Modifier.fillMaxWidth())
                        } else {
                            Text(stringResource(R.string.mehr_konto_abgemeldet), style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
                            PrimaryButton(text = stringResource(R.string.mehr_anmelden), onClick = onAnmelden, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            item(key = "allgemein") {
                MehrAbschnitt(stringResource(R.string.mehr_allgemein)) {
                    val automatisch = stringResource(R.string.mehr_sprache_auto, erscheintAls.anzeige())
                    MehrAuswahlZeile(
                        titel = stringResource(R.string.mehr_sprache),
                        wert = if (sprache == SpracheWahl.Automatisch) automatisch else sprache.anzeige(),
                        optionen = SpracheWahl.entries.toList(),
                        gewaehlt = sprache,
                        beschriftung = { it.anzeige() },
                        onWahl = onSprache,
                    )
                    MehrTrenner()
                    MehrAuswahlZeile(
                        titel = stringResource(R.string.mehr_app_icon),
                        wert = stringResource(appIcon.titel),
                        optionen = AppIconWahl.entries.toList(),
                        gewaehlt = appIcon,
                        beschriftung = { stringResource(it.titel) },
                        onWahl = onAppIcon,
                    )
                    MehrTrenner()
                    MehrLinkZeile(titel = stringResource(R.string.mehr_gespeicherte_daten), onClick = onOpenGespeicherteDaten)
                    MehrTrenner()
                    MehrLinkZeile(titel = stringResource(R.string.mehr_datenschutz), onClick = { onOpenUrl(AppLinks.DATENSCHUTZ) }, extern = true)
                    MehrTrenner()
                    MehrLinkZeile(titel = stringResource(R.string.mehr_impressum), onClick = { onOpenUrl(AppLinks.IMPRESSUM) }, extern = true)
                }
            }
            item(key = "version") {
                Text(
                    text = stringResource(R.string.mehr_version, version, build),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun Darstellung.titel(): Int = when (this) {
    Darstellung.System -> R.string.mehr_dar_system
    Darstellung.Hell -> R.string.mehr_dar_hell
    Darstellung.Dunkel -> R.string.mehr_dar_dunkel
}

private fun MensaPriceGroup.titel(): Int = when (this) {
    MensaPriceGroup.Studierende -> R.string.preisgruppe_studierende
    MensaPriceGroup.Bedienstete -> R.string.preisgruppe_bedienstete
    MensaPriceGroup.Gaeste -> R.string.preisgruppe_gaeste
}

@Composable
private fun SpracheWahl.anzeige(): String = stringResource(
    when (this) {
        SpracheWahl.Automatisch -> R.string.mehr_sprache_automatisch
        SpracheWahl.Deutsch -> R.string.mehr_sprache_de
        SpracheWahl.Englisch -> R.string.mehr_sprache_en
    },
)
