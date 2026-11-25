# M6 – Thoska (NFC)

**Status:** **umgesetzt, Test mit echter Karte offen** (2026-11-25) · **Abhängig von:** M2 · **Gate:** `compileDebugKotlin` bestanden; Unit-Tests und Lint grün; **Abnahme mit echter Thoska auf einem NFC-Gerät steht noch aus** (der Emulator hat kein NFC)

## Ziel

Das Guthaben der Thoska (MIFARE DESFire) wird per NFC ausgelesen, mit Verlauf und Kartennamen.

## Umgesetzt

### NFC (`data/nfc`)
- [x] Manifest: `android.permission.NFC`, `uses-feature android.hardware.nfc` mit `required="false"`. Ohne NFC-Hardware bleibt die App installierbar, der Thoska-Tab entfällt (`DeviceCapabilities`, schon aus M2).
- [x] `NfcThoskaReader`: `NfcAdapter.enableReaderMode` (`NfcA`/`NfcB`, ohne NDEF-Prüfung) mit `IsoDep`, 3 s Zeitlimit. Der Lesemodus gilt, solange gewartet wird, und endet auch bei Abbruch der Coroutine. Die Ansicht beendet das Warten in `ON_STOP`.
- [x] `DesfireThoska`: die drei Pseudo-APDUs unverändert von iOS (`90 5A … 5F 84 15`, `90 6C … 01`, `90 F5 … 01`), Guthaben = Little-Endian-Int32 in Tausendstel, letzte Buchung aus Byte 12/13, Karten-ID = UID in kleinem Hex. `CardTransport` trennt die Karte von Android, damit sich alles ohne Gerät testen lässt.
- [x] Fehlerfälle als `ThoskaError`: kein NFC, NFC aus (Button zu den NFC-Einstellungen), Kartentyp nicht unterstützt, Verbindung abgebrochen (Karte zu früh weg), Fehlerstatus der Karte.
- [x] Statt des System-Dialogs wie auf iOS zeigt die Taste „Halte deine Thoska an dein Gerät …“ mit Ladekreis.

### Daten
- [x] Room (`CampusDatabase`, erste Tabellen): `thoska_balance` (Beträge als `Long` in Tausendstel), `thoska_card_name`. Schema in `app/schemas`.
- [x] Die Datenbank ist von der Cloud-Sicherung ausgenommen (`data_extraction_rules.xml`, `backup_rules.xml`), damit „bleibt nur auf deinem Gerät“ stimmt; die direkte Übertragung auf ein neues Gerät bleibt möglich.
- [x] `ThoskaCardNaming` (leer oder gleich Karten-ID = ohne Namen, Ersatzname „Karte …C3D4“, leere Eingabe setzt zurück), `ThoskaFormatting`, `ThoskaHistorySummary` (14 Tage / 3 Monate, Aufladung bei Guthaben > Vorgänger + 5 Tausendstel auch gegenüber einem Vorgänger vor dem Zeitraum, Ausgaben ohne Aufladungen, Balken je Auslesung bzw. je Berliner Kalenderwoche mit letztem Stand).

### UI
- [x] `ThoskaScreen`: Karte mit Verlauf Blau → Nachtblau und grünem Band (Betrag schrumpft, statt aufs Band zu laufen), „Zuletzt ausgelesen“, „Letzte Buchung“, Karte benennen, „Guthaben aktualisiert“, Fuß mit Hinweis und Taste.
- [x] `ThoskaHistoryScreen`: Kartenwahl und Umbenennen im Menü, Zeitraum, Ausgaben, Balkendiagramm (Canvas, grün bei Aufladung, mindestens 6 Plätze, Beschreibung für TalkBack), Liste.
- [x] Deeplink `thoska/auslesen` startet das Auslesen, sobald die Ansicht sichtbar ist (bei Kaltstart wartet der Wunsch); auf Geräten ohne NFC wird er ignoriert.

### Tests
`DesfireThoskaTest` (Befehlsbytes, Parsing, Fehlerstatus, Ablauf mit Fake-Karte), `ThoskaDomainTest` (Verlauf, Namen, Formatierung), `ThoskaViewModelTest`, `ThoskaHistoryViewModelTest`.

## Abweichungen von iOS (bewusst)

- **Letzte Auslesung bleibt sichtbar:** iOS zeigt nach dem Start „Noch nicht ausgelesen“, Android zeigt die zuletzt gespeicherte Auslesung (aus dem Verlauf), die Bestätigung „Guthaben aktualisiert“ erst nach einem Auslesen.
- **Statusbytes:** iOS wertet die Statusbytes der Karte nicht aus. `DesfireThoska.checkStatus` bricht nur bei klaren Fehlern ab (`91 xx` mit `xx` ≠ `00`, ISO-Fehler `67`–`6F`), alles andere gilt wie bei iOS als Erfolg.
- **Vorzeichen:** Die Wertdatei der Karte ist vorzeichenbehaftet, Android liest den Int32 mit Vorzeichen (iOS liest ihn ohne).

## Noch zu tun (vor „abgeschlossen“)

- **Abnahme mit echter Thoska:** Guthaben und letzte Buchung mit der iOS-App vergleichen, Verhalten bei zu früh weggenommener Karte, Karte ohne ISO-DEP, NFC aus. Auf dem Emulator ist der Tab nicht sichtbar, die Oberfläche ist nur über Previews (Light, Dark, leer, NFC aus, Fehler, Auslesen) geprüft.

## Quellen (iOS)

`ServicesThoskaCardReader.swift`, `Campus Ilmenau/Thoska/*`, `ViewsThoskaThoskaView.swift`, `ModelsThoskaBalanceRecord.swift`, `ModelsThoskaCardName.swift`, `Shared/NFCVerfuegbarkeit.swift`
