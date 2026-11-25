package de.kaigothe.campusilmenau.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.core.device.DeviceCapabilities
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.data.repository.EventsRequest
import de.kaigothe.campusilmenau.data.repository.EventsRequests
import de.kaigothe.campusilmenau.data.repository.ThoskaRequests
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Zustand der App-Shell: die sichtbaren Tabs und Ziele von außen. */
@HiltViewModel
class ShellViewModel @Inject constructor(
    capabilities: DeviceCapabilities,
    private val router: DeepLinkRouter,
    private val settings: AppSettings,
    private val eventsRequests: EventsRequests,
    private val thoskaRequests: ThoskaRequests,
) : ViewModel() {

    /** Die Tabs dieses Geräts: ohne NFC-Hardware kein Thoska. */
    val tabs: List<AppTab> = AppTab.visible(capabilities.hasNfc)

    val deepLinkRequest: StateFlow<DeepLinkRequest?> = router.request

    /**
     * Setzt, was ein Link außer dem Ziel verlangt: `campusilmenau://mensa?mensa=<id>` wählt die Mensa (eine unbekannte
     * ID ändert nichts), die Links auf die Veranstaltungsliste setzen deren Auswahl zurück bzw. auf „Heute Abend“.
     */
    fun prepare(link: DeepLink) {
        when (link) {
            is DeepLink.Mensa -> {
                val venue = MensaVenue.fromKey(link.venueId) ?: return
                viewModelScope.launch { settings.setMensaVenue(venue) }
            }

            DeepLink.Events -> eventsRequests.post(EventsRequest.ShowAll)
            DeepLink.EventsHeuteAbend -> eventsRequests.post(EventsRequest.ShowEvening)
            // Ohne NFC-Hardware gibt es den Tab nicht: Dann ignoriert schon der Navigator den Link, ein Wunsch bliebe liegen.
            DeepLink.ThoskaAuslesen -> if (AppTab.Thoska in tabs) thoskaRequests.requestScan()
            else -> Unit
        }
    }

    fun consume(id: Long) = router.consume(id)
}
