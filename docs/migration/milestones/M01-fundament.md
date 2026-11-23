# M1 – Fundament

**Status:** offen · **Abhängig von:** M0 · **Gate:** 1 (`./gradlew assembleDebug`)

## Ziel

Ein Projekt, das mit Hilt, Netzwerk, Datenbank, DataStore und Theme baut. Noch keine Feature-Screens.

## Aufgaben

### Gradle und Abhängigkeiten
- [ ] `minSdk` auf 26 anheben.
- [ ] `gradle/libs.versions.toml` aktualisieren: `activity-compose`, `lifecycle`, `core-ktx` auf aktuelle Versionen.
- [ ] Neu hinzufügen: Hilt, KSP, Room, DataStore, `kotlinx.serialization`, Netzwerk (Retrofit/OkHttp oder Ktor), Coil, WorkManager, Navigation, Material-Icons-Extended, Splashscreen, Test-Bibliotheken.
- [ ] Entscheidung dokumentieren: Navigation 3 vs. Navigation Compose, Retrofit vs. Ktor ([02-entscheidungen.md](../02-entscheidungen.md)).
- [ ] `BuildConfig` aktivieren, `API_BASE_URL` für debug/release setzen. Geheimnisse nur über `local.properties`.

### Projektstruktur
- [ ] Pakete anlegen nach [03-architektur.md](../03-architektur.md).
- [ ] `CampusApp` mit `@HiltAndroidApp`, `MainActivity` mit `@AndroidEntryPoint`, Manifest anpassen (Name, Internet-Berechtigung).
- [ ] Hilt-Module: Netzwerk, Datenbank, Dispatcher, Uhr.

### Theme
- [ ] Farben aus `Colors.xcassets` / `tokens.json` in `ui/theme/Color.kt` übernehmen (Light + Dark, Nachtblau `#0E1A2E`).
- [ ] `ExtendedColors` über `CompositionLocal` für Slots ohne Material-Pendant (`accentSoft`, `greenSoft`, `blueSoft`, `warningFill`, `swipe*` usw.).
- [ ] Plus Jakarta Sans als Font-Resource (`res/font/`), Typografie in `Type.kt`, Laufweite −2 % für Titel.
- [ ] Shapes (Ecken 16/12/10, Kapsel, Sheet 14 oben), Abstands-Konstanten (4-dp-Raster).
- [ ] `enableEdgeToEdge()` und Insets-Behandlung.

### Netzwerk und Daten
- [ ] `ApiUmgebung`, Basis-Client, Fehlertypen (Netz, HTTP, Serialisierung), `Retry-After`/Cache-Header.
- [ ] DataStore-Grundlage für Einstellungen.
- [ ] Room-Database-Grundgerüst (leer, Schema-Export an).

### Ressourcen
- [ ] `Localizable.xcstrings` nach `strings.xml` konvertieren (Grundstock). Neue Texte pro Screen ergänzen.
- [ ] Adaptive App-Icon aus den SVGs (`App Icon Light/Dark/Tinted`), Themed-Icon.

## Abnahme

- `./gradlew assembleDebug` läuft mit Exit 0.
- App startet im Emulator mit einem Platzhalter-Screen im richtigen Theme (Light und Dark).
- Hilt-Injection in ein Beispiel-ViewModel funktioniert.

## Quellen (iOS)

`Design/Logo 2026/Farben/*`, `Schrift/*`, `Localizable.xcstrings`, `Shared/APIUmgebung.swift`
