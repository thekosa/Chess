package pl.projekt.szachy.console;

import pl.projekt.szachy.core.LocalGame;
import pl.projekt.szachy.computer.ComputerGame;
import pl.projekt.szachy.computer.GameOptions;
import org.jline.nativ.JLineNativeLoader;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.jni.JniTerminalProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Standalone console entry point. No JavaFX or HTTP server is needed. */
public final class ConsoleMain {
    private ConsoleMain() { }

    public static void main(String[] args) throws Exception {
        if (Arrays.asList(args).contains("--help")) {
            System.out.println("Szachy ASCII: e2 e4, moves e2, e7 e8 q, new, history, size full/compact, quit.");
            System.out.println("Opcje: --ansi, --plain. Rozmiar okna jest wykrywany na biezaco.");
            System.out.println("Tryb --plain: --columns=N, --rows=N. Plansza pelna: min. 117x66.");
            System.out.println("Komputer: --opponent=COMPUTER --difficulty=EASY|MEDIUM|HARD --side=WHITE|BLACK.");
            System.out.println("W grze: computer easy/medium/hard [white/black], local, retry.");
            return;
        }
        boolean ansi = !Arrays.asList(args).contains("--plain")
                && (System.console() != null || Arrays.asList(args).contains("--ansi"));
        int columns = dimension(args, "columns", 120);
        int rows = dimension(args, "rows", 66);
        TerminalScreen screen;
        if (ansi) {
            Terminal terminal = TerminalBuilder.builder().name("Szachy ASCII").system(true)
                    .provider("jni").dumb(false).encoding(StandardCharsets.UTF_8).build();
            screen = new TerminalScreen(terminal);
        } else {
            PrintWriter out = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            screen = new TerminalScreen(out, input, columns, rows);
        }
        try (screen; ComputerGame game = new ComputerGame(GameOptions.fromArguments(args))) {
            Thread cleanup = new Thread(screen::close, "restore-terminal");
            Runtime.getRuntime().addShutdownHook(cleanup);
            try {
                screen.open();
                new ConsoleGame(game, screen, AsciiBoard.Size.FULL, ansi).run();
            } finally {
                try {
                    Runtime.getRuntime().removeShutdownHook(cleanup);
                } catch (IllegalStateException ignored) {
                    // The JVM is already shutting down; the hook will restore the terminal.
                }
            }
        }
    }

    /** Runtime locations also cover IntelliJ's separate module output directories. */
    public static String runtimeClasspath() throws Exception {
        List<Class<?>> entries = List.of(ConsoleMain.class, LocalGame.class, ComputerGame.class, Terminal.class,
                JniTerminalProvider.class, JLineNativeLoader.class);
        java.util.ArrayList<String> paths = new java.util.ArrayList<>();
        for (Class<?> entry : entries) {
            paths.add(Path.of(entry.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        }
        return paths.stream().distinct().collect(Collectors.joining(File.pathSeparator));
    }

    private static int dimension(String[] args, String name, int fallback) {
        String prefix = "--" + name + "=";
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                int value = Integer.parseInt(arg.substring(prefix.length()));
                if (value <= 0) {
                    throw new IllegalArgumentException(name + " musi byc dodatnie.");
                }
                return value;
            }
        }
        return fallback;
    }
}
