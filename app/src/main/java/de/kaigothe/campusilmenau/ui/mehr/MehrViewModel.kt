package de.kaigothe.campusilmenau.ui.mehr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.data.repository.PaiaRepository
import de.kaigothe.campusilmenau.data.repository.PaiaState
import de.kaigothe.campusilmenau.domain.model.Darstellung
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MehrUiState(
    val darstellung: Darstellung = Darstellung.System,
    val heuteMensa: MensaVenue = MensaVenue.MensaEhrenberg,
    val priceGroup: MensaPriceGroup = MensaPriceGroup.Studierende,
    val paia: PaiaState = PaiaState(),
)

/**
 * „Mehr und Einstellungen“. Die Einstellungen liegen unter denselben Schlüsseln wie im Rest der App (`AppSettings`),
 * es wird nichts doppelt gespeichert.
 */
@HiltViewModel
class MehrViewModel @Inject constructor(
    private val settings: AppSettings,
    private val paia: PaiaRepository,
) : ViewModel() {

    val state: StateFlow<MehrUiState> = combine(
        settings.darstellung, settings.heuteMensa, settings.priceGroup, paia.state,
    ) { darstellung, heuteMensa, priceGroup, paiaState ->
        MehrUiState(darstellung, heuteMensa, priceGroup, paiaState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MehrUiState())

    fun setDarstellung(darstellung: Darstellung) {
        viewModelScope.launch { settings.setDarstellung(darstellung) }
    }

    fun setHeuteMensa(venue: MensaVenue) {
        viewModelScope.launch { settings.setHeuteMensa(venue) }
    }

    fun setPriceGroup(group: MensaPriceGroup) {
        viewModelScope.launch { settings.setPriceGroup(group) }
    }

    /** Verwirft Token und Sitzung im Speicher und die gespeicherten Zugangsdaten. */
    fun logout() = paia.logout()
}
