package pl.projekt.szachy.core.api;

import pl.projekt.szachy.core.model.GameState;
import pl.projekt.szachy.core.model.Move;
import pl.projekt.szachy.core.model.MoveResult;
import pl.projekt.szachy.core.model.Square;

import java.util.List;

/** Queries expose snapshots; commands change the state owned by the engine. */
public interface GameApi {
    GameState getState();

    GameState newGame();

    List<Move> getLegalMoves(Square from);

    MoveResult makeMove(Move move);

    /** Claims a draw in the current position; false leaves the game unchanged. */
    default boolean claimDraw() { return false; }
}
