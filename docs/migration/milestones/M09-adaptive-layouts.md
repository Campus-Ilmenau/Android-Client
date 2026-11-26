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
- [ ] Katalog: Treffer und Titel nebeneinander (List-Detail-Szene).
- [ ] Merkliste: Liste und Titel nebeneinander, Pfeiltasten ↑/↓ wechseln die Auswahl.
- [ ] Events: Liste und Detail nebeneinander.
- [ ] Mehr und Einstellungen: Bereiche und Untermenü nebeneinander.
- [ ] Ziegen-Einlauf in der Seitenleiste (`ZiegeEinlauf`).
- [ ] Tastatur- und Mausbedienung (Fokus, Tooltips, Shortcuts).
- [ ] Tests (portiert): `PadNavigationTests`, `PadRasterTests`, `PadMensaTests`, `KatalogPadTests`, `MerklistePadTests`, `MehrPadTests`.

## Abnahme

- Auf Tablet-, Foldable- und Desktop-Emulator sind alle Hauptscreens nutzbar.
- Fenstergröße ändern verliert keinen Zustand.

## Quellen (iOS)

`Campus Ilmenau/Pad/*`, `*Pad*.swift` in `Bibliothek/`, `Events/`, `Mensa/`, `Mehr/`, `Heute/`
