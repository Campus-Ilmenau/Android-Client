package de.kaigothe.campusilmenau.ui.mehr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.data.repository.MerklisteRepository
import de.kaigothe.campusilmenau.data.repository.PaiaRepository
import de.kaigothe.campusilmenau.data.repository.ThoskaRepository
import de.kaigothe.campusilmenau.domain.model.Darstellung
import de.kaigothe.campusilmenau.domain.model.library.merkliste.GemerkterTitel
import de.kaigothe.campusilmenau.domain.model.mehr.ThoskaKarteUebersicht
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GespeicherteDatenUiState(
    val karten: List<ThoskaKarteUebersicht> = emptyList(),
    val kartenNamen: Int = 0,
    val merkliste: List<GemerkterTitel> = emptyList(),
    val gemerkteVeranstaltungen: Int = 0,
    val darstellung: Darstellung = Darstellung.System,
    val heuteMensa: MensaVenue = MensaVenue.MensaEhrenberg,
    val priceGroup: MensaPriceGroup = MensaPriceGroup.Studierende,
    val mensaFilter: MensaFilter = MensaFilter(),
    val ausgeblendeteHinweise: Int = 0,
    val gemerkteGerichte: Int = 0,
    /** Gespeicherte Zugangsdaten („Angemeldet bleiben“). */
    val speichertZugangsdaten: Boolean = false,
    val loeschenDialog: Boolean = false,
)

/** „Gespeicherte Daten“: zeigt alles, was die App auf diesem Gerät ablegt, und löscht es auf Wunsch. */
@HiltViewModel
class GespeicherteDatenViewModel @Inject constructor(
    private val thoska: ThoskaRepository,
    private val merkliste: MerklisteRepository,
    private val settings: AppSettings,
    private val paia: PaiaRepository,
) : ViewModel() {

    private val dialog = MutableStateFlow(false)

    private data class Einstellungen(
        val darstellung: Darstellung,
        val heuteMensa: MensaVenue,
        val priceGroup: MensaPriceGroup,
        val filter: MensaFilter,
        val hinweise: Int,
        val gerichte: Int,
        val veranstaltungen: Int,
    )

    private val einstellungen = combine(
        settings.darstellung, settings.heuteMensa, settings.priceGroup, settings.mensaFilter,
        combine(settings.dismissedNotices, settings.mensaFavorites, settings.eventBookmarks) { a, b, c -> Triple(a.size, b.size, c.size) },
    ) { darstellung, mensa, preis, filter, zahlen ->
        Einstellungen(darstellung, mensa, preis, filter, zahlen.first, zahlen.second, zahlen.third)
    }

    val state: StateFlow<GespeicherteDatenUiState> = combine(
        thoska.balances, thoska.names, merkliste.liste, einstellungen, combine(paia.state, dialog) { p, d -> p.remembersCredentials to d },
    ) { balances, names, liste, e, extra ->
        GespeicherteDatenUiState(
            karten = ThoskaKarteUebersicht.of(balances, names),
            kartenNamen = names.size,
            merkliste = liste.offen + liste.gefunden,
            gemerkteVeranstaltungen = e.veranstaltungen,
            darstellung = e.darstellung,
            heuteMensa = e.heuteMensa,
            priceGroup = e.priceGroup,
            mensaFilter = e.filter,
            ausgeblendeteHinweise = e.hinweise,
            gemerkteGerichte = e.gerichte,
            speichertZugangsdaten = extra.first,
            loeschenDialog = extra.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GespeicherteDatenUiState())

    fun zeigeLoeschen() = dialog.update { true }
    fun schliesseLoeschen() = dialog.update { false }

    /**
     * Löscht alles, was die App lokal ablegt: Thoska-Verlauf und Kartennamen, Merkliste, Einstellungen und gemerkte
     * Veranstaltungen sowie die Sitzung und die gespeicherten Zugangsdaten der Bibliothek.
     */
    fun alleLoeschen() {
        dialog.update { false }
        viewModelScope.launch {
            thoska.deleteAll()
            merkliste.alleEntfernen()
            settings.clearAll()
            paia.logout()
        }
    }
}
