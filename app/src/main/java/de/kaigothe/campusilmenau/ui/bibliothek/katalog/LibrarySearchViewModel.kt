package de.kaigothe.campusilmenau.ui.bibliothek.katalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.data.repository.LibraryCatalogRepository
import de.kaigothe.campusilmenau.data.repository.LibraryCoverLoader
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailability
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMedia
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryMediaFilter
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchQuery
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchResponse
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibrarySearchSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

sealed interface SearchPhase {
    /** Noch nichts gesucht. */
    data object Start : SearchPhase
    data object Loading : SearchPhase
    data object Results : SearchPhase

    /** Die Suche war erfolgreich, aber ohne Treffer. */
    data object Empty : SearchPhase
    data class Failed(val error: ApiError) : SearchPhase
}

/** Ein Block der Trefferliste. Bei gemischten Antworten gibt es zwei mit Überschrift. */
data class SearchSection(val id: String, val title: UiText?, val items: List<LibraryMedia>, val fromCatalog: Boolean)

data class PageState(val query: LibrarySearchQuery, val totalCount: Int, val hasMore: Boolean)

/** Eingaben der erweiterten Suche. Die Felder zählen nur, solange der Bereich offen ist. */
data class AdvancedFields(
    val title: String = "",
    val author: String = "",
    val isbn: String = "",
    val subject: String = "",
    val yearFrom: String = "",
    val yearTo: String = "",
) {
    val isFilled: Boolean get() = listOf(title, author, isbn, subject, yearFrom, yearTo).any { it.isNotBlank() }
}

data class LibrarySearchState(
    val text: String = "",
    val kind: LibraryMediaFilter = LibraryMediaFilter.PrintBook,
    val advancedOpen: Boolean = false,
    val fields: AdvancedFields = AdvancedFields(),
    /** Fehler der erweiterten Suche (Jahre, fehlender Begriff), unter den Feldern. */
    val formError: UiText? = null,
    val phase: SearchPhase = SearchPhase.Start,
    val executedQuery: LibrarySearchQuery? = null,
    val response: LibrarySearchResponse? = null,
    val items: List<LibraryMedia> = emptyList(),
    val basePage: PageState? = null,
    /** Fortsetzung im Gesamtkatalog („Im Gesamtkatalog weitersuchen“). */
    val continuationItems: List<LibraryMedia> = emptyList(),
    val continuationPage: PageState? = null,
    /** Die Fortsetzung lief schon (auch ohne Treffer). */
    val catalogTried: Boolean = false,
    val isStale: Boolean = false,
    val staleSince: Instant? = null,
    val isLoadingMore: Boolean = false,
    val isContinuing: Boolean = false,
    val isRefreshing: Boolean = false,
    /** Fehler beim Nachladen oder Aktualisieren; die Liste bleibt stehen. */
    val loadMoreError: ApiError? = null,
    /** Die Suche dauert länger: Hinweis auf den Gesamtkatalog. */
    val showsCatalogHint: Boolean = false,
    val contentsEnabled: Boolean = false,
    /** Verfügbarkeit je Titel, nachgeladen für die sichtbaren Karten. */
    val availability: Map<String, LibraryAvailability> = emptyMap(),
    /** `search_id` je Titel: gilt nur bis zur nächsten Suche und geht beim Öffnen eines Titels mit. */
    val searchIds: Map<String, String> = emptyMap(),
) {
    /** Feldsuche und `in=catalog` laufen im Gesamtkatalog; die Antwort der Feldsuche trägt kein `source`. */
    val isCatalogOnlySearch: Boolean
        get() {
            val response = response ?: return false
            if (response.source == LibrarySearchSource.Catalog) return true
            return response.source == null && executedQuery?.isFieldSearch == true
        }

    val sections: List<SearchSection>
        get() = buildList {
            val response = response
            if (response != null) {
                if (response.source == LibrarySearchSource.Mixed) {
                    val local = items.filter { !response.isFromCatalog(it) }
                    val catalog = items.filter { response.isFromCatalog(it) }
                    if (local.isNotEmpty()) add(SearchSection("local", UiText(R.string.bib_kat_sektion_bestand), local, fromCatalog = false))
                    if (catalog.isNotEmpty()) add(SearchSection("catalog", UiText(R.string.bib_kat_sektion_katalog), catalog, fromCatalog = true))
                } else if (items.isNotEmpty()) {
                    add(SearchSection("main", null, items, isCatalogOnlySearch))
                }
            }
            if (continuationItems.isNotEmpty()) {
                add(
                    SearchSection(
                        "continued",
                        if (items.isEmpty()) null else UiText(R.string.bib_kat_sektion_weitere),
                        continuationItems,
                        fromCatalog = true,
                    ),
                )
            }
        }

    /** Die Inhaltssuche hat gegriffen: `/features` erlaubt sie und ein Treffer steht im Inhaltsverzeichnis oder Klappentext. */
    val searchedInContents: Boolean
        get() = contentsEnabled && items.any { it.match?.isInTableOfContents == true || it.match?.isInBlurb == true }

    /** Kopfzeile der Treffer nach der Tabelle in `Uebergabe.md`. */
    val header: Pair<UiText, UiText?>?
        get() {
            val response = response
            val query = executedQuery
            if (phase != SearchPhase.Results || response == null || query == null) return null
            val continuation = continuationPage
            // Nur Treffer aus der Fortsetzung (der Bestand hatte nichts).
            if (items.isEmpty() && continuation != null) {
                return UiText(R.string.bib_kat_treffer_gesamtkatalog, continuation.totalCount) to null
            }
            val line1: UiText
            var line2: UiText?
            if (isCatalogOnlySearch) {
                line1 = UiText(R.string.bib_kat_treffer_gesamtkatalog, response.totalCount)
                line2 = if (query.isFieldSearch) fieldSummary(query) else null
            } else if (response.source == LibrarySearchSource.Mixed) {
                line1 = UiText(R.string.bib_kat_treffer, response.totalCount)
                line2 = UiText(R.string.bib_kat_gemischt)
            } else if (response.catalogSuggested) {
                line1 = UiText(R.string.bib_kat_treffer_bestand, response.totalCount)
                line2 = UiText(R.string.bib_kat_katalog_zu_spaet)
            } else {
                line1 = response.correctedQuery?.let { UiText(R.string.bib_kat_ergebnisse_fuer, it) }
                    ?: UiText(R.string.bib_kat_treffer, response.totalCount)
                line2 = if (searchedInContents) UiText(R.string.bib_kat_inhalte_gesucht) else UiText(R.string.bib_kat_treffer_bestand, response.totalCount)
            }
            if (continuation != null) line2 = UiText(R.string.bib_kat_ergaenzt, continuation.totalCount)
            return line1 to line2
        }

    /** Hinweis bei veralteten Daten: „Stand von 10:12 – …“. */
    val staleNotice: UiText?
        get() {
            if (!isStale) return null
            val since = staleSince ?: return UiText(R.string.bib_kat_stale)
            val minutes = BerlinTime.minutesOfDay(since)
            return UiText(R.string.bib_kat_stale_stand, "%02d:%02d".format(minutes / 60, minutes % 60))
        }

    /** Fortsetzung im Gesamtkatalog anbieten, weil der Verbund nicht rechtzeitig geantwortet hat. */
    val offersCatalogContinuation: Boolean
        get() = phase == SearchPhase.Results && response?.catalogSuggested == true && !catalogTried && !isCatalogOnlySearch

    /** Am Ende einer gemischten Liste: „Im Gesamtkatalog suchen“. */
    val offersMixedEnd: Boolean
        get() = phase == SearchPhase.Results && response?.source == LibrarySearchSource.Mixed && !catalogTried

    /** Bei „keine Treffer“ auch den Gesamtkatalog anbieten. */
    val offersCatalogForEmpty: Boolean
        get() = phase == SearchPhase.Empty && !catalogTried && !isCatalogOnlySearch

    val loadMoreCount: Int?
        get() {
            val page = basePage ?: return null
            if (!page.hasMore || response?.source == LibrarySearchSource.Mixed) return null
            return minOf(LibrarySearchQuery.PAGE_SIZE, maxOf(1, page.totalCount - items.size))
        }

    val loadMoreCountInCatalog: Int?
        get() {
            val page = continuationPage ?: return null
            if (!page.hasMore) return null
            return minOf(LibrarySearchQuery.PAGE_SIZE, maxOf(1, page.totalCount - continuationItems.size - items.size))
        }

    /** Wie die Suche in „Keine Treffer für …“ genannt wird. */
    val emptyQueryText: UiText
        get() {
            val trimmed = text.trim()
            if (trimmed.isNotEmpty()) return UiText.plain(trimmed)
            return executedQuery?.let(::fieldSummary) ?: UiText.plain("")
        }
}

/** „Person „Zacher“ · E-Book · 2015–2024 · neueste zuerst“. */
fun fieldSummary(query: LibrarySearchQuery): UiText {
    val parts = buildList {
        if (query.text.isNotBlank()) add(UiText(R.string.bib_kat_feld_text, query.text.trim()))
        if (query.title.isNotBlank()) add(UiText(R.string.bib_kat_feld_titel, query.title.trim()))
        if (query.author.isNotBlank()) add(UiText(R.string.bib_kat_feld_person, query.author.trim()))
        if (query.isbn.isNotBlank()) add(UiText(R.string.bib_kat_feld_isbn, query.isbn.trim()))
        if (query.subject.isNotBlank()) add(UiText(R.string.bib_kat_feld_schlagwort, query.subject.trim()))
        query.mediaType?.let { add(UiText(it.pillTitle())) }
        val from = query.yearFrom.trim()
        val to = query.yearTo.trim()
        when {
            from.isNotEmpty() && to.isNotEmpty() -> add(UiText.plain("$from–$to"))
            from.isNotEmpty() -> add(UiText(R.string.bib_kat_feld_ab, from))
            to.isNotEmpty() -> add(UiText(R.string.bib_kat_feld_bis, to))
        }
        add(UiText(R.string.bib_kat_feld_neueste))
    }
    return parts.reduce { acc, next -> UiText(R.string.bib_kat_verbinder, acc, next) }
}

fun LibraryMediaFilter.pillTitle(): Int = when (this) {
    LibraryMediaFilter.PrintBook -> R.string.bib_kat_pille_gedruckt
    LibraryMediaFilter.Ebook -> R.string.bib_kat_pille_ebook
    LibraryMediaFilter.Journal -> R.string.bib_kat_pille_zeitschrift
    LibraryMediaFilter.All -> R.string.bib_kat_pille_alle
}

/**
 * Zustand und Ablauf der Katalogsuche: wann gesucht wird (700 ms nach dem letzten Tippen, sofort bei Return, Medienart
 * und „Suchen“) und was die Oberfläche zeigt. Die Netzwerkarbeit liegt im [LibraryCatalogRepository].
 */
@HiltViewModel
class LibrarySearchViewModel @Inject constructor(
    private val repository: LibraryCatalogRepository,
    private val covers: LibraryCoverLoader,
) : ViewModel() {

    private val _state = MutableStateFlow(LibrarySearchState())
    val state: StateFlow<LibrarySearchState> = _state.asStateFlow()

    private var debounceJob: Job? = null
    private var searchJob: Job? = null
    private var hintJob: Job? = null
    private var availabilityJob: Job? = null
    private var lastStartedQuery: LibrarySearchQuery? = null
    private var generation = 0
    private val pendingAvailability = ArrayDeque<String>()
    private val requestedAvailability = mutableSetOf<String>()
    private var didLoadFeatures = false

    /** Beim Erscheinen: einmal abfragen, ob Inhaltsangaben erlaubt sind. Wird nicht gespeichert. */
    fun onAppear() {
        if (didLoadFeatures) return
        didLoadFeatures = true
        viewModelScope.launch {
            val enabled = try {
                repository.contentsAvailable()
            } catch (_: ApiError) {
                false
            }
            _state.update { it.copy(contentsEnabled = enabled) }
        }
    }

    /** Ein Begriff von außen (Link `?q=`, Sprachassistent): im Feld, Treffer sofort. */
    fun searchFromOutside(term: String) {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return
        debounceJob?.cancel()
        // Eine offene erweiterte Suche ersetzt die Suche nach dem Begriff.
        _state.update { it.copy(text = trimmed.take(LibrarySearchQuery.MAX_TEXT_LENGTH), advancedOpen = false, formError = null) }
        startSearch()
    }

    /** Nach jeder Änderung im Suchfeld: kurz warten, dann suchen. Bei geöffneter erweiterter Suche gilt nur „Suchen“. */
    fun onTextChange(newText: String) {
        val text = newText.take(LibrarySearchQuery.MAX_TEXT_LENGTH)
        _state.update { it.copy(text = text) }
        debounceJob?.cancel()
        if (_state.value.advancedOpen) return
        if (text.isBlank()) {
            reset()
            return
        }
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE)
            startSearch()
        }
    }

    /** Return-Taste oder „Suchen“: sofort suchen. */
    fun submit() {
        debounceJob?.cancel()
        startSearch()
    }

    fun selectKind(kind: LibraryMediaFilter) {
        if (_state.value.kind == kind) return
        _state.update { it.copy(kind = kind) }
        // Nur neu suchen, wenn schon etwas gesucht wurde oder ein Begriff dasteht.
        if (currentQuery() != null) submit()
    }

    fun toggleAdvanced() = _state.update { it.copy(advancedOpen = !it.advancedOpen, formError = null) }

    fun updateFields(transform: (AdvancedFields) -> AdvancedFields) {
        _state.update {
            val next = transform(it.fields)
            it.copy(
                fields = next.copy(
                    yearFrom = next.yearFrom.take(4),
                    yearTo = next.yearTo.take(4),
                ),
            )
        }
    }

    fun clearText() {
        debounceJob?.cancel()
        _state.update { it.copy(text = "") }
        if (!_state.value.advancedOpen) reset()
    }

    /** „Erneut versuchen“ nach einem Fehler: dieselbe Anfrage, auch wenn die erweiterte Suche zugeklappt ist. */
    fun retry() {
        debounceJob?.cancel()
        startSearch(lastStartedQuery)
    }

    /** „Suchen“ in der erweiterten Suche: sofort suchen, dann Felder zuklappen. Die Anfrage entsteht vor dem Zuklappen. */
    fun submitAdvanced() {
        debounceJob?.cancel()
        startSearch()
        if (_state.value.formError == null) _state.update { it.copy(advancedOpen = false) }
    }

    /**
     * Pull to Refresh: dieselbe Suche noch einmal. Die alte Liste bleibt stehen, bis die neue da ist; bei einem Fehler bleibt
     * sie erhalten. Ohne Treffer in der Anzeige (Fehler, leer) läuft die normale Suche.
     */
    fun refresh() {
        val current = _state.value
        val query = current.executedQuery
        if (current.phase != SearchPhase.Results || query == null) {
            if (current.phase == SearchPhase.Start || current.phase == SearchPhase.Loading) return
            debounceJob?.cancel()
            startSearch()
            return
        }
        searchJob?.cancel()
        val mine = ++generation
        _state.update { it.copy(loadMoreError = null, isRefreshing = true) }
        searchJob = viewModelScope.launch {
            try {
                val result = repository.search(query)
                if (mine != generation) return@launch
                resetAvailability()
                _state.update { apply(it.copy(continuationItems = emptyList(), continuationPage = null, catalogTried = false, availability = emptyMap(), searchIds = emptyMap()), result, query) }
            } catch (e: ApiError) {
                if (mine == generation) _state.update { it.copy(loadMoreError = e) }
            } finally {
                if (mine == generation) _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /** Die Anfrage aus den Eingaben. Die Felder der erweiterten Suche zählen nur, solange der Bereich offen ist. */
    fun currentQuery(page: Int = 1): LibrarySearchQuery? = queryOf(_state.value, page)

    private fun queryOf(state: LibrarySearchState, page: Int = 1): LibrarySearchQuery? {
        val useFields = state.advancedOpen && state.fields.isFilled
        var query = LibrarySearchQuery(text = state.text, mediaType = state.kind, page = page)
        if (useFields) {
            val f = state.fields
            query = query.copy(title = f.title, author = f.author, isbn = f.isbn, subject = f.subject, yearFrom = f.yearFrom, yearTo = f.yearTo)
        }
        return if (query.hasSearchTerm || useFields) query else null
    }

    private fun startSearch(override: LibrarySearchQuery? = null) {
        val query = override ?: currentQuery()
        if (query == null) {
            reset()
            return
        }
        val issue = query.validate().firstOrNull()
        if (issue != null) {
            // Jahre und fehlende Begriffe der erweiterten Suche erscheinen unter den Feldern.
            _state.update { it.copy(formError = if (it.advancedOpen) issue.message else null) }
            if (!_state.value.advancedOpen) reset()
            return
        }
        lastStartedQuery = query
        searchJob?.cancel()
        val mine = ++generation
        beginLoading()
        searchJob = viewModelScope.launch {
            try {
                val result = repository.search(query)
                if (mine != generation) return@launch
                hintJob?.cancel()
                _state.update { apply(it, result, query).copy(showsCatalogHint = false) }
            } catch (e: ApiError) {
                if (mine != generation) return@launch
                hintJob?.cancel()
                _state.update { it.copy(phase = SearchPhase.Failed(e), showsCatalogHint = false) }
            }
        }
    }

    private fun beginLoading() {
        resetAvailability()
        _state.update {
            it.copy(
                phase = SearchPhase.Loading, response = null, items = emptyList(), basePage = null,
                continuationItems = emptyList(), continuationPage = null, catalogTried = false,
                isStale = false, staleSince = null, loadMoreError = null, availability = emptyMap(),
                searchIds = emptyMap(), showsCatalogHint = false, formError = null,
            )
        }
        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            delay(CATALOG_HINT_DELAY)
            _state.update { it.copy(showsCatalogHint = true) }
        }
    }

    private fun apply(state: LibrarySearchState, response: LibrarySearchResponse, query: LibrarySearchQuery): LibrarySearchState {
        val items = response.items.distinctBy { it.id }
        return state.copy(
            response = response,
            executedQuery = query,
            items = items,
            searchIds = state.searchIds + items.mapNotNull { m -> response.searchId?.let { m.id to it } },
            basePage = PageState(query, response.totalCount, response.hasMore),
            isStale = response.isStale,
            staleSince = response.oldestFetchedAt,
            phase = if (items.isEmpty()) SearchPhase.Empty else SearchPhase.Results,
        )
    }

    /** Zurück zum Start, wenn das Feld leer ist. */
    private fun reset() {
        searchJob?.cancel()
        hintJob?.cancel()
        generation++
        resetAvailability()
        _state.update {
            it.copy(
                phase = SearchPhase.Start, response = null, items = emptyList(), basePage = null,
                continuationItems = emptyList(), continuationPage = null, catalogTried = false,
                isStale = false, showsCatalogHint = false, loadMoreError = null, availability = emptyMap(),
                searchIds = emptyMap(), executedQuery = null,
            )
        }
    }

    private fun resetAvailability() {
        availabilityJob?.cancel()
        availabilityJob = null
        pendingAvailability.clear()
        requestedAvailability.clear()
    }

    // region Mehr laden

    /** „Weitere 20 Treffer laden“ für die Liste des Bestands bzw. der Feldsuche. */
    fun loadMore() {
        val current = _state.value
        val page = current.basePage ?: return
        if (!page.hasMore || current.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        val mine = generation
        val next = page.query.nextPage()
        viewModelScope.launch {
            try {
                val result = repository.search(next)
                if (mine != generation) return@launch
                _state.update { s ->
                    val fresh = result.itemsExcluding(s.items.mapTo(mutableSetOf()) { it.id })
                    s.copy(
                        items = s.items + fresh,
                        searchIds = s.searchIds + fresh.mapNotNull { m -> result.searchId?.let { m.id to it } },
                        basePage = PageState(next, result.totalCount, result.hasMore),
                        isStale = s.isStale || result.isStale,
                    )
                }
            } catch (e: ApiError) {
                if (mine == generation) _state.update { it.copy(loadMoreError = e) }
            } finally {
                if (mine == generation) _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    /**
     * „Im Gesamtkatalog (weiter)suchen“: dieselbe Suche mit `in=catalog` ab Seite 1. Die Katalog-Liste enthält die schon
     * gezeigten Titel noch einmal, deshalb kommen sie nach `id` nicht doppelt in die Anzeige.
     */
    fun continueInCatalog() {
        val current = _state.value
        val base = current.executedQuery ?: return
        if (current.isContinuing) return
        _state.update { it.copy(isContinuing = true, loadMoreError = null) }
        val mine = generation
        val query = base.continuedInCatalog()
        viewModelScope.launch {
            try {
                val result = repository.search(query)
                if (mine != generation) return@launch
                _state.update { s ->
                    val shown = s.items.mapTo(mutableSetOf()) { it.id }
                    val fresh = result.itemsExcluding(shown).distinctBy { it.id }
                    s.copy(
                        continuationItems = fresh,
                        searchIds = s.searchIds + fresh.mapNotNull { m -> result.searchId?.let { m.id to it } },
                        continuationPage = PageState(query, result.totalCount, result.hasMore),
                        catalogTried = true,
                        isStale = s.isStale || result.isStale,
                        phase = if (fresh.isNotEmpty()) SearchPhase.Results else s.phase,
                    )
                }
            } catch (e: ApiError) {
                if (mine == generation) _state.update { it.copy(loadMoreError = e) }
            } finally {
                if (mine == generation) _state.update { it.copy(isContinuing = false) }
            }
        }
    }

    /** „Weitere … Treffer laden“ in der Fortsetzung. */
    fun loadMoreInCatalog() {
        val current = _state.value
        val page = current.continuationPage ?: return
        if (!page.hasMore || current.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        val mine = generation
        val next = page.query.nextPage()
        viewModelScope.launch {
            try {
                val result = repository.search(next)
                if (mine != generation) return@launch
                _state.update { s ->
                    val shown = (s.items + s.continuationItems).mapTo(mutableSetOf()) { it.id }
                    val fresh = result.itemsExcluding(shown).distinctBy { it.id }
                    s.copy(
                        continuationItems = s.continuationItems + fresh,
                        searchIds = s.searchIds + fresh.mapNotNull { m -> result.searchId?.let { m.id to it } },
                        continuationPage = PageState(next, result.totalCount, result.hasMore),
                        isStale = s.isStale || result.isStale,
                    )
                }
            } catch (e: ApiError) {
                if (mine == generation) _state.update { it.copy(loadMoreError = e) }
            } finally {
                if (mine == generation) _state.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    // endregion

    // region Status der sichtbaren Treffer

    /** Eine Karte wird sichtbar: ihren Status zum nächsten Stapel (höchstens 20 IDs) hinzufügen. */
    fun requestAvailability(id: String) {
        if (_state.value.availability.containsKey(id) || !requestedAvailability.add(id)) return
        pendingAvailability.addLast(id)
        if (availabilityJob?.isActive == true) return
        val mine = generation
        availabilityJob = viewModelScope.launch {
            delay(AVAILABILITY_WINDOW)
            flushAvailability(mine)
        }
    }

    private suspend fun flushAvailability(mine: Int) {
        while (pendingAvailability.isNotEmpty()) {
            val batch = List(minOf(LibraryCatalogRepository.AVAILABILITY_BATCH, pendingAvailability.size)) { pendingAvailability.removeFirst() }
            val loaded: Map<String, LibraryAvailability>? = try {
                repository.availability(batch)
            } catch (e: CancellationException) {
                throw e
            } catch (_: ApiError) {
                null
            }
            if (mine != generation) return
            // Ohne Antwort bleibt „Status unbekannt“ statt eines endlosen Platzhalters.
            val unknown = { id: String -> LibraryAvailability(id, LibraryAvailabilityStatus.Unknown, emptyList()) }
            _state.update { s -> s.copy(availability = s.availability + batch.associateWith { loaded?.get(it) ?: unknown(it) }) }
        }
    }

    // endregion

    /** Das Cover des Titels (mit Wiederholung bei „wird noch geholt“), `null` ohne Cover. */
    suspend fun cover(media: LibraryMedia): android.graphics.Bitmap? = covers.image(media)

    /** `search_id` zum Titel, falls die Suche einen geliefert hat. */
    fun searchId(media: LibraryMedia): String? = _state.value.searchIds[media.id]

    companion object {
        /** Pause nach dem letzten Tippen, bevor gesucht wird. */
        val DEBOUNCE = 700.milliseconds

        /** Nach so langer Wartezeit erscheint der Hinweis auf den Gesamtkatalog. */
        val CATALOG_HINT_DELAY = 900.milliseconds

        /** Sammelzeit für Status-Abfragen sichtbarer Karten. */
        val AVAILABILITY_WINDOW = 80.milliseconds
    }
}
