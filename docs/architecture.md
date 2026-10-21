# Silnik i interfejsy gry

## Granica modułów

`desktop -> GameApi / ComputerGame -> LocalGame -> ChessRules -> GameState`

`console -> GameApi / ComputerGame -> LocalGame -> ChessRules -> GameState`

`React -> HTTP/JSON -> ChessWebServer -> ComputerGame -> LocalGame -> ChessRules -> GameState`

`Web / konsola -> HTTP z tokenem miejsca -> Rooms.Room -> LocalGame`

`konsola -> RemoteGame (network) -> HTTP -> Rooms.Room`

Silnik jest biblioteką Java zależną wyłącznie od `java.base`.
Interfejs otrzymuje obiekt `GameApi` w konstruktorze kontrolera.
Punkty startowe tworzą `ComputerGame`, który implementuje `GameApi` i posiada
własny `LocalGame`. W trybie dwóch osób przekazuje ruchy obu stron, a w grze
z komputerem pozwala użytkownikowi poruszać wyłącznie jego stroną.

Deskryptory `module-info.java` określają granice modułów Javy.
Silnik eksportuje API, model i klasę `LocalGame`.
Interfejs wymaga silnika i modułów JavaFX, a kontroler udostępnia FXML.
Moduł `console` korzysta z `core` oraz JLine Terminal z dostawcą JNI.
JLine odpowiada za dostęp do terminala i klawiatury, a silnik pozostaje
zależny wyłącznie od `java.base`.
Moduł `computer` zależy tylko od `core`; frontend konsolowy, webowy i desktopowy
korzystają z niego wspólnie. `web` używa też standardowego `jdk.httpserver` i Gson.
`desktop` zna także punkty wejścia konsoli i serwera, aby otworzyć je z menu startowego.
Reguły gry pozostają w `core`; uruchamianie trybów należy do frontendów.

## API

- `getState()` zwraca niezmienny obraz aktualnej partii.
- `newGame()` tworzy nową pozycję początkową i zwraca jej obraz.
- `getLegalMoves(from)` zwraca legalne ruchy aktywnej strony z danego pola.
- `makeMove(move)` ponownie sprawdza ruch i zwraca `MoveResult`.
  Przy odrzuceniu polecenia stan pozostaje bez zmian.
  Kod błędu jest niezależny od języka i sposobu wyświetlania komunikatu.

`Move` zawiera pola początkowe i docelowe oraz opcjonalny rodzaj promocji.
Zapytanie o ruchy pionka dochodzącego do ostatniego rzędu zawiera osobne
możliwości promocji na hetmana, wieżę, gońca i skoczka.
Interfejs wybiera jedną z nich; silnik nie narzuca automatycznej promocji.

## Reprezentacja i kierunki

Mapa `Map<Square, Piece>` wiąże pole z figurą. Brak wpisu oznacza puste pole.
Figura zawiera rodzaj i stronę; pozycja występuje tylko w mapie.
Współrzędne są wspólne dla obu graczy: `(0, 0) = a1`, `(7, 7) = h8`.
Piony białych zwiększają rząd, a piony czarnych go zmniejszają.
Pozostałe figury używają tych samych kierunków dla obu kolorów.
JavaFX przelicza rząd na wiersz ekranu, aby białe znajdowały się na dole.

`GameState` zawiera również stronę na ruchu, prawa do roszady, pole bicia
w przelocie, status partii, historię ruchów, zbite figury, licznik półruchów
bez bicia/ruchu pionem i liczbę wystąpień aktualnej pozycji.
Wszystkie kolekcje oraz ich elementy są niezmienne.
Każda sesja ma własny obiekt `LocalGame`. Adapter HTTP szereguje żądania
do jednej partii przez synchronizację na `ComputerGame`, wspólną również z
wątkiem zatwierdzającym ruch Stockfisha.

## Komputerowy przeciwnik

`GameOptions` określa przeciwnika, poziom i stronę człowieka. `ComputerGame`
przechowuje też monotoniczną rewizję, stan obliczeń i błąd. Po ruchu człowieka
zleca obliczenie na jednej ze wspólnych dwóch kolejek roboczych (pula dwóch
wątków, maksymalnie 256 oczekujących zadań). `StockfishClient` uruchamia proces
UCI, przekazuje pozycję początkową i historię ruchów, a następnie odbiera `bestmove`.
Ruch jest ponownie walidowany przez `LocalGame`. Proces kończy się po obliczeniu;
nie pozostaje uruchomiony podczas oczekiwania na człowieka.

Reset i zamknięcie anulują zadanie oraz zwiększają generację. Wynik starej
generacji nie może zmienić nowej partii. Błąd obliczeń pozostawia kolej komputera;
nie przydziela jego figur użytkownikowi. Ponowienie jest jawną operacją.
Tryb dwóch osób nie uruchamia silnika ani pobierania.

`StockfishInstallation` instaluje przypiętą wersję 19 z oficjalnego GitHub,
weryfikuje SHA-256 i zachowuje pliki licencji oraz źródeł z paczki. Blokada
plikowa zapobiega równoległej instalacji przez kilka procesów aplikacji.
Możliwa jest konfiguracja własnej ścieżki silnika i cache; patrz `stockfish.md`.

JavaFX odczytuje zmiany co 100 ms, interaktywny terminal przy odświeżaniu
wejścia, a React co 400 ms przez HTTP. Ruch komputera jest animowany z dwóch
zatwierdzonych stanów tak samo jak ruch człowieka. Żaden interfejs nie liczy
ruchu samodzielnie. W konsolowym trybie `--plain` aplikacja czeka na odpowiedź
komputera przed kolejnym blokującym odczytem linii.

## Ruchy i bicie

`ChessRules` generuje ruchy odpowiednie dla typu figury.
Wieża, goniec i hetman skanują kolejne pola aż do przeszkody:
własna figura blokuje kierunek, a obcą można zbić, po czym kierunek się kończy.
Pion sprawdza osobno ruch prosto na puste pole i bicie po skosie.
Bicie w przelocie korzysta z pola zapamiętanego po ostatnim podwójnym
ruchu pionka i usuwa pion z sąsiedniego pola.

Pola atakowane są wyznaczane osobno od możliwych ruchów.
Każdy kandydat jest symulowany, a ruch pozostawiający własnego króla
pod szachem zostaje odrzucony. Ta sama kontrola uwzględnia
odsłonięcie króla po biciu w przelocie.
Roszada wymaga praw do niej, właściwej wieży, pustej drogi i bezpiecznej
drogi króla. Ruch króla lub wieży i zbicie wieży usuwają właściwe prawa.
Król pozostaje na planszy; zakończenie partii rozstrzyga mat lub pat.

## Remisy

`LocalGame` przechowuje mapę wystąpień pozycji. Klucz zawiera planszę, stronę,
prawa do roszady i pole en passant tylko wtedy, gdy istnieje legalne bicie
(także po uwzględnieniu związania piona). Pierwsza pozycja liczy się jako
pierwsze wystąpienie; reset czyści mapę i licznik półruchów.

`GameState.canClaimDraw()` sygnalizuje trzecią powtórzoną pozycję lub 100
półruchów. `GameApi.claimDraw()` zatwierdza remis w aktualnej pozycji i nie
dopisuje fikcyjnego ruchu. Zgłoszenie przez zapowiedź przyszłego ruchu nie jest
jeszcze obsługiwane. Przy 5 wystąpieniach lub 150 półruchach remis jest automatyczny.
Mat w ostatnim ruchu ma pierwszeństwo przed limitem 75 posunięć.

Niewystarczający materiał obejmuje K–K, K+B–K, K+N–K i pozycje z samymi
królami oraz gońcami wyłącznie na polach tego samego koloru. Dwa skoczki,
różnopolowe gońce lub skoczki obu stron nie są automatycznie uznawane za remis.
Ogólne wykrywanie każdej możliwej martwej pozycji (np. z zablokowanymi pionami)
pozostaje poza zakresem tej wersji.

`ComputerGame` przyjmuje zgłoszenia człowieka wyłącznie w jego kolejce.
Komputer zgłasza dostępny remis przed rozpoczęciem obliczeń. Każde udane
zgłoszenie zwiększa rewizję, ale zachowuje historię ruchów bez zmian.

## Interfejs

Kliknięcie własnej figury odpytuje API o ruchy i podświetla ich pola docelowe.
Kliknięcie celu wysyła wybrany `Move`.
Frontend odrysowuje wyłącznie stan zwrócony przez backend.
Zaznaczenie figury, okno wyboru promocji i opis komunikatów należą do UI.
Aktualizacja planszy, bicie, kolejka i wynik partii należą do silnika.
Historia na ekranie używa zapisu pól początkowych i docelowych;
obsługa SAN/PGN pozostaje do dalszych prac.

Silnik zatwierdza ruch od razu. `BoardView` i `MoveAnimator` pokazują
przejście od poprzedniego do nowego obrazu planszy w osobnej warstwie.
Animacje korzystają z wbudowanych przejść JavaFX: przesunięcia, zanikania
i skalowania. Różnice między obrazami wskazują zbite figury oraz dodatkowe
przesunięcia, takie jak ruch wieży przy roszadzie. Frontend nie wyznacza
legalności tych ruchów. Po zakończeniu animacji odrysowuje nowy stan.
Kliknięcia planszy są w tym czasie wstrzymane. Reset usuwa warstwę animacji
i anuluje jej zakończenie, aby poprzedni stan nie zastąpił nowej partii.

## Wybór trybu i konsola

`Launcher` otwiera okno `ModeSelectionView`. Wybór JavaFX tworzy kontroler
dotychczasowej planszy. Wybór ASCII otwiera osobny terminal i proces Javy
z `ConsoleMain`; `ConsoleLauncher` odczytuje lokalizacje klas silnika, konsoli i JLine,
więc działa zarówno z katalogami klas IntelliJ, jak i z plikami JAR.
Wybór Web uruchamia jeden serwer `ChessWebServer` na dostępnym porcie
i otwiera przeglądarkę przez JavaFX HostServices. Serwer jest współdzielony
między kolejnymi otwarciami przeglądarki; każda sesja gry ma osobny silnik.
Zamknięcie aplikacji wywołuje `MainWindow.stop()` i zatrzymuje serwer.

`ConsoleGame` odczytuje polecenia, odpytuje `GameApi` i pokazuje odpowiedzi.
`AsciiBoard` układa figury z `PieceArt` na kolorowych polach. Szerokość pola
jest dwukrotnością jego wysokości w znakach, aby uwzględnić proporcje czcionek
terminalowych. Dostępne są cztery rozmiary planszy.

`TerminalScreen` używa bufora znaków i kolorów oraz sterowania kursorem ANSI.
W animacji odświeża tylko zmienione wiersze, bez czyszczenia całego ekranu.
JLine odczytuje rzeczywisty obszar okna. Podczas oczekiwania na klawiaturę
rozmiar jest sprawdzany co 100 ms; jego zmiana unieważnia bufor poprzedniej
klatki i powoduje dopasowanie oraz pełne odrysowanie planszy.
Polecenia są czytane w trybie raw, bez systemowego echa. Enter nie wypisuje
nowej linii. Pole wprowadzania jest rysowane na ustalonym wierszu poniżej
planszy, z zapasem miejsca na brzegu ekranu. Przy zbyt małym oknie widok
zastępuje planszę komunikatem, zachowując stan partii i wpisywane polecenie.
Ruch trwa około 360 ms. Tak jak w JavaFX, ścieżki figur i zbicia wynikają
z różnic między zatwierdzonymi stanami silnika. Promowana figura zastępuje
pionka po dotarciu do pola docelowego. Tryb bez ANSI pokazuje statyczne klatki
i odróżnia czarne figury wypełnieniem `#`.

Windowsowy skrypt startowy uruchamia proces Javy, a JLine konfiguruje ANSI,
tryb wejścia i odczyt wymiarów. Skrypt nie zmienia bufora ani wielkości okna
przez PowerShell RawUI. Polecenia i ścieżki są kodowane dla
PowerShell, co zachowuje spacje i polskie znaki w katalogach projektu.
Przy wyjściu konsola przywraca kursor, kolory, poprzedni ekran i tryb wejścia.
Pakowanie modułu konsoli kopiuje zależności do `target/lib`, a manifest JAR
wskazuje te pliki, aby samodzielne uruchomienie nie wymagało JavaFX.

## Web i HTTP

`ChessWebServer` podaje gotowe zasoby React oraz API z tego samego adresu,
więc normalna gra nie wymaga CORS ani serwera Node. `WebMain` uruchamia
ten sam moduł samodzielnie, bez JavaFX. Domyślny adres jest lokalny.
Kod React jest w `web/frontend`, a wynik Vite w `web/src/main/resources/public`.
Gotowe zasoby pozostają w repozytorium, aby zwykłe uruchomienie Java nie
uruchamiało instalacji npm. Zależności serwera są pakowane do `target/lib`.

`GameSessions` przechowuje maksymalnie 256 sesji z własnymi silnikami.
Sesja wygasa po 2 godzinach bezczynności. Polecenia ruchu i resetu wymagają
numeru `revision`; nieaktualne żądanie dostaje HTTP 409 i aktualny obraz
partii. Odrzucony ruch nie zmienia stanu, a udany ruch lub reset zwiększa
rewizję. `GameJson` jawnie mapuje model na JSON, bez zmieniania modelu silnika
i otwierania jego klas dla refleksji. Szczegóły zawiera `web-api.md`.

React trzyma identyfikator sesji w `sessionStorage`, żeby przywrócić partię
po odświeżeniu karty. Stan jest wyświetlaną kopią odpowiedzi serwera.
Legalne pola zawsze pochodzą z zapytania HTTP. Żądania mają limit czasu,
a równoległe kliknięcia są blokowane. Po utracie połączenia trzeba pobrać
aktualną pozycję; interfejs nie ponawia automatycznie polecenia ruchu.

Animacja CSS używa stałych identyfikatorów wizualnych figur i różnic między
stanami zwróconymi przez serwer. Różnice wykrywają także ruch wieży oraz
zbicie pionka z sąsiedniego pola. Pionek zmienia rysunek po dojściu do pola
promocji. Te identyfikatory i ścieżki nie są regułami gry. Ustawienie systemowe
ograniczania animacji wyłącza przejścia. Nowa partia anuluje pozostałe klatki
poprzedniego ruchu.

## Efekty zakończenia

JavaFX (`EndCelebration`) i React (`EndCelebration.jsx`) rysują figury z grawitacją,
odbiciami i kolorowymi smugami w osobnej warstwie Canvas. Konsola używa
`AsciiBoard.celebrationFrame` i tego samego mechanizmu odświeżania co zwykłe ruchy.
Efekty startują po zatwierdzonym zakończeniu i zakończeniu animacji ostatniego
ruchu, trwają maksymalnie 6,5 s, pozwalają na pominięcie i są anulowane przez reset.
Nie blokują rozpoczęcia nowej partii i nie modyfikują modelu gry.

Przeglądarka respektuje `prefers-reduced-motion`; JavaFX i konsola przyjmują
`-Dchess.reducedMotion=true`. Statyczny komunikat wyniku pozostaje dostępny
również bez efektu. Ponowne odrysowanie stanu nie odtwarza pominiętej animacji.

## Pokoje online

`Rooms` przechowuje do 128 niezależnych pokoi w pamięci. `Room` szereguje
dołączanie, odzyskiwanie połączenia, ruchy, zgłoszenia remisu i zgody na rewanż
pod jednym monitorem. Limit dwóch miejsc jest sprawdzany na serwerze. Kolor
pochodzi z uwierzytelnionego miejsca, nigdy z danych żądania ruchu.

Każde miejsce ma tajny token odzyskiwania i osobny token aktywnego połączenia.
Nowy `clientId` przy odzyskaniu miejsca wymienia token połączenia, unieważniając
starą kartę. Lista pokoi ujawnia tylko dane lobby; plansza i polecenia wymagają
tokena Bearer. Rozłączone miejsce pozostaje zarezerwowane. Opuszczenie zamyka
cały pokój; bezczynne pokoje wygasają po 2 godzinach, a zamknięte są usuwane
przy kolejnej operacji lobby.

Frontend pokazuje lobby lub istniejącą planszę z własnym kolorem i uprawnieniami.
Obaj gracze widzą aktualizacje przez polling HTTP co około 400–500 ms. Rewanż
wymaga zgody obu stron i zamienia kolory. Nie ma zdalnego jednostronnego resetu.

`network.RemoteGame` implementuje `GameApi` bez zależności od JavaFX, konsoli
lub klas serwera. Tłumaczy JSON na niezmienne `GameState`; legalność zawsze
wyznacza serwer. Odpytuje stan w tle, więc wejście terminala nadal reaguje.
Moduł zależy od `core` i Gson. `console` zależy dodatkowo od `network`.

`WebConfig` ustawia adres nasłuchiwania i dokładną listę dozwolonych originów.
Samodzielny serwer przyjmuje konfigurację z argumentów lub środowiska.
Docker/Caddy podaje stronę i API z jednego originu HTTPS. Partie nadal są
w pamięci jednego procesu, bez bazy i koordynacji między replikami.
Pełny kontrakt i wdrożenie opisuje `online.md`.

## Sprawdzanie

`mvnw -pl core verify` buduje silnik i sprawdza go bez JavaFX.
Testy obejmują kierunki obu stron, blokowanie ruchów, bicie, bezpieczeństwo
króla, roszadę, promocję, bicie w przelocie, mata i pata.
Liczby pozycji z ustawienia początkowego na głębokościach 1–3 wynoszą
20, 400 i 8902; test sprawdza je przez generowanie ruchów.

Opcjonalne `-Dchess.gui.tests=true` uruchamia JavaFX bez okna gry.
Testy sprawdzają rzeczywiste kliknięcia przycisków pól, podświetlenia,
ruchy, bicie, reset i komunikat o macie oraz odrzucenie ruchu przez backend.
Sprawdzają także uruchomienie frontendu i JavaFX jako nazwanych modułów.
Obejmują pośrednie klatki ruchu i bicia, roszadę, bicie w przelocie,
promocję, szybkie kliknięcia oraz reset podczas animacji.

Plik `.mvn/jvm.config` ustawia `file.encoding=COMPAT` dla procesu Mavena,
aby pliki argumentów procesów testowych używały kodowania systemowego.
To obsługuje ścieżki z polskimi znakami na Windows.
Kod źródłowy i zasoby projektu zachowują jawne kodowanie UTF-8.
