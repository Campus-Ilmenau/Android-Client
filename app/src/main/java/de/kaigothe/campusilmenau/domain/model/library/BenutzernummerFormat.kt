package de.kaigothe.campusilmenau.domain.model.library

/** Prüft, ob ein gelesener Barcode eine Benutzernummer sein kann. Alles andere wird still ignoriert. */
object BenutzernummerFormat {
    /** Erlaubte Länge. Das Login-Feld prüft sonst nichts, daher gilt diese Regel. */
    val length = 8..14

    /**
     * Entfernt Leerraum und gibt den Wert zurück, wenn er 8 bis 14 Zeichen lang ist, mit einer ASCII-Ziffer beginnt und nur
     * aus ASCII-Ziffern und -Buchstaben besteht (am Ende steht oft ein X), sonst `null`. Nur bei Codabar ([isCodabar]) werden
     * vorher die Start-/Stoppzeichen A–D vorn und hinten entfernt; bei anderen Codes wären das echte Buchstaben der Nummer.
     */
    fun normalize(raw: String, isCodabar: Boolean = false): String? {
        var text = raw.filterNot { it.isWhitespace() }
        if (isCodabar) {
            if (text.firstOrNull()?.let(::isCodabarMark) == true) text = text.drop(1)
            if (text.lastOrNull()?.let(::isCodabarMark) == true) text = text.dropLast(1)
        }
        if (text.length !in length) return null
        if (text.first() !in '0'..'9') return null
        if (!text.all { it in '0'..'9' || it in 'a'..'z' || it in 'A'..'Z' }) return null
        return text
    }

    private fun isCodabarMark(c: Char) = c in "ABCDabcd"
}
