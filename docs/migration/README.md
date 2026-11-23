# Migration Campus Ilmenau: iOS → Android

Dieser Ordner dokumentiert die Umsetzung der iOS-App „Campus Ilmenau“ als Android-App (Kotlin, Jetpack Compose).

- **iOS-Quelle:** `/Users/kaigothe/Projekte/campus-ilmenau/Campus Ilmenau`
- **Android-Ziel:** dieses Projekt, `de.kaigothe.campusilmenau`
- **Stand der Planung:** 2026-11-23

## Inhalt

| Datei | Zweck |
|---|---|
| [01-ausgangslage.md](01-ausgangslage.md) | Analyse der iOS-App: Umfang, Features, Backend, Zustand des Android-Projekts |
| [02-entscheidungen.md](02-entscheidungen.md) | Getroffene Entscheidungen (Hilt, minSdk 26, Wear OS später usw.) |
| [03-architektur.md](03-architektur.md) | Zielarchitektur, Paketstruktur, Bibliotheken, Mapping iOS → Android |
| [04-nicht-portierbar.md](04-nicht-portierbar.md) | iOS-Funktionen ohne 1:1-Gegenstück und die Ersatzlösung |
| [05-risiken.md](05-risiken.md) | Risiken und Gegenmaßnahmen |
| [milestones/](milestones/) | Ein Dokument pro Meilenstein mit Aufgaben, Abnahmekriterien und Quellen |

## Meilensteine im Überblick

| ID | Meilenstein | Status |
|---|---|---|
| [M0](milestones/M00-audit-spezifikation.md) | Audit und Spezifikation | offen |
| [M1](milestones/M01-fundament.md) | Fundament (Gradle, Hilt, Theme, Netzwerk) | offen |
| [M2](milestones/M02-design-system.md) | Design-System-Bausteine und App-Shell | offen |
| [M3](milestones/M03-mensa.md) | Mensa | offen |
| [M4](milestones/M04-heute.md) | Heute | offen |
| [M5](milestones/M05-events.md) | Events | offen |
| [M6](milestones/M06-thoska-nfc.md) | Thoska (NFC) | offen |
| [M7](milestones/M07-bibliothek.md) | Bibliothek | offen |
| [M8](milestones/M08-mehr-teilen.md) | Mehr, Teilen, Einstellungen | offen |
| [M9](milestones/M09-adaptive-layouts.md) | Adaptive Layouts (Tablet, Faltgeräte) | offen |
| [M10](milestones/M10-widgets-shortcuts.md) | Widgets und Shortcuts | offen |
| [M11](milestones/M11-qualitaet-release.md) | Qualität und Release | offen |

Spätere Erweiterung: **Wear OS** (eigenes Modul, siehe [02-entscheidungen.md](02-entscheidungen.md)).

## Empfohlene Reihenfolge

M0 → M1 → M2 → M3 → M4 → M6 → M7 → M5 → M8 → M9 → M10 → M11

Begründung: Mensa (M3) spielt das gesamte Fundament einmal durch (Netzwerk, Theme, Navigation, Tests). Thoska (M6) trägt das größte technische Risiko (echte Karte nötig). Die Bibliothek (M7) ist mit ca. 10k LOC der größte Block und wird in Teilschritten umgesetzt.

## Build-Gates

Nach diesen Meilensteinen muss der Build grün sein, bevor es weitergeht:

| Gate | Nach | Prüfung |
|---|---|---|
| 1 | M1 | `./gradlew assembleDebug` |
| 2 | M3 | `./gradlew testDebugUnitTest` für die Mensa-Logik, Screens im Emulator geprüft |
| 3 | M7 | `./gradlew compileDebugKotlin`, danach `assembleDebug` |
| 4 | M11 | `./gradlew :app:assembleDebug` und `./gradlew :app:testDebugUnitTest`, beide Exit 0 |

Zusätzlich nach jedem Feature-Slice (oder alle 2 Screens): `./gradlew compileDebugKotlin`.

## Arbeitsweise

- Eine Ansicht pro Schritt: erst `Uebergabe.md` und die `.dc.html` des Designs lesen, dann umsetzen, dann mit dem Mockup vergleichen.
- Konzepte portieren, nicht Zeile für Zeile übersetzen.
- Fachlogik und Tests aus iOS dienen als Spezifikation (Testfälle werden mitportiert).
- Keine Geheimnisse im Quelltext: Schlüssel gehören in `local.properties` und werden über `BuildConfig` gelesen.
- Jeder Screen bekommt `@Preview` (Light und Dark).
- Jeder Meilenstein wird hier mit Status und Datum nachgeführt.
