# API: Events

Quelle: `Shared/EventsService.swift`, `EventFilter.swift`, `CampusEvent.swift`, `EventKalender.swift`. Lesend, ohne Anmeldung. Modelle nach echten Antworten der Dev-API (Stand 5.11.2026).

## Endpunkte

### `GET /v1/events`

| Parameter | Wert | Hinweis |
|---|---|---|
| `from` | `yyyy-MM-dd` (**UTC-Tag**) | Beginn des Zeitraums |
| `to` | `yyyy-MM-dd` (**UTC-Tag**) | Ende; die App sendet Folgetag von `bis` |
| `date` | `yyyy-MM-dd` (**Berliner Tag**) | einzelner Tag |
| `evening` | `true` | „Heute Abend“ (ab 17 Uhr; ganztägige entfallen), nur zusammen mit `date` |
| `organizer` | Veranstalter-**ID** | Name führt zu 400 `invalid_parameter_format` |
| `category` | `Veranstaltung` \| `Frist` | case-insensitiv |
| `include_cancelled` | `true` \| `false` | **immer senden**, sonst entscheidet die Server-Voreinstellung |
| `page` | ab 1 | |
| `per_page` | `100` | |

Antwort: `{ "data": [Event], "has_more": bool, "total_count": int, "page": int }`. Fehlende Felder: `has_more` = false, `total_count` = `data.size`, `page` = 1.

### `GET /v1/events/{id}`

Einzelnes Event. `id` als Pfadteil kodieren. **404 → „Veranstaltung nicht gefunden“**.

### `GET /v1/events/organizers`

`{ "data": [ { "id": "cafe-bohne", "name": "…", "upcoming_event_count": 3 } ] }`. Die App zeigt nur Veranstalter mit `upcoming_event_count > 0`, alphabetisch (`localizedStandardCompare`, Android: `Collator` für de).

## Event

```json
{
  "id": "string", "title": "string",
  "category": "VERANSTALTUNG | FRIST",
  "start_at": "2026-11-05T18:00:00.000Z", "end_at": "...?", "all_day": false,
  "status": "scheduled | tentative | cancelled",
  "location": "string?", "location_address": "string?",
  "organizer": { "id": "string?", "name": "string?" },
  "description": "string? (reiner Text mit Absätzen)",
  "image_url": "string?", "source_url": "string?", "ticket_url": "string?"
}
```

Pflicht: `id`, `title`, `start_at` (parsebar). Alles andere ist optional, **fehlende oder kaputte Felder sind kein Fehler**:

- Texte werden getrimmt, leer → `null`.
- URLs nur `http`/`https` mit Host, sonst `null`.
- `category`: erste Stelle groß, Rest klein („FRIST“ → „Frist“).
- `status`: `tentative`/`unconfirmed` → unbestätigt; `cancelled`/`canceled` → abgesagt; alles andere (auch fehlend) → findet statt.
- `veranstalterAnzeigen`: `null`, wenn leer oder gleich dem Ort (case-insensitiv).

## Zeit- und Filterlogik

Alle Tage in `Europe/Berlin`.

**Zeitfilter** (Reihenfolge):
1. `heuteAbend` → `date=<Tag oder heute>`, `evening=true` (nicht mit Zeitraum kombinierbar).
2. sonst `tag` → `date=<Tag>`.
3. sonst Zeitraum → `from=<UTC-Datum von>`, optional `to=<UTC-Datum von (bis + 1 Tag)>`. Ohne Zeitraum: ab heute 00:00 Berliner Zeit.

**Fenster verbreitern und zuschneiden:** `from`/`to` zählen in UTC-Tagen, Berliner Mitternacht liegt am Vortag um 22/23 Uhr UTC. Die App fragt breiter und schneidet danach auf Berliner Tage zu (`beginn >= vonGrenze`, `beginn < bisGrenze`).

**Laufende mehrtägige Veranstaltungen:** Bei „ab Tag X“ und **Seite 1** zusätzlich eine Abfrage `date=<X>` mit den gemeinsamen Parametern und `per_page=100`. Behalten werden Events mit `beginn < X` und `läuftAm(X)`. Ergebnis wird mit der Liste vereinigt (nach `id` dedupliziert) und nach `beginn` sortiert. Schlägt diese Abfrage fehl, bleibt die Liste ohne sie (kein Fehler).

**Letzter Tag eines Events** (`letzterTag`), Berliner Tage:
- Kein Ende oder Ende ≤ Beginn → Starttag.
- Ende genau um Mitternacht (exklusives Ende, auch bei ganztägigen) → Vortag des Endes (mindestens Starttag).
- Ende am Starttag → Starttag.
- Ende am Folgetag bis 06:00 → noch Starttag (Nachtveranstaltung).
- sonst Tag des Endes.
- `läuftAm(tag)`: `tagesbeginn ≥ ersterTag` und `≤ letzterTag`. `istMehrtaegig`: `letzterTag > ersterTag`. `istFrist`: Kategorie „frist“.

## Fehler

404 → `notFound`. Fehlerhülle `{error:{code,message,param}}` → `apiError`. Sonst `httpError`. Abbruch → kein Fehler.

## Android-Umsetzung (M5)

- `EventsApi`, DTOs (tolerant), Mapper → `CampusEvent`.
- `EventFilter.anfrage(jetzt)` als reine Funktion mit `Clock`, Tests wie `EventsTests`/`EventsAnsichtTests`.
- Zeit: `java.time` mit `ZoneId.of("Europe/Berlin")`, `OffsetDateTime`-Parser für beide ISO-Varianten.
