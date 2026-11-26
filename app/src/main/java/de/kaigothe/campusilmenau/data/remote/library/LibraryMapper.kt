package de.kaigothe.campusilmenau.data.remote.library

import de.kaigothe.campusilmenau.core.time.ApiDates
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailability
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryContents
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryContentsStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryCover
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryCoverStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryItem
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryLink
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryLocation
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMatch
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMatchField
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMatchStage
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaDetail
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaType
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchResponse
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchSource
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryTableOfContents
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryTocEntry
import java.net.URI
import java.time.LocalDate
import java.time.ZoneId

fun LibrarySearchDto.toDomain(isStale: Boolean = false) = LibrarySearchResponse(
    items = data.map { it.toDomain() },
    totalCount = totalCount ?: data.size,
    page = page,
    perPage = perPage ?: data.size,
    hasMore = hasMore,
    source = source?.let(LibrarySearchSource::of),
    correctedQuery = correctedQuery.clean(),
    catalogSuggested = catalogSuggested,
    catalogChecked = catalogChecked,
    catalogTotal = catalogTotal,
    searchId = searchId.clean(),
    isStale = isStale,
)

fun MediaDto.toDomain() = media(
    id, ppn, mediaType, title, subtitle, authors, edition, year, publisher, isbns, language, subjects, rvk, onlineUrl, catalogUrl,
    abstract, links, fetchedAt, cover, match,
)

fun MediaDetailDto.toDomain() = LibraryMediaDetail(
    media = media(
        id, ppn, mediaType, title, subtitle, authors, edition, year, publisher, isbns, language, subjects, rvk, onlineUrl, catalogUrl,
        abstract, links, fetchedAt, cover, match,
    ),
    items = items.map { it.toDomain() },
    availabilityStatus = LibraryAvailabilityStatus.of(availabilityStatus),
    contentsStatus = contentsStatus?.let(LibraryContentsStatus::of),
    contents = contents?.toDomain(),
)

fun AvailabilityDto.toDomain() = LibraryAvailability(id, LibraryAvailabilityStatus.of(availabilityStatus), items.map { it.toDomain() })

fun ItemDto.toDomain() = LibraryItem(
    label = label.trim(),
    location = location?.let { LibraryLocation(it.id, it.code.clean(), it.name.trim(), it.floorPlanUrl.webUrl()) },
    status = LibraryAvailabilityStatus.of(status),
    dueDate = dueDate?.let(::parseDue),
    reserveUrl = reserveUrl.webUrl(),
    queue = queue,
)

private fun ContentsDto.toDomain() = LibraryContents(
    tableOfContents = tableOfContents?.let { toc ->
        LibraryTableOfContents(toc.text.clean(), toc.entries.map { LibraryTocEntry(it.number.clean(), it.title.trim(), it.page.clean(), it.level) }, toc.sourceUrl.webUrl())
    },
    blurb = blurb?.text.clean(),
)

@Suppress("LongParameterList")
private fun media(
    id: String, ppn: String?, mediaType: String?, title: String, subtitle: String?, authors: List<String>, edition: String?,
    year: String?, publisher: String?, isbns: List<String>, language: String?, subjects: List<String>, rvk: List<String>,
    onlineUrl: String?, catalogUrl: String?, abstract: String?, links: List<LinkDto>, fetchedAt: String?, cover: CoverDto?, match: MatchDto?,
) = LibraryMedia(
    id = id,
    ppn = ppn.clean(),
    mediaType = LibraryMediaType.of(mediaType),
    title = title.trim(),
    subtitle = subtitle.clean(),
    authors = authors.mapNotNull { it.clean() },
    edition = edition.clean(),
    year = year.clean(),
    publisher = publisher.clean(),
    isbns = isbns,
    language = language.clean(),
    subjects = subjects,
    rvk = rvk,
    onlineUrl = onlineUrl.webUrl(),
    catalogUrl = catalogUrl.webUrl(),
    abstract = abstract.clean(),
    links = links.map { LibraryLink(it.kind, it.url.webUrl(), it.mimeType) },
    fetchedAt = fetchedAt?.let(ApiDates::parse),
    cover = cover?.let { LibraryCover(LibraryCoverStatus.of(it.status), it.url.clean(), it.width, it.height) } ?: LibraryCover.Missing,
    match = match?.let { m -> LibraryMatch(m.fields.map(LibraryMatchField::of), m.stage?.let(LibraryMatchStage::of), m.snippet.clean()) },
)

/** Rückgabedatum als Zeitstempel oder als reiner Tag (dann Mitternacht Berliner Zeit). */
private fun parseDue(text: String) = ApiDates.parse(text)
    ?: runCatching { LocalDate.parse(text.trim()).atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant() }.getOrNull()

private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun String?.webUrl(): String? {
    val text = clean() ?: return null
    val uri = runCatching { URI(text) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    return if ((scheme == "http" || scheme == "https") && !uri.host.isNullOrEmpty()) text else null
}
