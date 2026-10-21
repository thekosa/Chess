# Gra online — branch `online`

Jeden proces serwera Java obsługuje lobby i wiele niezależnych pokoi. Tworzenie
pokoju nie uruchamia osobnego procesu ani nie wymaga nowego portu. Przeglądarka
i konsola są równorzędnymi klientami tej samej partii na serwerze.

## Uruchomienie na swoim komputerze

Z głównego katalogu projektu:

```powershell
.\mvnw.cmd -pl web -am -Dmaven.test.skip=true package
java -jar web/target/chess-web-1.0-SNAPSHOT.jar --port=8080
```

Otwórz `http://localhost:8080/`. Domyślna strona to lista pokoi. Wpisz nick,
utwórz pokój albo wybierz wolny z listy. Utworzenie pokoju od razu zajmuje
pierwsze miejsce. Drugie otrzymuje przeciwny kolor. Białe mogą ruszyć dopiero
po dołączeniu drugiej osoby. Po zakończeniu oboje wybierają rewanż, a serwer
resetuje partię i zamienia kolory.

W IntelliJ po dodaniu modułu `network` użyj **Reload All Maven Projects**.
Konfiguracja **Szachy Online** uruchamia ten sam serwer na porcie 8080.
Zwykłe uruchomienie **Szachy → Graj w przeglądarce** zachowuje ustawienia gry
lokalnej/Stockfisha; przycisk **Pokoje online** na stronie otwiera lobby tego serwera.

Do sprawdzenia dwóch miejsc na jednym komputerze użyj dwóch niezależnych kart,
np. zwykłego okna i okna prywatnego. Skopiowanie karty z jej sesją oznacza
odzyskanie tego samego miejsca; poprzednia karta traci uprawnienia do gry.

## Drugi komputer w tej samej sieci

Serwer musi nasłuchiwać na interfejsie sieciowym. Podstaw własny lokalny adres IP:

```powershell
java -jar web/target/chess-web-1.0-SNAPSHOT.jar --host=0.0.0.0 --port=8080 --public-origin=http://192.168.1.20:8080 --public-origin=http://localhost:8080
```

Na obu komputerach otwórz `http://192.168.1.20:8080/`. Systemowa zapora musi
zezwalać na połączenia do tego portu w Twojej sieci. `127.0.0.1` i `localhost`
wskazują zawsze komputer gracza, więc link z takiego adresu nie zadziała u znajomego.

## Konsola kontra przeglądarka

Można użyć przycisku **Konsola online…** w oknie startowym lub uruchomić klienta:

```powershell
.\mvnw.cmd -pl console -am -Dmaven.test.skip=true package
java -jar console/target/chess-console-1.0-SNAPSHOT.jar --ansi --server=http://localhost:8080 --name=Michal
```

Klient pokazuje tę samą listę pokoi. Wybierz numer albo `new`, by utworzyć pokój.
`refresh` odświeża listę. Opcjonalne `--room=UUID` od razu próbuje dołączyć do pokoju.
Zwykłe ruchy i animacje działają jak wcześniej. `new` w partii online oznacza
zgodę na rewanż po zakończeniu, `draw` zgłasza dostępny remis, a `quit` zamyka pokój.
`local` i `computer` nie przełączają trwającej sesji online na lokalną.

Przeglądarka zachowuje dane odzyskania miejsca w `sessionStorage`. Konsola
nie zapisuje tokenu na dysku; przerwanej sesji terminalowej nie odzyskuje się
automatycznie po ponownym uruchomieniu procesu. API klienta Java ma metodę
`reconnect(roomId, playerToken)` dla dalszych integracji. Normalne `quit`
zwalnia pokój przez jego zamknięcie dla obu stron.

## Umieszczenie na stronie

Najprościej wystawić aplikację pod osobną subdomeną, np. `szachy.twojastrona.pl`,
i dodać na głównej stronie odnośnik do niej. Serwer Java podaje też gotowy frontend
React, więc w środowisku docelowym nie trzeba osobno uruchamiać Node.js.
Obecny frontend korzysta ze ścieżek `/api` i `/pieces`; konfiguracja poniżej
zakłada katalog główny subdomeny, a nie podkatalog `/szachy`.

W repozytorium są `Dockerfile`, `compose.yaml` i konfiguracja Caddy z HTTPS.
Na serwerze z Dockerem:

1. Ustaw rekord DNS subdomeny na IP serwera.
2. Skopiuj `.env.example` do `.env` i wpisz swoją domenę w `CHESS_DOMAIN`.
3. Uruchom `docker compose up --build -d`.

Caddy przyjmuje ruch na portach 80/443 i przekazuje go do aplikacji Java.
Nie wystawiamy osobno portu 8080 na zewnątrz w tej konfiguracji. Cache Stockfisha
jest w osobnym wolumenie. Pokoje i partie są na razie w pamięci, więc restart
aplikacji je usuwa. Nie uruchamiaj kilku replik aplikacji za load balancerem:
bez wspólnego magazynu klienci mogliby trafić do różnych zestawów pokoi.

Bez Dockera można uruchomić JAR na VPS z JDK/JRE 21 i reverse proxy:

```sh
java -jar chess-web-1.0-SNAPSHOT.jar --host=127.0.0.1 --port=8080 --public-origin=https://szachy.twojastrona.pl
```

Zachowaj katalog `lib` obok JAR. Środowisko przyjmuje także `PORT`, `CHESS_HOST`
i `CHESS_ALLOWED_ORIGINS` (lista adresów origin oddzielona przecinkami).
Origin to dokładny schemat, host i port; nie jest zgadywany z niezaufanych
nagłówków proxy. W trybie developerskim Vite dodaj np.
`--public-origin=http://localhost:5173`. Normalna strona i API mają wspólny origin.

## Limit dwóch klientów i życie pokoju

- Dwa miejsca na pokój, egzekwowane atomowo na serwerze nawet przy równoczesnym dołączaniu.
- Publiczna lista pokazuje nazwę, kolory/nicki, zajętość i status. Nie udostępnia tokenów ani planszy osobom bez miejsca.
- Operacje partii wymagają `Authorization: Bearer <accessToken>`; klient nie wybiera koloru w żądaniu ruchu.
- Serwer przypisuje kolor, sprawdza swoją kolej, legalność ruchu i numer rewizji.
- Odzyskanie miejsca wymaga tajnego `playerToken`. Nowa karta/połączenie unieważnia dostęp poprzedniej karty tego gracza.
- Rozłączenie nie otwiera miejsca dla trzeciej osoby. Po 15 sekundach bez odpytywania gracz jest oznaczony jako rozłączony, ale miejsce pozostaje zarezerwowane.
- Opuszczenie pokoju zamyka go dla obu osób. Nie ma widzów ani przejmowania czyjejś partii.
- Pokoje wygasają po 2 godzinach bez aktywności graczy; samo wyświetlanie lobby nie przedłuża ich życia. Limit to 128 pokoi.
- Ruchy i stan są odświeżane przez odpytywanie co około 400–500 ms; lobby co 3 sekundy.

## Kontrakt HTTP

- `GET /api/rooms` → `{rooms:[{id,name,playerCount,capacity,status,players,createdAt}]}`.
- `POST /api/rooms` z `{name,playerName,side,clientId}` → HTTP 201 i dane miejsca.
- `POST /api/rooms/{id}/join` z `{playerName,clientId}` → dane drugiego miejsca lub HTTP 409 `ROOM_FULL`.
- `POST /api/rooms/{id}/reconnect` z `{playerToken,clientId}` → nowe dane aktywnego połączenia.
- `GET /api/rooms/{id}` → stan partii i `room` z `yourSide`, `canMove`, listą graczy oraz zgodami na rewanż.
- `GET /api/rooms/{id}/moves?from=e2` → legalne ruchy własnej strony albo pusta lista.
- `POST /api/rooms/{id}/moves` z `{revision,from,to,promotion}` → nowy stan.
- `POST /api/rooms/{id}/claim-draw` z `{revision}` → zgłoszenie remisu.
- `POST /api/rooms/{id}/rematch` z `{revision}` → zgoda na rewanż; po drugiej zgodzie nowa partia i zamiana kolorów.
- `POST /api/rooms/{id}/leave` z `{}` → zamknięcie pokoju.

Dane miejsca: `{roomId,playerToken,accessToken,state}`. `clientId` to UUID nowego
uruchomienia klienta. GET stanu i wszystkie operacje gry po dołączeniu wymagają
`accessToken` w nagłówku Bearer. Tokenów nie umieszczaj w URL ani publicznym lobby.
Zgubiona odpowiedź `join` może być odebrana ponownie z tym samym `clientId`.
Próba wejścia trzeciego klienta nie zwraca danych żadnego miejsca.
Nieaktualna rewizja: 409 `STALE_STATE`; brak uprawnień: 403 `INVALID_SESSION`;
czekanie na drugiego gracza: 409 `WAITING_FOR_PLAYER`. Błędy poleceń aktualnego
gracza zawierają bieżący `state`, żeby frontend mógł odświeżyć planszę.
