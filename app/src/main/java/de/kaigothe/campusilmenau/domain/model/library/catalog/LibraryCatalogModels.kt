package de.kaigothe.campusilmenau.domain.model.library.catalog

import java.time.Instant

// Modelle der Katalog-API (Pfade unter /v1/library). Ohne Anmeldung, nur lesend. Alle Codes haben einen Auffangwert
// (`Other`, `Unknown`), damit ein neuer Wert der API nichts abbricht.

enum class LibraryMediaType(val code: String?) {
    PrintBook("print_book"), Ebook("ebook"), Journal("journal"), Other(null);

    companion object {
        fun of(code: String?) = entries.firstOrNull { it.code != null && it.code == code } ?: Other
    }
}

enum class LibraryAvailabilityStatus(val code: String?) {
    Available("available"), OnLoan("on_loan"), ReferenceOnly("reference_only"), Online("online"), Unknown(null);

    companion object {
        fun of(code: String?) = entries.firstOrNull { it.code != null && it.code == code } ?: Unknown
    }
}

enum class LibraryCoverStatus(val code: String?) {
    Found("found"), Pending("pending"), Absent("none"), Disabled("disabled"), Other(null);

    companion object {
        fun of(code: String?) = entries.firstOrNull { it.code != null && it.code == code } ?: Other
    }
}

/** Woher die Treffer einer Suche stammen. */
enum class LibrarySearchSource(val code: String?) {
    /** Nur Bestand der UB Ilmenau. */
    Local("local"),

    /** Wenig im Bestand, ergänzt aus dem Gesamtkatalog. */
    Mixed("mixed"),

    /** Gesamtkatalog. */
    Catalog("catalog"),
    Other(null);

    companion object {
        fun of(code: String?) = entries.firstOrNull { it.code != null && it.code == code } ?: Other
    }
}

/** Wie ein Treffer gefunden wurde (`match.stage`). */
enum class LibraryMatchStage(val code: String?) {
    Exact("exact"), Corrected("corrected"), Relaxed("relaxed"), Catalog("catalog"), Other(null);

    companion object {
        fun of(code: String?) = entries.firstOrNull { it.code != null && it.code == code } ?: Other
    }
}

enum class LibraryMatchField(val code: String?) {
    Title("title"), Author("author"), Subject("subject"), Abstract("abstract"), TableOfContents("toc"), Blurb("blurb"), Other(null);

    companion object {
        fun of(code: String?) = entries.firstOrNull { it.code != null && it.code == code } ?: Other
    }
}

enum class LibraryContentsStatus(val code: String?) {
    Available("available"), Pending("pending"), Unavailable("unavailable"), Disabled("disabled"), Other(null);

    companion object {
        fun of(code: String?) = entries.firstOrNull { it.code != null && it.code == code } ?: Other
    }
}

/** `url` ist ein Pfad relativ zur Basis-URL oder eine absolute Adresse. */
data class LibraryCover(val status: LibraryCoverStatus, val url: String?, val width: Int? = null, val height: Int? = null) {
    /** Nur bei `found` oder `pending` lohnt ein Abruf. */
    val isFetchable: Boolean get() = url != null && (status == LibraryCoverStatus.Found || status == LibraryCoverStatus.Pending)

    companion object {
        val Missing = LibraryCover(LibraryCoverStatus.Absent, null)
    }
}

data class LibraryLink(val kind: String, val url: String?, val mimeType: String?)

/** Warum ein Treffer passt. Bei `in=catalog` gibt es kein `match`. */
data class LibraryMatch(val fields: List<LibraryMatchField>, val stage: LibraryMatchStage?, val snippet: String?) {
    val isInTableOfContents: Boolean get() = LibraryMatchField.TableOfContents in fields
    val isInBlurb: Boolean get() = LibraryMatchField.Blurb in fields
}

/** Ein Titel aus Suche oder Detail. IDs haben die Form `med_<PPN>`. */
data class LibraryMedia(
    val id: String,
    val ppn: String? = null,
    val mediaType: LibraryMediaType = LibraryMediaType.Other,
    val title: String,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val edition: String? = null,
    val year: String? = null,
    val publisher: String? = null,
    val isbns: List<String> = emptyList(),
    /** MARC-Sprachcode, zum Beispiel `ger`. */
    val language: String? = null,
    val subjects: List<String> = emptyList(),
    val rvk: List<String> = emptyList(),
    val onlineUrl: String? = null,
    val catalogUrl: String? = null,
    val abstract: String? = null,
    val links: List<LibraryLink> = emptyList(),
    val fetchedAt: Instant? = null,
    val cover: LibraryCover = LibraryCover.Missing,
    val match: LibraryMatch? = null,
) {
    /** Der Treffer ist als Gesamtkatalog-Treffer markiert (nur bei gemischten Antworten). */
    val isMarkedAsCatalogHit: Boolean get() = match?.stage == LibraryMatchStage.Catalog
}

/** Antwort von `/v1/library/search`. */
data class LibrarySearchResponse(
    val items: List<LibraryMedia>,
    val totalCount: Int,
    val page: Int,
    val perPage: Int,
    val hasMore: Boolean,
    /** Fehlt bei der Feldsuche, sie läuft immer im Gesamtkatalog. */
    val source: LibrarySearchSource?,
    val correctedQuery: String?,
    /** Der Verbund hat nicht rechtzeitig geantwortet und wird zum Weitersuchen angeboten. */
    val catalogSuggested: Boolean,
    val catalogChecked: Boolean,
    val catalogTotal: Int?,
    /** Nur bis zur nächsten Suche aufheben und beim Öffnen eines Titels mitgeben. */
    val searchId: String?,
    /** `X-Cache-Status: stale`: Die Antwort kam aus dem Zwischenspeicher, weil der Katalog langsam war. */
    val isStale: Boolean = false,
) {
    /** Bei `source == catalog` alle Treffer, bei `mixed` die mit `match.stage == catalog`. */
    fun isFromCatalog(media: LibraryMedia) = source == LibrarySearchSource.Catalog || media.isMarkedAsCatalogHit

    /** Älteste Abrufzeit der Treffer, für „Stand von 10:12“ bei veralteten Antworten. */
    val oldestFetchedAt: Instant? get() = items.mapNotNull { it.fetchedAt }.minOrNull()

    /** Treffer ohne die schon gezeigten (nach „Im Gesamtkatalog suchen“ stehen frühere Treffer erneut in der Liste). */
    fun itemsExcluding(shownIds: Set<String>): List<LibraryMedia> = items.filter { it.id !in shownIds }
}

data class LibraryLocation(val id: String?, val code: String?, val name: String, val floorPlanUrl: String?)

/** Ein Exemplar im Regal. */
data class LibraryItem(
    /** Signatur. */
    val label: String,
    val location: LibraryLocation?,
    val status: LibraryAvailabilityStatus,
    val dueDate: Instant?,
    /** Link zum Vormerken im UB-Katalog, nur bei ausgeliehenen Exemplaren. */
    val reserveUrl: String?,
    val queue: Int?,
)

/** Verfügbarkeit eines Titels (`/library/availability` und Teil des Details). */
data class LibraryAvailability(val id: String, val status: LibraryAvailabilityStatus, val items: List<LibraryItem>) {
    /** Frühestes Rückgabedatum aller Exemplare. */
    val earliestDueDate: Instant? get() = items.mapNotNull { it.dueDate }.minOrNull()
    val totalQueue: Int get() = items.sumOf { it.queue ?: 0 }
    val availableCount: Int get() = items.count { it.status == LibraryAvailabilityStatus.Available }
}

data class LibraryTocEntry(val number: String?, val title: String, val page: String?, val level: Int)

data class LibraryTableOfContents(val text: String?, val entries: List<LibraryTocEntry>, val sourceUrl: String?)

/** Inhaltsverzeichnis und Klappentext. Wird nicht dauerhaft gespeichert. */
data class LibraryContents(val tableOfContents: LibraryTableOfContents?, val blurb: String?) {
    val hasTableOfContents: Boolean
        get() = tableOfContents != null && (tableOfContents.entries.isNotEmpty() || !tableOfContents.text.isNullOrEmpty())
    val hasBlurb: Boolean get() = !blurb.isNullOrEmpty()
}

/** Antwort von `/v1/library/media/{id}`. */
data class LibraryMediaDetail(
    val media: LibraryMedia,
    val items: List<LibraryItem>,
    /** Gesamtstatus; ohne Exemplare meist `Unknown`. */
    val availabilityStatus: LibraryAvailabilityStatus,
    val contentsStatus: LibraryContentsStatus?,
    val contents: LibraryContents?,
) {
    /** Inhalte dürfen gezeigt werden: Status `available` und tatsächlich vorhanden. */
    val hasContents: Boolean
        get() = contentsStatus == LibraryContentsStatus.Available && contents != null && (contents.hasTableOfContents || contents.hasBlurb)

    val availability: LibraryAvailability get() = LibraryAvailability(media.id, availabilityStatus, items)
}
