package de.kaigothe.campusilmenau.ui.mensa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.data.repository.MensaRepository
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFavorites
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.ui.navigation.MensaGerichtRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Zustand der Gericht-Ansicht. */
sealed interface MensaMealDetailState {
    data object Loading : MensaMealDetailState

    /** Das Gericht steht im Speiseplan des Tages nicht (mehr), oder der Tag in der URL ist ungültig. */
    data object NotFound : MensaMealDetailState

    data class Failed(val error: ApiError) : MensaMealDetailState

    data class Loaded(
        val meal: MensaMeal,
        val priceGroup: MensaPriceGroup,
        val isFavorite: Boolean,
    ) : MensaMealDetailState
}

/**
 * Die Daten eines Gerichts. Die Route liefert ID und Tag. Der Speiseplan des Tages kommt meist aus dem Zwischenspeicher
 * der Liste, ein Deeplink lädt ihn bei Bedarf selbst.
 */
@HiltViewModel(assistedFactory = MensaMealDetailViewModel.Factory::class)
class MensaMealDetailViewModel @AssistedInject constructor(
    @Assisted private val route: MensaGerichtRoute,
    private val repository: MensaRepository,
    private val settings: AppSettings,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(route: MensaGerichtRoute): MensaMealDetailViewModel
    }

    private val meal = MutableStateFlow<MensaMealDetailState>(MensaMealDetailState.Loading)

    val state: StateFlow<MensaMealDetailState> = combine(
        meal,
        settings.priceGroup,
        settings.mensaFavorites,
    ) { loaded, priceGroup, favorites ->
        if (loaded is MensaMealDetailState.Loaded) {
            loaded.copy(priceGroup = priceGroup, isFavorite = MensaFavorites.key(loaded.meal) in favorites)
        } else {
            loaded
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MensaMealDetailState.Loading)

    init {
        load()
    }

    fun retry() = load()

    fun toggleFavorite() {
        val current = state.value as? MensaMealDetailState.Loaded ?: return
        viewModelScope.launch { settings.setMensaFavorite(current.meal.name, !current.isFavorite) }
    }

    private fun load() {
        val day = parseDay(route.day) ?: run {
            meal.value = MensaMealDetailState.NotFound
            return
        }
        meal.value = MensaMealDetailState.Loading
        viewModelScope.launch {
            meal.value = try {
                repository.meal(route.id, day)?.let {
                    MensaMealDetailState.Loaded(it, MensaPriceGroup.Studierende, isFavorite = false)
                } ?: MensaMealDetailState.NotFound
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                MensaMealDetailState.Failed(e)
            }
        }
    }

    /** Ohne Tag gilt heute (Berlin). Ein unlesbarer Tag ist ungültig. */
    private fun parseDay(text: String?): LocalDate? =
        if (text == null) BerlinTime.date(clock.instant()) else runCatching { LocalDate.parse(text) }.getOrNull()
}
