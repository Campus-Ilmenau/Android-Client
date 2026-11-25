package de.kaigothe.campusilmenau.ui.thoska

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.data.nfc.NfcState
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaBalance
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaError
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaFormatting
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.PrimaryButton
import de.kaigothe.campusilmenau.ui.components.SecondaryButton
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import java.time.Instant

/** Der Tab „Thoska“: Karte mit Guthaben, letzte Auslesung, Verlauf und die Taste zum Auslesen. */
@Composable
fun ThoskaScreen(
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThoskaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requested by viewModel.scanRequested.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current

    // NFC kann in den Einstellungen umgeschaltet worden sein. Das Warten endet, wenn die Ansicht nicht mehr sichtbar ist.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshNfc() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.cancelScan() }

    // Der Link `thoska/auslesen` startet das Auslesen sofort, sobald die Ansicht da ist.
    LaunchedEffect(requested, activity) {
        if (requested && activity != null) {
            viewModel.consumeScanRequest()
            viewModel.scan(activity)
        }
    }

    ThoskaContent(
        state = state,
        onScan = { activity?.let(viewModel::scan) },
        onOpenHistory = onOpenHistory,
        onRename = viewModel::rename,
        onOpenNfcSettings = { openNfcSettings(context, activity) },
        modifier = modifier,
    )
}

private fun openNfcSettings(context: android.content.Context, activity: Activity?) {
    val intent = Intent(Settings.ACTION_NFC_SETTINGS)
    runCatching { (activity ?: context).startActivity(intent.apply { if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
}

/** Oberfläche ohne ViewModel, damit sie sich in Vorschauen in jedem Zustand zeigen lässt. */
@Composable
fun ThoskaContent(
    state: ThoskaUiState,
    onScan: () -> Unit,
    onOpenHistory: () -> Unit,
    onRename: (cardId: String, name: String) -> Unit,
    onOpenNfcSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CampusTheme.colors
    val balance = state.balance
    var renaming by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.ScreenEdge)
                .padding(bottom = Spacing.Xl),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.thoska_titel),
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                TextLink(
                    text = stringResource(R.string.thoska_verlauf),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    onClick = onOpenHistory,
                )
            }

            val spoken = spokenText(state)
            ThoskaBalanceCard(
                balanceText = balance?.let { ThoskaFormatting.euro(it.balanceMilli) } ?: stringResource(R.string.thoska_guthaben_leer),
                spokenText = spoken,
                cardName = state.cardName,
            )

            CampusCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoColumn(
                        title = stringResource(R.string.thoska_zuletzt),
                        value = balance?.let { ThoskaFormatting.lastRead(it.scannedAt, state.now).asString() }
                            ?: stringResource(R.string.thoska_noch_nicht),
                        modifier = Modifier.weight(1f),
                    )
                    InfoColumn(
                        title = stringResource(R.string.thoska_letzte_buchung),
                        value = balance?.let { ThoskaFormatting.euro(it.lastTransactionMilli) } ?: "–",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Erst nach dem Auslesen, denn der Name hängt an der Karten-ID.
            if (balance != null) {
                TextLink(
                    text = stringResource(if (state.cardName == null) R.string.thoska_karte_benennen else R.string.thoska_karte_umbenennen),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    onClick = { renaming = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.showsConfirmation) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.green, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.onGreen, modifier = Modifier.size(24.dp))
                    Text(
                        text = stringResource(R.string.thoska_aktualisiert),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.onGreen,
                    )
                }
            }
        }

        Footer(state = state, onScan = onScan, onOpenNfcSettings = onOpenNfcSettings)
    }

    if (renaming && balance != null) {
        RenameDialog(
            initial = state.cardName.orEmpty(),
            onSave = { name ->
                onRename(balance.cardId, name)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
}

@Composable
private fun spokenText(state: ThoskaUiState): String {
    val balance = state.balance ?: return stringResource(R.string.thoska_karte_sprechen_leer)
    val amount = ThoskaFormatting.spokenEuro(balance.balanceMilli).asString()
    val name = state.cardName
    return if (name == null) {
        stringResource(R.string.thoska_karte_sprechen, amount)
    } else {
        stringResource(R.string.thoska_karte_sprechen_name, amount, name)
    }
}

@Composable
private fun InfoColumn(title: String, value: String, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    Column(modifier = modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
    }
}

/** Hinweis, Taste zum Auslesen und Statusmeldung. Der Fuß bleibt unten stehen. */
@Composable
private fun Footer(state: ThoskaUiState, onScan: () -> Unit, onOpenNfcSettings: () -> Unit) {
    val colors = CampusTheme.colors
    val status = statusText(state)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.thoska_hinweis),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        PrimaryButton(
            text = stringResource(if (state.isScanning) R.string.thoska_scan_aktiv else R.string.thoska_auslesen),
            onClick = onScan,
            icon = Icons.Outlined.Nfc,
            isLoading = state.isScanning,
            enabled = state.nfc != NfcState.Missing && !state.isScanning,
        )
        if (state.nfc == NfcState.Off) {
            SecondaryButton(text = stringResource(R.string.thoska_nfc_einstellungen), onClick = onOpenNfcSettings)
        }
        if (status != null) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

/** Ohne NFC steht hier der Grund, sonst die Meldung des Auslesens (Karte nicht erkannt usw.). */
@Composable
private fun statusText(state: ThoskaUiState): String? = when {
    state.nfc == NfcState.Missing -> stringResource(R.string.thoska_fehler_kein_nfc)
    state.nfc == NfcState.Off -> stringResource(R.string.thoska_fehler_nfc_aus)
    else -> when (state.error) {
        null -> null
        is ThoskaError.NoNfc -> stringResource(R.string.thoska_fehler_kein_nfc)
        is ThoskaError.NfcOff -> stringResource(R.string.thoska_fehler_nfc_aus)
        is ThoskaError.UnsupportedCard -> stringResource(R.string.thoska_fehler_kartentyp)
        is ThoskaError.ConnectionLost -> stringResource(R.string.thoska_fehler_verbindung)
        is ThoskaError.CardRefused -> stringResource(R.string.thoska_fehler_allgemein)
    }
}

/** Alert zum Benennen einer Karte. Ein leerer Name setzt die Karte zurück. */
@Composable
fun RenameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val field = rememberTextFieldState(initial)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.thoska_dialog_titel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.thoska_dialog_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CampusTheme.colors.textSecondary,
                )
                OutlinedTextField(
                    state = field,
                    label = { Text(stringResource(R.string.thoska_dialog_name)) },
                    lineLimits = androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(field.text.toString()) }) {
                Text(stringResource(R.string.thoska_dialog_sichern), color = CampusTheme.colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.abbrechen), color = CampusTheme.colors.accent) }
        },
    )
}

/** Link in `accent` mit 48 dp hoher Tippfläche, der Text steht mittig in der Höhe. */
@Composable
fun TextLink(text: String, style: androidx.compose.ui.text.TextStyle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.heightIn(min = Spacing.MinTouchTarget).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = text, style = style, color = CampusTheme.colors.accent)
    }
}

// MARK: - Karte

/**
 * Die Thoska-Karte: Verlauf von Blau nach Nachtblau mit grünem Band. Bleibt in Light und Dark gleich und nutzt deshalb nur
 * Farben ohne Dark-Variante. Der Betrag bleibt links vom grünen Band (dessen Kante unten bei 48 % der Breite beginnt) und
 * schrumpft bei langen Beträgen, statt aufs Band zu laufen.
 */
@Composable
fun ThoskaBalanceCard(balanceText: String, spokenText: String, cardName: String?, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(colors.blue, colors.brandNight), start = Offset.Zero, end = Offset(size.width, size.height)))
                val band = Path().apply {
                    moveTo(size.width * 0.72f, 0f)
                    lineTo(size.width * 0.86f, 0f)
                    lineTo(size.width * 0.62f, size.height)
                    lineTo(size.width * 0.48f, size.height)
                    close()
                }
                drawPath(band, colors.green.copy(alpha = 0.9f))
            }
            .heightIn(min = 200.dp)
            .clearAndSetSemantics { contentDescription = spokenText },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                tint = colors.onBlue,
                modifier = Modifier.size(44.dp),
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.thoska_titel).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = colors.onBlue.copy(alpha = 0.85f),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(0.46f).padding(start = 20.dp, bottom = 20.dp, top = 80.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = if (cardName != null) stringResource(R.string.thoska_guthaben_name, cardName) else stringResource(R.string.thoska_guthaben),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onBlue.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = balanceText,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp, lineHeight = 50.sp),
                color = colors.onBlue,
                maxLines = 1,
                autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 44.sp),
            )
        }
    }
}

// MARK: - Vorschauen

private fun sample(scannedAgoSeconds: Long = 3600) = ThoskaBalance(
    cardId = "04a1b2c3d4",
    balanceMilli = 12_400,
    lastTransactionMilli = 2_900,
    scannedAt = Instant.parse("2026-11-25T10:00:00Z").minusSeconds(scannedAgoSeconds),
)

private val PreviewNow: Instant = Instant.parse("2026-11-25T10:00:00Z")

@Composable
private fun PreviewHost(dark: Boolean, state: ThoskaUiState) {
    CampusIlmenauTheme(darkTheme = dark) {
        ThoskaContent(state, onScan = {}, onOpenHistory = {}, onRename = { _, _ -> }, onOpenNfcSettings = {})
    }
}

@Preview(name = "Thoska Light", showBackground = true, heightDp = 780)
@Composable
private fun ThoskaLightPreview() = PreviewHost(false, ThoskaUiState(NfcState.On, sample(), now = PreviewNow))

@Preview(name = "Thoska Dark", showBackground = true, heightDp = 780, backgroundColor = 0xFF0E1A2E)
@Composable
private fun ThoskaDarkPreview() = PreviewHost(true, ThoskaUiState(NfcState.On, sample(), "Mensakarte", showsConfirmation = true, now = PreviewNow))

@Preview(name = "Thoska leer", showBackground = true, heightDp = 780)
@Composable
private fun ThoskaEmptyPreview() = PreviewHost(false, ThoskaUiState(NfcState.On, now = PreviewNow))

@Preview(name = "Thoska NFC aus", showBackground = true, heightDp = 780)
@Composable
private fun ThoskaNfcOffPreview() = PreviewHost(false, ThoskaUiState(NfcState.Off, now = PreviewNow))

@Preview(name = "Thoska Fehler", showBackground = true, heightDp = 780)
@Composable
private fun ThoskaErrorPreview() = PreviewHost(false, ThoskaUiState(NfcState.On, sample(), error = ThoskaError.UnsupportedCard(), now = PreviewNow))

@Preview(name = "Thoska Auslesen", showBackground = true, heightDp = 780)
@Composable
private fun ThoskaScanningPreview() = PreviewHost(false, ThoskaUiState(NfcState.On, sample(), isScanning = true, now = PreviewNow))
