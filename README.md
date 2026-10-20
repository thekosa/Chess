# Szachy

Projekt rozpoczęty na zajęciach JTP. Gra dla dwóch osób lub przeciwko komputerowi,
z niezależnym silnikiem oraz interfejsami JavaFX, ASCII w terminalu i React w przeglądarce.

## Uruchamianie

Wymagany jest JDK 21 i ustawione `JAVA_HOME`. Maven Wrapper pobiera narzędzie
budowania, a Maven pobiera JavaFX, JLine, Gson oraz zależności testowe przy pierwszym użyciu.

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

W IntelliJ IDEA należy otworzyć główny `pom.xml` jako projekt Maven i wybrać
JDK 21. Po zaimportowaniu zależności można wybrać konfigurację **Szachy** na
górnym pasku i nacisnąć zieloną strzałkę (lub `Shift+F10`). Konfiguracja
jest zapisana w `.run/Szachy.run.xml` i przed uruchomieniem kompiluje projekt.

Na początku pojawia się okno wyboru trybu: **Graficzny**, **Konsola ASCII**
oraz **Web**. Tryb graficzny otwiera obecną planszę, konsolowy uruchamia
osobne okno terminala, a webowy otwiera grę w domyślnej przeglądarce.
Konfiguracje IntelliJ **Szachy ASCII** i **Szachy Web** od razu wybierają
odpowiedni tryb. Po dodaniu modułów `console`, `web` i `computer` należy odświeżyć
projekt Maven w IntelliJ (**Reload All Maven Projects**).

Można też otworzyć `desktop/src/main/java/pl/projekt/szachy/desktop/Launcher.java`
i kliknąć zieloną strzałkę obok metody `main`. Uruchomienie w IDE korzysta
bezpośrednio z modułów projektu; nie wymaga wcześniejszego `mvn install`.

Deskryptory `module-info.java` pozwalają IntelliJ uruchomić aplikację na
ścieżce modułów Java wraz z JavaFX. Po zmianie konfiguracji należy odświeżyć
projekt Maven w IDE.

## Podział projektu

- `core` — stan partii i publiczne API, bez zależności od JavaFX, Swinga lub AWT.
- `computer` — wspólna obsługa przeciwnika Stockfish, ustawień partii i obliczeń w tle.
- `console` — rysowane figury ASCII, kolory i animacje; korzysta z silnika i JLine.
- `web` — adapter HTTP silnika i gotowy frontend React; działa również bez JavaFX.
- `desktop` — interfejs JavaFX oraz okno wyboru trzech trybów.
- `docs/architecture.md` — kontrakt i zasady dalszego rozwoju.

## Gra

Zaczynają białe. Kliknij figurę aktywnej strony, a następnie podświetlone
pole. Kropki oznaczają puste pola docelowe, a złota ramka przy figurze
przeciwnika oznacza możliwe bicie. Kolejne ruchy wykonują naprzemiennie
białe i czarne. Przy promocji pionka wybierz hetmana, wieżę, gońca lub skoczka.

Silnik sprawdza ruchy wszystkich figur, przeszkody, bicie, bezpieczeństwo
króla, roszadę, bicie w przelocie oraz promocję. Rozpoznaje szacha, mata i pata.
Interfejs wyświetla historię ruchów, zbite figury i wynik zakończonej partii.
„Nowa gra” pozwala wybrać przeciwnika i resetuje pozycję, kolejkę, historię, zbicia i zaznaczenie.
Wszystkie dane planszy i decyzje o legalności ruchu pochodzą z silnika.

Figury przesuwają się płynnie przez około 300 ms, a zbita figura kurczy się
i zanika. Animacje obejmują również roszadę, bicie w przelocie i promocję.
Podczas animacji kliknięcia planszy są wstrzymane. Zatwierdzenie nowej gry
przerywa animację i resetuje widok.

## Gra z komputerem

W oknie startowym wybierz **Komputer — Stockfish**, poziom **Łatwy**, **Średni**
lub **Trudny** oraz grę białymi albo czarnymi. Ustawienia dotyczą wszystkich
interfejsów: JavaFX, ASCII i Web. Zmiana przeciwnika lub poziomu rozpoczyna nową partię.
W oknie i przeglądarce służy do tego przycisk **Nowa gra / Nowa partia**.

Przy pierwszym ruchu komputera aplikacja automatycznie pobiera oficjalny
Stockfish 19 (około 80 MB) i sprawdza SHA-256. Później silnik działa lokalnie,
bez internetu, konta i płatnego API. Obliczenia oraz pobieranie odbywają się
w tle. Przy niepowodzeniu można ponowić próbę lub rozpocząć inną partię.
Poziomy zmieniają siłę oraz czas namysłu; nie oznaczają konkretnych rankingów Elo.
Nawet najniższy poziom Stockfisha może stanowić wyzwanie dla początkujących.

Obsługiwane automatycznie: Windows i Linux x64/ARM64 oraz macOS Intel/Apple Silicon.
W projekcie cache jest w ignorowanym `.tools/stockfish`; dla przeniesionej aplikacji
w `~/.cache/szachy/stockfish`. Własny plik silnika można wskazać zmienną
`CHESS_STOCKFISH_PATH`. Szczegóły: [docs/stockfish.md](docs/stockfish.md).

Zapis i wczytywanie, cofanie ruchów oraz dodatkowe reguły remisów
(powtórzenia, licznik posunięć i niewystarczający materiał) pozostają
do kolejnych etapów.

## Wersja webowa

Uruchom **Szachy**, a następnie wybierz **Graj w przeglądarce**. Można też
użyć konfiguracji **Szachy Web**. Aplikacja uruchomi lokalny serwer na wolnym
porcie i otworzy jego adres w przeglądarce. Pozostaw aplikację Szachy otwartą
podczas gry — jej zamknięcie zatrzymuje serwer.

Gotowy frontend jest zapisany w `web/src/main/resources/public`, dlatego
do uruchomienia z IntelliJ wystarczą JDK i zależności Maven. Node.js i npm
są potrzebne dopiero przy zmianach w kodzie React.

Kliknij figurę, a potem podświetlone pole. Dostępne są animacje ruchów i bić,
wybór promocji, historia, lista zbitych figur, obracanie planszy i nowa partia.
Reguły oraz stan należą do silnika na serwerze. Interfejs pobiera stan i legalne
ruchy przez API HTTP; sam nie podejmuje decyzji o legalności posunięcia.

Partia jest przywracana po odświeżeniu tej samej karty, dopóki serwer działa
i sesja nie wygasła (2 godziny bezczynności). Nowa karta ma własną partię.
Zapis jest w pamięci serwera; jego restart usuwa partie. Można grać
z komputerem lub we dwoje w jednej przeglądarce. Pokoje i gracze z różnych komputerów
pozostają do następnego etapu.

Serwer można uruchomić bez JavaFX, z głównego katalogu repozytorium:

```powershell
.\mvnw.cmd -pl web -am -Dmaven.test.skip=true package
java -jar web/target/chess-web-1.0-SNAPSHOT.jar --port=8080
```

Następnie otwórz `http://127.0.0.1:8080/`. `--port=0` wybiera wolny port
i wypisuje adres. Przy przenoszeniu pliku JAR zachowaj katalog `web/target/lib`.
Obecna konfiguracja nasłuchuje wyłącznie lokalnie na `127.0.0.1`.

### Zmiany w React

Kod interfejsu znajduje się w `web/frontend`. Wymagany jest Node.js 22.12+
lub 20.19+ oraz npm. Instalacja i zbudowanie gotowych plików:

```powershell
cd web/frontend
npm ci
npm run build
```

Budowanie aktualizuje `web/src/main/resources/public`. Te pliki należy
zachować razem ze źródłami, aby zielona strzałka w IntelliJ działała bez npm.
Po zmianach odśwież zasoby/uruchom ponownie aplikację. Do pracy z odświeżaniem
na żywo uruchom osobno serwer Java na porcie 8080 i `npm run dev`.
Vite przekazuje żądania `/api` do tego serwera.

Kontrakt HTTP jest opisany w [docs/web-api.md](docs/web-api.md).

## Konsola ASCII

Każde pole ma dwa razy więcej kolumn niż wierszy: w pełnym rozmiarze
14 × 7. Przy typowej czcionce terminala daje to wizualnie kwadratowe pola.
Kolory tła rozróżniają pola, a jasne i ciemne znaki — strony. Animacja
przesuwa całe rysunki figur, wygasza zbite figury i uwzględnia ruch wieży
przy roszadzie, bicie w przelocie oraz promocję.

Polecenia:

- `e2` — zaznacz figurę i podświetl legalne ruchy; potem wpisz np. `e4`.
- `moves e2` — pokaż dostępne pola docelowe.
- `e2 e4` — wykonaj ruch bez wcześniejszego zaznaczenia.
- `e7 e8 q` — promocja (`q` hetman, `r` wieża, `b` goniec, `n` skoczek).
- `cancel`, `new`, `history`, `help`, `quit` — anulowanie wyboru, nowa partia,
  ostatnie ruchy, pomoc i zakończenie.
- `size full`, `size compact`, `size small`, `size tiny` — maksymalny rozmiar rysunków.
- `computer easy`, `computer medium`, `computer hard black` — nowa partia z komputerem;
  opcjonalny kolor `white` lub `black` (domyślnie biały).
- `local` — nowa partia dwóch osób; `retry` — ponowienie nieudanego ruchu komputera.

Z okna wyboru na Windows uruchamiany jest PowerShell z obsługą ANSI.
JLine odczytuje rzeczywistą wielkość terminala, a gra na bieżąco dopasowuje
planszę, również po maksymalizacji okna i zmianie czcionki. Pełna plansza
wymaga co najmniej 117 kolumn i 66 wierszy; kompaktowa 85 × 50.
Najmniejsza plansza potrzebuje 53 × 34. Przy mniejszym oknie pojawia się
komunikat o powiększeniu okna; partia pozostaje zachowana. Polecenie
`size` ustala górny limit rozmiaru, a za duża plansza jest automatycznie
zmniejszana. Po wpisaniu `size full` i powiększeniu okna wracają pełne figury.
Klawiatura działa bez automatycznego echa i przewijania terminala po Enterze.
Backspace usuwa ostatni znak, a Ctrl+U czyści wpisywane polecenie.
Na Linux okno wyboru korzysta z `xterm` lub `gnome-terminal`;
na macOS otwiera Terminal.

Konsolę można też uruchomić samodzielnie, bez JavaFX. Z głównego katalogu
repozytorium w terminalu obsługującym ANSI (np. Windows Terminal):

```powershell
.\mvnw.cmd -pl console -am -Dmaven.test.skip=true package
java -jar console/target/chess-console-1.0-SNAPSHOT.jar --ansi
```

Bezpośredni start z komputerem: dodaj
`--opponent=COMPUTER --difficulty=MEDIUM --side=WHITE`.

Katalog `console/target/lib` zawiera zależności potrzebne plikowi JAR;
przy przenoszeniu konsoli zachowaj go obok pliku aplikacji.
Bez prawdziwego terminala aplikacja wybiera tryb statyczny; można go też
wymusić opcją `--plain`. W tym trybie czarne figury mają sylwetki z `#`,
a animacje i kolory są wyłączone. Tylko w trybie statycznym opcje
`--columns=N` i `--rows=N` określają dostępne miejsce; tryb interaktywny
zawsze korzysta z rzeczywistych wymiarów okna.

Sam silnik można zbudować i sprawdzić bez pobierania JavaFX:

```powershell
.\mvnw.cmd -pl core verify
```

Dodatkowy test integracji JavaFX (na komputerze z dostępem do środowiska
graficznego) ładuje FXML, grafiki i sprawdza odświeżanie widoku na podstawie API:

```powershell
.\mvnw.cmd -Dchess.gui.tests=true verify
```

Testy nie otwierają okna. Sprawdzają również kliknięcia, podświetlenia,
wykonanie ruchu, bicie, mata, klatki animacji i reset partii w trakcie ruchu.
Zapisują podglądy w
`desktop/target/board-preview.png`, `legal-moves-preview.png`
oraz `capture-preview.png` i `capture-animation-preview.png`.
