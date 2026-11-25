package de.kaigothe.campusilmenau.ui.thoska

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.data.repository.ThoskaRepository
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaCardNaming
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaCardOption
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Verlauf der Thoska-Auslesungen. [cards] sind die Karten, zuletzt ausgelesene zuerst, [selectedCardId] die gewählte (ohne
 * Wahl die zuletzt ausgelesene), [readings] die Auslesungen dieser Karte. Der Fallback-Name braucht Ressourcen, darum steht
 * er hier noch nicht: [ThoskaCardOption.displayName] ist bei Karten ohne Namen leer, die Ansicht setzt „Karte …C3D4“ ein.
 */
data class ThoskaHistoryState(
    val cards: List<ThoskaCardOption> = emptyList(),
    val selectedCardId: String? = null,
    val readings: List<ThoskaReading> = emptyList(),
    val now: Instant,
)

@HiltViewModel
class ThoskaHistoryViewModel @Inject constructor(
    private val repository: ThoskaRepository,
    private val clock: Clock,
) : ViewModel() {

    private val selected = MutableStateFlow<String?>(null)

    val state: StateFlow<ThoskaHistoryState> = combine(repository.balances, repository.names, selected) { balances, names, selected ->
        // Karten, zuletzt ausgelesene zuerst.
        val latest = balances.groupBy { it.cardId }.mapValues { (_, list) -> list.maxOf { it.scannedAt } }
        val cards = latest.entries.sortedByDescending { it.value }.map { (id, _) ->
            val custom = ThoskaCardNaming.customName(id, names)
            ThoskaCardOption(id = id, displayName = custom.orEmpty(), customName = custom)
        }
        val effective = selected?.takeIf { id -> cards.any { it.id == id } } ?: cards.firstOrNull()?.id
        ThoskaHistoryState(
            cards = cards,
            selectedCardId = effective,
            readings = balances.filter { it.cardId == effective }.map { ThoskaReading(it.scannedAt, it.balanceMilli, it.lastTransactionMilli) },
            now = clock.instant(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ThoskaHistoryState(now = clock.instant()))

    fun selectCard(id: String) {
        selected.value = id
    }

    fun rename(cardId: String, name: String) {
        viewModelScope.launch { repository.rename(cardId, name) }
    }
}
