package de.kaigothe.campusilmenau

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.domain.model.Darstellung
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Zustand der Activity: heute nur die gewählte Darstellung. Die Shell (M2) erweitert ihn. */
@HiltViewModel
class MainViewModel @Inject constructor(
    settings: AppSettings,
) : ViewModel() {

    val darstellung: StateFlow<Darstellung> = settings.darstellung
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Darstellung.System)
}
