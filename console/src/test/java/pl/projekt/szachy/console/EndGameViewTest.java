package pl.projekt.szachy.console;

import org.junit.jupiter.api.Test;
import pl.projekt.szachy.core.LocalGame;
import pl.projekt.szachy.core.model.*;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class EndGameViewTest {
    @Test void celebrationFramesFitEveryBoardSizeAndDoNotChangeThePosition() {
        GameState state = new LocalGame().getState().withStatus(GameStatus.CHECKMATE);
        for (AsciiBoard.Size size : AsciiBoard.Size.values()) {
            AsciiBoard board = new AsciiBoard(size);
            TerminalScreen.Frame still = board.frame(state, null, Set.of(), "");
            for (double time : new double[]{0, .5, 2, 4, 6}) {
                TerminalScreen.Frame frame = board.celebrationFrame(state, time, "");
                assertEquals(still.width, frame.width);
                assertEquals(still.height, frame.height);
            }
        }
        assertEquals(32, state.board().size());
        assertTrue(state.moveHistory().isEmpty());
    }
    @Test void allDrawCausesHaveAReadableTerminalMessage() {
        AsciiBoard board = new AsciiBoard(AsciiBoard.Size.FULL);
        for (GameStatus status : GameStatus.values()) {
            if (!status.isFinished() || status == GameStatus.CHECKMATE) continue;
            var frame = board.frame(new LocalGame().getState().withStatus(status), null, Set.of(), "");
            StringBuilder text = new StringBuilder();
            for (char[] row : frame.chars) text.append(row);
            assertTrue(text.toString().toUpperCase().contains("REMIS"));
        }
    }
}
