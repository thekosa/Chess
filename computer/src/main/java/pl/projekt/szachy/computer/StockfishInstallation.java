package pl.projekt.szachy.computer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

/** Downloads an official, checksum-pinned release once. All release license/source files are retained. */
final class StockfishInstallation {
    private static final String RELEASE = "https://github.com/official-stockfish/Stockfish/releases/download/sf_19/";
    private record Release(String name, String sha256) {
        String archive() { return name + (name.contains("windows") ? ".zip" : ".tar.gz"); }
        String binary() { return name + (name.contains("windows") ? ".exe" : ""); }
    }
    private StockfishInstallation() { }

    static Path executable() throws Exception {
        String override = System.getProperty("chess.stockfish.path", System.getenv("CHESS_STOCKFISH_PATH"));
        if (override != null && !override.isBlank()) {
            Path executable = Path.of(override).toAbsolutePath().normalize();
            if (!Files.isRegularFile(executable)) throw new IOException("Nie znaleziono Stockfisha: " + executable);
            return executable;
        }
        Release release = release();
        Path cache = cache();
        Files.createDirectories(cache);
        Path marker = cache.resolve("sf19-" + release.name + ".path");
        try (FileChannel channel = FileChannel.open(cache.resolve("install.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            try (FileLock ignored = acquire(channel)) {
                if (Files.isRegularFile(marker)) {
                    Path path = cache.resolve(Files.readString(marker).strip()).normalize();
                    if (path.startsWith(cache) && Files.isRegularFile(path)) return path;
                }
                Path archive = cache.resolve(release.archive());
                if (!Files.isRegularFile(archive) || !checksum(archive).equals(release.sha256)) {
                    download(release, archive);
                }
                if (!checksum(archive).equals(release.sha256)) {
                    throw new IOException("Suma kontrolna pobranego Stockfisha jest niepoprawna. Spróbuj ponownie.");
                }
                Path unpacked = Files.createDirectory(cache.resolve("sf19-" + UUID.randomUUID()));
                if (release.archive().endsWith(".zip")) unzip(archive, unpacked);
                else untar(archive, unpacked);
                Path executable;
                try (var paths = Files.walk(unpacked)) {
                    executable = paths.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().equals(release.binary()))
                            .findFirst().orElseThrow(() -> new IOException("W archiwum brakuje pliku silnika."));
                }
                if (!release.name.contains("windows") && !executable.toFile().setExecutable(true, true)) {
                    throw new IOException("Nie udało się nadać uprawnień do uruchomienia Stockfisha.");
                }
                Path preparedMarker = cache.resolve(marker.getFileName() + "." + UUID.randomUUID() + ".tmp");
                Files.writeString(preparedMarker, cache.relativize(executable).toString());
                try {
                    Files.move(preparedMarker, marker, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(preparedMarker, marker, StandardCopyOption.REPLACE_EXISTING);
                }
                return executable;
            }
        }
    }
    private static FileLock acquire(FileChannel channel) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(5);
        while (System.nanoTime() < deadline) {
            try {
                FileLock lock = channel.tryLock();
                if (lock != null) return lock;
            } catch (OverlappingFileLockException ignored) { }
            Thread.sleep(100);
        }
        throw new IOException("Trwa instalacja Stockfisha w innym procesie. Spróbuj ponownie później.");
    }
    private static Path cache() throws Exception {
        String configured = System.getProperty("chess.stockfish.cache");
        if (configured != null && !configured.isBlank()) return Path.of(configured).toAbsolutePath().normalize();
        Path location = Path.of(StockfishInstallation.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        for (Path parent = location; parent != null; parent = parent.getParent()) {
            if (Files.isRegularFile(parent.resolve("pom.xml")) && Files.isDirectory(parent.resolve("core"))) {
                return parent.resolve(".tools/stockfish").toAbsolutePath().normalize();
            }
        }
        return Path.of(System.getProperty("user.home"), ".cache", "szachy", "stockfish").toAbsolutePath().normalize();
    }
    private static Release release() throws IOException {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        boolean arm = arch.equals("aarch64") || arch.equals("arm64");
        boolean x64 = arch.equals("amd64") || arch.equals("x86_64");
        if (os.contains("mac") && (arm || x64)) return new Release("stockfish-macos-universal",
                "a1f0e3bcc5a6927a11fe6fc8e54a779754645f3c2bae2cf13420fd1957adaa77");
        if (os.contains("win") && x64) return new Release("stockfish-windows-x86-64-universal",
                "3c8bf1f9ea66a09350a40df4f632288285ac206d99f33ab5842c408fc30b48a7");
        if (os.contains("win") && arm) return new Release("stockfish-windows-arm64-universal",
                "8372ad3f0d7276deb2c70f801f541ec7db463219fc6d9c7592864e542aa4f401");
        if (os.contains("linux") && x64) return new Release("stockfish-linux-x86-64-universal",
                "9defc0d4e55d49c65a6d042f3e571a39fcea499ade6dbe741b53b8c65e03611f");
        if (os.contains("linux") && arm) return new Release("stockfish-linux-arm64-universal",
                "fe26cfd1d9db4c8af3d21e24d9ff34cacb31c1f940085a7583da11796f2bac01");
        throw new IOException("Brak automatycznego pakietu dla " + os + "/" + arch
                + ". Wskaż własny plik przez CHESS_STOCKFISH_PATH.");
    }
    private static void download(Release release, Path destination) throws Exception {
        Path temporary = destination.resolveSibling(destination.getFileName() + "." + UUID.randomUUID() + ".part");
        HttpURLConnection connection = (HttpURLConnection) URI.create(RELEASE + release.archive()).toURL().openConnection();
        connection.setConnectTimeout(20_000);
        connection.setReadTimeout(30_000);
        connection.setRequestProperty("User-Agent", "Chess-Stockfish-Installer/1.0");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Pobieranie Stockfisha: HTTP " + connection.getResponseCode());
            try (InputStream input = connection.getInputStream(); OutputStream out = Files.newOutputStream(temporary)) {
                byte[] bytes = new byte[65536];
                long total = 0;
                int length;
                while ((length = input.read(bytes)) >= 0) {
                    interrupted();
                    total += length;
                    if (total > 160_000_000) throw new IOException("Archiwum Stockfisha jest zbyt duże.");
                    out.write(bytes, 0, length);
                }
            }
            if (!checksum(temporary).equals(release.sha256)) throw new IOException("Niepoprawna suma kontrolna Stockfisha.");
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
            Files.deleteIfExists(temporary);
        }
    }
    private static String checksum(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] bytes = new byte[65536];
            int length;
            while ((length = input.read(bytes)) >= 0) { interrupted(); digest.update(bytes, 0, length); }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private static void unzip(Path archive, Path directory) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            java.util.zip.ZipEntry entry;
            long total = 0;
            while ((entry = zip.getNextEntry()) != null) {
                interrupted();
                Path target = directory.resolve(entry.getName()).normalize();
                if (!target.startsWith(directory)) throw new IOException("Niepoprawna ścieżka w archiwum.");
                if (entry.isDirectory()) { Files.createDirectories(target); continue; }
                Files.createDirectories(target.getParent());
                try (OutputStream out = Files.newOutputStream(target)) {
                    byte[] bytes = new byte[65536];
                    int length;
                    while ((length = zip.read(bytes)) >= 0) {
                        interrupted();
                        total += length;
                        if (total > 512_000_000) throw new IOException("Rozpakowane archiwum jest zbyt duże.");
                        out.write(bytes, 0, length);
                    }
                }
            }
        }
    }
    private static void untar(Path archive, Path directory) throws Exception {
        Process tar = new ProcessBuilder("tar", "-xzf", archive.toString(), "-C", directory.toString())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!tar.waitFor(60, TimeUnit.SECONDS) || tar.exitValue() != 0) {
                throw new IOException("Nie udało się rozpakować Stockfisha poleceniem tar.");
            }
        } finally { if (tar.isAlive()) tar.destroyForcibly(); }
    }
    private static void interrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Anulowano obliczenia.");
    }
}
