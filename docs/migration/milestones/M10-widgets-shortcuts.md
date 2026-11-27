# M10 – Widgets und Shortcuts

**Status:** offen · **Abhängig von:** M3, M5 · **Gate:** `compileDebugKotlin`

## Ziel

Homescreen-Widgets und Sprach-/Shortcut-Zugang. Siri-Ersatz „wenn möglich“ (Entscheidung 2).

## Aufgaben

### Widgets (Glance)
- [x] „Mensa heute“ (10b): `MensaWidget` (Glance, `SizeMode.Responsive` klein/mittel/groß). Es zeigt die Jetzt-Karte der Startseite (`HeuteContent.currentCard`: Phase des Tages, Mensa, Filter und Preisgruppe aus `AppSettings`, dem Pendant zur App Group), klein ein Gericht, sonst bis zu drei. `WidgetDataSource` lädt Speiseplan und Veranstaltungen mit je 6 s Zeitlimit (frisch, bei Fehler Zwischenspeicher, sonst die Fehlerkarte der Startseite). Auf dem Emulator platziert: zeigt „Heute · ab 11:00, Mensa Ehrenberg, Gemüsepfanne €3.70“.
- [x] „Heute Abend“ (10b): `AbendWidget` mit bis zu drei Veranstaltungen (Uhrzeit, Titel, Ort), gleiche Quelle wie die Karte auf der Startseite. Noch nicht auf dem Gerät platziert (gleicher Code-Weg wie das Mensa-Widget, getestet in `WidgetModelsTest`).
- [x] Aktualisierung per WorkManager: `WidgetRefreshWorker` alle 30 Minuten, nur mit Netz, angelegt in `CampusApp` (`enqueueUniquePeriodicWork`, bestehende bleibt). `updatePeriodMillis` ist 0. Ein persistenter Daten-Cache wie der `WidgetStore` auf iOS fehlt: Nach einem Neustart ohne Netz zeigt das Widget die Fehlerkarte.
- [x] Bilder verkleinern: entfällt, die Widgets zeigen keine Bilder (Text und Farbflächen), damit gibt es keine RemoteViews-Bitmap-Grenze zu beachten.
- [x] Deeplinks aus Widgets: Kopf öffnet den Speiseplan der Mensa bzw. die Liste von heute Abend, eine Zeile das Gericht bzw. die Veranstaltung (`campusilmenau://…`). Auf dem Emulator geprüft: Tipp auf das Gericht öffnet seine Seite.
- [ ] Optional: Quick-Settings-Tile als Pendant zu den Steuerelementen (nicht umgesetzt).
- [x] Tests: `WidgetModelsTest` (Auswahl der Gerichte, Filter, Fehlerkarte, Links, Abend-Liste). Die iOS-Tests `WidgetSpeicherTests` entfallen, weil es keinen Speicher gibt. Offen: Widget-Wahl der Mensa (iOS: `MensaWidgetKonfiguration`, hier gilt immer die Mensa der Startseite) und Sperrbildschirm-Größen (siehe 04-nicht-portierbar.md).

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
