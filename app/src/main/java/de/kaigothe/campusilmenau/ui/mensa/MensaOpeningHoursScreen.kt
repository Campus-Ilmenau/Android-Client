package de.kaigothe.campusilmenau.ui.mensa

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.mensa.Canteen
import de.kaigothe.campusilmenau.domain.model.mensa.MensaOpeningHours
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.domain.model.mensa.OpeningHour
import de.kaigothe.campusilmenau.ui.components.BackBar
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.components.ChipVariant
import de.kaigothe.campusilmenau.ui.components.SecondaryButton
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import java.time.LocalDateTime
import java.time.format.TextStyle

/** Öffnungszeiten der vier Mensen: heutige Zeit mit Status-Chip, ein Tipp klappt die Wochenzeiten auf. */
@Composable
fun MensaOpeningHoursScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MensaOpeningHoursViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    MensaOpeningHoursContent(state = state, now = now, onBack = onBack, onRetry = viewModel::retry, modifier = modifier)
}

@Composable
fun MensaOpeningHoursContent(
    state: MensaOpeningHoursState,
    now: LocalDateTime,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyOpen: MensaVenue? = MensaVenue.MensaEhrenberg,
) {
    val colors = CampusTheme.colors
    // Welche Mensa aufgeklappt ist, überlebt Drehen und Prozessende (gespeichert wird der Schlüssel).
    var openKey by rememberSaveable { mutableStateOf(initiallyOpen?.key) }
    val venues = (state as? MensaOpeningHoursState.Loaded)?.venues
        ?: MensaVenue.entries.map { VenueHours(it, canteen = null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        BackBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.ScreenEdge)
                .padding(bottom = Spacing.Xl),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.zeiten_titel),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp, lineHeight = 34.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() },
            )
            Text(
                text = stringResource(R.string.zeiten_heute_wochentag, now.dayOfWeek.getDisplayName(TextStyle.FULL, LocalConfiguration.current.locales[0])),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 4.dp),
            )

            venues.forEach { entry ->
                val isOpen = openKey == entry.venue.key
                VenueCard(
                    entry = entry,
                    now = now,
                    isOpen = isOpen,
                    onToggle = { openKey = if (isOpen) null else entry.venue.key },
                )
            }

            when (state) {
                MensaOpeningHoursState.Loading -> Note(stringResource(R.string.zeiten_laedt))
                is MensaOpeningHoursState.Failed -> {
                    Note(stringResource(R.string.zeiten_fehler))
                    SecondaryButton(text = stringResource(R.string.erneut_versuchen), onClick = onRetry, compact = true)
                }

                is MensaOpeningHoursState.Loaded -> Unit
            }

            Text(
                text = stringResource(R.string.zeiten_hinweis),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 6.dp),
            )
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = CampusTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Composable
private fun VenueCard(entry: VenueHours, now: LocalDateTime, isOpen: Boolean, onToggle: () -> Unit) {
    val colors = CampusTheme.colors
    val venue = entry.venue
    val hours = entry.canteen?.openingHours.orEmpty()
    val stateText = stringResource(if (isOpen) R.string.aufgeklappt else R.string.zugeklappt)
    val hint = stringResource(if (isOpen) R.string.zeiten_wochenzeiten_ausblenden else R.string.zeiten_wochenzeiten_anzeigen)

    CampusCard(
        modifier = Modifier
            .animateContentSize()
            .semantics { stateDescription = "$stateText. $hint" },
        verticalPadding = 14.dp,
        onClick = onToggle,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = venue.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                )
                Text(
                    text = todayLine(venue, hours, now),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            if (hours.isNotEmpty()) StatusChip(hours, now)
        }
        if (isOpen) WeekHours(venue, hours)
    }
}

@Composable
private fun todayLine(venue: MensaVenue, hours: List<OpeningHour>, now: LocalDateTime): String {
    if (hours.isEmpty()) return "–"
    val line = MensaOpeningHours.range(now.dayOfWeek, hours)?.let { stringResource(R.string.zeiten_heute, it) }
        ?: stringResource(R.string.zeiten_heute_geschlossen)
    return if (venue.hasMealtimes) stringResource(R.string.zeiten_mit_abendessen, line) else line
}

@Composable
private fun StatusChip(hours: List<OpeningHour>, now: LocalDateTime) {
    when (MensaOpeningHours.state(hours, now)) {
        MensaOpeningHours.State.Open -> CampusChip(stringResource(R.string.status_geoeffnet), variant = ChipVariant.Green)
        MensaOpeningHours.State.ClosingSoon -> CampusChip(stringResource(R.string.status_gleich_zu), variant = ChipVariant.Warning)
        MensaOpeningHours.State.Closed -> CampusChip(stringResource(R.string.status_geschlossen), variant = ChipVariant.DangerSoft)
    }
}

@Composable
private fun WeekHours(venue: MensaVenue, hours: List<OpeningHour>) {
    val colors = CampusTheme.colors
    val closed = stringResource(R.string.zeiten_geschlossen)
    val rows = MensaOpeningHours.weekRows(hours).map { MensaOpeningHours.label(it) to (it.range ?: closed) }
    val dinner = if (venue.hasMealtimes) MensaOpeningHours.dinnerHours(hours) else null
    val dinnerRow = dinner?.let {
        val value = if (it.fridayCloses != null) {
            stringResource(R.string.zeiten_abendessen_bis_freitag, it.closes, it.fridayCloses)
        } else {
            stringResource(R.string.zeiten_abendessen_bis, it.closes)
        }
        stringResource(R.string.abendessen) to value
    }

    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = colors.separator)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (rows + listOfNotNull(dinnerRow)).forEach { (label, value) ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

// MARK: - Vorschauen

private fun canteen(name: String, weekday: Pair<String, String>, friday: Pair<String, String>? = null, saturday: Pair<String, String>? = null): Canteen {
    val hours = listOf("mon", "tue", "wed", "thu").map { OpeningHour(it, weekday.first, weekday.second) } +
        OpeningHour("fri", (friday ?: weekday).first, (friday ?: weekday).second) +
        listOfNotNull(saturday?.let { OpeningHour("sat", it.first, it.second) })
    return Canteen(null, name, hasMenu = true, city = "Ilmenau", address = null, openingHours = hours)
}

private val PreviewCanteens = listOf(
    canteen("Mensa Ehrenberg", "11:00" to "14:00"),
    canteen("Cafeteria Mensa Ehrenberg", "08:00" to "20:00", "08:00" to "19:30", "11:30" to "14:00"),
    canteen("Cafeteria NANOteria", "11:00" to "14:00"),
    canteen("Cafeteria Röntgenbau", "10:30" to "14:00"),
)

private val PreviewState = MensaOpeningHoursState.Loaded(
    MensaVenue.entries.map { venue -> VenueHours(venue, PreviewCanteens.firstOrNull { venue.matches(it.name) }) },
)

@Preview(name = "Öffnungszeiten Light", showBackground = true, heightDp = 800)
@Composable
private fun OpeningHoursLightPreview() {
    CampusIlmenauTheme(darkTheme = false) {
        MensaOpeningHoursContent(
            PreviewState, LocalDateTime.of(2026, 11, 4, 12, 15), onBack = {}, onRetry = {},
            initiallyOpen = MensaVenue.CafeteriaEhrenberg,
        )
    }
}

@Preview(name = "Öffnungszeiten Dark", showBackground = true, heightDp = 800, backgroundColor = 0xFF0E1A2E)
@Composable
private fun OpeningHoursDarkPreview() {
    CampusIlmenauTheme(darkTheme = true) {
        MensaOpeningHoursContent(PreviewState, LocalDateTime.of(2026, 11, 4, 13, 45), onBack = {}, onRetry = {})
    }
}

@Preview(name = "Öffnungszeiten Fehler", showBackground = true, heightDp = 800)
@Composable
private fun OpeningHoursFailedPreview() {
    CampusIlmenauTheme(darkTheme = false) {
        MensaOpeningHoursContent(
            MensaOpeningHoursState.Failed(de.kaigothe.campusilmenau.core.network.ApiError.Network(java.io.IOException("offline"))),
            LocalDateTime.of(2026, 11, 4, 12, 15), onBack = {}, onRetry = {},
        )
    }
}
