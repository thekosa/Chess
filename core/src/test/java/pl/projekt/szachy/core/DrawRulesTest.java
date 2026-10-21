package pl.projekt.szachy.core;

import org.junit.jupiter.api.Test;
import pl.projekt.szachy.core.model.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DrawRulesTest {
    @Test void threefoldCanBeClaimedAndFivefoldEndsAutomatically() {
        LocalGame game = new LocalGame();
        cycle(game); cycle(game);
        assertEquals(3, game.getState().repetitionCount());
        assertTrue(game.getState().canClaimDraw());
        assertFalse(game.getState().status().isFinished());
        assertTrue(game.claimDraw());
        assertEquals(GameStatus.DRAW_REPETITION, game.getState().status());
        assertEquals(MoveError.GAME_OVER, game.makeMove(move("e2", "e4")).error());
        game.newGame();
        for (int i = 0; i < 4; i++) cycle(game);
        assertEquals(GameStatus.DRAW_FIVEFOLD_REPETITION, game.getState().status());
        assertTrue(game.getLegalMoves(sq("e2")).isEmpty());
        assertFalse(game.claimDraw());
    }
    @Test void unsuccessfulClaimLeavesStateUnchangedAndResetClearsCounters() {
        LocalGame game = new LocalGame();
        GameState before = game.getState();
        assertFalse(game.claimDraw());
        assertSame(before, game.getState());
        cycle(game); cycle(game);
        game.newGame();
        assertEquals(1, game.getState().repetitionCount());
        assertEquals(0, game.getState().halfmoveClock());
        assertFalse(game.getState().canClaimDraw());
    }
    @Test void fiftyMovesRequireClaimButSeventyFiveEndAutomatically() {
        LocalGame game = new LocalGame(position(99, "wKa1", "bKh8", "wRb2"));
        play(game, "b2", "b3");
        assertEquals(100, game.getState().halfmoveClock());
        assertTrue(game.getState().canClaimDraw());
        assertTrue(game.claimDraw());
        assertEquals(GameStatus.DRAW_FIFTY_MOVES, game.getState().status());
        game = new LocalGame(position(149, "wKa1", "bKh8", "wRb2"));
        play(game, "b2", "b3");
        assertEquals(GameStatus.DRAW_SEVENTY_FIVE_MOVES, game.getState().status());
    }
    @Test void pawnMovesAndCapturesResetTheHalfmoveClock() {
        LocalGame game = new LocalGame(position(99, "wKa1", "bKh8", "wPe2", "wRb2", "bNb4"));
        play(game, "e2", "e4");
        assertEquals(0, game.getState().halfmoveClock());
        game = new LocalGame(position(99, "wKa1", "bKh8", "wRb2", "bNb4"));
        play(game, "b2", "b4");
        assertEquals(0, game.getState().halfmoveClock());
    }
    @Test void checkmateTakesPrecedenceOverSeventyFiveMoves() {
        LocalGame game = new LocalGame(position(149, "wKf6", "wQg6", "bKh8"));
        play(game, "g6", "g7");
        assertEquals(GameStatus.CHECKMATE, game.getState().status());
    }
    @Test void onlyProvenInsufficientMaterialPositionsAreStopped() {
        for (String[] pieces : List.of(new String[]{"wKa1", "bKh8"},
                new String[]{"wKa1", "bKh8", "wNb2"}, new String[]{"wKa1", "bKh8", "wBc1"},
                new String[]{"wKa1", "bKh8", "wBc1", "bBf8"})) {
            assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, new LocalGame(position(0, pieces)).getState().status());
        }
        for (String[] pieces : List.of(new String[]{"wKa1", "bKh8", "wNb2", "wNc2"},
                new String[]{"wKa1", "bKh8", "wBc1", "bBc8"}, new String[]{"wKa1", "bKh8", "wNb2", "bNg7"})) {
            assertFalse(new LocalGame(position(0, pieces)).getState().status().isFinished());
        }
    }
    @Test void repetitionKeyIncludesTurnCastlingAndOnlyLegalEnPassant() {
        GameState state = position(0, "wKe1", "bKe8", "wRa1", "wRh1", "wPe5", "bPd5");
        GameState ep = new GameState(state.board(), Side.WHITE, CastlingRights.NONE, sq("d6"),
                GameStatus.ACTIVE, List.of(), List.of());
        assertNotEquals(ChessRules.positionKey(state), ChessRules.positionKey(ep));
        GameState pinned = position(0, "wKe1", "bKa8", "wPe5", "bPd5", "bRe8");
        GameState pinnedEp = new GameState(pinned.board(), Side.WHITE, CastlingRights.NONE, sq("d6"),
                GameStatus.ACTIVE, List.of(), List.of());
        assertEquals(ChessRules.positionKey(pinned), ChessRules.positionKey(pinnedEp));
        GameState noCaptureEp = new GameState(state.board(), Side.WHITE, CastlingRights.NONE, sq("a6"),
                GameStatus.ACTIVE, List.of(), List.of());
        assertEquals(ChessRules.positionKey(state), ChessRules.positionKey(noCaptureEp));
        assertNotEquals(ChessRules.positionKey(state), ChessRules.positionKey(new GameState(state.board(), Side.BLACK)));
        assertNotEquals(ChessRules.positionKey(state), ChessRules.positionKey(new GameState(state.board(), Side.WHITE,
                CastlingRights.ALL, null, GameStatus.ACTIVE, List.of(), List.of())));
    }
    private static void cycle(LocalGame game) {
        play(game, "g1", "f3"); play(game, "g8", "f6"); play(game, "f3", "g1"); play(game, "f6", "g8");
    }
    private static void play(LocalGame game, String from, String to) { assertTrue(game.makeMove(move(from, to)).applied()); }
    private static Move move(String from, String to) { return new Move(sq(from), sq(to)); }
    private static Square sq(String name) { return new Square(name.charAt(0) - 'a', name.charAt(1) - '1'); }
    private static GameState position(int halfmoves, String... pieces) {
        Map<Square, Piece> board = new HashMap<>();
        for (String piece : pieces) {
            PieceType type = switch (piece.charAt(1)) {
                case 'K' -> PieceType.KING; case 'Q' -> PieceType.QUEEN; case 'R' -> PieceType.ROOK;
                case 'B' -> PieceType.BISHOP; case 'N' -> PieceType.KNIGHT; default -> PieceType.PAWN;
            };
            board.put(sq(piece.substring(2)), new Piece(piece.charAt(0) == 'w' ? Side.WHITE : Side.BLACK, type));
        }
        return new GameState(board, Side.WHITE, CastlingRights.NONE, null, GameStatus.ACTIVE, List.of(), List.of(), halfmoves, 1);
    }
}
