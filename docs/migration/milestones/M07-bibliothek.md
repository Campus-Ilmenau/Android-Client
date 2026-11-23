# M7 – Bibliothek

**Status:** offen · **Abhängig von:** M2, Klärung PAIA-Speicherung (M0) · **Gate:** 3 (`compileDebugKotlin`, dann `assembleDebug`)

## Ziel

Bibliothekskonto, Katalogsuche, Merkliste und Ausweis. Größter Block (ca. 9.800 LOC iOS), daher in fünf Teilschritten mit je eigenem Build.

## 7a – PAIA-Konto

- [ ] PAIA-Client (`ServicesPAIAService`): Login, Token, Ausleihen, Vormerkungen, Mitteilungen.
- [ ] Modelle `PAIALoanItem`, `PAIANotification`, `PAIAHoldCard`.
- [ ] Zugangsspeicherung: Token verschlüsselt (Keystore). **Optional** Passwort speichern (Schalter „Angemeldet bleiben“, Auto-Login).
- [ ] Abmelden löscht Token und Passwort (roter, kritischer Button).
- [ ] Screens: Abgemeldet (`PAIASignedOutView`), Ausleihen (`BibliothekScreen`, `PAIALoanItemRow`, Timeline), Mitteilungen, Detailblatt (`BibliothekDetailBlatt`).
- [ ] Badges „Gleich zu“/„Fällig“, Aufbereitung (`BibliothekPresentation`).

## 7b – Login-Sheet und Scan

- [ ] Login-Sheet (Formular stabil gegenüber Szenenwechsel/Autofill, bekannter iOS-Fehler).
- [ ] Autofill/Credential Manager.
- [ ] Benutzernummer: Buchstaben erlaubt.
- [ ] Kamera-Scan der Benutzernummer: CameraX + ML Kit. `CAMERA`-Berechtigung als Runtime-Request im Scan-Auslöser, Fallback manuelle Eingabe.

## 7c – Katalog

- [ ] Such-Modell (`LibrarySearchModel`): Debounce 700 ms, Generationszähler/Abbruch, Reset bei leerem Feld.
- [ ] Parameter und Quellen laut `Suche-API-Analyse.md`: `q`, `title`, `author`, `isbn`, `subject`, `year_from/to`, `type`, `in=local|catalog|content`, `page`, `per_page=20`.
- [ ] Fortsetzung „Im Gesamtkatalog suchen“ (`in=catalog`, `page=1`, Deduplizierung per `id`).
- [ ] Availability-Batching (80 ms, max. 20 IDs), `GET /v1/library/features` beim Öffnen.
- [ ] Cover mit Retry bei `cover_pending` (Coil, `Retry-After`).
- [ ] UI: Katalog-Screen, Ergebniskarte, erweiterte Suche, Medienart-Pills, Titelansicht (`LibraryTitleView`, `LibraryTitleModel`), Pull-to-Refresh.
- [ ] Hinweise: langsamer Katalog (`X-Cache-Status: stale`), Wartehinweis nach 900 ms.

## 7d – Merkliste

- [ ] Merkliste lokal (Room), Abgleich mit Verfügbarkeit.
- [ ] Liste und Titel (Tablet: nebeneinander, siehe M9).

## 7e – Ausweis

- [ ] Bibliotheksausweis mit Strichcode (ZXing), Schutz per `BiometricPrompt` mit Geräte-PIN als Fallback.
- [ ] Helligkeit/Bildschirm beim Anzeigen optimieren.

## Tests (aus iOS portieren)

- [ ] `BibliothekTests`, `LibraryCatalogTests`, `LibrarySearchModelTests`, `LibraryTitleTests`, `BibMerklisteTests`, `BibliotheksausweisTests`, `BenutzernummerScannerTests`

## Abnahme

- Login (mit und ohne „Angemeldet bleiben“), Ausleihen, Mitteilungen, Abmelden.
- Suche verhält sich wie iOS (Debounce, Gesamtkatalog-Fortsetzung, Deduplizierung).
- Ausweis erscheint erst nach Biometrie/PIN.
- Passwort wird nie im Klartext gespeichert oder geloggt.

## Quellen (iOS)

`Campus Ilmenau/Bibliothek/*` (inkl. `Katalog/`, `Merkliste/`, `Ausweis/`, `Login/`), `ViewsBibliothek*.swift`, `ServicesPAIAService.swift`, `ModelsPAIA*.swift`, `Suche-API-Analyse.md`
