# M7 – Bibliothek

**Status:** offen · **Abhängig von:** M2, Klärung PAIA-Speicherung (M0) · **Gate:** 3 (`compileDebugKotlin`, dann `assembleDebug`)

## Ziel

Bibliothekskonto, Katalogsuche, Merkliste und Ausweis. Größter Block (ca. 9.800 LOC iOS), daher in fünf Teilschritten mit je eigenem Build.

## 7a – PAIA-Konto

- [ ] PAIA-Client (`ServicesPAIAService`): Login, Token, Ausleihen, Vormerkungen, Mitteilungen.
- [ ] Modelle `PAIALoanItem`, `PAIANotification`, `PAIAHoldCard`.
- [ ] Sitzung wie iOS nur im Speicher (Token, Patron-ID, Ablauf aus `expires_in`, 401/403 → Sitzung abgelaufen). Siehe [api/paia.md](../api/paia.md).
- [ ] **Neu gegenüber iOS:** optional Passwort speichern (Schalter „Angemeldet bleiben“, Auto-Login beim Start/nach Ablauf), verschlüsselt über Android Keystore, nicht im Backup. Das Token wird nie gespeichert.
- [ ] Verlängern (`renew`), Mitteilungen (404 = leer).
- [ ] Abmelden löscht Token und Passwort (roter, kritischer Button).
- [ ] Screens: Abgemeldet (`PAIASignedOutView`), Ausleihen (`BibliothekScreen`, `PAIALoanItemRow`, Timeline), Mitteilungen, Detailblatt (`BibliothekDetailBlatt`).
- [ ] Badges „Gleich zu“/„Fällig“, Aufbereitung (`BibliothekPresentation`).

## 7b – Login-Sheet und Scan

- [x] Login-Sheet (Formular stabil gegenüber Szenenwechsel/Autofill, bekannter iOS-Fehler). Zustand mit `rememberSaveable`/ViewModel halten.
- [x] Autofill (`contentType` Username/Password). Credential Manager noch nicht angebunden.
- [x] Schalter „Angemeldet bleiben“ mit Erklärtext (Passwort wird verschlüsselt auf dem Gerät gespeichert). Aus: nur Sitzung im Speicher, wie iOS.
- [x] Benutzernummer: Buchstaben erlaubt. Nach Sitzungsablauf bleibt die Nummer vorbelegt, nach manuellem Abmelden ist das Feld leer.
- [x] Scanner der Benutzernummer: **Barcode** auf der Thoska-Rückseite (nicht Text). CameraX + ML Kit **Barcode Scanning** (Code 39/93/128, ITF, EAN-8/13, Codabar). `BenutzernummerFormat` portieren: Leerraum entfernen, 8–14 Zeichen, beginnt mit ASCII-Ziffer, nur ASCII-Ziffern/-Buchstaben, bei Codabar Start/Stopp A–D abtrennen. Anderes still ignorieren.
- [x] Scanner-Zustände: prüft, scannt, gefunden (kurze Pause, dann schließen), verweigert, nicht verfügbar. Taschenlampe. Kein Bild speichern, nichts loggen.
- [x] `CAMERA` als Runtime-Request im Scan-Auslöser (`rememberLauncherForActivityResult`), Fallback manuelle Eingabe. Nach dem Schließen Fokus auf Passwort (nach Treffer) bzw. Benutzernummer.

## 7c – Katalog

- [x] Such-Modell (`LibrarySearchViewModel`): Debounce 700 ms, Generationszähler/Abbruch, Reset bei leerem Feld.
- [x] Parameter und Quellen laut `Suche-API-Analyse.md`: `q`, `title`, `author`, `isbn`, `subject`, `year_from/to`, `type`, `in=local|catalog|content`, `page`, `per_page=20`.
- [x] Fortsetzung „Im Gesamtkatalog suchen“ (`in=catalog`, `page=1`, Deduplizierung per `id`).
- [x] Availability-Batching (80 ms, max. 20 IDs), `GET /v1/library/features` beim Öffnen.
- [x] Cover mit Retry bei `cover_pending` (`Retry-After`, höchstens 30 s, drei Versuche). Eigener `LibraryCoverLoader` mit Sitzungs-Cache statt Coil, weil das Cover-Ergebnis (404 + `cover_pending`) sonst nicht auswertbar ist.
- [x] UI: Katalog-Screen, Ergebniskarte, erweiterte Suche, Medienart-Pills, Titelansicht (`LibraryTitleScreen`, `LibraryTitleViewModel`), Pull-to-Refresh. Einstieg über die Lupe im Bibliothek-Tab; Ausleihen und Vormerkungen öffnen ihren Titel über `catalogPpn`.
- [x] Hinweise: langsamer Katalog (`X-Cache-Status: stale`), Wartehinweis nach 900 ms.
- [ ] Offen für später: Merkliste-Taste (7d) und Teilen (M8) in der Titelansicht, Deeplink `katalog/suche?q=` (M10), Prüfung auf Gerät/Emulator.

## 7d – Merkliste

- [ ] Merkliste lokal (Room), funktioniert abgemeldet. Felder siehe [spec/lokale-daten.md](../spec/lokale-daten.md) (`foundAt`, `changedAt` mitführen, keine Grabsteine bis Wear OS).
- [ ] Offen/Gefunden, Entfernen und „Gefunden“ **widerrufbar** (Snackbar „Widerrufen“), „Alle entfernen“.
- [ ] Verfügbarkeit beim Öffnen frisch laden (Pakete à 20 IDs, parallel), nie speichern, Fehler → „Unbekannt“. Zeile mit Signatur, Standort, „+ N weitere“, Lageplan-Link.
- [ ] Liste und Titel (Tablet: nebeneinander, siehe M9).

## 7e – Ausweis

- [ ] Strichcode **Code 39**, selbst gezeichnet (kein ZXing): Muster-Tabelle aus `Strichcode.code39Muster` übernehmen, Start/Stopp `*`, schmale Lücke zwischen Zeichen, Kleinbuchstaben → groß, unbekannte Zeichen → kein Bild. Ganzzahlige Pixelbreiten, breit:schmal ≈ 2,5:1, Höhe 32 px, Ruhezone ≥ 10 schmale Striche, schmaler Strich höchstens 1,8 px/pt. Ausgabe als Bitmap ohne Filterung (`FilterQuality.None`) oder Canvas mit ganzzahligen Pixeln.
- [ ] Schutz per `BiometricPrompt` bei **jedem** Öffnen (keine zwischengespeicherte Bestätigung). Zulässige Authentifizierung: siehe Entscheidung 11 in [02-entscheidungen.md](../02-entscheidungen.md) (Vorschlag: Biometrie oder Geräte-PIN).
- [ ] Hinweise für „nicht eingerichtet“, „keine Bildschirmsperre gesetzt“, „gesperrt“ (Texte aus `BibliotheksausweisZugang`).
- [ ] Inhalt: Name, Ablaufdatum, Status, Nutzergruppe-Chip. Die Zuordnungstabelle der UB-URIs ist in iOS **leer** (TODO): Chip entfällt, bis die URIs bekannt sind. `email`/`address` nie lesen. Daten nur im Speicher.
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
