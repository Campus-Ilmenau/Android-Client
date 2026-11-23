# API: Bibliothekskatalog

Quelle: `Bibliothek/Katalog/LibraryCatalogService.swift`, `LibraryCatalogQuery.swift`, `LibraryCatalogModels.swift`. Ausführliche Beschreibung des Suchverhaltens: **`Suche-API-Analyse.md`** im iOS-Projekt (Stand 2026-11-08). Ohne Anmeldung, nur lesend. Die App spricht nur diese eigene API, nie direkt den UB-Katalog oder den GBV-Gesamtkatalog.

## Endpunkte

### `GET /v1/library/search`

| Parameter | Wert | Gesendet wenn |
|---|---|---|
| `q` | Freitext, getrimmt, max. 200 Zeichen | nicht leer |
| `title`, `author`, `isbn`, `subject` | Feldsuche | gefüllt und `in` ist nicht `local`/`content` |
| `year_from`, `year_to` | vierstellig, 1000–9999, keine führende 0 | gefüllt |
| `type` | `print_book` \| `ebook` \| `journal` \| `all` | gesetzt (UI-Voreinstellung „Gedruckt“ = `print_book`; ohne Angabe gilt `print_book` serverseitig) |
| `in` | `local` \| `catalog` \| `content` | siehe unten |
| `page` | ab 1 | immer |
| `per_page` | `20` | immer |

**Validierung vor dem Senden** (sonst 400): mindestens ein Suchbegriff (bei `local`/`content` nur `q` zählt); `q` ≤ 200; Jahre vierstellig, von ≤ bis, Spanne ≤ 150; `page` ≥ 1. Fehlertexte: „Bitte gib einen Suchbegriff ein.“, „Der Suchbegriff darf höchstens 200 Zeichen lang sein.“, „Bitte das Jahr vierstellig eingeben.“, „„von“ darf nicht nach „bis“ liegen.“, „Höchstens 150 Jahre auf einmal.“

**Quelle der Suche:**
- Freitext ohne `in`: Bestand der UB zuerst, Antwort mit `source` = `local|mixed|catalog`.
- Feldsuche (ohne `in`): läuft im Gesamtkatalog, Antwort ohne `source`.
- `in=catalog`: Gesamtkatalog, Treffer ohne `match`. Erzwingt „Im Gesamtkatalog weitersuchen“, immer `page=1`.
- `in=content`: im Code definiert, **wird nirgends gesetzt**; Inhaltssuche läuft vermutlich serverseitig im Standardfall (`features.contents`).

Antwort:

```json
{ "data": [Media], "total_count": 0, "page": 1, "per_page": 20, "has_more": false,
  "source": "local|mixed|catalog", "corrected_query": "string?",
  "catalog_suggested": false, "catalog_checked": false, "catalog_total": 0, "search_id": "string?" }
```

### `GET /v1/library/availability?ids=a,b,c`

Höchstens **20 IDs**, App dedupliziert. Antwort: `{ "data": [ { "id", "availability_status", "items": [Item] } ] }`. Unbekannte IDs kommen mit Status `unknown`.

### `GET /v1/library/media/{id}`

Query: `search_id` (aus der Suche, wenn man von dort kam), `track=false` beim Neuladen einer offenen Seite und bei der Merkliste. Antwort: Media-Felder plus `items`, `availability_status`, `contents_status`, `contents`.

### `GET /v1/library/features`

`{ "contents": true|false }`. Einmal beim Öffnen des Katalogs, **nicht speichern**.

### Cover

`media.cover.url` ist ein Pfad relativ zur Basis-URL (oder absolut). Abruf nur bei `cover.status` = `found` oder `pending`. Antworten:
- 2xx → Bild.
- 404 `cover_pending` → später erneut, `Retry-After` (Standard 30 s).
- 404 `cover_missing` oder 404 ohne Code → kein Cover.
Loader: bis zu 3 Versuche, Wartezeit `min(Retry-After, 30)`, Bild im Sitzungs-Cache. Android: Coil mit eigenem Interceptor/Fetcher für diese Logik.

## Modelle

IDs: `med_<PPN>`. **Alle Codes** (Medienart, Status, Cover-Status, Quelle, Match-Stufe, Match-Feld, Inhalts-Status) haben einen Auffangwert `other(raw)`/`unknown`, damit neue API-Werte nichts brechen.

| Code | Werte |
|---|---|
| Medienart | `print_book` Buch, `ebook` E-Book, `journal` Zeitschrift, sonst „Medium“ |
| Verfügbarkeit | `available` „Ausleihbar“, `on_loan` „Ausgeliehen [bis TT.MM.]“, `reference_only` „Nur vor Ort“, `online` „Online lesen“, sonst „Status unbekannt“ |
| Cover | `found`, `pending`, `none`, `disabled`, sonst other |
| Match-Stufe | `exact`, `corrected`, `relaxed`, `catalog` |
| Match-Feld | `title`, `author`, `subject`, `abstract`, `toc`, `blurb` |
| Inhalte | `available`, `pending`, `unavailable`, `disabled` |

**Media:** `id, ppn, media_type, title, subtitle, authors[], edition, year, publisher, isbns[], language (MARC), subjects[], rvk[], online_url, catalog_url, abstract, links[{kind,url,mime_type}], fetched_at, cover{status,url,width,height}, match{fields[],stage,snippet}`. Pflicht: `id`, `title`. URLs tolerant lesen (kaputt → `null`).
**Item (Exemplar):** `label` (Signatur), `location{id,code,name,floor_plan_url}`, `status`, `due_date`, `reserve_url`, `queue`.
**Availability:** frühestes Rückgabedatum = Minimum aller `due_date`; Summe der `queue`; Anzahl `available`.
**Contents:** `table_of_contents{text, entries[{number,title,page,level}], source_url}`, `blurb{text}`. Inhalte anzeigen nur bei `contents_status == available` und tatsächlich vorhandenem Inhalt. Nicht dauerhaft speichern.
**Sprache:** MARC-Code → deutscher Name (Tabelle `LibraryLanguage`, ca. 40 Codes, Fallback `Locale`, sonst Großschreibung des Codes).

## Antwort-Hinweise (Header)

`X-Cache-Status: stale` → Hinweis „Katalog antwortet langsam / Verfügbarkeit kann sich geändert haben“ (Stand: `fetched_at`, bei Listen der älteste). `X-Request-Id` bei Problemen mitschicken. `Retry-After` als Sekunden oder HTTP-Datum (Android: beides parsen).

## Fehler

| Status | Bedeutung |
|---|---|
| 400 | `badRequest(param, message)`, sollte durch die Vorprüfung selten vorkommen |
| 404 | Titel nicht gefunden (`cover_*`-Codes gehören zum Cover-Abruf) |
| 503 (auch ohne lesbaren Body) | `catalogUnavailable`, „Katalog gerade nicht erreichbar“, **wiederholbar** (`networkError` ebenfalls) |
| sonst mit Fehlerhülle | `apiError(status, code, message, param)` |
| sonst | `httpError(status)` |

## Weitere Regeln aus `Suche-API-Analyse.md`

- Tippen: 700 ms Debounce, leeres Feld setzt zurück (nichts senden), keine Mindestlänge. Return/„Suchen“ sofort. Medienart-Pill wechselt → sofort neu (falls Begriff da). Erweiterte Suche offen → kein Auto-Suchen. Pull-to-Refresh: dieselbe Anfrage, alte Liste bleibt bis die neue da ist. Neue Suche bricht die laufende ab (Task-Cancel + Generationszähler).
- „Im Gesamtkatalog weitersuchen/suchen“ erscheint bei `catalog_suggested`, bei `source == mixed` (am Listenende), bei 0 Treffern (sofern keine Katalog-/Feldsuche), oder wenn die Suche länger als 900 ms dauert (Hinweistext, rein zeitbasiert). Nie bei Feldsuche und bei `in=catalog`.
- Fortsetzung: letzte Suche mit `in=catalog`, `page=1`; Treffer, die schon gezeigt wurden, nach `id` entfernen; eigener Block „Weitere aus dem Gesamtkatalog“; weitere Seiten mit `in=catalog`, `page=2…`; Kopfzeile „Ergänzt um N Treffer aus dem Gesamtkatalog“; kein Treffer: „Auch im Gesamtkatalog gibt es keine Treffer. Prüfe die Schreibweise.“
- Availability: gebündelt nach 80 ms, höchstens 20 IDs je Aufruf, für sichtbare Karten und die Merkliste.

## Android-Umsetzung (M7c)

- `LibraryApi`, DTOs mit Auffangwerten, `LibrarySearchQuery` als reine Klasse mit `validate()` und `queryItems`, Tests wie `LibraryCatalogTests`, `LibrarySearchModelTests`.
- Suche im `ViewModel` mit `Flow.debounce(700)`, `collectLatest` (ersetzt Generationszähler) – Verhalten per Test absichern.
