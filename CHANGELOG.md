# Changelog

Alle Änderungen der Android-App „Campus Ilmenau“. Das Format folgt [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
die Versionen folgen [Semantic Versioning](https://semver.org/lang/de/).

## [Unreleased]

### Hinzugefügt
- **Heute:** Startseite mit Jetzt-Karte (Mensa nach Tageszeit), „Als Nächstes“, „Heute Abend“ und Hinweisen.
- **Mensa:** Speiseplan aller Mensen mit Tagesleiste, Filter (Ernährung, Bio, Allergene), Gericht-Detail mit allen Preisgruppen,
  Favoriten, Öffnungszeiten.
- **Thoska:** Guthaben und letzte Buchungen per NFC auslesen, Verlauf, Kartennamen (nur auf dem Gerät).
- **Bibliothek:** Anmeldung mit PAIA (Passwort optional verschlüsselt im Keystore), Ausleihen und Vormerkungen, Verlängern,
  Mitteilungen, Bibliotheksausweis mit Code 39 hinter der Biometrie, Benutzernummer per Kamera scannen.
- **Katalog:** Suche im Bestand und im Gesamtkatalog, Feldsuche, Treffer mit Verfügbarkeit und Cover, Titelansicht.
- **Merkliste:** lokal, mit Rückgängig, „gefunden“ und Verfügbarkeit.
- **Events:** Liste mit Tagesleiste, Filter, Merken, Detail, Kalender-Eintrag, Karte, vergangene Veranstaltungen.
- **Teilen:** Gericht, Speiseplan und Veranstaltung als Bild oder Text, Medium als Text.
- **Mehr:** Darstellung (System, Hell, Dunkel), Sprache (Deutsch, Englisch), Mensa-Einstellungen, alternatives App-Icon (Dunkel),
  gespeicherte Daten einsehen und löschen, Datenschutz und Impressum.
- **Tablets und Faltgeräte:** Navigation Rail bzw. Seitenleiste, Liste neben Detail, mehrspaltige Seiten, Tastenkürzel.
- **Widgets:** „Mensa heute“ und „Heute Abend“ (Glance), Aktualisierung alle 30 Minuten.
- **App-Shortcuts:** Mensa heute, Heute Abend, Katalog, Merkliste, Öffnungszeiten (und Thoska auf Geräten mit NFC).
- **Deeplinks:** `campusilmenau://…` für alle Ziele (Widgets, Shortcuts, Browser).

### Sicherheit
- Nur HTTPS (`network_security_config`), keine Klartext-Verbindungen.
- Zugangsdaten der Bibliothek verschlüsselt im Android Keystore, nie in Logs und nicht in der Cloud-Sicherung.
- Datenbank und Zugangsdaten sind von der Sicherung ausgenommen (`data_extraction_rules`, `backup_rules`).
