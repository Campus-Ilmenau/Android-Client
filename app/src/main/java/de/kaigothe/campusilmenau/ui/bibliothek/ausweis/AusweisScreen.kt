package de.kaigothe.campusilmenau.ui.bibliothek.ausweis

import android.view.WindowManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.library.ausweis.AusweisDaten
import de.kaigothe.campusilmenau.ui.components.BandsStyle
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.components.DiagonalBands
import de.kaigothe.campusilmenau.ui.components.rememberReduceMotion
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Bibliotheksausweis: Vollbild, zum Scannen an der Ausleihtheke. Schließt nach 2 Minuten von selbst und sofort, wenn die App
 * nicht mehr sichtbar ist (auch im App-Umschalter darf er nicht zu sehen sein). Die Helligkeit steht so lange auf Maximum,
 * und das Fenster ist gegen Bildschirmfotos und Vorschaubilder geschützt (`FLAG_SECURE`).
 */
@Composable
fun AusweisScreen(daten: AusweisDaten, clock: Clock, onClose: () -> Unit, dauerSekunden: Int = AusweisDaten.DAUER_SEKUNDEN) {
    val close by rememberUpdatedState(onClose)
    // Auch im App-Umschalter und bei gesperrtem Bildschirm darf der Ausweis nicht offen bleiben.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { close() }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val attributes = window?.attributes
            attributes?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            window?.attributes = attributes
            onDispose {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        AusweisContent(daten = daten, clock = clock, dauerSekunden = dauerSekunden, onClose = onClose)
    }
}

@Composable
fun AusweisContent(daten: AusweisDaten, clock: Clock, dauerSekunden: Int, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val close by rememberUpdatedState(onClose)
    LaunchedEffect(dauerSekunden) {
        delay(dauerSekunden.toLong() * 1_000L)
        close()
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.ausweis_titel),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Box(
                modifier = Modifier
                    .heightIn(min = Spacing.MinTouchTarget)
                    .clickable(role = Role.Button, onClick = onClose)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.ausweis_fertig), style = MaterialTheme.typography.titleMedium, color = colors.accent)
            }
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 34.dp)) {
            Karte(daten = daten, clock = clock, dauerSekunden = dauerSekunden)
            LichtbildHinweis()
            daten.hinweis?.let { BibliotheksHinweis(it) }
            Text(
                text = stringResource(R.string.ausweis_fussnote),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 14.dp),
            )
        }
    }
}

@Composable
private fun Karte(daten: AusweisDaten, clock: Clock, dauerSekunden: Int) {
    val colors = CampusTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface),
    ) {
        DiagonalBands(style = BandsStyle.Hero, height = 92.dp) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 96.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.LocalLibrary, contentDescription = null, tint = colors.onBlue, modifier = Modifier.size(36.dp))
                Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                    Text(stringResource(R.string.ausweis_uni), style = MaterialTheme.typography.titleMedium, color = colors.onBlue)
                    Text(stringResource(R.string.ausweis_unterzeile), style = MaterialTheme.typography.bodySmall, color = colors.onBlue.copy(alpha = 0.85f))
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            daten.name?.let { name ->
                Beschriftung(stringResource(R.string.ausweis_name))
                Text(name, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, modifier = Modifier.padding(top = 2.dp))
            }
            daten.nutzergruppe?.let { CampusChip(text = it.asString(), modifier = Modifier.padding(top = 6.dp)) }

            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                if (daten.benutzernummer.isNotEmpty()) {
                    Column(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Beschriftung(stringResource(R.string.ausweis_nummer))
                        Text(
                            daten.benutzernummer,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            maxLines = 1,
                        )
                    }
                }
                daten.gueltigBis?.let { bis ->
                    val kritisch = daten.gueltigBisIstKritisch(BerlinTime.date(clock.instant()))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Beschriftung(stringResource(R.string.ausweis_gueltig))
                        Text(
                            dateFormatter.format(bis),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (kritisch) colors.danger else colors.textPrimary,
                        )
                    }
                }
            }
            daten.statusText?.let { text ->
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.dangerSoft)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = colors.danger, modifier = Modifier.size(20.dp))
                    Text(text.asString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.danger)
                }
            }
            if (daten.benutzernummer.isNotEmpty()) StrichcodeFeld(daten.benutzernummer, modifier = Modifier.padding(top = 16.dp))
        }
        LiveZeile(clock = clock, dauerSekunden = dauerSekunden)
    }
}

@Composable
private fun Beschriftung(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
        color = CampusTheme.colors.textTertiary,
    )
}

/** Weißes Feld mit Strichcode und Ziffern. Immer Schwarz auf Weiß, auch im Dark Mode, damit Scanner ihn lesen. */
@Composable
private fun StrichcodeFeld(nummer: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.ausweis_strichcode_cd, nummer)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            // Ruhezone links und rechts, mindestens 18 dp; das Bild darf nur so breit sein, wie dieses Feld innen frei lässt.
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Code39View(nummer = nummer)
        Text(
            text = nummer,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, letterSpacing = 4.sp),
            color = Color.Black,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** Uhr mit Sekunden, Restzeit und pulsierender Punkt: So sieht das Personal, dass es kein Screenshot ist. */
@Composable
private fun LiveZeile(clock: Clock, dauerSekunden: Int) {
    val colors = CampusTheme.colors
    val reduce = rememberReduceMotion()
    val start = remember { clock.instant() }
    var now by remember { mutableLongStateOf(clock.millis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = clock.millis()
            // Auf volle Sekunden getaktet, damit die Uhr mit der Systemuhr tickt.
            delay(1_000L - (now % 1_000L))
        }
    }
    val rest = (dauerSekunden - (now - start.toEpochMilli()) / 1_000.0).let { kotlin.math.ceil(it).toInt() }
    val seconds = BerlinTime.minutesOfDay(java.time.Instant.ofEpochMilli(now)).let { minutes ->
        "%02d:%02d:%02d".format(minutes / 60, minutes % 60, (now / 1_000L) % 60)
    }
    val description = stringResource(R.string.ausweis_live_cd, maxOf(0, rest) / 60, maxOf(0, rest) % 60)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulsPunkt(reduce)
        Text(
            stringResource(R.string.ausweis_live, seconds),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 7.dp),
        )
        Box(modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.ausweis_schliesst, AusweisDaten.restzeit(rest)),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun PulsPunkt(reduceMotion: Boolean) {
    val alpha = if (reduceMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "puls")
        val value by transition.animateFloat(1f, 0.35f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulsAlpha")
        value
    }
    Box(modifier = Modifier.size(8.dp).alpha(alpha).clip(CircleShape).background(CampusTheme.colors.green).clearAndSetSemantics { })
}

@Composable
private fun LichtbildHinweis() {
    val colors = CampusTheme.colors
    CampusCard(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top, modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(colors.warningFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Badge, contentDescription = null, tint = colors.onWarningFill, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.ausweis_lichtbild_titel), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                Text(stringResource(R.string.ausweis_lichtbild_text), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun BibliotheksHinweis(text: String) {
    val colors = CampusTheme.colors
    CampusCard(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Beschriftung(stringResource(R.string.ausweis_hinweis_titel))
            Text(text, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
        }
    }
}
