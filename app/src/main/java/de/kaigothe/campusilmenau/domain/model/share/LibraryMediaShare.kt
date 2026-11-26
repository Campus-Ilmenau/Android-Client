package de.kaigothe.campusilmenau.domain.model.share

import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryItem
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaDetail
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryPersonName
import de.kaigothe.campusilmenau.domain.model.library.catalog.trimmedSubtitle

/**
 * Ein Medium aus dem Katalog als Text. Ohne Verfügbarkeit (die ändert sich), ohne Token und ohne Benutzerdaten; der Link
 * zum UB-Katalog zeigt den aktuellen Stand.
 *
 * @param items Exemplare aus dem Detail; solange es nicht geladen ist, leer.
 * @param more Text für „und weitere“ in der Personenzeile („u. a.“).
 */
class LibraryMediaShare(
    val media: LibraryMedia,
    private val strings: ShareStrings,
    private val more: String,
    val items: List<LibraryItem> = emptyList(),
    val isOnline: Boolean = false,
) : ShareContent {
    constructor(detail: LibraryMediaDetail, strings: ShareStrings, more: String) :
        this(detail.media, strings, more, detail.items, detail.availabilityStatus == LibraryAvailabilityStatus.Online)

    override val title: String get() = media.title

    override val text: String
        get() {
            val edition = listOfNotNull(ShareText.clean(media.edition), ShareText.clean(media.year)).joinToString(", ")
            val persons = LibraryPersonName.list(media.authors, more)
            val byline = listOf(persons, edition).filter { it.isNotEmpty() }.joinToString(" · ")
            val head = listOf(strings.mediumIntro(), media.title, media.trimmedSubtitle.orEmpty(), byline, placeLine())
            return ShareText.compose(listOf(head, listOf(media.catalogUrl.orEmpty())), strings)
        }

    /** „Lehrbuchsammlung, 2. OG · ZQ 5000 R444(16)“ vom ersten Exemplar, bei Online-Titeln „Online im Campusnetz“. */
    private fun placeLine(): String {
        if (isOnline) return strings.mediumOnline()
        val item = items.firstOrNull() ?: return ""
        return listOfNotNull(ShareText.clean(item.location?.name), signature(item.label)).joinToString(" · ")
    }

    companion object {
        /** Signatur ohne Exemplar-Endung: „ZQ 5000 R444(16)-1“ → „ZQ 5000 R444(16)“. */
        fun signature(label: String): String? = ShareText.clean(label.replace(Regex("-\\d{1,2}$"), ""))
    }
}
