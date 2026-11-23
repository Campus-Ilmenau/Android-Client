# 03 – Zielarchitektur

## Prinzipien

- MVVM: `ViewModel` hält `StateFlow<UiState>`, Screens lesen mit `collectAsStateWithLifecycle()`.
- Unidirektionaler Datenfluss, State Hoisting, `LazyColumn` immer mit `key`.
- Hilt für DI. Dispatcher werden injiziert (testbar).
- Fachlogik (Tagesphasen, Filter, Parser, Formatierung) liegt in reinem Kotlin ohne Android-Abhängigkeit, damit sie unit-testbar und später für Wear OS teilbar ist.
- `MainActivity` macht nur `enableEdgeToEdge()`, Theme und Navigations-Host.

## Paketstruktur

Root-Paket: `de.kaigothe.campusilmenau` (bleibt unverändert).

```
app/src/main/java/de/kaigothe/campusilmenau/
├── MainActivity.kt
├── CampusApp.kt                 # @HiltAndroidApp
├── di/                          # Hilt-Module (Netzwerk, Datenbank, Dispatcher)
├── core/
│   ├── network/                 # API-Client, ApiUmgebung, Fehlerbehandlung
│   ├── time/                    # Uhr (injizierbar), Formatierung
│   ├── security/                # Keystore-gestützte Ablage (PAIA)
│   └── common/                  # Result-Typen, Hilfsfunktionen
├── data/
│   ├── local/                   # Room: Entities, DAOs, Database
│   ├── preferences/             # DataStore
│   └── repository/              # Repositories je Feature
├── domain/
│   └── model/                   # Fachmodelle, reine Logik
├── feature/
│   ├── heute/
│   ├── mensa/
│   ├── thoska/
│   ├── bibliothek/
│   │   ├── konto/               # PAIA: Ausleihen, Mitteilungen, Login
│   │   ├── katalog/
│   │   ├── merkliste/
│   │   └── ausweis/
│   ├── events/
│   ├── mehr/
│   ├── teilen/
│   └── shortcuts/
├── ui/
│   ├── theme/                   # Color.kt, Type.kt, Shape.kt, Theme.kt
│   ├── components/              # Card, Chip, Buttons, Toast, DiagonalBands, GoatLoader
│   ├── navigation/              # NavGraph, Routen, Deeplinks, Shell
│   └── launch/                  # Start-Animation
└── widget/                      # Glance-Widgets
```

Innerhalb von `feature/<name>/` gilt: `XxxScreen.kt`, `XxxViewModel.kt`, `XxxUiState.kt`, bei Bedarf `components/`.

## Bibliotheken (Version Catalog `gradle/libs.versions.toml`)

| Zweck | Bibliothek |
|---|---|
| UI | Compose BOM, Material 3, Material-Icons-Extended, Adaptive (`material3-adaptive`), Navigation (siehe unten) |
| DI | Hilt (`hilt-android`, `hilt-navigation-compose`), KSP |
| Netzwerk | Retrofit + OkHttp oder Ktor (M1), `kotlinx.serialization` |
| Datenbank | Room (KSP) |
| Einstellungen | DataStore Preferences |
| Bilder | Coil 3 |
| Hintergrund | WorkManager |
| Widgets | Glance (`glance-appwidget`, `glance-material3`) |
| Kamera / Scan | CameraX, ML Kit **Barcode Scanning** (der Scanner liest einen Barcode, keinen Text) |
| Barcode erzeugen | keine Bibliothek: eigener Code-39-Renderer (wie iOS `Strichcode`) |
| Biometrie | `androidx.biometric` |
| Sicherheit | Android Keystore (`security-crypto` oder Tink) |
| Splash | `androidx.core:core-splashscreen` |
| Tests | JUnit, MockK, Turbine, `kotlinx-coroutines-test`, Compose UI Test |

Bestehende Versionen (`activity-compose`, `lifecycle`, `core-ktx`) werden in M1 aktualisiert.

## Mapping iOS → Android

| iOS | Android |
|---|---|
| SwiftUI `View` | `@Composable` |
| `@State`, `@Observable` | `remember`/`mutableStateOf`, `ViewModel` + `StateFlow` |
| `@Environment(AppRouter.self)` | Navigation-State im `ViewModel`/`NavController`, Hilt-Singleton |
| `NavigationStack` | Navigation Compose / Navigation 3 |
| Tab-Leiste (`TabBar`) | `NavigationBar` bzw. eigener Baustein |
| `List`/`LazyVStack` | `LazyColumn`/`LazyVerticalGrid` |
| `.sheet` | `ModalBottomSheet` |
| `@AppStorage`/`UserDefaults` | DataStore Preferences |
| App Group (Widget-Teilen) | DataStore, den App und Glance-Widget gemeinsam nutzen (gleiche App, kein Group-Container nötig) |
| SwiftData `@Model` | Room `@Entity` + `@Dao` |
| `async/await`, `Task` | Coroutines, `viewModelScope` |
| Combine / `AsyncStream` | `Flow`/`StateFlow` |
| `URLSession` | Retrofit/OkHttp bzw. Ktor |
| CoreNFC `NFCTagReaderSession` | `NfcAdapter.enableReaderMode` + `IsoDep` |
| LocalAuthentication (Face ID) | `BiometricPrompt` |
| VisionKit/Vision (Scan) | CameraX + ML Kit |
| EventKit | `CalendarContract` Insert-Intent |
| Charts | Compose-Canvas / Vico |
| `ShareLink`/UIActivityViewController | `Intent.ACTION_SEND` + `FileProvider` |
| WidgetKit | Glance |
| AppIntents/Siri | App Shortcuts / AppFunctions |
| Deeplink `onOpenURL` | Intent-Filter + Navigation-Deeplinks |
| Launch Screen + Animation | SplashScreen-API + Compose-Animation |
| `Localizable.xcstrings` | `res/values/strings.xml` |
| `.xcassets` Farben | Theme-`ColorScheme` + `ExtendedColors` |

## Navigation

- Fünf Tabs: Heute, Mensa, Thoska, Bibliothek, Events. „Mehr“ ist kein Tab, sondern der Personen-Button oben rechts auf Heute.
- Jeder Tab behält einen eigenen Back Stack (`launchSingleTop`, `restoreState`).
- Alle Ziele aus `AppRouter` (Mensa, Gericht, Events, Event, Thoska auslesen, Katalog, Titel, Öffnungszeiten, Merkliste, Mehr-Bereich) sind Routen und per Deeplink erreichbar (`campusilmenau://…`).
- Die iPad-Hülle wird in M9 über Window-Size-Classes abgebildet (Rail/Drawer statt Bottom Bar).

## Tests

- Fachlogik: JUnit + Turbine + `TestClock` (Pendant zu `TestClock.swift`).
- ViewModels: `kotlinx-coroutines-test`, Fake-Repositories.
- UI: Compose-UI-Tests für Hauptflüsse.
- Die 40 iOS-Testdateien dienen als Testfall-Katalog. Reihenfolge: zuerst Mensa-, Heute-, Katalog-, Event-Logik.
