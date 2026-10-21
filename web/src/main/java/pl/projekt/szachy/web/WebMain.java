package pl.projekt.szachy.web;

import java.util.concurrent.CountDownLatch;
import java.util.HashSet;
import java.util.Set;

/** Standalone configurable web server; the desktop chooser can start a loopback instance. */
public final class WebMain {
    private WebMain() { }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        String host = System.getenv().getOrDefault("CHESS_HOST", "127.0.0.1");
        Set<String> origins = new HashSet<>();
        String configured = System.getenv().getOrDefault("CHESS_ALLOWED_ORIGINS", "");
        for (String origin : configured.split(",")) if (!origin.isBlank()) origins.add(origin.strip());
        for (String argument : args) {
            if (argument.equals("--help")) {
                System.out.println("Szachy Web: [--port=8080] [--host=0.0.0.0] [--public-origin=https://szachy.example.pl]");
                System.out.println("Zmienne: PORT, CHESS_HOST, CHESS_ALLOWED_ORIGINS (oddzielone przecinkami).");
                return;
            }
            if (argument.startsWith("--host=")) { host = argument.substring(7); continue; }
            if (argument.startsWith("--public-origin=")) { origins.add(argument.substring(16)); continue; }
            if (!argument.matches("--port=[0-9]{1,5}")) {
                throw new IllegalArgumentException("Nieznany argument: " + argument);
            }
            port = Integer.parseInt(argument.substring(7));
        }
        if (port > 65535) {
            throw new IllegalArgumentException("Port musi należeć do zakresu 0–65535.");
        }
        ChessWebServer server = ChessWebServer.start(new WebConfig(host, port, origins));
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "stop-web-server"));
        System.out.println("Szachy Web: " + server.address());
        System.out.println("Otwórz ten adres w przeglądarce. Zatrzymanie serwera: Ctrl+C.");
        new CountDownLatch(1).await();
    }
}
