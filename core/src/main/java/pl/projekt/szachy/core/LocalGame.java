package pl.projekt.szachy.core;

import pl.projekt.szachy.core.api.GameApi;
import pl.projekt.szachy.core.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Owns a single local game. All commands are validated here, independently of the UI. */
public final class LocalGame implements GameApi {
    private GameState state;
    private final Map<ChessRules.PositionKey, Integer> repetitions = new HashMap<>();

    public LocalGame() {
        newGame();
    }

    /** Package-local position setup for rule verification. */
    LocalGame(GameState position) {
        state = ChessRules.updateStatus(Objects.requireNonNull(position));
        repetitions.put(ChessRules.positionKey(state), state.repetitionCount());
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
        state = new GameState(board, Side.WHITE, CastlingRights.ALL, null,
                GameStatus.ACTIVE, List.of(), List.of());
        repetitions.clear();
        repetitions.put(ChessRules.positionKey(state), 1);
        return state;
    }

    @Override
    public List<Move> getLegalMoves(Square from) {
        Objects.requireNonNull(from, "from");
        return state.status().isFinished() ? List.of() : ChessRules.legalMoves(state, from);
    }

    @Override
    public MoveResult makeMove(Move move) {
        Objects.requireNonNull(move, "move");
        if (state.status().isFinished()) {
            return new MoveResult(MoveError.GAME_OVER, state);
        }
        Piece piece = state.board().get(move.from());
        if (piece == null) {
            return new MoveResult(MoveError.ILLEGAL_MOVE, state);
        }
        if (piece.side() != state.sideToMove()) {
            return new MoveResult(MoveError.WRONG_SIDE, state);
        }
        List<Move> legal = ChessRules.legalMoves(state, move.from());
        if (!legal.contains(move)) {
            boolean promotionNeeded = move.promotion() == null && legal.stream()
                    .anyMatch(candidate -> candidate.to().equals(move.to()) && candidate.promotion() != null);
            return new MoveResult(promotionNeeded ? MoveError.PROMOTION_REQUIRED : MoveError.ILLEGAL_MOVE, state);
        }
        GameState next = ChessRules.apply(state, move, true);
        int count = repetitions.merge(ChessRules.positionKey(next), 1, Integer::sum);
        state = ChessRules.updateStatus(next.withRepetitionCount(count));
        return new MoveResult(MoveError.NONE, state);
    }

    @Override public boolean claimDraw() {
        if (!state.canClaimDraw()) return false;
        state = state.withStatus(state.repetitionCount() >= 3 ? GameStatus.DRAW_REPETITION : GameStatus.DRAW_FIFTY_MOVES);
        return true;
    }
}
