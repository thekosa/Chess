package pl.projekt.szachy.web;

import org.junit.jupiter.api.Test;
import pl.projekt.szachy.computer.GameOptions;
import pl.projekt.szachy.core.model.*;
import static org.junit.jupiter.api.Assertions.*;

class GameSessionsTest {
    @Test void drawClaimsChangeRevisionAndJsonWithoutAddingAnArtificialMove() {
        try (GameSessions sessions = new GameSessions()) {
            var session = sessions.create(GameOptions.local());
            for (int i = 0; i < 2; i++) {
                play(session, "g1", "f3"); play(session, "g8", "f6");
                play(session, "f3", "g1"); play(session, "f6", "g8");
            }
            var json = session.snapshot();
            assertTrue(json.get("canClaimDraw").getAsBoolean());
            assertEquals(3, json.get("repetitionCount").getAsInt());
            assertEquals(8, json.get("halfmoveClock").getAsInt());
            assertTrue(session.game.claimDraw());
            json = session.snapshot();
            assertEquals("DRAW_REPETITION", json.get("status").getAsString());
            assertEquals(9, json.get("revision").getAsInt());
            assertEquals(8, json.getAsJsonArray("history").size());
            assertFalse(json.get("canClaimDraw").getAsBoolean());
            assertFalse(session.game.claimDraw());
            assertEquals(9, session.game.revision());
            session.game.newGame();
            assertEquals(10, session.game.revision());
            assertEquals("ACTIVE", session.snapshot().get("status").getAsString());
        }
    }
    @Test void sessionsRemainIndependentAndShutdownDisallowsNewGames() {
        GameSessions sessions = new GameSessions();
        var first = sessions.create(GameOptions.local());
        var second = sessions.create(GameOptions.local());
        play(first, "e2", "e4");
        assertEquals(1, first.game.revision());
        assertEquals(0, second.game.revision());
        assertFalse(second.game.claimDraw());
        sessions.close();
        assertThrows(ApiException.class, () -> sessions.create(GameOptions.local()));
        assertThrows(ApiException.class, () -> sessions.get(first.id));
    }
    private static void play(GameSessions.Session session, String from, String to) {
        assertTrue(session.game.makeMove(new Move(square(from), square(to))).applied());
    }
    private static Square square(String name) { return new Square(name.charAt(0) - 'a', name.charAt(1) - '1'); }
}
