# M3 – Mensa

**Status:** **abgeschlossen** (2026-11-24) · **Abhängig von:** M2 · **Gate:** 2 (Unit-Tests Mensa-Logik grün, Screens im Emulator geprüft) – bestanden, 187 Unit-Tests, Lint ohne Fehler

## Ziel

Der Mensa-Tab ist vollständig: Speiseplan für 4 Mensen bis 3 Wochen im Voraus, Filter, Preisgruppen, Detail, Öffnungszeiten. Erste vollständige vertikale Scheibe durch alle Schichten.

## Umgesetzt

### Daten und Domäne (`domain/model/mensa`, `data/`)
- [x] Modelle: `MensaMeal`, `MensaVenue`, `MensaPriceGroup`, `MensaDiet`, `Canteen`, `OpeningHour`.
- [x] Repository `MensaRepository` mit Live-Daten der Mensa-API und 5-Minuten-Zwischenspeicher; Gerichte lassen sich nicht einzeln abfragen, `meal(id, day)` sucht im Speiseplan des Tages.
- [x] Filter-Logik (`MensaFilter`) und Speicherung in DataStore (Format und Schlüssel wie iOS).
- [x] Preisgruppen mit Speicherung. Die Auswahl in „Mehr“ kommt mit M8, bis dahin gilt „Studierende“.
- [x] Öffnungszeiten-Logik (`MensaOpeningHours`, `MensaStatus`) mit injizierter Uhr (Europe/Berlin).
- [x] Präsentationslogik (`MensaPresentation`, `MensaFormatting`): „Vegetarisch“ zeigt auch vegane Gerichte.
- [x] Favoriten (`MensaFavorites`): lokal, über den normalisierten Namen (getrimmt, klein geschrieben), Schlüssel `mensaFavoriteMeals`.
- [x] Echte Beispielantworten der Dev-API gesichert (`docs/migration/api/samples`, `app/src/test/resources/mensa`).
- [x] Deeplinks `mensa?mensa=<id>`, `mensa/gericht/<id>?tag=`, `mensa/zeiten` öffnen die Ziele. Die ID wählt die Mensa (`ShellViewModel.prepare`), ein Gericht oder die Zeiten liegen auf der Wurzel des Mensa-Tabs.

### ViewModels (`ui/mensa`)
- [x] `MensaViewModel`: 15 Werktage, laufende Ladevorgänge werden beim Tagwechsel abgebrochen, „Erneut versuchen“ umgeht den Zwischenspeicher, Öffnungszeiten sind Beiwerk (Fehler lässt nur den Status-Chip weg).
- [x] `MensaMealDetailViewModel` mit Hilt-Assisted-Injection (Route liefert ID und Tag), Zustände geladen / nicht gefunden / Fehler.
- [x] `MensaOpeningHoursViewModel`.
- [x] `WriteThroughState`: gewählte Mensa und Filter reagieren sofort und werden im Hintergrund gespeichert; Meldungen der Einstellung überschreiben den Zustand nicht, solange noch gespeichert wird (kein Zurückspringen bei schnellem Umschalten), Änderungen von außen (Deeplink) kommen an.
- [x] `LoadingGate` (Ziege erst nach 0,4 s, mindestens 0,6 s, Ausblenden 0,2 s, Hinweis ab 8 s) mit Tests in virtueller Zeit.

### UI
- [x] `MensaScreen`: Kopf mit Uhr-Taste, Namensleiste mit wanderndem Unterstrich (folgt dem Finger) und Zähler „n passend“, Tagesleiste, Filterleiste, `HorizontalPager` je Mensa, Mittag/Abendessen für die Cafeteria Ehrenberg.
- [x] `MensaMealCard`, `MensaFilterSheet` (`CampusSheet` mit eigenen Aktionen „Zurücksetzen“ / „Fertig“), `MensaMealDetailScreen` (`DiagonalBands` in zwei Ebenen, Preise, Allergene, Zusatzstoffe, Bestandteile, CO₂, Favorit), `MensaOpeningHoursScreen`.
- [x] Ladebereich `GoatLoadingBlock` und `MessageCard` (Fehler, leerer Filter) als wiederverwendbare Bausteine.
- [x] Texte in `values/` und `values-en/`, Previews Light/Dark.

### Tests
- [x] Filter, Präsentation, Öffnungszeiten, Formatierung, Repository (MockWebServer mit echten Antworten), Einstellungen, `LoadingGate`, `MensaViewModel`, `MensaMealDetailViewModel`, Navigator (Deeplinks auf Gericht und Zeiten).
- Die ViewModel-Tests nutzen einen `InMemoryPreferences`-DataStore. Der echte DataStore liest auf `Dispatchers.IO`, das der Test-Scheduler nicht kennt; die Datei-Variante prüft `AppSettingsTest`.

## Auf dem Emulator geprüft (Dev-API)

Speiseplan mit echten Daten, Gericht-Detail, Filter-Sheet, Öffnungszeiten, Deeplink `campusilmenau://mensa?mensa=cafeteriaEhrenberg` (wählt die Mensa und das Pager-Ziel), Light und Dark.

## Bewusst verschoben

| Was | Wohin | Grund |
|---|---|---|
| Teilen von Speiseplan und Gericht (Bild, Text) | M8 | gehört zum gemeinsamen Teilen-Baustein |
| Preisgruppe wählen | M8 | Einstellung unter „Mehr“ |
| Siri-Tipp im Speiseplan | M10 | Assistent |
| Widget-Anbindung (Filter, Preisgruppe an App Group) | M10 | Widgets |
| iPad-Variante (`MensaPadScreen`, Popover-Filter) | M9 | adaptive Layouts |

## Befunde

- Die Chips „Bio“ und „Regional“ kommen in den aktuellen API-Daten nicht vor (die Logik ist umgesetzt und getestet, die Daten liefern sie derzeit nicht).
- Die Schlüssel der API-Daten sind `camelCase`, nur die Hülle und die Fehlerantwort sind `snake_case`. Ein `CamelCaseKeysInterceptor` normalisiert alle Antworten (siehe [api/README.md](../api/README.md)).

## Quellen (iOS)

`Campus Ilmenau/Mensa/*`, `ViewsMensa*.swift`, `Shared/Mensa*.swift`, `Shared/ModelsMensaMeal.swift`
