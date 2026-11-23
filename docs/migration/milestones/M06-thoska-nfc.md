# M6 – Thoska (NFC)

**Status:** offen · **Abhängig von:** M2 · **Gate:** `compileDebugKotlin`, Test mit echter Karte auf einem Gerät

## Ziel

Das Guthaben der Thoska (MIFARE DESFire) wird per NFC ausgelesen, mit Verlauf und Kartennamen.

## Aufgaben

### NFC
- [ ] Manifest: `android.permission.NFC`, `uses-feature android.hardware.nfc` mit `required="false"`.
- [ ] Reader über `NfcAdapter.enableReaderMode` mit `IsoDep` (Technologien `NfcA`/`IsoDep`), Lebenszyklus an den Screen binden.
- [ ] DESFire-Kommandos als Pseudo-APDU (CLA `0x90`) unverändert übernehmen:
  - Select Application `0x5A`, App-ID `5F 84 15`
  - Get Value `0x6C`, Datei `0x01`
  - Get File Settings `0xF5`
- [ ] Parsing: Guthaben = Little-Endian Int32 ÷ 1000, letzte Buchung aus File-Settings (Byte 12/13, ÷ 1000), Karten-ID als Hex der UID.
- [ ] Fehlerfälle: kein NFC, NFC aus (Link zu den Einstellungen), falscher Kartentyp, Abbruch, Verbindungsverlust.
- [ ] UI-Hinweis „Halte deine Thoska an dein Gerät“ (kein System-Dialog wie auf iOS).

### Daten
- [ ] Room: `ThoskaBalanceRecord`, `ThoskaCardName` (Pendant zu SwiftData).
- [ ] Kartennamen-Logik (`ThoskaCardNaming`), Formatierung (`ThoskaFormatting`).
- [ ] Verlauf-Model (`ThoskaHistoryModel`).

### UI
- [ ] `ThoskaScreen` mit animierter Karte (Flip per Wischgeste), Guthaben in Plus Jakarta Sans.
- [ ] Verlauf (`ThoskaHistoryView`) mit Chart (Entscheidung Canvas vs. Vico).
- [ ] Benennen der Karte.
- [ ] **Gerät ohne NFC-Hardware: Thoska-Tab entfällt** (wie iOS `NFCVerfuegbarkeit`), der Deeplink `thoska/auslesen` wird dort ignoriert. NFC vorhanden, aber ausgeschaltet: Hinweis mit Weg in die Einstellungen.
- [ ] Verlauf-Auswertung portieren (`ThoskaHistorySummary`): Zeiträume 14 Tage und 3 Monate, Aufladung bei Guthaben > Vorgänger + 0,005, Ausgaben = Summe der Rückgänge ohne Aufladungen, Balken je Auslesung (14 Tage) bzw. je Kalenderwoche mit letztem Stand (3 Monate). Beträge als `Long` in Tausendstel speichern.
- [ ] Kartenname: leerer Name oder Name gleich Karten-ID gilt als „ohne Namen“, Fallback „Karte …C3D4“ (letzte 4 Zeichen, groß), leerer Eingabe setzt zurück.
- [ ] Previews, Deeplink „Thoska auslesen“ (startet das Auslesen sofort).

## Abnahme

- Auslesen auf echtem Gerät mit echter Thoska liefert dasselbe Guthaben wie die iOS-App.
- Verlauf wird gespeichert, Kartenname bleibt erhalten.
- App bricht auf Geräten ohne NFC nicht ab.

## Quellen (iOS)

`ServicesThoskaCardReader.swift`, `Campus Ilmenau/Thoska/*`, `ViewsThoskaThoskaView.swift`, `ModelsThoskaBalanceRecord.swift`, `ModelsThoskaCardName.swift`, `Shared/NFCVerfuegbarkeit.swift`
