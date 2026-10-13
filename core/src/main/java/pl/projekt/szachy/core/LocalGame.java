package pl.projekt.szachy.core;

import pl.projekt.szachy.core.api.GameApi;
import pl.projekt.szachy.core.model.GameState;
import pl.projekt.szachy.core.model.Piece;
import pl.projekt.szachy.core.model.PieceType;
import pl.projekt.szachy.core.model.Side;
import pl.projekt.szachy.core.model.Square;

import java.util.HashMap;
import java.util.Map;

/** Owns a single local game. No UI classes or dependencies belong here. */
public final class LocalGame implements GameApi {
    private GameState state;

    public LocalGame() {
        newGame();
    }

    @Override
    public GameState getState() {
        return state;
    }

    @Override
    public GameState newGame() {
        Map<Square, Piece> board = new HashMap<>();
        PieceType[] backRank = {
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        };
        for (int file = 0; file < Square.BOARD_SIZE; file++) {
            board.put(new Square(file, 0), new Piece(Side.WHITE, backRank[file]));
            board.put(new Square(file, 1), new Piece(Side.WHITE, PieceType.PAWN));
            board.put(new Square(file, 6), new Piece(Side.BLACK, PieceType.PAWN));
            board.put(new Square(file, 7), new Piece(Side.BLACK, backRank[file]));
        }
        state = new GameState(board, Side.WHITE);
        return state;
    }
}
