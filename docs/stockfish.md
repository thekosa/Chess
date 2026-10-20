# Stockfish

Wspólny moduł `computer` obsługuje komputerowego przeciwnika we wszystkich
interfejsach. Reguły, legalność i wynik nadal rozstrzyga moduł `core`.

## Poziomy

- Łatwy: `Skill Level 0`, do około 200 ms namysłu.
- Średni: `Skill Level 8`, do około 500 ms namysłu.
- Trudny: `Skill Level 18`, do około 1000 ms namysłu.

Każdy proces używa jednego wątku i 32 MiB tablicy transpozycji.
Poziomy są względnymi ustawieniami, bez przypisanych wartości Elo.
Stockfish nawet na najniższym poziomie może być trudny dla początkujących.
Do czasu odpowiedzi dochodzą start procesu i ewentualne oczekiwanie w kolejce.

## Instalacja i działanie offline

Pierwsza gra z komputerem pobiera oficjalny pakiet **Stockfish 19** z wydania
[`sf_19`](https://github.com/official-stockfish/Stockfish/releases/tag/sf_19).
Przypięty SHA-256 jest sprawdzany przed rozpakowaniem. Pakiet ma około 80 MB.
Wersja uniwersalna dobiera obsługiwane instrukcje procesora. Obsługiwane paczki:
Windows x64/ARM64, Linux x64/ARM64 i macOS Universal. Linux/macOS wymagają `tar`.

W repozytorium instalacja trafia do `.tools/stockfish`; w przeniesionej aplikacji
do `~/.cache/szachy/stockfish`. Plik `sf19-*.path` wskazuje zainstalowany plik.
Katalog zawiera oryginalne archiwum i rozpakowaną paczkę, w tym jej licencję
i pliki źródłowe. Silnik oraz cache nie są dodawane do Git.
Po instalacji gra z komputerem nie wymaga internetu ani żadnego klucza API.
Tryb dwóch osób nie pobiera ani nie uruchamia Stockfisha.

Można użyć własnej instalacji zgodnej z protokołem UCI i opcją `Skill Level`:

```powershell
$env:CHESS_STOCKFISH_PATH='C:\narzedzia\stockfish\stockfish.exe'
```

Alternatywnie ustaw opcję JVM `-Dchess.stockfish.path=/pelna/sciezka/do/stockfish`.
Opcja JVM `-Dchess.stockfish.cache=/pelna/sciezka/do/cache` zmienia katalog
instalacji. Przy starcie konsoli z okna wyboru dziedziczona jest zmienna
środowiskowa; opcje JVM z procesu okna nie są przekazywane do osobnego procesu.

Nieudane pobranie, start lub obliczenie nie zmienia planszy. W oknie i Web
pojawia się przycisk ponowienia; w konsoli polecenie `retry`. Można również
rozpocząć nową partię z drugim człowiekiem. Reset anuluje stare obliczenia.

## Licencja

Stockfish jest osobnym programem na licencji **GPLv3**. Projekt pobiera
niezmodyfikowaną oficjalną paczkę i zachowuje `Copying.txt` oraz dołączone
źródła. Źródła odpowiadające wersji 19 są też dostępne w oficjalnym
[repozytorium Stockfisha](https://github.com/official-stockfish/Stockfish/tree/sf_19).
Własna dystrybucja zawierająca binaria Stockfisha powinna zachowywać licencję
i zapewniać odpowiadający im kod źródłowy.
