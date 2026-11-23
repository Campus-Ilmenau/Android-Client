# Lokale Daten und Einstellungen

Alle Schlüssel und Dateien der iOS-App und ihre Android-Entsprechung. Es gibt **keine Migration alter Daten** (Android ist eine Neuinstallation).

## Einstellungen (`UserDefaults` / `@AppStorage` → DataStore Preferences)

| iOS-Schlüssel | Typ / Werte | Vorgabe | Geteilt mit Widget | Bedeutung | Android |
|---|---|---|---|---|---|
| `darstellung` | `system` \| `hell` \| `dunkel` | `system` | nein | Hell/Dunkel der ganzen App | `AppSettings.darstellung` |
| `mensaPriceGroup` | `studierende` \| `bedienstete` \| `gaeste` | `studierende` | ja | Preisgruppe | `AppSettings.preisgruppe` |
| `heuteMensa` | Venue-rawValue | `mensaEhrenberg` | ja | Mensa auf Heute | `AppSettings.heuteMensa` |
| `MensaFilter` | JSON `{diet, onlyBio, excludedAllergens[]}` | leer (`diet: alle`) | ja | Mensa-Filter | `AppSettings.mensaFilter` (als JSON oder Einzelschlüssel) |
| `mensaFavoriteMeals` | `[String]` (normalisierte Namen) | leer | nein | Favoriten (Name, getrimmt, lowercase) | Room-Tabelle oder `Set<String>` im DataStore |
| `heuteHinweiseAusgeblendet` | `[String]` (sortiert) | leer | nein | weggetippte Hinweise | DataStore |
| `siriTippMensaOeffnungen` | Int | 0 | nein | Zähler Speiseplan-Öffnungen (Tipp ab 3) | DataStore (nur wenn der Tipp auf Android existiert, M10) |
| `siriTippMensaVerborgen` | Bool | false | nein | Tipp weggetippt | wie oben |
| `siriTippAbendGezeigt` | Bool | false | nein | Abend-Tipp schon erschienen | wie oben |
| `debugWidgetUhr` | – | – | – | nur Debug | `BuildConfig.DEBUG` |

Nicht zu übernehmen: `MensaFilterStates` (alter Drei-Zustände-Filter, Migration), `libraryFavoriteTitles` (alte Favoriten-IDs, Migration), `appGroupEinstellungenUebernommen`/`mensaFilterUebernommen` (Migrationsmarken).

**App Group:** Hier liegt nur, was nicht veraltet (Mensa auf Heute, Preisgruppe, Filter, `WidgetStore`). **Keine Kontodaten** (kein Thoska-Guthaben, keine Ausleihen, kein Token). Android: ein gemeinsamer `DataStore`, den App und Glance-Widget nutzen, mit derselben Regel.

## Dateien (`Application Support` → Room)

| iOS-Datei | Inhalt | Android |
|---|---|---|
| `Merkliste.json` | `{ eintraege: [GemerkterTitel], grabsteine: [{id, deletedAt}] }`. Ältere Dateien: nur das Array. | Room-Tabelle `merkliste_titel` |
| `MerklisteEvents.json` | `[String]` sortierte Event-IDs | Room-Tabelle `merkliste_events` oder DataStore-Set |

**`GemerkterTitel`:** `id` (`med_<PPN>`), `titel` (leer = von früher übernommen, unvollständig), `personen`, `jahr?`, `medientyp` (API-Code), `coverURL?`, `gemerktAm`, `foundAt?` (am Regal abgehakt), `changedAt` (Standard = `gemerktAm`).
Regeln: Sortierung neueste zuerst nach `gemerktAm`; eine ID nur einmal; beim Nachladen bleiben `gemerktAm`, `foundAt`, `changedAt`. Entfernen liefert Eintrag + Position zurück (Widerruf). `anzeigeTitel` = Titel oder „Titel <PPN>“. Untertitel = „Personen · Jahr“ ohne Leeres.
Grabsteine (30 Tage) dienen nur dem Abgleich mit der Apple Watch → bis Wear OS **nicht anlegen**, Spalten `foundAt`/`changedAt` aber mitführen.

## Datenbank (SwiftData → Room)

| iOS-Model | Felder | Android-Entity |
|---|---|---|
| `ThoskaBalanceRecord` | `cardId: String`, `balance: Double`, `lastTransaction: Double`, `scannedAt: Date` | `ThoskaBalanceEntity` (Index auf `cardId`, `scannedAt`) |
| `ThoskaCardName` | `cardId: String` (**eindeutig**), `name: String` | `ThoskaCardNameEntity` (PK `cardId`) |

Hinweis: Beträge als `Double` wie in iOS ist für Anzeige ausreichend. Die Karte liefert Tausendstel (Int). Empfehlung Android: als `Long` in Tausendstel speichern (kein Rundungsrauschen, siehe Aufladungs-Toleranz 0,005), Anzeige mit Umrechnung.

## Nicht gespeichert (bewusst)

PAIA-Token, Benutzernummer, Passwort (iOS), Verfügbarkeitsstatus der Merkliste, Inhaltsverzeichnis/Klappentext, Kontodaten des Ausweises (`PAIAPatron`).
**Android-Ausnahme:** optional gespeichertes Passwort (verschlüsselt, Keystore), nicht im Backup.

## Backup

Android: `backup_rules.xml` und `data_extraction_rules.xml` so setzen, dass verschlüsselte Zugangsdaten **nicht** gesichert werden. Merkliste, Thoska-Verlauf und Einstellungen dürfen gesichert werden (Entscheidung in M11 bestätigen).
