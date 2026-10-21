package pl.projekt.szachy.core.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** An immutable snapshot. Empty squares are absent from the board map. */
public record GameState(Map<Square, Piece> board, Side sideToMove,
                        CastlingRights castlingRights, Square enPassantTarget,
                        GameStatus status, List<Move> moveHistory, List<Piece> capturedPieces,
                        int halfmoveClock, int repetitionCount) {
    public GameState {
        board = Map.copyOf(Objects.requireNonNull(board, "board"));
        Objects.requireNonNull(sideToMove, "sideToMove");
        Objects.requireNonNull(castlingRights, "castlingRights");
        Objects.requireNonNull(status, "status");
        moveHistory = List.copyOf(Objects.requireNonNull(moveHistory, "moveHistory"));
        capturedPieces = List.copyOf(Objects.requireNonNull(capturedPieces, "capturedPieces"));
        if (halfmoveClock < 0 || repetitionCount < 1) throw new IllegalArgumentException("Invalid draw counters");
    }

    public GameState(Map<Square, Piece> board, Side sideToMove, CastlingRights castlingRights,
                     Square enPassantTarget, GameStatus status, List<Move> moveHistory, List<Piece> capturedPieces) {
        this(board, sideToMove, castlingRights, enPassantTarget, status, moveHistory, capturedPieces, 0, 1);
    }

    public boolean canClaimDraw() {
        return !status.isFinished() && (halfmoveClock >= 100 || repetitionCount >= 3);
    }

    /** Custom positions have no inferred right to castle. */
    public GameState(Map<Square, Piece> board, Side sideToMove) {
        this(board, sideToMove, CastlingRights.NONE, null, GameStatus.ACTIVE, List.of(), List.of());
    }

    public GameState withStatus(GameStatus newStatus) {
        return new GameState(board, sideToMove, castlingRights, enPassantTarget,
                newStatus, moveHistory, capturedPieces, halfmoveClock, repetitionCount);
    }

    public GameState withRepetitionCount(int count) {
        return new GameState(board, sideToMove, castlingRights, enPassantTarget,
                status, moveHistory, capturedPieces, halfmoveClock, count);
    }
}
