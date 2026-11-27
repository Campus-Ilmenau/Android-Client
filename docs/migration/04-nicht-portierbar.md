# 04 – iOS-Funktionen ohne 1:1-Gegenstück

| iOS-Funktion | Android-Ersatz | Auswirkung |
|---|---|---|
| SF Pro + Dynamic Type | System-Schrift (Roboto), Skalierung über `sp` | Optik leicht anders. Plus Jakarta Sans bleibt für Titel und Zahlen. |
| Siri / AppIntents | App Shortcuts (M10a, umgesetzt); AppFunctions für Assistenten offen | Fest verdrahtete Ziele statt gesprochener Fragen: Die Shortcuts öffnen die Stelle in der App. Keine Antwortkarten und keine freie Spracheingabe wie bei Siri. „Abendessen“ (Cafeteria) und „Hat die Mensa offen?“ haben keinen eigenen Shortcut, weil der Link die Tageszeit nicht kennt. |
| Handoff „Auf iPhone öffnen“ von der Watch | entfällt | Hängt an der Watch (Wear OS ist später). |
| Alternative App-Icons (Hell/Dunkel) | `activity-alias` je Variante | App startet beim Wechsel kurz neu. Themed Icon (Android 13+) deckt „Tinted“ ab. |
| Smart-Stack-Relevanz | kein Gegenstück | Entfällt. |
| Widget-Zeitleiste (ein Eintrag je Phasenwechsel, Neuladen um 6:00) | Glance-Widgets, `WidgetRefreshWorker` alle 30 Minuten | Das Widget zeichnet die Jetzt-Karte beim Laden neu statt eine Zeitleiste vorzuberechnen; zwischen zwei Aktualisierungen kann es bis zu 30 Minuten hinter einem Phasenwechsel (z. B. 14:00) zurückliegen. |
| Widgets auf dem Sperrbildschirm (Zeile, Rechteck, Kreis) | entfällt | Android kennt Sperrbildschirm-Widgets nur auf neuen Geräten/Herstellern; die Home-Größen (klein, mittel, groß) sind umgesetzt. |
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
