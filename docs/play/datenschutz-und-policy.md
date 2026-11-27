# Play Console: Datensicherheit, Berechtigungen, Policy

Grundlage für das Formular „Datensicherheit“ und die Policy-Erklärungen. Der Stand folgt dem Code (siehe Spalte „Wo“); bei
jeder Änderung an Berechtigungen, Netzwerkzielen oder gespeicherten Daten wird diese Datei mit angepasst.

## Berechtigungen (`AndroidManifest.xml`)

| Berechtigung | Wofür | Runtime-Anfrage | Wo |
|---|---|---|---|
| `INTERNET` | API der Mensa und Events, PAIA-Server der UB, Cover und Plakate | nicht nötig (normal) | überall |
| `NFC` | Thoska auslesen | nicht nötig (normal); `uses-feature` ist optional, ohne NFC entfällt der Thoska-Tab | `ThoskaReader` |
| `CAMERA` | Benutzernummer von der Thoska scannen (Barcode) | ja, beim Öffnen des Scanners (`BenutzernummerScanner`, `RequestPermission`) | `BenutzernummerScanner` |

Keine weiteren Berechtigungen: kein Standort, keine Kontakte, kein Speicherzugriff (Bilder zum Teilen liegen im
Zwischenspeicher der App und gehen per `FileProvider` raus), kein Kalenderzugriff (Eintrag über den Insert-Intent).

## Komponenten und `exported`

| Komponente | `exported` | Grund |
|---|---|---|
| `MainActivity` | ja | Deeplinks `campusilmenau://…`. Eingehende Links werden nur über `DeepLink.parse` ausgewertet, unbekannte werden ignoriert, es gibt keinen Intent-Anhang, der weitergereicht wird. |
| Launcher-Aliase `IconStandard`, `IconDunkel` | ja | Launcher-Einträge (Pflicht). |
| Widget-Receiver | ja | Pflicht für App-Widgets; sie verarbeiten nur `APPWIDGET_UPDATE`. |
| `FileProvider` | nein | Bilder zum Teilen, nur `cache/share/`, Zugriff über Leseerlaubnis auf die Adresse. |

## Netzwerk

Nur HTTPS (`network_security_config.xml`, `cleartextTrafficPermitted=false`). Ziele: `api.campus-ilmenau.de` (Debug:
`dev-api.campus-ilmenau.de`), `paia.gbv.de` (Bibliothekskonto), Bild- und Link-Ziele der Quellen (Plakate, Cover, Katalog).
Das HTTP-Logging gibt es nur im Debug-Build und nur Kopfzeilen (`Authorization` geschwärzt).

## Gespeicherte Daten

| Daten | Ort | Verschlüsselt | In der Cloud-Sicherung |
|---|---|---|---|
| Einstellungen (Mensa, Filter, Preisgruppe, Darstellung), Event-Favoriten (nur IDs), ausgeblendete Hinweise | DataStore `settings` | nein (keine personenbezogenen Daten) | ja |
| Thoska-Auslesungen, Kartennamen, Merkliste | Room `campus.db` | nein | **nein** (ausgenommen) |
| PAIA-Passwort („Angemeldet bleiben“, optional) | `paia_credentials` (Keystore-Schlüssel, AES-GCM) | ja | **nein** (ausgenommen) |
| Sprache (vor Android 13) | SharedPreferences `sprache` | nein | ja |
| PAIA-Sitzung (Token) | nur im Arbeitsspeicher | – | – |

„Alle gespeicherten Daten löschen“ (Mehr → Gespeicherte Daten) entfernt Datenbank, Einstellungen, Sitzung und das gespeicherte
Passwort.

## Formular „Datensicherheit“ (Vorschlag)

- **Daten werden erhoben:** Ja, die Anmeldung beim Bibliothekskonto (PAIA) sendet Benutzernummer und Passwort an den Server der
  UB; sonst sendet die App keine personenbezogenen Daten an eigene Server.
- **Weitergabe an Dritte:** Nein.
- **Verschlüsselung bei der Übertragung:** Ja (HTTPS).
- **Daten löschen:** Ja, in der App unter „Gespeicherte Daten“; Abmelden löscht Sitzung und Passwort.
- **Kategorien:** Benutzer-IDs/Zugangsdaten (nur Bibliothekskonto, nur auf Wunsch gespeichert); Kamerabilder werden nur zum
  Erkennen des Barcodes ausgewertet, nicht gespeichert und nicht übertragen.
- **Keine Werbung, keine Analyse-SDKs, kein Tracking.** (Crash-Reporting ist noch nicht eingebunden, siehe M11; kommt es, ist
  das Formular um „Absturzprotokolle“ zu ergänzen.)

## Policy-Hinweise

- **Konto löschen:** Die App erstellt keine Konten; das Bibliothekskonto gehört der UB.
- **Kamera:** nur beim Öffnen des Scanners; ohne Kamera (oder bei Ablehnung) bleibt die Eingabe von Hand.
- **Biometrie:** nur für den Bibliotheksausweis, ohne Speicherung biometrischer Daten (System-`BiometricPrompt`).
- **Zielgruppe:** Studierende und Beschäftigte der TU Ilmenau, keine Kinder-App.
- **Datenschutzerklärung:** `https://campus-ilmenau.de/datenschutz/` (in der App unter Mehr → Allgemein).
