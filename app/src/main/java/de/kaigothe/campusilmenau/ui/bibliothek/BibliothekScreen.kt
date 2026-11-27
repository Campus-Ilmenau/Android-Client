package de.kaigothe.campusilmenau.ui.bibliothek

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import de.kaigothe.campusilmenau.ui.adaptive.AdaptiveColumns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import de.kaigothe.campusilmenau.ui.bibliothek.ausweis.AusweisPruefung
import de.kaigothe.campusilmenau.ui.bibliothek.ausweis.AusweisScreen
import de.kaigothe.campusilmenau.ui.bibliothek.ausweis.findFragmentActivity
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.focus.focusRequester
import de.kaigothe.campusilmenau.ui.components.SecondaryButton
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.library.LoanLists
import de.kaigothe.campusilmenau.domain.model.library.PaiaError
import de.kaigothe.campusilmenau.domain.model.library.PaiaNotification
import de.kaigothe.campusilmenau.domain.model.library.kind
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.CampusSheet
import de.kaigothe.campusilmenau.ui.components.GoatLoadingBlock
import de.kaigothe.campusilmenau.ui.components.MessageCard
import de.kaigothe.campusilmenau.ui.components.NeutralSwitchRow
import de.kaigothe.campusilmenau.ui.components.PrimaryButton
import de.kaigothe.campusilmenau.ui.components.SegmentOption
import de.kaigothe.campusilmenau.ui.components.SegmentedControl
import de.kaigothe.campusilmenau.ui.components.SoftDestructiveButton
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Der Tab „Bibliothek“: Ausleihen und Vormerkungen aus dem PAIA-Konto, Mitteilungen, Anmelde-Sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibliothekScreen(
    onOpenCatalog: () -> Unit,
    onOpenMerkliste: () -> Unit,
    onOpenTitle: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BibliothekViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pruefungTitel = stringResource(R.string.ausweis_pruefung_titel)

    LaunchedEffect(viewModel) { viewModel.start() }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            if (state.paia.isAuthenticated) {
                scope.launch {
                    refreshing = true
                    try {
                        viewModel.refresh()
                    } finally {
                        refreshing = false
                    }
                }
            }
        },
        modifier = modifier,
    ) {
        BibliothekContent(
            state = state,
            loadingGate = viewModel.loadingGate,
            onSelectTab = viewModel::selectTab,
            onRenew = viewModel::renew,
            onDismissError = viewModel::dismissError,
            onLogout = viewModel::logout,
            onOpenNotifications = viewModel::openNotifications,
            onOpenLogin = viewModel::openLogin,
            onOpenCatalog = onOpenCatalog,
            onOpenMerkliste = onOpenMerkliste,
            onOpenTitle = onOpenTitle,
            onShowAusweis = {
                context.findFragmentActivity()?.let { activity ->
                    viewModel.zeigeAusweis { AusweisPruefung.pruefen(activity, pruefungTitel) }
                }
            },
        )
    }

    state.ausweis?.let { daten -> AusweisScreen(daten = daten, clock = viewModel.clock, onClose = viewModel::schliesseAusweis) }
    state.zugangHinweis?.let { hinweis ->
        AlertDialog(
            onDismissRequest = viewModel::schliesseZugangHinweis,
            title = { Text(hinweis.titel.asString()) },
            text = { Text(hinweis.text.asString()) },
            confirmButton = {
                TextButton(onClick = viewModel::schliesseZugangHinweis) {
                    Text(stringResource(R.string.ausweis_ok), color = CampusTheme.colors.accent)
                }
            },
        )
    }

    if (state.notificationsVisible) {
        NotificationsSheet(
            notifications = state.paia.notifications,
            onRefresh = { scope.launch { viewModel.refreshNotifications() } },
            onDismiss = viewModel::closeNotifications,
        )
    }
    if (state.loginVisible) {
        LoginSheet(
            form = state.login,
            expired = state.paia.sessionExpired,
            onUsername = viewModel::setUsername,
            onPassword = viewModel::setPassword,
            onRemember = viewModel::setRemember,
            onLogin = viewModel::login,
            onDismiss = viewModel::cancelLogin,
        )
    }
}

/** Oberfläche ohne ViewModel, damit sie sich in Vorschauen in jedem Zustand zeigen lässt. */
@Composable
fun BibliothekContent(
    state: BibliothekUiState,
    loadingGate: de.kaigothe.campusilmenau.ui.components.LoadingGate?,
    onSelectTab: (BibliothekTab) -> Unit,
    onRenew: (de.kaigothe.campusilmenau.domain.model.library.PaiaLoanItem) -> Unit,
    onDismissError: () -> Unit,
    onLogout: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenCatalog: () -> Unit = {},
    onOpenMerkliste: () -> Unit = {},
    onOpenTitle: (id: String) -> Unit = {},
    onShowAusweis: () -> Unit = {},
) {
    val colors = CampusTheme.colors
    val paia = state.paia
    val lists = remember(paia.items) { LoanLists(paia.items) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Zwei Spalten: Ausleihen und Vormerkungen nebeneinander statt des Umschalters; drei: dazu das Konto (iPad: `BibliothekPad`).
    val columns = AdaptiveColumns.count(availableDp = maxWidth.value.toInt(), minColumnDp = ColumnMinDp, maxColumns = 3, gapDp = ColumnGapDp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.ScreenEdge)
            .padding(bottom = Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tab_bibliothek),
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                val summaryTexts = when {
                    !paia.isAuthenticated -> listOf(stringResource(R.string.bib_nicht_angemeldet))
                    paia.items.isEmpty() && paia.isLoading -> listOf(stringResource(R.string.bib_wird_geladen))
                    else -> lists.summary(state.today).map { it.asString() }
                }
                Text(
                    text = summaryTexts.joinToString(stringResource(R.string.bib_zusammenfassung_trenner)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            // Katalog und Merkliste brauchen keine Anmeldung.
            IconButton(onClick = onOpenMerkliste, modifier = Modifier.size(Spacing.MinTouchTarget)) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = stringResource(R.string.bib_merkliste_oeffnen), tint = colors.textSecondary)
            }
            IconButton(onClick = onOpenCatalog, modifier = Modifier.size(Spacing.MinTouchTarget)) {
                Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.bib_kat_oeffnen), tint = colors.textSecondary)
            }
            if (paia.isAuthenticated) {
                val count = paia.notifications.size
                IconButton(onClick = onOpenNotifications, modifier = Modifier.size(Spacing.MinTouchTarget)) {
                    val description = if (count > 0) stringResource(R.string.bib_mitteilungen_n, count) else stringResource(R.string.bib_mitteilungen)
                    Icon(Icons.Outlined.Notifications, contentDescription = description, tint = if (count > 0) colors.accent else colors.textSecondary)
                }
            }
        }

        if (!paia.isAuthenticated) {
            SignedOutView(expired = paia.sessionExpired, onOpenLogin = onOpenLogin)
            return@Column
        }

        paia.error?.let { ErrorBox(error = it, onDismiss = onDismissError) }

        if (state.isFirstLoad && loadingGate != null) {
            GoatLoadingBlock(message = stringResource(R.string.bib_wird_geladen), gate = loadingGate)
            return@Column
        }

        val loansList: @Composable () -> Unit = {
            if (lists.loans.isEmpty()) {
                MessageCard(title = stringResource(R.string.bib_leer_ausleihen_titel), text = stringResource(R.string.bib_leer_ausleihen_text))
            } else {
                lists.loans.forEach { item ->
                    LoanCard(
                        item = item,
                        today = state.today,
                        onRenew = { onRenew(item) },
                        onOpen = item.catalogPpn?.let { ppn -> { onOpenTitle("med_$ppn") } },
                    )
                }
            }
        }
        val holdsList: @Composable () -> Unit = {
            if (lists.holds.isEmpty()) {
                MessageCard(title = stringResource(R.string.bib_leer_vormerkungen_titel), text = stringResource(R.string.bib_leer_vormerkungen_text))
            } else {
                lists.holds.forEach { item ->
                    HoldCard(item = item, onOpen = item.catalogPpn?.let { ppn -> { onOpenTitle("med_$ppn") } })
                }
            }
        }
        val account: @Composable () -> Unit = {
            val footer = when {
                paia.remembersCredentials && paia.lastUsername != null -> stringResource(R.string.bib_fuss_gespeichert_nutzer, paia.lastUsername)
                paia.remembersCredentials -> stringResource(R.string.bib_fuss_gespeichert)
                paia.lastUsername != null -> stringResource(R.string.bib_fuss_nutzer, paia.lastUsername)
                else -> stringResource(R.string.bib_fuss_ohne_nutzer)
            }
            Text(text = footer, style = MaterialTheme.typography.bodySmall, color = colors.textTertiary, modifier = Modifier.padding(top = 6.dp))
            SecondaryButton(
                text = stringResource(R.string.ausweis_zeigen),
                onClick = onShowAusweis,
                icon = Icons.Outlined.Badge,
                isLoading = state.ausweisWirdGeprueft,
                enabled = !state.ausweisWirdGeprueft,
                modifier = Modifier.fillMaxWidth(),
            )
            SoftDestructiveButton(text = stringResource(R.string.bib_abmelden), onClick = onLogout, modifier = Modifier.fillMaxWidth())
        }

        if (columns >= 2) {
            val loansTitle = stringResource(R.string.bib_segment_mit_zahl, stringResource(R.string.bib_segment_ausgeliehen), lists.loans.size)
            val holdsTitle = stringResource(R.string.bib_segment_mit_zahl, stringResource(R.string.bib_segment_vormerkungen), lists.holds.size)
            Row(horizontalArrangement = Arrangement.spacedBy(ColumnGapDp.dp), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SpaltenKopf(loansTitle)
                    loansList()
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SpaltenKopf(holdsTitle)
                    holdsList()
                }
                if (columns >= 3) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { account() }
                }
            }
            if (columns == 2) account()
            return@Column
        }

        SegmentedControl(
            options = listOf(
                SegmentOption(BibliothekTab.Loans, stringResource(R.string.bib_segment_mit_zahl, stringResource(R.string.bib_segment_ausgeliehen), lists.loans.size)),
                SegmentOption(BibliothekTab.Holds, stringResource(R.string.bib_segment_mit_zahl, stringResource(R.string.bib_segment_vormerkungen), lists.holds.size)),
            ),
            selected = state.tab,
            onSelect = onSelectTab,
            modifier = Modifier.fillMaxWidth(),
        )

        when (state.tab) {
            BibliothekTab.Loans -> loansList()
            BibliothekTab.Holds -> holdsList()
        }
        account()
    }
    }
}

/** Jede Spalte braucht mindestens so viel Breite (dp), der Abstand dazwischen ist [ColumnGapDp]. */
private const val ColumnMinDp = 340
private const val ColumnGapDp = 16

/** Überschrift einer Spalte („Ausgeliehen (3)“). */
@Composable
private fun SpaltenKopf(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = CampusTheme.colors.textPrimary,
        modifier = Modifier.padding(horizontal = 2.dp).semantics { heading() },
    )
}

@Composable
private fun SignedOutView(expired: Boolean, onOpenLogin: () -> Unit) {
    MessageCard(
        title = stringResource(if (expired) R.string.bib_login_titel_abgelaufen else R.string.bib_login_titel),
        text = stringResource(if (expired) R.string.bib_login_text_abgelaufen else R.string.bib_login_text),
        buttonText = stringResource(R.string.bib_login_titel),
        onButtonClick = onOpenLogin,
    )
}

/** Fehlerkasten in `dangerSoft`; das Antippen schließt ihn. */
@Composable
private fun ErrorBox(error: PaiaError, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.dangerSoft, RoundedCornerShape(12.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(text = error.message(), style = MaterialTheme.typography.bodyMedium, color = colors.danger)
    }
}

// MARK: - Mitteilungen

@Composable
private fun NotificationsSheet(notifications: List<PaiaNotification>, onRefresh: () -> Unit, onDismiss: () -> Unit) {
    val colors = CampusTheme.colors
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    CampusSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.bib_mitteilungen),
        endAction = { dismiss -> androidx.compose.material3.TextButton(onClick = dismiss) { Text(stringResource(R.string.fertig), color = colors.accent) } },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.ScreenEdge, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (notifications.isEmpty()) {
                MessageCard(title = stringResource(R.string.bib_mitteilungen_leer_titel), text = stringResource(R.string.bib_mitteilungen_leer_text))
            } else {
                notifications.forEach { notification ->
                    CampusCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = notification.kind.title.asString(),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary,
                            )
                            Text(text = notification.about, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            Text(
                                text = formatter.format(BerlinTime.date(notification.date)),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textTertiary,
                            )
                        }
                    }
                }
            }
            Text(text = stringResource(R.string.bib_mitteilungen_hinweis), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
            LaunchedEffect(Unit) { onRefresh() }
        }
    }
}

// MARK: - Anmelden

private enum class ScanFocus { Username, Password }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginSheet(
    form: LoginForm,
    expired: Boolean,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onRemember: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = CampusTheme.colors
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var scanning by rememberSaveable { mutableStateOf(false) }
    // Nach dem Scanner: bei Treffer Fokus aufs Passwort, sonst auf die Benutzernummer.
    var focusAfterScan by remember { mutableStateOf<ScanFocus?>(null) }
    val usernameFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val passwordFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(focusAfterScan, scanning) {
        if (!scanning) {
            when (focusAfterScan) {
                ScanFocus.Password -> passwordFocus.requestFocus()
                ScanFocus.Username -> usernameFocus.requestFocus()
                null -> Unit
            }
            focusAfterScan = null
        }
    }
    if (scanning) {
        BenutzernummerScanner(
            onScanned = { number ->
                onUsername(number)
                focusAfterScan = ScanFocus.Password
                scanning = false
            },
            onCancel = { scanning = false },
            onEnterManually = {
                focusAfterScan = ScanFocus.Username
                scanning = false
            },
        )
    }
    CampusSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.ScreenEdge, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(if (expired) R.string.bib_login_titel_abgelaufen else R.string.bib_login_titel),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(if (expired) R.string.bib_login_text_abgelaufen else R.string.bib_login_text),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            if (expired) {
                Text(text = stringResource(R.string.bib_login_hinweis_abgelaufen), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
            }
            form.error?.let { error ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.dangerSoft, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Text(
                        text = stringResource(R.string.bib_login_fehler_titel),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.danger,
                    )
                    Text(text = error.message(), style = MaterialTheme.typography.bodyMedium, color = colors.danger)
                }
            }
            OutlinedTextField(
                value = form.username,
                onValueChange = onUsername,
                label = { Text(stringResource(R.string.bib_login_nummer)) },
                placeholder = { Text(stringResource(R.string.bib_login_nummer_beispiel)) },
                supportingText = { Text(stringResource(R.string.bib_login_nummer_hinweis)) },
                singleLine = true,
                enabled = !form.isLoggingIn,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().focusRequester(usernameFocus).semantics { contentType = ContentType.Username },
            )
            SecondaryButton(
                text = stringResource(R.string.bib_scan_starten),
                onClick = { scanning = true },
                icon = Icons.Outlined.QrCodeScanner,
                enabled = !form.isLoggingIn,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.password,
                onValueChange = onPassword,
                label = { Text(stringResource(R.string.bib_login_passwort)) },
                singleLine = true,
                enabled = !form.isLoggingIn,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = stringResource(if (showPassword) R.string.bib_login_passwort_verbergen else R.string.bib_login_passwort_zeigen),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus).semantics { contentType = ContentType.Password },
            )
            NeutralSwitchRow(label = stringResource(R.string.bib_login_bleiben), checked = form.remember, onCheckedChange = onRemember)
            Text(
                text = stringResource(if (form.remember) R.string.bib_login_bleiben_text else R.string.bib_login_ohne_text),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
            )
            PrimaryButton(
                text = stringResource(R.string.bib_login_titel),
                onClick = onLogin,
                isLoading = form.isLoggingIn,
                enabled = form.canLogin,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(text = stringResource(R.string.bib_login_quelle), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
        }
    }
}
