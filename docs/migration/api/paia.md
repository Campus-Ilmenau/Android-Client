# API: PAIA (UB Ilmenau)

Quelle: `ServicesPAIAService.swift`, `ModelsPAIALoanItem.swift`, `ModelsPAIANotification.swift`, `Bibliothek/Ausweis/PAIAPatron.swift`. Spezifikation: <https://gbv.github.io/paia/paia.html>

**Basis-URL:** `https://paia.gbv.de/DE-Ilm1` (fest im Code, nicht umgebungsabhängig).

## Sitzung

- **Nichts wird dauerhaft gespeichert.** Token, Patron-ID, Ablaufzeit und Benutzernummer liegen nur im Speicher der Service-Instanz (`PAIAService.shared`, damit die Sitzung Navigationswechsel überlebt). Das Login-Sheet sagt ausdrücklich: „Benutzernummer und Passwort werden nicht gespeichert – nach einem Neustart der App meldest du dich neu an.“
- **Android-Erweiterung:** optional gespeichertes Passwort (Auto-Login) – siehe [02-entscheidungen.md](../02-entscheidungen.md), Entscheidung 5, und `M07`.
- Sitzung endet: manuell (Abmelden), bei **401/403** der Antwort, oder lokal, wenn `expires_in` abgelaufen ist (`tokenExpiryDate = jetzt + expires_in`). Bei Ablauf: Zustand leeren, Flag `sessionExpired = true` (Titel/Hinweis im Login-Sheet), Fehler „Sitzung abgelaufen, bitte erneut anmelden“. Erfolgreicher Login setzt das Flag zurück.
- Beim Beenden werden `items`, `notifications` und `patron` geleert.

## Endpunkte

### `POST /auth/login`

`Content-Type: application/x-www-form-urlencoded`, Body (Resource-Owner-Password-Grant):

```
grant_type=password
username=<Benutzernummer>
password=<Passwort>
scope=read_patron read_fees read_items write_items read_notifications
```

Erfolg: `{ "access_token", "token_type", "patron": "string?", "scope": "string?", "expires_in": int? }`.
Fehler: `{ "error": "string", "error_description": "string?" }` → Meldung = `error_description ?? error` (Typ `authenticationFailed`). Sonst `httpError(status)`.

### `GET /core/{patron}/items`

`Authorization: Bearer <access_token>`. `{patron}` aus der Login-Antwort, prozent-kodiert (`urlPathAllowed`). Antwort `{ "doc": [Item] }`.

**Item:** `item` (URI, optional), `edition`, `about`, `label`, `queue`, `renewals`, `reminder`, `starttime`, `endtime`, `cancancel`, `canrenew`, `storage`, `storageid`, `comment`, `status` (Pflicht, Int 0–5).
**ID im UI:** `item ?? about ?? <zufällig>`.
**Status:** 0 „Kein Status“, 1 „Vorgemerkt“, 2 „Bestellt“, 3 „Ausgeliehen“, 4 „Bereitgestellt“, 5 „Abgelehnt“.

### `GET /core/{patron}/notifications`

Antwort `{ "notification": [ { "id", "about", "date", "item?", "url?" } ] }`. Der Endpunkt ist laut Spezifikation optional: **404 → leere Liste, kein Fehler** (er darf die Fehlermeldung der Ausleihen nicht überschreiben).

### `GET /core/{patron}`

Konto für den Ausweis: `name`, `expires`, `status` (Int), `type` (Array von URIs), `note`. `email` und `address` werden **bewusst nicht gelesen**. Fehler setzen keine `errorMessage` (der Ausweis funktioniert auch ohne). Wurde währenddessen abgemeldet, dürfen die Daten nicht zurückkehren.

### `POST /core/{patron}/renew`

`Content-Type: application/json`, Body `{ "doc": [ { "item": "<URI>" } ] }`. Antwort `{ "doc": [ { "item?", "endtime?", "status?", "error?" } ] }`. Ergebnis zum Item suchen (`item == URI`, sonst erstes). `error` gesetzt → „Verlängerung fehlgeschlagen: <error>“. Erfolg: Eintrag lokal mit neuem `endtime`, `status`, `renewals + 1` ersetzen.

## Datumsformate

Vollständiges ISO 8601 (ohne Millisekunden) **oder** reines Datum `yyyy-MM-dd` (UTC). Beides akzeptieren.

## Fehler (`PAIAError`)

| Fall | Text |
|---|---|
| invalidURL | „Ungültige PAIA-Server-URL“ |
| invalidResponse | „Ungültige Server-Antwort“ |
| httpError(status) | „HTTP-Fehler: …“ |
| authenticationFailed(msg) | die Meldung des Servers |
| notAuthenticated | „Nicht angemeldet“ |
| sessionExpired | „Sitzung abgelaufen, bitte erneut anmelden“ |
| renewalFailed(msg) | „Verlängerung fehlgeschlagen: …“ |
| missingItemIdentifier | „Dieses Medium kann nicht verlängert werden“ |

`sessionExpired` wird nicht als normale Fehlermeldung gespeichert (Login-Sheet übernimmt).

## Offene Punkte

- **Nutzergruppen-URIs** (`type[]` → „Studierende“, „Beschäftigte“, „Externe“): Tabelle im iOS-Code **leer** (TODO). Bei der UB erfragen oder aus einer echten Antwort ablesen. Bis dahin entfällt der Chip auf dem Ausweis.
- Beispielantworten (anonymisiert) für Tests sichern.

## Android-Umsetzung (M7a)

- `PaiaApi` (form-encoded Login, JSON für renew), `PaiaSession` als Singleton (Hilt) mit `StateFlow` für Zustand und `Clock` für Ablauf.
- Optionaler Auto-Login: Passwort verschlüsselt (Android Keystore), nur auf ausdrücklichen Wunsch, beim Abmelden und in „Gespeicherte Daten löschen“ entfernen, aus Backups ausschließen.
- Keine Zugangsdaten in Logs, `Authorization`-Header im HTTP-Logging schwärzen.
