package pl.projekt.szachy.desktop;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import pl.projekt.szachy.core.api.GameApi;
import pl.projekt.szachy.core.model.GameState;
import pl.projekt.szachy.core.model.Side;

import java.util.Objects;

public final class MainWindowController {
    private final GameApi game;
    private final BoardView boardView = new BoardView();

    @FXML
    private StackPane boardContainer;
    @FXML
    private Label turnLabel;

    public MainWindowController(GameApi game) {
        this.game = Objects.requireNonNull(game);
    }

    @FXML
    private void initialize() {
        boardContainer.getChildren().setAll(boardView);
        render(game.getState());
    }

    @FXML
    private void newGame() {
        render(game.newGame());
    }

    private void render(GameState state) {
        boardView.render(state);
        turnLabel.setText(state.sideToMove() == Side.WHITE ? "Ruch: białe" : "Ruch: czarne");
    }
}
