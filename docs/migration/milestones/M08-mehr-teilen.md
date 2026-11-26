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
- [x] Alternative App-Icons (`AppIcon`, `AppIconWahl`) über zwei `activity-alias` (Standard, Dunkel); Themed Icon als „Tinted“-Ersatz im Standard-Icon. Android hat kein „Automatisch“ nach Hell/Dunkel, daher nur Standard und Dunkel. Der Wechsel nutzt `DONT_KILL_APP`, der Launcher zieht das Icon nach (kann einen Moment dauern). Auf Gerät noch zu prüfen: Launcher-Eintrag bleibt einzeln, Wechsel Standard ↔ Dunkel.
- [x] Links (`AppLinks`): Datenschutz (`https://campus-ilmenau.de/datenschutz/`) und Impressum (`https://campus-ilmenau.de/impressum/`) in „Allgemein“, öffnen im Browser. In der iOS-App sind die Adressen noch leer (`AppLinks.swift`).
- [x] „Gespeicherte Daten“ (`GespeicherteDatenView`): Übersicht (Thoska, Merkliste, Veranstaltungen, Einstellungen, was nicht gespeichert wird) und **Alle gespeicherten Daten löschen** (Verlauf, Merkliste, Einstellungen, Sitzung und gespeichertes PAIA-Passwort).
- [ ] Shortcut-Tipps (`SiriFragenView`, `SiriTipps`) als Android-Variante (siehe M10).

### Teilen
- [x] Inhalte als Text (8b-1): Gericht (`MensaMealShare`), Speiseplan einer Mensa (`MensaPlanShare`), Event (`EventShare`), Medium (`LibraryMediaShare`). Reines Kotlin in `domain/model/share/`, Texte über `ShareStrings` (App: `ResourceShareStrings`, `share_*` de/en). Teilen-Blatt per `ACTION_SEND` (`Context.shareText`).
- [x] Bildkarten (8b-2): `SharePlanCard`, `ShareMealCard`, `ShareEventTicket` in `ui/share/card/` (feste Dichte 3, fester Modus: hell bzw. nachtblau, 1080 px breit, Höhe nach Inhalt, mindestens 450 dp). Gerendert wird erst bei der Wahl „Als Bild teilen“: `ShareImageRenderer` setzt eine `ComposeView` außerhalb des Fensters ans Fenster und zeichnet sie in ein `Bitmap` (statt `GraphicsLayer`, weil die Karte ein eigenes Fenster-Layout mit fester Dichte braucht). Vorschauen in `ShareCardPreviews.kt`.
- [x] Share-Intent mit `FileProvider` (`<cache>/share/`, nicht exportiert), Text und Bild getrennt: Das Bild geht ohne Text und ohne Betreff raus, die Marke steht im Bild.
- [x] Teilen-Taste mit Menü „Als Bild / Als Text teilen“ (`ShareMenu`) im Speiseplan, im Gericht und in der Veranstaltung; die Titelansicht der Bibliothek teilt nur Text (wie iOS).
- [x] Bild sichern: über das Teilen-Blatt („In Dateien/Fotos sichern“ der Apps), keine Berechtigung nötig; ein eigenes `MediaStore`-Speichern entfällt.
- [x] Tests: `ShareTextTest` (29 Fälle: Bausteine, Texte, Dateinamen, Zeiten). Die Bildkarten prüfen die Vorschauen und der Lauf auf dem Emulator (Speiseplan und Veranstaltung als PNG erzeugt, Teilen-Blatt zeigt das Bild). Offen: Gericht-Karte und Abgesagt/Unbestätigt auf dem Gerät sowie Telegram/Messages-Verhalten.

## Abnahme

- Darstellung wechselt ohne Neustart. Icon-Wechsel funktioniert.
- Geteilte Karten sehen aus wie in den Mockups (Light/Dark).
- „Gespeicherte Daten löschen“ entfernt alle lokalen Daten.

## Quellen (iOS)

`Campus Ilmenau/Mehr/*`, `Campus Ilmenau/Teilen/*`, `Campus Ilmenau/Siri/SiriTipps.swift`
