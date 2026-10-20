package pl.projekt.szachy.computer;

import pl.projekt.szachy.core.LocalGame;
import pl.projekt.szachy.core.api.GameApi;
import pl.projekt.szachy.core.model.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/** Owns the game and serializes human commands and background computer moves. */
public final class ComputerGame implements GameApi, AutoCloseable {
    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(2, 2,
            0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(256), runnable -> {
                Thread thread = new Thread(runnable, "chess-stockfish");
                thread.setDaemon(true);
                return thread;
            });
    private final LocalGame game = new LocalGame();
    private GameOptions options;
    private long revision;
    private long generation;
    private boolean thinking;
    private boolean closed;
    private String error;
    private Future<?> pending;

    public ComputerGame(GameOptions options) {
        this.options = Objects.requireNonNull(options);
        synchronized (this) { schedule(); }
    }
    public record Snapshot(GameState state, long revision, GameOptions options,
                           boolean thinking, String error) { }
    public synchronized Snapshot snapshot() {
        return new Snapshot(game.getState(), revision, options, thinking, error);
    }
    public synchronized long revision() { return revision; }
    public synchronized boolean isComputerTurn() {
        return options.opponent() == Opponent.COMPUTER && !game.getState().status().isFinished()
                && game.getState().sideToMove() != options.humanSide();
    }
    @Override public synchronized GameState getState() { return game.getState(); }
    @Override public synchronized List<Move> getLegalMoves(Square from) {
        return closed || isComputerTurn() ? List.of() : game.getLegalMoves(from);
    }
    @Override public synchronized MoveResult makeMove(Move move) {
        if (closed || isComputerTurn()) return new MoveResult(MoveError.WRONG_SIDE, game.getState());
        MoveResult result = game.makeMove(move);
        if (result.applied()) { revision++; schedule(); }
        return result;
    }
    @Override public synchronized GameState newGame() { return configure(options); }
    public synchronized GameState configure(GameOptions options) {
        if (closed) throw new IllegalStateException("Gra została zamknięta.");
        cancel();
        this.options = Objects.requireNonNull(options);
        GameState state = game.newGame();
        revision++;
        schedule();
        return state;
    }
    public synchronized void retryComputer() {
        if (!closed && !thinking && isComputerTurn()) schedule();
    }
    private void schedule() {
        if (closed || !isComputerTurn()) return;
        GameState position = game.getState();
        Difficulty difficulty = options.difficulty();
        long ticket = generation;
        thinking = true;
        error = null;
        try {
            pending = WORKERS.submit(() -> calculate(position, difficulty, ticket));
        } catch (RejectedExecutionException exception) {
            thinking = false;
            error = "Komputer jest zajęty. Spróbuj ponownie za chwilę.";
        }
    }
    private void calculate(GameState position, Difficulty difficulty, long ticket) {
        try {
            Move move;
            try (StockfishClient engine = new StockfishClient()) {
                move = engine.bestMove(position, difficulty);
            }
            synchronized (this) {
                if (closed || generation != ticket || game.getState() != position) return;
                MoveResult result = game.makeMove(move);
                if (!result.applied()) throw new IllegalStateException("Stockfish zwrócił niedozwolony ruch.");
                revision++;
                thinking = false;
            }
        } catch (Exception exception) {
            synchronized (this) {
                if (closed || generation != ticket) return;
                thinking = false;
                error = "Nie udało się wykonać ruchu komputera: " + exception.getMessage();
            }
        }
    }
    private void cancel() {
        generation++;
        if (pending != null) {
            pending.cancel(true);
            if (pending instanceof Runnable runnable) WORKERS.remove(runnable);
        }
        pending = null;
        thinking = false;
        error = null;
    }
    @Override public synchronized void close() { closed = true; cancel(); }
}
