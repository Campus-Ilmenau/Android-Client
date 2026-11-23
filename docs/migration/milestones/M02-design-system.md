# M2 – Design-System-Bausteine und App-Shell

**Status:** offen · **Abhängig von:** M1 · **Gate:** `compileDebugKotlin`

## Ziel

Alle wiederkehrenden Bausteine existieren einmal und werden überall genutzt. Die App hat eine Navigations-Hülle mit fünf Tabs.

## Aufgaben

### Bausteine (`ui/components/`)
- [ ] `Card` (Fläche `surface`, Ecken 16, Innenabstand 14/16, flach).
- [ ] `Chip` mit Varianten: green, blue, warning, accentSoft, dangerSoft, neutral.
- [ ] `PrimaryButton` / `SecondaryButton` (Höhe 50, Ecken 12, Semibold 16), `DestructiveButton` nur für kritische Aktionen.
- [ ] `Toast` (unten über der Tab-Leiste, verschwindet nach 2,4 s).
- [ ] Sheet-Baustein auf Basis `ModalBottomSheet` (Griff, „Abbrechen“ links oben).
- [ ] `DiagonalBands` (Canvas, Blau vorne, Grün dahinter, ca. 60°, weicher Schatten).
- [ ] `GoatLoader` (Ladeanimation, 96/44/28 dp, Farbe `accent`).
- [ ] Abendsonne (Kreis mit Verlauf `brandOrange → brandYellow`).
- [ ] Neutraler Toggle (Pendant `NeutralToggleStyle`).
- [ ] Preview-Katalog (Light/Dark), Pendant zu `DesignFoundationPreview.swift`.

### App-Shell (`ui/navigation/`)
- [ ] Routen und NavGraph für alle Ziele aus `AppRouter`.
- [ ] Tab-Leiste mit fünf Tabs (Heute, Mensa, Thoska, Bibliothek, Events). Aktiv `accent`, sonst `textTertiary`. Tab-Leiste verschwindet bei sichtbarer Tastatur.
- [ ] Tabs werden erst beim ersten Besuch aufgebaut und behalten Zustand.
- [ ] „Mehr“ als Personen-Button auf Heute.
- [ ] Deeplink-Router für `campusilmenau://` (Pendant `DeepLink.swift` / `DeepLinkEingang.swift`).
- [ ] Zurück-Verhalten (Predictive Back).

### Start-Animation
- [ ] Splash-Screen: statischer erster Frame (Ziege auf `launchWall`).
- [ ] Anschließend Compose-Animation „Durch die Ziege“ in 1,1 s (`Launch/Launch-Animation.md`, `LaunchTimeline.swift`, `LaunchAnimation.swift`).

## Abnahme

- Alle Bausteine in Previews (Light/Dark) sichtbar und mit Mockup verglichen.
- Navigation zwischen den Tabs und Deeplinks funktionieren mit Platzhalter-Screens.
- Start-Animation läuft ohne Sprung vom Splash in die App.

## Quellen (iOS)

`Campus Ilmenau/DesignSystem/*` (Card, Buttons, Toast, DiagonalBands, GoatLoading, NeutralToggleStyle), `Launch/*`, `ContentView.swift`, `AppRouter.swift`, `Shared/DeepLink*.swift`
