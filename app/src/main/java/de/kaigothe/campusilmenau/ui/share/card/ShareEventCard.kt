package de.kaigothe.campusilmenau.ui.share.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.domain.model.events.EventStatus
import de.kaigothe.campusilmenau.domain.model.heute.HeuteClock
import de.kaigothe.campusilmenau.domain.model.share.EventShare
import de.kaigothe.campusilmenau.domain.model.share.ShareStrings
import de.kaigothe.campusilmenau.domain.model.share.ShareText
import de.kaigothe.campusilmenau.ui.theme.CampusTheme

private val CornerRadiusDp = 22.dp
private val NotchDiameter = 22.dp

/**
 * Die Fläche der Eintrittskarte: abgerundetes Rechteck (Ecken 22) mit zwei echten halbrunden Ker¾n (Ø 22) am linken und
 * rechten Rand auf Höhe [notchY] (in Pixeln). Die Ker¾n sind aus der Fläche geschnitten, dort scheint der Hintergrund durch.
 */
private fun DrawScope.ticketPath(notchY: Float): Path {
    val radius = NotchDiameter.toPx() / 2f
    val base = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(CornerRadiusDp.toPx()))) }
    val notches = Path().apply {
        addOval(Rect(Offset(0f, notchY), radius))
        addOval(Rect(Offset(size.width, notchY), radius))
    }
    return Path.combine(PathOperation.Difference, base, notches)
}

/**
 * Die Eintrittskarte: oben Veranstalter, Titel, Beginn und Datum, dann der Abriss, unten Ort und Beschreibung. Abgesagt
 * und unbestätigt haben einen Chip und einen Kasten; fehlende Angaben entfallen.
 *
 * Ein eigenes Layout setzt die Teile untereinander und gibt die Höhe des Abrisses an die Fläche dahinter weiter, damit die
 * Ker¾n schon beim ersten Zeichnen genau dort sitzen.
 */
@Composable
fun ShareEventTicket(share: EventShare, strings: ShareStrings, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    // Mitte der Abrisszeile in Pixeln; das Layout schreibt sie vor dem Zeichnen.
    val notchY = remember { floatArrayOf(0f) }

    Layout(
        content = {
            Box(modifier = Modifier.drawBehind { drawPath(ticketPath(notchY[0]), colors.surface) })
            TicketTop(share, strings)
            TearRow()
            TicketBottom(share)
        },
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(CornerRadiusDp)),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val free = Constraints(minWidth = width, maxWidth = width)
        val top = measurables[1].measure(free)
        val tear = measurables[2].measure(free)
        val bottom = measurables[3].measure(free)
        val height = top.height + tear.height + bottom.height
        notchY[0] = top.height + tear.height / 2f
        val background = measurables[0].measure(Constraints.fixed(width, height))
        layout(width, height) {
            background.place(0, 0)
            top.place(0, 0)
            tear.place(0, top.height)
            bottom.place(0, top.height + tear.height)
        }
    }
}

@Composable
private fun TicketTop(share: EventShare, strings: ShareStrings, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val event = share.event
    val cancelled = event.status == EventStatus.Cancelled
    val organizer = ShareText.clean(event.organizer) ?: ShareText.clean(event.category) ?: strings.cardEventFallbackOrganizer()
    val start = if (event.allDay) strings.cardEventAllDay() else HeuteClock.time(event.start)
    val end = event.end
    val until = when {
        event.allDay -> ""
        end != null && end > event.start && !event.isMultiDay -> strings.cardEventUntil(HeuteClock.time(end))
        else -> strings.cardEventNoEnd()
    }
    val note = when (event.status) {
        EventStatus.Cancelled -> strings.cardEventCancelledNote() to true
        EventStatus.Tentative -> strings.cardEventTentativeNote() to false
        EventStatus.Scheduled -> null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Weicher oranger Schein oben rechts: orange.500 mit 28 %, läuft bis zur Kartenmitte aus.
            .shareGlow(colors.brandOrange.copy(alpha = 0.28f), anchorX = 1f, anchorY = 0f, widthFactor = 1.2f * 0.6f, heightFactor = 0.9f * 0.6f)
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 18.dp),
    ) {
        when (event.status) {
            EventStatus.Cancelled -> ChipText(strings.cardEventCancelledChip(), colors.dangerSoft, colors.danger, Modifier.padding(bottom = 10.dp))
            // Weiße Fläche mit dunkler Schrift, wie im Artboard.
            EventStatus.Tentative -> ChipText(strings.cardEventTentativeChip(), colors.textPrimary.copy(alpha = 0.92f), colors.brandNight, Modifier.padding(bottom = 10.dp))
            EventStatus.Scheduled -> Unit
        }

        // Schrift auf Nachtblau, kein Button: `accent` ist im Dunklen hell genug.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DarkMode, contentDescription = null, tint = colors.accent, modifier = Modifier.size(13.dp))
            Text(
                text = organizer.uppercase(strings.locale),
                style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.69.sp, color = colors.accent),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = event.title,
            style = displayStyle(size = 31, lineHeight = 34, color = colors.textPrimary.copy(alpha = if (cancelled) 0.85f else 1f))
                .copy(textDecoration = if (cancelled) TextDecoration.LineThrough else TextDecoration.None),
            modifier = Modifier.padding(top = 6.dp),
        )

        Row(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = start,
                style = displayStyle(size = 46, lineHeight = 50, color = colors.textPrimary).copy(fontFeatureSettings = "tnum"),
                maxLines = 1,
                softWrap = false,
            )
            if (until.isNotEmpty()) {
                Text(
                    text = until,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary),
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
        }

        Text(
            text = share.dateText,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
            modifier = Modifier.padding(top = 6.dp),
        )

        note?.let { (text, isAlert) ->
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = if (isAlert) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isAlert) colors.danger else colors.textSecondary,
                ),
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .background(if (isAlert) colors.dangerSoft else colors.surfaceSecondary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

/** Gestrichelte Linie (2 dp) des Abrisses, 20 dp vom linken und rechten Rand entfernt. */
@Composable
private fun TearRow() {
    val color = CampusTheme.colors.separator
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(NotchDiameter)
            .drawBehind {
                val inset = 20.dp.toPx()
                drawLine(
                    color = color,
                    start = Offset(inset, size.height / 2f),
                    end = Offset(size.width - inset, size.height / 2f),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                )
            },
    )
}

@Composable
private fun TicketBottom(share: EventShare) {
    val colors = CampusTheme.colors
    val event = share.event
    val name = ShareText.clean(event.location)
    val address = ShareText.clean(event.address)
    val primary = name ?: address
    // Abgesagt: keine Beschreibung. Absätze werden zu einem Fließtext (die Karte zeigt höchstens drei Zeilen).
    val description = if (event.status == EventStatus.Cancelled) null else ShareText.clean(event.description)?.replace(Regex("\\s*\\n\\s*"), " ")

    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 18.dp)) {
        if (primary != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.width(18.dp).height(18.dp))
                Column {
                    Text(primary, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary))
                    if (name != null && address != null) {
                        Text(address, style = TextStyle(fontSize = 12.5.sp, color = colors.textSecondary), modifier = Modifier.padding(top = 1.dp))
                    }
                }
            }
        }
        if (description != null) {
            Text(
                text = description,
                style = TextStyle(fontSize = 13.5.sp, color = colors.textSecondary),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (primary == null) 0.dp else 12.dp),
            )
        }
    }
}
