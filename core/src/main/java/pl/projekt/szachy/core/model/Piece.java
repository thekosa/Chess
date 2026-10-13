package pl.projekt.szachy.core.model;

import java.util.Objects;

/** A piece's identity. Its location belongs to the board, not to this object. */
public record Piece(Side side, PieceType type) {
    public Piece {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(type, "type");
    }
}
