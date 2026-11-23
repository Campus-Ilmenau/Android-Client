# M0 – Audit und Spezifikation

**Status:** abgeschlossen (2026-11-23), mit einem offenen Rest (echte API-Beispielantworten, siehe unten) · **Abhängig von:** – · **Gate:** –

## Ziel

Vor dem ersten Android-Code ist klar, was die iOS-App im Detail tut, welche API-Verträge gelten und welche Lücken es gibt.

## Ergebnis

| Dokument | Inhalt |
|---|---|
| [`.migration/feature_specification.md`](../../../.migration/feature_specification.md) | Alle Features, Navigation, Fachregeln, nicht offensichtliche Logik, Abweichungen Android |
| [`api/`](../api/README.md) | Verträge Mensa, Events, Library, PAIA |
| [`spec/lokale-daten.md`](../spec/lokale-daten.md) | Alle Einstellungsschlüssel, Dateien, Datenbank-Modelle |
| [`spec/deeplinks.md`](../spec/deeplinks.md) | Vollständige Deeplink-Tabelle und Verhalten |
| [`spec/tests-inventar.md`](../spec/tests-inventar.md) | 41 iOS-Testdateien nach Meilensteinen |

## Aufgaben

- [x] `.migration/feature_specification.md` erstellen.
- [x] Mockup-Beschreibung (`Uebergabe.md`) und `CLAUDE.md` des iOS-Projekts gelesen. Die einzelnen Artboards (`*.dc.html`) werden pro Ansicht direkt vor der Umsetzung gelesen.
- [x] API-Verträge dokumentieren:
  - [x] Mensa
  - [x] Events
  - [x] Library (inkl. `Suche-API-Analyse.md`)
  - [x] PAIA
- [x] Klären: Wie speichert iOS den PAIA-Zugang? **Gar nicht.** Token, Patron und Benutzernummer liegen nur im Speicher, es gibt kein Keychain im Code. Die Android-Funktion „Passwort optional speichern“ ist neu, siehe [paia.md](../api/paia.md).
- [x] Thoska: NFC-Ablauf, Kommandos und Parsing dokumentiert (siehe [M6](M06-thoska-nfc.md)).
- [x] Alle `@AppStorage`/`UserDefaults`-Schlüssel, Dateien und SwiftData-Modelle aufgelistet.
- [x] Deeplink-Schema vollständig erfasst.
- [x] Tests inventarisiert und Meilensteinen zugeordnet.
- [x] Lücken in [04-nicht-portierbar.md](../04-nicht-portierbar.md) nachgetragen.
- [x] Pläne der Meilensteine mit den Funden angepasst (M3, M4, M6, M7, M8).
- [ ] **Offen:** Je Endpunkt eine echte Beispielantwort der Dev-API sichern (`docs/migration/api/samples/`). Das braucht Netzzugriff auf die Dev-API und wird zu Beginn von M3 (Mensa), M5 (Events) und M7 (Katalog) erledigt. Bis dahin gelten die aus dem iOS-Code abgeleiteten Verträge.

## Wichtige Funde (Änderungen gegenüber dem ersten Plan)

1. **PAIA wird in iOS nicht gespeichert.** Auto-Login ist neue Funktion auf Android, nicht Port.
2. **Der Benutzernummer-Scanner liest einen Barcode**, keinen Text. Android: ML Kit **Barcode** Scanning (Code 39/93/128, ITF, EAN, Codabar), nicht Text Recognition.
3. **Der Strichcode des Ausweises ist ein selbst gezeichneter Code 39.** Android braucht keine ZXing-Abhängigkeit, sondern denselben Renderer in Compose.
4. **Biometrie für den Ausweis lässt in iOS nie den Gerätecode zu.** Android-Entscheidung offen (siehe [02-entscheidungen.md](../02-entscheidungen.md), Nr. 11).
5. **Hinweise auf Heute haben noch keinen Server-Endpunkt** (`LeereHinweiseQuelle`). Android bekommt zunächst dieselbe leere Quelle.
6. **Der Thoska-Tab entfällt auf Geräten ohne NFC.**
7. **Mehr** hat einen Bereich „Sprache“, der im Plan fehlte (Android: App-Sprache).
8. **Mensa-Favoriten** (Gerichte, über Namen) fehlten im Plan.
9. **Nutzergruppen-URIs der UB** sind in iOS ein offenes TODO (Chip auf dem Ausweis bleibt weg).
10. Die Merkliste enthält Watch-Abgleichslogik (Grabsteine), die für die Erst-Version entfällt.

## Abnahme

- Spezifikation und API-Dokumente liegen im Projekt. ✔
- Keine offene Frage zu PAIA-Speicherung, Mensa-Quelle und Deeplinks. ✔
- Echte Beispielantworten: noch offen (siehe oben).

## Quellen (iOS)

`Shared/`, `Campus Ilmenau/AppRouter.swift`, `Suche-API-Analyse.md`, `CLAUDE.md`, `Design/…/Uebergabe.md`
