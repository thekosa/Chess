package pl.projekt.szachy.desktop;

import pl.projekt.szachy.console.ConsoleMain;
import pl.projekt.szachy.computer.GameOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Starts a real interactive terminal, including when the chooser runs from an IDE. */
final class ConsoleLauncher {
    private ConsoleLauncher() { }

    static void launch(GameOptions options) throws Exception {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String java = Path.of(System.getProperty("java.home"), "bin",
                os.contains("win") ? "java.exe" : "java").toString();
        String classpath = ConsoleMain.runtimeClasspath();
        if (os.contains("win")) {
            launchWindows(java, classpath, options);
        } else if (os.contains("mac")) {
            String command = shellQuote(java) + " -cp " + shellQuote(classpath)
                    + " pl.projekt.szachy.console.ConsoleMain --ansi " + String.join(" ", options.arguments());
            String script = "tell application \"Terminal\"\nactivate\ndo script \""
                    + command.replace("\\", "\\\\").replace("\"", "\\\"") + "\"\nend tell";
            launchAndWait(List.of("osascript", "-e", script));
        } else {
            launchLinux(java, classpath, options);
        }
    }

    private static void launchWindows(String java, String classpath, GameOptions options) throws Exception {
        String bootstrap;
        try (var input = Objects.requireNonNull(ConsoleLauncher.class.getResourceAsStream("console-window.ps1"))) {
            bootstrap = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("@JAVA@", powershellQuote(java))
                    .replace("@CLASSPATH@", powershellQuote(classpath))
                    .replace("@OPTIONS@", options.arguments().stream().map(ConsoleLauncher::powershellQuote)
                            .collect(Collectors.joining(" ")));
        }
        String encoded = encode(bootstrap);
        String launch = "Start-Process -FilePath 'powershell.exe' -WindowStyle Normal "
                + "-ArgumentList @('-NoLogo', '-NoProfile', '-EncodedCommand', '" + encoded + "') -ErrorAction Stop";
        // A separate console is intentional: IDE output panes do not provide a terminal screen.
        launchAndWait(List.of("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
                "-EncodedCommand", encode(launch)));
    }

    private static void launchLinux(String java, String classpath, GameOptions options) throws IOException {
        List<String> game = new ArrayList<>(List.of(java, "-cp", classpath, "pl.projekt.szachy.console.ConsoleMain",
                "--ansi"));
        game.addAll(options.arguments());
        for (List<String> terminal : List.of(
                List.of("xterm", "-geometry", "120x66", "-T", "Szachy ASCII", "-e"),
                List.of("gnome-terminal", "--geometry=120x66", "--"))) {
            List<String> command = new ArrayList<>(terminal);
            command.addAll(game);
            try {
                new ProcessBuilder(command).start();
                return;
            } catch (IOException ignored) {
                // Try the next installed terminal.
            }
        }
        throw new IOException("Nie znaleziono xterm ani gnome-terminal. Uruchom ConsoleMain we własnym terminalu.");
    }

    private static void launchAndWait(List<String> command) throws IOException, InterruptedException {
        Path log = Files.createTempFile("chess-terminal-", ".log");
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(log.toFile()).start();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroy();
                throw new IOException("Uruchamianie terminala trwa zbyt długo. Spróbuj ponownie.");
            }
            if (process.exitValue() != 0) {
                throw new IOException("Nie udało się otworzyć terminala. " + Files.readString(log));
            }
        } finally {
            Files.deleteIfExists(log);
        }
    }

    private static String encode(String script) {
        return Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
    }

    private static String powershellQuote(String text) {
        return "'" + text.replace("'", "''") + "'";
    }

    private static String shellQuote(String text) {
        return "'" + text.replace("'", "'\"'\"'") + "'";
    }
}
