package pl.projekt.szachy.core.model;

import java.util.Map;
import java.util.Objects;

/** An immutable snapshot. Empty squares are absent from the board map. */
public record GameState(Map<Square, Piece> board, Side sideToMove) {
    public GameState {
        board = Map.copyOf(Objects.requireNonNull(board, "board"));
        Objects.requireNonNull(sideToMove, "sideToMove");
    }
}
