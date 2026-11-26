package de.kaigothe.campusilmenau.domain.model.library.catalog

/** Namen aus der Bibliothek stehen als „Nachname, Vorname“; angezeigt werden sie als „Vorname Nachname“. */
object LibraryPersonName {
    fun display(raw: String): String {
        val trimmed = raw.trim()
        val parts = trimmed.split(",", limit = 2).map { it.trim() }
        return if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) "${parts[1]} ${parts[0]}" else trimmed
    }

    /** Bis zu [limit] Namen; bei mehr folgt [more] („u. a.“). */
    fun list(authors: List<String>, more: String, limit: Int = 3): String {
        val names = authors.map(::display).filter { it.isNotEmpty() }
        return if (names.size > limit) names.take(limit).joinToString(", ") + " " + more else names.joinToString(", ")
    }
}

/** „Personen · Jahr · Medienart“; [typeName] und [more] kommen aus den Ressourcen. */
fun LibraryMedia.metaLine(typeName: String, more: String): String =
    listOf(LibraryPersonName.list(authors, more), year.orEmpty(), typeName).filter { it.isNotEmpty() }.joinToString(" · ")

/** Untertitel ohne Leerraum, `null`, wenn keiner da ist. */
val LibraryMedia.trimmedSubtitle: String? get() = subtitle?.trim()?.takeIf { it.isNotEmpty() }
