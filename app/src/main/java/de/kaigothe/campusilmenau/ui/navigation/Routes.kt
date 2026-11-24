package de.kaigothe.campusilmenau.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Wurzel des jeweiligen Tabs. Sie liegt als erstes Element im Back Stack des Tabs. */
@Serializable
data object HeuteRoute : NavKey

@Serializable
data object MensaRoute : NavKey

@Serializable
data object ThoskaRoute : NavKey

@Serializable
data object BibliothekRoute : NavKey

@Serializable
data object EventsRoute : NavKey

/** Ein Gericht. Die IDs der API gelten nur für einen Tag, darum gehört der Tag (`yyyy-MM-dd`, Berlin) zur Route. */
@Serializable
data class MensaGerichtRoute(val id: String, val day: String?) : NavKey

/** Öffnungszeiten der Mensen. */
@Serializable
data object MensaZeitenRoute : NavKey

/** „Mehr und Einstellungen“, erreichbar über den Personen-Button auf „Heute“. */
@Serializable
data class MehrRoute(val bereich: String? = null) : NavKey

/** Die Wurzel-Route des Tabs. */
val AppTab.route: NavKey
    get() = when (this) {
        AppTab.Heute -> HeuteRoute
        AppTab.Mensa -> MensaRoute
        AppTab.Thoska -> ThoskaRoute
        AppTab.Bibliothek -> BibliothekRoute
        AppTab.Events -> EventsRoute
    }
