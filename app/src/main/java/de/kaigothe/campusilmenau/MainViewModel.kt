package de.kaigothe.campusilmenau

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.domain.model.Darstellung
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Zustand der Activity: Solange die Einstellungen laden, hält der Splash den Bildschirm. */
sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val darstellung: Darstellung) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: AppSettings,
) : ViewModel() {

    /**
     * Sofort gestartet (`Eagerly`), weil der Splash den Zustand schon liest, bevor die erste Ansicht steht.
     * Schlägt das Lesen fehl, gilt die Standard-Darstellung, der Start hängt nie.
     */
    val uiState: StateFlow<MainUiState> = settings.darstellung
        .catch { emit(Darstellung.System) }
        .map<Darstellung, MainUiState> { MainUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)
}
