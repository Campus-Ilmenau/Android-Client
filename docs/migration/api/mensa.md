# API: Mensa

Quelle: `Shared/ServicesMensaService.swift`, `MensaQuelle.swift`, `MensaVenues.swift`, `MensaFilter.swift`. Ohne Anmeldung.

## Endpunkte

### `GET /v1/mensa` – Speiseplan

| Parameter | Wert | Hinweis |
|---|---|---|
| `period` | `today` \| `week` | Alternative zu `date` |
| `date` | `yyyy-MM-dd` | Berliner Tag. Die App fragt immer mit `date`, gebildet aus **12:00 Uhr Berliner Zeit** des Tages, damit der Tag unabhängig von der Gerätezeitzone stimmt. |
| `canteen` | `rsc_id` (Int) | optional |
| `per_page` | `100` | immer, laut Code Maximum |

Antwort: Listen-Hülle mit `data: [Meal]`.

**Meal**

```json
{
  "id": "string",
  "object": "meal",
  "canteen": { "rsc_id": 1, "name": "Mensa Ehrenberg", "city": "...", "address": "...", "latitude": 0.0, "longitude": 0.0 },
  "date": "2026-11-23T...",
  "meal_group": "Hauptgericht | Abendessen | ...",
  "title": "string",
  "dish_id": "string?",
  "is_active": true,
  "last_seen_at": "...",
  "additive_codes": [ { "code": "...", "name": "...", "description": "..." } ],
  "allergen_codes": [ { "code": "...", "name": "...", "description": "..." } ],
  "symbol_codes":   [ { "code": "...", "name": "...", "description": "..." } ],
  "flags": { "vegetarian": false, "vegan": false, "pork": false, "beef": false, "poultry": false, "fish": false, "garlic": false, "alcohol": false },
  "co2_gram_per_portion": 0.0,
  "like_count": 0,
  "feedback_url": "string?",
  "source_meal_key": "string?",
  "production_date": "...?",
  "price": { "student": 2.9, "staff": 4.1, "guest": 5.2, "currency": "€" },
  "components": [ { "name": "Reis", "codes": [ { "code": "...", "name": "...", "description": "..." } ] } ],
  "created_at": "...",
  "updated_at": "..."
}
```

Pflichtfelder laut iOS-Decoder: `id, object, canteen{name}, date, title, is_active, last_seen_at, additive_codes, allergen_codes, symbol_codes, flags{alle 8}, components, created_at, updated_at`. Android sollte **toleranter** sein (Standardwerte), damit eine neue API-Version nicht alles bricht. `price` darf fehlen.

### `GET /v1/mensa/canteens`

Antwort: Listen-Hülle mit `data: [Canteen]`.

```json
{
  "object": "canteen", "rsc_id": 1, "name": "Mensa Ehrenberg", "has_menu": true,
  "city": "Ilmenau", "address": "...", "latitude": 0.0, "longitude": 0.0,
  "seating_capacity": 0, "info_page_url": "...",
  "opening_hours": [ { "weekday": "mon", "opens_at": "11:00", "closes_at": "14:00" } ],
  "notice": { "raw_text": "...", "notice_type": "...", "parsed_from": "...", "parsed_until": "..." }
}
```

`weekday` ist `mon|tue|wed|thu|fri|sat|sun`. Ein fehlender Wochentag heißt „geschlossen“. Zeiten als `HH:mm`.

### `GET /v1/mensa/codes`

Nur in Kommentaren genannt (Kategorien `allergen`, `additive`), **nicht aufgerufen**. Quelle der festen Listen.

## Abbildung auf das App-Modell (`MensaMeal`)

| App-Feld | Quelle |
|---|---|
| `category` | `meal_group`, sonst `"Gericht"` |
| `name` | `title` |
| `description` | Namen der `components`, mit „, “ verbunden (`null`, wenn leer) |
| `price` | `student`, `staff`, `guest`, `currency` (Standard „€“) |
| `allergies` | `allergen_codes[].name` (ohne `null`) |
| `additives` | `additive_codes[].name` |
| `categories` | `symbol_codes[].name` |
| `badges` | „Vegan“ wenn `flags.vegan`; „Vegetarisch“ wenn `flags.vegetarian`; „Bio“ wenn irgendein `symbol_codes[].code` (lowercase) „bio“ enthält |
| `components` | `name` + `codes[].name` |
| `canteenName` | `canteen.name` |
| `date` | `date` |

## Fachregeln

- **Venue-Zuordnung:** `canteen.name` getrimmt und case-insensitiv gleich dem API-Namen (siehe [feature_specification.md](../../../.migration/feature_specification.md), Abschnitt 3.2).
- **Abendessen:** `meal_group` enthält „abend“ (case-insensitiv).
- **Preisformat:** `formatted(currency)`: 3-stelliger Währungscode → Code in Großbuchstaben, sonst `EUR`. Locale `de_DE`. Fehlender Preis „–“.
- **Gericht-IDs** gelten nur für einen Tag. Es gibt keine Einzelabfrage.
- **Zeitlimit:** Heute/Widget laden mit Zeitlimit, Fehler/Zeitablauf ergeben „keine Daten“, nie ein hängender Screen.

## Fehler

`{ "error": { code, message } }` → `apiError(code, message)`; sonst `httpError(status)`; Dekodierfehler → `decodingError`.

## Android-Umsetzung (M3)

- `MensaApi` (Retrofit/Ktor), DTOs mit Standardwerten, Mapper `MealDto → MensaMeal`.
- `MensaRepository` mit `suspend fun speiseplan(tag: LocalDate)` und `mensen()`; kurzlebiger In-Memory-Cache je Tag.
- Jede Anfrage zustandslos (kein gemeinsames `meals`-Feld).
