package pl.projekt.szachy.web;

import pl.projekt.szachy.core.LocalGame;
import pl.projekt.szachy.core.api.GameApi;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Each browser game has its own engine. Callers serialize commands on a Session. */
final class GameSessions {
    private static final long IDLE_TIMEOUT = Duration.ofHours(2).toNanos();
    private final Map<String, Session> sessions = new HashMap<>();

    synchronized Session create() {
        long now = System.nanoTime();
        sessions.values().removeIf(session -> now - session.touched > IDLE_TIMEOUT);
        if (sessions.size() >= 256) {
            throw new ApiException(429, "TOO_MANY_GAMES", "Za dużo otwartych partii. Spróbuj później.");
        }
        Session session = new Session(UUID.randomUUID().toString());
        sessions.put(session.id, session);
        return session;
    }

    synchronized Session get(String id) {
        Session session = sessions.get(id);
        long now = System.nanoTime();
        if (session == null || now - session.touched > IDLE_TIMEOUT) {
            sessions.remove(id);
            throw new ApiException(404, "GAME_NOT_FOUND", "Ta partia wygasła lub serwer został uruchomiony ponownie.");
        }
        session.touched = now;
        return session;
    }

    static final class Session {
        final String id;
        final GameApi game = new LocalGame();
        long revision;
        volatile long touched = System.nanoTime();

        Session(String id) {
            this.id = id;
        }

        com.google.gson.JsonObject snapshot() {
            return GameJson.state(id, revision, game.getState());
        }
    }
}
