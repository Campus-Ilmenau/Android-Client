# M2 – Design-System-Bausteine und App-Shell

**Status:** abgeschlossen (2026-11-24) · **Abhängig von:** M1 · **Gate:** `compileDebugKotlin`, `assembleDebug`, Unit-Tests ✔

## Ziel

Alle wiederkehrenden Bausteine existieren einmal und werden überall genutzt. Die App hat eine Navigations-Hülle mit fünf Tabs, Deeplinks und die Start-Animation „Durch die Ziege“.

## Ergebnis

- Bausteine in `ui/components/`, Preview-Katalog in Light und Dark (`DesignCatalogPreview.kt`).
- App-Shell mit **Navigation 3**: je Tab ein eigener Back Stack, „Ausstieg über Heute“, Tab-Leiste verschwindet bei sichtbarer Tastatur, Thoska-Tab nur mit NFC-Hardware.
- Deeplinks `campusilmenau://` als reine Kotlin-Klasse (`DeepLink`) mit Rundweg-Tests für alle 12 Ziele, Intent-Filter im Manifest, `singleTop`.
- Start-Animation: System-Splash als erster Frame, danach die Compose-Animation. Auf dem Emulator in Hell und Dunkel geprüft.
- 72 Unit-Tests (M1: 33, M2: 39), alle grün. Lint: 0 Fehler.

## Aufgaben

### Bausteine (`ui/components/`)
- [x] `CampusCard` (Fläche `surface`, Ecken 16, Innenabstand 14/16, flach, optional tippbar).
- [x] `CampusChip` mit sechs Varianten: Green, Blue, Warning, AccentSoft, DangerSoft, Neutral.
- [x] `PrimaryButton`, `SecondaryButton` (auch `compact`), `DestructiveButton`, `SoftDestructiveButton`. Höhe 50, Ecken 12, Semibold 16. Laden und deaktiviert unterstützt, Tippfläche 48 dp, Text einzeilig.
- [x] `Toast` mit `ToastState`/`ToastHost` (2,4 s, Aktion rechts, neue Meldung startet die Dauer neu). Die Shell stellt ihn über `LocalToastState` bereit.
- [x] `CampusSheet` auf Basis von `ModalBottomSheet` (immer volle Höhe, Ecken 14 oben, „Abbrechen“ links oben).
- [x] `DiagonalBands` mit den Stilen Header, Event, Hero und den Ebenen All, Behind, Front.
- [x] `GoatLoader` (96/44/28 dp, Farbe `accent`, Boden und Staub ab 60 dp, bei „Bewegung reduzieren“ nur Pulsieren). Bewegungslogik in `GoatMotion` getrennt.
- [x] `EveningSun` (Abendsonne).
- [x] `NeutralSwitchVisual`/`NeutralSwitchRow` und `SegmentedControl` (kamen aus `NeutralToggleStyle`/`SegmentedControl` der iOS-App).
- [x] Preview-Katalog: Bausteine, Bänder, Tab-Leiste, Ziege (Hell, Dunkel, Bewegung reduziert).

### App-Shell (`ui/navigation/`)
- [x] Routen als `@Serializable NavKey`: je Tab eine Wurzel, dazu `MehrRoute(bereich)`.
- [x] `AppTab` (Heute, Mensa, Thoska, Bibliothek, Events) mit Icon und Titel. Tab-Leiste `CampusTabBar` (aktiv `accent`, sonst `textTertiary`, oben Trennlinie).
- [x] `NavigationState`/`Navigator`: Zustand überlebt Drehen und Prozessende, Tabs werden erst beim Besuch aufgebaut, erneutes Tippen auf den Tab meldet ein Ereignis (für „nach oben scrollen“).
- [x] „Mehr“ als Personen-Button auf Heute, öffnet über Heute (auch aus anderen Tabs per Link).
- [x] `DeepLink` (parse/url, Tab-Zuordnung), `DeepLinkRouter` (Anfragen mit Id, eine neuere bleibt stehen), Auswertung in `AppShell` erst nach der Start-Animation.
- [x] Zurück-Verhalten: Route entfernen, sonst zurück zu Heute, sonst App verlassen. Vorhersage-Zurück über `enableOnBackInvokedCallback`.

### Start-Animation (`ui/launch/`)
- [x] Splash über `core-splashscreen`: statischer erster Frame (Hell: weiße Ziege auf `#FF861E`, Dunkel: nachtblaue Ziege vor der Abendsonne). Der Splash bleibt, bis die Einstellungen gelesen sind, und wird ohne eigene Ausblendung entfernt.
- [x] `LaunchTimeline`/`LaunchFrame` (1,1 s): halten, ausholen auf 0,88×, Ziegenfüllung ausblenden, durchfliegen bis zur Endskalierung, Inhalt skaliert von 1,08 auf 1,0.
- [x] `LaunchWall`: Wand mit Ziege als Loch (`BlendMode.Clear` in eigener Ebene), Abendsonne mit Horizont im Dark Mode, zwei Ringe (Parallaxe), Kantenlicht. Farben folgen dem **System**, nicht der App-Darstellung.
- [x] `LaunchOverlay`: spielt nur beim Kaltstart (Zustand per `rememberSaveable`), blockiert Berührungen bis zum Ende, bei „Bewegung reduzieren“ nur kurz halten und überblenden.

## Abweichungen und Entscheidungen

- **Namen:** Bausteine heißen `CampusCard`, `CampusChip` usw., um Verwechslungen mit Material-3-Komponenten zu vermeiden.
- **Ziegengröße im ersten Frame:** iOS 180 pt breit, Android **ca. 131 dp** (0,47 × 288 dp × 996/1024). Nur so sitzt die Ziege exakt auf dem Splash-Symbol des Systems (288 dp, Kreismaske 192 dp), und die Compose-Animation läuft ohne Sprung weiter. Der Startpunkt ist der Mittelpunkt des umschließenden Kreises (420 | 500), nicht der der Bounding Box, damit Kreismasken die Ziege nicht anschneiden. Der erste Frame steht deshalb nicht 10 pt über der Mitte wie auf iOS.
- **Splash auf Android:** Bei „Dunkel“ in der App und hellem System zeigt der Splash das Hell-Design (er folgt dem System). Die Wand der Animation folgt ebenfalls dem System, danach wechselt der Inhalt zur App-Darstellung.
- **Segment-Auswahl:** Statt `matchedGeometryEffect` blendet die Auswahlfläche per Farbanimation um.
- **Deeplinks:** Die Shell wählt den Tab und quittiert die Anfrage sofort. Details (Mensa-ID, Gericht, Event-ID, Suchbegriff) lesen die Ansichten ab M3 aus der Anfrage, **bevor** sie sie quittieren. Dafür wird in M3 die Quittierung von der Shell zu den Ansichten verlagert.
- **Material-Icons:** `material-icons-extended` ersetzt die SF-Symbole (Sonne, Besteck, NFC, Buch, Kalender, Person).

## Bekannte Punkte

- `DiagonalBands` zeichnet den Schatten über `setShadowLayer`. Er erscheint ab Android 9 (minSdk 26 zeigt ihn nicht). Die Flächen selbst sind überall gleich.
- Im Katalog bricht „Halloween-Party“ bei 140 dp Abstand rechts um. Das Event-Detail (M5) setzt die Schrift dort passend.
- Lint meldet für die Splash-Symbole „Vektor größer als 200 dp“. Das ist für das System-Splash-Maß (288 dp) so vorgesehen.
- `ToastHost` steht in der Shell über dem Inhalt, aber unter der Tab-Leiste. Er wird ab M3 von den Ansichten genutzt.
- Tablet-Layouts (Navigation Rail statt Leiste) folgen in M9.

## Abnahme

- [x] Alle Bausteine in Previews (Light und Dark) sichtbar und mit dem Design verglichen.
- [x] Navigation zwischen den Tabs und Deeplinks funktionieren (auf dem Emulator geprüft: `mensa`, `events/<id>`, `mehr?bereich=konto`, Zurück).
- [x] Start-Animation läuft ohne Sprung vom Splash in die App (Hell und Dunkel).
- [x] `./gradlew :app:assembleDebug` und `:app:testDebugUnitTest` mit Exit 0.

## Quellen (iOS)

`Campus Ilmenau/DesignSystem/*` (Card, Chip, Buttons, Toast, DiagonalBands, GoatLoader, NeutralToggleStyle, SegmentedControl, TabBar), `Launch/*`, `ContentView.swift`, `AppRouter.swift`, `Shared/DeepLink*.swift`, `Design/Logo 2026/Launch/*`
