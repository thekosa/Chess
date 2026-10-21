package pl.projekt.szachy.network;

import com.google.gson.*;
import pl.projekt.szachy.core.model.Side;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/** A transport client usable by any Java frontend; it never computes chess moves. */
public final class OnlineClient {
    private final URI server;
    private final String clientId = UUID.randomUUID().toString();
    public OnlineClient(String address) {
        server = URI.create(address.endsWith("/") ? address : address + "/");
        if (!("http".equals(server.getScheme()) || "https".equals(server.getScheme()))
                || server.getHost() == null || server.getUserInfo() != null || server.getQuery() != null || server.getFragment() != null) {
            throw new IllegalArgumentException("Podaj adres serwera http:// lub https://.");
        }
    }
    public record PlayerInfo(String name, Side side, boolean connected) { }
    public record RoomInfo(String id, String name, int playerCount, String status, List<PlayerInfo> players) { }
    public List<RoomInfo> rooms() {
        List<RoomInfo> result = new ArrayList<>();
        for (JsonElement element : request("rooms", null, null).getAsJsonArray("rooms")) {
            JsonObject room = element.getAsJsonObject();
            List<PlayerInfo> players = new ArrayList<>();
            for (JsonElement value : room.getAsJsonArray("players")) {
                var p = value.getAsJsonObject();
                players.add(new PlayerInfo(p.get("name").getAsString(), Side.valueOf(p.get("side").getAsString()), p.get("connected").getAsBoolean()));
            }
            result.add(new RoomInfo(room.get("id").getAsString(), room.get("name").getAsString(),
                    room.get("playerCount").getAsInt(), room.get("status").getAsString(), List.copyOf(players)));
        }
        return List.copyOf(result);
    }
    public RemoteGame create(String roomName, String playerName, Side side) {
        JsonObject body = identity(playerName); body.addProperty("name", roomName); body.addProperty("side", side.name());
        return new RemoteGame(this, request("rooms", body, null));
    }
    public RemoteGame join(String roomId, String playerName) {
        return new RemoteGame(this, request("rooms/" + roomId(roomId) + "/join", identity(playerName), null));
    }
    public RemoteGame reconnect(String roomId, String playerToken) {
        JsonObject body = new JsonObject(); body.addProperty("clientId", clientId); body.addProperty("playerToken", playerToken);
        return new RemoteGame(this, request("rooms/" + roomId(roomId) + "/reconnect", body, null));
    }
    private JsonObject identity(String name) {
        JsonObject body = new JsonObject(); body.addProperty("playerName", name); body.addProperty("clientId", clientId); return body;
    }
    private static String roomId(String value) {
        if (!value.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException("Niepoprawny identyfikator pokoju.");
        return value;
    }
    JsonObject request(String path, JsonObject body, String token) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) server.resolve("api/" + path).toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");
            if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                connection.setRequestMethod("POST"); connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream output = connection.getOutputStream()) { output.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            }
            int status = connection.getResponseCode();
            InputStream stream = status < 400 ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) throw new IOException("Serwer zwrócił HTTP " + status);
            JsonObject result;
            try (stream) {
                byte[] bytes = stream.readNBytes(1_048_577);
                if (bytes.length > 1_048_576) throw new IOException("Za duża odpowiedź serwera.");
                result = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            if (status >= 300) throw new OnlineException(status, result);
            return result;
        } catch (OnlineException exception) { throw exception; }
        catch (Exception exception) { throw new OnlineException("Brak połączenia z serwerem: " + exception.getMessage()); }
        finally { if (connection != null) connection.disconnect(); }
    }
    public static List<String> runtimeClasspath() throws Exception {
        return List.of(Path.of(OnlineClient.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
                Path.of(Gson.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
    }
    static final class OnlineException extends IllegalArgumentException {
        final int status;
        final JsonObject data;
        OnlineException(int status, JsonObject data) {
            super(data.has("message") ? data.get("message").getAsString() : "Błąd serwera: " + status);
            this.status = status; this.data = data;
        }
        OnlineException(String message) { super(message); status = 0; data = new JsonObject(); }
    }
}
