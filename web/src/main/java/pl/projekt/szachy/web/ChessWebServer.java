package pl.projekt.szachy.web;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import pl.projekt.szachy.core.model.*;
import pl.projekt.szachy.computer.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** HTTP adapter, private games, two-player rooms and static React files, without JavaFX. */
public final class ChessWebServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final GameSessions sessions = new GameSessions();
    private final Rooms rooms = new Rooms();
    private final Set<String> allowedOrigins;
    private final String bindHost;
    private final AtomicBoolean closed = new AtomicBoolean();

    private ChessWebServer(WebConfig config) throws IOException {
        bindHost = config.host();
        if (ChessWebServer.class.getResource("/public/index.html") == null) {
            throw new IOException("Brakuje frontendu. W web/frontend uruchom npm ci i npm run build.");
        }
        server = HttpServer.create(new InetSocketAddress(config.host(), config.port()), 0);
        allowedOrigins = new HashSet<>(config.allowedOrigins());
        if (config.host().equals("127.0.0.1") || config.host().equals("localhost") || config.host().equals("::1")) {
            allowedOrigins.add(WebConfig.origin("http://127.0.0.1:" + server.getAddress().getPort()));
            allowedOrigins.add(WebConfig.origin("http://localhost:" + server.getAddress().getPort()));
            allowedOrigins.add(WebConfig.origin("http://[::1]:" + server.getAddress().getPort()));
        }
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }

    /** Port 0 asks the OS for an available port, avoiding collisions in the launcher. */
    public static ChessWebServer start(int port) throws IOException {
        return new ChessWebServer(new WebConfig("127.0.0.1", port, Set.of()));
    }

    public static ChessWebServer start(WebConfig config) throws IOException { return new ChessWebServer(config); }

    public URI address() {
        String host = bindHost.equals("0.0.0.0") ? "127.0.0.1" : bindHost.equals("::") ? "::1" : bindHost;
        if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]";
        return URI.create("http://" + host + ":" + server.getAddress().getPort() + "/");
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
                exchange.getResponseHeaders().set("Referrer-Policy", "same-origin");
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/api") || path.startsWith("/api/")) {
                    exchange.getResponseHeaders().set("Cache-Control", "no-store");
                    api(exchange, path);
                } else {
                    staticFile(exchange, path);
                }
            } catch (ApiException exception) {
                error(exchange, exception.status, exception.code, exception.getMessage());
            } catch (JsonParseException | IllegalArgumentException | IllegalStateException exception) {
                error(exchange, 400, "BAD_REQUEST", "Niepoprawne dane żądania.");
            } catch (Exception exception) {
                System.getLogger(getClass().getName()).log(System.Logger.Level.ERROR, "HTTP request failed", exception);
                error(exchange, 500, "SERVER_ERROR", "Nie udało się obsłużyć żądania. Spróbuj ponownie.");
            }
        }
    }

    private void api(HttpExchange exchange, String path) throws IOException {
        String method = exchange.getRequestMethod();
        if (path.equals("/api/health")) {
            requireMethod(method, "GET");
            JsonObject response = new JsonObject();
            response.addProperty("status", "ok");
            json(exchange, 200, response);
            return;
        }
        if (method.equals("POST")) {
            validateOrigin(exchange);
        }
        if (path.equals("/api/rooms") || path.startsWith("/api/rooms/")) {
            roomsApi(exchange, path, method);
            return;
        }
        if (path.equals("/api/games")) {
            requireMethod(method, "POST");
            GameOptions options = options(body(exchange), GameOptions.local());
            GameSessions.Session session = sessions.create(options);
            synchronized (session.game) {
                json(exchange, 201, session.snapshot());
            }
            return;
        }
        String[] parts = path.split("/");
        if (parts.length < 4 || !parts[1].equals("api") || !parts[2].equals("games")
                || !parts[3].matches("[a-f0-9-]{36}") || parts.length > 5) {
            throw new ApiException(404, "NOT_FOUND", "Nie znaleziono endpointu.");
        }
        GameSessions.Session session = sessions.get(parts[3]);
        synchronized (session.game) {
            if (parts.length == 4) {
                requireMethod(method, "GET");
                json(exchange, 200, session.snapshot());
            } else if (parts[4].equals("moves") && method.equals("GET")) {
                String from = query(exchange.getRequestURI(), "from");
                JsonObject response = new JsonObject();
                response.addProperty("revision", session.game.revision());
                JsonArray moves = new JsonArray();
                session.game.getLegalMoves(square(from)).forEach(move -> moves.add(GameJson.move(move)));
                response.add("moves", moves);
                json(exchange, 200, response);
            } else if (parts[4].equals("moves") || parts[4].equals("reset") || parts[4].equals("retry")
                    || parts[4].equals("claim-draw")) {
                requireMethod(method, "POST");
                JsonObject request = body(exchange);
                String revision = required(request, "revision", false);
                if (!revision.matches("0|[1-9][0-9]{0,15}") || Long.parseLong(revision) != session.game.revision()) {
                    JsonObject response = errorJson("STALE_STATE", "Plansza zmieniła się. Pobrano aktualną pozycję.");
                    response.add("state", session.snapshot());
                    json(exchange, 409, response);
                    return;
                }
                if (parts[4].equals("reset")) {
                    session.game.configure(options(request, session.game.snapshot().options()));
                    json(exchange, 200, session.snapshot());
                    return;
                }
                if (parts[4].equals("retry")) {
                    session.game.retryComputer();
                    json(exchange, 200, session.snapshot());
                    return;
                }
                if (parts[4].equals("claim-draw")) {
                    boolean claimed = session.game.claimDraw();
                    JsonObject response = claimed ? session.snapshot()
                            : errorJson("DRAW_UNAVAILABLE", "W tej pozycji nie można zgłosić remisu.");
                    if (!claimed) response.add("state", session.snapshot());
                    json(exchange, claimed ? 200 : 422, response);
                    return;
                }
                PieceType promotion = null;
                if (request.has("promotion") && !request.get("promotion").isJsonNull()) {
                    promotion = PieceType.valueOf(required(request, "promotion", true));
                }
                Move move = new Move(square(required(request, "from", true)),
                        square(required(request, "to", true)), promotion);
                MoveResult result = session.game.makeMove(move);
                if (!result.applied()) {
                    JsonObject response = errorJson(result.error().name(), moveError(result.error()));
                    response.add("state", session.snapshot());
                    json(exchange, 422, response);
                    return;
                }
                json(exchange, 200, session.snapshot());
            } else {
                throw new ApiException(404, "NOT_FOUND", "Nie znaleziono endpointu.");
            }
        }
    }

    private void roomsApi(HttpExchange exchange, String path, String method) throws IOException {
        if (path.equals("/api/rooms")) {
            if (method.equals("GET")) {
                JsonObject result = new JsonObject(); result.add("rooms", rooms.list());
                json(exchange, 200, result);
            } else {
                requireMethod(method, "POST");
                JsonObject request = body(exchange);
                json(exchange, 201, rooms.create(required(request, "name", true), required(request, "playerName", true),
                        request.has("side") ? Side.valueOf(required(request, "side", true)) : Side.WHITE,
                        required(request, "clientId", true)));
            }
            return;
        }
        String[] parts = path.split("/");
        if (parts.length < 4 || parts.length > 5 || !parts[3].matches("[a-f0-9-]{36}")) {
            throw new ApiException(404, "NOT_FOUND", "Nie znaleziono pokoju.");
        }
        Rooms.Room room = rooms.get(parts[3]);
        synchronized (room) {
            if (parts.length == 5 && (parts[4].equals("join") || parts[4].equals("reconnect"))) {
                requireMethod(method, "POST");
                JsonObject request = body(exchange);
                JsonObject result = parts[4].equals("join")
                        ? room.join(required(request, "playerName", true), required(request, "clientId", true))
                        : room.reconnect(required(request, "playerToken", true), required(request, "clientId", true));
                json(exchange, 200, result);
                return;
            }
            String header = exchange.getRequestHeaders().getFirst("Authorization");
            Rooms.Player player = room.authenticate(header != null && header.startsWith("Bearer ") ? header.substring(7) : null);
            try {
                if (parts.length == 4) {
                    requireMethod(method, "GET"); json(exchange, 200, room.snapshot(player));
                    return;
                }
                if (parts[4].equals("moves") && method.equals("GET")) {
                    json(exchange, 200, room.moves(player, square(query(exchange.getRequestURI(), "from"))));
                    return;
                }
                requireMethod(method, "POST");
                JsonObject request = body(exchange);
                if (parts[4].equals("leave")) {
                    room.close();
                    JsonObject result = new JsonObject(); result.addProperty("closed", true);
                    json(exchange, 200, result); return;
                }
                String revision = required(request, "revision", false);
                if (!revision.matches("0|[1-9][0-9]{0,15}")) throw new ApiException(400, "BAD_REVISION", "Niepoprawna rewizja.");
                long expected = Long.parseLong(revision);
                JsonObject result = switch (parts[4]) {
                    case "moves" -> room.move(player, expected, new Move(square(required(request, "from", true)),
                            square(required(request, "to", true)), request.has("promotion") && !request.get("promotion").isJsonNull()
                            ? PieceType.valueOf(required(request, "promotion", true)) : null));
                    case "claim-draw" -> room.claimDraw(player, expected);
                    case "rematch" -> room.rematch(player, expected);
                    default -> throw new ApiException(404, "NOT_FOUND", "Nie znaleziono endpointu.");
                };
                json(exchange, 200, result);
            } catch (ApiException exception) {
                JsonObject result = errorJson(exception.code, exception.getMessage());
                if (!room.closed) result.add("state", room.snapshot(player));
                json(exchange, exception.status, result);
            }
        }
    }

    private void staticFile(HttpExchange exchange, String path) throws IOException {
        String method = exchange.getRequestMethod();
        if (!method.equals("GET") && !method.equals("HEAD")) {
            throw new ApiException(405, "METHOD_NOT_ALLOWED", "Dozwolone metody: GET, HEAD.");
        }
        if (path.equals("/")) {
            path = "/index.html";
        }
        if (path.contains("..") || path.contains("\\") || !path.matches("/[A-Za-z0-9_./-]+")) {
            throw new ApiException(404, "NOT_FOUND", "Nie znaleziono pliku.");
        }
        try (var input = ChessWebServer.class.getResourceAsStream("/public" + path)) {
            if (input == null) {
                throw new ApiException(404, "NOT_FOUND", "Nie znaleziono pliku.");
            }
            String extension = path.substring(path.lastIndexOf('.') + 1);
            String type = Map.of("html", "text/html; charset=utf-8", "js", "text/javascript; charset=utf-8",
                    "css", "text/css; charset=utf-8", "png", "image/png", "svg", "image/svg+xml",
                    "ico", "image/x-icon").getOrDefault(extension, "application/octet-stream");
            exchange.getResponseHeaders().set("Content-Type", type);
            exchange.getResponseHeaders().set("Cache-Control", path.startsWith("/assets/")
                    ? "public, max-age=31536000, immutable" : "no-cache");
            exchange.getResponseHeaders().set("Content-Security-Policy",
                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                            + "img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'");
            byte[] bytes = input.readAllBytes();
            exchange.sendResponseHeaders(200, method.equals("HEAD") ? -1 : bytes.length);
            if (!method.equals("HEAD")) {
                exchange.getResponseBody().write(bytes);
            }
        }
    }

    private static GameOptions options(JsonObject request, GameOptions fallback) {
        return new GameOptions(request.has("opponent") ? Opponent.valueOf(required(request, "opponent", true)) : fallback.opponent(),
                request.has("difficulty") ? Difficulty.valueOf(required(request, "difficulty", true)) : fallback.difficulty(),
                request.has("humanSide") ? Side.valueOf(required(request, "humanSide", true)) : fallback.humanSide());
    }

    private static JsonObject body(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.split(";", 2)[0].strip().equalsIgnoreCase("application/json")) {
            throw new ApiException(415, "JSON_REQUIRED", "Wymagany Content-Type: application/json.");
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(8193);
        if (bytes.length > 8192) {
            throw new ApiException(413, "BODY_TOO_LARGE", "Za duże żądanie.");
        }
        JsonElement value = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        if (value == null || !value.isJsonObject()) {
            throw new ApiException(400, "BAD_REQUEST", "Wymagany obiekt JSON.");
        }
        return value.getAsJsonObject();
    }

    private static String required(JsonObject body, String key, boolean string) {
        JsonElement element = body.get(key);
        if (element == null || !element.isJsonPrimitive()
                || (string ? !element.getAsJsonPrimitive().isString() : !element.getAsJsonPrimitive().isNumber())) {
            throw new ApiException(400, "BAD_REQUEST", "Brak lub niepoprawne pole: " + key);
        }
        return element.getAsString();
    }

    private static Square square(String value) {
        if (value == null || !value.matches("[a-h][1-8]")) {
            throw new ApiException(400, "BAD_SQUARE", "Pole musi należeć do zakresu a1–h8.");
        }
        return new Square(value.charAt(0) - 'a', value.charAt(1) - '1');
    }

    private static String query(URI uri, String key) {
        if (uri.getRawQuery() != null) {
            for (String pair : uri.getRawQuery().split("&")) {
                String[] values = pair.split("=", 2);
                if (values.length == 2 && URLDecoder.decode(values[0], StandardCharsets.UTF_8).equals(key)) {
                    return URLDecoder.decode(values[1], StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    private void validateOrigin(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null) {
            if (!allowedOrigins.contains(WebConfig.origin(origin))) {
                throw new ApiException(403, "ORIGIN_DENIED", "Niedozwolone źródło żądania.");
            }
        }
    }

    private static void requireMethod(String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new ApiException(405, "METHOD_NOT_ALLOWED", "Wymagana metoda: " + expected);
        }
    }

    private static String moveError(MoveError error) {
        return switch (error) {
            case WRONG_SIDE -> "To nie kolej tej strony.";
            case ILLEGAL_MOVE -> "Ten ruch jest niedozwolony.";
            case PROMOTION_REQUIRED -> "Wybierz figurę do promocji pionka.";
            case GAME_OVER -> "Partia jest zakończona. Rozpocznij nową grę.";
            case NONE -> "";
        };
    }

    private static JsonObject errorJson(String code, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("error", code);
        response.addProperty("message", message);
        return response;
    }

    private static void error(HttpExchange exchange, int status, String code, String message) throws IOException {
        json(exchange, status, errorJson(code, message));
    }

    private static void json(HttpExchange exchange, int status, JsonObject value) throws IOException {
        byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            server.stop(0);
            sessions.close();
            rooms.close();
            executor.shutdownNow();
        }
    }
}
