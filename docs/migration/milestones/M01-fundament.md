# M1 – Fundament

**Status:** abgeschlossen (2026-11-23) · **Abhängig von:** M0 · **Gate:** 1 (`./gradlew :app:assembleDebug`) ✔

## Ziel

Ein Projekt, das mit Hilt, Netzwerk, DataStore und Theme baut. Noch keine Feature-Screens.

## Ergebnis

- `./gradlew :app:assembleDebug` und `./gradlew :app:testDebugUnitTest` laufen mit Exit 0 (33 Unit-Tests, 0 Fehler).
- Die App startet auf dem Emulator (API 37) im richtigen Theme, Light und Dark (Nachtblau), mit Edge-to-Edge und Plus Jakarta Sans.
- Hilt-Injection läuft end-to-end: `MainViewModel` → `AppSettings` → DataStore → Theme der `MainActivity`.

## Aufgaben

### Gradle und Abhängigkeiten
- [x] `minSdk` auf 26 angehoben.
- [x] `gradle/libs.versions.toml` aktualisiert (Kotlin 2.4.21, KSP 2.3.14, Hilt 2.60.1, Room 2.8.5, DataStore 1.3.0, Retrofit 3.0.0, OkHttp 5.5.1, kotlinx.serialization 1.13.0, Coroutines 1.11.0, Lifecycle 2.11.1, Activity 1.13.0, Core 1.19.1).
- [x] Hinzugefügt: Hilt, KSP, Room (inkl. Gradle-Plugin, Schema-Verzeichnis `app/schemas`), DataStore, kotlinx.serialization, Retrofit/OkHttp, Coroutines, MockK, Turbine, MockWebServer.
- [x] **Entscheidung Netzwerk:** Retrofit + OkHttp (siehe [02-entscheidungen.md](../02-entscheidungen.md)).
- [x] **Entscheidung Navigation:** Navigation 3, Einbau in M2.
- [x] `BuildConfig.API_BASE_URL`: debug → `https://dev-api.campus-ilmenau.de/develop/`, release → `https://api.campus-ilmenau.de/`. Keine Geheimnisse nötig, die APIs sind öffentlich.
- **Abweichung:** Coil, Navigation 3, Splashscreen, WorkManager, Material-Icons-Extended kommen **mit dem Meilenstein, der sie zum ersten Mal braucht** (M2, M5, M10), nicht auf Vorrat.
- **Abweichung:** Compose BOM bleibt auf **2026.02.01**. BOM 2026.11.01 (Compose 1.13) verlangt `compileSdk` 37.1, auf dieser Maschine ist nur `android-37.0` installiert. Aktualisierung: SDK 37.1 installieren, `compileSdk` anpassen, BOM heben.

### Projektstruktur
- [x] Pakete angelegt: `core/{common,network,time}`, `data/preferences`, `di`, `domain/model`, `ui/theme`. Die übrigen (`feature/*`, `data/local`, `data/repository`, `ui/components`, `ui/navigation`) entstehen mit ihren Meilensteinen, damit keine leeren Ordner bleiben.
- [x] `CampusApp` mit `@HiltAndroidApp`, `MainActivity` mit `@AndroidEntryPoint`, Manifest: Name, `INTERNET`.
- [x] Hilt-Module: `NetworkModule` (Json, OkHttp, Retrofit), `DataModule` (DataStore), `DispatcherModule` (IO/Default, `Clock`).
- **Abweichung Room:** Die `RoomDatabase` entsteht mit der ersten Entity (M6 Thoska bzw. M7 Merkliste). Eine leere Datenbank mit Schema v1 würde nur später eine Migration erzwingen. Plugin, Compiler und Schema-Verzeichnis sind eingerichtet und laufen mit.

### Theme
- [x] Farben aus `tokens.json` in `ui/theme/Color.kt` (Light und Dark, Nachtblau `#0E1A2E`).
- [x] `CampusColors` über `CompositionLocal` (`CampusTheme.colors`) für Slots ohne Material-Pendant (`accentSoft`, `greenSoft`, `blueSoft`, `warningFill`, `swipe*`, Marke). Material-Slots: `primary` = `buttonPrimary` (neutral), `tertiary` = `accent`, `error` = `danger`. Dynamische Farben aus.
- [x] Plus Jakarta Sans (`res/font`, aus `Design/Logo 2026/Schrift/Android`), Typografie in `Type.kt`: `displayLarge/Medium/Small` = 38/34/32 sp ExtraBold, Laufweite −2 %. Alle anderen Stile bleiben System-Schrift. Lizenz: `docs/licenses/PlusJakartaSans-OFL.txt`.
- [x] Formen (16/12/10, Kapsel, Sheet 14 oben) in `Shape.kt`, Abstände im 4-dp-Raster in `Spacing.kt`.
- [x] `enableEdgeToEdge()` mit Systemleisten, die der gewählten Darstellung folgen.
- [x] Fensterhintergrund vor dem ersten Compose-Frame in Light/Dark (`values/colors.xml`, `values-night`).

### Netzwerk und Daten
- [x] `ApiCaller`/`ApiError` (Netz, HTTP mit Fehlerhülle, Dekodierung), `RetryAfter`, `ApiJson` (snake_case, tolerant), Abbruch wird nie verschluckt.
- [x] `ApiDates`/`ApiInstantSerializer` für alle drei Datumsformate, `BerlinTime` für Europe/Berlin.
- [x] DataStore-Grundlage: `AppSettings` mit `darstellung`. Weitere Schlüssel (Preisgruppe, Mensa auf Heute, Mensa-Filter) kommen mit M3/M4.
- [x] Dispatcher-Qualifier `@IoDispatcher`, `@DefaultDispatcher`, injizierbare `Clock`.

### Ressourcen
- [x] Adaptive App-Icon aus `App Icon Light.svg`: Verlaufshintergrund, weiße Ziege, Monochrom-Ebene für Themed Icons (Android 13+). Die Ziege ist gegenüber iOS (Skalierung 0,6) auf 0,47 verkleinert und auf ihrem umschließenden Kreis zentriert, weil Launcher mit Kreismaske sonst Kopf und Hufe abschneiden (auf dem Emulator geprüft). Eine Dark-Variante gibt es auf Android nicht, das Themed Icon deckt das ab.
- [x] Standard-Launcher-Bitmaps (`mipmap-*dpi/*.webp`) entfernt, bei `minSdk` 26 reichen Adaptive Icons.
- **Abweichung Strings:** Statt die 51 Schlüssel von `Localizable.xcstrings` in einem Rutsch zu konvertieren, kommen sie **pro Ansicht** mit dem jeweiligen Screen (Skill-Regel „Strings first, per screen“). Quellsprache ist Deutsch, es gibt `values/` (de) und `values-en/`. Verfügbare Sprachen laut Katalog: **de und en**.

## Abnahme

- [x] `./gradlew :app:assembleDebug` mit Exit 0.
- [x] App startet im Emulator mit Platzhalter-Screen im richtigen Theme (Light und Dark).
- [x] Hilt-Injection in ein ViewModel funktioniert.
- [x] Unit-Tests: `ApiDatesTest` (8), `BerlinTimeTest` (7), `RetryAfterTest` (5), `ApiCallerTest` (9), `AppSettingsTest` (4).

## Bekannte Punkte für M2

- Der Platzhalter-Screen (`ui/PlatzhalterScreen.kt`) und seine Strings entfallen mit der App-Shell.
- Beim Kaltstart mit der Einstellung „Dunkel“ kann bis zum ersten gelesenen DataStore-Wert kurz das Light-Theme aufblitzen. M2 hält den Splash-Screen, bis die Darstellung geladen ist (`setKeepOnScreenCondition`).
- Beim ersten Start auf dem Emulator fällt die erste Compose-Frame langsam aus (Verifikation von Klassen, Cold Start). Baseline Profile in M11.

## Quellen (iOS)

`Design/Logo 2026/Farben/*`, `Schrift/*`, `App Icon Light.svg`, `Localizable.xcstrings`, `Shared/APIUmgebung.swift`
