# M9 – Adaptive Layouts

**Status:** offen · **Abhängig von:** M3–M8 · **Gate:** `compileDebugKotlin`

## Ziel

Die iPad-Hülle wird auf Android über Fenstergrößen abgebildet: Tablets, Faltgeräte, Chromebooks, Split-Screen.

## Aufgaben

- [x] Breitenklasse des Fensters in der Shell (9a): `WindowWidthClass` (Compact < 600 dp, Medium ab 600, Expanded ab 840). Compact: Tab-Leiste unten. Medium: Navigation Rail (88 dp). Expanded: Seitenleiste (248 dp) mit Kopf (Ziege + „Campus Ilmenau“). Beide haben „Mehr“ als Eintrag unten (`CampusNavigationRail`). Die Unterziele der Bibliothek und Events (iOS-Abschnitte „Ausleihen/Katalog/Merkliste/Ausweis“, „Heute Abend“) sind keine eigenen Einträge der Leiste: Sie sind über die Seiten und die Tastenkürzel erreichbar (Strg+F öffnet die Katalogsuche).
- [x] Zustand bleibt beim Wechsel der Breite erhalten: Tab und Back Stacks liegen im `NavigationState` (`rememberSaveable`), Tag, Filter, Preisgruppe und Tab der Bibliothek in ihren ViewModels, der gewählte Bereich unter „Mehr“ und das Mensa-Sheet in `rememberSaveable`. Die Breite wird nur zum Anordnen gelesen, nicht gespeichert.
- [x] Heute in zwei Spalten (9c): ab 680 dp Inhaltsbreite links Hinweise und „Jetzt“, rechts „Als Nächstes“ und „Heute Abend“ (Reihenfolge wie auf dem Handy). `AdaptiveColumns` rechnet die Spalten aus der Breite des Inhalts (ohne Seitenleiste).
- [x] Mensa mit allen Mensen als Spalten (9c): ab 320 dp je Spalte bis zu vier, bei großer Schrift (Skalierung ≥ 1,3) höchstens zwei in zwei Zeilen. Tagesleiste und Filter stehen fest, nur die Spalten scrollen; jede Spalte hat Name, Status, Teilen und bei der Cafeteria Mittag/Abendessen. Nicht übernommen: die angepinnten Spaltenköpfe (die Spalten scrollen mit ihrem Kopf).
- [x] Bibliothek mit zwei bzw. drei Spalten (9c): Ausleihen und Vormerkungen nebeneinander statt des Umschalters, bei drei Spalten dazu das Konto (Ausweis, Abmelden).
- [x] Katalog (9b): Treffer und Titel nebeneinander ab 840 dp (`ListDetailSceneStrategy` aus `adaptive-navigation3` 1.3.0, `listPane`/`detailPane` als Metadaten der Einträge in `AppShell`). Rechts steht ohne Auswahl ein Platzhalter (Ziege). Das Detail zeigt neben der Liste keine Zurück-Taste (`LocalSplitView`); ein Tipp auf einen anderen Eintrag ersetzt das offene Detail (`Navigator.openDetail`), „Zurück“ führt zur Liste.
- [x] Merkliste (9c): Liste und Titel nebeneinander (gleiche Szene wie der Katalog), die Zeile des offenen Titels ist hervorgehoben, Pfeiltasten ↑/↓ wechseln die Auswahl (`MerklisteSelection`).
- [x] Events (9b): Liste und Detail nebeneinander ab 840 dp, auf dem Emulator mit 2560 × 1600 geprüft. Die vergangenen Veranstaltungen bleiben eine eigene Seite.
- [x] Mehr und Einstellungen (9c): ab 720 dp links die Bereiche (Darstellung, Mensa, Bibliothekskonto, Allgemein), rechts der gewählte; der Link `mehr?bereich=` wählt den Bereich. Die Auswahl bleibt über Drehen und Größenänderung.
- [x] Ziegen-Einlauf in der Seitenleiste (9c): Die Ziege läuft beim Erscheinen von links ein (Versatz und Einblenden, 0,9 s), mit reduzierter Bewegung steht sie sofort da. Kein Trab-Frame-Ablauf wie auf iOS (`ZiegeEinlauf`), nur die Bewegung.
- [x] Tastatur und Maus (9c): Strg+1…9 wählt den Tab (ohne NFC rücken die Zahlen nach), Strg+, öffnet „Mehr“, Strg+F die Katalogsuche, Esc geht eine Seite zurück (`AppShortcuts`, in `AppShell`). Die Rail-Einträge haben Tooltips mit dem Kürzel („Heute (Strg+1)“); Pfeiltasten in der Merkliste. Tab-Fokus läuft über die Einträge. Offen: eigene Mauszeiger und Tooltips an den runden Tasten der Köpfe.
- [x] Tests: `WindowWidthClassTest`, `AdaptiveColumnsTest`, `AppShortcutsTest`, `MerklisteSelectionTest` und die fünf neuen Faelle in `NavigatorTest` (Detail ersetzen statt auflägen). Die iOS-Tests `PadNavigationTests`, `PadRasterTests` entsprechen `NavigatorTest`/`AdaptiveColumnsTest`; Rendering-Tests der Spalten (`PadMensaTests`, `MehrPadTests`) bleiben der Emulator-Prüfung vorbehalten.

## Abnahme

- Auf Tablet-, Foldable- und Desktop-Emulator sind alle Hauptscreens nutzbar.
- Fenstergröße ändern verliert keinen Zustand.

## Quellen (iOS)

`Campus Ilmenau/Pad/*`, `*Pad*.swift` in `Bibliothek/`, `Events/`, `Mensa/`, `Mehr/`, `Heute/`
