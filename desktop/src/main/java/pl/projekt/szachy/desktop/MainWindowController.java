package pl.projekt.szachy.desktop;

import javafx.fxml.FXML;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.control.Button;
import pl.projekt.szachy.computer.ComputerGame;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import pl.projekt.szachy.core.api.GameApi;
import pl.projekt.szachy.core.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class MainWindowController implements AutoCloseable {
    private final GameApi game;
    private final BoardView boardView;
    private Square selected;
    private List<Move> legalMoves = List.of();
    private GameState displayedState;
    private Timeline computerUpdates;

    @FXML
    private StackPane boardContainer;
    @FXML
    private Label turnLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Label capturedLabel;
    @FXML
    private ListView<String> historyList;
    @FXML private Button retryButton;
    @FXML private Button claimDrawButton;

    public MainWindowController(GameApi game) {
        this.game = Objects.requireNonNull(game);
        boardView = new BoardView(this::onSquareClicked);
    }

    @FXML
    private void initialize() {
        boardContainer.getChildren().setAll(boardView);
        render(game.getState());
        if (game instanceof ComputerGame) {
            computerUpdates = new Timeline(new KeyFrame(Duration.millis(100), event -> updateComputer()));
            computerUpdates.setCycleCount(Timeline.INDEFINITE);
            computerUpdates.play();
            updateComputer();
        }
    }

    @FXML
    private void newGame() {
        if (game instanceof ComputerGame computer) {
            GameOptionsView.choose(boardContainer.getScene().getWindow(), computer.snapshot().options())
                    .ifPresent(options -> {
                        clearSelection();
                        render(computer.configure(options));
                        messageLabel.setText("Nowa partia. Zaczynają białe.");
                        updateComputer();
                    });
            return;
        }
        clearSelection();
        render(game.newGame());
        messageLabel.setText("Nowa partia. Zaczynają białe.");
    }

    private void onSquareClicked(Square square) {
        if (boardView.isAnimating()) {
            return;
        }
        GameState state = game.getState();
        if (game instanceof ComputerGame computer && computer.isComputerTurn()) {
            updateComputer();
            return;
        }
        if (state.status().isFinished()) {
            messageLabel.setText("Partia zakończona. Możesz rozpocząć nową grę.");
            return;
        }
        if (square.equals(selected)) {
            clearSelection();
            messageLabel.setText("");
            render(state);
            return;
        }
        if (selected != null) {
            List<Move> candidates = legalMoves.stream().filter(move -> move.to().equals(square)).toList();
            if (!candidates.isEmpty()) {
                Optional<Move> chosen = chooseMove(candidates);
                if (chosen.isEmpty()) {
                    return;
                }
                Move move = chosen.get();
                MoveResult result = game.makeMove(move);
                clearSelection();
                if (result.applied()) {
                    messageLabel.setText("Ruch " + move.from() + " → " + move.to() + ".");
                    boardView.animateMove(state, result.state(), move, () -> render(result.state()));
                } else {
                    render(result.state());
                    messageLabel.setText(errorMessage(result.error()));
                }
                return;
            }
        }
        Piece piece = state.board().get(square);
        if (piece != null && piece.side() == state.sideToMove()) {
            selected = square;
            legalMoves = game.getLegalMoves(square);
            messageLabel.setText(legalMoves.isEmpty() ? "Ta figura nie ma legalnego ruchu."
                    : "Wybierz podświetlone pole.");
        } else {
            messageLabel.setText(selected == null ? "Teraz ruch " + sideName(state.sideToMove()) + "."
                    : "Wybierz jedno z podświetlonych pól.");
        }
        render(state);
    }

    private Optional<Move> chooseMove(List<Move> candidates) {
        if (candidates.getFirst().promotion() == null) {
            return Optional.of(candidates.getFirst());
        }
        List<PromotionChoice> choices = Arrays.stream(PromotionChoice.values())
                .filter(choice -> candidates.stream().anyMatch(move -> move.promotion() == choice.type))
                .toList();
        ChoiceDialog<PromotionChoice> dialog = new ChoiceDialog<>(choices.getFirst(), choices);
        dialog.setTitle("Promocja pionka");
        dialog.setHeaderText("Na jaką figurę zamienić pionka?");
        dialog.setContentText("Figura:");
        if (boardContainer.getScene().getWindow() != null) {
            dialog.initOwner(boardContainer.getScene().getWindow());
        }
        return dialog.showAndWait().flatMap(choice -> candidates.stream()
                .filter(move -> move.promotion() == choice.type).findFirst());
    }

    private void clearSelection() {
        selected = null;
        legalMoves = List.of();
    }

    private void render(GameState state) {
        displayedState = state;
        boardView.render(state, selected, legalMoves.stream().map(Move::to).collect(Collectors.toSet()));
        turnLabel.setText(switch (state.status()) {
            case ACTIVE -> "Ruch: " + sideName(state.sideToMove());
            case CHECK -> "Ruch: " + sideName(state.sideToMove()) + "\nSzach!";
            case CHECKMATE -> "Mat! Wygrywają " + sideName(state.sideToMove().opposite()) + ".";
            case STALEMATE -> "Pat. Remis.";
            case DRAW_REPETITION -> "Remis: trzykrotne powtórzenie pozycji.";
            case DRAW_FIFTY_MOVES -> "Remis: reguła 50 posunięć.";
            case DRAW_INSUFFICIENT_MATERIAL -> "Remis: niewystarczający materiał do mata.";
            case DRAW_FIVEFOLD_REPETITION -> "Remis: pięciokrotne powtórzenie pozycji.";
            case DRAW_SEVENTY_FIVE_MOVES -> "Remis: reguła 75 posunięć.";
        });
        boolean claimAvailable = state.canClaimDraw()
                && (!(game instanceof ComputerGame computer) || !computer.isComputerTurn());
        claimDrawButton.setVisible(claimAvailable);
        claimDrawButton.setManaged(claimAvailable);
        historyList.getItems().clear();
        for (int index = 0; index < state.moveHistory().size(); index++) {
            Move move = state.moveHistory().get(index);
            String text = (index / 2 + 1) + (index % 2 == 0 ? ". " : "... ")
                    + move.from() + " → " + move.to();
            if (move.promotion() != null) {
                text += " = " + BoardView.pieceName(move.promotion());
            }
            historyList.getItems().add(text);
        }
        if (!historyList.getItems().isEmpty()) {
            historyList.scrollTo(historyList.getItems().size() - 1);
        }
        capturedLabel.setText("Zbite białe: " + captures(state, Side.WHITE)
                + "\nZbite czarne: " + captures(state, Side.BLACK));
    }

    private void updateComputer() {
        if (!(game instanceof ComputerGame computer) || boardView.isAnimating()) return;
        var snapshot = computer.snapshot();
        GameState next = snapshot.state();
        if (next != displayedState) {
            GameState before = displayedState;
            clearSelection();
            if (next.moveHistory().size() == before.moveHistory().size() + 1) {
                Move move = next.moveHistory().getLast();
                messageLabel.setText("Stockfish: " + move.from() + " → " + move.to() + ".");
                boardView.animateMove(before, next, move, () -> render(next));
                return;
            }
            render(next);
        }
        retryButton.setVisible(snapshot.error() != null);
        retryButton.setManaged(snapshot.error() != null);
        if (snapshot.thinking()) {
            messageLabel.setText("Stockfish myśli… Przy pierwszym użyciu pobiera się silnik (ok. 80 MB).");
        } else if (snapshot.error() != null) {
            messageLabel.setText(snapshot.error());
        } else if (snapshot.options().opponent() == pl.projekt.szachy.computer.Opponent.COMPUTER) {
            turnLabel.setText(turnLabel.getText().split("\nStockfish:")[0] + "\nStockfish: "
                    + snapshot.options().difficulty());
        }
    }
    @FXML private void retryComputer() {
        if (game instanceof ComputerGame computer) { computer.retryComputer(); updateComputer(); }
    }
    @FXML private void claimDraw() {
        if (boardView.isAnimating()) return;
        if (game.claimDraw()) {
            clearSelection();
            render(game.getState());
            messageLabel.setText("Remis został zgłoszony.");
        } else messageLabel.setText("W tej pozycji nie można zgłosić remisu.");
    }
    @Override public void close() {
        if (computerUpdates != null) computerUpdates.stop();
        boardView.close();
    }

    private String captures(GameState state, Side side) {
        String result = Arrays.stream(PieceType.values())
                .map(type -> {
                    long count = state.capturedPieces().stream()
                            .filter(piece -> piece.side() == side && piece.type() == type).count();
                    return count == 0 ? "" : BoardView.pieceName(type) + " ×" + count;
                })
                .filter(text -> !text.isEmpty()).collect(Collectors.joining(", "));
        return result.isEmpty() ? "—" : result;
    }

    private String sideName(Side side) {
        return side == Side.WHITE ? "białe" : "czarne";
    }

    private String errorMessage(MoveError error) {
        return switch (error) {
            case NONE -> "";
            case WRONG_SIDE -> "To nie kolej tej strony.";
            case ILLEGAL_MOVE -> "Ten ruch jest niedozwolony.";
            case PROMOTION_REQUIRED -> "Wybierz figurę do promocji pionka.";
            case GAME_OVER -> "Partia już się zakończyła.";
        };
    }

    private enum PromotionChoice {
        QUEEN(PieceType.QUEEN), ROOK(PieceType.ROOK), BISHOP(PieceType.BISHOP), KNIGHT(PieceType.KNIGHT);

        private final PieceType type;

        PromotionChoice(PieceType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return BoardView.pieceName(type);
        }
    }
}
