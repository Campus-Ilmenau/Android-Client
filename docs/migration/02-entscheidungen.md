# 02 – Entscheidungen

Stand: 2026-11-23

| # | Thema | Entscheidung | Begründung |
|---|---|---|---|
| 1 | Wear OS | **Später** als eigenes Modul. | Basis-App zuerst. Fachlogik so strukturieren, dass sie sich teilen lässt (siehe [03-architektur.md](03-architektur.md)). |
| 2 | Siri-Ersatz | **Wenn möglich** in M10 (App Shortcuts bzw. AppFunctions). | Funktionsumfang kann kleiner sein als bei Siri. Widgets haben Vorrang. |
| 3 | Dependency Injection | **Hilt** | Offizielle Google-Lösung, ViewModel-/Lifecycle-Integration, Prüfung zur Compile-Zeit. |
| 4 | minSdk | **26** (bisher 24) | Adaptive Icons, bessere NFC-/Biometrie-APIs. |
| 5 | PAIA-Passwort | **Optional speicherbar** (Auto-Login). | Schalter „Angemeldet bleiben“ im Login-Sheet. Passwort verschlüsselt über Android Keystore. Ohne Schalter wird, wie in iOS, nur die Sitzung im Speicher gehalten. **Neu gegenüber iOS:** Die iOS-App speichert nichts (M0). Das Token wird nie gespeichert. |
| 6 | Schrift | Plus Jakarta Sans für große Titel und Zahlen, System-Schrift (Roboto) für den Rest. | SF Pro ist auf Android nicht lizenziert. Schriftskalierung über `sp`. |
| 7 | Farben | Material-3-Theme aus `Colors.xcassets`/`tokens.json`, Light und Dark. | Dark ist Nachtblau `#0E1A2E`. Regeln aus dem Design bleiben (siehe unten). |
| 8 | API-Umgebung | `BuildConfig`: debug → `dev-api…/develop`, release → `api.campus-ilmenau.de`. | Wie `APIUmgebung.swift`. |
| 9 | Lokalisierung | `strings.xml`, Deutsch als Standard. | `Localizable.xcstrings` wird konvertiert. Alle UI-Texte über `stringResource`. |
| 10 | Dokumentationsort | `docs/migration/` | Sichtbar in der Projektansicht. Die vom Migrations-Skill geforderte `feature_specification.md` liegt unter `.migration/feature_specification.md`. |
| 11 | Biometrie für den Bibliotheksausweis | **Offen.** Vorschlag: Biometrie **oder** Geräte-PIN (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`). | iOS erlaubt nur Face ID/Touch ID und nie den Gerätecode. Auf Android haben viele Geräte nur eine PIN, der Ausweis wäre dort sonst unbenutzbar. Strikte Parität wäre `BIOMETRIC_STRONG` allein. Entscheidung vor M7e. |
| 12 | Thoska-Tab ohne NFC | Tab **entfällt** auf Geräten ohne NFC-Hardware. | Wie iOS (`NFCVerfuegbarkeit`). Ist NFC vorhanden, aber ausgeschaltet, erscheint ein Hinweis mit Weg in die Einstellungen. |
| 13 | Strichcode des Ausweises | Eigener **Code-39**-Renderer, kein ZXing. | iOS zeichnet Code 39 selbst. Gleiche Logik, keine zusätzliche Bibliothek. |
| 14 | Scanner der Benutzernummer | ML Kit **Barcode** Scanning, nicht Text Recognition. | iOS liest den Barcode auf der Thoska-Rückseite. |
| 15 | Merkliste-Abgleich (Watch) | Entfällt bis Wear OS. | `foundAt`/`changedAt` werden mitgeführt, Grabsteine nicht. |

## Design-Regeln (aus dem iOS-`CLAUDE.md`, gelten weiter)

- Farben nur über Theme-Slots, nie Hex-Werte im Screen-Code.
- Grün und Blau nur als Flächen (Bänder, Badges), nie als Text oder Button.
- Buttons neutral (`buttonPrimary`, `buttonSecondary`). Rot nur für kritische Aktionen (Abmelden, Löschen).
- `accent` (Orange) für Links, aktiven Tab, Auswahl, nicht für Buttons.
- Text braucht mindestens 4,5 : 1 Kontrast.
- Startseite „Heute“ zeigt keine Live-Daten wie Thoska-Guthaben oder Anzahl Ausleihen (sie veralten).
- Tippziele mindestens 48 × 48 dp (Android-Richtlinie, statt 44 pt), Abstände in 4-dp-Schritten.
- Abstände: Bildschirmrand 16, Kartenabstand 10, Abschnittsabstand 22. Ecken: Karten 16, Buttons 12, Segment 10, Chips Kapsel, Sheets 14 oben.

## In M1 entschieden

| # | Thema | Entscheidung | Begründung |
|---|---|---|---|
| 16 | Navigation | **Navigation 3** (stabil 1.2.x), Einbau in M2. | Stabil, Back Stack liegt in der eigenen Hand (passt zum Router-Modell der iOS-App mit Zielen von außen), Szenen für List-Detail in M9. |
| 17 | Netzwerk | **Retrofit 3 + OkHttp 5** mit `kotlinx.serialization`. | Verbreitet, gut testbar (MockWebServer), `HttpException` mit Fehlerhülle lässt sich sauber in `ApiError` übersetzen. |
| 18 | Kotlin/KSP | Kotlin 2.4.21, KSP 2.3.14. | Mit AGP 9.5.2 (eingebautes Kotlin), Hilt 2.60.1 und Room 2.8.5 gebaut und getestet. |
| 19 | Compose BOM | **2026.02.01** (vorerst nicht 2026.11.01). | BOM 2026.11.01 verlangt `compileSdk` 37.1, installiert ist nur 37.0. Aktualisierung nach Installation der Plattform 37.1. |
| 20 | Room | Datenbank entsteht mit der ersten Entity (M6/M7). | Keine leere Schema-Version 1. Plugin, Compiler und Schema-Verzeichnis sind eingerichtet. |
| 21 | Abhängigkeiten | Erst mit dem Meilenstein, der sie braucht (Coil, Navigation 3, Splashscreen, WorkManager, Icons). | Keine ungenutzten Bibliotheken im Build. |
| 22 | App-Icon | Ziege auf 0,47 verkleinert (iOS: 0,6), kein Dark-Icon. | Kreismaske der Launcher schneidet sonst Kopf und Hüfe ab. Themed Icon (Android 13+) ersetzt die Dark-Variante. |

## Offen / später entscheiden

- Chart-Bibliothek für den Thoska-Verlauf (Compose-Canvas oder Vico, Entscheidung in M6).
- Biometrie für den Ausweis (Entscheidung 11, vor M7e).
