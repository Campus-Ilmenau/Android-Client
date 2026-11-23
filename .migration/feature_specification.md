# Feature-Spezifikation Campus Ilmenau (iOS → Android)

Stand: 2026-11-23 · Quelle: iOS-Projekt `Campus Ilmenau` (iOS-Projekt, Nachbarordner dieses Repos)

Diese Datei beschreibt, **was** die iOS-App tut und was Android davon übernimmt. Details zu Schnittstellen stehen in [`docs/migration/api/`](../docs/migration/api/README.md), lokale Daten in [`docs/migration/spec/lokale-daten.md`](../docs/migration/spec/lokale-daten.md), Deeplinks in [`docs/migration/spec/deeplinks.md`](../docs/migration/spec/deeplinks.md).

**Lesestand:** Für M0 wurden die Fachlogik, Services, Modelle, Router und Speicher vollständig gelesen. Die Ansichten (SwiftUI-Views, Mockups `*.dc.html`) werden je Meilenstein direkt vor der Umsetzung gelesen („eine Ansicht pro Schritt“).

## 1. Produkt

Studierenden-App der TU Ilmenau. Fünf Tabs plus „Mehr“. Kernfunktionen: Mensa-Speiseplan, Thoska-Guthaben per NFC, Bibliothekskonto und Katalog, Veranstaltungen, Tageszeit-abhängige Startseite. Sprache der Oberfläche: Deutsch (Siri-Englisch-Entwurf existiert, Sprachbereich in „Mehr“ vorhanden).

Durchgängige Regel: **Alle Tage und Uhrzeiten laufen in der Zeitzone `Europe/Berlin`**, nicht in der Gerätezeit (`EventKalender`, `HeuteUhr`). Auf Android immer `ZoneId.of("Europe/Berlin")` und eine injizierbare Uhr (`Clock`).

## 2. Navigation

Tabs (`AppTab`): **Heute, Mensa, Thoska, Bibliothek, Events**. „Mehr“ ist kein Tab, sondern der Personen-Button auf Heute (auf dem iPad bei sichtbarer Seitenleiste ein eigener Eintrag).

- Tabs werden erst beim ersten Besuch aufgebaut und behalten danach Zustand (`besuchteTabs`).
- Die Tab-Leiste verschwindet, solange die Tastatur sichtbar ist.
- Der Thoska-Tab entfällt auf Geräten ohne NFC-Hardware (`NFCVerfuegbarkeit.vorhanden`). Der Deeplink `thoska/auslesen` wird dort ignoriert.
- Ziele innerhalb der Tabs (iPad-Seitenleiste, auf Android Rail/Drawer ab Medium): Bibliothek → Ausleihen, Katalog, Merkliste, Ausweis. Events → Alle, Heute Abend.
- Ein **Router** (`AppRouter`) hält die Wünsche an Tabs („zeig diese Mensa“, „öffne Event X“). Auf Android entspricht das einem Navigations-Zustand pro Tab plus einem Deeplink-Einstieg (siehe [deeplinks.md](../docs/migration/spec/deeplinks.md)).
- Ziele von außen warten, bis die Start-Animation vorbei ist (`wartetAufStart`), und gehen sonst nicht verloren.
- Zustand überlebt den Wechsel der Hülle (iPad ↔ schmal): Events-Modell, Katalogsuche, Mensa-Tagesindex und Mahlzeit liegen im Router. Auf Android: ViewModels auf Activity-/Graph-Ebene.

## 3. Features

### 3.1 Heute

Startseite **ohne Live-Daten** (kein Thoska-Guthaben, keine Anzahl Ausleihen). Aufbau von oben:

1. Datum und Uhrzeit, Begrüßung, rechts Personen-Button → Mehr.
2. Hinweise (max. 2).
3. Jetzt-Karte mit Mensa-Wechsel.
4. „Als Nächstes“ (`HeuteAlsNaechstesView`) und „Heute Abend“-Karte.

**Tagesphasen** (Minuten seit Mitternacht, Berlin): morgen < 10:30, mittag < 14:00, nachmittag < 16:30, abend < 19:30, nacht darüber. Überschrift der Jetzt-Karte: morgen „Heute“, mittag/abend/nacht „Jetzt“, nachmittag „Gleich“.
**Begrüßung:** < 11:00 „Guten Morgen“, < 14:00 „Mahlzeit!“, < 18:00 „Hallo!“, sonst „Guten Abend“.
**Nächster Wechsel:** kleinster der Zeitpunkte 10:30, 11:00, 14:00, 16:30, 18:00, 19:30, sonst Mitternacht des Folgetags (`Tagesphase.naechsterWechsel`). Die Seite aktualisiert sich dort.

Inhalt der Jetzt-Karte je Phase: siehe `Uebergabe.md` (Tabelle) und [M4](../docs/migration/milestones/M04-heute.md). Ab 19:30 läuft ein Club-Event (Kopf Nachtblau mit Sonne), „Mensa ändern“ entfällt.

**Mensa auf der Startseite:** eigene Auswahl `heuteMensa`, unabhängig vom Mensa-Tab, Vorgabe Mensa Ehrenberg. Der Mensa-Filter gilt auch hier.

**Hinweise:** Struktur `Hinweis(id, kategorie, zeitangabe, titel, text, symbol, farbe blau|grün, von, bis, prioritaet)`, höhere Priorität zuerst, nur im Zeitfenster `[von, bis)`. ✕ blendet dauerhaft aus (`heuteHinweiseAusgeblendet`). **Es gibt noch keinen Endpunkt**, `LeereHinweiseQuelle` liefert nichts, der Abschnitt entfällt. Android übernimmt dasselbe (leere Quelle, austauschbar). Laut `CLAUDE.md`: bei neuen Daten (Hinweise, Club-Beacons) zuerst nachfragen.

**Robustheit:** Quellen laufen mit Zeitlimit (`heuteMitZeitlimit`), Fehler/Zeitablauf ergeben `nil`, die Seite hängt nie. „Nur Speiseplan und Öffnungszeiten scheitern, Events und Hinweise kommen.“

### 3.2 Mensa

Vier feste Mensen (erscheinen immer, auch ohne Gerichte), Zuordnung über `canteen.name` der API (case-insensitiv, getrimmt):

| Venue (rawValue) | API-Name | Anzeige | Kurzname |
|---|---|---|---|
| `mensaEhrenberg` | Mensa Ehrenberg | Mensa Ehrenberg | Ehrenberg |
| `cafeteriaEhrenberg` | Cafeteria Mensa Ehrenberg | Cafeteria Ehrenberg | Cafeteria Ehrenberg |
| `nanoteria` | Cafeteria NANOteria | Cafeteria Nanoteria | Nanoteria |
| `roentgenbau` | Cafeteria Röntgenbau | Cafeteria Röntgenbau | Röntgenbau |

Die rawValues sind gespeichert und in Deeplinks enthalten, **nicht umbenennen**. Nur die Cafeteria Ehrenberg hat Mittag **und** Abendessen (`hasMealtimes`). Ein Gericht ist Abendessen, wenn `meal_group` „abend“ enthält (case-insensitiv).

- Speiseplan bis 3 Wochen im Voraus, Tagesauswahl, Wischen zwischen den Mensen.
- **Filter** (`MensaFilter`, UND-verknüpft): Ernährung `alle|vegetarisch|vegan` („vegetarisch“ lässt vegane durch), „Nur Bio“, ausgeschlossene Allergene. Feste Liste mit 26 Allergennamen der API (siehe `MensaFilter.knownAllergens`) plus alles, was geladene Gerichte nennen. Filter wird gespeichert und gilt für Mensa-Tab, Heute und Widget.
- **Preisgruppe** (`studierende|bedienstete|gaeste`, Vorgabe Studierende) bestimmt den großen Preis. Preisformat „2,90 €“ (de_DE), fehlender Preis „–“. Währung: 3 Buchstaben → Code, sonst EUR.
- **Chips** in Liste: nur Vegan, Vegetarisch, Bio, Regional; ein veganes Gericht zeigt nur „Vegan“; „vegetarisch“ erscheint als Label auch bei veganen. Im Detail zusätzlich weitere Kategorien der API (z. B. „Kinderessen“) als neutrale Chips. Zusatz: Regeln stehen in `widgetChips` (gemeinsam mit Widget).
- **Detail:** Nährwerte (CO₂ pro Portion), Komponenten mit Allergenen und Zusatzstoffen. Zuordnung der Codes einer Komponente über **Namen** (Allergene aus fester Liste + Gericht; Zusatzstoffe aus fester Liste der API (17 Namen) + Gericht). Allergene kommen teils doppelt von der API → deduplizieren, Reihenfolge behalten. Text „Allergene: … · Zusatzstoffe: …“ bzw. „Keine weiteren Angaben“.
- **Favoriten** (`MensaFavoriteStore`): lokal, ein Gericht zählt über seinen **normalisierten Namen** (getrimmt, lowercase), weil die API-ID pro Tag wechselt.
- **Gericht-IDs gelten nur einen Tag** und lassen sich nicht einzeln abfragen → Deeplink `mensa/gericht/<id>?tag=yyyy-MM-dd`.
- **Öffnungszeiten** (`MensaOpeningHours`): Wochenzeilen mit zusammengefassten gleichen Zeiten („Montag – Donnerstag“, „Samstag, Sonntag“ → „geschlossen“), heutige Zeit „11:00–14:00 Uhr“ bzw. „heute geschlossen“, Status `open | closingSoon (<30 min) | closed`. Abendessen-Text der Cafeteria Ehrenberg: nur Ende der Öffnungszeit (Montag), ggf. „, Freitag bis …“.
- Siri-Tipp im Speiseplan ab dem 3. Öffnen (siehe 3.8).
- Ladetexte zufällig und verspielt, `GoatLoader`.

### 3.3 Thoska (NFC)

Guthaben der Thoska/Mensa-Chipkarte (MIFARE DESFire). Ablauf, Kommandos und Parsing: siehe [M6](../docs/migration/milestones/M06-thoska-nfc.md).

- Verlauf lokal gespeichert (`ThoskaBalanceRecord`: cardId, balance, lastTransaction, scannedAt), mehrere Karten über `cardId`.
- Kartennamen (`ThoskaCardName`, `cardId` eindeutig). Ein Name, der gleich der Karten-ID ist oder leer, gilt als „ohne Namen“ (Altlast). Fallback-Anzeige „Karte …C3D4“ (letzte 4 Zeichen der ID, groß). Leerer Name setzt zurück. Name bleibt nur auf dem Gerät.
- **Verlauf** (`ThoskaHistorySummary`): Zeiträume 14 Tage und 3 Monate. Aufladung, wenn Guthaben > Vorgänger + 0,005 (auch wenn der Vorgänger vor dem Zeitraum liegt). Ausgaben = Summe der Rückgänge aufeinanderfolgender Auslesungen im Zeitraum (ohne Aufladungen). Balken: bei 14 Tagen einer je Auslesung, bei 3 Monaten einer je Kalenderwoche mit dem letzten Stand der Woche (Aufladung, wenn eine Auslesung der Woche eine war).
- Animierte Karte, die sich per Wischgeste umdreht und das Guthaben zeigt. Guthaben in Plus Jakarta Sans.
- Deeplink `thoska/auslesen` startet das Auslesen sofort.
- Geräte ohne NFC: Tab entfällt.

### 3.4 Bibliothek

**Konto (PAIA)**: Login, Ausleihen, Vormerkungen, Bestellungen, Mitteilungen, Verlängern. Schnittstelle: [paia.md](../docs/migration/api/paia.md).
- Zugang wird **nicht gespeichert** (iOS: nur Token im Speicher, „nach einem Neustart meldest du dich neu an“). Android ergänzt **optional** gespeichertes Passwort (Entscheidung 5).
- Session endet bei 401/403 oder lokal bei Ablauf von `expires_in` → „Sitzung abgelaufen, bitte erneut anmelden“. Das Login-Sheet zeigt dann einen eigenen Titel/Hinweis. Nach Ablauf bleibt die Benutzernummer im Speicher vorbelegt, nach manuellem Abmelden bleibt das Feld leer.
- Statusbadges je Eintrag (`PAIAItemStatus`: 0 kein Status, 1 vorgemerkt, 2 bestellt, 3 ausgeliehen, 4 bereitgestellt, 5 abgelehnt). Badges „Gleich zu“/„Fällig“ in Warn-Farbe.
- Verlängern: nur wenn `item` (URI) vorhanden, sonst „Dieses Medium kann nicht verlängert werden“. Ergebnis aktualisiert `endtime`, `status`, `renewals + 1` lokal.
- Mitteilungen: Endpunkt optional, 404 = leere Liste (kein Fehler).

**Login-Sheet:** Benutzernummer (Buchstaben erlaubt), Passwort, Scan per Kamera. Formular muss Autofill/Passwortmanager überstehen (bekannter iOS-Fehler: Eingabe ging beim Szenenwechsel verloren).
**Scanner** (`BenutzernummerFormat`): liest den **Barcode** auf der Rückseite der Thoska (kein Text). Gültig: Leerraum entfernt, 8–14 Zeichen, beginnt mit ASCII-Ziffer, nur ASCII-Ziffern/-Buchstaben (am Ende oft ein „X“). Nur bei Codabar werden Start/Stopp-Zeichen A–D vorn und hinten entfernt. Akzeptierte Formate: Code 39 (inkl. Mod 43), Code 93, Code 128, ITF, ITF-14, EAN-13, EAN-8 (+ Codabar). Alles andere wird still ignoriert. Taschenlampe schaltbar. Zustände: prüft, scannt, gefunden (kurze Pause, dann schließen), verweigert, nicht verfügbar. Kein Bild wird gespeichert, nichts geloggt. Nach Schließen Fokus auf Passwort (nach Treffer) bzw. Benutzernummer (nach „selbst eingeben“).

**Katalog:** Schnittstelle und Verhalten: [library.md](../docs/migration/api/library.md) und `Suche-API-Analyse.md` im iOS-Projekt. Debounce 700 ms, Generationszähler, Fortsetzung im Gesamtkatalog, Availability-Batching (80 ms, max. 20 IDs), Cover mit Retry (`cover_pending`, bis zu 3 Versuche, Wartezeit min(Retry-After, 30 s)), Cover-Cache pro Sitzung.

**Merkliste (Katalogtitel):** lokal, ohne PAIA, funktioniert abgemeldet. Eintrag `GemerkterTitel(id med_<PPN>, titel, personen, jahr, medientyp, coverURL, gemerktAm, foundAt, changedAt)`.
- Neueste zuerst nach `gemerktAm`; Liste „offen“ und „gefunden“ (am Regal abgehakt, `foundAt`).
- Doppelte IDs verhindern; ein von früher übernommener Eintrag ohne Daten wird nachträglich ergänzt, beim Nachladen bleiben `gemerktAm`, `foundAt`, `changedAt` unverändert.
- Entfernen ist **widerrufbar** an der alten Position (`EntfernterTitel`), „Gefunden“ ebenfalls widerrufbar. „Alle entfernen“.
- Grabsteine (30 Tage) und `changedAt`-Konfliktregel dienen nur dem Abgleich mit der Apple Watch. → Für die Erst-Version auf Android **nicht nötig**, aber `foundAt` und `changedAt` mitführen, damit Wear OS später anschließen kann.
- Verfügbarkeit wird beim Öffnen der Liste frisch geladen (Pakete à 20 IDs, parallel), nie gespeichert. Fehler zeigen „Unbekannt“. Zeile zeigt Signatur und Standort des besten Exemplars (erst ausleihbar, dann nur vor Ort, dann ausgeliehen, sonst erstes), „+ N weitere“, Lageplan-Link nur http(s).

**Ausweis:** Bibliotheksausweis mit Strichcode der Benutzernummer, geschützt per Biometrie.
- iOS erlaubt nur Face ID/Touch ID, **nie** den Gerätecode (`deviceOwnerAuthenticationWithBiometrics`), bei jedem Aufruf neuer Kontext (keine frühere Bestätigung gilt). Hinweise für „nicht eingerichtet“, „kein Code gesetzt“, „gesperrt“.
- Inhalt: Name, Ablaufdatum, Status, Nutzergruppe-Chip. Die Tabelle URI → Text ist **leer** (`PAIANutzergruppe.tabelle`, TODO: echte URIs bei der UB erfragen), bis dahin entfällt der Chip. `email` und `address` werden verworfen. Daten nur im Speicher, weg bei Abmelden/401/Neustart.
- **Strichcode ist Code 39** (von der App selbst gezeichnet, Muster in `Strichcode.code39Muster`): Start/Stopp `*`, je schmale Lücke zwischen Zeichen, Kleinbuchstaben → groß, unbekannte Zeichen → kein Bild. Ganzzahlige Pixelbreiten (kein Weichzeichnen), breit:schmal ≈ 2,5:1, Höhe 32 px, Ruhezone ≥ 10 schmale Striche im Rand (min. 18 pt), schmaler Strich höchstens 1,8 px/pt. Code 128 existiert als nicht genutzte Alternative. → Android: denselben Renderer in Kotlin/Canvas.

### 3.5 Events

Schnittstelle: [events.md](../docs/migration/api/events.md).
- Liste ab heute, nach Beginn aufsteigend, 100 pro Seite, Filter: Zeitraum, Tag, „Heute Abend“ (ab 17 Uhr, ganztägige fallen weg), Veranstalter (nur per ID), Kategorie (Veranstaltung/Frist), abgesagte ausblenden.
- Statusanzeige: findet statt / unbestätigt / abgesagt.
- Mehrtägige Veranstaltungen zählen an jedem Tag dazwischen; Regeln für den letzten Tag in `CampusEvent.letzterTag` (siehe events.md).
- Detail mit Bild, Ort, Adresse, Veranstalter (nicht anzeigen, wenn gleich Ort), Beschreibung, Info-/Ticket-Link (nur http/https).
- Merkliste lokal (nur IDs), Filter „Gemerkt“ wirkt auf die geladene Liste. Kein Server-Merken. Entfernen des Sterns im Filter „Gemerkt“ darf nicht abstürzen.
- „Heute Abend“-Karte auf Heute (`HeuteAbendModel`).
- Kalender-Export mit Kalenderauswahl (iOS EventKit). Android: Insert-Intent.
- Teilen (Text und Karte).
- „Vergangene“ Veranstaltungen (`EventsVergangeneView`).

### 3.6 Mehr

Bereiche (`MehrBereich`): **konto** (Bibliothekskonto), **darstellung**, **mensa**, **sprache**, **ueber**. Kacheln: blau (konto, darstellung), grün (mensa), neutral (sprache, über).
- Darstellung: System/Hell/Dunkel (`darstellung`).
- Mensa: Preisgruppe, Mensa auf Heute, Filter-Einstieg („Filter für Ernährung und Allergene“).
- Sprache: eigener Bereich (Android: App-Sprache über `LocaleManager`).
- App-Icon wählbar: Automatisch, Hell, Dunkel (iPad und iPhone).
- Links: Impressum, Datenschutz, UB-Seite usw.
- „Gespeicherte Daten“: Übersicht und Löschen.
- Kurzbefehle/Siri-Tipps.

### 3.7 Teilen

Teilkarten als Bild (Gericht, Veranstaltung, Medium) und Texte. Beim Teilen als Bild wird **kein zusätzlicher Text** mitgeschickt. Bild sichern in Fotos. Texte und Karten: `Teilen/*`.

### 3.8 Siri, Widgets, Uhr

- **Siri/AppIntents:** Mensa heute, Öffnungszeiten, Abendessen, „Heute Abend“, Katalogsuche, Merkliste. Parameter: Mensa (`MensaOrt` inkl. „Cafeteria“ ohne Ort), Ernährung (`vegetarisch|vegan`). Rohwerte gespeichert.
- **Siri-Tipps (Mehr/Speiseplan):** Speiseplan zeigt den Tipp ab dem 3. Öffnen (`siriTippMensaOeffnungen`), × blendet dauerhaft aus (`siriTippMensaVerborgen`). Tipp in „Heute Abend“ erscheint genau einmal (`siriTippAbendGezeigt`). Kein Tipp in Thoska und Bibliothek.
- **Widgets:** „Mensa heute“ (gemeinsamer Filter und Preisgruppe, Schalter „Filter aus der App anwenden“, Mensa-Wechsel-Knopf in der Reihenfolge der Venues), „Heute Abend“, Sperrbildschirm-Widgets und Steuerelemente. Daten im `WidgetStore` der App Group, **keine Kontodaten** (kein Thoska-Guthaben, keine Ausleihen, kein PAIA-Token).
- **Apple Watch:** Mensa, Heute Abend, Merkliste, Komplikationen, Smart Stack. → **Wear OS später**, eigenes Modul.

### 3.9 Start-Animation

Statischer Launch Screen (`LaunchGoat` auf `launchWall`) = erster Frame, danach „Durch die Ziege“ in 1,1 s. Ziele von außen warten, bis sie vorbei ist. Quellen: `Launch/*`, `Design/Logo 2026/Launch/Launch-Animation.md`, `Goat Mask.pdf`.

### 3.10 iPad-Hülle

Tab-Leiste oben und Seitenleiste, Zwei-Spalten-Layouts. Nur bei Breite `.regular` auf dem iPad, ein iPhone Max im Querformat bleibt in der iPhone-Hülle. → M9 über `WindowSizeClass`.

## 4. Nicht offensichtliche Logik (Checkliste für die Umsetzung)

- Berliner Zeitzone überall; `from`/`to` der Events-API zählen in UTC-Tagen (Fenster verbreitert, dann clientseitig auf Berliner Tage zuschneiden).
- API-Datumsformate: ISO 8601 **mit** Millisekunden (`2026-10-29T00:06:54.519Z`), **ohne** Millisekunden, und reines Datum `2026-11-05` (= 12:00 UTC). Parser muss alle drei können.
- URL-Query selbst kodieren: `+` bleibt in `URLComponents` stehen und würde als Leerzeichen gelesen („C++“). Nur unreservierte Zeichen (`A–Z a–z 0–9 - . _ ~`) bleiben stehen.
- Katalogsuche: Task-Abbruch plus Generationszähler; Abbrüche (`cancelled`) sind kein Fehler.
- Cover: 404 mit `cover_pending` ist „noch nicht da“ (Retry-After, Standard 30 s), `cover_missing` ist „gibt keins“.
- Mensa: Berliner Mittag (12:00) des Tages als Anfragezeitpunkt, damit der API-Tag unabhängig von der Gerätezeitzone stimmt. Jede Anfrage braucht eigenen Zustand (parallele Anfragen dürfen sich nicht überschreiben).
- Zusätzlich beim Android-Port: Event-Liste „ab Tag X“ holt in der ersten Seite zusätzlich laufende mehrtägige Veranstaltungen (zweite Abfrage mit `date=`), Fehler dabei sind unkritisch.
- Mensa-Favoriten über Namen, Event-Merkliste über IDs, Katalog-Merkliste über `med_<PPN>`.
- Das Login-Sheet darf bei Szenen-/Konfigurationswechsel keine Eingabe verlieren.
- Passwort/Token nie loggen, nie in Backup (Android: `data_extraction_rules`).

## 5. Bewusste Abweichungen Android

| Thema | iOS | Android |
|---|---|---|
| Biometrie Ausweis | nur Biometrie | Biometrie **oder** Geräte-PIN (Entscheidung offen, siehe [02-entscheidungen.md](../docs/migration/02-entscheidungen.md)) |
| Gespeichertes PAIA-Passwort | nein | optional (Schalter) |
| Kalender-Export | EventKit mit Kalenderauswahl | Insert-Intent |
| Scanner | Vision/VisionKit | CameraX + ML Kit Barcode Scanning |
| Strichcode Ausweis | selbst gezeichnet (Code 39) | derselbe Renderer in Compose-Canvas |
| App Group | geteilte UserDefaults | gemeinsamer DataStore |
| Widgets/Siri | WidgetKit/AppIntents | Glance, App Shortcuts/AppFunctions |
| Merkliste-Abgleich | mit Apple Watch | entfällt bis Wear OS |

## 6. Offene Punkte (an den Meilenstein gebunden)

- Hinweise: kein Endpunkt (M4, nachfragen).
- Nutzergruppen-URIs der UB (M7e, bei der UB erfragen oder aus echter `core/{patron}`-Antwort ablesen).
- Mensa-Endpunkte `/v1/mensa/codes` (kennt die App nur aus Kommentaren, nicht aufgerufen).
- Verfügbare App-Sprachen aus `Localizable.xcstrings` (M1, prüfen).
- Wert des Zeitlimits `heuteMitZeitlimit` (M4, in `HeuteModel` nachsehen).
- Ob die API Katalog-Feldsuchen wirklich im Gesamtkatalog sortiert (siehe `Suche-API-Analyse.md`, Abschnitt 7).
