package pl.projekt.szachy.desktop;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import pl.projekt.szachy.computer.*;
import pl.projekt.szachy.core.model.Side;
import java.util.Optional;

/** The same choices are used by the mode launcher and the new-game dialog. */
final class GameOptionsView extends VBox {
    private final ComboBox<Opponent> opponent = new ComboBox<>();
    private final ComboBox<Difficulty> difficulty = new ComboBox<>();
    private final ComboBox<String> side = new ComboBox<>();

    GameOptionsView(GameOptions initial) {
        super(10);
        opponent.getItems().setAll(Opponent.values());
        opponent.setValue(initial.opponent());
        difficulty.getItems().setAll(Difficulty.values());
        difficulty.setValue(initial.difficulty());
        side.getItems().setAll("Białymi", "Czarnymi");
        side.setValue(initial.humanSide() == Side.WHITE ? "Białymi" : "Czarnymi");
        opponent.setId("opponent"); difficulty.setId("difficulty"); side.setId("human-side");
        opponent.setAccessibleText("Przeciwnik"); difficulty.setAccessibleText("Poziom trudności");
        side.setAccessibleText("Twój kolor");
        difficulty.disableProperty().bind(opponent.valueProperty().isEqualTo(Opponent.TWO_PLAYERS));
        side.disableProperty().bind(difficulty.disableProperty());
        HBox fields = new HBox(18, field("Z kim grasz?", opponent),
                field("Poziom komputera", difficulty), field("Grasz", side));
        Label hint = new Label("Stockfish pobierze się automatycznie przy pierwszej grze z komputerem (ok. 80 MB). Potem działa offline.");
        hint.setWrapText(true);
        getChildren().setAll(fields, hint);
    }
    GameOptions options() {
        return new GameOptions(opponent.getValue(), difficulty.getValue(),
                side.getValue().equals("Białymi") ? Side.WHITE : Side.BLACK);
    }
    static Optional<GameOptions> choose(Window owner, GameOptions initial) {
        GameOptionsView view = new GameOptionsView(initial);
        Dialog<GameOptions> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Nowa partia");
        dialog.setHeaderText("Wybierz przeciwnika i rozpocznij od początku.");
        dialog.getDialogPane().setContent(view);
        ButtonType play = new ButtonType("Graj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(play, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == play ? view.options() : null);
        return dialog.showAndWait();
    }
    private static VBox field(String label, Control control) { return new VBox(6, new Label(label), control); }
}
