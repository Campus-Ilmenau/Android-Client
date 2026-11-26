package de.kaigothe.campusilmenau.domain.model.library.ausweis

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Strichcode der Benutzernummer im Format Code 39, wie auf der Thoska. Die App zeichnet ihn selbst (kein ZXing). Alles hier ist
 * reine Rechnung: Die Oberfläche zeichnet die [Layout.bars] als Rechtecke mit ganzzahligen Pixeln, ohne Filterung.
 */
object Code39 {
    /**
     * Muster je Zeichen: neun Elemente, abwechselnd Strich und Lücke, beginnend mit einem Strich.
     * `n` = schmal, `w` = breit (genau drei breite je Zeichen).
     */
    val patterns: Map<Char, String> = mapOf(
        '0' to "nnnwwnwnn", '1' to "wnnwnnnnw", '2' to "nnwwnnnnw", '3' to "wnwwnnnnn", '4' to "nnnwwnnnw",
        '5' to "wnnwwnnnn", '6' to "nnwwwnnnn", '7' to "nnnwnnwnw", '8' to "wnnwnnwnn", '9' to "nnwwnnwnn",
        'A' to "wnnnnwnnw", 'B' to "nnwnnwnnw", 'C' to "wnwnnwnnn", 'D' to "nnnnwwnnw", 'E' to "wnnnwwnnn",
        'F' to "nnwnwwnnn", 'G' to "nnnnnwwnw", 'H' to "wnnnnwwnn", 'I' to "nnwnnwwnn", 'J' to "nnnnwwwnn",
        'K' to "wnnnnnnww", 'L' to "nnwnnnnww", 'M' to "wnwnnnnwn", 'N' to "nnnnwnnww", 'O' to "wnnnwnnwn",
        'P' to "nnwnwnnwn", 'Q' to "nnnnnnwww", 'R' to "wnnnnnwwn", 'S' to "nnwnnnwwn", 'T' to "nnnnwnwwn",
        'U' to "wwnnnnnnw", 'V' to "nwwnnnnnw", 'W' to "wwwnnnnnn", 'X' to "nwnnwnnnw", 'Y' to "wwnnwnnnn",
        'Z' to "nwwnwnnnn", '-' to "nwnnnnwnw", '.' to "wwnnnnwnn", ' ' to "nwwnnnwnn", '$' to "nwnwnwnnn",
        '/' to "nwnwnnnwn", '+' to "nwnnnwnwn", '%' to "nnnwnwnwn", '*' to "nwnnwnwnn",
    )

    /** Ein Element des Codes: Strich oder Lücke, schmal oder breit. */
    data class Element(val isBar: Boolean, val isWide: Boolean)

    /**
     * Alle Elemente von links nach rechts mit Start-/Stoppzeichen `*` und je einer schmalen Lücke zwischen den Zeichen,
     * ohne Ruhezone (die liefert der Rand um das Bild). Kleinbuchstaben werden zu Großbuchstaben (Code 39 kennt nur diese).
     * `null` bei unbekannten Zeichen, leerem Text und bei `*` im Text.
     */
    fun elements(text: String): List<Element>? {
        val chars = text.uppercase()
        if (chars.isEmpty() || '*' in chars || chars.any { it !in patterns }) return null
        return buildList {
            ("*" + chars + "*").forEachIndexed { index, c ->
                if (index > 0) add(Element(isBar = false, isWide = false))
                patterns.getValue(c).forEachIndexed { position, form -> add(Element(isBar = position % 2 == 0, isWide = form == 'w')) }
            }
        }
    }

    /** Breiter Strich in Pixeln zu einem schmalen: etwa 2,5 : 1 (zulässig sind 2 bis 3). */
    fun wideFor(narrow: Int): Int = max(narrow + 1, (narrow * 2.5).roundToInt())

    fun widthInPixels(elements: List<Element>, narrow: Int): Int {
        val wide = wideFor(narrow)
        return elements.sumOf { if (it.isWide) wide else narrow }
    }

    /** Ein schwarzer Strich: [x] und [width] in Pixeln. */
    data class Bar(val x: Int, val width: Int)

    /** Der Code in ganzen Pixeln: [width] Pixel breit, [bars] die schwarzen Striche. [narrow] ist die schmale Strichbreite. */
    data class Layout(val width: Int, val narrow: Int, val bars: List<Bar>)

    /**
     * Wählt die größte ganzzahlige Strichbreite (in Pixeln), bei der der Code in [availablePixels] passt, und legt ihn damit
     * aus. Der Code füllt so die Breite fast aus, und trotzdem ist jeder schmale Strich gleich breit. Ein schmaler Strich ist
     * höchstens 1,8 Pixel pro dp breit ([density] = Pixel pro dp): Die Ruhezone von 10 schmalen Strichen liegt im Rand des
     * Felds (mindestens 18 dp). `null`, wenn der Text nicht darstellbar ist oder kein Platz da ist.
     */
    fun layout(text: String, availablePixels: Int, density: Float): Layout? {
        if (availablePixels <= 0 || density <= 0f) return null
        val elements = elements(text) ?: return null
        val limit = max(1, floor(1.8f * density).toInt())
        var narrow = 1
        for (candidate in 1..limit) if (widthInPixels(elements, candidate) <= availablePixels) narrow = candidate
        val wide = wideFor(narrow)
        val bars = mutableListOf<Bar>()
        var x = 0
        for (element in elements) {
            val width = if (element.isWide) wide else narrow
            if (element.isBar) bars += Bar(x, width)
            x += width
        }
        return Layout(width = x, narrow = narrow, bars = bars)
    }
}
