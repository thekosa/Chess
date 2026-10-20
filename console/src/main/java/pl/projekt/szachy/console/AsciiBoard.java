package pl.projekt.szachy.console;

import pl.projekt.szachy.core.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Presentation only: all animation paths are differences between engine snapshots. */
final class AsciiBoard {
    private static final int LEFT = 3;
    private static final int TOP = 2;
    private static final int WHITE = 0xFFFAE5;
    private static final int BLACK = 0x0E1916;
    private Size size;
    private Size preferredSize;

    AsciiBoard(Size size) {
        this.size = size;
        this.preferredSize = size;
    }

    void setSize(Size size) {
        preferredSize = size;
    }

    void fitToViewport(int columns, int rows) {
        Size fitting = Size.fitting(columns, rows);
        size = fitting.ordinal() > preferredSize.ordinal() ? fitting : preferredSize;
    }

    TerminalScreen.Frame frame(GameState state, Square selected, Set<Square> targets, String message) {
        TerminalScreen.Frame frame = background(state, selected, targets, message);
        state.board().forEach((square, piece) -> piece(frame, square.file() * size.width,
                (7 - square.rank()) * size.height, piece, 1));
        return frame;
    }

    TerminalScreen.Frame animationFrame(GameState before, GameState after, Move move,
                                         double progress, String message) {
        TerminalScreen.Frame frame = background(before, null, Set.of(), message);
        List<Travel> travelling = travels(before, after, move);
        for (Map.Entry<Square, Piece> entry : before.board().entrySet()) {
            Square square = entry.getKey();
            Piece value = entry.getValue();
            if (travelling.stream().anyMatch(travel -> travel.from.equals(square))) {
                continue;
            }
            boolean captured = value.side() != before.sideToMove()
                    && !value.equals(after.board().get(square));
            // The victim stays visible until the moving sprite approaches its square.
            double opacity = captured ? Math.clamp((1 - progress) / 0.45, 0, 1) : 1;
            piece(frame, square.file() * size.width, (7 - square.rank()) * size.height, value, opacity);
        }
        double eased = progress * progress * (3 - 2 * progress);
        for (Travel travel : travelling) {
            int x = (int) Math.round((travel.from.file()
                    + (travel.to.file() - travel.from.file()) * eased) * size.width);
            int y = (int) Math.round((7 - travel.from.rank()
                    - (travel.to.rank() - travel.from.rank()) * eased) * size.height);
            piece(frame, x, y, travel.piece, 1);
        }
        return frame;
    }

    private List<Travel> travels(GameState before, GameState after, Move move) {
        List<Travel> result = new ArrayList<>();
        result.add(new Travel(move.from(), move.to(), before.board().get(move.from())));
        before.board().forEach((from, piece) -> {
            if (from.equals(move.from()) || piece.side() != before.sideToMove()
                    || piece.equals(after.board().get(from))) {
                return;
            }
            List<Square> arrivals = after.board().entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(move.to()) && piece.equals(entry.getValue())
                            && !piece.equals(before.board().get(entry.getKey())))
                    .map(Map.Entry::getKey).toList();
            if (arrivals.size() == 1) {
                result.add(new Travel(from, arrivals.getFirst(), piece));
            }
        });
        return result;
    }

    private TerminalScreen.Frame background(GameState state, Square selected,
                                              Set<Square> targets, String message) {
        int boardHeight = size.height * 8;
        TerminalScreen.Frame frame = new TerminalScreen.Frame(LEFT + size.width * 8 + 1, boardHeight + 8);
        frame.text(LEFT, 0, "SZACHY / ASCII    jasne figury: biale | ciemne: czarne");
        for (int rank = 0; rank < 8; rank++) {
            frame.text(0, TOP + (7 - rank) * size.height + size.height / 2, String.valueOf(rank + 1));
            for (int file = 0; file < 8; file++) {
                Square square = new Square(file, rank);
                int background = (file + rank) % 2 == 0 ? 0x597568 : 0x8D9A7F;
                if (targets.contains(square)) {
                    background = 0x639B98;
                }
                if (square.equals(selected)) {
                    background = 0xAD8644;
                }
                Piece occupant = state.board().get(square);
                if (state.status() == GameStatus.CHECK && occupant != null
                        && occupant.side() == state.sideToMove() && occupant.type() == PieceType.KING) {
                    background = 0xAE665B;
                }
                for (int y = 0; y < size.height; y++) {
                    for (int x = 0; x < size.width; x++) {
                        frame.background[TOP + (7 - rank) * size.height + y][LEFT + file * size.width + x]
                                = background;
                    }
                }
                if (targets.contains(square) && occupant == null) {
                    frame.text(LEFT + file * size.width + size.width / 2,
                            TOP + (7 - rank) * size.height + size.height / 2, "*");
                }
            }
        }
        int footer = TOP + boardHeight;
        for (int file = 0; file < 8; file++) {
            frame.text(LEFT + file * size.width + size.width / 2, footer, "" + (char) ('a' + file));
        }
        String status = switch (state.status()) {
            case ACTIVE -> "Ruch: " + sideName(state.sideToMove());
            case CHECK -> "SZACH! Ruch: " + sideName(state.sideToMove());
            case CHECKMATE -> "MAT! Wygrywaja " + sideName(state.sideToMove().opposite()) + ".";
            case STALEMATE -> "PAT. Remis.";
        };
        if (!state.moveHistory().isEmpty()) {
            Move last = state.moveHistory().getLast();
            status += "    Ostatni ruch: " + last.from() + "-" + last.to()
                    + (last.promotion() == null ? "" : "=" + last.promotion().name());
        }
        frame.text(LEFT, footer + 1, status);
        long white = state.capturedPieces().stream().filter(piece -> piece.side() == Side.WHITE).count();
        frame.text(LEFT, footer + 2, "Zbite biale: " + white + " | Zbite czarne: "
                + (state.capturedPieces().size() - white));
        frame.text(LEFT, footer + 3, message);
        frame.text(LEFT, footer + 4, "e2 e4 | e2 (wybor) | moves e2 | new | quit");
        frame.text(LEFT, footer + 5, "help | history | size full/compact/small/tiny");
        return frame;
    }

    private void piece(TerminalScreen.Frame frame, int x, int y, Piece piece, double opacity) {
        if (piece == null || opacity < 0.05) {
            return;
        }
        String[] sprite = PieceArt.sprite(piece.type(), size != Size.FULL);
        int artWidth = sprite[0].length();
        int originX = LEFT + x + (size.width - artWidth) / 2;
        int color = piece.side() == Side.WHITE ? WHITE : BLACK;
        for (int row = 0; row < size.height; row++) {
            int sourceRow = size.height == sprite.length ? row
                    : (int) Math.round(row * (sprite.length - 1.0) / (size.height - 1));
            String line = sprite[sourceRow];
            for (int col = 0; col < line.length(); col++) {
                int screenX = originX + col;
                int screenY = TOP + y + row;
                if (line.charAt(col) != ' ' && screenX >= LEFT && screenX < LEFT + size.width * 8
                        && screenY >= TOP && screenY < TOP + size.height * 8) {
                    frame.chars[screenY][screenX] = line.charAt(col);
                    frame.foreground[screenY][screenX] = blend(color, frame.background[screenY][screenX], opacity);
                }
            }
        }
    }

    private static int blend(int foreground, int background, double opacity) {
        int rgb = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            int from = (foreground >> shift) & 255;
            int to = (background >> shift) & 255;
            rgb |= (int) Math.round(from * opacity + to * (1 - opacity)) << shift;
        }
        return rgb;
    }

    private static String sideName(Side side) {
        return side == Side.WHITE ? "biale" : "czarne";
    }

    enum Size {
        FULL(14, 7), COMPACT(10, 5), SMALL(8, 4), TINY(6, 3);

        final int width;
        final int height;

        Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        static Size fitting(int columns, int rows) {
            for (Size size : values()) {
                if (columns >= size.width * 8 + 5 && rows >= size.height * 8 + 10) {
                    return size;
                }
            }
            return TINY;
        }
    }

    private record Travel(Square from, Square to, Piece piece) { }
}
