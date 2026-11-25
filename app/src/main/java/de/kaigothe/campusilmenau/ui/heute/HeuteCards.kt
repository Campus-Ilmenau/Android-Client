package de.kaigothe.campusilmenau.ui.heute

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.domain.model.heute.HeuteClock
import de.kaigothe.campusilmenau.domain.model.heute.HeuteEvent
import de.kaigothe.campusilmenau.domain.model.heute.HeuteFarbe
import de.kaigothe.campusilmenau.domain.model.heute.HeuteJetztKarte
import de.kaigothe.campusilmenau.domain.model.heute.HeuteTermin
import de.kaigothe.campusilmenau.domain.model.heute.HeuteZeile
import de.kaigothe.campusilmenau.domain.model.heute.HeuteZiel
import de.kaigothe.campusilmenau.domain.model.heute.Hinweis
import de.kaigothe.campusilmenau.ui.components.BandsStyle
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.components.DiagonalBands
import de.kaigothe.campusilmenau.ui.mensa.title
import de.kaigothe.campusilmenau.ui.mensa.variant
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.CardShape
import java.time.Instant

private val HeroShape = RoundedCornerShape(22.dp)

/** Überschrift eines Abschnitts: 13 sp Semibold, `textSecondary`, Versalien. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        ),
        color = CampusTheme.colors.textSecondary,
        modifier = modifier.semantics { heading() },
    )
}

/** Die Jetzt-Karte: Kopf (Mensa: Blau mit grünem Band, Event: Nachtblau mit Abendsonne), Zeilen und Fußlink. */
@Composable
fun HeuteJetztKarteView(card: HeuteJetztKarte, onZiel: (HeuteZiel) -> Unit, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    Column(modifier = modifier.fillMaxWidth().clip(HeroShape).background(colors.surface)) {
        val header: @Composable () -> Unit = { CardHeader(card) }
        if (card.style == HeuteJetztKarte.Style.Mensa) {
            Box(modifier = Modifier.clickable(role = Role.Button) { onZiel(card.ziel) }) { header() }
        } else {
            header()
        }
        card.rows.forEach { row ->
            HorizontalDivider(color = colors.separator)
            val target = row.ziel
            if (target != null) {
                Box(modifier = Modifier.clickable(role = Role.Button) { onZiel(target) }) { RowContent(row) }
            } else {
                RowContent(row)
            }
        }
        HorizontalDivider(color = colors.separator)
        Text(
            text = card.linkText.asString(),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accent,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { onZiel(card.ziel) }
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun CardHeader(card: HeuteJetztKarte) {
    val colors = CampusTheme.colors
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp)
                .semantics(mergeDescendants = true) { heading() },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Das grüne Band beginnt oben bei 78 % der Breite, der Text bleibt links davon (weißer Text auf Grün
            // hätte zu wenig Kontrast).
            card.kicker?.let {
                Text(
                    text = it.asString().uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                    ),
                    color = colors.onBlue.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(0.70f),
                )
            }
            Text(
                text = card.title.asString(),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 22.sp, lineHeight = 28.sp),
                color = colors.onBlue,
                modifier = Modifier.fillMaxWidth(0.58f).padding(top = 2.dp),
            )
            card.subtitle?.asString()?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onBlue.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(0.58f),
                )
            }
        }
    }
    when (card.style) {
        HeuteJetztKarte.Style.Mensa -> DiagonalBands(style = BandsStyle.Hero, height = null) { content() }
        HeuteJetztKarte.Style.Event -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.brandNight)
                .clipToBounds()
                .drawBehind {
                    // Abendsonne: rechts unten angeschnitten, Mittelpunkt bei 92 % Breite und 120 % Höhe.
                    val radius = 70.dp.toPx()
                    val center = Offset(size.width * 0.92f, size.height * 1.2f)
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(colors.brandOrange, colors.brandYellow),
                            startY = center.y - radius,
                            endY = center.y + radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                },
        ) { content() }
    }
}

/** Eine Zeile: Gericht (bis 2 Zeilen), Chips, rechts der Preis. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowContent(row: HeuteZeile) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = row.name.asString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.chips.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.chips.forEach { CampusChip(text = it.title(), variant = it.tone.variant()) }
                }
            }
        }
        row.right?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 1,
            )
        }
    }
}

/** Ladezustand der Jetzt-Karte: Flächen in der Form der Karte, ohne Inhalt. */
@Composable
fun HeuteJetztPlatzhalter(modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val description = stringResource(R.string.heute_wird_geladen)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .background(colors.surface)
            .semantics { contentDescription = description },
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(96.dp).background(colors.blue.copy(alpha = 0.35f)))
        repeat(3) {
            HorizontalDivider(color = colors.separator)
            Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).background(colors.surfaceSecondary, CircleShape))
            }
        }
        HorizontalDivider(color = colors.separator)
        Spacer48()
    }
}

@Composable
private fun Spacer48() = Box(modifier = Modifier.height(48.dp))

/** Abschnitt „Als Nächstes“: eine Karte mit Zeilen aus Zeit, Farbpunkt, Titel, Unterzeile und Pfeil. */
@Composable
fun HeuteAlsNaechstesView(appointments: List<HeuteTermin>, onZiel: (HeuteZiel) -> Unit, modifier: Modifier = Modifier) {
    if (appointments.isEmpty()) return
    val colors = CampusTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(stringResource(R.string.heute_als_naechstes))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, CardShape)
                .padding(horizontal = 16.dp),
        ) {
            appointments.forEachIndexed { index, appointment ->
                AppointmentRow(appointment, onZiel)
                if (index < appointments.lastIndex) {
                    // Eingerückt bis zum Titel.
                    HorizontalDivider(color = colors.separator, modifier = Modifier.padding(start = 52.dp + 8.dp + 24.dp))
                }
            }
        }
    }
}

@Composable
private fun AppointmentRow(appointment: HeuteTermin, onZiel: (HeuteZiel) -> Unit) {
    val colors = CampusTheme.colors
    val title = appointment.title.asString()
    val subtitle = appointment.subtitle.asString()
    val description = listOf(appointment.time, title, subtitle).filter { it.isNotEmpty() }.joinToString(", ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button) { onZiel(appointment.ziel) }
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = appointment.time,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            modifier = Modifier.width(52.dp),
        )
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .background(if (appointment.color == HeuteFarbe.Blue) colors.blue else colors.brandOrange, CircleShape),
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                )
                if (subtitle.isNotEmpty()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Abschnitt „Heute Abend“: bis zu 3 Veranstaltungen als waagerecht scrollende Karten. Ohne Einträge (auch bei Fehler oder
 * Zeitablauf) entfällt der Abschnitt ganz, es gibt keinen Leer-Zustand.
 */
@Composable
fun HeuteAbendKarte(
    events: List<HeuteEvent>,
    now: Instant,
    onAlle: () -> Unit,
    onOeffne: (HeuteEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (events.isEmpty()) return
    val colors = CampusTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.heute_abend_titel), modifier = Modifier.weight(1f))
            val allDescription = stringResource(R.string.heute_alle_abend_beschreibung)
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onAlle)
                    .semantics { contentDescription = allDescription }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.heute_alle),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent,
                )
            }
        }
        // Die Karten laufen bis an den Bildschirmrand, der erste Eintrag beginnt am 16-dp-Rand der Seite.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(events, key = { it.id }) { event ->
                EventCard(event = event, now = now, onClick = { onOeffne(event) })
            }
        }
    }
}

@Composable
private fun EventCard(event: HeuteEvent, now: Instant, onClick: () -> Unit) {
    val colors = CampusTheme.colors
    val running = event.start <= now
    val clock = HeuteClock.time(event.start)
    val time = if (running) stringResource(R.string.heute_event_laeuft_seit, clock) else stringResource(R.string.heute_event_heute_um, clock)
    val who = event.organizer ?: event.location?.trim()?.takeIf { it.isNotEmpty() }
    val description = listOfNotNull(time, event.title, who).joinToString(", ")
    Column(
        modifier = Modifier
            .width(220.dp)
            .background(colors.surface, CardShape)
            .clip(CardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = time.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = colors.accent,
        )
        Text(
            text = event.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (who != null) {
            Text(text = who, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

/** Redaktioneller Hinweis: Icon-Fläche, Kategorie und Zeit in `accent`, Titel, eine Zeile, ✕ zum dauerhaften Ausblenden. */
@Composable
fun HeuteHinweisKarte(notice: Hinweis, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val blue = notice.tone == Hinweis.Tone.Blue
    val kicker = listOfNotNull(notice.category, notice.timeLabel).filter { it.isNotEmpty() }.joinToString(" · ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, CardShape)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(if (blue) colors.blue else colors.green, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = if (blue) colors.onBlue else colors.onGreen,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = kicker,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = colors.accent,
            )
            Text(
                text = notice.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            if (notice.text.isNotEmpty()) {
                Text(text = notice.text, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        val dismiss = stringResource(R.string.heute_hinweis_ausblenden)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onDismiss)
                .semantics { contentDescription = dismiss },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Outlined.Close, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}
