# M3 – Mensa

**Status:** offen · **Abhängig von:** M2 · **Gate:** 2 (Unit-Tests Mensa-Logik grün, Screens im Emulator geprüft)

## Ziel

Der Mensa-Tab ist vollständig: Speiseplan für 4 Mensen bis 3 Wochen im Voraus, Filter, Preisgruppen, Detail, Öffnungszeiten. Erste vollständige vertikale Scheibe durch alle Schichten.

## Aufgaben

### Daten und Domäne
- [ ] Modelle: `MensaMeal`, `MensaVenue(s)`, `MensaOrt`, `Ernaehrung`, `MensaPriceGroup`.
- [ ] Mensa-Quelle/-Service (`MensaQuelle`, `ServicesMensaService`) als Repository mit Live-Daten und Cache.
- [ ] Filter-Logik (`MensaFilter`) und Speicherung in DataStore.
- [ ] Preisgruppen (Studierende, Mitarbeitende, Gäste) mit Speicherung.
- [ ] Öffnungszeiten-Logik (`MensaOpeningHours`) mit injizierbarer Uhr.
- [ ] Präsentationslogik (`MensaMealPresentation`, `MensaFormatting`): „vegetarisch“ auch bei veganen Gerichten.
- [ ] Favoriten (`MensaFavoriteStore`): lokal, Gericht zählt über den **normalisierten Namen** (getrimmt, lowercase), weil die API-ID pro Tag wechselt.
- [ ] Echte Beispielantworten der Dev-API sichern (siehe [api/README.md](../api/README.md)), Datumsformat der Gerichte prüfen.
- [ ] Deeplinks `mensa`, `mensa/gericht/<id>?tag=`, `mensa/zeiten` anbinden (Gericht-IDs gelten nur einen Tag).

### UI
- [ ] `MensaScreen` mit Pager zwischen den Mensen und Tagesleiste (Datumsauswahl, bis 3 Wochen).
- [ ] Gerichtekarte (`MensaMealCard`): Chips, Preis, gleiche Höhe, keine abgeschnittenen Texte.
- [ ] Filter-Sheet (`MensaFilterSheet`).
- [ ] Gericht-Detail mit Nährwerten und Komponenten, Kopf mit `DiagonalBands`.
- [ ] Öffnungszeiten-Ansicht.
- [ ] Zufällige, verspielte Ladetexte, `GoatLoader`.
- [ ] Fehler- und Leerzustände.
- [ ] Previews Light/Dark.

### Tests (aus iOS portieren)
- [ ] `MensaFilterTests`
- [ ] `MensaDetailTests`
- [ ] `WidgetGerichtTests` (Logik-Teil)

## Abnahme

- Mit echten Daten: alle 4 Mensen, Datumswechsel, Filter wirken, Preisgruppe wechselt Preise.
- Unit-Tests grün, Verhalten stimmt mit iOS überein.
- Screens entsprechen den Mockups (`MensaGericht` usw.).

## Quellen (iOS)

`Campus Ilmenau/Mensa/*`, `ViewsMensa*.swift`, `Shared/Mensa*.swift`, `Shared/ModelsMensaMeal.swift`, `Shared/Ernaehrung.swift`
