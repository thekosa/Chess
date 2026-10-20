package pl.projekt.szachy.core.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** An immutable snapshot. Empty squares are absent from the board map. */
public record GameState(Map<Square, Piece> board, Side sideToMove,
                        CastlingRights castlingRights, Square enPassantTarget,
                        GameStatus status, List<Move> moveHistory, List<Piece> capturedPieces) {
    public GameState {
        board = Map.copyOf(Objects.requireNonNull(board, "board"));
        Objects.requireNonNull(sideToMove, "sideToMove");
        Objects.requireNonNull(castlingRights, "castlingRights");
        Objects.requireNonNull(status, "status");
        moveHistory = List.copyOf(Objects.requireNonNull(moveHistory, "moveHistory"));
        capturedPieces = List.copyOf(Objects.requireNonNull(capturedPieces, "capturedPieces"));
    }

    /** Custom positions have no inferred right to castle. */
    public GameState(Map<Square, Piece> board, Side sideToMove) {
        this(board, sideToMove, CastlingRights.NONE, null, GameStatus.ACTIVE, List.of(), List.of());
    }

    public GameState withStatus(GameStatus newStatus) {
        return new GameState(board, sideToMove, castlingRights, enPassantTarget,
                newStatus, moveHistory, capturedPieces);
    }
}
