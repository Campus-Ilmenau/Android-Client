# M8 – Mehr, Teilen, Einstellungen

**Status:** offen · **Abhängig von:** M3, M5, M7 (für die Teilen-Inhalte) · **Gate:** `compileDebugKotlin`

## Ziel

Bereich „Mehr“ mit Einstellungen sowie Teilen von Gerichten, Events und Medien.

## Aufgaben

### Mehr
- [x] `MehrView`-Pendant (Handy-Liste: Darstellung, Mensa, Bibliothekskonto, Allgemein), Erreichbarkeit per Deeplink `mehr?bereich=` (scrollt zum Bereich). Die zweispaltige Tablet-Anordnung (`MehrPad`) folgt in M9.
- [x] Bereiche laut iOS (`MehrBereich`): **Bibliothekskonto, Darstellung, Mensa, Sprache, Über die App**. Die Kacheln der Tablet-Ansicht kommen mit M9.
- [x] Bereich „Sprache“: Automatisch/Deutsch/English. Ab Android 13 über `LocaleManager` (`locales_config.xml`), davor eigene Speicherung und `attachBaseContext` (die App nutzt kein AppCompat-Theme).
- [x] Bereich „Mensa“: Preisgruppe, Mensa auf Heute, Öffnungszeiten. (Der Einstieg in den Mensa-Filter bleibt im Mensa-Tab.)
- [x] Darstellung (`AppDarstellung`): System/Hell/Dunkel über DataStore, wirkt sofort im Theme.
- [ ] Alternative App-Icons (`AlternativeAppIcons`) über `activity-alias`; Hinweis auf kurzen Neustart. Themed Icon als „Tinted“-Ersatz.
- [ ] Links (`AppLinks`): Impressum, Datenschutz. In iOS sind die Adressen noch leer, die Zeilen entfallen dann; erst eintragen, wenn sie feststehen.
- [x] „Gespeicherte Daten“ (`GespeicherteDatenView`): Übersicht (Thoska, Merkliste, Veranstaltungen, Einstellungen, was nicht gespeichert wird) und **Alle gespeicherten Daten löschen** (Verlauf, Merkliste, Einstellungen, Sitzung und gespeichertes PAIA-Passwort).
- [ ] Shortcut-Tipps (`SiriFragenView`, `SiriTipps`) als Android-Variante (siehe M10).

### Teilen
- [ ] Teilkarten als Bitmap rendern (Compose → `GraphicsLayer`) (`ShareCardView`, `ShareEventCard`, `ShareImage`, `ShareBranding`).
- [ ] Share-Intent mit `FileProvider`, Text und Bild getrennt (kein zusätzlicher Text beim Teilen als Bild).
- [ ] Inhalte: Gericht (`MensaShare`), Event (`EventShare`), Medium (`LibraryMediaShare`).
- [ ] Bild sichern: über Share-Intent bzw. `MediaStore`, keine Berechtigung nötig.
- [ ] Tests: `ShareTextTests`, `ShareCardTests`.

## Abnahme

- Darstellung wechselt ohne Neustart. Icon-Wechsel funktioniert.
- Geteilte Karten sehen aus wie in den Mockups (Light/Dark).
- „Gespeicherte Daten löschen“ entfernt alle lokalen Daten.

## Quellen (iOS)

`Campus Ilmenau/Mehr/*`, `Campus Ilmenau/Teilen/*`, `Campus Ilmenau/Siri/SiriTipps.swift`
