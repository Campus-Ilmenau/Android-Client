package de.kaigothe.campusilmenau.domain.model.library.catalog

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText

/** Filter „Medienart“ der Suche (Parameter `type`). */
enum class LibraryMediaFilter(val code: String) { PrintBook("print_book"), Ebook("ebook"), Journal("journal"), All("all") }

/** Wo gesucht wird (Parameter `in`). */
enum class LibrarySearchScope(val code: String) {
    /** Nur der Bestand der UB Ilmenau. Es zählt nur `q`. */
    Local("local"),

    /** Der Gesamtkatalog („Im Gesamtkatalog weitersuchen“). */
    Catalog("catalog"),

    /** Auch Inhaltsverzeichnisse und Klappentexte. Es zählt nur `q`. */
    Content("content"),
}

/** Eingabefelder der Suche, damit ein Fehler unter dem richtigen Feld stehen kann. */
enum class LibraryQueryField { Text, Title, Author, Isbn, Subject, YearFrom, YearTo }

/** Ein Fehler in der Eingabe, den die App vor dem Senden abfängt (die API würde mit 400 antworten). */
data class LibraryQueryIssue(val field: LibraryQueryField, val message: UiText)

/** Suchanfrage an `/v1/library/search`: Freitext (`q`) oder Feldsuche. Seiten à 20 Treffer. */
data class LibrarySearchQuery(
    val text: String = "",
    val title: String = "",
    /** Person (Parameter `author`). */
    val author: String = "",
    val isbn: String = "",
    val subject: String = "",
    /** `null` (ohne `in`): Freitext zuerst im Bestand, Feldsuche im Katalog. */
    val scope: LibrarySearchScope? = null,
    /** `null` lässt die Voreinstellung der API gelten (`print_book`). */
    val mediaType: LibraryMediaFilter? = null,
    /** Jahre so, wie sie getippt wurden. */
    val yearFrom: String = "",
    val yearTo: String = "",
    val page: Int = 1,
) {
    private val usesOnlyText: Boolean get() = scope == LibrarySearchScope.Local || scope == LibrarySearchScope.Content

    /** Mindestens eines der Felder, nach denen die API sucht, ist gefüllt (bei `local`/`content` nur der Freitext). */
    val hasSearchTerm: Boolean
        get() = (if (usesOnlyText) listOf(text) else listOf(text, title, author, isbn, subject)).any { it.isNotBlank() }

    /** Eine Suche über Felder statt Freitext. Sie läuft im Gesamtkatalog, die Antwort enthält kein `source`. */
    val isFieldSearch: Boolean
        get() = !usesOnlyText && listOf(title, author, isbn, subject, yearFrom, yearTo).any { it.isNotBlank() }

    /** Dieselbe Suche im Gesamtkatalog, ab Seite 1. */
    fun continuedInCatalog() = copy(scope = LibrarySearchScope.Catalog, page = 1)

    fun nextPage() = copy(page = page + 1)

    /** Prüft die Eingabe. Eine leere Liste heißt: darf gesendet werden. */
    fun validate(): List<LibraryQueryIssue> = buildList {
        if (!hasSearchTerm) add(LibraryQueryIssue(LibraryQueryField.Text, UiText(R.string.bib_suche_fehler_begriff)))
        if (text.trim().length > MAX_TEXT_LENGTH) {
            add(LibraryQueryIssue(LibraryQueryField.Text, UiText(R.string.bib_suche_fehler_laenge, MAX_TEXT_LENGTH)))
        }
        addAll(validateYears(yearFrom, yearTo))
        if (page < 1) add(LibraryQueryIssue(LibraryQueryField.Text, UiText(R.string.bib_suche_fehler_seite)))
    }

    /** Query-Parameter für die API. Leere Felder entfallen; bei `local` und `content` entfallen auch die Felder. */
    fun params(): Map<String, String> = buildMap {
        fun add(name: String, value: String) {
            val trimmed = value.trim()
            if (trimmed.isNotEmpty()) put(name, trimmed)
        }
        add("q", text)
        if (!usesOnlyText) {
            add("title", title)
            add("author", author)
            add("isbn", isbn)
            add("subject", subject)
        }
        scope?.let { put("in", it.code) }
        mediaType?.let { put("type", it.code) }
        add("year_from", yearFrom)
        add("year_to", yearTo)
        put("page", page.toString())
        put("per_page", PAGE_SIZE.toString())
    }

    companion object {
        const val MAX_TEXT_LENGTH = 200
        const val PAGE_SIZE = 20
        const val MAX_YEAR_SPAN = 150

        /** Jahre: vierstellig (1000–9999), „von“ nicht nach „bis“, höchstens 150 Jahre. Leere Felder sind erlaubt. */
        fun validateYears(from: String, to: String): List<LibraryQueryIssue> = buildList {
            val fromText = from.trim()
            val toText = to.trim()
            val fromYear = parseYear(fromText)
            val toYear = parseYear(toText)
            if (fromText.isNotEmpty() && fromYear == null) add(LibraryQueryIssue(LibraryQueryField.YearFrom, UiText(R.string.bib_suche_fehler_jahr)))
            if (toText.isNotEmpty() && toYear == null) add(LibraryQueryIssue(LibraryQueryField.YearTo, UiText(R.string.bib_suche_fehler_jahr)))
            if (fromYear != null && toYear != null) {
                if (fromYear > toYear) {
                    add(LibraryQueryIssue(LibraryQueryField.YearFrom, UiText(R.string.bib_suche_fehler_von_bis)))
                } else if (toYear - fromYear > MAX_YEAR_SPAN) {
                    add(LibraryQueryIssue(LibraryQueryField.YearTo, UiText(R.string.bib_suche_fehler_spanne, MAX_YEAR_SPAN)))
                }
            }
        }

        /** Vierstellig, ohne führende Null (die API verlangt mindestens 1000). */
        private fun parseYear(text: String): Int? =
            if (text.length == 4 && text.all { it in '0'..'9' } && text.first() != '0') text.toInt() else null
    }
}
