package de.kaigothe.campusilmenau.data.remote.library

import kotlinx.serialization.Serializable

/**
 * Antwortmodelle der Katalog-API. Tolerant: Bis auf `id` und `title` hat jedes Feld einen Standardwert, Codes, Daten und
 * Links bleiben Text (der Mapper räumt auf, ein kaputter Wert verliert nur sein Feld). Schlüssel als camelCase.
 */
@Serializable
data class LibrarySearchDto(
    val data: List<MediaDto> = emptyList(),
    val totalCount: Int? = null,
    val page: Int = 1,
    val perPage: Int? = null,
    val hasMore: Boolean = false,
    val source: String? = null,
    val correctedQuery: String? = null,
    val catalogSuggested: Boolean = false,
    val catalogChecked: Boolean = false,
    val catalogTotal: Int? = null,
    val searchId: String? = null,
)

@Serializable
data class CoverDto(val status: String? = null, val url: String? = null, val width: Int? = null, val height: Int? = null)

@Serializable
data class LinkDto(val kind: String = "", val url: String? = null, val mimeType: String? = null)

@Serializable
data class MatchDto(val fields: List<String> = emptyList(), val stage: String? = null, val snippet: String? = null)

@Serializable
data class MediaDto(
    val id: String,
    val ppn: String? = null,
    val mediaType: String? = null,
    val title: String,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val edition: String? = null,
    val year: String? = null,
    val publisher: String? = null,
    val isbns: List<String> = emptyList(),
    val language: String? = null,
    val subjects: List<String> = emptyList(),
    val rvk: List<String> = emptyList(),
    val onlineUrl: String? = null,
    val catalogUrl: String? = null,
    val abstract: String? = null,
    val links: List<LinkDto> = emptyList(),
    val fetchedAt: String? = null,
    val cover: CoverDto? = null,
    val match: MatchDto? = null,
)

@Serializable
data class LocationDto(val id: String? = null, val code: String? = null, val name: String, val floorPlanUrl: String? = null)

@Serializable
data class ItemDto(
    val label: String = "",
    val location: LocationDto? = null,
    val status: String? = null,
    val dueDate: String? = null,
    val reserveUrl: String? = null,
    val queue: Int? = null,
)

@Serializable
data class AvailabilityDto(val id: String, val availabilityStatus: String? = null, val items: List<ItemDto> = emptyList())

@Serializable
data class AvailabilityListDto(val data: List<AvailabilityDto> = emptyList())

@Serializable
data class TocEntryDto(val number: String? = null, val title: String, val page: String? = null, val level: Int = 1)

@Serializable
data class TocDto(val text: String? = null, val entries: List<TocEntryDto> = emptyList(), val sourceUrl: String? = null)

@Serializable
data class BlurbDto(val text: String? = null)

@Serializable
data class ContentsDto(val tableOfContents: TocDto? = null, val blurb: BlurbDto? = null)

/** `/media/{id}`: die Felder des Titels (flach) plus Exemplare, Gesamtstatus und Inhalte. */
@Serializable
data class MediaDetailDto(
    val id: String,
    val ppn: String? = null,
    val mediaType: String? = null,
    val title: String,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val edition: String? = null,
    val year: String? = null,
    val publisher: String? = null,
    val isbns: List<String> = emptyList(),
    val language: String? = null,
    val subjects: List<String> = emptyList(),
    val rvk: List<String> = emptyList(),
    val onlineUrl: String? = null,
    val catalogUrl: String? = null,
    val abstract: String? = null,
    val links: List<LinkDto> = emptyList(),
    val fetchedAt: String? = null,
    val cover: CoverDto? = null,
    val match: MatchDto? = null,
    val items: List<ItemDto> = emptyList(),
    val availabilityStatus: String? = null,
    val contentsStatus: String? = null,
    val contents: ContentsDto? = null,
)

@Serializable
data class FeaturesDto(val contents: Boolean = false)
