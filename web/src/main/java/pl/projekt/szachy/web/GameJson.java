package pl.projekt.szachy.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import pl.projekt.szachy.core.model.*;

/** Explicit transport format. The engine does not know about JSON or HTTP. */
final class GameJson {
    private GameJson() { }

    static JsonObject state(String id, long revision, GameState state) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("revision", revision);
        json.addProperty("sideToMove", state.sideToMove().name());
        json.addProperty("status", state.status().name());
        json.addProperty("halfmoveClock", state.halfmoveClock());
        json.addProperty("repetitionCount", state.repetitionCount());
        json.addProperty("canClaimDraw", state.canClaimDraw());
        JsonObject rights = new JsonObject();
        rights.addProperty("whiteKingSide", state.castlingRights().whiteKingSide());
        rights.addProperty("whiteQueenSide", state.castlingRights().whiteQueenSide());
        rights.addProperty("blackKingSide", state.castlingRights().blackKingSide());
        rights.addProperty("blackQueenSide", state.castlingRights().blackQueenSide());
        json.add("castlingRights", rights);
        json.addProperty("enPassantTarget", state.enPassantTarget() == null ? null : state.enPassantTarget().toString());
        JsonArray board = new JsonArray();
        state.board().entrySet().stream().sorted(java.util.Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    JsonObject piece = piece(entry.getValue());
                    piece.addProperty("square", entry.getKey().toString());
                    board.add(piece);
                });
        json.add("board", board);
        JsonArray history = new JsonArray();
        state.moveHistory().forEach(move -> history.add(move(move)));
        json.add("history", history);
        JsonArray captured = new JsonArray();
        state.capturedPieces().forEach(piece -> captured.add(piece(piece)));
        json.add("capturedPieces", captured);
        return json;
    }

    static JsonObject move(Move move) {
        JsonObject json = new JsonObject();
        json.addProperty("from", move.from().toString());
        json.addProperty("to", move.to().toString());
        json.add("promotion", move.promotion() == null ? JsonNull.INSTANCE
                : new com.google.gson.JsonPrimitive(move.promotion().name()));
        return json;
    }

    private static JsonObject piece(Piece piece) {
        JsonObject json = new JsonObject();
        json.addProperty("side", piece.side().name());
        json.addProperty("type", piece.type().name());
        return json;
    }
}
