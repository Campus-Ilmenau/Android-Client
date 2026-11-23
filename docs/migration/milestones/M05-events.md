# M5 – Events

**Status:** offen · **Abhängig von:** M2 · **Gate:** `compileDebugKotlin`

## Ziel

Veranstaltungsliste mit Filter, Detail, Merkliste und Kalender-Export.

## Aufgaben

### Daten und Domäne
- [ ] Modelle (`CampusEvent`, Veranstalter) und Service (`GET /v1/events`, `/v1/events/{id}`, `/v1/events/organizers`).
- [ ] Filter (`EventFilter`, `EventsAuswahl`), Zeitformatierung (`EventZeitFormatter`).
- [ ] Merkliste (`EventsMerkStore`) in Room oder DataStore, Abgleich mit Server-Daten (`MerklisteAbgleichTests`).
- [ ] Kalender-Logik (`EventKalender`): Event → Kalender-Eintrag.

### UI
- [ ] `EventsScreen` mit Liste, Leisten (`EventsLeisten`), Event-Karte (`EventKarte`).
- [ ] Filter-Sheet (`EventsFilterSheet`).
- [ ] „Vergangene“ (`EventsVergangeneView`).
- [ ] Detailansicht (`EventDetailView`, `EventDetailModel`) mit `DiagonalBands`-Kopf.
- [ ] Kalender-Export über `CalendarContract`-Insert-Intent.
- [ ] Event-Links und Deeplinks (`EventsLinkTests`).
- [ ] Fehler-/Leerzustände, `GoatLoader`, Previews.

### Tests (aus iOS portieren)
- [ ] `EventsTests`, `EventsAnsichtTests`, `EventDetailTests`, `EventsLinkTests`, `MerklisteAbgleichTests`

## Abnahme

- Liste, Filter, Detail, Merkliste und Kalender-Export funktionieren mit echten Daten.
- Entfernen eines Sterns im Filter „Gemerkt“ stürzt nicht ab (bekannter iOS-Fehler).
- Event-Deeplinks öffnen das richtige Event.

## Quellen (iOS)

`Campus Ilmenau/Events/*`, `Shared/CampusEvent.swift`, `EventsService.swift`, `EventsQuelle.swift`, `EventFilter.swift`, `EventKalender.swift`
