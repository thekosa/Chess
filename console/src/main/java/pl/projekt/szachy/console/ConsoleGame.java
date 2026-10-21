package pl.projekt.szachy.console;

import pl.projekt.szachy.core.api.GameApi;
import pl.projekt.szachy.computer.*;
import pl.projekt.szachy.core.model.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Line input and ASCII output; every chess decision is delegated to GameApi. */
final class ConsoleGame {
    private final GameApi game;
    private final TerminalScreen screen;
    private final AsciiBoard board;
    private final boolean animate;
    private Square selected;
    private Set<Square> targets = Set.of();
    private GameState displayedState;
    private GameState celebrated;
    private long celebrationStart;
    private String message = "Wpisz e2, aby zaznaczyc pionka, potem e4, aby go przesunac.";

    ConsoleGame(GameApi game, TerminalScreen screen,
                AsciiBoard.Size size, boolean animate) {
        this.game = game;
        this.screen = screen;
        this.board = new AsciiBoard(size);
        this.animate = animate;
        this.displayedState = game.getState();
    }

    void run() throws IOException, InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            // Plain input cannot poll while readLine blocks, so finish the computer turn first.
            if (!animate && game instanceof ComputerGame computer) {
                boolean announced = false;
                while (computer.snapshot().thinking()) {
                    if (!announced) { screen.draw(frame(screen.columns(), screen.rows())); announced = true; }
                    Thread.sleep(100);
                }
            }
            String line = screen.readLine((columns, rows) -> {
                return frame(columns, rows);
            });
            if (line == null || line.strip().equalsIgnoreCase("quit") || line.strip().equalsIgnoreCase("exit")) {
                return;
            }
            try {
                command(line.strip().toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                message = exception.getMessage();
            }
        }
    }

    private void command(String line) throws InterruptedException {
        celebrationStart = 0;
        if (line.isEmpty()) {
            return;
        }
        String[] parts = line.split("\\s+");
        switch (parts[0]) {
            case "new" -> {
                requireCount(parts, 1);
                game.newGame();
                displayedState = game.getState();
                clearSelection();
                message = "Nowa partia. Zaczynaja biale.";
            }
            case "moves" -> {
                requireCount(parts, 2);
                select(square(parts[1]));
            }
            case "size" -> {
                requireCount(parts, 2);
                try {
                    board.setSize(AsciiBoard.Size.valueOf(parts[1].toUpperCase(Locale.ROOT)));
                    message = "Maksymalny rozmiar: " + parts[1] + ". Plansza dopasowuje sie do okna.";
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("Rozmiary: full, compact, small, tiny.");
                }
            }
            case "help" -> message = "e2 e4; e7 e8 q/r/b/n; new; computer easy/medium/hard [white/black]; local; retry; draw; skip; quit.";
            case "draw" -> {
                requireCount(parts, 1);
                message = game.claimDraw() ? "Remis zostal zgloszony." : "W tej pozycji nie mozna zglosic remisu.";
                clearSelection();
            }
            case "skip" -> { requireCount(parts, 1); message = "Efekt pominiety. Nowa partia: new."; }
            case "retry" -> {
                requireCount(parts, 1);
                if (game instanceof ComputerGame computer) computer.retryComputer();
            }
            case "local", "computer" -> {
                if (!(game instanceof ComputerGame computer)) throw new IllegalArgumentException("Brak modulu komputera.");
                GameOptions options;
                if (parts[0].equals("local")) { requireCount(parts, 1); options = GameOptions.local(); }
                else {
                    if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException("computer easy/medium/hard [white/black]");
                    options = new GameOptions(Opponent.COMPUTER, Difficulty.valueOf(parts[1].toUpperCase(Locale.ROOT)),
                            parts.length == 3 ? Side.valueOf(parts[2].toUpperCase(Locale.ROOT)) : Side.WHITE);
                }
                computer.configure(options);
                displayedState = computer.getState();
                clearSelection();
                message = "Nowa partia: " + options.opponent() + (options.opponent() == Opponent.COMPUTER
                        ? " / " + options.difficulty() + " / grasz " + (options.humanSide() == Side.WHITE ? "bialymi" : "czarnymi") : "");
            }
            case "history" -> message = history();
            case "cancel" -> {
                clearSelection();
                message = "Anulowano zaznaczenie.";
            }
            default -> {
                if (parts.length == 1) {
                    Square square = square(parts[0]);
                    if (selected != null && targets.contains(square)) {
                        makeMove(new Move(selected, square));
                    } else {
                        select(square);
                    }
                } else if (parts.length == 2 || parts.length == 3) {
                    makeMove(new Move(square(parts[0]), square(parts[1]),
                            parts.length == 3 ? promotion(parts[2]) : null));
                } else {
                    throw new IllegalArgumentException("Wpisz ruch, np. e2 e4, albo help.");
                }
            }
        }
    }

    private void select(Square square) {
        if (game instanceof ComputerGame computer && computer.isComputerTurn()) {
            throw new IllegalArgumentException("Teraz gra Stockfish. W razie bledu: retry; nowa partia: new.");
        }
        List<Move> moves = game.getLegalMoves(square);
        Piece piece = game.getState().board().get(square);
        if (piece == null || piece.side() != game.getState().sideToMove()) {
            throw new IllegalArgumentException("Wybierz figure strony, ktora jest teraz na ruchu.");
        }
        selected = square;
        targets = moves.stream().map(Move::to).collect(Collectors.toSet());
        String destinations = moves.stream().map(move -> move.to().toString()).distinct().sorted()
                .collect(Collectors.joining(", "));
        message = destinations.isEmpty() ? "Ta figura nie ma legalnego ruchu."
                : square + ": " + destinations;
    }

    private void makeMove(Move move) throws InterruptedException {
        GameState before = game.getState();
        MoveResult result = game.makeMove(move);
        if (!result.applied()) {
            message = switch (result.error()) {
                case WRONG_SIDE -> "To nie kolej tej strony.";
                case ILLEGAL_MOVE -> "Ten ruch jest niedozwolony.";
                case PROMOTION_REQUIRED -> "Wybierz promocje, np. e7 e8 q (hetman), r, b albo n.";
                case GAME_OVER -> "Partia zakonczona. Wpisz new, aby zaczac kolejna.";
                case NONE -> "";
            };
            return;
        }
        clearSelection();
        message = "Ruch " + move.from() + " -> " + move.to() + ".";
        animateMove(before, result.state(), move);
        displayedState = result.state();
    }

    private TerminalScreen.Frame frame(int columns, int rows) {
        GameState state = game.getState();
        if (state != displayedState) {
            clearSelection();
            if (state.moveHistory().size() == displayedState.moveHistory().size() + 1) {
                Move move = state.moveHistory().getLast();
                message = "Stockfish: " + move.from() + " -> " + move.to() + ".";
                try { animateMove(displayedState, state, move); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            }
            displayedState = state;
        }
        board.fitToViewport(columns, rows);
        if (!state.status().isFinished()) celebrated = null;
        else if (animate && !Boolean.getBoolean("chess.reducedMotion")) {
            if (celebrated != state) { celebrated = state; celebrationStart = System.nanoTime(); }
            double elapsed = (System.nanoTime() - celebrationStart) / 1e9;
            if (elapsed < 6.5) return board.celebrationFrame(state, elapsed, message);
        }
        String footer = message;
        if (game instanceof ComputerGame computer) {
            var snapshot = computer.snapshot();
            if (snapshot.thinking()) footer = "Stockfish mysli... Pierwszy start: pobieranie ok. 80 MB. new / quit";
            else if (snapshot.error() != null) footer = "Blad komputera. Wpisz retry / new / local. " + snapshot.error();
        }
        return board.frame(state, selected, targets, footer);
    }

    private void animateMove(GameState before, GameState after, Move move) throws InterruptedException {
        if (animate) {
            long start = System.nanoTime();
            long duration = 360_000_000L;
            double progress;
            do {
                progress = Math.min(1, (System.nanoTime() - start) / (double) duration);
                board.fitToViewport(screen.columns(), screen.rows());
                screen.draw(board.animationFrame(before, after, move, progress, message));
                if (progress < 1) {
                    Thread.sleep(25);
                }
            } while (progress < 1);
        }
    }

    private void clearSelection() {
        selected = null;
        targets = Set.of();
    }

    private String history() {
        List<Move> moves = game.getState().moveHistory();
        if (moves.isEmpty()) {
            return "Historia jest pusta.";
        }
        // A short tail fits the footer even on smaller terminals.
        return "Ostatnie: " + moves.subList(Math.max(0, moves.size() - 4), moves.size()).stream()
                .map(move -> move.from() + "-" + move.to()
                        + (move.promotion() == null ? "" : "=" + move.promotion().name()))
                .collect(Collectors.joining(" "));
    }

    private static Square square(String text) {
        if (!text.matches("[a-h][1-8]")) {
            throw new IllegalArgumentException("Pole zapisujemy od a1 do h8, np. e2. Pomoc: help.");
        }
        return new Square(text.charAt(0) - 'a', text.charAt(1) - '1');
    }

    private static PieceType promotion(String text) {
        return switch (text) {
            case "q" -> PieceType.QUEEN;
            case "r" -> PieceType.ROOK;
            case "b" -> PieceType.BISHOP;
            case "n" -> PieceType.KNIGHT;
            default -> throw new IllegalArgumentException("Promocja: q=hetman, r=wieza, b=goniec, n=skoczek.");
        };
    }

    private static void requireCount(String[] parts, int count) {
        if (parts.length != count) {
            throw new IllegalArgumentException("Niepoprawne argumenty polecenia. Wpisz help.");
        }
    }
}
