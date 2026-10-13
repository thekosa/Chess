# Szachy

Projekt rozpoczęty na zajęciach JTP. Pierwszy etap rozwoju wydziela silnik gry
od interfejsu JavaFX.

## Uruchamianie

Wymagany jest JDK 21 i ustawione `JAVA_HOME`. Maven Wrapper pobiera narzędzie
budowania, a Maven pobiera JavaFX oraz zależności testowe przy pierwszym użyciu.

Windows (PowerShell):

```powershell
.\mvnw.cmd install
.\mvnw.cmd -pl desktop javafx:run
```

Linux / macOS:

```sh
sh ./mvnw install
sh ./mvnw -pl desktop javafx:run
```

W IntelliJ IDEA należy otworzyć główny `pom.xml` jako projekt Maven i wybrać JDK 21.

## Podział projektu

- `core` — stan partii i publiczne API, bez zależności od JavaFX, Swinga lub AWT.
- `desktop` — interfejs JavaFX, zależny od API silnika.
- `docs/architecture.md` — kontrakt i zasady dalszego rozwoju.

Aktualnie aplikacja pokazuje poprawną pozycję początkową (32 figury) i pozwala
rozpocząć nową grę. Wszystkie dane planszy pochodzą z silnika.
Ruchy, ich podświetlanie, zmiana kolejki, bicie i pozostałe zasady szachowe
będą dodawane w kolejnych etapach. Zapis i wczytywanie partii również pozostają
do zaimplementowania.

Sam silnik można zbudować i sprawdzić bez pobierania JavaFX:

```powershell
.\mvnw.cmd -pl core verify
```

Dodatkowy test integracji JavaFX (na komputerze z dostępem do środowiska
graficznego) ładuje FXML, grafiki i sprawdza odświeżanie widoku na podstawie API:

```powershell
.\mvnw.cmd -Dchess.gui.tests=true verify
```

Test nie otwiera okna; zapisuje podgląd planszy w
`desktop/target/board-preview.png`.
