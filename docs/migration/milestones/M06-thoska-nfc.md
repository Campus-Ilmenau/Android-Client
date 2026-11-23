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
- [ ] Gerät ohne NFC: Erklärung statt Scan-Button.
- [ ] Previews, Deeplink „Thoska auslesen“.

## Abnahme

- Auslesen auf echtem Gerät mit echter Thoska liefert dasselbe Guthaben wie die iOS-App.
- Verlauf wird gespeichert, Kartenname bleibt erhalten.
- App bricht auf Geräten ohne NFC nicht ab.

## Quellen (iOS)

`ServicesThoskaCardReader.swift`, `Campus Ilmenau/Thoska/*`, `ViewsThoskaThoskaView.swift`, `ModelsThoskaBalanceRecord.swift`, `ModelsThoskaCardName.swift`, `Shared/NFCVerfuegbarkeit.swift`
