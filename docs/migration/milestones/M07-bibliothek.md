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

- [x] Merkliste lokal (Room, Datenbank-Version 2 mit Migration), funktioniert abgemeldet. Felder siehe [spec/lokale-daten.md](../spec/lokale-daten.md) (`foundAt`, `changedAt` mitgeführt, keine Grabsteine bis Wear OS). Lesezeichen in der Titelansicht (Toast „Anzeigen“), Einstieg über das Lesezeichen im Bibliothek-Tab.
- [x] Offen/Gefunden, Entfernen und „Gefunden“ **widerrufbar** (Toast „Widerrufen“), „Merkliste leeren“ mit Rückfrage. Wischen (rechts: entfernen, links: gefunden) plus Menü beim langen Drücken und TalkBack-Aktionen. Widerruf setzt den Eintrag mit seinem `gemerktAm` zurück, die Stelle ergibt sich aus der Sortierung (keine eigene Position nötig).
- [x] Verfügbarkeit beim Öffnen und Aktualisieren frisch laden (Pakete à 20 IDs, parallel), nie speichern, Fehler → „Unbekannt“. Zeile mit Signatur, Standort, „+ N weitere“, Lageplan-Link.
- [ ] Liste und Titel nebeneinander (Tablet) folgt in M9.

## 7e – Ausweis

- [x] Strichcode **Code 39**, selbst gezeichnet (kein ZXing): Muster-Tabelle aus `Strichcode.code39Muster` übernehmen, Start/Stopp `*`, schmale Lücke zwischen Zeichen, Kleinbuchstaben → groß, unbekannte Zeichen → kein Bild. Ganzzahlige Pixelbreiten, breit:schmal ≈ 2,5:1, Höhe 32 px, Ruhezone ≥ 10 schmale Striche, schmaler Strich höchstens 1,8 px/pt. Ausgabe als Bitmap ohne Filterung (`FilterQuality.None`) oder Canvas mit ganzzahligen Pixeln.
- [x] Schutz per `BiometricPrompt` bei **jedem** Öffnen (neuer Prompt ohne `CryptoObject`, keine zwischengespeicherte Bestätigung). Entscheidung 11: Biometrie oder Geräte-PIN. `MainActivity` ist dazu eine `FragmentActivity`.
- [x] Hinweise für „nicht eingerichtet“, „nicht verfügbar“, „gesperrt“ (`AusweisZugang`, mit Tests auf die Fehlercodes).
- [x] Inhalt: Name, Ablaufdatum, Status, Nutzergruppe-Chip (entfällt, bis die URIs der UB bekannt sind). `email`/`address` werden nie gelesen. Daten nur im Speicher; schließt nach 2 Minuten und sobald die App nicht mehr sichtbar ist.
- [x] Helligkeit auf Maximum, Bildschirm bleibt an, `FLAG_SECURE` (keine Screenshots/Vorschaubilder).
- [ ] Auf Gerät prüfen: BiometricPrompt mit `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` auf Android 8–10, Scan des Strichcodes an einem echten Lesestoßen.

## Tests (aus iOS portieren)

- [ ] `BibliothekTests`, `LibraryCatalogTests`, `LibrarySearchModelTests`, `LibraryTitleTests`, `BibMerklisteTests`, `BibliotheksausweisTests`, `BenutzernummerScannerTests`

## Abnahme

- Login (mit und ohne „Angemeldet bleiben“), Ausleihen, Mitteilungen, Abmelden.
- Suche verhält sich wie iOS (Debounce, Gesamtkatalog-Fortsetzung, Deduplizierung).
- Ausweis erscheint erst nach Biometrie/PIN.
- Passwort wird nie im Klartext gespeichert oder geloggt.

## Quellen (iOS)

`Campus Ilmenau/Bibliothek/*` (inkl. `Katalog/`, `Merkliste/`, `Ausweis/`, `Login/`), `ViewsBibliothek*.swift`, `ServicesPAIAService.swift`, `ModelsPAIA*.swift`, `Suche-API-Analyse.md`
