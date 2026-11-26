package de.kaigothe.campusilmenau.ui.bibliothek.katalog

import android.content.ClipData
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.asString
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryContents
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryItemRow
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryLanguage
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaDetail
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryStatusBlock
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryTableOfContents
import de.kaigothe.campusilmenau.domain.model.library.catalog.editionLine
import de.kaigothe.campusilmenau.domain.model.library.catalog.facts
import de.kaigothe.campusilmenau.domain.model.library.catalog.personsLine
import de.kaigothe.campusilmenau.domain.model.library.catalog.primaryAction
import de.kaigothe.campusilmenau.domain.model.library.catalog.row
import de.kaigothe.campusilmenau.domain.model.library.catalog.shortText
import de.kaigothe.campusilmenau.domain.model.library.catalog.statusBlock
import de.kaigothe.campusilmenau.domain.model.library.catalog.trimmedSubtitle
import de.kaigothe.campusilmenau.ui.components.BackBar
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.components.GoatLoader
import de.kaigothe.campusilmenau.ui.components.PrimaryButton
import de.kaigothe.campusilmenau.ui.components.SecondaryButton
import de.kaigothe.campusilmenau.ui.navigation.LocalToastState
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalContext
import de.kaigothe.campusilmenau.ui.adaptive.LocalSplitView
import de.kaigothe.campusilmenau.domain.model.share.LibraryMediaShare
import de.kaigothe.campusilmenau.ui.share.ShareButton
import de.kaigothe.campusilmenau.ui.share.rememberShareStrings
import de.kaigothe.campusilmenau.ui.share.shareText
import de.kaigothe.campusilmenau.ui.theme.CardShape
import de.kaigothe.campusilmenau.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Ein Titel des Katalogs. Lädt erst beim Öffnen, nichts wird vorgeladen. */
@Composable
fun LibraryTitleScreen(
    id: String,
    searchId: String?,
    onBack: () -> Unit,
    onOpenMerkliste: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryTitleViewModel = hiltViewModel<LibraryTitleViewModel, LibraryTitleViewModel.Factory>(
        key = "titel-$id",
        creationCallback = { factory -> factory.create(id, searchId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = LocalToastState.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current
    val copied = stringResource(R.string.bib_titel_signatur_kopiert)
    val refreshFailed = stringResource(R.string.bib_titel_aktualisieren_fehler)

    val more = stringResource(R.string.bib_kat_u_a)
    val context = LocalContext.current
    val shareStrings = rememberShareStrings()
    val addedText = stringResource(R.string.bib_merkliste_gesetzt)
    val showText = stringResource(R.string.bib_merkliste_anzeigen)
    val removedText = stringResource(R.string.bib_merkliste_entfernt_kurz)
    LaunchedEffect(viewModel, addedText, showText, removedText) {
        viewModel.meldungen.collect { meldung ->
            when (meldung) {
                TitelMeldung.Gemerkt -> toast.show(addedText, actionLabel = showText, onAction = onOpenMerkliste)
                TitelMeldung.Entfernt -> toast.show(removedText)
            }
        }
    }
    LaunchedEffect(state.refreshFailed) {
        if (state.refreshFailed) {
            toast.show(refreshFailed)
            viewModel.refreshFailedShown()
        }
    }

    LibraryTitleContent(
        state = state,
        coverFor = viewModel::cover,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onToggleToc = viewModel::toggleToc,
        onToggleBlurb = viewModel::toggleBlurb,
        onToggleBookmark = { viewModel.toggleBookmark(more) },
        onShare = { state.detail?.let { context.shareText(LibraryMediaShare(it, shareStrings, more)) } },
        onOpenUrl = { url -> if (url.startsWith("http://", true) || url.startsWith("https://", true)) runCatching { uriHandler.openUri(url) } },
        onCopySignature = { signature ->
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(signature, signature)))
                toast.show(copied)
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTitleContent(
    state: LibraryTitleState,
    coverFor: suspend (LibraryMedia) -> Bitmap?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleToc: () -> Unit,
    onToggleBlurb: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopySignature: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Neben der Liste braucht der Titel keine Zurück-Taste.
            if (LocalSplitView.current) {
                Spacer(modifier = Modifier.weight(1f).height(Spacing.MinTouchTarget))
            } else {
                BackBar(onBack = onBack, description = stringResource(R.string.bib_titel_zurueck), modifier = Modifier.weight(1f))
            }
            if (state.detail != null) {
                ShareButton(onClick = onShare)
                val bookmarked = state.isBookmarked
                IconButton(onClick = onToggleBookmark, modifier = Modifier.padding(end = 4.dp).size(Spacing.MinTouchTarget)) {
                    Icon(
                        if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(if (bookmarked) R.string.bib_merkliste_entfernen else R.string.bib_merkliste_setzen),
                        tint = if (bookmarked) CampusTheme.colors.accent else CampusTheme.colors.textPrimary,
                    )
                }
            }
        }
        PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, top = 8.dp, bottom = 28.dp),
            ) {
                when (state.phase) {
                    TitlePhase.Loading -> Box(modifier = Modifier.fillMaxWidth().padding(top = 120.dp), contentAlignment = Alignment.Center) {
                        GoatLoader(size = 96.dp)
                    }
                    TitlePhase.NotFound -> NotFoundBlock(onBack = onBack)
                    TitlePhase.Failed -> FailedBlock(onRetry = onRefresh)
                    TitlePhase.Loaded -> state.detail?.let {
                        TitleDetail(state, it, coverFor, onToggleToc, onToggleBlurb, onOpenUrl, onCopySignature)
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberDateFormatter(): (Instant) -> String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        val format: (Instant) -> String = { formatter.format(BerlinTime.date(it)) }
        format
    }
}

@Composable
private fun TitleDetail(
    state: LibraryTitleState,
    detail: LibraryMediaDetail,
    coverFor: suspend (LibraryMedia) -> Bitmap?,
    onToggleToc: () -> Unit,
    onToggleBlurb: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopySignature: (String) -> Unit,
) {
    val colors = CampusTheme.colors
    val media = detail.media
    val date = rememberDateFormatter()
    val locale = LocalConfiguration.current.locales[0]

    Header(media = media, coverFor = coverFor)

    state.detail?.takeIf { it.isStale }?.let {
        val since = it.media.fetchedAt
        val text = if (since != null) {
            val minutes = BerlinTime.minutesOfDay(since)
            stringResource(R.string.bib_kat_stale_stand, "%02d:%02d".format(minutes / 60, minutes % 60))
        } else {
            stringResource(R.string.bib_kat_stale)
        }
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.warningSoft)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = colors.warning, modifier = Modifier.padding(top = 2.dp).size(18.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.warning)
        }
    }

    StatusBlock(detail.statusBlock(date), modifier = Modifier.padding(top = 16.dp))

    val primary = detail.primaryAction
    val catalogUrl = media.catalogUrl
    if (primary != null || catalogUrl != null) {
        val browserHint = stringResource(R.string.bib_titel_hinweis_browser_cd)
        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (primary != null) {
                PrimaryButton(
                    text = stringResource(primary.title),
                    onClick = { onOpenUrl(primary.url) },
                    trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                    modifier = Modifier.fillMaxWidth().semantics { stateDescription = browserHint },
                )
            }
            if (catalogUrl != null) {
                SecondaryButton(
                    text = stringResource(R.string.bib_titel_im_katalog),
                    onClick = { onOpenUrl(catalogUrl) },
                    trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                    modifier = Modifier.fillMaxWidth().semantics { stateDescription = browserHint },
                )
            }
            Text(
                text = stringResource(R.string.bib_titel_hinweis_browser),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
            )
        }
    }

    if (detail.items.isNotEmpty()) {
        val rows = remember(detail.items, date) { detail.items.map { it.row(date) } }
        SectionHeader(stringResource(R.string.bib_titel_exemplare, rows.size))
        CampusCard(verticalPadding = 2.dp) {
            rows.forEachIndexed { index, row ->
                ItemRow(row = row, onOpenUrl = onOpenUrl, onCopySignature = onCopySignature)
                if (index < rows.lastIndex) Divider()
            }
        }
    }

    if (state.showsContents) detail.contents?.let { ContentsSection(it, state, onToggleToc, onToggleBlurb) }

    val languageName = media.language?.let {
        LibraryLanguage.name(it, locale, multiple = stringResource(R.string.bib_sprache_mehrere), unknown = stringResource(R.string.bib_sprache_unbekannt))
    }
    FactsSection(media = media, languageName = languageName)

    Text(
        text = stringResource(R.string.bib_titel_quelle),
        style = MaterialTheme.typography.bodySmall,
        color = colors.textTertiary,
        modifier = Modifier.padding(top = 18.dp, start = 4.dp, end = 4.dp),
    )
}

// region Kopf

@Composable
private fun Header(media: LibraryMedia, coverFor: suspend (LibraryMedia) -> Bitmap?) {
    val colors = CampusTheme.colors
    val more = stringResource(R.string.bib_kat_u_a)
    val kind = listOfNotNull(media.mediaType.displayName(), media.language?.let { code ->
        LibraryLanguage.name(code, LocalConfiguration.current.locales[0], stringResource(R.string.bib_sprache_mehrere), stringResource(R.string.bib_sprache_unbekannt))
    }).filter { it.isNotEmpty() }.joinToString(" · ")
    val cover by produceState<ImageBitmap?>(null, media.id) { value = coverFor(media)?.asImageBitmap() }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
        TitleCover(media = media, image = cover)
        Column(modifier = Modifier.weight(1f).padding(bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = kind.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp),
                color = colors.textTertiary,
            )
            Text(
                text = media.title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
    media.trimmedSubtitle?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, modifier = Modifier.padding(top = 12.dp)) }
    val persons = media.personsLine(more)
    if (persons.isNotEmpty()) Text(persons, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.padding(top = 6.dp))
    val edition = media.editionLine
    if (edition.isNotEmpty()) Text(edition, style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary, modifier = Modifier.padding(top = 2.dp))
}

/** Cover 96 × 136 oder Platzhalter mit Symbol. */
@Composable
private fun TitleCover(media: LibraryMedia, image: ImageBitmap?) {
    val colors = CampusTheme.colors
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 136.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            androidx.compose.foundation.Image(
                bitmap = image,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(media.mediaType.placeholderIcon(), contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(32.dp))
        }
    }
}

// endregion

// region Status und Exemplare

@Composable
private fun StatusBlock(block: LibraryStatusBlock, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val (container, content) = when (block.tone) {
        LibraryStatusBlock.Tone.Green -> colors.green to colors.onGreen
        LibraryStatusBlock.Tone.Blue -> colors.blue to colors.onBlue
        LibraryStatusBlock.Tone.Neutral -> colors.surface to colors.textPrimary
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(container)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(block.title.asString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = content)
        Text(block.subtitle.asString(), style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.9f))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = CampusTheme.colors.textSecondary,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp, start = 4.dp, end = 4.dp).semantics { heading() },
    )
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 1.dp).background(CampusTheme.colors.separator))
}

@Composable
private fun ItemRow(row: LibraryItemRow, onOpenUrl: (String) -> Unit, onCopySignature: (String) -> Unit) {
    val colors = CampusTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Text(
                text = row.place.asString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            CampusChip(text = row.status.displayText(), variant = row.status.chipVariant())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.signature,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f, fill = false),
            )
            val description = stringResource(R.string.bib_titel_signatur_kopieren)
            // Sichtbar 30 dp hoch, die Tippfläche misst 48 dp.
            Box(
                modifier = Modifier
                    .heightIn(min = Spacing.MinTouchTarget)
                    .clickable(role = Role.Button, onClick = { onCopySignature(row.signature) })
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.bib_titel_kopieren),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    modifier = Modifier
                        .heightIn(min = 30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        row.extra?.let { Text(it.asString(), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary) }
        row.floorPlanUrl?.let { plan ->
            val hint = stringResource(R.string.bib_titel_hinweis_browser_cd)
            Row(
                modifier = Modifier
                    .heightIn(min = Spacing.MinTouchTarget)
                    .clickable(role = Role.Button, onClick = { onOpenUrl(plan) })
                    .semantics { stateDescription = hint },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.bib_titel_lageplan), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.accent)
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// endregion

// region Inhalt

@Composable
private fun ContentsSection(contents: LibraryContents, state: LibraryTitleState, onToggleToc: () -> Unit, onToggleBlurb: () -> Unit) {
    val colors = CampusTheme.colors
    SectionHeader(stringResource(R.string.bib_titel_inhalt))
    CampusCard(verticalPadding = 0.dp) {
        val toc = contents.tableOfContents
        if (contents.hasTableOfContents && toc != null) {
            Fold(title = stringResource(R.string.bib_titel_toc), isOpen = state.tocOpen, onToggle = onToggleToc) { TableOfContents(toc) }
        }
        if (contents.hasTableOfContents && contents.hasBlurb) Divider()
        val blurb = contents.blurb
        if (contents.hasBlurb && blurb != null) {
            Fold(title = stringResource(R.string.bib_titel_klappentext), isOpen = state.blurbOpen, onToggle = onToggleBlurb) {
                Text(blurb, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp))
            }
        }
    }
}

@Composable
private fun Fold(title: String, isOpen: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    val colors = CampusTheme.colors
    val stateText = stringResource(if (isOpen) R.string.bib_titel_aufgeklappt else R.string.bib_titel_zugeklappt)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.MinTouchTarget)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = stateText }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary, modifier = Modifier.weight(1f))
            Icon(if (isOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = colors.textTertiary)
        }
        if (isOpen) content()
    }
}

/** Gliederung mit Seiten; ohne Gliederung der Fließtext (gekürzt). */
@Composable
private fun TableOfContents(toc: LibraryTableOfContents) {
    val colors = CampusTheme.colors
    if (toc.entries.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            toc.entries.forEach { entry ->
                val top = entry.level <= 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (maxOf(0, entry.level - 1) * 16).dp, top = 4.dp, bottom = 4.dp)
                        .semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = listOfNotNull(entry.number, entry.title).filter { it.isNotEmpty() }.joinToString(" "),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (top) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (top) colors.textPrimary else colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    entry.page?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary) }
                }
            }
        }
    } else {
        toc.shortText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) }
    }
}

// endregion

// region Weitere Angaben

@Composable
private fun FactsSection(media: LibraryMedia, languageName: String?) {
    val facts = remember(media, languageName) { media.facts(languageName) }
    if (facts.isEmpty) return
    val colors = CampusTheme.colors
    val rows = listOfNotNull(
        facts.isbn?.let { R.string.bib_titel_f_isbn to it },
        facts.language?.let { R.string.bib_titel_f_sprache to it },
        facts.publisher?.let { R.string.bib_titel_f_verlag to it },
        facts.rvk?.let { R.string.bib_titel_f_rvk to it },
    )
    SectionHeader(stringResource(R.string.bib_titel_weitere_angaben))
    CampusCard(verticalPadding = 2.dp) {
        rows.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp).semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
                Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
            if (index < rows.lastIndex || facts.subjects.isNotEmpty() || facts.abstract != null) Divider()
        }
        if (facts.subjects.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.bib_titel_f_schlagwoerter), style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    facts.subjects.forEach { subject ->
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            if (facts.abstract != null) Divider()
        }
        facts.abstract?.let {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.bib_titel_f_beschreibung), style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
                Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            }
        }
    }
}

// endregion

@Composable
private fun NotFoundBlock(onBack: () -> Unit) {
    val colors = CampusTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.bib_titel_nicht_gefunden), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(stringResource(R.string.bib_titel_nicht_gefunden_text), style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, textAlign = TextAlign.Center)
        SecondaryButton(text = stringResource(R.string.bib_titel_zurueck), onClick = onBack, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun FailedBlock(onRetry: () -> Unit) {
    val colors = CampusTheme.colors
    CampusCard(modifier = Modifier.padding(top = 16.dp), verticalPadding = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.bib_kat_fehler_titel), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary, textAlign = TextAlign.Center)
            Text(stringResource(R.string.bib_kat_fehler_text), style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, textAlign = TextAlign.Center)
            PrimaryButton(text = stringResource(R.string.bib_kat_erneut), onClick = onRetry, modifier = Modifier.padding(top = 10.dp))
        }
    }
}
