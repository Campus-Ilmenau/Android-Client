package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.core.network.ApiError
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.data.preferences.AppSettings
import de.kaigothe.campusilmenau.domain.model.heute.HeuteContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Die Daten der Widgets: dieselben wie auf der Startseite, ohne Anmeldung. Mensa und Preisgruppe kommen aus den
 * Einstellungen der App (Pendant zur App Group der iOS-Widgets), Speiseplan und Veranstaltungen aus den Repositories.
 *
 * Jeder Teil lädt für sich mit kurzem Zeitlimit (ein Widget bekommt vom System wenig Zeit): erst frisch, bei Fehler der
 * Zwischenspeicher. Fehlt auch der, bleibt der Teil leer und die Anzeige zeigt ihre Fehlerkarte.
 */
@Singleton
class WidgetDataSource @Inject constructor(
    private val mensa: MensaRepository,
    private val events: HeuteEventsSource,
    private val settings: AppSettings,
    private val clock: Clock,
) {
    companion object {
        /** So lange wartet jeder Teil auf das Netz. */
        val TimeLimit = 6.seconds
    }

    suspend fun content(): HeuteContent = coroutineScope {
        val now = clock.instant()
        val today = BerlinTime.date(now)
        val canteens = async { fresh({ mensa.canteens(forceRefresh = true) }, { mensa.canteens() }) }
        val mealsToday = async { fresh({ mensa.meals(today, forceRefresh = true) }, { mensa.meals(today) }) }
        val mealsTomorrow = async { fresh({ mensa.meals(today.plusDays(1), forceRefresh = true) }, { mensa.meals(today.plusDays(1)) }) }
        val evening = async { part { events.evening(now) } }
        HeuteContent(
            now = now,
            venue = settings.heuteMensa.first(),
            filter = settings.mensaFilter.first(),
            priceGroup = settings.priceGroup.first(),
            canteens = canteens.await(),
            mealsToday = mealsToday.await(),
            mealsTomorrow = mealsTomorrow.await(),
            eventsEvening = evening.await(),
        )
    }

    private suspend fun <T> fresh(load: suspend () -> T, cached: suspend () -> T): T? = part(load) ?: part(cached)

    /** Ein Teil mit Zeitlimit. Fehler und Zeitablauf geben `null`, nur ein Abbruch wird weitergereicht. */
    private suspend fun <T> part(block: suspend () -> T): T? = try {
        withTimeoutOrNull(TimeLimit) { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: ApiError) {
        null
    }
}
