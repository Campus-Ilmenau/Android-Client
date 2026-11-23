# M9 – Adaptive Layouts

**Status:** offen · **Abhängig von:** M3–M8 · **Gate:** `compileDebugKotlin`

## Ziel

Die iPad-Hülle wird auf Android über Fenstergrößen abgebildet: Tablets, Faltgeräte, Chromebooks, Split-Screen.

## Aufgaben

- [ ] `WindowSizeClass` in der Shell auswerten. Compact: Bottom-Leiste. Medium/Expanded: Navigation Rail bzw. Drawer (Pendant `PadRootView`, `PadZiel`).
- [ ] Zustand bleibt beim Wechsel der Breite erhalten (Tag, Filter, geladene Daten, Rotation, Falten).
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
