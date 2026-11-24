package de.kaigothe.campusilmenau.ui.mensa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.data.repository.MensaRepository
import de.kaigothe.campusilmenau.domain.model.mensa.Canteen
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.ui.components.LoadingGate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

sealed interface MensaOpeningHoursState {
    data object Loading : MensaOpeningHoursState
    data class Failed(val error: ApiError) : MensaOpeningHoursState

    /** Die vier festen Mensen in der Reihenfolge der App. [canteen] ist `null`, wenn die API sie nicht liefert. */
    data class Loaded(val venues: List<VenueHours>) : MensaOpeningHoursState
}

data class VenueHours(val venue: MensaVenue, val canteen: Canteen?)

@HiltViewModel
class MensaOpeningHoursViewModel @Inject constructor(
    private val repository: MensaRepository,
    clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow<MensaOpeningHoursState>(MensaOpeningHoursState.Loading)
    val state: StateFlow<MensaOpeningHoursState> = _state.asStateFlow()

    val loadingGate = LoadingGate(viewModelScope, initiallyLoading = true)

    /** Die Berliner Zeit für „Jetzt geöffnet“, jede halbe Minute erneuert, solange die Ansicht sichtbar ist. */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.ofInstant(clock.instant(), BerlinTime.zone))
            delay(30.seconds)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LocalDateTime.ofInstant(clock.instant(), BerlinTime.zone),
    )

    init {
        load(forceRefresh = false)
    }

    fun retry() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean) {
        _state.value = MensaOpeningHoursState.Loading
        loadingGate.setLoading(true)
        viewModelScope.launch {
            _state.value = try {
                val canteens = repository.canteens(forceRefresh)
                MensaOpeningHoursState.Loaded(
                    MensaVenue.entries.map { venue -> VenueHours(venue, canteens.firstOrNull { venue.matches(it.name) }) },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                MensaOpeningHoursState.Failed(e)
            }
            loadingGate.setLoading(false)
        }
    }
}
