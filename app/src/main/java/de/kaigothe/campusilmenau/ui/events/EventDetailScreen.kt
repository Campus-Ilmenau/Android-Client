package de.kaigothe.campusilmenau.ui.events

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.events.CalendarEntry
import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventDetailDisplay
import de.kaigothe.campusilmenau.domain.model.events.EventStatus
import de.kaigothe.campusilmenau.domain.model.events.EventTimes
import de.kaigothe.campusilmenau.ui.components.BackBar
import de.kaigothe.campusilmenau.ui.components.BandsStyle
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.components.ChipVariant
import de.kaigothe.campusilmenau.ui.components.DiagonalBands
import de.kaigothe.campusilmenau.ui.components.GoatLoader
import de.kaigothe.campusilmenau.ui.components.MessageCard
import de.kaigothe.campusilmenau.ui.components.PrimaryButton
import de.kaigothe.campusilmenau.ui.components.SecondaryButton
import de.kaigothe.campusilmenau.ui.navigation.LocalToastState
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.CardShape
import de.kaigothe.campusilmenau.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate

private val HeroMinHeight = 280.dp
private val OnImage = Color.White

/** Detail einer Veranstaltung nach `EventDetail.dc.html`. Lädt `detail(id)`, die Beschreibung ist ausklappbar. */
@Composable
fun EventDetailScreen(
    id: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel<EventDetailViewModel, EventDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(id) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = LocalToastState.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val bookmarkedText = stringResource(R.string.events_toast_gemerkt)
    val unbookmarkedText = stringResource(R.string.events_toast_nicht_mehr_gemerkt)

    when (val current = state) {
        is EventDetailState.Loaded -> EventDetailContent(
            event = current.event,
            isBookmarked = current.isBookmarked,
            upcomingCount = current.upcomingCount,
            now = remember { Instant.now() },
            onBack = onBack,
            onToggleBookmark = {
                toast.show(if (current.isBookmarked) unbookmarkedText else bookmarkedText)
                viewModel.toggleBookmark()
            },
            onAllFromOrganizer = {
                viewModel.showAllFromOrganizer(current.event)
                onBack()
            },
            onOpenUrl = { url -> runCatching { uriHandler.openUri(url) } },
            onOpenMap = { address -> openMap(context, address) },
            onAddToCalendar = { addToCalendar(context, CalendarEntry.of(current.event)) },
            modifier = modifier,
        )

        else -> Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
            BackBar(onBack = onBack, description = stringResource(R.string.events_detail_zurueck))
            when (current) {
                EventDetailState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GoatLoader(size = 96.dp)
                }

                EventDetailState.NotFound -> MessageCard(
                    title = stringResource(R.string.events_nicht_gefunden_titel),
                    text = stringResource(R.string.events_nicht_gefunden_text),
                    buttonText = stringResource(R.string.zurueck),
                    onButtonClick = onBack,
                    modifier = Modifier.padding(horizontal = Spacing.ScreenEdge),
                )

                EventDetailState.Failed -> MessageCard(
                    title = stringResource(R.string.events_detail_fehler_titel),
                    text = stringResource(R.string.events_fehler_text),
                    buttonText = stringResource(R.string.erneut_versuchen),
                    onButtonClick = viewModel::retry,
                    modifier = Modifier.padding(horizontal = Spacing.ScreenEdge),
                )

                is EventDetailState.Loaded -> Unit
            }
        }
    }
}

/** Trägt die Veranstaltung über den Insert-Intent des Kalenders ein: Die App braucht dafür keine Berechtigung. */
private fun addToCalendar(context: Context, entry: CalendarEntry) {
    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, entry.title)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, entry.beginMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, entry.endMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, entry.allDay)
        .apply {
            entry.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            entry.url?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
        }
    runCatching { context.startActivity(intent) }
}

/** Sucht die Adresse in der Karten-App des Geräts (`geo:`-Suche, keine Koordinaten). */
private fun openMap(context: Context, address: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address)))) }
}

/** Oberfläche ohne ViewModel, damit sie sich in Vorschauen in jeder Variante zeigen lässt. */
@Composable
fun EventDetailContent(
    event: CampusEvent,
    isBookmarked: Boolean,
    upcomingCount: Int?,
    now: Instant,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onAllFromOrganizer: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onAddToCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val display = EventDetailDisplay(event)
    val colors = CampusTheme.colors
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Hero(display)
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StatusNote(display)
                InfoCard(
                    display = display,
                    now = now,
                    upcomingCount = upcomingCount,
                    canShowAll = event.organizerId != null,
                    onAllFromOrganizer = onAllFromOrganizer,
                    onOpenMap = onOpenMap,
                )
                display.description?.let { Description(it) }
                Actions(display, onOpenUrl = onOpenUrl, onAddToCalendar = onAddToCalendar)
                Text(
                    text = stringResource(R.string.events_angaben_hinweis),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp),
                )
            }
        }

        // Runde weiße Tasten. Sie bleiben in beiden Modi weiß auf dem Bild.
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.events_detail_zurueck), onClick = onBack)
            Box(modifier = Modifier.weight(1f))
            // Das Teilen der Veranstaltung kommt mit M8 hierher, links vom Stern.
            RoundButton(
                icon = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                description = stringResource(if (isBookmarked) R.string.events_nicht_mehr_merken else R.string.events_merken),
                tint = if (isBookmarked) colors.accent else colors.brandNight,
                onClick = onToggleBookmark,
            )
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = CampusTheme.colors.brandNight) {
    Box(
        modifier = Modifier
            .size(Spacing.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.92f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

// MARK: - Kopf

/** Das Plakat mit Verlauf und Titel; ohne Bild oder solange es lädt der `DiagonalBands`-Baustein. */
@Composable
private fun Hero(display: EventDetailDisplay) {
    val colors = CampusTheme.colors
    val event = display.event
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = HeroMinHeight).clipToBounds()) {
        Box(modifier = Modifier.matchParentSize()) {
            val imageUrl = event.imageUrl
            if (imageUrl != null) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                    loading = { Box(modifier = Modifier.matchParentSize()) { DiagonalBands(style = BandsStyle.Event, height = null, modifier = Modifier.matchParentSize()) } },
                    error = { Box(modifier = Modifier.matchParentSize()) { DiagonalBands(style = BandsStyle.Event, height = null, modifier = Modifier.matchParentSize()) } },
                )
            } else {
                DiagonalBands(style = BandsStyle.Event, height = null, modifier = Modifier.matchParentSize())
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
                    .background(Brush.verticalGradient(listOf(colors.brandNight.copy(alpha = 0f), colors.brandNight.copy(alpha = 0.78f)))),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = if (event.imageUrl == null) 80.dp else 20.dp, top = 64.dp, bottom = 22.dp)
                .align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (display.cancelled) {
                CampusChip(text = stringResource(R.string.events_chip_faellt_aus), variant = ChipVariant.DangerSoft)
            } else if (display.tentative) {
                Text(
                    text = stringResource(R.string.events_chip_unbestaetigt),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.brandNight,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.92f), CircleShape).padding(horizontal = 9.dp, vertical = 3.dp),
                )
            }
            display.organizer?.let {
                Text(
                    text = it.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = OnImage.copy(alpha = 0.9f),
                )
            }
            Text(
                text = event.title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = display.titleSize.sp,
                    lineHeight = (display.titleSize + 6).sp,
                    textDecoration = if (display.cancelled) TextDecoration.LineThrough else TextDecoration.None,
                ),
                color = OnImage.copy(alpha = if (display.cancelled) 0.85f else 1f),
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}

// MARK: - Hinweis

@Composable
private fun StatusNote(display: EventDetailDisplay) {
    val colors = CampusTheme.colors
    val shape = RoundedCornerShape(14.dp)
    if (display.cancelled) {
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.dangerSoft, shape).padding(horizontal = 14.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Cancel, contentDescription = null, tint = colors.danger, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.events_chip_faellt_aus),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.danger,
                )
                Text(text = stringResource(R.string.events_hinweis_abgesagt), style = MaterialTheme.typography.bodyMedium, color = colors.danger)
            }
        }
    } else if (display.tentative) {
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surface, shape).padding(horizontal = 14.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(24.dp))
            Text(text = stringResource(R.string.events_hinweis_unbestaetigt), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

// MARK: - Infokarte

@Composable
private fun InfoCard(
    display: EventDetailDisplay,
    now: Instant,
    upcomingCount: Int?,
    canShowAll: Boolean,
    onAllFromOrganizer: () -> Unit,
    onOpenMap: (String) -> Unit,
) {
    val colors = CampusTheme.colors
    val (date, time) = EventTimes.dateAndTime(display.event, now)
    Column(modifier = Modifier.fillMaxWidth().background(colors.surface, CardShape).padding(horizontal = 16.dp, vertical = 4.dp)) {
        InfoRow(Icons.Outlined.CalendarMonth, mergeAll = true) {
            Text(text = date, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
            Text(text = time.asString(), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        display.location?.let { place ->
            HorizontalDivider(color = colors.separator)
            InfoRow(Icons.Outlined.Place) {
                Text(text = place, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                display.address?.let { address ->
                    Text(text = address, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    LinkText(text = stringResource(R.string.events_in_karten) + " ↗", onClick = { onOpenMap(address) })
                }
            }
        }
        display.organizer?.let { organizer ->
            HorizontalDivider(color = colors.separator)
            InfoRow(Icons.Outlined.Groups) {
                Text(text = organizer, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
                if (canShowAll) {
                    val title = if (upcomingCount != null) {
                        stringResource(R.string.events_alle_n_kommenden, upcomingCount, organizer)
                    } else {
                        stringResource(R.string.events_alle_kommenden, organizer)
                    }
                    LinkText(text = title, onClick = onAllFromOrganizer)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, mergeAll: Boolean = false, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).then(if (mergeAll) Modifier.semantics(mergeDescendants = true) {} else Modifier),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = CampusTheme.colors.textSecondary, modifier = Modifier.padding(top = 1.dp).size(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = CampusTheme.colors.accent,
        modifier = Modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 12.dp),
    )
}

// MARK: - Beschreibung

/** Reiner Text mit Absätzen; ab vier Zeilen gekürzt, mit „Mehr anzeigen“. */
@Composable
private fun Description(text: String) {
    val colors = CampusTheme.colors
    var expanded by rememberSaveable { mutableStateOf(false) }
    var truncated by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) truncated = it.hasVisualOverflow },
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        if (truncated || expanded) {
            LinkText(
                text = stringResource(if (expanded) R.string.events_weniger_anzeigen else R.string.events_mehr_anzeigen),
                onClick = { expanded = !expanded },
            )
        }
    }
}

// MARK: - Aktionen

@Composable
private fun Actions(display: EventDetailDisplay, onOpenUrl: (String) -> Unit, onAddToCalendar: () -> Unit) {
    val event = display.event
    Column(modifier = Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (display.showTickets) {
            event.ticketUrl?.let { ticket ->
                PrimaryButton(text = stringResource(R.string.events_tickets), onClick = { onOpenUrl(ticket) })
            }
        }
        if (display.showCalendar) {
            if (display.calendarIsPrimary) {
                PrimaryButton(text = stringResource(R.string.events_zum_kalender), onClick = onAddToCalendar)
            } else {
                SecondaryButton(text = stringResource(R.string.events_zum_kalender), onClick = onAddToCalendar)
            }
        }
        event.infoUrl?.let { info ->
            SecondaryButton(text = stringResource(R.string.events_infoseite), onClick = { onOpenUrl(info) })
        }
        if (display.showExternalHint) {
            Text(
                text = stringResource(R.string.events_extern_hinweis),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = CampusTheme.colors.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// MARK: - Vorschauen

private val PreviewDay = LocalDate.of(2026, 11, 28)
private val PreviewEvent = CampusEvent(
    id = "e1",
    title = "Semesterabschlussparty mit Live-Musik",
    start = BerlinTime.at(PreviewDay, 21 * 60),
    end = BerlinTime.at(PreviewDay.plusDays(1), 3 * 60),
    location = "Club Ilmenau",
    address = "Ehrenbergstraße 29, 98693 Ilmenau",
    organizer = "Studentenclub",
    organizerId = "club",
    description = "Zeile eins der Beschreibung.\n\nZeile zwei mit mehr Text, damit die Beschreibung gekürzt werden kann und der Link erscheint. " +
        "Noch ein Satz, der dafür sorgt, dass es mehr als vier Zeilen werden, wenn die Schrift groß genug ist.",
    ticketUrl = "https://example.org/tickets",
    infoUrl = "https://example.org/info",
)

@Composable
private fun DetailPreview(dark: Boolean, event: CampusEvent) {
    CampusIlmenauTheme(darkTheme = dark) {
        EventDetailContent(
            event = event, isBookmarked = true, upcomingCount = 4, now = BerlinTime.at(PreviewDay, 12 * 60),
            onBack = {}, onToggleBookmark = {}, onAllFromOrganizer = {}, onOpenUrl = {}, onOpenMap = {}, onAddToCalendar = {},
        )
    }
}

@Preview(name = "Event vollständig Light", showBackground = true, heightDp = 1100)
@Composable
private fun EventFullLightPreview() = DetailPreview(false, PreviewEvent)

@Preview(name = "Event vollständig Dark", showBackground = true, heightDp = 1100, backgroundColor = 0xFF0E1A2E)
@Composable
private fun EventFullDarkPreview() = DetailPreview(true, PreviewEvent)

@Preview(name = "Event abgesagt", showBackground = true, heightDp = 900)
@Composable
private fun EventCancelledPreview() = DetailPreview(false, PreviewEvent.copy(status = EventStatus.Cancelled))

@Preview(name = "Event knapp", showBackground = true, heightDp = 700)
@Composable
private fun EventShortPreview() = DetailPreview(
    false,
    CampusEvent(id = "k", title = "Vortrag", start = BerlinTime.at(PreviewDay, 18 * 60)),
)
