package de.kaigothe.campusilmenau.ui.navigation

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Die Links, mit denen Widgets, Shortcuts und Assistenten die App an einer bestimmten Stelle öffnen.
 * Schema `campusilmenau://`, Host = Bereich, Pfad = Unterziel (Tabelle in `docs/migration/spec/deeplinks.md`).
 *
 * | Ziel | URL |
 * |---|---|
 * | Speiseplan einer Mensa | `campusilmenau://mensa?mensa=<id>` |
 * | Gericht | `campusilmenau://mensa/gericht/<id>` (optional `?tag=yyyy-MM-dd`) |
 * | Öffnungszeiten der Mensa | `campusilmenau://mensa/zeiten` |
 * | Alle Veranstaltungen | `campusilmenau://events` |
 * | Veranstaltungen „Heute Abend“ | `campusilmenau://events?filter=heute-abend` |
 * | Event-Detail | `campusilmenau://events/<id>` |
 * | Thoska auslesen | `campusilmenau://thoska/auslesen` |
 * | Katalogsuche mit Fokus | `campusilmenau://katalog/suche` |
 * | Katalogsuche mit Begriff | `campusilmenau://katalog/suche?q=<begriff>` |
 * | Katalog-Titel | `campusilmenau://katalog/titel/<id>` |
 * | Merkliste | `campusilmenau://bibliothek/merkliste` |
 * | Mehr und Einstellungen | `campusilmenau://mehr` (optional `?bereich=<darstellung|mensa|konto|sprache|ueber>`) |
 *
 * `<id>` der Mensa ist der Schlüssel der Mensa (zum Beispiel `mensaEhrenberg`).
 * Die Klasse ist reines Kotlin und lässt sich ohne Android testen.
 */
sealed interface DeepLink {

    /** Speiseplan; ohne ID die zuletzt gezeigte Mensa. */
    data class Mensa(val venueId: String?) : DeepLink

    /**
     * Ein Gericht. Die IDs der Mensa-API gelten nur für einen Tag und lassen sich nicht einzeln abfragen,
     * darum kann der Link den Tag (`yyyy-MM-dd`, Berliner Tag) mitgeben.
     */
    data class Gericht(val id: String, val day: String?) : DeepLink

    data object MensaZeiten : DeepLink

    /** Die Veranstaltungsliste ohne Filter. */
    data object Events : DeepLink

    data object EventsHeuteAbend : DeepLink

    data class Event(val id: String) : DeepLink

    data object ThoskaAuslesen : DeepLink

    data object KatalogSuche : DeepLink

    /** Die Katalogsuche mit Begriff: Treffer gleich laden, kein Fokus im Suchfeld. */
    data class KatalogSuchen(val query: String) : DeepLink

    data class KatalogTitel(val id: String) : DeepLink

    data object Merkliste : DeepLink

    /** „Mehr und Einstellungen“. Ein unbekannter Bereich gilt wie keiner. */
    data class Mehr(val bereich: String?) : DeepLink

    /** Die URL zum Ziel, zum Beispiel für Widgets und Shortcuts. */
    val url: String
        get() = when (this) {
            is Mensa -> build("mensa", query = listOfNotNull(venueId?.let { "mensa" to it }))
            is Gericht -> build("mensa", listOf("gericht", id), listOfNotNull(day?.let { "tag" to it }))
            MensaZeiten -> build("mensa", listOf("zeiten"))
            Events -> build("events")
            EventsHeuteAbend -> build("events", query = listOf("filter" to "heute-abend"))
            is Event -> build("events", listOf(id))
            ThoskaAuslesen -> build("thoska", listOf("auslesen"))
            KatalogSuche -> build("katalog", listOf("suche"))
            is KatalogSuchen -> build("katalog", listOf("suche"), listOf("q" to query))
            is KatalogTitel -> build("katalog", listOf("titel", id))
            Merkliste -> build("bibliothek", listOf("merkliste"))
            is Mehr -> build("mehr", query = listOfNotNull(bereich?.let { "bereich" to it }))
        }

    companion object {
        const val SCHEME = "campusilmenau"

        /** Das Ziel zur URL, oder `null`, wenn sie nicht zur App gehört oder kein bekanntes Ziel hat. */
        fun parse(url: String): DeepLink? {
            val uri = runCatching { URI(url) }.getOrNull() ?: return null
            if (uri.scheme?.lowercase() != SCHEME) return null
            val area = uri.rawAuthority?.lowercase() ?: return null
            val path = uri.rawPath.orEmpty().split('/').filter { it.isNotEmpty() }.map(::decode)
            val query = parseQuery(uri.rawQuery)
            fun param(name: String): String? = query[name]?.takeIf { it.isNotEmpty() }

            return when {
                area == "mensa" && path.isEmpty() -> Mensa(param("mensa"))
                area == "mensa" && path.size == 2 && path[0] == "gericht" -> Gericht(path[1], param("tag"))
                area == "mensa" && path == listOf("zeiten") -> MensaZeiten
                area == "events" && path.isEmpty() -> when (param("filter")) {
                    null -> Events
                    "heute-abend" -> EventsHeuteAbend
                    else -> null
                }

                area == "events" && path.size == 1 -> Event(path[0])
                area == "thoska" && path == listOf("auslesen") -> ThoskaAuslesen
                area == "katalog" && path == listOf("suche") -> param("q")?.let(::KatalogSuchen) ?: KatalogSuche
                area == "katalog" && path.size == 2 && path[0] == "titel" -> KatalogTitel(path[1])
                area == "bibliothek" && path == listOf("merkliste") -> Merkliste
                area == "mehr" && path.isEmpty() -> Mehr(param("bereich"))
                else -> null
            }
        }

        private fun build(
            area: String,
            path: List<String> = emptyList(),
            query: List<Pair<String, String>> = emptyList(),
        ): String {
            val pathPart = path.joinToString(separator = "") { "/" + encode(it) }
            val queryPart = if (query.isEmpty()) "" else query.joinToString(prefix = "?", separator = "&") { (name, value) ->
                encode(name) + "=" + encode(value)
            }
            return "$SCHEME://$area$pathPart$queryPart"
        }

        private fun parseQuery(raw: String?): Map<String, String> {
            if (raw.isNullOrEmpty()) return emptyMap()
            val result = LinkedHashMap<String, String>()
            raw.split('&').forEach { pair ->
                val index = pair.indexOf('=')
                val name = decode(if (index < 0) pair else pair.substring(0, index))
                val value = if (index < 0) "" else decode(pair.substring(index + 1))
                // Bei doppelten Namen gilt der erste Wert, wie bei `queryItems.first`.
                result.putIfAbsent(name, value)
            }
            return result
        }

        /** Nur `%XX` wird dekodiert. Ein `+` bleibt ein Plus (Suchbegriff „C++“). */
        private fun decode(text: String): String =
            URLDecoder.decode(text.replace("+", "%2B"), "UTF-8")

        /** Nur unreservierte Zeichen (`A–Z a–z 0–9 - . _ ~`) bleiben stehen, alles andere wird als UTF-8 kodiert. */
        private fun encode(text: String): String = buildString {
            for (byte in text.toByteArray(StandardCharsets.UTF_8)) {
                val c = byte.toInt() and 0xFF
                if (c.toChar().isUnreserved()) append(c.toChar()) else append('%').append("%02X".format(c))
            }
        }

        private fun Char.isUnreserved(): Boolean =
            this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '-' || this == '.' || this == '_' || this == '~'
    }
}

/** Der Tab, zu dem ein Link gehört. `null` bei „Mehr“, das kein Tab ist. */
val DeepLink.tab: AppTab?
    get() = when (this) {
        is DeepLink.Mensa, is DeepLink.Gericht, DeepLink.MensaZeiten -> AppTab.Mensa
        DeepLink.Events, DeepLink.EventsHeuteAbend, is DeepLink.Event -> AppTab.Events
        DeepLink.ThoskaAuslesen -> AppTab.Thoska
        DeepLink.KatalogSuche, is DeepLink.KatalogSuchen, is DeepLink.KatalogTitel, DeepLink.Merkliste -> AppTab.Bibliothek
        is DeepLink.Mehr -> null
    }
