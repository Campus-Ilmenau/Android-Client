# 01 – Ausgangslage

## iOS-App

SwiftUI-App für Studierende der TU Ilmenau. Projektpfad: `Campus Ilmenau` (Nachbarordner dieses Repos).

### Umfang

| Target / Ordner | Dateien | LOC |
|---|---|---|
| App (`Campus Ilmenau/`) | 158 | ca. 31.000 |
| Shared (`Shared/`) | 43 | ca. 5.700 |
| Tests (`Campus IlmenauTests/`) | 41 | ca. 11.800 |
| Widgets (`CampusWidgets/`) | 9 | ca. 1.400 |
| Watch (`Campus Ilmenau Watch/`, `WatchShared/`, `Watch-Widgets/`) | 35 | ca. 4.100 |
| SiriShared | 5 | ca. 350 |

### Feature-Bereiche der App (LOC)

| Bereich | LOC | Inhalt |
|---|---|---|
| Bibliothek | 9.800 | PAIA-Login, Ausleihen, Mitteilungen, Katalogsuche, Merkliste, Ausweis mit Barcode (Face ID), Kamera-Scan der Benutzernummer |
| Events | 3.900 | Liste, Filter, Detail, Merkliste, Kalender-Export, „Heute Abend“ |
| DesignSystem | 2.100 | Card, Chip, Buttons, DiagonalBands, Toast, GoatLoading |
| Heute | 2.000 | Tagesphasen, Jetzt-Karte, Hinweise, Mensa-Sheet |
| Mensa | 1.900 | 4 Mensen, bis 3 Wochen, Filter, Preisgruppen, Öffnungszeiten, Detail |
| Mehr | 1.800 | Darstellung, App-Icons, Links, Gespeicherte Daten, Siri-Tipps |
| Teilen | 1.500 | Teilkarten als Bild, Texte für Gericht/Event/Medium |
| Siri | 1.050 | AppIntents, Katalog-Intents |
| Thoska | 800 | NFC-Auslesen, Verlauf, Kartennamen |
| Pad | 600 | iPad-Shell (Seitenleiste, Zwei-Spalten-Layouts) |
| Launch | 400 | Start-Animation „Durch die Ziege“ |

### Verwendete Apple-Frameworks

SwiftUI, Foundation, AppIntents, WidgetKit, Observation, UIKit, SwiftData, CoreNFC, WatchConnectivity, VisionKit, Vision, LocalAuthentication, EventKit, EventKitUI, CoreImage, Charts, SafariServices, AVFoundation, Combine.

### Backend

- Eigene API: `https://api.campus-ilmenau.de` (Release), `https://dev-api.campus-ilmenau.de/develop` (Debug)
  - `GET /v1/events`, `/v1/events/{id}`, `/v1/events/organizers`
  - `GET /v1/library/search`, `/v1/library/media/{id}`, `/v1/library/availability`, `/v1/library/features`
  - Mensa-Endpunkte (siehe M0)
- GBV PAIA: `https://paia.gbv.de/DE-Ilm1` (Bibliothekskonto)
- Details der Katalogsuche: `Suche-API-Analyse.md` im iOS-Projekt.

### Lokale Daten (iOS)

- SwiftData: `ThoskaBalanceRecord`, `ThoskaCardName`
- UserDefaults / `@AppStorage` (teils App Group): Preisgruppe, Heute-Mensa, Darstellung, Siri-Tipps, Mensa-Filter
- Keychain: wird nicht benutzt. Der PAIA-Zugang (Token, Benutzernummer) liegt **nur im Speicher** und ist nach einem Neustart weg (in M0 geklärt, siehe [api/paia.md](api/paia.md)).
- Dateien in Application Support: `Merkliste.json` (Katalogtitel), `MerklisteEvents.json` (Event-IDs). Details in [spec/lokale-daten.md](spec/lokale-daten.md).

### Berechtigungen (Info.plist)

| iOS | Zweck | Android-Gegenstück |
|---|---|---|
| NFC (ISO 7816, AID `D2760000850100`) | Thoska auslesen | `android.permission.NFC` |
| NSCameraUsageDescription | Benutzernummer scannen | `CAMERA` (Runtime) |
| NSFaceIDUsageDescription | Bibliotheksausweis schützen | `BiometricPrompt` (`USE_BIOMETRIC`) |
| NSCalendarsWriteOnly | Event in Kalender | `CalendarContract`-Insert-Intent (keine Berechtigung) |
| NSPhotoLibraryAdd | geteilte Bilder sichern | `MediaStore` bzw. Share-Intent (keine Berechtigung ab API 29) |
| URL-Scheme `campusilmenau` | Deeplinks | Intent-Filter mit Scheme `campusilmenau` |

### Design

Das Design liegt im iOS-Projekt als Submodul `Design/` (Repo `campus-ilmenau-ci`, nicht ändern):

- `Design/Logo 2026/Mockup/Uebergabe.md` – Beschreibung aller Ansichten (zuerst lesen)
- `Design/Logo 2026/Mockup/artboards/*.dc.html` – Quelltext je Ansicht
- `Design/Logo 2026/Farben/` – `Colors.xcassets`, `tokens.json`
- `Design/Logo 2026/Schrift/` – Plus Jakarta Sans
- `Design/Logo 2026/Launch/` – Start-Animation
- `Design/Logo 2026/App Icon *.svg`

## Android-Projekt (Ist-Zustand)

Leeres Compose-Gerüst:

- Namespace/applicationId: `de.kaigothe.campusilmenau`
- AGP 9.5.2, Kotlin 2.2.10, Compose BOM 2026.02.01, compileSdk/targetSdk 37, minSdk 24 (wird auf 26 angehoben)
- Vorhanden: `MainActivity.kt`, `ui/theme/{Color,Type,Theme}.kt`, Standard-Icons
- Nicht vorhanden: DI, Navigation, Netzwerk, Room, DataStore, Tests über das Beispiel hinaus
- Veraltete Versionen: `activity-compose 1.8.0`, `lifecycle-runtime-ktx 2.6.1`, `core-ktx 1.10.1`
