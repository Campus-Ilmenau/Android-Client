package de.kaigothe.campusilmenau.ui.bibliothek.merkliste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.data.repository.LibraryCatalogRepository
import de.kaigothe.campusilmenau.data.repository.LibraryCoverLoader
import de.kaigothe.campusilmenau.data.repository.MerklisteRepository
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.merkliste.GemerkterTitel
import de.kaigothe.campusilmenau.domain.model.library.merkliste.MerklisteExemplar
import de.kaigothe.campusilmenau.domain.model.library.merkliste.MerklisteListe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Was „Widerrufen“ rückgängig macht. */
sealed interface MerklisteWiderruf {
    data class Entfernt(val titel: GemerkterTitel) : MerklisteWiderruf

    /** Der Eintrag vor dem Abhaken bzw. Zurücknehmen. */
    data class Gefunden(val vorher: GemerkterTitel) : MerklisteWiderruf
}

/** Meldung unten, ggf. mit „Widerrufen“. */
data class MerklisteMeldung(val text: UiText, val widerruf: MerklisteWiderruf?)

data class MerklisteUiState(
    val liste: MerklisteListe = MerklisteListe(emptyList(), emptyList()),
    /** Status je ID. Fehlt eine ID, lädt sie noch. */
    val status: Map<String, LibraryAvailabilityStatus> = emptyMap(),
    val exemplare: Map<String, MerklisteExemplar> = emptyMap(),
    val clearDialogVisible: Boolean = false,
) {
    /** „3 Titel · 1 gefunden“. */
    val zusammenfassung: UiText
        get() {
            val anzahl = liste.anzahl
            val gefunden = liste.gefunden.size
            return when {
                anzahl == 0 -> UiText(R.string.bib_merkliste_noch_keine)
                gefunden > 0 -> UiText(R.string.bib_merkliste_zusammen_gefunden, anzahl, gefunden)
                anzahl == 1 -> UiText(R.string.bib_merkliste_ein_titel)
                else -> UiText(R.string.bib_merkliste_zusammen, anzahl)
            }
        }
}

/**
 * Zustand der Merkliste: Titel aus Room, Status und Exemplare frisch aus dem Katalog (nie gespeichert), Entfernen und
 * „Gefunden“ widerrufbar.
 */
@HiltViewModel
class MerklisteViewModel @Inject constructor(
    private val repository: MerklisteRepository,
    private val catalog: LibraryCatalogRepository,
    private val covers: LibraryCoverLoader,
) : ViewModel() {

    private data class Local(
        val status: Map<String, LibraryAvailabilityStatus> = emptyMap(),
        val exemplare: Map<String, MerklisteExemplar> = emptyMap(),
        val clearDialogVisible: Boolean = false,
    )

    private val local = MutableStateFlow(Local())
    private val _meldungen = MutableSharedFlow<MerklisteMeldung>(extraBufferCapacity = 4)

    /** Meldungen für den Toast; „Widerrufen“ ruft [widerrufen] auf. */
    val meldungen: SharedFlow<MerklisteMeldung> = _meldungen.asSharedFlow()

    val state: StateFlow<MerklisteUiState> = combine(repository.liste, local) { liste, local ->
        MerklisteUiState(liste, local.status, local.exemplare, local.clearDialogVisible)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MerklisteUiState())

    /** Status aller gemerkten Titel frisch laden, in Paketen von 20 IDs (parallel). Fehler zeigen „Unbekannt“. */
    fun ladeStatus() {
        viewModelScope.launch {
            val ids = repository.liste.first().let { l -> (l.offen + l.gefunden).map { it.id } }
            if (ids.isEmpty()) return@launch
            val loaded = try {
                catalog.availability(ids)
            } catch (_: ApiError) {
                null
            }
            local.update { l ->
                l.copy(
                    status = l.status + ids.associateWith { loaded?.get(it)?.status ?: LibraryAvailabilityStatus.Unknown },
                    exemplare = l.exemplare + (loaded?.mapNotNull { (id, a) -> MerklisteExemplar.of(a)?.let { id to it } } ?: emptyList()),
                )
            }
        }
    }

    fun entfernen(titel: GemerkterTitel) {
        viewModelScope.launch {
            val removed = repository.entfernen(titel.id) ?: return@launch
            _meldungen.emit(MerklisteMeldung(UiText(R.string.bib_merkliste_entfernt, titel.anzeigeTitel), MerklisteWiderruf.Entfernt(removed)))
        }
    }

    /** Hakt ab oder nimmt zurück; die Zeile wandert in den anderen Abschnitt. */
    fun setzeGefunden(titel: GemerkterTitel, gefunden: Boolean) {
        viewModelScope.launch {
            val vorher = repository.setzeGefunden(titel.id, gefunden) ?: return@launch
            val text = UiText(if (gefunden) R.string.bib_merkliste_gefunden_meldung else R.string.bib_merkliste_wieder_offen, titel.anzeigeTitel)
            _meldungen.emit(MerklisteMeldung(text, MerklisteWiderruf.Gefunden(vorher)))
        }
    }

    fun widerrufen(widerruf: MerklisteWiderruf) {
        viewModelScope.launch {
            when (widerruf) {
                is MerklisteWiderruf.Entfernt -> {
                    repository.wiederherstellen(widerruf.titel)
                    ladeStatus()
                }
                is MerklisteWiderruf.Gefunden -> repository.widerrufeGefunden(widerruf.vorher)
            }
        }
    }

    fun zeigeLeerenDialog() = local.update { it.copy(clearDialogVisible = true) }
    fun schliesseLeerenDialog() = local.update { it.copy(clearDialogVisible = false) }

    fun leeren() {
        viewModelScope.launch {
            local.update { it.copy(clearDialogVisible = false) }
            repository.alleEntfernen()
            _meldungen.emit(MerklisteMeldung(UiText(R.string.bib_merkliste_geleert), widerruf = null))
        }
    }

    /** Das Cover des Titels (mit Wiederholung bei „wird noch geholt“). */
    suspend fun cover(titel: GemerkterTitel) = covers.image(titel.alsMedia)
}
