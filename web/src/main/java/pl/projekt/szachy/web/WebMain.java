package pl.projekt.szachy.web;

import java.util.concurrent.CountDownLatch;

/** Standalone local web server. The desktop chooser can start the same server in process. */
public final class WebMain {
    private WebMain() { }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        for (String argument : args) {
            if (argument.equals("--help")) {
                System.out.println("Szachy Web: java -jar chess-web-1.0-SNAPSHOT.jar [--port=8080]");
                return;
            }
            if (!argument.matches("--port=[0-9]{1,5}")) {
                throw new IllegalArgumentException("Nieznany argument: " + argument);
            }
            port = Integer.parseInt(argument.substring(7));
        }
        if (port > 65535) {
            throw new IllegalArgumentException("Port musi należeć do zakresu 0–65535.");
        }
        ChessWebServer server = ChessWebServer.start(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "stop-web-server"));
        System.out.println("Szachy Web: " + server.address());
        System.out.println("Otwórz ten adres w przeglądarce. Zatrzymanie serwera: Ctrl+C.");
        new CountDownLatch(1).await();
    }
}
