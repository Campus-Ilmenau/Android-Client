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
- [x] App Shortcuts (10a): fünf statische in `res/xml/shortcuts.xml` (Mensa heute, Heute Abend, Katalog, Merkliste, Öffnungszeiten) an beiden Launcher-Aliasen, dazu ein dynamischer für „Thoska auslesen“ nur mit NFC (`CampusShortcuts.publish`, beim Start der Activity). Texte de/en, Symbole weiß auf Nachtblau wie die Kachel der iOS-Kurzbefehle. „Abendessen“ (Cafeteria) ist keiner: Der Link kennt die Tageszeit nicht. `ShortcutsXmlTest` prüft, dass jede Adresse ein bekanntes Ziel öffnet und alle Texte in beiden Sprachen stehen. Auf dem Emulator beim System angemeldet (`dumpsys shortcut`).
- [ ] AppFunctions prüfen: welche Funktionen sich für Assistenten freigeben lassen (`SiriMensa`, `SiriAbend`, `SiriKatalog`, `SiriMerkliste`, `SiriOffen`).
- [ ] Antwortkarten/-texte (`SiriKarten`, `SiriMeldung`) als Antworttexte portieren, soweit die Plattform das erlaubt.
- [ ] Ergebnis und Grenzen in [04-nicht-portierbar.md](../04-nicht-portierbar.md) nachtragen.
- [ ] Tests (Logik): `SiriMensaTests`, `SiriAbendTests`, `SiriKatalogTests`, `SiriKurzbefehleTests`, `SiriZielTests`.

## Abnahme

- Widgets lassen sich platzieren, zeigen aktuelle Daten und öffnen die richtige Stelle in der App.
- Shortcuts erscheinen beim langen Drücken des App-Icons und führen zum Ziel.

## Quellen (iOS)

`CampusWidgets/*`, `Shared/*Widget*.swift`, `Shared/Siri*.swift`, `SiriShared/*`, `Campus Ilmenau/Siri/*`, `CampusShortcuts.swift`
