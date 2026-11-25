package de.kaigothe.campusilmenau.ui.navigation

import de.kaigothe.campusilmenau.domain.model.heute.HeuteZiel

/** Der Link zum Ziel einer Karte oder eines Termins auf „Heute“. Die Mensa-Ziele wählen auch die Mensa im Mensa-Tab. */
fun HeuteZiel.toDeepLink(): DeepLink = when (this) {
    is HeuteZiel.Speiseplan -> DeepLink.Mensa(venue.key)
    is HeuteZiel.Oeffnungszeiten -> DeepLink.MensaZeiten
    is HeuteZiel.Gericht -> DeepLink.Gericht(id, day)
    is HeuteZiel.Event -> DeepLink.Event(id)
    HeuteZiel.AlleEvents -> DeepLink.Events
}
