# 04 – iOS-Funktionen ohne 1:1-Gegenstück

| iOS-Funktion | Android-Ersatz | Auswirkung |
|---|---|---|
| SF Pro + Dynamic Type | System-Schrift (Roboto), Skalierung über `sp` | Optik leicht anders. Plus Jakarta Sans bleibt für Titel und Zahlen. |
| Siri / AppIntents | App Shortcuts bzw. AppFunctions (M10, „wenn möglich“) | Funktionsumfang kleiner. Keine freie Spracheingabe wie bei Siri. |
| Handoff „Auf iPhone öffnen“ von der Watch | entfällt | Hängt an der Watch (Wear OS ist später). |
| Alternative App-Icons (Hell/Dunkel) | `activity-alias` je Variante | App startet beim Wechsel kurz neu. Themed Icon (Android 13+) deckt „Tinted“ ab. |
| Smart-Stack-Relevanz | kein Gegenstück | Entfällt. |
| Apple Watch (Mensa, Heute Abend, Merkliste, Komplikationen) | Wear OS (später, eigenes Modul) | Nicht im Basisplan. |
| Face ID (iOS erlaubt nie den Gerätecode) | `BiometricPrompt` (Fingerabdruck, Gesicht, ggf. Geräte-PIN) | Verfügbare Methode hängt vom Gerät ab. Ob die Geräte-PIN zugelassen wird, ist offen (Entscheidung 11 in [02-entscheidungen.md](02-entscheidungen.md)). |
| EventKit mit Kalenderauswahl in der App | Insert-Intent, Auswahl übernimmt die Kalender-App | Keine eigene Kalender-Auswahl, keine Berechtigung nötig. |
| App Group (geteilter Speicher App/Widget) | gemeinsamer DataStore | Einfacher, da dieselbe App. |
| Lock-Screen-Widgets / Steuerelemente | Glance-Widgets, ggf. Quick-Settings-Tile | Lockscreen-Widgets nur auf neueren Android-Versionen/Herstellern. |
| NFC-Session-Dialog des Systems | Eigener Hinweis in der UI (Reader Mode hat keinen System-Dialog) | UI muss „Halte die Thoska an dein Gerät“ selbst zeigen. |
| Geräte ohne NFC | Thoska-Tab entfällt (wie iOS) | NFC vorhanden, aber aus: Hinweis mit Weg in die Einstellungen. Deeplink `thoska/auslesen` wird ohne NFC ignoriert. |
| Redaktionelle Hinweise auf Heute | austauschbare leere Quelle | Es gibt serverseitig noch keinen Endpunkt, auch in iOS nicht. Abschnitt entfällt, bis es Daten gibt. |
| Nutzergruppe-Chip auf dem Ausweis | entfällt vorerst | iOS-Tabelle der UB-URIs ist leer (TODO). |
| Merkliste-Abgleich mit der Uhr (Grabsteine, `changedAt`-Konfliktregel) | entfällt bis Wear OS | `foundAt`/`changedAt` werden schon mitgeführt. |

## Ablauf bei weiteren Lücken

Wird beim Portieren eine weitere iOS-Funktion ohne Gegenstück gefunden, wird sie hier ergänzt: Funktion, Ersatzlösung, Auswirkung. Es wird nichts stillschweigend weggelassen.
