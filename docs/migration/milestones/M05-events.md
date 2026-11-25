# M5 – Events

**Status:** **abgeschlossen** (2026-11-25), vorgezogen vor M6 · **Abhängig von:** M2 · **Gate:** `compileDebugKotlin` – bestanden; zusätzlich Unit-Tests, Lint und Emulator mit der echten Dev-API

## Ziel

Veranstaltungsliste mit Filter, Detail, Merkliste und Kalender-Export.

## Umgesetzt

### Daten und Domäne
- [x] `CampusEvent` mit den Berliner Tagesregeln (`firstDay`, `lastDay`, `runsOn`, Nachtveranstaltung bis 06:00, Ende genau um Mitternacht), `EventStatus`, `EventOrganizer`, `EventPage`.
- [x] `EventFilter.query(now)`: Zeitfilter (Heute Abend, Tag, Zeitraum), verbreitertes UTC-Fenster mit Grenzen zum Zuschneiden, Veranstalter-ID, Kategorie, `include_cancelled` immer gesendet.
- [x] `EventsApi` (Retrofit), tolerante DTOs, Mapper (Texte getrimmt, Links nur `http`/`https` mit Host, Kategorie „Frist“, Status), `EventsRepository`: Zuschneiden auf Berliner Tage, laufende mehrtägige Veranstaltungen auf Seite 1 dazu (ohne Doppelte, Fehler der Zusatzabfrage ignoriert), Detail (404 = nicht gefunden), Veranstalter (nur mit kommenden, deutsch sortiert).
- [x] `EventsSelection` (Tag, Heute Abend, Gemerkt, Veranstalter, Art, Abgesagte), `EventGrouping`, `EventTimes` (Zeitzeilen und Abschnittstitel als `UiText`), `EventDetailDisplay`, `CalendarEntry`.
- [x] Merkliste in DataStore (`eventsMerkliste`, nur IDs, nur auf dem Gerät); der Filter „Gemerkt“ arbeitet auf der geladenen Liste und lädt dafür alle Seiten.
- [x] Die Startseite „Heute“ liest ihre Events jetzt aus dem Repository (`RepositoryHeuteEventsSource`), der Platzhalter aus M4 ist weg.

### UI
- [x] `EventsScreen`: Kopf mit Filter-Taste und Zahl, Tagesleiste (14 Tage, Punkte für Tage mit Terminen), Chipleiste (Heute Abend, Gemerkt, Veranstalter mit Zahl), nach Tagen gruppierte Liste mit Nachladen beim Scrollen, Pull-to-Refresh, Platzhalter, Fehler- und Leer-Zustände (gemerkt, Abend heute/anderer Tag, allgemein).
- [x] `EventCard` (Stern merkt, durchgestrichen bei Absage, Chips „Fällt aus“, „Noch nicht bestätigt“, „Frist“, TalkBack-Aktion „Merken“), `EventsFilterSheet` (Art, Abgesagte ausblenden).
- [x] „Vergangene“ (`EventsPastScreen`): letzter Monat bis gestern, neueste zuerst, nur Art und Abgesagte aus dem Sheet.
- [x] `EventDetailScreen`: Kopf mit Plakat (Coil) oder `DiagonalBands`, Hinweis bei Absage und unbestätigtem Termin, Infokarte (Datum und Zeit, Ort mit „In Karten öffnen“, Veranstalter mit „Alle n kommenden von …“), ausklappbare Beschreibung, Tickets, Kalender, Infoseite.
- [x] Kalender-Export über den Insert-Intent des Kalenders (`CalendarContract.Events`), ganztägig in UTC.
- [x] Deeplinks: `events` und `events?filter=heute-abend` setzen die Auswahl zurück bzw. auf Heute Abend (über `EventsRequests`, auch wenn der Tab noch nie offen war), `events/<id>` öffnet das Detail über der Liste.

### Tests
`CampusEventTest` (Status, letzter Tag, Veranstalter, Zeitzeilen, Abschnittstitel, Detail-Anzeige, Kalendereintrag, Filter), `EventsRepositoryTest` (MockWebServer mit snake_case-Antworten über die Schlüssel-Umwandlung), `EventsViewModelTest` (Laden, Fehler, Auswahl, Gemerkt, Nachladen, Vergangene, Wünsche von außen), Navigator-Test für die Event-Links.

## Entscheidungen und Befunde

- **Neue Abhängigkeit:** Coil 3 (`coil-compose`, `coil-network-okhttp`) für die Plakate. Sie nutzt einen eigenen HTTP-Client, die Schlüssel-Umwandlung der API gilt dort nicht.
- **Detail ohne Vorabdaten:** iOS zeigt zuerst die Daten aus der Liste. Android lädt über die Route nur mit der ID und zeigt kurz die Ziege. Ein gemeinsamer Zwischenspeicher wäre eine spätere Verbesserung.
- **Bekannter iOS-Fehler:** Der Stern im Filter „Gemerkt“ ist reiner Zustand ohne Haptik-Modifier an der entfernten View, es gibt keinen Absturz.
- **Eintrag in den Kalender:** Der Insert-Intent liefert kein Ergebnis zurück, darum gibt es keine Bestätigung „Im Kalender eingetragen“ wie in iOS.
- Uhrzeiten fest 24 Stunden (wie M4).

## Bewusst verschoben

| Was | Wohin |
|---|---|
| Teilen einer Veranstaltung (Bild, Text) | M8 |
| Siri-Tipp bei „Heute Abend“, Event-Widgets | M10 |
| iPad-Anordnung mit Liste und Detail nebeneinander | M9 |

## Quellen (iOS)

`Campus Ilmenau/Events/*`, `Shared/CampusEvent.swift`, `EventsService.swift`, `EventFilter.swift`, `EventKalender.swift`
