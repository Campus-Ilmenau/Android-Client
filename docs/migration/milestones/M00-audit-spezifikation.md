# M0 – Audit und Spezifikation

**Status:** offen · **Abhängig von:** – · **Gate:** –

## Ziel

Vor dem ersten Android-Code ist klar, was die iOS-App im Detail tut, welche API-Verträge gelten und welche Lücken es gibt.

## Aufgaben

- [ ] `.migration/feature_specification.md` erstellen: alle Features, Ein-/Ausstiegspunkte, Datenflüsse, Android-Ziel je Feature.
- [ ] Mockup-Beschreibung lesen: `Design/Logo 2026/Mockup/Uebergabe.md` und die `artboards/*.dc.html` (Maße, Texte, Zustände).
- [ ] API-Verträge dokumentieren (`docs/migration/api/`): Endpunkte, Parameter, Beispielantworten, Fehlerfälle.
  - [ ] Mensa (Quellen in `Shared/ServicesMensaService.swift`, `MensaQuelle.swift`, `MensaVenues.swift`)
  - [ ] Events (`EventsService.swift`, `EventsQuelle.swift`, `CampusEvent.swift`)
  - [ ] Library (`Bibliothek/Katalog/LibraryCatalogService.swift`, `Suche-API-Analyse.md`)
  - [ ] PAIA (`ServicesPAIAService.swift`): Login, Items, Notifications, Token-Verhalten
- [ ] Klären: Wie speichert iOS PAIA-Zugang? (kein Keychain-Treffer) → Ergebnis in Entscheidung 5 einarbeiten.
- [ ] Thoska: NFC-Ablauf, Kommandos und Parsing dokumentieren (`ServicesThoskaCardReader.swift`).
- [ ] Alle `@AppStorage`/`UserDefaults`-Schlüssel auflisten (Name, Typ, Standardwert, Bedeutung).
- [ ] Deeplink-Schema (`Shared/DeepLink.swift`, `AppRouter.swift`) vollständig erfassen.
- [ ] Tests inventarisieren (`Campus IlmenauTests/`): welche Testfälle sind portabel.
- [ ] Offene Lücken in [04-nicht-portierbar.md](../04-nicht-portierbar.md) nachtragen.
- [ ] Reihenfolge der Meilensteine bestätigen.

## Abnahme

- `feature_specification.md` und API-Dokumente liegen im Projekt.
- Für jeden Endpunkt gibt es mindestens eine echte Beispielantwort.
- Keine offene Frage zu PAIA-Speicherung, Mensa-Quelle und Deeplinks.

## Quellen (iOS)

`Shared/`, `Campus Ilmenau/AppRouter.swift`, `Suche-API-Analyse.md`, `CLAUDE.md`, `Design/…/Uebergabe.md`
