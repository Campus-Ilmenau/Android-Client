# M4 – Heute

**Status:** offen · **Abhängig von:** M3 (Mensa), Events-Modelle aus M5 für „Heute Abend“ (zunächst mit Fake-Daten) · **Gate:** `compileDebugKotlin`

## Ziel

Die Startseite zeigt je nach Uhrzeit Mensa, Abendessen oder Veranstaltungen, dazu Hinweise. **Keine Live-Daten** wie Thoska-Guthaben oder Anzahl Ausleihen.

## Aufgaben

- [ ] `Tagesphase` und `HeuteUhr` als reine Logik mit injizierbarer Uhr.
- [ ] Begrüßung: bis 11 „Guten Morgen“, bis 14 „Mahlzeit!“, bis 18 „Hallo!“, danach „Guten Abend“.
- [ ] Jetzt-Karte je Tageszeit:
  - bis 10:30: gewählte Mensa, Status, 2 Gerichte
  - 10:30–14:00: Status, 3 Gerichte mit Chips und Preis
  - 14:00–16:30: Abendessen in der Mensa Ehrenberg ab 16:30
  - 16:30–19:30: Abendessen geöffnet
  - ab 19:30: laufendes Club-Event (Kopf Nachtblau mit Sonne), „Mensa ändern“ entfällt
- [ ] „Heute Abend“-Karte (`HeuteAbendKarte`, `HeuteAbendModel`) mit Events des Abends.
- [ ] „Als Nächstes“-Bereich (`HeuteAlsNaechstesView`).
- [ ] Hinweise (max. 2, Priorität/Zeitraum `[von, bis)`, höhere Priorität zuerst, ✕ blendet dauerhaft aus; ohne Hinweise entfällt der Abschnitt). **Es gibt noch keinen Server-Endpunkt**: wie iOS eine austauschbare `HinweiseQuelle` mit leerer Standard-Implementierung. Bei neuen Daten laut `CLAUDE.md` zuerst nachfragen.
- [ ] Quellen mit Zeitlimit laden (Fehler/Zeitablauf → `null`), die Seite hängt nie. Wert des Limits in `HeuteModel` nachsehen.
- [ ] Mensa-Wechsel-Sheet (Radio-Liste der 4 Mensen, „Fertig“), Auswahl in DataStore (`HeuteMensaSpeicher`).
- [ ] Mensa-Filter gilt auch für die Essenskarten auf Heute.
- [ ] Personen-Button öffnet Mehr (M8).
- [ ] Previews: Morgen, Nachmittag, Abend, Nacht (Light/Dark).

### Tests (aus iOS portieren)
- [ ] `HeuteModelTests`
- [ ] `HeuteAbendTests`
- [ ] `LoadingGateTests`

## Abnahme

- Phasenwechsel stimmen an den Grenzzeiten (10:30, 14:00, 16:30, 19:30, Begrüßung 11/14/18).
- Verhalten entspricht den Mockups `Heute*.dc.html`.

## Quellen (iOS)

`Campus Ilmenau/Heute/*`, `Campus Ilmenau/Events/HeuteAbend*.swift`, `Shared/HeuteUhr.swift`
