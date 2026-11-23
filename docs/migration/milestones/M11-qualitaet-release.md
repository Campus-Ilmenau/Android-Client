# M11 – Qualität und Release

**Status:** offen · **Abhängig von:** M1–M10 · **Gate:** 4 (`./gradlew :app:assembleDebug` und `./gradlew :app:testDebugUnitTest`, beide Exit 0)

## Ziel

Die App ist stabil, getestet, barrierefrei und bereit für Google Play.

## Aufgaben

### Tests
- [ ] Verbliebene iOS-Tests portieren (Liste in M0), Kernlogik zuerst.
- [ ] Compose-UI-Tests für Hauptflüsse: Mensa-Filter, Login, Katalogsuche, Event merken.
- [ ] Stabilitäts-Triage: Abstürze zuerst, dann Feature-Parität, dann kritische UI-Fehler.

### Barrierefreiheit
- [ ] TalkBack-Beschriftungen wie im Mockup (`aria-label`).
- [ ] Tippziele ≥ 48 dp, Kontrast ≥ 4,5 : 1, Schriftskalierung bis 200 %.
- [ ] Reihenfolge der Fokussierung prüfen.

### Performance und Stabilität
- [ ] Baseline Profile und Startzeit-Messung.
- [ ] R8/Optimierung, Keep-Regeln prüfen.
- [ ] ANR-Audit (keine Netz-/DB-Zugriffe im Main-Thread).
- [ ] Crash-Reporting einbinden (Crashlytics).

### Sicherheit und Policy
- [ ] Manifest prüfen: nur nötige Berechtigungen, `exported`-Flags, Backup-Regeln (`allowBackup`, `data_extraction_rules` ohne Passwort/Token).
- [ ] Netzwerk: nur HTTPS.
- [ ] Datenschutzangaben (Kamera, gespeicherte Anmeldedaten) und Play-Data-Safety-Formular.
- [ ] Play-Policy-Check.

### Release
- [ ] Signierung, Release-Build, App-Bundle.
- [ ] Play-Store-Eintrag: Texte, Screenshots (aus den Teilen-Screenshots ableiten), Icon.
- [ ] CI nach Vorbild `.githubworkflowsios.yml`: Build, Unit-Tests, Lint.
- [ ] `CHANGELOG.md` für Android anlegen.
- [ ] `.migration/migration_walkthrough.md`: jede umgesetzte Funktion mit der Datei, in der sie verwendet wird.

## Abnahme

- Gate 4 grün. Keine offenen Abstürze.
- Alle Hauptflüsse auf realen Geräten geprüft (inkl. NFC-Gerät).
- Pre-Completion-Checkliste: keine `TODO`-Platzhalter, alle UI-Texte in `strings.xml`, jede Berechtigung hat einen Runtime-Request mit Aufrufstelle, kein Room-Zugriff im Main-Thread.
