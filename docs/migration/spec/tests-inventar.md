# Inventar der iOS-Tests

41 Dateien in `Campus IlmenauTests/` (ca. 11.800 LOC, Swift Testing). Sie dienen als **Testfall-Katalog**: Testfälle werden nach JUnit/Turbine portiert, nicht die Swift-Dateien.

| iOS-Testdatei | Bereich | Meilenstein | Hinweis |
|---|---|---|---|
| `TestClock` | Hilfsmittel | M1 | Android: injizierbare `Clock`/`TestTimeSource` |
| `MensaFilterTests` | Mensa | M3 | Filterlogik |
| `MensaDetailTests` | Mensa | M3 | Chips, Komponenten, Allergene |
| `WidgetGerichtTests` | Mensa/Widget | M3, M10 | Chips-Regeln sind gemeinsam |
| `HeuteModelTests` | Heute | M4 | Phasen, Begrüßung, Wechsel |
| `HeuteAbendTests` | Heute/Events | M4 | |
| `LoadingGateTests` | Heute | M4 | Ladeverhalten/Zeitlimit |
| `EventsTests` | Events | M5 | Filter, Anfrage |
| `EventsAnsichtTests` | Events | M5 | |
| `EventDetailTests` | Events | M5 | |
| `EventsLinkTests` | Events | M5 | Zustellung der Event-Links |
| `BibliothekTests` | Bibliothek | M7a | PAIA, Anzeige |
| `BenutzernummerScannerTests` | Bibliothek | M7b | `BenutzernummerFormat` |
| `LibraryCatalogTests` | Bibliothek | M7c | Query, Validierung, Modelle |
| `LibrarySearchModelTests` | Bibliothek | M7c | Debounce, Fortsetzung |
| `LibraryTitleTests` | Bibliothek | M7c | |
| `BibMerklisteTests` | Bibliothek | M7d | Store, Widerruf, Gefunden |
| `BibliotheksausweisTests` | Bibliothek | M7e | Strichcode, Zugang |
| `ShareTextTests` | Teilen | M8 | |
| `ShareCardTests` | Teilen | M8 | Teilkarten (UI-Test/Screenshot) |
| `PadNavigationTests` | iPad | M9 | auf Window-Size-Classes übertragen |
| `PadRasterTests` | iPad | M9 | |
| `PadMensaTests` | iPad | M9 | |
| `KatalogPadTests` | iPad | M9 | |
| `MerklistePadTests` | iPad | M9 | |
| `MehrPadTests` | iPad | M9 | |
| `ZiegeEinlaufTests` | iPad | M9 | |
| `MensaWidgetTests` | Widget | M10 | |
| `AbendWidgetTests` | Widget | M10 | |
| `WidgetSpeicherTests` | Widget | M10 | |
| `SteuerelementIntentTests` | Widget | M10 | optional |
| `SiriMensaTests` | Siri | M10 | Antwortlogik |
| `SiriAbendTests` | Siri | M10 | |
| `SiriKatalogTests` | Siri | M10 | |
| `SiriKurzbefehleTests` | Siri | M10 | |
| `SiriZielTests` | Siri | M10 | Deeplink-Ziele |
| `MerklisteAbgleichTests` | Watch-Abgleich | später (Wear OS) | nicht in der Erst-Version |
| `UhrEinstellungenTests` | Watch | später (Wear OS) | |
| `UhrSiriTests` | Watch | später (Wear OS) | |
| `UhrWidgetsTests` | Watch | später (Wear OS) | |
| `SmartStackTests` | Smart Stack | entfällt | kein Gegenstück |

Reihenfolge der Portierung: zuerst reine Fachlogik (Mensa, Heute, Events, Katalog-Query, Merkliste, `BenutzernummerFormat`, Code-39-Strichcode, Thoska-Verlauf), danach UI- und Plattformtests.

Noch **nicht** in iOS-Tests abgedeckt, aber auf Android zu testen: `DeepLink`-Parser, Thoska-Parsing (Little-Endian ÷ 1000), `MensaOpeningHours`, `ThoskaHistorySummary`, `EventFilter.anfrage`, Zeitzonen-Grenzfälle (Sommer-/Winterzeit, Mitternacht).
