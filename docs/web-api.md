# API wersji webowej

API i pliki interfejsu są dostępne z tego samego adresu. Ten dokument opisuje
prywatne partie lokalne/Stockfisha; pokoje online mają osobny kontrakt w
[online.md](online.md). Serwer uruchomiony
samodzielnie używa domyślnie `http://127.0.0.1:8080`, a okno wyboru korzysta
z wolnego portu. Wszystkie odpowiedzi API są JSON z `Cache-Control: no-store`.
Polecenia POST wymagają `Content-Type: application/json`; limit treści to 8 KiB.

## Operacje

- `GET /api/health` — odpowiedź `{"status":"ok"}`.
- `POST /api/games`, treść `{}` lub ustawienia przeciwnika — nowa partia, HTTP 201 i stan.
- `GET /api/games/{id}` — aktualny stan.
- `GET /api/games/{id}/moves?from=e2` — legalne ruchy figury aktywnej strony.
- `POST /api/games/{id}/moves` — wykonanie ruchu.
- `POST /api/games/{id}/reset` — reset tej samej sesji do pozycji początkowej.
- `POST /api/games/{id}/retry` — ponowienie nieudanego ruchu komputera (treść: aktualna `revision`).
- `POST /api/games/{id}/claim-draw` — zgłoszenie remisu w aktualnej pozycji (treść: aktualna `revision`).

Przykładowe polecenie ruchu:

```json
{"revision":0,"from":"e2","to":"e4","promotion":null}
```

Przy promocji `promotion` przyjmuje `QUEEN`, `ROOK`, `BISHOP` lub `KNIGHT`.
Reset wymaga tylko aktualnej rewizji, np. `{"revision":12}`.
Może też zmienić ustawienia. Przy tworzeniu pominięte ustawienia oznaczają
dwóch graczy, poziom średni i biały kolor człowieka; reset zachowuje pominięte wartości.

Przykład utworzenia partii z komputerem:

```json
{"opponent":"COMPUTER","difficulty":"EASY","humanSide":"BLACK"}
```

`opponent`: `TWO_PLAYERS` lub `COMPUTER`; `difficulty`: `EASY`, `MEDIUM`, `HARD`;
`humanSide`: `WHITE`, `BLACK`. Ustawienia komputera są ignorowane w grze dwóch osób.

## Stan

```json
{
  "id": "identyfikator-uuid",
  "revision": 0,
  "sideToMove": "WHITE",
  "status": "ACTIVE",
  "opponent": "TWO_PLAYERS",
  "difficulty": "MEDIUM",
  "humanSide": "WHITE",
  "computerThinking": false,
  "computerError": null,
  "halfmoveClock": 0,
  "repetitionCount": 1,
  "canClaimDraw": false,
  "board": [{"square":"e2","side":"WHITE","type":"PAWN"}],
  "history": [],
  "capturedPieces": []
}
```

Przykład pokazuje tylko jeden wpis planszy; odpowiedź zawiera wszystkie figury.
Puste pola nie mają wpisów. Strony to `WHITE` i `BLACK`, rodzaje figur:
`PAWN`, `KNIGHT`, `BISHOP`, `ROOK`, `QUEEN`, `KING`. Statusy to `ACTIVE`,
`CHECK`, `CHECKMATE`, `STALEMATE`. Historia ma obiekty `{from,to,promotion}`,
a zbite figury `{side,type}`. Współrzędne zawsze są wspólne dla obu stron.

Dodatkowe końcowe statusy remisów: `DRAW_REPETITION`, `DRAW_FIFTY_MOVES`,
`DRAW_INSUFFICIENT_MATERIAL`, `DRAW_FIVEFOLD_REPETITION`, `DRAW_SEVENTY_FIVE_MOVES`.
W każdym końcowym stanie klient blokuje dalsze ruchy i może pokazać efekt końca partii.
`halfmoveClock` to półruchy bez bicia/ruchu pionem, `repetitionCount` liczy
wystąpienia aktualnej pozycji. `canClaimDraw` dotyczy strony na ruchu; w grze
z komputerem człowiek nie zgłasza remisu za komputer.

Udane `/claim-draw` zwraca HTTP 200 i nowy stan oraz zwiększa rewizję bez
zmiany historii ruchów. Niedostępny remis: HTTP 422, kod `DRAW_UNAVAILABLE`
i pełny bieżący `state`. Zgłoszenie dotyczy obecnej pozycji, bez zapowiadania ruchu.

Zapytanie o ruchy zwraca rewizję pozycji, dla której je wyznaczono:

```json
{"revision":0,"moves":[{"from":"e2","to":"e3","promotion":null},{"from":"e2","to":"e4","promotion":null}]}
```

Udany ruch/reset zwraca HTTP 200 i cały nowy stan. Rewizja rośnie również
po ruchu Stockfisha i resecie; nie jest numerem ruchu. Serwer wykonuje kontrolę
rewizji i zmianę silnika pod tą samą blokadą `ComputerGame`.

Ruch komputera jest asynchroniczny. Klient w trybie `COMPUTER` odpytuje stan
co około 400 ms i animuje zmianę po wzroście rewizji. `computerThinking` obejmuje
również oczekiwanie w kolejce i pierwszą instalację. `computerError` zawiera
komunikat błędu lub `null`; można wtedy wysłać `/retry`. Te dwa pola mogą
zmienić się bez zwiększenia rewizji planszy. Ponowienie zwraca HTTP 200 i stan.
Podczas kolejki komputera lista legalnych ruchów dla użytkownika jest pusta,
a próba ruchu człowieka daje `WRONG_SIDE`.

## Błędy i czas życia

Niepoprawna treść lub pole: HTTP 400; nieistniejący endpoint/sesja: 404;
niepoprawna metoda: 405; zbyt duża treść: 413; inny typ treści: 415.
Przekroczenie limitu 256 aktywnych sesji daje 429. Błąd silnika daje 422,
a nieaktualna rewizja 409. Odpowiedzi 409 i 422 zawierają bieżący stan:

```json
{"error":"STALE_STATE","message":"Plansza zmieniła się. Pobrano aktualną pozycję.","state":{}}
```

W rzeczywistej odpowiedzi `state` ma pełną strukturę opisaną powyżej.
Kody silnika: `WRONG_SIDE`, `ILLEGAL_MOVE`, `PROMOTION_REQUIRED`, `GAME_OVER`.
Klient powinien odświeżyć pozycję po konflikcie i nie ponawiać automatycznie
ruchu, którego wyniku nie zna po zerwaniu połączenia.

Sesje są przechowywane w pamięci i wygasają po 2 godzinach bezczynności.
Restart serwera usuwa wszystkie partie. Obecne API jest lokalne, bez kont,
ról graczy w prywatnej partii lokalnej. Identyfikator sesji `/api/games` nie przydziela kolorów.
Do gry dwóch klientów użyj `/api/rooms` z tokenami miejsc — nie współdziel
prywatnego identyfikatora lokalnej partii jako mechanizmu gry online.
