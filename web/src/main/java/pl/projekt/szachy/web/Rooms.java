package pl.projekt.szachy.web;

import com.google.gson.*;
import pl.projekt.szachy.core.LocalGame;
import pl.projekt.szachy.core.model.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** An authoritative two-seat lobby. Joining and every game command are atomic per room. */
final class Rooms implements AutoCloseable {
    private static final long IDLE = Duration.ofHours(2).toNanos();
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private boolean closed;

    synchronized JsonObject create(String name, String playerName, Side side, String clientId) {
        ensureOpen();
        prune();
        if (rooms.size() >= 128) throw new ApiException(429, "ROOM_LIMIT", "Za dużo pokoi. Spróbuj później.");
        Room room = new Room(label(name, 40), label(playerName, 24), side, client(clientId));
        rooms.put(room.id, room);
        return room.credentials(room.players.getFirst());
    }
    synchronized JsonArray list() {
        ensureOpen(); prune();
        JsonArray result = new JsonArray();
        rooms.values().stream().sorted(Comparator.comparing(Room::isFull)
                .thenComparing(room -> room.created, Comparator.reverseOrder()))
                .forEach(room -> result.add(room.summary()));
        return result;
    }
    synchronized Room get(String id) {
        ensureOpen(); prune();
        Room room = rooms.get(id);
        if (room == null) throw new ApiException(404, "ROOM_NOT_FOUND", "Pokój został zamknięty lub wygasł.");
        return room;
    }
    private void prune() {
        long now = System.nanoTime();
        rooms.values().removeIf(room -> {
            if (!room.closed && now - room.touched <= IDLE) return false;
            room.close();
            return true;
        });
    }
    private void ensureOpen() {
        if (closed) throw new ApiException(503, "SERVER_STOPPING", "Serwer kończy pracę.");
    }
    @Override public synchronized void close() { closed = true; rooms.values().forEach(Room::close); rooms.clear(); }
    private static String label(String value, int max) {
        if (value == null || value.strip().isEmpty() || value.strip().length() > max
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new ApiException(400, "INVALID_NAME", "Nazwa musi mieć od 1 do " + max + " znaków.");
        }
        return value.strip();
    }
    private static String client(String value) {
        if (value == null || !value.matches("[a-fA-F0-9-]{36}")) throw new ApiException(400, "INVALID_CLIENT", "Brak identyfikatora klienta.");
        return value;
    }
    private static String token() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private static boolean sameToken(String supplied, String expected) {
        return supplied != null && MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    static final class Room {
        final String id = UUID.randomUUID().toString();
        final String name;
        final Instant created = Instant.now();
        final LocalGame game = new LocalGame();
        final List<Player> players = new ArrayList<>(2);
        final Set<Side> rematch = EnumSet.noneOf(Side.class);
        volatile long touched = System.nanoTime();
        volatile boolean closed;
        long revision;

        Room(String name, String playerName, Side side, String clientId) {
            this.name = name;
            players.add(new Player(playerName, side, clientId));
        }
        synchronized boolean isFull() { return players.size() == 2; }
        synchronized JsonObject join(String playerName, String clientId) {
            ensureOpen();
            client(clientId);
            for (Player player : players) {
                if (player.clientId.equals(clientId)) return credentials(player);
            }
            if (players.size() >= 2) throw new ApiException(409, "ROOM_FULL", "Ten pokój ma już dwóch graczy.");
            Player player = new Player(label(playerName, 24), players.getFirst().side.opposite(), clientId);
            players.add(player);
            touched = System.nanoTime();
            return credentials(player);
        }
        synchronized JsonObject reconnect(String playerToken, String clientId) {
            ensureOpen(); client(clientId);
            for (Player player : players) {
                if (sameToken(playerToken, player.resumeToken)) {
                    // A reloaded or copied tab replaces the old connection, never creates a third seat.
                    if (!player.clientId.equals(clientId)) player.accessToken = token();
                    player.clientId = clientId;
                    player.lastSeen = touched = System.nanoTime();
                    return credentials(player);
                }
            }
            throw new ApiException(403, "INVALID_SESSION", "Nie można odzyskać miejsca w tym pokoju.");
        }
        synchronized Player authenticate(String accessToken) {
            ensureOpen();
            for (Player player : players) {
                if (sameToken(accessToken, player.accessToken)) {
                    player.lastSeen = touched = System.nanoTime();
                    return player;
                }
            }
            throw new ApiException(403, "INVALID_SESSION", "Brak miejsca w pokoju albo gra jest otwarta w innej karcie.");
        }
        synchronized JsonObject credentials(Player player) {
            JsonObject result = new JsonObject();
            result.addProperty("roomId", id);
            result.addProperty("playerToken", player.resumeToken);
            result.addProperty("accessToken", player.accessToken);
            result.add("state", snapshot(player));
            return result;
        }
        synchronized JsonObject summary() {
            JsonObject json = new JsonObject();
            json.addProperty("id", id); json.addProperty("name", name);
            json.addProperty("createdAt", created.toString());
            json.addProperty("playerCount", players.size());
            json.addProperty("capacity", 2);
            json.addProperty("status", status());
            json.add("players", playerList());
            return json;
        }
        synchronized JsonObject snapshot(Player player) {
            ensureOpen();
            JsonObject json = GameJson.state(id, revision, game.getState());
            json.addProperty("opponent", "ONLINE");
            json.addProperty("computerThinking", false);
            json.add("computerError", JsonNull.INSTANCE);
            JsonObject room = summary();
            room.addProperty("yourSide", player.side.name());
            room.addProperty("canMove", canMove(player));
            room.addProperty("rematchRequested", rematch.contains(player.side));
            room.addProperty("opponentWantsRematch", rematch.contains(player.side.opposite()));
            json.add("room", room);
            json.addProperty("canClaimDraw", canMove(player) && game.getState().canClaimDraw());
            return json;
        }
        synchronized JsonObject moves(Player player, Square from) {
            JsonObject json = new JsonObject();
            json.addProperty("revision", revision);
            JsonArray moves = new JsonArray();
            if (canMove(player)) game.getLegalMoves(from).forEach(move -> moves.add(GameJson.move(move)));
            json.add("moves", moves);
            return json;
        }
        synchronized JsonObject move(Player player, long expected, Move move) {
            checkRevision(expected);
            requireTurn(player);
            MoveResult result = game.makeMove(move);
            if (!result.applied()) throw new ApiException(422, result.error().name(), switch (result.error()) {
                case PROMOTION_REQUIRED -> "Wybierz figurę do promocji.";
                case GAME_OVER -> "Partia została zakończona.";
                default -> "Ten ruch jest niedozwolony.";
            });
            revision++;
            return snapshot(player);
        }
        synchronized JsonObject claimDraw(Player player, long expected) {
            checkRevision(expected); requireTurn(player);
            if (!game.claimDraw()) throw new ApiException(422, "DRAW_UNAVAILABLE", "W tej pozycji nie można zgłosić remisu.");
            revision++;
            return snapshot(player);
        }
        synchronized JsonObject rematch(Player player, long expected) {
            checkRevision(expected);
            if (players.size() != 2 || !game.getState().status().isFinished()) {
                throw new ApiException(409, "GAME_NOT_FINISHED", "Rewanż jest dostępny po zakończeniu partii.");
            }
            rematch.add(player.side);
            if (rematch.size() == 2) {
                players.forEach(p -> p.side = p.side.opposite());
                rematch.clear(); game.newGame(); revision++;
            }
            return snapshot(player);
        }
        private void requireTurn(Player player) {
            ensureOpen();
            if (players.size() != 2) throw new ApiException(409, "WAITING_FOR_PLAYER", "Czekamy na drugiego gracza.");
            if (game.getState().status().isFinished()) throw new ApiException(422, "GAME_OVER", "Partia została zakończona.");
            if (game.getState().sideToMove() != player.side) throw new ApiException(403, "WRONG_SIDE", "Teraz ruch przeciwnika.");
        }
        private void checkRevision(long expected) {
            if (revision != expected) throw new ApiException(409, "STALE_STATE", "Pozycja zmieniła się. Pobrano aktualną planszę.");
        }
        private boolean canMove(Player player) {
            return !closed && players.size() == 2 && !game.getState().status().isFinished()
                    && game.getState().sideToMove() == player.side;
        }
        private String status() { return game.getState().status().isFinished() ? "FINISHED" : players.size() == 2 ? "PLAYING" : "WAITING"; }
        private JsonArray playerList() {
            JsonArray result = new JsonArray();
            for (Player player : players) {
                JsonObject json = new JsonObject();
                json.addProperty("name", player.name); json.addProperty("side", player.side.name());
                json.addProperty("connected", System.nanoTime() - player.lastSeen < Duration.ofSeconds(15).toNanos());
                result.add(json);
            }
            return result;
        }
        private void ensureOpen() {
            if (closed) throw new ApiException(410, "ROOM_CLOSED", "Gracz opuścił pokój lub pokój wygasł. Wróć do lobby.");
        }
        synchronized void close() { closed = true; }
    }
    static final class Player {
        final String name;
        final String resumeToken = token();
        String accessToken = token();
        String clientId;
        Side side;
        long lastSeen = System.nanoTime();
        Player(String name, Side side, String clientId) { this.name = name; this.side = side; this.clientId = clientId; }
    }
}
