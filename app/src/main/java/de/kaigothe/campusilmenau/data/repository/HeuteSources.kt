package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.events.CampusEvent
import de.kaigothe.campusilmenau.domain.model.events.EventFilter
import de.kaigothe.campusilmenau.domain.model.heute.HeuteEvent
import de.kaigothe.campusilmenau.domain.model.heute.Hinweis
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Woher die Heute-Seite ihre Veranstaltungen holt: die von heute Abend und die von morgen, ohne abgesagte.
 * Die Quelle der App ist [RepositoryHeuteEventsSource].
 */
interface HeuteEventsSource {
    suspend fun evening(now: Instant): List<HeuteEvent>
    suspend fun tomorrow(now: Instant): List<HeuteEvent>
}

/** Die Events der Startseite aus dem Events-Repository: dieselben Abfragen wie „Heute Abend“ und „Morgen“ in der Liste. */
@Singleton
class RepositoryHeuteEventsSource @Inject constructor(
    private val events: EventsRepository,
) : HeuteEventsSource {
    override suspend fun evening(now: Instant): List<HeuteEvent> =
        events.list(EventFilter(eveningOnly = true, hideCancelled = true)).events.visible()

    override suspend fun tomorrow(now: Instant): List<HeuteEvent> =
        events.list(EventFilter(day = BerlinTime.date(now).plusDays(1), hideCancelled = true)).events.visible()

    private fun List<CampusEvent>.visible() = filterNot { it.isCancelled }.map { it.toHeuteEvent() }
}

/** Woher die Hinweise kommen. Einen Endpunkt gibt es noch nicht, siehe [EmptyNoticesSource]. */
interface NoticesSource {
    suspend fun notices(now: Instant): List<Hinweis>
}

/** Platzhalter, bis der Server Hinweise liefert: keine Hinweise, der Abschnitt entfällt. */
@Singleton
class EmptyNoticesSource @Inject constructor() : NoticesSource {
    override suspend fun notices(now: Instant): List<Hinweis> = emptyList()
}
