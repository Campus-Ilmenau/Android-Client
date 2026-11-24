package de.kaigothe.campusilmenau.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.core.device.DeviceCapabilities
import de.kaigothe.campusilmenau.data.preferences.AppSettings
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
) : ViewModel() {

    /** Die Tabs dieses Geräts: ohne NFC-Hardware kein Thoska. */
    val tabs: List<AppTab> = AppTab.visible(capabilities.hasNfc)

    val deepLinkRequest: StateFlow<DeepLinkRequest?> = router.request

    /**
     * Setzt, was ein Link außer dem Ziel verlangt: `campusilmenau://mensa?mensa=<id>` wählt die Mensa. Eine unbekannte
     * ID ändert nichts, es bleibt die zuletzt gewählte Mensa.
     */
    fun prepare(link: DeepLink) {
        if (link is DeepLink.Mensa) {
            val venue = MensaVenue.fromKey(link.venueId) ?: return
            viewModelScope.launch { settings.setMensaVenue(venue) }
        }
    }

    fun consume(id: Long) = router.consume(id)
}
