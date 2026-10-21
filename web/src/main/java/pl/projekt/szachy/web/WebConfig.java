package pl.projekt.szachy.web;

import java.net.URI;
import java.util.Set;

/** Explicit origins work both directly and behind an HTTPS reverse proxy. */
public record WebConfig(String host, int port, Set<String> allowedOrigins) {
    public WebConfig {
        if (host == null || host.isBlank() || port < 0 || port > 65535) throw new IllegalArgumentException("Niepoprawny host lub port.");
        allowedOrigins = allowedOrigins.stream().map(WebConfig::origin).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    static String origin(String value) {
        URI uri = URI.create(value);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty() && !uri.getPath().equals("/"))) {
            throw new IllegalArgumentException("Origin musi mieć postać https://szachy.example.pl");
        }
        int port = uri.getPort();
        if ((port == 80 && uri.getScheme().equals("http")) || (port == 443 && uri.getScheme().equals("https"))) port = -1;
        return uri.getScheme() + "://" + uri.getHost().toLowerCase(java.util.Locale.ROOT) + (port < 0 ? "" : ":" + port);
    }
}
