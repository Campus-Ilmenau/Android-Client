# Migration Walkthrough

Jede umgesetzte Funktion mit der Datei, in der sie verwendet wird (Einstieg; Pfade relativ zu
`app/src/main/java/de/kaigothe/campusilmenau/`). Der Plan steht in `docs/migration/README.md`, die Aufgaben je Meilenstein in
`docs/migration/milestones/`.

## Hülle und Navigation (M1, M9)

| Funktion | Wo |
|---|---|
| App-Hülle, Tab-Leiste, Rail/Seitenleiste, Tastenkürzel | `ui/navigation/AppShell.kt`, `CampusTabBar.kt`, `CampusNavigationRail.kt`, `AppShortcuts.kt` |
| Back Stacks je Tab, „Ausstieg über Heute“ | `ui/navigation/NavigationState.kt`, `Navigator.kt` |
| Deeplinks `campusilmenau://…` | `ui/navigation/DeepLink.kt`, `DeepLinkRouter.kt`, `MainActivity.kt` |
| Liste neben Detail (ab 840 dp) | `ui/navigation/AppShell.kt` (`ListDetailSceneStrategy`), `ui/adaptive/SplitView.kt` |
| Spaltenzahl aus der Breite | `ui/adaptive/AdaptiveColumns.kt`, `WindowWidthClass.kt` |
| Start-Animation, Splash | `ui/launch/`, `MainActivity.kt` |
| Design-System (Farben, Schrift, Bausteine) | `ui/theme/`, `ui/components/` |

## Heute (M3)

| Funktion | Wo |
|---|---|
| Phasen des Tages, Jetzt-Karte, „Als Nächstes“, „Heute Abend“ | `domain/model/heute/HeuteContent.kt`, `ui/heute/HeuteScreen.kt`, `HeuteCards.kt` |
| Laden und Aktualisieren | `ui/heute/HeuteViewModel.kt` |

## Mensa (M3)

| Funktion | Wo |
|---|---|
| Speiseplan, Mensen als Seiten bzw. Spalten | `ui/mensa/MensaScreen.kt`, `MensaViewModel.kt` |
| Filter | `domain/model/mensa/MensaFilter.kt`, `ui/mensa/MensaFilterSheet.kt` |
| Gericht-Detail, Favoriten | `ui/mensa/MensaMealDetailScreen.kt`, `MensaMealDetailViewModel.kt` |
| Öffnungszeiten | `ui/mensa/MensaOpeningHoursScreen.kt` |
| Daten | `data/repository/MensaRepository.kt`, `data/remote/mensa/` |

## Thoska (M4)

| Funktion | Wo |
|---|---|
| Auslesen per NFC | `data/nfc/ThoskaReader.kt`, `ui/thoska/ThoskaScreen.kt` |
| Verlauf, Kartennamen | `ui/thoska/ThoskaHistoryScreen.kt`, `data/local/` (Room) |

## Events (M5)

| Funktion | Wo |
|---|---|
| Liste, Tagesleiste, Filter | `ui/events/EventsScreen.kt`, `EventsViewModel.kt`, `EventsFilterSheet.kt` |
| Detail, Kalender-Eintrag, Karte | `ui/events/EventDetailScreen.kt` |
| Vergangene | `ui/events/EventsPastScreen.kt` |

## Bibliothek (M6, M7)

| Funktion | Wo |
|---|---|
| Konto (PAIA), Ausleihen, Vormerkungen, Mitteilungen | `ui/bibliothek/BibliothekScreen.kt`, `BibliothekViewModel.kt`, `data/repository/PaiaRepository.kt` |
| Passwort im Keystore | `data/security/KeystoreCredentialStore.kt` |
| Benutzernummer scannen | `ui/bibliothek/BenutzernummerScanner.kt` |
| Katalogsuche, Treffer, Titel | `ui/bibliothek/katalog/` |
| Merkliste | `ui/bibliothek/merkliste/`, `data/repository/MerklisteRepository.kt` |
| Bibliotheksausweis (Biometrie, Code 39) | `ui/bibliothek/ausweis/`, `domain/model/library/ausweis/` |

## Mehr, Teilen, Shortcuts, Widgets (M8, M10)

| Funktion | Wo |
|---|---|
| Einstellungen, Sprache, App-Icon, gespeicherte Daten | `ui/mehr/` |
| Teilen als Text | `domain/model/share/`, `ui/share/ShareSupport.kt`, `ShareMenu.kt` |
| Teilen als Bild | `ui/share/ShareImage.kt`, `ui/share/card/` |
| App-Shortcuts | `res/xml/shortcuts.xml`, `ui/shortcuts/CampusShortcuts.kt` |
| Widgets „Mensa heute“, „Heute Abend“ | `ui/widget/`, `data/repository/WidgetDataSource.kt`, `CampusApp.kt` (Aktualisierung) |

## Sicherheit und Release (M11)

| Funktion | Wo |
|---|---|
| Nur HTTPS | `res/xml/network_security_config.xml`, `AndroidManifest.xml` |
| Sicherung ohne Zugangsdaten und Datenbank | `res/xml/data_extraction_rules.xml`, `backup_rules.xml` |
| Signierung ohne Geheimnisse im Repository | `app/build.gradle.kts` (`CAMPUS_KEYSTORE_*`) |
| CI | `.github/workflows/android.yml` |
| Play-Angaben | `docs/play/datenschutz-und-policy.md` |
