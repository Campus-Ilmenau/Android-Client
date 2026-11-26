package de.kaigothe.campusilmenau.domain.model.share

import de.kaigothe.campusilmenau.domain.model.mensa.MealChip
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealPrice
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import java.util.Locale

/** Ein teilbarer Inhalt. [title] ist die Überschrift des Teilen-Blatts, [text] der reine Text (ohne Markdown). */
interface ShareContent {
    val title: String
    val text: String
}

/** Marke unter jedem geteilten Text und auf jedem Bild. */
object ShareBranding {
    /** Die Adresse steht nur hier. */
    const val DOMAIN = "campus-ilmenau.de"
}

/**
 * Die Texte zum Teilen in der Sprache der App. Die Domäne kennt keine Ressourcen; die App liefert sie über
 * `ResourceShareStrings`, die Tests über eine feste Sprache. Alle Texte sind Sätze oder Satzteile, die zusammengesetzt werden.
 */
interface ShareStrings {
    val locale: Locale

    /** „Geteilt mit Campus Ilmenau – campus-ilmenau.de“ */
    fun brand(domain: String): String
    fun groupName(group: MensaPriceGroup): String
    fun planOffer(dinner: Boolean): String
    fun hours(range: String): String

    /** „Preise für Studierende“ */
    fun priceNote(group: MensaPriceGroup): String
    fun chipTitle(chip: MealChip): String
    fun fallbackCategory(): String
    fun allergens(list: String): String
    fun additives(list: String): String
    fun co2(grams: String): String

    fun eventCancelled(title: String): String
    fun eventTentative(): String
    fun eventOrganizer(name: String): String
    fun eventTickets(url: String): String
    fun eventInfo(url: String): String
    fun eventAllDay(): String
    fun eventRange(from: String, to: String): String
    fun eventFrom(from: String): String

    fun mediumIntro(): String
    fun mediumOnline(): String

    // Nur auf den Bildkarten.
    fun cardDinnerCafeteria(): String
    fun cardDisclaimer(): String
    fun cardMealDisclaimer(): String
    fun cardAllergens(): String
    fun cardAdditives(): String
    fun cardEnvironment(): String
    fun cardEventAllDay(): String
    fun cardEventCancelledChip(): String
    fun cardEventCancelledNote(): String
    fun cardEventFallbackOrganizer(): String
    fun cardEventNoEnd(): String
    fun cardEventTentativeChip(): String
    fun cardEventTentativeNote(): String
    fun cardEventUntil(time: String): String
}

object ShareText {
    /**
     * Setzt die Blöcke zu einem Text zusammen: Zeilen eines Blocks untereinander, zwischen den Blöcken eine Leerzeile,
     * leere Blöcke entfallen, die Markenzeile steht zuletzt.
     */
    fun compose(blocks: List<List<String>>, strings: ShareStrings): String =
        (blocks + listOf(listOf(strings.brand(ShareBranding.DOMAIN))))
            .map { block -> block.filter { it.isNotEmpty() } }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n") { it.joinToString("\n") }

    /** Entfernt Leerraum an den Rändern; Leeres wird zu `null`. */
    fun clean(text: String?): String? = text?.trim()?.takeIf { it.isNotEmpty() }

    /** Ein Preis wie „2,90 €“ mit normalem Leerzeichen statt des festen, damit der Text überall gleich aussieht. */
    fun plainSpaces(text: String): String = text.replace(' ', ' ').replace(' ', ' ')

    /**
     * Kürzt am Wortende auf höchstens [limit] Zeichen (inklusive „…“). Absätze bleiben, mehr als eine Leerzeile am
     * Stück nicht.
     */
    fun shortened(text: String, limit: Int = 280): String {
        val normalized = text
            .replace("\r\n", "\n")
            .replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        if (normalized.length <= limit) return normalized

        val budget = limit - 1
        var cut = normalized.substring(0, budget)
        // Steht der Schnitt mitten im Wort, geht es bis zum letzten Leerzeichen davor zurück.
        if (!normalized[budget].isWhitespace()) {
            val space = cut.indexOfLast { it.isWhitespace() }
            if (space >= 0) cut = cut.substring(0, space)
        }
        return cut.trim { it.isWhitespace() || it in ",;:–-" } + "…"
    }
}

/** „2,90 €“ in der Sprache des Textes; `null` bei fehlendem Preis (im Text entfällt er). */
fun MensaMealPrice.shareText(group: MensaPriceGroup, locale: Locale): String? {
    val value = when (group) {
        MensaPriceGroup.Studierende -> student
        MensaPriceGroup.Bedienstete -> staff
        MensaPriceGroup.Gaeste -> guest
    } ?: return null
    return ShareText.plainSpaces(MensaMealPrice.format(value, currency, locale))
}

/** Reihenfolge der Gruppen mit der eigenen zuerst. */
fun MensaPriceGroup.ownFirst(): List<MensaPriceGroup> = listOf(this) + MensaPriceGroup.entries.filter { it != this }
