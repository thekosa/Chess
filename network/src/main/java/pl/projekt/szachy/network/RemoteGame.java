package pl.projekt.szachy.network;

import com.google.gson.*;
import pl.projekt.szachy.core.api.GameApi;
import pl.projekt.szachy.core.model.*;
import java.util.*;
import java.util.concurrent.*;

/** Cached server state plus commands. Background polling keeps terminal input responsive. */
public final class RemoteGame implements GameApi, AutoCloseable {
    private final OnlineClient client;
    private final String id;
    private final String accessToken;
    private final String playerToken;
    private final ScheduledExecutorService polling = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "chess-room-poll"); thread.setDaemon(true); return thread;
    });
    private volatile GameState state;
    private volatile JsonObject snapshot;
    private volatile String fault;
    private volatile boolean closed;

    RemoteGame(OnlineClient client, JsonObject credentials) {
        this.client = client; id = credentials.get("roomId").getAsString();
        accessToken = credentials.get("accessToken").getAsString(); playerToken = credentials.get("playerToken").getAsString();
        install(credentials.getAsJsonObject("state"));
        polling.scheduleWithFixedDelay(this::refresh, 400, 500, TimeUnit.MILLISECONDS);
    }
    public String roomId() { return id; }
    public String playerToken() { return playerToken; }
    public Side yourSide() { return Side.valueOf(snapshot.getAsJsonObject("room").get("yourSide").getAsString()); }
    public boolean canMove() { return fault == null && snapshot.getAsJsonObject("room").get("canMove").getAsBoolean(); }
    public String message() {
        if (fault != null) return fault;
        JsonObject room = snapshot.getAsJsonObject("room");
        if (room.get("status").getAsString().equals("WAITING")) return "Pokoj: " + room.get("name").getAsString() + " | Czekamy na drugiego gracza.";
        if (room.get("rematchRequested").getAsBoolean()) return "Rewanz zaproponowany. Czekamy na zgode przeciwnika.";
        if (room.get("opponentWantsRematch").getAsBoolean()) return "Przeciwnik chce rewanzu. Wpisz new, aby zaakceptowac.";
        return "ONLINE | Grasz " + (yourSide() == Side.WHITE ? "bialymi" : "czarnymi")
                + " | " + (canMove() ? "Twoja kolej." : state.status().isFinished() ? "Koniec partii. Rewanz: new." : "Ruch przeciwnika.");
    }
    @Override public GameState getState() { return state; }
    @Override public synchronized List<Move> getLegalMoves(Square from) {
        if (!canMove()) return List.of();
        JsonArray values = call("/moves?from=" + from, null).getAsJsonArray("moves");
        List<Move> moves = new ArrayList<>(); for (JsonElement value : values) moves.add(move(value.getAsJsonObject()));
        return List.copyOf(moves);
    }
    @Override public synchronized MoveResult makeMove(Move move) {
        JsonObject body = command(); body.addProperty("from", move.from().toString()); body.addProperty("to", move.to().toString());
        if (move.promotion() != null) body.addProperty("promotion", move.promotion().name());
        try { install(call("/moves", body)); return new MoveResult(MoveError.NONE, state); }
        catch (OnlineClient.OnlineException exception) {
            String code = exception.data.has("error") ? exception.data.get("error").getAsString() : "";
            MoveError error = switch (code) {
                case "WRONG_SIDE", "WAITING_FOR_PLAYER" -> MoveError.WRONG_SIDE;
                case "GAME_OVER" -> MoveError.GAME_OVER;
                case "PROMOTION_REQUIRED" -> MoveError.PROMOTION_REQUIRED;
                case "ILLEGAL_MOVE", "STALE_STATE" -> MoveError.ILLEGAL_MOVE;
                default -> throw exception;
            };
            return new MoveResult(error, state);
        }
    }
    @Override public synchronized boolean claimDraw() {
        try { install(call("/claim-draw", command())); return true; }
        catch (OnlineClient.OnlineException exception) {
            if (exception.data.has("error") && exception.data.get("error").getAsString().equals("DRAW_UNAVAILABLE")) return false;
            throw exception;
        }
    }
    @Override public synchronized GameState newGame() { install(call("/rematch", command())); return state; }
    private JsonObject command() {
        JsonObject body = new JsonObject(); body.addProperty("revision", snapshot.get("revision").getAsLong()); return body;
    }
    private synchronized void refresh() {
        if (closed) return;
        try { install(call("", null)); fault = null; }
        catch (IllegalArgumentException exception) {
            fault = exception.getMessage();
            if (exception instanceof OnlineClient.OnlineException online
                    && (online.status == 403 || online.status == 404 || online.status == 410)) polling.shutdown();
        }
    }
    private JsonObject call(String suffix, JsonObject body) {
        if (closed) throw new IllegalArgumentException("Opuszczono pokoj.");
        try { return client.request("rooms/" + id + suffix, body, accessToken); }
        catch (OnlineClient.OnlineException exception) {
            if (exception.data.has("state")) install(exception.data.getAsJsonObject("state"));
            throw exception;
        }
    }
    private void install(JsonObject next) {
        JsonObject previous = snapshot;
        snapshot = next;
        if (previous != null && previous.get("revision").equals(next.get("revision"))) return;
        Map<Square, Piece> board = new HashMap<>();
        for (JsonElement value : next.getAsJsonArray("board")) {
            JsonObject p = value.getAsJsonObject(); board.put(square(p.get("square").getAsString()), piece(p));
        }
        List<Move> history = new ArrayList<>(); for (JsonElement value : next.getAsJsonArray("history")) history.add(move(value.getAsJsonObject()));
        List<Piece> captures = new ArrayList<>(); for (JsonElement value : next.getAsJsonArray("capturedPieces")) captures.add(piece(value.getAsJsonObject()));
        JsonObject rights = next.getAsJsonObject("castlingRights");
        state = new GameState(board, Side.valueOf(next.get("sideToMove").getAsString()),
                new CastlingRights(rights.get("whiteKingSide").getAsBoolean(), rights.get("whiteQueenSide").getAsBoolean(),
                        rights.get("blackKingSide").getAsBoolean(), rights.get("blackQueenSide").getAsBoolean()),
                next.get("enPassantTarget").isJsonNull() ? null : square(next.get("enPassantTarget").getAsString()),
                GameStatus.valueOf(next.get("status").getAsString()), history, captures,
                next.get("halfmoveClock").getAsInt(), next.get("repetitionCount").getAsInt());
    }
    private static Square square(String value) { return new Square(value.charAt(0) - 'a', value.charAt(1) - '1'); }
    private static Piece piece(JsonObject value) { return new Piece(Side.valueOf(value.get("side").getAsString()), PieceType.valueOf(value.get("type").getAsString())); }
    private static Move move(JsonObject value) {
        return new Move(square(value.get("from").getAsString()), square(value.get("to").getAsString()),
                !value.has("promotion") || value.get("promotion").isJsonNull() ? null : PieceType.valueOf(value.get("promotion").getAsString()));
    }
    @Override public void close() {
        closed = true; polling.shutdownNow();
        try { client.request("rooms/" + id + "/leave", new JsonObject(), accessToken); } catch (IllegalArgumentException ignored) { }
    }
}
