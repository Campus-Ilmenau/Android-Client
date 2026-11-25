# M4 – Heute

**Status:** **abgeschlossen** (2026-11-25), Events und Hinweise mit leeren Quellen · **Abhängig von:** M3 (Mensa), Events aus M5 · **Gate:** `compileDebugKotlin` – bestanden; zusätzlich Unit-Tests, Lint und Emulator

## Ziel

Die Startseite zeigt je nach Uhrzeit Mensa, Abendessen oder Veranstaltungen, dazu Hinweise. **Keine Live-Daten** wie Thoska-Guthaben oder Anzahl Ausleihen.

## Umgesetzt

- [x] `Tagesphase` und `Begruessung` (Grenzen 10:30 / 14:00 / 16:30 / 19:30 und 11 / 14 / 18 Uhr) als reine Logik, alles in Europe/Berlin über `BerlinTime`, `HeuteClock` für Uhrzeiten (immer 24 Stunden).
- [x] `HeuteContent`: die gesamte Anzeige-Logik als reine Berechnung (Jetzt-Karte je Phase, Abendessen adaptiv mit „Haus“-Regel Mensa/Cafeteria Ehrenberg, Nacht-Event-Karte mit Rückfall auf den Mittag von morgen, „Als Nächstes“ höchstens 3, „Heute Abend“ höchstens 3, Hinweise höchstens 2 nach Priorität im Zeitraum `[von, bis)`, `nextChange`, Mensa-Wahl mit Öffnungszeiten, Fehlerkarte). Mensa-Filter und Preisgruppe gelten auch hier.
- [x] `UiText` (Ressource plus Argumente) als Ergebnistyp: Die Logik bleibt ohne Android-Kontext testbar, Tests vergleichen Ressource und Zahlen statt Sätze.
- [x] `HeuteViewModel`: jeder Teil lädt parallel und für sich mit 8 s Zeitlimit (Fehler und Zeitablauf lassen nur den Abschnitt weg, die Seite hängt nie), `watch()` weckt zu jeder Minute und zu jedem früheren Wechsel, lädt neu bei Phasen- oder Tageswechsel oder nach 10 Minuten, neuer Tag setzt die Daten von gestern zurück.
- [x] Einstellungen: Mensa auf der Startseite (`heuteMensa`) und ausgeblendete Hinweise (`heuteHinweiseAusgeblendet`), Schlüssel wie iOS.
- [x] UI: Kopf mit Datum, Uhrzeit und Begrüßung, Personen-Button öffnet Mehr, Hinweise mit ✕, Jetzt-Karte (Kopf Blau mit grünem Band oder Nachtblau mit Abendsonne), „Als Nächstes“, „Heute Abend“ (ab 15:00 vor „Als Nächstes“), Mensa-Wechsel-Sheet, Pull-to-Refresh, Platzhalter beim ersten Laden, Previews Morgen / Mittag / Nachmittag / Nacht in Light und Dark.
- [x] Ziele laufen über `HeuteZiel.toDeepLink()` und damit über dieselben Links wie Widgets und Shortcuts.

### Tests

`HeuteContentTest` (Phasen- und Begrüßungsgrenzen, Karten, Abendessen, Nacht, Hinweise, Termine, nächster Wechsel, Mensa-Wahl) und `HeuteViewModelTest` (Teilfehler, Zeitlimit, Nachladen, Tageswechsel, abgebrochener Ladevorgang, gespeicherte Mensa, ausgeblendete Hinweise). `LoadingGateTests` kamen schon mit M3.

## Entscheidungen und Befunde

- **Events:** M4 lief zuerst mit einer leeren Quelle. Mit M5 (vorgezogen) liefert `RepositoryHeuteEventsSource` die echten Veranstaltungen, nur die Bindung in `RepositoryModule` wurde getauscht.
- **Hinweise:** Es gibt keinen Server-Endpunkt. Wie in iOS liefert `EmptyNoticesSource` nichts und der Abschnitt entfällt. Bei neuen Daten zuerst nachfragen (siehe `CLAUDE.md`).
- **Uhrzeitformat:** fest 24 Stunden. iOS folgt der Region (12 Stunden in manchen Regionen); das ist eine bewusste Vereinfachung.
- **Abgebrochener Ladevorgang:** Navigation 3 hält die Start-Seite hinter anderen Tabs nur im Zustand CREATED, ein laufender Ladevorgang wird dann abgebrochen. Der Stand gilt darum erst nach Abschluss als geladen (Fehler im ersten Wurf, mit Emulator gefunden und per Test abgesichert).
- Das `HeuteUhr`-Startargument für Debug-Builds gibt es noch nicht; die Previews decken die Phasen ab.

## Offen für spätere Meilensteine

- Zwei-Spalten-Layout und Popover für Tablets (M9), Widget-Anbindung (M10), Mehr-Auswahl der Mensa (M8), Event-Detail beim Tippen (M5, bis dahin öffnet es den Events-Tab).

## Quellen (iOS)

`Campus Ilmenau/Heute/*`, `Campus Ilmenau/Events/HeuteAbend*.swift`, `Shared/HeuteUhr.swift`
