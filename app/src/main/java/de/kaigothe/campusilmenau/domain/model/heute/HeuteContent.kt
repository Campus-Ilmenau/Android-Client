package de.kaigothe.campusilmenau.domain.model.heute

import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.common.UiText
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.mensa.Canteen
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFilter
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFormatting
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealtime
import de.kaigothe.campusilmenau.domain.model.mensa.MensaOpeningHours
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.domain.model.mensa.listChips
import java.time.Instant
import java.time.LocalDate

/**
 * Alles, was die Startseite „Heute“ je nach Uhrzeit zeigt, als reine Berechnung aus den geladenen Daten (Pendant zu den
 * Anzeige-Werten von `HeuteModel`). Die Regeln folgen dem Script von `Heute.dc.html`.
 *
 * - Fehlt ein Teil (`null`: Fehler, Zeitlimit), entfällt nur sein Abschnitt.
 * - [now] ist die einzige Uhr, damit sich jede Phase testen und in Vorschauen zeigen lässt.
 * - Das Abendessen ist adaptiv: Abendessen-Orte sind alle Mensen, die heute Gerichte der Gruppe „Abendessen“ liefern.
 *   Normal ist das die Cafeteria Ehrenberg, in der Prüfungszeit manchmal die Mensa Ehrenberg.
 */
class HeuteContent(
    val now: Instant,
    val venue: MensaVenue,
    val filter: MensaFilter = MensaFilter(),
    val priceGroup: MensaPriceGroup = MensaPriceGroup.Studierende,
    val canteens: List<Canteen>? = null,
    val mealsToday: List<MensaMeal>? = null,
    val mealsTomorrow: List<MensaMeal>? = null,
    val eventsEvening: List<HeuteEvent>? = null,
    val eventsTomorrow: List<HeuteEvent>? = null,
    val notices: List<Hinweis> = emptyList(),
    val dismissedNotices: Set<String> = emptySet(),
) {
    companion object {
        /** Beginn des Abendessens in Minuten seit Mitternacht (16:30). Die API nennt keinen Beginn. */
        const val DinnerStart = 16 * 60 + 30

        /** Ab dieser Uhrzeit (15:00) steht „Heute Abend“ vor „Als Nächstes“. */
        const val EventsFirstFrom = 15 * 60
        const val MaxAppointments = 3
        const val MaxNotices = 2

        /** So viele Einträge zeigt die Karte „Heute Abend“ höchstens. */
        const val MaxEveningEntries = 3
    }

    private val minutes: Int = BerlinTime.minutesOfDay(now)
    private val today: LocalDate = BerlinTime.date(now)
    private val tomorrow: LocalDate = today.plusDays(1)

    val phase: Tagesphase = Tagesphase.of(now)
    val greeting: Begruessung = Begruessung.of(now)

    /** Ab 15:00 und nicht nachts steht „Heute Abend“ vor „Als Nächstes“. */
    val eventsFirst: Boolean get() = minutes >= EventsFirstFrom && phase != Tagesphase.Nacht

    /** Höchstens 2, ohne ausgeblendete und abgelaufene, wichtigste zuerst. */
    val activeNotices: List<Hinweis>
        get() = notices
            .filter { it.appliesAt(now) && it.id !in dismissedNotices }
            .sortedByDescending { it.priority }
            .take(MaxNotices)

    /** Karte „Heute Abend“: Events von heute Abend, höchstens 3. Nachts ohne das Event, das schon in der Jetzt-Karte steht. */
    val eveningEvents: List<HeuteEvent>
        get() {
            val inCard = if (phase == Tagesphase.Nacht) nightEvent else null
            return eventsEvening.orEmpty()
                .filter { !it.allDay && (it.end?.let { end -> end > now } ?: true) && it.id != inCard?.id }
                .sortedBy { it.start }
                .take(MaxEveningEntries)
        }

    /** Die große Karte oben. `null`, wenn ihre Daten fehlen: dann entfällt sie. */
    val currentCard: HeuteJetztKarte?
        get() = when (phase) {
            Tagesphase.Morgen -> venueCard(2, UiText(R.string.heute_vorschau_mittag))
            Tagesphase.Mittag -> venueCard(3, UiText(R.string.heute_mittagsangebot))
            // Ohne Abendessen-Daten heute (zum Beispiel am Wochenende) bleibt die gewählte Mensa.
            Tagesphase.Nachmittag, Tagesphase.Abend ->
                dinnerCard() ?: venueCard(3, UiText(R.string.heute_mittagsangebot))

            Tagesphase.Nacht -> eventCard() ?: lunchTomorrowCard()
        }

    /** „Als Nächstes“, höchstens 3 Einträge. Keine Live-Daten wie Guthaben oder Ausleihen. */
    val upcoming: List<HeuteTermin>
        get() {
            val entries: List<HeuteTermin?> = when (phase) {
                Tagesphase.Morgen, Tagesphase.Mittag -> venueAppointments + listOf(dinnerAppointment, eventAppointment(onlyToday = true))
                Tagesphase.Nachmittag -> listOf(dinnerAppointment, eventAppointment(onlyToday = true))
                Tagesphase.Abend -> listOf(eventAppointment(onlyToday = true), lunchTomorrowAppointment)
                Tagesphase.Nacht -> listOf(lunchTomorrowAppointment, eventAppointment(onlyToday = false))
            }
            return entries.filterNotNull().take(MaxAppointments)
        }

    /** Nächster Zeitpunkt, an dem sich etwas an der Anzeige ändert. */
    val nextChange: Instant
        get() {
            val candidates = mutableListOf(Tagesphase.nextChange(now))
            opening(venue, today)?.let { (opens, closes) ->
                candidates += BerlinTime.at(today, opens)
                candidates += BerlinTime.at(today, closes)
            }
            (eventsEvening.orEmpty() + eventsTomorrow.orEmpty()).forEach { event ->
                candidates += event.start
                event.end?.let { candidates += it }
            }
            candidates += BerlinTime.at(today, DinnerStart)
            return candidates.filter { it > now }.minOrNull() ?: BerlinTime.startOfDay(tomorrow)
        }

    // MARK: Mensa-Wahl und Fehlerkarte

    /** Eine Mensa im Auswahl-Sheet. [info] ist `null`, solange Öffnungszeiten fehlen. */
    sealed interface MensaInfo {
        data class Hours(val opens: Int, val closes: Int, val dinner: Boolean) : MensaInfo
        data object ClosedToday : MensaInfo
    }

    data class MensaOption(val venue: MensaVenue, val info: MensaInfo?)

    /** Die vier Mensen für das Auswahl-Sheet, mit den heutigen Öffnungszeiten aus der API. */
    val venueOptions: List<MensaOption>
        get() = MensaVenue.entries.map { option ->
            val info = if (canteens == null) {
                null
            } else {
                opening(option, today)?.let { (opens, closes) ->
                    MensaInfo.Hours(opens, closes, dinner = dinnerVenues?.contains(option) == true)
                } ?: MensaInfo.ClosedToday
            }
            MensaOption(option, info)
        }

    /** Ersatz für die Jetzt-Karte, wenn nach dem Laden Daten fehlen. */
    val errorCard: HeuteJetztKarte
        get() = HeuteJetztKarte(
            heading = phase.heading,
            kicker = null,
            title = UiText.plain(venue.displayName),
            subtitle = UiText(R.string.heute_speiseplan_nicht_verfuegbar),
            rows = emptyList(),
            linkText = UiText(R.string.heute_zum_mensa_tab),
            ziel = HeuteZiel.Speiseplan(venue),
            changeVenueVisible = phase != Tagesphase.Nacht,
            style = HeuteJetztKarte.Style.Mensa,
        )

    // MARK: Mensa

    /** Öffnungszeit der Mensa am Tag in Minuten; `null` bei geschlossen oder ohne Angabe. */
    private fun opening(of: MensaVenue, day: LocalDate): Pair<Int, Int>? {
        val canteen = canteens?.firstOrNull { of.matches(it.name) } ?: return null
        val key = MensaOpeningHours.key(day.dayOfWeek)
        val hours = canteen.openingHours.firstOrNull { it.weekday.lowercase() == key } ?: return null
        val opens = HeuteClock.minutes(hours.opensAt) ?: return null
        val closes = HeuteClock.minutes(hours.closesAt) ?: return null
        return opens to closes
    }

    /** Mittagsgerichte der Mensa: alles, was nicht zur Gruppe „Abendessen“ gehört. */
    private fun lunchMeals(of: MensaVenue, plan: List<MensaMeal>?): List<MensaMeal> =
        plan.orEmpty().filter { of.matches(it.canteenName) && !MensaMealtime.isDinner(it) }

    /** Nur Gerichte, die zum Mensa-Filter der App passen (Mensa-Ansicht, Heute und Widget nutzen denselben). */
    private fun matching(meals: List<MensaMeal>): List<MensaMeal> = filter.apply(meals)

    /** [day]: Tag des Gerichts, wenn es nicht heute ist (die IDs der API gelten nur für einen Tag). */
    private fun row(meal: MensaMeal, day: LocalDate? = null) = HeuteZeile(
        id = meal.id,
        name = UiText.plain(meal.name),
        chips = meal.listChips,
        right = meal.price.formatted(priceGroup),
        ziel = HeuteZiel.Gericht(meal.id, day?.toString()),
    )

    private fun clock(minutes: Int) = UiText.plain(HeuteClock.time(minutes))

    /** Jetzt-Karte der gewählten Mensa (morgens, mittags und als Rückfall am Nachmittag und Abend). */
    private fun venueCard(count: Int, subtitle: UiText): HeuteJetztKarte? {
        // Ohne Speiseplan oder Öffnungszeiten fehlt die Karte.
        if (mealsToday == null || canteens == null) return null
        val hours = opening(venue, today)
        val closed: Boolean
        val kicker: UiText
        if (hours != null && minutes < hours.second) {
            closed = false
            kicker = if (minutes >= hours.first) {
                UiText(R.string.heute_kicker_offen_bis, clock(hours.second))
            } else {
                UiText(R.string.heute_kicker_ab, clock(hours.first))
            }
        } else {
            closed = true
            kicker = UiText(R.string.heute_geschlossen)
        }
        val meals = lunchMeals(venue, mealsToday)
        // Ohne Angebot (zum Beispiel sonntags) sagt die Kachel das ausdrücklich.
        val subtitleText = when {
            meals.isNotEmpty() -> subtitle
            closed -> UiText(R.string.heute_mensa_geschlossen)
            else -> UiText(R.string.heute_keine_angebote)
        }
        return HeuteJetztKarte(
            heading = phase.heading,
            kicker = kicker,
            title = UiText.plain(venue.displayName),
            subtitle = subtitleText,
            rows = matching(meals).take(count).map { row(it) },
            linkText = UiText(R.string.heute_link_speiseplan),
            ziel = HeuteZiel.Speiseplan(venue),
            changeVenueVisible = true,
            style = HeuteJetztKarte.Style.Mensa,
        )
    }

    // MARK: Abendessen

    /**
     * Mensen, die heute Abendessen ausgeben, nach den Daten der API. Die Cafeteria Ehrenberg steht vorn, weil sie der
     * Normalfall ist. `null`, solange der Speiseplan fehlt.
     */
    private val dinnerVenues: List<MensaVenue>?
        get() {
            val plan = mealsToday ?: return null
            val order = listOf(MensaVenue.CafeteriaEhrenberg, MensaVenue.MensaEhrenberg, MensaVenue.Nanoteria, MensaVenue.Roentgenbau)
            return order.filter { candidate -> plan.any { candidate.matches(it.canteenName) && MensaMealtime.isDinner(it) } }
        }

    /** Der Ort, den die Seite zeigt: die gewählte Mensa, wenn sie Abendessen hat, dann ein Ort im selben Haus, sonst der erste. */
    private val dinnerVenue: MensaVenue?
        get() {
            val venues = dinnerVenues?.takeIf { it.isNotEmpty() } ?: return null
            return venues.firstOrNull { it == venue } ?: venues.firstOrNull { building(it) == building(venue) } ?: venues[0]
        }

    /**
     * Die gewählte Mensa gibt Abendessen aus. Mensa und Cafeteria Ehrenberg zählen als ein Haus: Wer die Mensa Ehrenberg
     * gewählt hat, bekommt das Abendessen der Cafeteria nicht als „Tipp“.
     */
    private val ownVenueHasDinner: Boolean
        get() = dinnerVenues?.any { building(it) == building(venue) } == true

    /** „Mensa Ehrenberg“: Beide Ehrenberg-Orte heißen so, das Gebäude. */
    private fun building(of: MensaVenue): String = when (of) {
        MensaVenue.MensaEhrenberg, MensaVenue.CafeteriaEhrenberg -> "Mensa Ehrenberg"
        else -> of.displayName
    }

    /** „Cafeteria“ bzw. „Mensa“: wo im Gebäude. */
    private fun place(of: MensaVenue): UiText = when (of) {
        MensaVenue.CafeteriaEhrenberg -> UiText(R.string.heute_ort_cafeteria)
        MensaVenue.MensaEhrenberg -> UiText(R.string.heute_ort_mensa)
        else -> UiText.plain(of.shortName)
    }

    private fun dinnerCard(): HeuteJetztKarte? {
        val place = dinnerVenue ?: return null
        val plan = mealsToday ?: return null
        val meals = plan.filter { place.matches(it.canteenName) && MensaMealtime.isDinner(it) }
        val own = ownVenueHasDinner
        val kicker: UiText
        val title: UiText
        val subtitle: UiText
        val dinnerIn = UiText(R.string.heute_abend_in, building(place))
        when {
            phase == Tagesphase.Abend && own -> {
                kicker = UiText(R.string.heute_abend_jetzt_offen)
                title = UiText.plain(building(place))
                subtitle = place(place)
            }

            phase == Tagesphase.Abend -> {
                kicker = UiText(R.string.heute_hat_geschlossen, venue.displayName)
                title = dinnerIn
                subtitle = UiText(R.string.heute_abend_tipp_jetzt)
            }

            own -> {
                kicker = UiText(R.string.heute_kicker_heute_ab, clock(DinnerStart))
                title = dinnerIn
                subtitle = place(place)
            }

            else -> {
                kicker = UiText(R.string.heute_hat_geschlossen, venue.displayName)
                title = dinnerIn
                subtitle = UiText(R.string.heute_tipp_ab, clock(DinnerStart), place(place))
            }
        }
        return HeuteJetztKarte(
            heading = phase.heading,
            kicker = kicker,
            title = title,
            subtitle = subtitle,
            rows = matching(meals).take(3).map { row(it) },
            linkText = UiText(R.string.heute_link_abendangebot),
            ziel = HeuteZiel.Speiseplan(place),
            changeVenueVisible = true,
            style = HeuteJetztKarte.Style.Mensa,
        )
    }

    private val dinnerAppointment: HeuteTermin?
        get() {
            val place = dinnerVenue ?: return null
            if (minutes >= DinnerStart) return null
            return HeuteTermin(
                id = "abendessen",
                time = HeuteClock.time(DinnerStart),
                color = HeuteFarbe.BrandOrange,
                title = UiText(R.string.abendessen),
                subtitle = UiText(R.string.heute_abend_ort, building(place), place(place)),
                ziel = HeuteZiel.Speiseplan(place),
            )
        }

    /** „Öffnet“ und „Schließt“ der gewählten Mensa, nur in der Zukunft. */
    private val venueAppointments: List<HeuteTermin?>
        get() {
            val (opens, closes) = opening(venue, today) ?: return emptyList()
            val list = mutableListOf<HeuteTermin?>()
            if (opens > minutes) {
                list += HeuteTermin(
                    "mensa-oeffnet", HeuteClock.time(opens), HeuteFarbe.BrandOrange,
                    UiText(R.string.heute_oeffnet), UiText.plain(venue.displayName), HeuteZiel.Speiseplan(venue),
                )
            }
            if (closes > minutes) {
                list += HeuteTermin(
                    "mensa-schliesst", HeuteClock.time(closes), HeuteFarbe.BrandOrange,
                    UiText(R.string.heute_schliesst), UiText.plain(venue.displayName), HeuteZiel.Oeffnungszeiten(venue),
                )
            }
            return list
        }

    // MARK: Morgen

    /** Mittag morgen in der gewählten Mensa; leer ohne Speiseplan für morgen. */
    private val tomorrowMeals: List<MensaMeal> get() = lunchMeals(venue, mealsTomorrow)

    private fun lunchTomorrowCard(): HeuteJetztKarte? {
        val meals = tomorrowMeals
        if (meals.isEmpty()) return null
        val kicker = opening(venue, tomorrow)
            ?.let { UiText(R.string.heute_morgen_kicker_ab, clock(it.first)) } ?: UiText(R.string.heute_morgen_kicker)
        return HeuteJetztKarte(
            heading = phase.heading,
            kicker = kicker,
            title = UiText.plain(venue.displayName),
            subtitle = UiText(R.string.heute_mittag_morgen),
            rows = matching(meals).take(3).map { row(it, day = tomorrow) },
            linkText = UiText(R.string.heute_link_speiseplan),
            ziel = HeuteZiel.Speiseplan(venue),
            changeVenueVisible = true,
            style = HeuteJetztKarte.Style.Mensa,
        )
    }

    private val lunchTomorrowAppointment: HeuteTermin?
        get() {
            val first = tomorrowMeals.firstOrNull() ?: return null
            val from = opening(venue, tomorrow)?.let { UiText(R.string.heute_morgen_ab, clock(it.first)) }
                ?: UiText(R.string.heute_morgen)
            return HeuteTermin(
                id = "mittag-morgen",
                time = MensaFormatting.weekdayShort(tomorrow),
                color = HeuteFarbe.BrandOrange,
                title = UiText(R.string.heute_mittag_gericht, first.name),
                subtitle = UiText(R.string.heute_mensa_und, venue.displayName, from),
                ziel = HeuteZiel.Speiseplan(venue),
            )
        }

    // MARK: Events

    private fun relevant(event: HeuteEvent) = !event.allDay && !event.cancelled

    /** Das Event, das gerade läuft: Beginn ≤ jetzt < Ende, ohne Ende bis Mitternacht. */
    private val runningEvent: HeuteEvent?
        get() {
            val midnight = BerlinTime.startOfDay(tomorrow)
            return eventsEvening.orEmpty().filter(::relevant).sortedBy { it.start }
                .firstOrNull { it.start <= now && now < (it.end ?: midnight) }
        }

    /** Kommende Events von heute Abend und morgen, nach Beginn. */
    private val upcomingEvents: List<HeuteEvent>
        get() = (eventsEvening.orEmpty() + eventsTomorrow.orEmpty())
            .filter { relevant(it) && it.start > now }
            .sortedBy { it.start }

    private fun placeAndOrganizer(event: HeuteEvent): String =
        listOfNotNull(event.location, event.organizer).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ")

    /** Das Event der Jetzt-Karte in der Phase Nacht: das laufende, sonst das nächste des Abends. */
    private val nightEvent: HeuteEvent?
        get() {
            val evening = eventsEvening ?: return null
            runningEvent?.let { return it }
            return evening.filter(::relevant).sortedBy { it.start }.firstOrNull { it.start > now }
        }

    /** Nachts: das laufende Event, sonst das nächste des Abends. `null`, wenn es keins gibt. */
    private fun eventCard(): HeuteJetztKarte? {
        val main = nightEvent ?: return null
        val kicker = if (runningEvent != null) {
            UiText(R.string.heute_laeuft_seit, UiText.plain(HeuteClock.time(main.start)))
        } else {
            UiText(R.string.heute_kicker_ab, UiText.plain(HeuteClock.time(main.start)))
        }
        val after = upcomingEvents.firstOrNull { it.id != main.id && it.start > main.start }
        val rows = after?.let { event ->
            listOf(
                HeuteZeile(
                    id = event.id,
                    name = UiText(R.string.heute_danach, event.title),
                    chips = emptyList(),
                    right = if (HeuteClock.isSameDay(event.start, now)) {
                        HeuteClock.time(event.start)
                    } else {
                        "${MensaFormatting.weekdayShort(BerlinTime.date(event.start))} ${HeuteClock.time(event.start)}"
                    },
                ),
            )
        }.orEmpty()
        return HeuteJetztKarte(
            heading = phase.heading,
            kicker = kicker,
            title = UiText.plain(main.title),
            subtitle = UiText.plain(placeAndOrganizer(main)),
            rows = rows,
            linkText = UiText(R.string.heute_alle_events),
            ziel = HeuteZiel.AlleEvents,
            changeVenueVisible = false,
            style = HeuteJetztKarte.Style.Event,
        )
    }

    /** Das nächste Event als Termin. Heute: Uhrzeit; an einem anderen Tag: Wochentag, die Uhrzeit steht darunter. */
    private fun eventAppointment(onlyToday: Boolean): HeuteTermin? {
        val candidates = if (onlyToday) upcomingEvents.filter { HeuteClock.isSameDay(it.start, now) } else upcomingEvents
        val event = candidates.firstOrNull() ?: return null
        val sameDay = HeuteClock.isSameDay(event.start, now)
        val who = event.organizer ?: event.location ?: ""
        val subtitle = if (sameDay) who else listOf(who, HeuteClock.time(event.start)).filter { it.isNotEmpty() }.joinToString(" · ")
        return HeuteTermin(
            id = "event-${event.id}",
            time = if (sameDay) HeuteClock.time(event.start) else MensaFormatting.weekdayShort(BerlinTime.date(event.start)),
            color = HeuteFarbe.Blue,
            title = UiText.plain(event.title),
            subtitle = UiText.plain(subtitle),
            ziel = HeuteZiel.Event(event.id),
        )
    }
}
