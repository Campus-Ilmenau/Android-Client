# Deeplinks

Quelle: `Shared/DeepLink.swift`, `Shared/DeepLinkEingang.swift`, `AppRouter.swift`. Schema **`campusilmenau://`** (Host = Bereich, Pfad = Unterziel). Die Mensa-`<id>` ist `MensaVenue.rawValue`.

| Ziel | URL | iOS-Fall | Router-Wirkung |
|---|---|---|---|
| Speiseplan einer Mensa | `campusilmenau://mensa?mensa=<id>` | `.mensa(id)` | Mensa-Tab, ggf. Mensa wählen; ohne ID zuletzt gezeigte |
| Gericht | `campusilmenau://mensa/gericht/<id>[?tag=yyyy-MM-dd]` | `.gericht(id, tag)` | Mensa-Tab, Detail öffnen (Tag Berlin, fehlend = heute) |
| Öffnungszeiten | `campusilmenau://mensa/zeiten` | `.mensaZeiten` | Mensa-Tab mit Öffnungszeiten |
| Alle Veranstaltungen | `campusilmenau://events` | `.events` | Events-Tab, Liste ohne Filter |
| Heute Abend | `campusilmenau://events?filter=heute-abend` | `.eventsHeuteAbend` | Events-Tab mit Filter „Heute Abend“ |
| Event-Detail | `campusilmenau://events/<id>` | `.event(id)` | Events-Tab, Detail |
| Thoska auslesen | `campusilmenau://thoska/auslesen` | `.thoskaAuslesen` | Thoska-Tab, Scan sofort. **Ohne NFC: ignorieren** |
| Katalogsuche (Fokus) | `campusilmenau://katalog/suche` | `.katalogSuche` | Bibliothek → Katalog, Tastatur offen |
| Katalogsuche mit Begriff | `campusilmenau://katalog/suche?q=<begriff>` | `.katalogSuchen(begriff)` | Katalog, Treffer laden, **ohne** Fokus |
| Katalog-Titel | `campusilmenau://katalog/titel/<id>` | `.katalogTitel(id)` | Katalog, Titel-Detail |
| Merkliste | `campusilmenau://bibliothek/merkliste` | `.merkliste` | Bibliothek → Merkliste |
| Mehr & Einstellungen | `campusilmenau://mehr[?bereich=<b>]` | `.mehr(bereich)` | Mehr, bei Bereich vorgewählt |

`bereich` ∈ `darstellung | mensa | konto | sprache | ueber`. **Ein unbekannter Bereich gilt wie keiner.**

## Parsen

- Schema case-insensitiv, **Host** (lowercase) bestimmt den Bereich, `pathComponents` ohne `/` das Unterziel.
- Leere Query-Werte gelten als fehlend.
- `events` mit unbekanntem `filter` → **kein Ziel** (`nil`), nicht „alle“.
- Alles andere → `nil` (nicht zur App gehörig).
- Pfadteile mit `-._~` und Alphanumerik kodieren, alles andere prozent-kodieren.

## Verhalten

- **Warten auf Start:** Während die Start-Animation läuft, wird das Ziel gemerkt und danach geöffnet.
- **Von außen:** Eine Öffnung aus Link/Intent/Widget gilt 3 Sekunden als „von außen ausgelöst“ (`kamGeradeVonAussen`). Dann zählt die Aktion nicht als Handlung in der App (nur für Siri-Vorschläge relevant, auf Android erst in M10 von Interesse).
- **Wünsche** werden erst geliefert, wenn die Ansicht erschienen ist (kein „Rückfall nach 0,6 s“, in iOS entfernt). Auf Android: Navigation per Route mit Argumenten statt Flags.
- Nur Debug: `campusilmenau://debug-kompakt` (Split View), **nicht portieren**.
- Handoff von der Watch (`UhrHandoff`) nutzt denselben Weg → entfällt.

## Android-Umsetzung

- Intent-Filter in `AndroidManifest.xml` für `<data android:scheme="campusilmenau" />` (`VIEW`, `DEFAULT`, `BROWSABLE`).
- `sealed interface DeepLink` + `DeepLink.parse(Uri)` und `DeepLink.toUri()` (reine Kotlin-Klasse, Unit-Tests aus `SiriZielTests`/`SteuerelementIntentTests`-Logik).
- Navigation: Routen mit Argumenten, `onNewIntent` leitet an den Router.
- Zusätzlich später HTTPS-App-Links, falls die Website Links bereitstellt (offen).
