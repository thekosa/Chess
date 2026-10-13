# Silnik i interfejsy gry

## Granica modułów

`desktop -> GameApi -> LocalGame -> GameState`

Silnik jest zwykłą biblioteką Java. Nie ma zależności od grafiki, przycisków,
plików FXML, kolorów AWT ani serwera HTTP. Interfejs otrzymuje obiekt `GameApi`
w konstruktorze kontrolera. Jedynie punkt startowy aplikacji tworzy konkretny
silnik `LocalGame`.

Moduł `core` jest osobnym artefaktem Maven. Przyszły frontend konsolowy może
zależeć wyłącznie od `chess-core`, utworzyć własny silnik i odpytywać to samo API.
Ewentualny serwer HTTP może później opakować to API.

## Obowiązujące API

- `getState()` zwraca niezmienny obraz aktualnej partii.
- `newGame()` tworzy nową pozycję początkową i zwraca jej obraz.

Mapa planszy wiąże pole z figurą. Brak wpisu oznacza puste pole.
Figura przechowuje rodzaj i stronę; pozycja występuje tylko w mapie.
Współrzędne są wspólne dla obu graczy: `(0, 0) = a1`, `(7, 7) = h8`.
JavaFX sam przelicza rangę na wiersz ekranu, aby białe znajdowały się na dole.
Strona na ruchu należy do stanu silnika, a opis „Ruch: białe” należy do UI.

Dane zwracane z API są niezmienne i nie odsłaniają edytowalnego stanu silnika.
Każda sesja ma własny obiekt `LocalGame`. Obecny silnik jest przeznaczony do
lokalnego korzystania z jednego wątku; przyszły adapter sieciowy będzie musiał
szeregować żądania do jednej partii.

## Następne kroki

Przy wdrażaniu reguł dodamy do API zapytanie o legalne ruchy oraz polecenie
wykonania ruchu, z obsługą promocji. Polecenie musi zawsze ponownie sprawdzać
legalność. Błąd ma pozostawiać stan bez zmian i zwracać rezultat nadający się
zarówno dla GUI, jak i konsoli.

Po kliknięciu figury frontend poprosi silnik o legalne pola docelowe.
Po kliknięciu celu wyśle polecenie ruchu, a następnie wyświetli nowy obraz
partii. Aktualizowanie pozycji, bicie i zmiana kolejki należą wyłącznie do silnika.

Prawo do roszady, pole bicia w przelocie, licznik półruchów i wynik partii
dołączymy do stanu wraz z odpowiednimi regułami. Nie wystarczy odtworzyć tych
informacji z samego rozmieszczenia figur.

## Sprawdzanie granicy

`mvnw -pl core verify` buduje silnik i uruchamia jego testy bez JavaFX.
Testy sprawdzają pozycję początkową, ochronę stanu przed zmianami przez frontend,
izolację sesji i poprawność współrzędnych. Pełne `mvnw verify` dodatkowo
kompiluje interfejs i pakuje jego zasoby.

Opcjonalny test `-Dchess.gui.tests=true` uruchamia JavaFX bez wyświetlania
okna. Sprawdza grafiki, 64 pola, 32 figury oraz to, że odświeżenie widoku
korzysta z odpowiedzi API. Zapisuje też podgląd pozycji początkowej.
