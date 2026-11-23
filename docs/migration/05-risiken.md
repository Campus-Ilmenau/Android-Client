# 05 – Risiken und Gegenmaßnahmen

| # | Risiko | Wahrscheinlichkeit | Auswirkung | Gegenmaßnahme |
|---|---|---|---|---|
| 1 | Thoska-Auslesen funktioniert auf Android anders (IsoDep vs. MIFARE-Technologie, Antwortformat, Timeouts) | mittel | hoch | In M6 früh mit echter Karte und echtem Gerät testen. Kommandos und Parsing 1:1 aus iOS übernehmen. Beide Tech-Typen (`IsoDep`, ggf. `MifareClassic`/NfcA) prüfen. |
| 2 | Bibliothek ist mit ca. 10k LOC der größte Block | hoch | mittel | In Teilschritte 7a–7e zerlegen, je Teilschritt Build und Tests. |
| 3 | PAIA-Zugang: unklar, wie iOS Token/Passwort ablegt (kein Keychain im Code gefunden) | mittel | mittel | In M0 klären. Android-Lösung über Keystore, Schalter „Angemeldet bleiben“. |
| 4 | API-Verträge (Mensa, Events) nicht dokumentiert | mittel | mittel | In M0 aus dem iOS-Code extrahieren, Beispielantworten sichern, Modelle gegen echte Antworten prüfen. |
| 5 | Design nur als iOS-Mockup, Maße in pt | niedrig | niedrig | Maße 1:1 als dp übernehmen. Abweichungen (Tippziele 48 dp) dokumentieren. |
| 6 | Start-Animation „Durch die Ziege“ (Maske, 1,1 s) schwer mit Splash-API zu verbinden | mittel | niedrig | Statischer erster Frame über SplashScreen, danach eigene Compose-Animation. Fallback: einfache Animation. |
| 7 | Glance-Widgets sind in Layout und Interaktion eingeschränkt | mittel | niedrig | Widgets vereinfachen, nur Mensa heute und Heute Abend. |
| 8 | Kamera-Scan der Benutzernummer (Text/Barcode) unterschiedlich zuverlässig je Gerät | mittel | mittel | ML Kit mit manueller Eingabe als Fallback. Tests aus `BenutzernummerScannerTests` portieren. |
| 9 | Datenschutz/Play-Policy (Kamera, Passwort-Speicherung) | niedrig | mittel | Datenschutz-Angaben und Berechtigungsbegründung in M11 prüfen. Passwort nur verschlüsselt und nur auf ausdrücklichen Wunsch speichern. |
| 10 | Veraltete Basisversionen im Gerüst führen zu Versionskonflikten | niedrig | niedrig | In M1 alle Versionen zusammen aktualisieren und mit `assembleDebug` prüfen. |
