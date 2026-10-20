package pl.projekt.szachy.core.model;

import java.util.Objects;

public record Move(Square from, Square to, PieceType promotion) {
    public Move {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
    }

    public Move(Square from, Square to) {
        this(from, to, null);
    }
}
