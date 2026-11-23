# M10 – Widgets und Shortcuts

**Status:** offen · **Abhängig von:** M3, M5 · **Gate:** `compileDebugKotlin`

## Ziel

Homescreen-Widgets und Sprach-/Shortcut-Zugang. Siri-Ersatz „wenn möglich“ (Entscheidung 2).

## Aufgaben

### Widgets (Glance)
- [ ] „Mensa heute“: gemeinsamer Mensa-Filter und Preisgruppe über DataStore (Pendant App Group), Größen/Responsive (`MensaWidgetViews`, `MensaWidgetAnzeige`).
- [ ] „Heute Abend“ (`AbendWidget*`).
- [ ] Aktualisierung per WorkManager, Daten-Cache, Zustandsprüfung nach Update.
- [ ] Bilder verkleinern (Speichergrenze von RemoteViews beachten).
- [ ] Deeplinks aus Widgets in die App.
- [ ] Optional: Quick-Settings-Tile als Pendant zu den Steuerelementen.
- [ ] Tests: `MensaWidgetTests`, `AbendWidgetTests`, `WidgetSpeicherTests`.

### Shortcuts / Sprachzugang
- [ ] Statische und dynamische App Shortcuts: Mensa heute, Öffnungszeiten, Abendessen, Heute Abend, Katalogsuche, Merkliste.
- [ ] AppFunctions prüfen: welche Funktionen sich für Assistenten freigeben lassen (`SiriMensa`, `SiriAbend`, `SiriKatalog`, `SiriMerkliste`, `SiriOffen`).
- [ ] Antwortkarten/-texte (`SiriKarten`, `SiriMeldung`) als Antworttexte portieren, soweit die Plattform das erlaubt.
- [ ] Ergebnis und Grenzen in [04-nicht-portierbar.md](../04-nicht-portierbar.md) nachtragen.
- [ ] Tests (Logik): `SiriMensaTests`, `SiriAbendTests`, `SiriKatalogTests`, `SiriKurzbefehleTests`, `SiriZielTests`.

## Abnahme

- Widgets lassen sich platzieren, zeigen aktuelle Daten und öffnen die richtige Stelle in der App.
- Shortcuts erscheinen beim langen Drücken des App-Icons und führen zum Ziel.

## Quellen (iOS)

`CampusWidgets/*`, `Shared/*Widget*.swift`, `Shared/Siri*.swift`, `SiriShared/*`, `Campus Ilmenau/Siri/*`, `CampusShortcuts.swift`
