# M8 – Mehr, Teilen, Einstellungen

**Status:** offen · **Abhängig von:** M3, M5, M7 (für die Teilen-Inhalte) · **Gate:** `compileDebugKotlin`

## Ziel

Bereich „Mehr“ mit Einstellungen sowie Teilen von Gerichten, Events und Medien.

## Aufgaben

### Mehr
- [ ] `MehrView`-Pendant mit Bereichen (`MehrBereich`) und Untermenü, Erreichbarkeit per Deeplink (`mehr(bereich)`).
- [ ] Bereiche laut iOS (`MehrBereich`): **Bibliothekskonto, Darstellung, Mensa, Sprache, Über die App**. Kacheln: blau (Konto, Darstellung), grün (Mensa), neutral (Sprache, Über).
- [ ] Bereich „Sprache“: App-Sprache über `LocaleManager`/`AppCompatDelegate.setApplicationLocales` und `locales_config.xml`. Verfügbare Sprachen aus `Localizable.xcstrings` ableiten.
- [ ] Bereich „Mensa“: Preisgruppe, Mensa auf Heute, Einstieg in den Mensa-Filter („Filter für Ernährung und Allergene“).
- [ ] Darstellung (`AppDarstellung`): System/Hell/Dunkel über DataStore, wirkt sofort im Theme.
- [ ] Alternative App-Icons (`AlternativeAppIcons`) über `activity-alias`; Hinweis auf kurzen Neustart. Themed Icon als „Tinted“-Ersatz.
- [ ] Links (`AppLinks`): Impressum, Datenschutz, Bibliothek usw.
- [ ] „Gespeicherte Daten“ (`GespeicherteDatenView`): Übersicht und Löschen (inkl. gespeichertes PAIA-Passwort).
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
