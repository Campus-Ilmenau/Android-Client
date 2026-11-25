package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.core.common.IoDispatcher
import de.kaigothe.campusilmenau.core.network.ApiCaller
import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.data.remote.events.EventsApi
import de.kaigothe.campusilmenau.data.remote.events.toDomain
import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventFilter
import de.kaigothe.campusilmenau.domain.model.events.EventOrganizer
import de.kaigothe.campusilmenau.domain.model.events.EventPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Veranstaltungen. Fehler kommen als [ApiError] (ein fehlendes Event als `Http` mit `isNotFound`). */
interface EventsRepository {

    /**
     * Eine Seite der Liste, nach Beginn aufsteigend, bis zu 100 Veranstaltungen. Bei „ab Tag X“ holt die erste Seite
     * zusätzlich mehrtägige Veranstaltungen, die vor diesem Tag begonnen haben und noch laufen (die API findet sie über
     * `from` nicht).
     */
    suspend fun list(filter: EventFilter, page: Int = 1): EventPage

    /** Eine einzelne Veranstaltung. Gibt es sie nicht (mehr), ist es ein 404. */
    suspend fun detail(id: String): CampusEvent

    /** Veranstalter mit kommenden Veranstaltungen, alphabetisch. */
    suspend fun organizers(): List<EventOrganizer>
}

@Singleton
class NetworkEventsRepository @Inject constructor(
    private val api: EventsApi,
    private val caller: ApiCaller,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : EventsRepository {

    override suspend fun list(filter: EventFilter, page: Int): EventPage = withContext(io) {
        val query = filter.query(clock.instant())
        val params = LinkedHashMap<String, String>().apply {
            query.params.forEach { (name, value) -> put(name, value) }
            put("page", maxOf(1, page).toString())
            put("per_page", EventsApi.PAGE_SIZE.toString())
        }
        val response = caller.call { api.list(params) }
        val events = caller.call { response.data.map { it.toDomain() } }

        // Das Fenster der API ist in UTC-Tagen verbreitert, hier auf Berliner Tage zuschneiden.
        var result = events.filter { event ->
            (query.fromBoundary == null || event.start >= query.fromBoundary) &&
                (query.toBoundary == null || event.start < query.toBoundary)
        }

        if (page == 1 && query.runningOn != null) {
            val running = runningEvents(query.runningOn, query.common)
            val known = result.map { it.id }.toSet()
            result = (result + running.filter { it.id !in known }).sortedBy { it.start }
        }
        EventPage(
            events = result,
            page = response.page,
            hasMore = response.hasMore,
            total = response.totalCount ?: response.data.size,
        )
    }

    override suspend fun detail(id: String): CampusEvent = withContext(io) {
        caller.call { api.detail(id).toDomain() }
    }

    override suspend fun organizers(): List<EventOrganizer> = withContext(io) {
        val collator = Collator.getInstance(Locale.GERMAN)
        caller.call { api.organizers().data.map { it.toDomain() } }
            .filter { it.upcomingCount > 0 }
            .sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    /**
     * Mehrtägige Veranstaltungen, die vor [day] begonnen haben und an [day] noch laufen. Schlägt die Abfrage fehl,
     * bleibt die Liste ohne sie, statt ganz zu scheitern.
     */
    private suspend fun runningEvents(day: LocalDate, common: List<Pair<String, String>>): List<CampusEvent> {
        val params = LinkedHashMap<String, String>().apply {
            put("date", day.toString())
            common.forEach { (name, value) -> put(name, value) }
            put("per_page", EventsApi.PAGE_SIZE.toString())
        }
        return try {
            caller.call { api.list(params).data.map { it.toDomain() } }
                .filter { it.firstDay < day && it.runsOn(day) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: ApiError) {
            emptyList()
        }
    }
}
