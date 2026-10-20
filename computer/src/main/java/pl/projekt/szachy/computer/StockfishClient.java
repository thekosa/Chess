package pl.projekt.szachy.computer;

import pl.projekt.szachy.core.model.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/** A bounded UCI conversation with one native process; no UI or chess rules here. */
final class StockfishClient implements AutoCloseable {
    private static final Set<Process> ACTIVE = ConcurrentHashMap.newKeySet();
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ACTIVE.forEach(Process::destroyForcibly),
                "stop-stockfish"));
    }
    private final Process process;
    private final BufferedWriter input;
    private final BlockingQueue<String> output = new ArrayBlockingQueue<>(1024);
    private volatile boolean ended;

    StockfishClient() throws Exception {
        var executable = StockfishInstallation.executable();
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        process = new ProcessBuilder(executable.toString()).directory(executable.getParent().toFile())
                .redirectErrorStream(true).start();
        ACTIVE.add(process);
        input = process.outputWriter(StandardCharsets.UTF_8);
        Thread reader = new Thread(() -> {
            try (BufferedReader lines = process.inputReader(StandardCharsets.UTF_8)) {
                String line;
                while ((line = lines.readLine()) != null) {
                    if (!output.offer(line)) { output.poll(); output.offer(line); }
                }
            } catch (IOException ignored) {
                // EOF is reported to the waiting caller below.
            } finally { ended = true; }
        }, "stockfish-output");
        reader.setDaemon(true);
        reader.start();
    }
    Move bestMove(GameState state, Difficulty difficulty) throws Exception {
        send("uci");
        await("uciok", 20_000);
        send("setoption name Threads value 1");
        send("setoption name Hash value 32");
        send("setoption name Skill Level value " + difficulty.skill);
        send("setoption name UCI_LimitStrength value false");
        send("ucinewgame");
        send("isready");
        await("readyok", 20_000);
        String history = state.moveHistory().stream().map(StockfishClient::uci).collect(Collectors.joining(" "));
        send("position startpos" + (history.isEmpty() ? "" : " moves " + history));
        send("go movetime " + difficulty.thinkingMillis);
        String value = await("bestmove ", 20_000).split("\\s+")[1];
        if (!value.matches("[a-h][1-8][a-h][1-8][qrbn]?")) {
            throw new IOException("Silnik nie zwrócił ruchu (" + value + ").");
        }
        PieceType promotion = value.length() == 4 ? null : switch (value.charAt(4)) {
            case 'q' -> PieceType.QUEEN;
            case 'r' -> PieceType.ROOK;
            case 'b' -> PieceType.BISHOP;
            default -> PieceType.KNIGHT;
        };
        return new Move(new Square(value.charAt(0) - 'a', value.charAt(1) - '1'),
                new Square(value.charAt(2) - 'a', value.charAt(3) - '1'), promotion);
    }
    private static String uci(Move move) {
        return move.from().toString() + move.to() + (move.promotion() == null ? "" : switch (move.promotion()) {
            case QUEEN -> "q"; case ROOK -> "r"; case BISHOP -> "b"; case KNIGHT -> "n";
            default -> throw new IllegalArgumentException("Niepoprawna promocja.");
        });
    }
    private void send(String command) throws IOException { input.write(command); input.newLine(); input.flush(); }
    private String await(String prefix, long timeoutMillis) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline) {
            String line = output.poll(100, TimeUnit.MILLISECONDS);
            if (line != null && line.startsWith(prefix)) return line;
            if (line == null && ended) throw new IOException("Proces Stockfisha zakończył pracę.");
        }
        throw new IOException("Stockfish nie odpowiedział w wyznaczonym czasie.");
    }
    @Override public void close() {
        try { send("quit"); } catch (IOException ignored) { }
        try {
            if (!process.waitFor(200, TimeUnit.MILLISECONDS)) process.destroyForcibly();
        } catch (InterruptedException exception) { process.destroyForcibly(); Thread.currentThread().interrupt(); }
        ACTIVE.remove(process);
        try { input.close(); } catch (IOException ignored) { }
    }
}
