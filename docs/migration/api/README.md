# API-Verträge

Dokumentiert, was die iOS-App heute von den Schnittstellen erwartet. Quelle: iOS-Code (Stand 2026-11-23). Die Beispielantworten sind aus den Modellen und Kommentaren abgeleitet, **echte Antworten müssen vor M3/M5/M7 gegen die Dev-API geprüft werden**.

| Datei | Schnittstelle | Auth |
|---|---|---|
| [mensa.md](mensa.md) | Campus-Ilmenau-API `/v1/mensa` | keine |
| [events.md](events.md) | Campus-Ilmenau-API `/v1/events` | keine |
| [library.md](library.md) | Campus-Ilmenau-API `/v1/library` | keine |
| [paia.md](paia.md) | GBV PAIA der UB Ilmenau | Bearer-Token nach Login |

## Gemeinsam (Campus-Ilmenau-API)

- **Basis-URL:** Release `https://api.campus-ilmenau.de`, Debug `https://dev-api.campus-ilmenau.de/develop`. Nur die Basis wechselt, die Pfade (`/v1/...`) bleiben gleich. Android: `BuildConfig.API_BASE_URL`.
- **Format:** JSON. Die **Daten** (`data[]`) tragen `camelCase`-Schlüssel (`openingHours`, `opensAt`), die **Hülle** (`has_more`, `total_count`) und die **Fehlerantwort** (`request_id`) `snake_case`. Android: Ein `CamelCaseKeysInterceptor` (`core/network/CamelCaseKeys.kt`) wandelt jede Antwort auf `camelCase` um, damit die DTOs eine Schreibweise haben; `ApiJson` braucht keine Namensstrategie. Nur Objekte und Arrays werden angefasst (leere und HTML-Antworten bleiben unverändert). Befund aus M3 mit den echten Antworten der Dev-API.
- **Datumsformate:** ISO 8601 mit Millisekunden, ISO 8601 ohne Millisekunden, reines Datum `yyyy-MM-dd` (wird als 12:00 UTC gelesen). Ein Parser für alle drei.
- **Fehlerantwort:**
  ```json
  { "error": { "type": "...", "code": "invalid_parameter_format", "message": "...", "param": "organizer", "request_id": "..." } }
  ```
- **Header:** `Accept: application/json`. Antworten der Library-API tragen `X-Cache-Status` (`hit|miss|stale`), `X-Request-Id`, bei Cover-Wartezeit `Retry-After`.
- **URL-Kodierung:** Query-Werte selbst kodieren, nur `A–Z a–z 0–9 - . _ ~` bleiben stehen (sonst wird `+` zu Leerzeichen). Pfadteile genauso.
- **Abbruch:** Abgebrochene Anfragen (Coroutine-Cancel) sind kein Fehler und werden nicht angezeigt.
- **Listen-Hülle** (Mensa): `{ object, url, has_more, total_count, data[] }`.

## Vor dem Umsetzen zu prüfen

- [ ] Je Endpunkt eine echte Antwort der Dev-API sichern (`docs/migration/api/samples/`).
- [ ] Mensa: Datumsformat der Gerichte (mit/ohne Millisekunden, die iOS-App nutzt dort die strikte `.iso8601`-Strategie).
- [ ] `GET /v1/mensa/codes` (Kategorien `allergen`, `additive`): von der App nicht aufgerufen, aber als Quelle der festen Listen genannt.
- [ ] Rate-Limits, Caching-Header, Fehlerverhalten bei 5xx.
