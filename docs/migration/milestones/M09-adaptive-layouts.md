# M9 – Adaptive Layouts

**Status:** offen · **Abhängig von:** M3–M8 · **Gate:** `compileDebugKotlin`

## Ziel

Die iPad-Hülle wird auf Android über Fenstergrößen abgebildet: Tablets, Faltgeräte, Chromebooks, Split-Screen.

## Aufgaben

- [x] Breitenklasse des Fensters in der Shell (9a): `WindowWidthClass` (Compact < 600 dp, Medium ab 600, Expanded ab 840). Compact: Tab-Leiste unten. Medium: Navigation Rail (88 dp). Expanded: Seitenleiste (248 dp) mit Kopf (Ziege + „Campus Ilmenau“). Beide haben „Mehr“ als Eintrag unten (`CampusNavigationRail`). Die Sub-Seiten der Bibliothek und Events (iOS-Abschnitte „Ausleihen/Katalog/Merkliste/Ausweis“) sind noch nicht eigene Einträge der Leiste.
- [~] Zustand bleibt beim Wechsel der Breite erhalten: Tab und Back Stacks liegen im `NavigationState` (`rememberSaveable`), die Shell wechselt nur die Navigation am Rand. Tag, Filter und Scrollposition der einzelnen Screens sind noch zu prüfen (auf dem Emulator: Tablet-Größe per `wm size` geprüft, Seitenleiste erscheint).
- [ ] Heute in zwei Spalten.
- [ ] Mensa mit allen Mensen als Spalten (bei großer Schrift max. zwei, Tageskacheln wachsen mit). Kopf mit Filtern und Tagesleiste bleibt beim Scrollen.
- [ ] Bibliothek mit drei bzw. zwei Spalten.
- [x] Katalog (9b): Treffer und Titel nebeneinander ab 840 dp (`ListDetailSceneStrategy` aus `adaptive-navigation3` 1.3.0, `listPane`/`detailPane` als Metadaten der Einträge in `AppShell`). Rechts steht ohne Auswahl ein Platzhalter (Ziege). Das Detail zeigt neben der Liste keine Zurück-Taste (`LocalSplitView`); ein Tipp auf einen anderen Eintrag ersetzt das offene Detail (`Navigator.openDetail`), „Zurück“ führt zur Liste.
- [~] Merkliste: Liste und Titel nebeneinander (gleiche Szene wie der Katalog, der Titel ist das Detail). Offen: Pfeiltasten ↑/↓ wechseln die Auswahl, markierte Zeile in der Liste.
- [x] Events (9b): Liste und Detail nebeneinander ab 840 dp, auf dem Emulator mit 2560 × 1600 geprüft. Die vergangenen Veranstaltungen bleiben eine eigene Seite.
- [ ] Mehr und Einstellungen: Bereiche und Untermenü nebeneinander.
- [ ] Ziegen-Einlauf in der Seitenleiste (`ZiegeEinlauf`).
- [ ] Tastatur- und Mausbedienung (Fokus, Tooltips, Shortcuts).
- [ ] Tests (portiert): `PadNavigationTests`, `PadRasterTests`, `PadMensaTests`, `KatalogPadTests`, `MerklistePadTests`, `MehrPadTests`.

## Abnahme

- Auf Tablet-, Foldable- und Desktop-Emulator sind alle Hauptscreens nutzbar.
- Fenstergröße ändern verliert keinen Zustand.

## Quellen (iOS)

`Campus Ilmenau/Pad/*`, `*Pad*.swift` in `Bibliothek/`, `Events/`, `Mensa/`, `Mehr/`, `Heute/`
