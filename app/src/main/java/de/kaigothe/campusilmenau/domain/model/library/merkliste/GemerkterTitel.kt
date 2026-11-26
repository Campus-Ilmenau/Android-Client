package de.kaigothe.campusilmenau.domain.model.library.merkliste

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailability
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryCover
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryCoverStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaType
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryPersonName
import java.time.Instant

/**
 * Ein Titel auf der Merkliste. Liegt nur auf diesem Gerät, hat nichts mit PAIA oder der Anmeldung zu tun.
 * Der Verfügbarkeitsstatus wird nie gespeichert.
 */
data class GemerkterTitel(
    /** ID wie die Katalog-API sie liefert: `med_<PPN>`. */
    val id: String,
    /** Leer: von früher übernommen, unvollständig. */
    val titel: String,
    /** „Vorname Nachname, …“, schon so formatiert, wie die Zeile sie zeigt. */
    val personen: String = "",
    val jahr: String? = null,
    /** Code der Medienart (`print_book`, `ebook`, `journal`, …). */
    val medientyp: String = "print_book",
    /** Pfad oder Adresse des Covers, nur wenn es eines gibt. */
    val coverUrl: String? = null,
    val gemerktAm: Instant,
    /** Wann der Titel am Regal als „Gefunden“ abgehakt wurde. `null`: noch offen. */
    val foundAt: Instant? = null,
    /** Zeitpunkt der letzten Aktion an diesem Eintrag (für einen späteren Abgleich mit Wear OS). */
    val changedAt: Instant = gemerktAm,
) {
    val istGefunden: Boolean get() = foundAt != null

    /** Titel für die Anzeige; ist er unbekannt, die Nummer. */
    val anzeigeTitel: UiText get() = if (titel.isEmpty()) UiText(R.string.bib_merkliste_titel_nummer, id.removePrefix("med_")) else UiText.plain(titel)

    val mediaType: LibraryMediaType get() = LibraryMediaType.of(medientyp)

    /** „Personen · Jahr“, ohne Leeres. */
    val untertitel: String get() = listOf(personen, jahr.orEmpty()).filter { it.isNotEmpty() }.joinToString(" · ")

    /** Für das Laden des Covers. */
    val alsMedia: LibraryMedia
        get() = LibraryMedia(
            id = id,
            ppn = id.takeIf { it.startsWith("med_") }?.removePrefix("med_"),
            mediaType = mediaType,
            title = titel,
            cover = coverUrl?.let { LibraryCover(LibraryCoverStatus.Found, it) } ?: LibraryCover.Missing,
        )

    companion object {
        /** Aus einem Titel des Katalogs; das Cover nur, wenn es gefunden wurde. */
        fun of(media: LibraryMedia, more: String, gemerktAm: Instant): GemerkterTitel = GemerkterTitel(
            id = media.id,
            titel = media.title,
            personen = LibraryPersonName.list(media.authors, more, limit = 6),
            jahr = media.year,
            medientyp = media.mediaType.code ?: "other",
            coverUrl = media.cover.takeIf { it.status == LibraryCoverStatus.Found }?.url,
            gemerktAm = gemerktAm,
        )
    }
}

/** Die Merkliste für die Anzeige: offene Titel, neueste zuerst; gefundene, zuletzt gefundene zuerst. */
data class MerklisteListe(val offen: List<GemerkterTitel>, val gefunden: List<GemerkterTitel>) {
    val anzahl: Int get() = offen.size + gefunden.size
    val isEmpty: Boolean get() = anzahl == 0

    companion object {
        fun of(eintraege: List<GemerkterTitel>) = MerklisteListe(
            offen = eintraege.filter { !it.istGefunden }.sortedByDescending { it.gemerktAm },
            gefunden = eintraege.filter { it.istGefunden }.sortedByDescending { it.foundAt },
        )
    }
}

/**
 * Signatur und Standort eines gemerkten Titels, aus den Exemplaren der Verfügbarkeit. Wird nicht gespeichert.
 * Zeigt das Exemplar, das man am ehesten bekommt: erst ein ausleihbares, dann eines nur vor Ort, dann ein ausgeliehenes,
 * sonst das erste. `null`, wenn weder Signatur noch Standort bekannt sind (zum Beispiel E-Books).
 */
data class MerklisteExemplar(
    val signatur: String?,
    val standort: String?,
    /** Link zum Lageplan des Standorts, nur http(s). */
    val lageplan: String?,
    /** Anzahl weiterer Exemplare, die nicht gezeigt werden. */
    val weitere: Int,
) {
    companion object {
        private val reihenfolge = listOf(LibraryAvailabilityStatus.Available, LibraryAvailabilityStatus.ReferenceOnly, LibraryAvailabilityStatus.OnLoan)

        fun of(availability: LibraryAvailability): MerklisteExemplar? {
            val items = availability.items
            val wahl = reihenfolge.firstNotNullOfOrNull { status -> items.firstOrNull { it.status == status } } ?: items.firstOrNull() ?: return null
            fun gekuerzt(text: String?) = text?.trim()?.takeIf { it.isNotEmpty() }
            val plan = wahl.location?.floorPlanUrl?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
            val signatur = gekuerzt(wahl.label)
            val standort = gekuerzt(wahl.location?.name)
            if (signatur == null && standort == null) return null
            return MerklisteExemplar(signatur, standort, plan, weitere = items.size - 1)
        }
    }
}
