package de.kaigothe.campusilmenau.domain.model.share

/** Dateinamen der geteilten Bilder. */
object ShareFileName {
    /** „Mensa Ehrenberg“ → „Mensa-Ehrenberg“. Buchstaben und Ziffern bleiben, alles andere wird ein Bindestrich. */
    fun slug(text: String): String {
        val result = StringBuilder()
        var pendingDash = false
        for (char in text) {
            if (char.isLetterOrDigit()) {
                if (pendingDash && result.isNotEmpty()) result.append('-')
                pendingDash = false
                result.append(char)
            } else {
                pendingDash = true
            }
        }
        return result.take(80).toString()
    }

    /** Name mit Endung; ohne brauchbaren Namen der [fallback]. */
    fun png(name: String, fallback: String): String = slug(name).ifEmpty { fallback } + ".png"
}
