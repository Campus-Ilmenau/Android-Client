# M11 – Qualität und Release

**Status:** offen · **Abhängig von:** M1–M10 · **Gate:** 4 (`./gradlew :app:assembleDebug` und `./gradlew :app:testDebugUnitTest`, beide Exit 0)

## Ziel

Die App ist stabil, getestet, barrierefrei und bereit für Google Play.

## Aufgaben

### Tests
- [x] Bibliotheks-Tests aus iOS: Die Logik war schon portiert (`LoanPresentationTest` = `BibliothekTests`, `Code39Test`/`AusweisDatenTest` = `BibliotheksausweisTests`, `BenutzernummerFormatTest` = `BenutzernummerScannerTests`, `LibraryTitlePresentationTest`, `MerklisteRepositoryTest`, `LibrarySearchQueryTest`, `LibrarySearchViewModelTest`, `LibraryCatalogRepositoryTest`, `PaiaRepositoryTest`). Neu in M11: `LibraryTitleViewModelTest` (10, Laden, Fehler, Aktualisieren, Merken = Zustandsteil von `LibraryTitleTests`), `MerklisteViewModelTest` (12, Status in einer Anfrage, Fehler = „Unbekannt“, Exemplar, Widerrufen, Meldungen = Zustandsteil von `BibMerklisteTests`), `BibliothekViewModelTest` (19, Start, Ausweis öffnen mit Prüfung, Anmelden, Abmelden). Nicht portiert, weil es auf Android kein Gegenstück gibt: Nutzergruppe-Tabelle des Ausweises (siehe 04-nicht-portierbar.md) und Face-ID-/LAError-Texte (ersetzt durch den `BiometricPrompt`-Test in `AusweisDatenTest`).
- [~] Compose-UI-Tests für Hauptflüsse: Auf dem Emulator laufen `MensaFilterSheetTest` (3: Zurücksetzen gesperrt, Ernährung wählen, Zurücksetzen) und `LoanCardTest` (4: Titel und Signatur, Verlängern mit Rückfrage, Abbrechen, Server-Sperre). Sie testen die zustandslosen Composables ohne Hilt und Netz. Offen: Login, Katalogsuche und Event merken, weil sie ein Test-DI mit gefälschtem Netz brauchen (`HiltAndroidTest` mit Fake-Repositories). Start: `./gradlew :app:connectedDebugAndroidTest`.
- [ ] Stabilitäts-Triage: Abstürze zuerst, dann Feature-Parität, dann kritische UI-Fehler. Bisher keine offenen Abstürze; der Release-Build läuft auf dem Emulator durch Heute, Mensa, Bibliothek, Events und Katalogsuche.

### Barrierefreiheit
- [ ] TalkBack-Beschriftungen wie im Mockup (`aria-label`): Beschriftungen stehen in den Screens (Rail, Teilen, Chips, Tage); ein Durchgang mit TalkBack auf dem Gerät fehlt.
- [~] Tippziele ≥ 48 dp (`Spacing.MinTouchTarget`, durchgehend verwendet), Kontrast ≥ 4,5 : 1 (Paletten aus `tokens.json`). Schriftskalierung 200 % auf dem Emulator geprüft (Heute, Mensa, Events): Gefunden und behoben wurden die Titelspalte der Heute-Karte (brach Wortmitten um, ab Skalierung 1,3 jetzt breiter) und die feste 52-dp-Uhrzeitspalte in „Als Nächstes“ (`21:` / `00`). Bibliothek (braucht Login), Katalog, Ausweis und Dialoge bei 200 % offen. Kontrast nicht gemessen.
- [ ] Reihenfolge der Fokussierung prüfen.

### Performance und Stabilität
- [ ] Baseline Profile und Startzeit-Messung (braucht ein Macrobenchmark-Modul und ein Gerät).
- [x] R8/Optimierung: Release mit `optimization { enable = true }` baut und läuft (29 MB, ML Kit eingerechnet). Keine eigenen Keep-Regeln nötig: Room, Hilt, kotlinx.serialization und Glance bringen ihre Regeln mit. Geprüft mit einer mit dem Debug-Schlüssel signierten Kopie auf dem Emulator (Heute, Mensa, Bibliothek, Events, Katalogsuche mit Covern).
- [x] ANR-Audit: kein `runBlocking`, kein `allowMainThreadQueries`, kein `Thread.sleep`; Netz und Room laufen in Coroutines. Gefunden und behoben: `ProcessCameraProvider...get()` im `onDispose` des Scanners wartet jetzt im Listener.
- [x] Crash-Reporting: entschieden, **kein** Crashlytics (und kein anderes SDK). Die App bleibt ohne Analyse- und Absturz-SDKs, das Datensicherheits-Formular bleibt damit ohne „Absturzprotokolle“. Abstürze zeigt die Play Console (Android Vitals) ohne eigenen Code.

### Sicherheit und Policy
- [x] Manifest geprüft: `INTERNET`, `NFC`, `CAMERA` (nur mit Runtime-Anfrage), `exported` nur wo nötig (Deeplinks, Launcher, Widgets), `FileProvider` nicht exportiert, `data_extraction_rules` und `backup_rules` nehmen Datenbank und Zugangsdaten aus. Tabelle in [docs/play/datenschutz-und-policy.md](../../play/datenschutz-und-policy.md).
- [x] Netzwerk: nur HTTPS (`network_security_config.xml`, `cleartextTrafficPermitted=false`); HTTP-Logging nur im Debug, `Authorization` geschwärzt; CI prüft auf `http://`.
- [x] Datenschutzangaben (Kamera, gespeicherte Anmeldedaten) und Entwurf für das Play-Datensicherheits-Formular: [docs/play/datenschutz-und-policy.md](../../play/datenschutz-und-policy.md). Das Ausfüllen in der Play Console bleibt bei dir.
- [x] Play-Policy-Check (Berechtigungen, Kamera, Biometrie, Konto, Zielgruppe) im selben Dokument. Pre-Completion-Liste: keine `TODO`-Platzhalter, alle UI-Texte in `strings.xml` (de und en), jede Berechtigung mit Aufrufstelle, kein Room-Zugriff im Main-Thread (Room verbietet ihn).

### Release
- [~] Signierung: `signingConfigs.release` liest `CAMPUS_KEYSTORE_FILE`, `CAMPUS_KEYSTORE_PASSWORD`, `CAMPUS_KEY_ALIAS`, `CAMPUS_KEY_PASSWORD` aus `~/.gradle/gradle.properties` oder der Umgebung; `.gitignore` und CI sperren Keystores im Repository. Ohne die Werte entsteht ein unsignierter Release-Build. Das Anlegen des Schlüssels und der Upload-Schlüssel in der Play Console (Play App Signing) liegt bei dir; `./gradlew bundleRelease` erzeugt das App-Bundle.
- [~] Play-Store-Eintrag: Entwürfe für Titel, Kurz- und Langbeschreibung auf Deutsch und Englisch sowie die Liste der Grafiken stehen in [docs/play/store-eintrag.md](../../play/store-eintrag.md). Offen: Screenshots, Feature-Grafik, 512-px-Icon-Export, Datenschutz-URL und Kontakt-E-Mail.
- [x] CI nach Vorbild `.githubworkflowsios.yml`: `.github/workflows/android.yml` mit Build, Unit-Tests, Lint, Release-Build (R8) und einer Sicherheitsprüfung (Keystores, Dienstkonto-Schlüssel, `http://`, Backup-Regeln). Läuft erst nach dem Push.
- [x] `CHANGELOG.md` für Android angelegt (Abschnitt „Unreleased“).
- [x] `.migration/migration_walkthrough.md`: jede umgesetzte Funktion mit der Datei, in der sie verwendet wird.

## Abnahme

- Gate 4 grün. Keine offenen Abstürze.
- Alle Hauptflüsse auf realen Geräten geprüft (inkl. NFC-Gerät).
- Pre-Completion-Checkliste: keine `TODO`-Platzhalter, alle UI-Texte in `strings.xml`, jede Berechtigung hat einen Runtime-Request mit Aufrufstelle, kein Room-Zugriff im Main-Thread.
