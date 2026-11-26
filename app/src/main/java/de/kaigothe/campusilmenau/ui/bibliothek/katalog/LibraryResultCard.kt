package de.kaigothe.campusilmenau.ui.bibliothek.katalog

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailability
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMatch
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaType
import de.kaigothe.campusilmenau.domain.model.library.catalog.metaLine
import de.kaigothe.campusilmenau.domain.model.library.catalog.trimmedSubtitle
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.components.ChipVariant
import de.kaigothe.campusilmenau.ui.components.rememberReduceMotion
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.CardShape
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.platform.LocalConfiguration
import de.kaigothe.campusilmenau.core.time.BerlinTime

/** Farbe des Badges: Ausleihbar und Online grün, Nur vor Ort blau, sonst neutral. */
fun LibraryAvailabilityStatus.chipVariant(): ChipVariant = when (this) {
    LibraryAvailabilityStatus.Available, LibraryAvailabilityStatus.Online -> ChipVariant.Green
    LibraryAvailabilityStatus.ReferenceOnly -> ChipVariant.Blue
    LibraryAvailabilityStatus.OnLoan, LibraryAvailabilityStatus.Unknown -> ChipVariant.Neutral
}

/** „Ausgeliehen bis 12.03.2026“ mit Datum, falls bekannt. */
@Composable
fun LibraryAvailabilityStatus.displayText(dueDate: Instant? = null): String = when (this) {
    LibraryAvailabilityStatus.Available -> stringResource(R.string.bib_status_verfuegbar)
    LibraryAvailabilityStatus.OnLoan ->
        if (dueDate != null) stringResource(R.string.bib_status_ausgeliehen_bis, shortDate(dueDate)) else stringResource(R.string.bib_status_ausgeliehen)
    LibraryAvailabilityStatus.ReferenceOnly -> stringResource(R.string.bib_status_vor_ort)
    LibraryAvailabilityStatus.Online -> stringResource(R.string.bib_status_online)
    LibraryAvailabilityStatus.Unknown -> stringResource(R.string.bib_status_unbekannt)
}

@Composable
private fun shortDate(instant: Instant): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    return formatter.format(BerlinTime.date(instant))
}

@Composable
fun LibraryMediaType.displayName(): String = stringResource(
    when (this) {
        LibraryMediaType.PrintBook -> R.string.bib_medium_buch
        LibraryMediaType.Ebook -> R.string.bib_medium_ebook
        LibraryMediaType.Journal -> R.string.bib_medium_zeitschrift
        LibraryMediaType.Other -> R.string.bib_medium_andere
    },
)

/** Symbol im Platzhalter des Covers. */
fun LibraryMediaType.placeholderIcon(): ImageVector = when (this) {
    LibraryMediaType.PrintBook -> Icons.AutoMirrored.Outlined.MenuBook
    LibraryMediaType.Ebook -> Icons.Outlined.Tablet
    LibraryMediaType.Journal -> Icons.AutoMirrored.Outlined.Article
    LibraryMediaType.Other -> Icons.Outlined.Description
}

/** „Personen · Jahr · Medienart“. */
@Composable
fun LibraryMedia.metaText(): String = metaLine(typeName = mediaType.displayName(), more = stringResource(R.string.bib_kat_u_a))

/**
 * Karte eines Treffers: Cover oder Platzhalter, Titel, Untertitel, „Personen · Jahr · Medienart“, Status-Badge und bei
 * Treffern aus dem Gesamtkatalog der Chip „Gesamtkatalog“. [availability] ist `null`, solange der Status lädt.
 */
@Composable
fun LibraryResultCard(
    media: LibraryMedia,
    availability: LibraryAvailability?,
    fromCatalog: Boolean,
    searchText: String,
    cover: ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CampusTheme.colors
    val statusText = availability?.status?.displayText(availability.earliestDueDate)
    val description = "${media.title}, ${statusText ?: stringResource(R.string.bib_status_laedt)}"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LibraryCover(media = media, image = cover)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            media.trimmedSubtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Text(
                text = media.metaText(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (availability != null && statusText != null) {
                    CampusChip(text = statusText, variant = availability.status.chipVariant())
                } else {
                    SkeletonPill()
                }
                if (fromCatalog) CampusChip(text = stringResource(R.string.bib_kat_chip_gesamtkatalog), variant = ChipVariant.Neutral)
            }
            val snippet = media.match?.snippet
            if (!snippet.isNullOrEmpty()) {
                LibrarySnippet(match = media.match, snippet = snippet, searchText = searchText, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/** Cover 48 × 68 oder Platzhalter mit Symbol je Medienart (der Normalfall, es gibt nur wenige Cover). */
@Composable
fun LibraryCover(media: LibraryMedia, image: ImageBitmap?, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 68.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(width = 48.dp, height = 68.dp))
        } else {
            Icon(media.mediaType.placeholderIcon(), contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(22.dp))
        }
    }
}

/** Grauer Platzhalter für den Status, bis die Verfügbarkeit da ist. Pulsiert, außer bei „Bewegung reduzieren“. */
@Composable
fun SkeletonPill(modifier: Modifier = Modifier, width: androidx.compose.ui.unit.Dp = 78.dp, height: androidx.compose.ui.unit.Dp = 20.dp) {
    val reduce = rememberReduceMotion()
    val alpha = if (reduce) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val value by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "skeletonAlpha",
        )
        value
    }
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .alpha(alpha)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/** Karte aus grauen Flächen, während gesucht wird. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CampusTheme.colors.surface)
            .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(width = 48.dp, height = 68.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        Column(modifier = Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonPill(width = 200.dp, height = 20.dp)
            SkeletonPill(width = 120.dp, height = 14.dp)
            SkeletonPill()
        }
    }
}

/** Grauer Ausschnitt der Inhaltssuche: „Im Inhaltsverzeichnis … **Treffer** …“. */
@Composable
fun LibrarySnippet(match: LibraryMatch?, snippet: String, searchText: String, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val label = when {
        match?.isInTableOfContents == true -> stringResource(R.string.bib_kat_snippet_toc)
        match?.isInBlurb == true -> stringResource(R.string.bib_kat_snippet_blurb)
        else -> stringResource(R.string.bib_kat_snippet_text)
    }
    val strong = SpanStyle(fontWeight = FontWeight.Bold, color = colors.textPrimary)
    val text = remember(snippet, searchText, strong) { highlighted(snippet, searchText, strong) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = colors.textTertiary)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
    }
}

/** Hebt die Suchwörter im Ausschnitt fett hervor (ohne auf Groß- und Kleinschreibung zu achten). */
fun highlighted(snippet: String, term: String, style: SpanStyle) = buildAnnotatedString {
    val words = term.split(Regex("\\s+")).filter { it.length > 1 }
    val marked = BooleanArray(snippet.length)
    for (word in words) {
        var from = 0
        while (true) {
            val at = snippet.indexOf(word, from, ignoreCase = true)
            if (at < 0) break
            for (i in at until at + word.length) marked[i] = true
            from = at + word.length
        }
    }
    var i = 0
    while (i < snippet.length) {
        var j = i
        while (j < snippet.length && marked[j] == marked[i]) j++
        if (marked[i]) withStyle(style) { append(snippet.substring(i, j)) } else append(snippet.substring(i, j))
        i = j
    }
}
