package pl.projekt.szachy.core.model;

import java.util.Objects;

/** Rejected commands return the unchanged state and a frontend-independent reason. */
public record MoveResult(MoveError error, GameState state) {
    public MoveResult {
        Objects.requireNonNull(error, "error");
        Objects.requireNonNull(state, "state");
    }

    public boolean applied() {
        return error == MoveError.NONE;
    }
}
