# Interner Test in der Play Console

Schritt für Schritt vom Schlüssel bis zum ersten Tester. Die Passwörter und die Keystore-Datei gehören dir allein, sie laufen nicht über Claude und nicht ins Repository.

Angaben der App: Paket `de.kaigothe.campusilmenau`, `versionCode 1`, `versionName 1.0`, `minSdk 26`, `targetSdk 37`. Das Release-Build spricht die Produktion (`https://api.campus-ilmenau.de/`), das Debug-Build den Dev-Stand.

## 1. Upload-Schlüssel anlegen (einmalig)

```
keytool -genkeypair -v -keystore ~/campus-upload.jks -alias campus-upload -keyalg RSA -keysize 4096 -validity 10000
```

- Zwei Passwörter wählen (Keystore und Schlüssel, dürfen gleich sein) und im Passwortmanager ablegen.
- Die `.jks`-Datei an einem zweiten Ort sichern (verschlüsseltes Backup). Nicht ins Repository: `.gitignore` sperrt `*.jks` und `*.keystore`.
- Geht der Schlüssel verloren, lässt er sich in der Play Console zurücksetzen, solange Play App Signing aktiv ist. Das dauert aber Tage.

## 2. Schlüsseldaten für Gradle eintragen

In `~/.gradle/gradle.properties` (nicht im Projekt):

```
CAMPUS_KEYSTORE_FILE=/Users/<du>/campus-upload.jks
CAMPUS_KEYSTORE_PASSWORD=...
CAMPUS_KEY_ALIAS=campus-upload
CAMPUS_KEY_PASSWORD=...
```

## 3. App-Bundle bauen

```
./gradlew :app:bundleRelease
```

Ergebnis: `app/build/outputs/bundle/release/app-release.aab`. Prüfen, dass es signiert ist:

```
jarsigner -verify -verbose app/build/outputs/bundle/release/app-release.aab | head
```

Für jeden weiteren Upload `versionCode` in `app/build.gradle.kts` um 1 erhöhen. Play lehnt einen bereits benutzten Wert ab.

## 4. Konto und App in der Play Console

1. Entwicklerkonto anlegen (einmalig 25 $, Identitätsprüfung dauert einige Tage). Privatpersonen müssen vor der ersten Produktionsfreigabe zusätzlich einen geschlossenen Test mit mindestens 12 Testern über 14 Tage durchführen. Der interne Test ist davon nicht betroffen. Aktuelle Regeln in der Console nachlesen, sie ändern sich.
2. **App erstellen:** Name „Campus Ilmenau“, Standardsprache Deutsch, App (nicht Spiel), kostenlos.
3. **Play App Signing** akzeptieren (Standard). Google hält den App-Signaturschlüssel, dein Schlüssel aus Schritt 1 ist der Upload-Schlüssel.

## 5. Pflichtangaben (auch für den internen Test nötig)

Unter „App-Inhalte“ und „Dashboard“:

- **Datenschutzerklärung:** öffentliche URL. Die Texte dafür stehen in [datenschutz-und-policy.md](datenschutz-und-policy.md).
- **Datensicherheit:** Formular nach demselben Dokument ausfüllen. Keine Analyse- und Absturz-SDKs, Anmeldedaten nur auf Wunsch und verschlüsselt auf dem Gerät.
- **Zielgruppe:** nicht an Kinder gerichtet.
- **Werbung:** keine.
- **Berechtigungen und Funktionen:** Kamera (nur zum Einlesen der Benutzernummer), NFC.
- **Store-Eintrag:** Texte aus [store-eintrag.md](store-eintrag.md), dazu Symbol, Feature-Grafik und Screenshots. Für den internen Test reichen vorläufige Platzhalter, für die Produktion nicht.

## 6. Internen Test einrichten

1. **Test und Veröffentlichung → Tests → Interner Test → Neuen Release erstellen.**
2. `app-release.aab` hochladen.
3. Versionshinweise eintragen (de und en), zum Beispiel aus `CHANGELOG.md`.
4. **Tester:** Liste mit E-Mail-Adressen der Google-Konten anlegen und dem Test zuweisen.
5. Release prüfen und **für den internen Test freigeben**.
6. Den **Opt-in-Link** an die Tester schicken. Sie öffnen ihn mit dem Konto aus der Liste, nehmen teil und installieren die App im Play Store.

Ein interner Test ist meist innerhalb von Minuten bis wenigen Stunden verfügbar, ohne Prüfung durch Google.

## 7. Was die Tester prüfen sollen

- Heute, Mensa (Filter, Teilen), Events (Merken), Bibliothek (Anmelden, Verlängern, Katalog, Merkliste, Ausweis mit Biometrie).
- Sprachwechsel Deutsch und Englisch, Hell und Dunkel, Tablet.
- Widgets und Kurzbefehle auf dem Startbildschirm.
- Rückmeldungen mit Gerät und Android-Version. Abstürze zeigt die Play Console unter „Android Vitals“, ohne eigenen Code.

## Reihenfolge, die am wenigsten Wartezeit kostet

Entwicklerkonto zuerst beantragen (Prüfung dauert), währenddessen Schlüssel anlegen, Bundle bauen und die Datenschutz-URL bereitstellen.
