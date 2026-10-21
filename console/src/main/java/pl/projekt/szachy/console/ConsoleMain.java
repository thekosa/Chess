package pl.projekt.szachy.console;

import pl.projekt.szachy.core.LocalGame;
import pl.projekt.szachy.computer.ComputerGame;
import pl.projekt.szachy.computer.GameOptions;
import pl.projekt.szachy.core.api.GameApi;
import pl.projekt.szachy.core.model.Side;
import pl.projekt.szachy.network.OnlineClient;
import pl.projekt.szachy.network.RemoteGame;
import org.jline.nativ.JLineNativeLoader;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.jni.JniTerminalProvider;

import java.io.BufferedReader;
import java.io.IOException;
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
            System.out.println("Online: --server=https://szachy.example.pl [--name=Gracz] [--room=UUID] --ansi");
            return;
        }
        boolean ansi = !Arrays.asList(args).contains("--plain")
                && (System.console() != null || Arrays.asList(args).contains("--ansi"));
        int columns = dimension(args, "columns", 120);
        int rows = dimension(args, "rows", 66);
        GameApi game = option(args, "server") == null ? new ComputerGame(GameOptions.fromArguments(args)) : online(args);
        if (game == null) return;
        TerminalScreen screen;
        try {
            if (ansi) {
                Terminal terminal = TerminalBuilder.builder().name("Szachy ASCII").system(true)
                        .provider("jni").dumb(false).encoding(StandardCharsets.UTF_8).build();
                screen = new TerminalScreen(terminal);
            } else {
                PrintWriter out = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                screen = new TerminalScreen(out, input, columns, rows);
            }
        } catch (Exception exception) {
            ((AutoCloseable) game).close();
            throw exception;
        }
        try (screen; AutoCloseable session = (AutoCloseable) game) {
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
        paths.addAll(OnlineClient.runtimeClasspath());
        return paths.stream().distinct().collect(Collectors.joining(File.pathSeparator));
    }

    private static RemoteGame online(String[] args) throws IOException {
        OnlineClient client = new OnlineClient(option(args, "server"));
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String name = option(args, "name");
        if (name == null || name.isBlank()) {
            System.out.print("Twoj nick [Gracz] > "); System.out.flush();
            name = input.readLine(); if (name == null) return null;
            if (name.isBlank()) name = "Gracz";
        }
        String directRoom = option(args, "room");
        while (true) {
            try {
                if (directRoom != null) {
                    String room = directRoom; directRoom = null;
                    return client.join(room, name);
                }
                List<OnlineClient.RoomInfo> rooms = client.rooms();
                System.out.println("\nSZACHY / POKOJE ONLINE (maks. 2 graczy)");
                for (int index = 0; index < rooms.size(); index++) {
                    var room = rooms.get(index);
                    System.out.println((index + 1) + ". " + room.name() + " [" + room.playerCount() + "/2] " + room.status());
                }
                if (rooms.isEmpty()) System.out.println("Nie ma jeszcze pokoi. Utworz pierwszy.");
                System.out.print("Numer pokoju / new (utworz) / refresh / quit > "); System.out.flush();
                String choice = input.readLine();
                if (choice == null || choice.equalsIgnoreCase("quit")) return null;
                if (choice.equalsIgnoreCase("refresh")) continue;
                if (choice.equalsIgnoreCase("new")) {
                    System.out.print("Nazwa pokoju > "); System.out.flush();
                    String roomName = input.readLine(); if (roomName == null) return null;
                    System.out.print("Twoj kolor: white / black [white] > "); System.out.flush();
                    String side = input.readLine(); if (side == null) return null;
                    RemoteGame game = client.create(roomName, name, side.equalsIgnoreCase("black") ? Side.BLACK : Side.WHITE);
                    System.out.println("Pokoj utworzony: " + game.roomId());
                    return game;
                }
                int index = Integer.parseInt(choice.strip()) - 1;
                if (index < 0 || index >= rooms.size()) throw new IllegalArgumentException("Nie ma takiego pokoju.");
                return client.join(rooms.get(index).id(), name);
            } catch (IllegalArgumentException exception) {
                System.out.println("Nie udalo sie dolaczyc: " + exception.getMessage());
                System.out.print("Enter: sprobuj ponownie, quit: zakoncz > "); System.out.flush();
                String line = input.readLine(); if (line == null || line.equalsIgnoreCase("quit")) return null;
            }
        }
    }
    private static String option(String[] args, String key) {
        String prefix = "--" + key + "=";
        for (String arg : args) if (arg.startsWith(prefix)) return arg.substring(prefix.length());
        return null;
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
