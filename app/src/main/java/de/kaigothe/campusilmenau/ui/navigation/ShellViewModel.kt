package de.kaigothe.campusilmenau.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.core.device.DeviceCapabilities
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Zustand der App-Shell: die sichtbaren Tabs und Ziele von außen. */
@HiltViewModel
class ShellViewModel @Inject constructor(
    capabilities: DeviceCapabilities,
    private val router: DeepLinkRouter,
) : ViewModel() {

    /** Die Tabs dieses Geräts: ohne NFC-Hardware kein Thoska. */
    val tabs: List<AppTab> = AppTab.visible(capabilities.hasNfc)

    val deepLinkRequest: StateFlow<DeepLinkRequest?> = router.request

    fun consume(id: Long) = router.consume(id)
}
