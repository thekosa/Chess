package pl.projekt.szachy.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.projekt.szachy.core.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ChessRulesTest {
    @Test
    void standardOpeningHasKnownMoveCountsAtThreeDepths() {
        GameState state = new LocalGame().getState();
        assertEquals(20, perft(state, 1));
        assertEquals(400, perft(state, 2));
        assertEquals(8902, perft(state, 3));
    }

    @Test
    void pawnDirectionsAndTurnsAreEnforcedByTheBackend() {
        LocalGame game = new LocalGame();
        assertEquals(Set.of(sq("e3"), sq("e4")), targets(game, "e2"));
        assertTrue(game.getLegalMoves(sq("e7")).isEmpty());
        GameState before = game.getState();
        play(game, "e2", "e4");
        assertEquals(Side.BLACK, game.getState().sideToMove());
        assertEquals(Set.of(sq("e6"), sq("e5")), targets(game, "e7"));
        assertEquals(MoveError.WRONG_SIDE, game.makeMove(move("e4", "e5")).error());
        assertEquals(new Piece(Side.WHITE, PieceType.PAWN), before.board().get(sq("e2")));
        play(game, "e7", "e5");
        assertEquals(Side.WHITE, game.getState().sideToMove());
    }

    @Test
    void illegalMovesAndEmptyOriginsLeaveTheStateUntouched() {
        LocalGame game = new LocalGame();
        GameState state = game.getState();
        for (Move invalid : List.of(move("e2", "e5"), move("a3", "a4"),
                move("c1", "h6"), move("a1", "a3"), move("d1", "d8"), move("e2", "d3"))) {
            assertEquals(MoveError.ILLEGAL_MOVE, game.makeMove(invalid).error());
            assertSame(state, game.getState());
        }
        assertThrows(UnsupportedOperationException.class, () -> game.getLegalMoves(sq("e2")).clear());
    }

    @Test
    void pawnsCaptureOnlyDiagonallyAndCannotJumpOverAnOccupiedSquare() {
        LocalGame game = game(Side.WHITE, "wKa1", "bKh8", "wPe2", "bPe3", "bPd3", "wPf3");
        assertEquals(Set.of(sq("d3")), targets(game, "e2"));
        play(game, "e2", "d3");
        assertEquals(1, game.getState().capturedPieces().size());
        assertEquals(new Piece(Side.BLACK, PieceType.PAWN), game.getState().capturedPieces().getFirst());
        assertFalse(game.getState().board().containsKey(sq("e2")));
    }

    @Test
    void rookStopsAtFriendlyBlockersAndAtAnEnemyCapture() {
        LocalGame game = game(Side.WHITE, "wKa1", "bKh8", "wRd4", "wPd6", "bPf4");
        assertEquals(squares("d5", "e4", "f4", "c4", "b4", "a4", "d3", "d2", "d1"), targets(game, "d4"));
    }

    @Test
    void bishopStopsAlongEachDiagonal() {
        LocalGame game = game(Side.WHITE, "wKa1", "bKh8", "wBd4", "wPb6", "bPf6");
        assertEquals(squares("e5", "f6", "c5", "e3", "f2", "g1", "c3", "b2"), targets(game, "d4"));
    }

    @Test
    void queenUsesBothStraightAndDiagonalRays() {
        LocalGame game = game(Side.WHITE, "wKa1", "bKh8", "wQd4", "wPd6", "wPb6", "bPf4", "bPf6");
        assertTrue(targets(game, "d4").containsAll(squares("d5", "f4", "f6", "c5", "e3", "c4")));
        assertFalse(targets(game, "d4").contains(sq("d7")));
        assertFalse(targets(game, "d4").contains(sq("g4")));
        assertFalse(targets(game, "d4").contains(sq("g7")));
        assertFalse(targets(game, "d4").contains(sq("b6")));
    }

    @Test
    void knightCanJumpButCannotLandOnAFriendlyPiece() {
        LocalGame game = game(Side.WHITE, "wKa1", "bKh8", "wNd4", "wPf5", "bPb5", "wPd5", "wPe4");
        assertEquals(squares("b3", "b5", "c2", "c6", "e2", "e6", "f3"), targets(game, "d4"));
        play(game, "d4", "b5");
        assertEquals(1, game.getState().capturedPieces().size());
    }

    @Test
    void kingCannotEnterEnemyKingOrBishopAttacks() {
        LocalGame game = game(Side.WHITE, "wKd4", "bKf4", "bBg6", "wPa2");
        assertEquals(squares("c3", "c4", "c5", "d5"), targets(game, "d4"));
    }

    @Test
    void pawnAttacksAnEmptyDiagonalButDoesNotAttackStraightAhead() {
        LocalGame game = game(Side.WHITE, "wKe4", "bKh8", "bPd6");
        assertFalse(targets(game, "e4").contains(sq("e5")));
        assertTrue(targets(game, "e4").contains(sq("d5")));
    }

    @Test
    void aPinnedPieceCannotExposeItsOwnKing() {
        LocalGame game = game(Side.WHITE, "wKe1", "wRe2", "bRe8", "bKa8");
        assertFalse(targets(game, "e2").contains(sq("f2")));
        assertTrue(targets(game, "e2").contains(sq("e3")));
        GameState before = game.getState();
        assertEquals(MoveError.ILLEGAL_MOVE, game.makeMove(move("e2", "f2")).error());
        assertSame(before, game.getState());
    }

    @Test
    void kingsAreNeverCapturedAsAnOrdinaryMove() {
        LocalGame game = game(Side.WHITE, "wKa1", "wQh1", "bKh8");
        assertFalse(targets(game, "h1").contains(sq("h8")));
    }

    @Test
    void aCaptureChangesOnlyTheBackendAndNewGameResetsAllMetadata() {
        LocalGame game = new LocalGame();
        play(game, "e2", "e4");
        play(game, "d7", "d5");
        play(game, "e4", "d5");
        GameState captured = game.getState();
        assertEquals(31, captured.board().size());
        assertEquals(3, captured.moveHistory().size());
        assertEquals(1, captured.capturedPieces().size());
        assertThrows(UnsupportedOperationException.class, () -> captured.moveHistory().clear());
        assertThrows(UnsupportedOperationException.class, () -> captured.capturedPieces().clear());
        GameState reset = game.newGame();
        assertEquals(32, reset.board().size());
        assertEquals(Side.WHITE, reset.sideToMove());
        assertEquals(CastlingRights.ALL, reset.castlingRights());
        assertEquals(GameStatus.ACTIVE, reset.status());
        assertNull(reset.enPassantTarget());
        assertTrue(reset.moveHistory().isEmpty());
        assertTrue(reset.capturedPieces().isEmpty());
        assertEquals(31, captured.board().size());
    }

    @ParameterizedTest
    @EnumSource(Side.class)
    void castlingMovesTheKingAndRookForEitherPlayer(Side side) {
        int rank = side == Side.WHITE ? 0 : 7;
        String suffix = side == Side.WHITE ? "1" : "8";
        String prefix = side == Side.WHITE ? "w" : "b";
        Map<Square, Piece> board = board("wKe1", "bKe8", prefix + "Ra" + suffix, prefix + "Rh" + suffix);
        for (boolean kingSide : new boolean[]{true, false}) {
            LocalGame game = new LocalGame(new GameState(board, side, CastlingRights.ALL, null,
                    GameStatus.ACTIVE, List.of(), List.of()));
            Square from = new Square(4, rank);
            Square to = new Square(kingSide ? 6 : 2, rank);
            assertTrue(game.getLegalMoves(from).contains(new Move(from, to)));
            assertTrue(game.makeMove(new Move(from, to)).applied());
            assertEquals(new Piece(side, PieceType.KING), game.getState().board().get(to));
            assertEquals(new Piece(side, PieceType.ROOK),
                    game.getState().board().get(new Square(kingSide ? 5 : 3, rank)));
            assertFalse(game.getState().board().containsKey(new Square(kingSide ? 7 : 0, rank)));
            assertFalse(game.getState().castlingRights().allows(side, true));
            assertFalse(game.getState().castlingRights().allows(side, false));
        }
    }

    @Test
    void castlingIsForbiddenThroughCheckOutOfCheckAndAcrossBlockers() {
        for (String obstacle : new String[]{"bRf8", "bRe8", "wBf1"}) {
            LocalGame game = new LocalGame(new GameState(board("wKe1", "wRh1", "bKa8", obstacle),
                    Side.WHITE, CastlingRights.ALL, null, GameStatus.ACTIVE, List.of(), List.of()));
            assertFalse(targets(game, "e1").contains(sq("g1")));
        }
    }

    @Test
    void returningARookToItsHomeSquareDoesNotRestoreCastling() {
        LocalGame game = new LocalGame(new GameState(board("wKe1", "wRh1", "bKe8"), Side.WHITE,
                CastlingRights.ALL, null, GameStatus.ACTIVE, List.of(), List.of()));
        play(game, "h1", "h2");
        play(game, "e8", "e7");
        play(game, "h2", "h1");
        play(game, "e7", "e8");
        assertFalse(targets(game, "e1").contains(sq("g1")));
    }

    @Test
    void returningAKingToItsHomeSquareDoesNotRestoreCastling() {
        LocalGame game = new LocalGame(new GameState(board("wKe1", "wRa1", "wRh1", "bKe8"), Side.WHITE,
                CastlingRights.ALL, null, GameStatus.ACTIVE, List.of(), List.of()));
        play(game, "e1", "e2");
        play(game, "e8", "e7");
        play(game, "e2", "e1");
        play(game, "e7", "e8");
        assertFalse(targets(game, "e1").contains(sq("g1")));
        assertFalse(targets(game, "e1").contains(sq("c1")));
    }

    @Test
    void capturingAHomeRookRevokesOnlyThatCastlingRight() {
        LocalGame game = new LocalGame(new GameState(board("wKe1", "wRa1", "wRh1", "bKe8", "bBb2"),
                Side.BLACK, CastlingRights.ALL, null, GameStatus.ACTIVE, List.of(), List.of()));
        play(game, "b2", "a1");
        assertFalse(game.getState().castlingRights().whiteQueenSide());
        assertTrue(game.getState().castlingRights().whiteKingSide());
    }

    @Test
    void enPassantCapturesThePawnBesideTheDestination() {
        LocalGame game = new LocalGame();
        play(game, "e2", "e4");
        play(game, "a7", "a6");
        play(game, "e4", "e5");
        play(game, "d7", "d5");
        assertEquals(sq("d6"), game.getState().enPassantTarget());
        play(game, "e5", "d6");
        assertFalse(game.getState().board().containsKey(sq("d5")));
        assertEquals(new Piece(Side.WHITE, PieceType.PAWN), game.getState().board().get(sq("d6")));
        assertEquals(31, game.getState().board().size());
        assertEquals(1, game.getState().capturedPieces().size());
        assertNull(game.getState().enPassantTarget());
    }

    @Test
    void blackCanCaptureEnPassantInTheOppositeDirection() {
        LocalGame game = new LocalGame();
        play(game, "a2", "a3");
        play(game, "e7", "e5");
        play(game, "a3", "a4");
        play(game, "e5", "e4");
        play(game, "d2", "d4");
        play(game, "e4", "d3");
        assertFalse(game.getState().board().containsKey(sq("d4")));
        assertEquals(new Piece(Side.BLACK, PieceType.PAWN), game.getState().board().get(sq("d3")));
    }

    @Test
    void enPassantExpiresAfterAnotherMove() {
        LocalGame game = new LocalGame();
        play(game, "e2", "e4");
        play(game, "a7", "a6");
        play(game, "e4", "e5");
        play(game, "d7", "d5");
        play(game, "h2", "h3");
        play(game, "h7", "h6");
        assertFalse(targets(game, "e5").contains(sq("d6")));
    }

    @Test
    void enPassantCannotExposeTheKingAlongTheCapturedPawnsRank() {
        LocalGame game = new LocalGame(new GameState(board("wKe5", "wPf5", "bPg5", "bRh5", "bKa8"),
                Side.WHITE, CastlingRights.NONE, sq("g6"), GameStatus.ACTIVE, List.of(), List.of()));
        assertFalse(targets(game, "f5").contains(sq("g6")));
    }

    @ParameterizedTest
    @EnumSource(value = PieceType.class, names = {"QUEEN", "ROOK", "BISHOP", "KNIGHT"})
    void promotionRequiresAChoiceAndSupportsEachAllowedPiece(PieceType type) {
        LocalGame game = game(Side.WHITE, "wKh1", "bKh8", "wPa7");
        GameState before = game.getState();
        assertEquals(4, game.getLegalMoves(sq("a7")).size());
        assertEquals(MoveError.PROMOTION_REQUIRED, game.makeMove(move("a7", "a8")).error());
        assertSame(before, game.getState());
        assertTrue(game.makeMove(new Move(sq("a7"), sq("a8"), type)).applied());
        assertEquals(new Piece(Side.WHITE, type), game.getState().board().get(sq("a8")));
    }

    @Test
    void blackPromotionAndPromotionWithCaptureWork() {
        LocalGame black = game(Side.BLACK, "wKa8", "bKh8", "bPb2");
        assertTrue(black.makeMove(new Move(sq("b2"), sq("b1"), PieceType.BISHOP)).applied());
        assertEquals(new Piece(Side.BLACK, PieceType.BISHOP), black.getState().board().get(sq("b1")));
        LocalGame white = game(Side.WHITE, "wKh1", "bKh8", "wPb7", "bRc8");
        assertTrue(white.makeMove(new Move(sq("b7"), sq("c8"), PieceType.ROOK)).applied());
        assertEquals(1, white.getState().capturedPieces().size());
        assertEquals(new Piece(Side.WHITE, PieceType.ROOK), white.getState().board().get(sq("c8")));
    }

    @Test
    void invalidPromotionDoesNotMutateTheState() {
        LocalGame game = game(Side.WHITE, "wKh1", "bKh8", "wPa7");
        GameState before = game.getState();
        assertEquals(MoveError.ILLEGAL_MOVE,
                game.makeMove(new Move(sq("a7"), sq("a8"), PieceType.KING)).error());
        assertSame(before, game.getState());
    }

    @Test
    void foolsMateEndsTheGameAndRejectsFurtherMoves() {
        LocalGame game = new LocalGame();
        play(game, "f2", "f3");
        play(game, "e7", "e5");
        play(game, "g2", "g4");
        play(game, "d8", "h4");
        assertEquals(GameStatus.CHECKMATE, game.getState().status());
        assertEquals(Side.WHITE, game.getState().sideToMove());
        GameState finalState = game.getState();
        assertTrue(game.getLegalMoves(sq("e1")).isEmpty());
        assertEquals(MoveError.GAME_OVER, game.makeMove(move("e1", "e2")).error());
        assertSame(finalState, game.getState());
    }

    @Test
    void checkIsReportedWhileLegalResponsesRemainAvailable() {
        LocalGame game = game(Side.WHITE, "wKe1", "bKa8", "bRe8");
        assertEquals(GameStatus.CHECK, game.getState().status());
        play(game, "e1", "f1");
        assertEquals(GameStatus.ACTIVE, game.getState().status());
    }

    @Test
    void stalemateIsDistinguishedFromCheckmate() {
        LocalGame game = game(Side.WHITE, "wKa1", "bKc2", "bQb3");
        assertEquals(GameStatus.STALEMATE, game.getState().status());
        assertTrue(game.getState().status().isFinished());
    }

    private static long perft(GameState state, int depth) {
        if (depth == 0) {
            return 1;
        }
        long count = 0;
        for (Square from : state.board().keySet()) {
            for (Move move : ChessRules.legalMoves(state, from)) {
                count += perft(ChessRules.apply(state, move, false), depth - 1);
            }
        }
        return count;
    }

    private static LocalGame game(Side side, String... pieces) {
        return new LocalGame(new GameState(board(pieces), side));
    }

    private static Map<Square, Piece> board(String... pieces) {
        Map<Square, Piece> result = new HashMap<>();
        for (String piece : pieces) {
            PieceType type = switch (piece.charAt(1)) {
                case 'K' -> PieceType.KING;
                case 'Q' -> PieceType.QUEEN;
                case 'R' -> PieceType.ROOK;
                case 'B' -> PieceType.BISHOP;
                case 'N' -> PieceType.KNIGHT;
                case 'P' -> PieceType.PAWN;
                default -> throw new IllegalArgumentException(piece);
            };
            result.put(sq(piece.substring(2)), new Piece(piece.charAt(0) == 'w' ? Side.WHITE : Side.BLACK, type));
        }
        return result;
    }

    private static Set<Square> squares(String... squares) {
        return java.util.Arrays.stream(squares).map(ChessRulesTest::sq).collect(Collectors.toSet());
    }

    private static Set<Square> targets(LocalGame game, String from) {
        return game.getLegalMoves(sq(from)).stream().map(Move::to).collect(Collectors.toSet());
    }

    private static Square sq(String name) {
        return new Square(name.charAt(0) - 'a', name.charAt(1) - '1');
    }

    private static Move move(String from, String to) {
        return new Move(sq(from), sq(to));
    }

    private static void play(LocalGame game, String from, String to) {
        MoveResult result = game.makeMove(move(from, to));
        assertTrue(result.applied(), () -> from + "-" + to + ": " + result.error());
    }
}
