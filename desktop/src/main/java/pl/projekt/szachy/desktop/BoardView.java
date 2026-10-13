package pl.projekt.szachy.desktop;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import pl.projekt.szachy.core.model.GameState;
import pl.projekt.szachy.core.model.Piece;
import pl.projekt.szachy.core.model.Square;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Converts a backend snapshot into a board. It never modifies game data. */
final class BoardView extends GridPane {
    private static final int TILE_SIZE = 80;
    private final Map<Piece, Image> images = new HashMap<>();

    BoardView() {
        setAlignment(Pos.CENTER);
    }

    void render(GameState state) {
        getChildren().clear();
        for (int displayRow = 0; displayRow < Square.BOARD_SIZE; displayRow++) {
            int rank = Square.BOARD_SIZE - 1 - displayRow;
            add(coordinateLabel(Integer.toString(rank + 1)), 0, displayRow);
            for (int file = 0; file < Square.BOARD_SIZE; file++) {
                Square square = new Square(file, rank);
                StackPane tile = new StackPane();
                tile.setMinSize(TILE_SIZE, TILE_SIZE);
                tile.setPrefSize(TILE_SIZE, TILE_SIZE);
                tile.setMaxSize(TILE_SIZE, TILE_SIZE);
                tile.getStyleClass().add((file + rank) % 2 == 0 ? "dark-tile" : "light-tile");
                tile.setId("square-" + square);

                Piece piece = state.board().get(square);
                if (piece != null) {
                    ImageView icon = new ImageView(images.computeIfAbsent(piece, this::loadImage));
                    icon.setFitWidth(68);
                    icon.setFitHeight(68);
                    icon.setPreserveRatio(true);
                    tile.getChildren().add(icon);
                }
                add(tile, file + 1, displayRow);
            }
        }
        for (int file = 0; file < Square.BOARD_SIZE; file++) {
            add(coordinateLabel(String.valueOf((char) ('a' + file))), file + 1, Square.BOARD_SIZE);
        }
    }

    private Label coordinateLabel(String text) {
        Label label = new Label(text);
        label.setMinSize(28, 28);
        label.setMaxSize(Double.MAX_VALUE, 28);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().add("coordinate");
        return label;
    }

    private Image loadImage(Piece piece) {
        String filename = piece.side().name().toLowerCase(Locale.ROOT) + "_"
                + piece.type().name().toLowerCase(Locale.ROOT) + ".png";
        return new Image(Objects.requireNonNull(
                getClass().getResource("/pl/projekt/szachy/desktop/assets/" + filename),
                "Missing piece image: " + filename).toExternalForm());
    }
}
