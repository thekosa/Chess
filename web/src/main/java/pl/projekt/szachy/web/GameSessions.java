package pl.projekt.szachy.web;

import pl.projekt.szachy.computer.ComputerGame;
import pl.projekt.szachy.computer.GameOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Each browser game has its own engine. Commands and computer commits share its monitor. */
final class GameSessions implements AutoCloseable {
    private static final long IDLE_TIMEOUT = Duration.ofHours(2).toNanos();
    private final Map<String, Session> sessions = new HashMap<>();
    private boolean closed;

    synchronized Session create(GameOptions options) {
        if (closed) throw new ApiException(503, "SERVER_STOPPING", "Serwer kończy pracę.");
        long now = System.nanoTime();
        sessions.values().removeIf(session -> {
            if (now - session.touched <= IDLE_TIMEOUT) return false;
            session.game.close();
            return true;
        });
        if (sessions.size() >= 256) {
            throw new ApiException(429, "TOO_MANY_GAMES", "Za dużo otwartych partii. Spróbuj później.");
        }
        Session session = new Session(UUID.randomUUID().toString(), options);
        sessions.put(session.id, session);
        return session;
    }

    synchronized Session get(String id) {
        if (closed) throw new ApiException(503, "SERVER_STOPPING", "Serwer kończy pracę.");
        Session session = sessions.get(id);
        long now = System.nanoTime();
        if (session == null || now - session.touched > IDLE_TIMEOUT) {
            sessions.remove(id);
            if (session != null) session.game.close();
            throw new ApiException(404, "GAME_NOT_FOUND", "Ta partia wygasła lub serwer został uruchomiony ponownie.");
        }
        session.touched = now;
        return session;
    }

    @Override public synchronized void close() {
        closed = true;
        sessions.values().forEach(session -> session.game.close());
        sessions.clear();
    }

    static final class Session {
        final String id;
        final ComputerGame game;
        volatile long touched = System.nanoTime();

        Session(String id, GameOptions options) {
            this.id = id;
            this.game = new ComputerGame(options);
        }

        com.google.gson.JsonObject snapshot() {
            var snapshot = game.snapshot();
            var json = GameJson.state(id, snapshot.revision(), snapshot.state());
            json.addProperty("opponent", snapshot.options().opponent().name());
            json.addProperty("difficulty", snapshot.options().difficulty().name());
            json.addProperty("humanSide", snapshot.options().humanSide().name());
            json.addProperty("computerThinking", snapshot.thinking());
            json.addProperty("computerError", snapshot.error());
            return json;
        }
    }
}
