package de.kaigothe.campusilmenau.ui.bibliothek.katalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.data.repository.LibraryCatalogRepository
import de.kaigothe.campusilmenau.data.repository.LibraryCoverLoader
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TitlePhase {
    data object Loading : TitlePhase
    data object Loaded : TitlePhase

    /** 404. */
    data object NotFound : TitlePhase

    /** 503 oder Netzwerkfehler, ohne Daten, die man stattdessen zeigen könnte. */
    data object Failed : TitlePhase
}

data class LibraryTitleState(
    val phase: TitlePhase = TitlePhase.Loading,
    val detail: LibraryMediaDetail? = null,
    /** `/features` erlaubt Inhaltsverzeichnis und Klappentext. */
    val contentsEnabled: Boolean = false,
    val tocOpen: Boolean = true,
    val blurbOpen: Boolean = false,
    /** Einmalige Meldung nach einem fehlgeschlagenen Aktualisieren; die Seite zeigt weiter ihre Daten. */
    val refreshFailed: Boolean = false,
) {
    val media: LibraryMedia? get() = detail?.media

    /** Inhaltsverzeichnis und Klappentext nur, wenn `/features` es erlaubt und etwas da ist. */
    val showsContents: Boolean get() = contentsEnabled && detail?.hasContents == true
}

/** Zustand der Titelansicht: lädt den Titel mit Exemplaren und verwaltet die Aufklappbereiche. */
@HiltViewModel(assistedFactory = LibraryTitleViewModel.Factory::class)
class LibraryTitleViewModel @AssistedInject constructor(
    @Assisted("id") private val id: String,
    @Assisted("searchId") private val searchId: String?,
    private val repository: LibraryCatalogRepository,
    private val covers: LibraryCoverLoader,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("id") id: String, @Assisted("searchId") searchId: String?): LibraryTitleViewModel
    }

    private val _state = MutableStateFlow(LibraryTitleState())
    val state: StateFlow<LibraryTitleState> = _state.asStateFlow()

    init {
        load(track = true)
    }

    /** Ziehen zum Aktualisieren und „Erneut versuchen“: nicht noch einmal als Aufruf zählen. */
    fun refresh() = load(track = false)

    private fun load(track: Boolean) {
        viewModelScope.launch {
            if (_state.value.detail == null) _state.update { it.copy(phase = TitlePhase.Loading) }
            try {
                // Die Suche nach Inhalten ist ein Zusatz: Ein Fehler hier verhindert den Titel nicht.
                val contents = try {
                    repository.contentsAvailable()
                } catch (_: ApiError) {
                    false
                }
                val detail = repository.media(id, searchId, track = if (track) null else false)
                _state.update { it.copy(phase = TitlePhase.Loaded, detail = detail, contentsEnabled = contents, refreshFailed = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                _state.update {
                    when {
                        // Die Seite zeigt schon Daten: behalten und nur melden.
                        it.detail != null -> it.copy(refreshFailed = true)
                        e is ApiError.Http && e.isNotFound -> it.copy(phase = TitlePhase.NotFound)
                        else -> it.copy(phase = TitlePhase.Failed)
                    }
                }
            }
        }
    }

    fun toggleToc() = _state.update { it.copy(tocOpen = !it.tocOpen) }
    fun toggleBlurb() = _state.update { it.copy(blurbOpen = !it.blurbOpen) }
    fun refreshFailedShown() = _state.update { it.copy(refreshFailed = false) }

    suspend fun cover(media: LibraryMedia) = covers.image(media)
}
