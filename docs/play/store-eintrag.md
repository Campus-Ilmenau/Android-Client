# Play-Store-Eintrag (Entwurf)

Entwurf für die Store-Seite. Die Texte beschreiben nur, was die App kann (gegen die Strings der App geprüft, außer den Angaben zu Events und Tastenkürzeln, die aus den Milestones stammen). Grenzen der Play Console: Titel 30 Zeichen, Kurzbeschreibung 80 Zeichen, ausführliche Beschreibung 4000 Zeichen.

Vor dem Veröffentlichen prüfen: Ob die App als „inoffiziell“ oder im Auftrag der Universität erscheint, entscheidest du. Der Text unten macht dazu keine Aussage. Der Wortlaut zum Datenschutz steht in [datenschutz-und-policy.md](datenschutz-und-policy.md).

## Deutsch (Standardsprache)

**Titel (18 Zeichen):** Campus Ilmenau

**Kurzbeschreibung (77 Zeichen):**
Mensa, Bibliothek und Veranstaltungen in Ilmenau – alles in einer App.

**Ausführliche Beschreibung:**

Campus Ilmenau bündelt, was du im Studienalltag in Ilmenau brauchst: den Speiseplan, dein Bibliothekskonto und die Veranstaltungen.

HEUTE
Die Startseite zeigt, was jetzt zählt: ob die Mensa gerade geöffnet ist, was es zum Mittag gibt und wo du am Abend essen kannst.

MENSA
• Speisepläne der Mensen mit Preisen für Studierende, Bedienstete und Gäste
• Filter nach Ernährungsform und Allergenen
• Details zu jedem Gericht
• Gericht oder ganzen Tagesplan als Text oder als Bild teilen

BIBLIOTHEK
• Anmelden mit deiner Benutzernummer, Ausleihen und Vormerkungen ansehen
• Medien mit einem Tipp verlängern
• Mitteilungen der Bibliothek
• Katalogsuche mit Covern und Verfügbarkeit
• Merkliste, die auch ohne Anmeldung funktioniert
• Bibliotheksausweis als Strichcode, geschützt durch Fingerabdruck, Gesicht oder Geräte-PIN
• Benutzernummer per Kamera vom Strichcode deiner Thoska einlesen
• Optional „Angemeldet bleiben“: Das Passwort wird nur verschlüsselt auf deinem Gerät gespeichert

EVENTS
Veranstaltungen ansehen, merken und teilen.

AUF DEM STARTBILDSCHIRM
• Widgets für die Mensa heute und den Abend
• Kurzbefehle für Mensa, Abend, Katalog, Merkliste und Öffnungszeiten

FÜR JEDES GERÄT
Die App passt sich Telefon, Tablet und Faltgerät an, kennt Tastenkürzel am Tablet und folgt dem hellen oder dunklen Design. Verfügbar auf Deutsch und Englisch. Es gibt wahlweise ein dunkles App-Symbol.

DATENSCHUTZ
Keine Werbung, keine Analyse- und Absturz-Dienste. Dein Bibliothekspasswort wird nur gespeichert, wenn du „Angemeldet bleiben“ aktivierst, und nur verschlüsselt auf dem Gerät. Die Kamera wird nur zum Einlesen der Benutzernummer verwendet, es werden keine Bilder gespeichert.

## English

**Title:** Campus Ilmenau

**Short description (68 characters):**
Canteen, library and events in Ilmenau, all in one app.

**Full description:**

Campus Ilmenau brings together what you need for student life in Ilmenau: the canteen menu, your library account and events.

TODAY
The home screen shows what matters right now: whether the canteen is open, what is for lunch and where you can eat tonight.

CANTEEN
• Menus of the canteens with prices for students, employees and guests
• Filter by diet and allergens
• Details for every dish
• Share a dish or the whole day's menu as text or as an image

LIBRARY
• Sign in with your user number, see loans and reservations
• Renew items with one tap
• Library notifications
• Catalogue search with covers and availability
• A reading list that works without signing in
• Library card as a barcode, protected by fingerprint, face or device PIN
• Scan your user number from the barcode on your Thoska with the camera
• Optional "Stay signed in": the password is stored only in encrypted form on your device

EVENTS
Browse, save and share events.

ON YOUR HOME SCREEN
• Widgets for today's canteen and tonight
• Shortcuts for canteen, tonight, catalogue, reading list and opening hours

FOR EVERY DEVICE
The app adapts to phones, tablets and foldables, supports keyboard shortcuts on tablets and follows the light or dark theme. Available in German and English. A dark app icon is available as an option.

PRIVACY
No ads, no analytics and no crash reporting services. Your library password is stored only if you turn on "Stay signed in", and only encrypted on the device. The camera is used only to read the user number, and no images are saved.

## Grafiken

| Element | Vorgabe | Stand |
|---|---|---|
| App-Symbol | 512 × 512 px PNG | Aus dem adaptiven Symbol exportieren (`ic_launcher`), nicht im Repository |
| Feature-Grafik | 1024 × 500 px | offen |
| Telefon-Screenshots | mindestens 2, 16:9 bis 9:16 | offen |
| Tablet-Screenshots (7 und 10 Zoll) | empfohlen, sonst stuft Play die App bei großen Bildschirmen herab | offen |

Vorschlag für die Screenshots: Heute, Mensa, Gericht teilen, Bibliothek mit Ausleihen, Katalogsuche, Events, Widgets auf dem Startbildschirm, Tablet mit Seitenleiste. Die Emulator-Größen aus der Tablet-Prüfung (`adb shell wm size`) lassen sich dafür wiederverwenden.

## Formulare in der Play Console

- **Kategorie:** Bildung oder Lifestyle (deine Wahl).
- **Zielgruppe:** nicht an Kinder gerichtet.
- **Datensicherheit:** siehe [datenschutz-und-policy.md](datenschutz-und-policy.md).
- **Datenschutzerklärung:** eine öffentlich erreichbare URL ist nötig, weil die App Zugangsdaten verarbeitet. Du musst sie bereitstellen.
- **Kontakt:** eine E-Mail-Adresse, die in der Store-Seite öffentlich erscheint. Nimm bewusst eine eigene Adresse, nicht die private.
