# 05 – Risiken und Gegenmaßnahmen

| # | Risiko | Wahrscheinlichkeit | Auswirkung | Gegenmaßnahme |
|---|---|---|---|---|
| 1 | Thoska-Auslesen funktioniert auf Android anders (IsoDep vs. MIFARE-Technologie, Antwortformat, Timeouts) | mittel | hoch | In M6 früh mit echter Karte und echtem Gerät testen. Kommandos und Parsing 1:1 aus iOS übernehmen. Beide Tech-Typen (`IsoDep`, ggf. `MifareClassic`/NfcA) prüfen. |
| 2 | Bibliothek ist mit ca. 10k LOC der größte Block | hoch | mittel | In Teilschritte 7a–7e zerlegen, je Teilschritt Build und Tests. |
| 3 | Optional gespeichertes PAIA-Passwort ist neu (iOS speichert nichts, M0 geklärt) und sicherheitskritisch | mittel | hoch | Nur auf ausdrückliche Zustimmung, verschlüsselt über Android Keystore, nicht im Backup, nie loggen, beim Abmelden und in „Gespeicherte Daten löschen“ entfernen. Sicherheitsreview in M11. |
| 4 | API-Verträge (Mensa, Events) nicht dokumentiert | mittel | mittel | In M0 aus dem iOS-Code extrahieren, Beispielantworten sichern, Modelle gegen echte Antworten prüfen. |
| 5 | Design nur als iOS-Mockup, Maße in pt | niedrig | niedrig | Maße 1:1 als dp übernehmen. Abweichungen (Tippziele 48 dp) dokumentieren. |
| 6 | Start-Animation „Durch die Ziege“ (Maske, 1,1 s) schwer mit Splash-API zu verbinden | mittel | niedrig | Statischer erster Frame über SplashScreen, danach eigene Compose-Animation. Fallback: einfache Animation. |
| 7 | Glance-Widgets sind in Layout und Interaktion eingeschränkt | mittel | niedrig | Widgets vereinfachen, nur Mensa heute und Heute Abend. |
| 8 | Barcode-Scan der Benutzernummer ist je Gerät unterschiedlich zuverlässig (Codabar, kleine Codes, Licht) | mittel | mittel | ML Kit Barcode Scanning mit Taschenlampe und manueller Eingabe als Fallback. Tests aus `BenutzernummerScannerTests` portieren. Mit echter Thoska auf mehreren Geräten prüfen. |
| 11 | Mensa-/Events-/Library-Antworten weichen von den aus dem iOS-Code abgeleiteten Verträgen ab (keine echten Beispielantworten gesichert) | mittel | mittel | Zu Beginn von M3, M5 und M7 echte Antworten der Dev-API sichern und DTOs tolerant (Standardwerte, Auffangwerte) bauen. |
| 12 | Zeitzonen-Fehler (Berliner Tage vs. Gerätezeit, UTC-Fenster der Events-API) | mittel | mittel | Einheitlich `ZoneId.of("Europe/Berlin")` und injizierbare `Clock`, Tests für Mitternacht und Zeitumstellung. |
| 9 | Datenschutz/Play-Policy (Kamera, Passwort-Speicherung) | niedrig | mittel | Datenschutz-Angaben und Berechtigungsbegründung in M11 prüfen. Passwort nur verschlüsselt und nur auf ausdrücklichen Wunsch speichern. |
| 10 | Veraltete Basisversionen im Gerüst führen zu Versionskonflikten | niedrig | niedrig | In M1 alle Versionen zusammen aktualisieren und mit `assembleDebug` prüfen. |
