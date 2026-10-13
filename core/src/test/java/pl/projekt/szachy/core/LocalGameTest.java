package pl.projekt.szachy.core;

import org.junit.jupiter.api.Test;
import pl.projekt.szachy.core.api.GameApi;
import pl.projekt.szachy.core.model.GameState;
import pl.projekt.szachy.core.model.Piece;
import pl.projekt.szachy.core.model.PieceType;
import pl.projekt.szachy.core.model.Side;
import pl.projekt.szachy.core.model.Square;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class LocalGameTest {
    @Test
    void startsWithACompleteStandardPositionAndWhiteToMove() {
        GameApi game = new LocalGame();
        GameState state = game.getState();
        assertEquals(Side.WHITE, state.sideToMove());
        assertEquals(32, state.board().size());
        assertEquals(16, state.board().values().stream().filter(p -> p.side() == Side.WHITE).count());
        assertEquals(16, state.board().values().stream().filter(p -> p.side() == Side.BLACK).count());
        PieceType[] expected = {PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP,
                PieceType.QUEEN, PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK};
        for (int file = 0; file < 8; file++) {
            assertEquals(new Piece(Side.WHITE, expected[file]), state.board().get(new Square(file, 0)));
            assertEquals(new Piece(Side.BLACK, expected[file]), state.board().get(new Square(file, 7)));
            assertEquals(new Piece(Side.WHITE, PieceType.PAWN), state.board().get(new Square(file, 1)));
            assertEquals(new Piece(Side.BLACK, PieceType.PAWN), state.board().get(new Square(file, 6)));
            for (int rank = 2; rank <= 5; rank++) {
                assertFalse(state.board().containsKey(new Square(file, rank)));
            }
        }
    }

    @Test
    void frontendCannotModifyEngineStateThroughTheApi() {
        GameApi game = new LocalGame();
        GameState snapshot = game.getState();
        assertThrows(UnsupportedOperationException.class, snapshot.board()::clear);
        assertEquals(32, game.getState().board().size());
    }

    @Test
    void snapshotDoesNotRetainAMutableCallerOwnedBoard() {
        var board = new HashMap<Square, Piece>();
        board.put(new Square(4, 0), new Piece(Side.WHITE, PieceType.KING));
        GameState snapshot = new GameState(board, Side.WHITE);
        board.clear();
        assertEquals(1, snapshot.board().size());
    }

    @Test
    void newGameReplacesStateWithoutChangingPreviousSnapshots() {
        GameApi game = new LocalGame();
        GameState previous = game.getState();
        GameState fresh = game.newGame();
        assertNotSame(previous, fresh);
        assertSame(fresh, game.getState());
        assertEquals(previous, fresh);
        assertEquals(32, previous.board().size());
    }

    @Test
    void separateGamesDoNotShareState() {
        GameApi first = new LocalGame();
        GameApi second = new LocalGame();
        GameState otherState = second.getState();
        first.newGame();
        assertSame(otherState, second.getState());
        assertNotSame(first.getState(), second.getState());
    }

    @Test
    void invalidSquaresCannotEnterTheModel() {
        for (int invalid : new int[]{-1, 8}) {
            assertThrows(IllegalArgumentException.class, () -> new Square(invalid, 0));
            assertThrows(IllegalArgumentException.class, () -> new Square(0, invalid));
        }
        assertEquals("a1", new Square(0, 0).toString());
        assertEquals("h8", new Square(7, 7).toString());
    }
}
