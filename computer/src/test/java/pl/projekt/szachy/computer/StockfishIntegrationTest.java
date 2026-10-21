package pl.projekt.szachy.computer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.projekt.szachy.core.model.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Opt in explicitly: may install the official engine if it is not cached yet. */
@EnabledIfSystemProperty(named = "chess.stockfish.tests", matches = "true")
@Timeout(90)
class StockfishIntegrationTest {
    @ParameterizedTest @EnumSource(Difficulty.class)
    void nativeOpponentPlaysLegalMovesOnEveryLevel(Difficulty difficulty) throws Exception {
        try (ComputerGame game = new ComputerGame(new GameOptions(Opponent.COMPUTER, difficulty, Side.WHITE))) {
            assertTrue(game.makeMove(move("e2", "e4")).applied());
            assertEquals(MoveError.WRONG_SIDE, game.makeMove(move("e7", "e5")).error());
            assertTrue(game.getLegalMoves(square("e7")).isEmpty());
            waitForComputer(game);
            assertEquals(2, game.getState().moveHistory().size());
            assertEquals(Side.WHITE, game.getState().sideToMove());
            assertEquals(2, game.revision());
        }
    }
    @Test void computerCanStartAndResetDiscardsOldJobs() throws Exception {
        try (ComputerGame game = new ComputerGame(new GameOptions(Opponent.COMPUTER, Difficulty.EASY, Side.BLACK))) {
            waitForComputer(game);
            assertEquals(1, game.getState().moveHistory().size());
            assertEquals(Side.BLACK, game.getState().sideToMove());
            game.configure(new GameOptions(Opponent.COMPUTER, Difficulty.HARD, Side.BLACK));
            game.configure(GameOptions.local());
            GameState reset = game.getState();
            Thread.sleep(1500);
            assertSame(reset, game.getState());
            assertFalse(game.snapshot().thinking());
            assertEquals(32, reset.board().size());
        }
    }
    @Test void nativeEnginePlaysBothSidesWithoutBreakingTheRules() throws Exception {
        try (ComputerGame game = new ComputerGame(GameOptions.local())) {
            for (int ply = 0; ply < 12 && !game.getState().status().isFinished(); ply++) {
                try (StockfishClient engine = new StockfishClient()) {
                    Move move = engine.bestMove(game.getState(), Difficulty.EASY);
                    assertTrue(game.getLegalMoves(move.from()).contains(move));
                    assertTrue(game.makeMove(move).applied());
                }
            }
            assertTrue(game.getState().moveHistory().size() == 12 || game.getState().status().isFinished());
        }
    }
    private static void waitForComputer(ComputerGame game) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(75);
        while (game.snapshot().thinking() && System.nanoTime() < deadline) Thread.sleep(30);
        assertFalse(game.snapshot().thinking(), "Computer timed out");
        assertNull(game.snapshot().error());
    }
    private static Square square(String name) { return new Square(name.charAt(0) - 'a', name.charAt(1) - '1'); }
    private static Move move(String from, String to) { return new Move(square(from), square(to)); }
}
