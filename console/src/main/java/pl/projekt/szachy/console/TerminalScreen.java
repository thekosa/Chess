package pl.projekt.szachy.console;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

/** A small ANSI screen with row diffing, so animation never clears every frame. */
final class TerminalScreen implements AutoCloseable {
    static final int BACKGROUND = 0x16251F;
    static final int TEXT = 0xE5EADC;
    private static final String ESC = "\u001b[";
    private static final String PROMPT = "Ruch > ";
    private final PrintWriter out;
    private final boolean ansi;
    private final Terminal terminal;
    private final BufferedReader plainInput;
    private Frame previous;
    private final AtomicBoolean closed = new AtomicBoolean();
    private Attributes originalAttributes;
    private int columns;
    private int rows;
    private int promptRow;
    private int version;
    private int promptVersion = -1;
    private String previousPrompt;

    TerminalScreen(Terminal terminal) {
        this.terminal = terminal;
        this.out = terminal.writer();
        this.ansi = true;
        this.plainInput = null;
        refreshSize();
    }

    TerminalScreen(PrintWriter out, BufferedReader input, int columns, int rows) {
        this.terminal = null;
        this.out = out;
        this.ansi = false;
        this.plainInput = input;
        this.columns = columns;
        this.rows = rows;
    }

    void open() {
        if (ansi) {
            originalAttributes = terminal.enterRawMode();
            // No OS echo or automatic newline: the prompt is painted like any other row.
            terminal.echo(false);
            out.print(ESC + "?1049h" + ESC + "2J" + ESC + "H");
            out.flush();
        }
    }

    void draw(Frame frame) {
        if (!ansi) {
            for (int y = 0; y < frame.height; y++) {
                char[] line = frame.chars[y].clone();
                for (int x = 0; x < frame.width; x++) {
                    // A monochrome fallback uses filled silhouettes for black pieces.
                    if (line[x] != ' ' && frame.foreground[y][x] == 0x0E1916) {
                        line[x] = '#';
                    }
                }
                out.println(new String(line).stripTrailing());
            }
            out.flush();
            return;
        }
        refreshSize();
        frame = fittingFrame(frame);
        StringBuilder buffer = new StringBuilder();
        if (previous == null) {
            buffer.append(ESC).append("0m").append(ESC).append("2J");
        }
        if (previous != null && (previous.width != frame.width || previous.height != frame.height)) {
            buffer.append(ESC).append("2J");
            previous = null;
        }
        for (int y = 0; y < frame.height; y++) {
            if (previous != null && Arrays.equals(frame.chars[y], previous.chars[y])
                    && Arrays.equals(frame.foreground[y], previous.foreground[y])
                    && Arrays.equals(frame.background[y], previous.background[y])) {
                continue;
            }
            buffer.append(ESC).append(y + 1).append(";1H");
            int fg = -1;
            int bg = -1;
            for (int x = 0; x < frame.width; x++) {
                if (fg != frame.foreground[y][x]) {
                    fg = frame.foreground[y][x];
                    color(buffer, true, fg);
                }
                if (bg != frame.background[y][x]) {
                    bg = frame.background[y][x];
                    color(buffer, false, bg);
                }
                buffer.append(frame.chars[y][x]);
            }
            buffer.append(ESC).append("0m").append(ESC).append("K");
        }
        if (!buffer.isEmpty()) {
            out.print(ESC + "?25l");
            // The previously typed command must not remain underneath an animation.
            if (previousPrompt != null) {
                out.print(ESC + promptRow + ";1H" + ESC + "0m" + ESC + "2K");
            }
            out.print(buffer);
            out.flush();
            version++;
        }
        previous = frame;
        promptRow = frame.height + 1;
    }

    String readLine(BiFunction<Integer, Integer, Frame> view) throws IOException {
        if (!ansi) {
            draw(view.apply(columns, rows));
            out.print(PROMPT);
            out.flush();
            return plainInput.readLine();
        }
        StringBuilder input = new StringBuilder();
        int escapeState = 0;
        while (!closed.get()) {
            refreshSize();
            draw(view.apply(columns, rows));
            showPrompt(input.toString());
            // Poll while waiting, so maximizing, shrinking or changing the font redraws immediately.
            int key = terminal.reader().read(100);
            if (key == NonBlockingReader.READ_EXPIRED) {
                continue;
            }
            if (key < 0 || key == 3 || (key == 4 && input.isEmpty())) {
                return null;
            }
            if (escapeState != 0) {
                if (escapeState == 1 && (key == '[' || key == 'O')) {
                    escapeState = 2;
                } else if (escapeState == 1 || (key >= 0x40 && key <= 0x7e)) {
                    escapeState = 0;
                }
                continue;
            }
            if (key == 27) {
                escapeState = 1;
            } else if (key == '\r' || key == '\n') {
                return input.toString();
            } else if (key == 8 || key == 127) {
                if (!input.isEmpty()) {
                    input.deleteCharAt(input.length() - 1);
                }
            } else if (key == 21) {
                input.setLength(0);
            } else if (key >= 32 && key < 127 && input.length() < 80) {
                input.append((char) key);
            }
        }
        return null;
    }

    int columns() {
        refreshSize();
        return columns;
    }

    int rows() {
        refreshSize();
        return rows;
    }

    private void refreshSize() {
        if (terminal == null) {
            return;
        }
        org.jline.terminal.Size viewport = terminal.getSize();
        int newColumns = Math.max(2, viewport.getColumns());
        int newRows = Math.max(3, viewport.getRows());
        if (newColumns != columns || newRows != rows) {
            columns = newColumns;
            rows = newRows;
            previous = null;
            previousPrompt = null;
        }
    }

    private Frame fittingFrame(Frame frame) {
        // Always reserve the last column and two rows. Writing in the lower right
        // corner can scroll the terminal and invalidate every cached board row.
        if (frame.width < columns && frame.height + 2 <= rows) {
            return frame;
        }
        Frame warning = new Frame(Math.max(1, columns - 1), Math.max(1, Math.min(5, rows - 2)));
        warning.text(0, 0, "Okno terminala jest za male.");
        warning.text(0, 1, "Powieksz okno lub zmniejsz czcionke.");
        warning.text(0, 2, "Minimum: 53 kolumny x 34 wiersze.");
        warning.text(0, 3, "Partia pozostaje zachowana. Wyjscie: quit.");
        return warning;
    }

    private void showPrompt(String input) {
        int room = Math.max(0, columns - PROMPT.length() - 2);
        String visible = input.substring(Math.max(0, input.length() - room));
        String text = (PROMPT + visible).substring(0, Math.min(columns - 1, PROMPT.length() + visible.length()));
        if (version != promptVersion || !text.equals(previousPrompt)) {
            out.print(ESC + promptRow + ";1H" + ESC + "0m" + ESC + "2K" + text + ESC + "?25h");
            out.flush();
            previousPrompt = text;
            promptVersion = version;
        }
    }

    private static void color(StringBuilder buffer, boolean foreground, int rgb) {
        buffer.append(ESC).append(foreground ? "38;2;" : "48;2;")
                .append((rgb >> 16) & 255).append(';')
                .append((rgb >> 8) & 255).append(';').append(rgb & 255).append('m');
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (ansi) {
            out.print(ESC + "0m" + ESC + "?25h" + ESC + "?1049l");
            out.flush();
            try {
                if (originalAttributes != null) {
                    terminal.setAttributes(originalAttributes);
                }
            } finally {
                try {
                    terminal.close();
                } catch (IOException ignored) {
                    // The OS may already have closed the terminal window.
                }
            }
        }
    }

    static final class Frame {
        final int width;
        final int height;
        final char[][] chars;
        final int[][] foreground;
        final int[][] background;

        Frame(int width, int height) {
            this.width = width;
            this.height = height;
            chars = new char[height][width];
            foreground = new int[height][width];
            background = new int[height][width];
            for (int y = 0; y < height; y++) {
                Arrays.fill(chars[y], ' ');
                Arrays.fill(foreground[y], TEXT);
                Arrays.fill(background[y], BACKGROUND);
            }
        }

        void text(int x, int y, String text) {
            for (int i = 0; i < text.length() && x + i < width; i++) {
                if (x + i >= 0 && y >= 0 && y < height) {
                    chars[y][x + i] = text.charAt(i);
                }
            }
        }
    }
}
