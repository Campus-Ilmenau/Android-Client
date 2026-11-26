package de.kaigothe.campusilmenau.domain.model.library.catalog

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import java.time.Instant
import java.util.Locale

/** Große Fläche mit dem Gesamtstatus eines Titels. */
data class LibraryStatusBlock(val title: UiText, val subtitle: UiText, val tone: Tone) {
    enum class Tone {
        /** `green` mit `onGreen`. */
        Green,

        /** `blue` mit `onBlue`. */
        Blue,

        /** `surface` mit `textPrimary`. */
        Neutral,
    }
}

/** Die Aktion unter der Statusfläche. Beides öffnet eine Seite der Bibliothek im Browser. */
sealed interface LibraryPrimaryAction {
    val url: String
    val title: Int

    data class Reserve(override val url: String) : LibraryPrimaryAction {
        override val title: Int get() = R.string.bib_titel_vormerken
    }

    data class ReadOnline(override val url: String) : LibraryPrimaryAction {
        override val title: Int get() = R.string.bib_status_online
    }
}

/** Zeile der Liste „Exemplare“. [extra] ist „Zurück bis 12. Nov. · 2 Vormerkungen“ bzw. ein Hinweis zur Nutzung. */
data class LibraryItemRow(
    val place: UiText,
    val signature: String,
    val status: LibraryAvailabilityStatus,
    val extra: UiText?,
    val floorPlanUrl: String?,
)

/** „Weitere Angaben“: nur Felder mit Daten. */
data class LibraryTitleFacts(
    val isbn: String? = null,
    val language: String? = null,
    val publisher: String? = null,
    val rvk: String? = null,
    val subjects: List<String> = emptyList(),
    val abstract: String? = null,
) {
    val isEmpty: Boolean get() = isbn == null && language == null && publisher == null && rvk == null && subjects.isEmpty() && abstract == null
}

private fun queueText(count: Int) = UiText.plural(R.plurals.bib_titel_vormerkungen, count, count)

private fun join(parts: List<UiText>): UiText? = parts.reduceOrNull { acc, next -> UiText(R.string.bib_kat_verbinder, acc, next) }

/** Eine Zeile des Exemplars; [date] formatiert Rückgabedaten in der Sprache der App. */
fun LibraryItem.row(date: (Instant) -> String): LibraryItemRow {
    val extra = when (status) {
        LibraryAvailabilityStatus.OnLoan -> join(
            listOfNotNull(
                dueDate?.let { UiText(R.string.bib_titel_zurueck_bis, date(it)) },
                queue?.takeIf { it > 0 }?.let(::queueText),
            ),
        )
        LibraryAvailabilityStatus.ReferenceOnly -> UiText(R.string.bib_titel_nur_lesesaal)
        LibraryAvailabilityStatus.Online -> UiText(R.string.bib_titel_vpn)
        LibraryAvailabilityStatus.Available, LibraryAvailabilityStatus.Unknown -> null
    }
    return LibraryItemRow(
        place = location?.name?.takeIf { it.isNotBlank() }?.let(UiText::plain) ?: UiText(R.string.bib_titel_exemplar),
        signature = label,
        status = status,
        extra = extra,
        floorPlanUrl = location?.floorPlanUrl,
    )
}

/** Gesamtstatus als große Fläche. */
fun LibraryMediaDetail.statusBlock(date: (Instant) -> String): LibraryStatusBlock {
    val overview = availability
    return when (availabilityStatus) {
        LibraryAvailabilityStatus.Available -> {
            val total = items.size
            val subtitle = when (total) {
                0 -> UiText(R.string.bib_titel_status_keine_exemplare)
                1 -> UiText(R.string.bib_titel_status_ein_exemplar)
                else -> UiText(R.string.bib_titel_status_im_regal, overview.availableCount, total)
            }
            LibraryStatusBlock(UiText(R.string.bib_status_verfuegbar), subtitle, LibraryStatusBlock.Tone.Green)
        }
        LibraryAvailabilityStatus.OnLoan -> {
            val parts = listOfNotNull(
                overview.earliestDueDate?.let { UiText(R.string.bib_titel_fruehestens, date(it)) },
                overview.totalQueue.takeIf { it > 0 }?.let(::queueText),
            )
            LibraryStatusBlock(
                title = UiText(if (items.size > 1) R.string.bib_titel_alle_ausgeliehen else R.string.bib_status_ausgeliehen),
                subtitle = join(parts) ?: UiText(R.string.bib_titel_status_rueckgabe_katalog),
                tone = LibraryStatusBlock.Tone.Neutral,
            )
        }
        LibraryAvailabilityStatus.ReferenceOnly ->
            LibraryStatusBlock(UiText(R.string.bib_titel_status_vor_ort), UiText(R.string.bib_titel_nur_lesesaal_kurz), LibraryStatusBlock.Tone.Blue)
        LibraryAvailabilityStatus.Online ->
            LibraryStatusBlock(UiText(R.string.bib_titel_status_online), UiText(R.string.bib_titel_status_online_text), LibraryStatusBlock.Tone.Green)
        LibraryAvailabilityStatus.Unknown ->
            LibraryStatusBlock(UiText(R.string.bib_titel_status_unbekannt), UiText(R.string.bib_titel_status_unbekannt_text), LibraryStatusBlock.Tone.Neutral)
    }
}

/** „Vormerken“ nur bei ausgeliehenen Titeln mit `reserveUrl`, „Online lesen“ nur mit `onlineUrl`. */
val LibraryMediaDetail.primaryAction: LibraryPrimaryAction?
    get() = when (availabilityStatus) {
        LibraryAvailabilityStatus.OnLoan -> items.firstNotNullOfOrNull { it.reserveUrl }?.let { LibraryPrimaryAction.Reserve(it) }
        LibraryAvailabilityStatus.Online -> media.onlineUrl?.let { LibraryPrimaryAction.ReadOnline(it) }
        else -> null
    }

/** „16. Auflage · 2022 · Springer Vieweg“. */
val LibraryMedia.editionLine: String
    get() = listOf(edition.orEmpty(), year.orEmpty(), publisher.orEmpty()).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ")

fun LibraryMedia.personsLine(more: String): String = LibraryPersonName.list(authors, more, limit = 6)

/** „Weitere Angaben“ mit höchstens zwei ISBN. [languageName] kommt aus [LibraryLanguage]. */
fun LibraryMedia.facts(languageName: String?): LibraryTitleFacts {
    fun text(value: String?) = value?.trim()?.takeIf { it.isNotEmpty() }
    fun joined(values: List<String>, limit: Int = Int.MAX_VALUE) = values.mapNotNull(::text).take(limit).takeIf { it.isNotEmpty() }?.joinToString(", ")
    return LibraryTitleFacts(
        isbn = joined(isbns, limit = 2),
        language = languageName,
        publisher = text(publisher),
        rvk = joined(rvk),
        subjects = subjects.mapNotNull(::text),
        abstract = text(abstract),
    )
}

/** Längster Fließtext, den die Ansicht zeigt (der Rohtext eines ganzen Buches kann sehr lang sein). */
const val TOC_TEXT_LIMIT = 2000

/** Rohtext ohne Gliederung, gekürzt. Nur ein Rückfall, wenn es keine `entries` gibt. */
val LibraryTableOfContents.shortText: String?
    get() {
        val value = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (value.length > TOC_TEXT_LIMIT) value.take(TOC_TEXT_LIMIT).trim() + " …" else value
    }

/** Sprachcodes der Bibliothek (MARC, dreistellig) als Name in der Sprache der App. */
object LibraryLanguage {
    /** Bibliografische Codes (MARC), die Java nicht kennt, auf die terminologischen abgebildet. */
    private val aliases = mapOf("ger" to "deu", "fre" to "fra", "dut" to "nld", "chi" to "zho", "cze" to "ces", "gre" to "ell", "rum" to "ron")

    /** Dreibuchstabige Codes (ISO 639-2/T) auf die zweibuchstabigen, mit denen Java Sprachnamen kennt. */
    private val byIso3: Map<String, String> by lazy { Locale.getISOLanguages().associateBy { Locale.forLanguageTag(it).isO3Language } }

    /** `mul` und `und` haben keinen Sprachnamen; der Aufrufer liefert sie aus den Ressourcen. */
    fun name(code: String, locale: Locale, multiple: String, unknown: String): String {
        val key = code.trim().lowercase()
        return when (key) {
            "mul" -> multiple
            "und" -> unknown
            else -> {
                val two = byIso3[aliases[key] ?: key]
                val name = two?.let { Locale.forLanguageTag(it).getDisplayLanguage(locale) }
                // Ohne bekannten Namen gibt Java den Code zurück.
                if (name.isNullOrBlank() || name.equals(two, ignoreCase = true)) code.trim().uppercase() else name
            }
        }
    }
}
