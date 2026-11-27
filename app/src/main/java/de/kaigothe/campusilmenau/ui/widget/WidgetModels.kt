package de.kaigothe.campusilmenau.ui.widget

import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.domain.model.heute.HeuteClock
import de.kaigothe.campusilmenau.domain.model.heute.HeuteContent
import de.kaigothe.campusilmenau.ui.navigation.DeepLink
import de.kaigothe.campusilmenau.ui.navigation.toDeepLink

/** Eine Zeile im Widget: Text, rechts Preis oder Zeit, und der Link, den ein Tipp öffnet. */
data class WidgetRow(val text: String, val trailing: String?, val url: String)

/** Alles, was das Widget „Mensa heute“ zeigt, schon als Text. Die Ansicht rechnet nichts mehr. */
data class MensaWidgetData(
    val kicker: String?,
    val title: String,
    val subtitle: String?,
    val rows: List<WidgetRow>,
    /** Ein Tipp auf den Kopf. */
    val url: String,
)

/** Alles, was das Widget „Heute Abend“ zeigt. */
data class AbendWidgetData(
    val rows: List<AbendRow>,
    /** Ein Tipp auf den Kopf, die Liste von heute Abend. */
    val url: String,
)

data class AbendRow(val time: String, val title: String, val place: String?, val url: String)

/**
 * Die Jetzt-Karte der Startseite als Widget: dieselbe Auswahl (Phase des Tages, Mensa, Filter, Preisgruppe), damit das
 * Widget und die App dasselbe zeigen. Fehlen die Daten, steht die Fehlerkarte der Startseite da.
 *
 * @param resolve löst Texte aus den Ressourcen auf (in der Sprache der App).
 */
fun HeuteContent.toMensaWidgetData(resolve: (UiText) -> String): MensaWidgetData {
    val card = currentCard ?: errorCard
    val cardUrl = card.ziel.toDeepLink().url
    return MensaWidgetData(
        kicker = card.kicker?.let(resolve),
        title = resolve(card.title),
        subtitle = card.subtitle?.let(resolve),
        rows = card.rows.map { row -> WidgetRow(resolve(row.name), row.right, row.ziel?.toDeepLink()?.url ?: cardUrl) },
        url = cardUrl,
    )
}

/** Die Veranstaltungen von heute Abend (höchstens drei, wie auf der Startseite). */
fun HeuteContent.toAbendWidgetData(): AbendWidgetData = AbendWidgetData(
    rows = eveningEvents.map { event ->
        AbendRow(
            time = if (event.allDay) "" else HeuteClock.time(event.start),
            title = event.title,
            place = event.location?.trim()?.takeIf { it.isNotEmpty() },
            url = DeepLink.Event(event.id).url,
        )
    },
    url = DeepLink.EventsHeuteAbend.url,
)
