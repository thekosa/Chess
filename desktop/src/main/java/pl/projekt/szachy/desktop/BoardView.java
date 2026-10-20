package pl.projekt.szachy.desktop;

import javafx.geometry.Pos;
import javafx.animation.ParallelTransition;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import pl.projekt.szachy.core.model.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Renders backend data and passes square clicks to the controller. */
final class BoardView extends StackPane {
    private static final int TILE_SIZE = 80;
    private final Map<Piece, Image> images = new HashMap<>();
    private final Consumer<Square> onSquareClick;
    private final GridPane grid = new GridPane();
    private final Pane animationLayer = new Pane();
    private final MoveAnimator animator = new MoveAnimator(animationLayer);
    private final Map<Square, Button> squares = new HashMap<>();
    private final Map<Square, ImageView> pieceViews = new HashMap<>();

    BoardView(Consumer<Square> onSquareClick) {
        this.onSquareClick = Objects.requireNonNull(onSquareClick);
        setId("chess-board");
        grid.setAlignment(Pos.CENTER);
        animationLayer.setMouseTransparent(true);
        animationLayer.setId("move-animation-layer");
        getChildren().setAll(grid, animationLayer);
    }

    void render(GameState state, Square selected, Set<Square> destinations) {
        animator.cancel();
        grid.getChildren().clear();
        squares.clear();
        pieceViews.clear();
        Move lastMove = state.moveHistory().isEmpty() ? null : state.moveHistory().getLast();
        for (int displayRow = 0; displayRow < Square.BOARD_SIZE; displayRow++) {
            int rank = Square.BOARD_SIZE - 1 - displayRow;
            grid.add(coordinateLabel(Integer.toString(rank + 1)), 0, displayRow);
            for (int file = 0; file < Square.BOARD_SIZE; file++) {
                Square square = new Square(file, rank);
                Piece piece = state.board().get(square);
                Button tile = new Button();
                tile.setMinSize(TILE_SIZE, TILE_SIZE);
                tile.setPrefSize(TILE_SIZE, TILE_SIZE);
                tile.setMaxSize(TILE_SIZE, TILE_SIZE);
                tile.getStyleClass().addAll("board-square", (file + rank) % 2 == 0 ? "dark-tile" : "light-tile");
                tile.setId("square-" + square);
                squares.put(square, tile);
                tile.setAccessibleText(square + (piece == null ? ", puste pole"
                        : ", " + (piece.side() == Side.WHITE ? "białe: " : "czarne: ") + pieceName(piece.type())));
                tile.setOnAction(event -> onSquareClick.accept(square));
                StackPane graphic = new StackPane();
                if (piece != null) {
                    ImageView icon = new ImageView(images.computeIfAbsent(piece, this::loadImage));
                    icon.setFitWidth(68);
                    icon.setFitHeight(68);
                    icon.setPreserveRatio(true);
                    icon.setId("piece-" + square);
                    pieceViews.put(square, icon);
                    graphic.getChildren().add(icon);
                }
                if (lastMove != null && (square.equals(lastMove.from()) || square.equals(lastMove.to()))) {
                    tile.getStyleClass().add("last-move");
                }
                if (destinations.contains(square)) {
                    tile.getStyleClass().add("legal-move");
                    if (piece == null) {
                        Circle marker = new Circle(9, Color.rgb(29, 57, 30, 0.6));
                        marker.setMouseTransparent(true);
                        graphic.getChildren().add(marker);
                    } else {
                        tile.getStyleClass().add("capture-move");
                    }
                }
                if (square.equals(selected)) {
                    tile.getStyleClass().add("selected-square");
                }
                if ((state.status() == GameStatus.CHECK || state.status() == GameStatus.CHECKMATE)
                        && piece != null && piece.type() == PieceType.KING && piece.side() == state.sideToMove()) {
                    tile.getStyleClass().add("checked-king");
                }
                tile.setGraphic(graphic);
                grid.add(tile, file + 1, displayRow);
            }
        }
        for (int file = 0; file < Square.BOARD_SIZE; file++) {
            grid.add(coordinateLabel(String.valueOf((char) ('a' + file))), file + 1, Square.BOARD_SIZE);
        }
    }

    boolean isAnimating() {
        return animator.isAnimating();
    }

    ParallelTransition animateMove(GameState before, GameState after, Move move, Runnable finished) {
        render(before, null, Set.of());
        applyCss();
        layout();
        List<MoveAnimator.Motion> motions = new ArrayList<>();
        motions.add(new MoveAnimator.Motion(pieceViews.get(move.from()), squares.get(move.to())));
        List<ImageView> captures = new ArrayList<>();
        Side movingSide = before.board().get(move.from()).side();
        for (var entry : before.board().entrySet()) {
            Square from = entry.getKey();
            Piece piece = entry.getValue();
            if (piece.equals(after.board().get(from)) || from.equals(move.from())) {
                continue;
            }
            if (piece.side() != movingSide) {
                captures.add(pieceViews.get(from));
                continue;
            }
            // Infer additional moving sprites from the snapshots (e.g. the rook in castling).
            // Rules and special-move validation stay in the engine.
            List<Square> destinations = after.board().entrySet().stream()
                    .filter(target -> target.getValue().equals(piece))
                    .filter(target -> !target.getKey().equals(move.to()))
                    .filter(target -> !piece.equals(before.board().get(target.getKey())))
                    .map(Map.Entry::getKey).toList();
            if (destinations.size() == 1) {
                motions.add(new MoveAnimator.Motion(pieceViews.get(from), squares.get(destinations.getFirst())));
            }
        }
        return animator.play(motions, captures, finished);
    }

    static String pieceName(PieceType type) {
        return switch (type) {
            case PAWN -> "pion";
            case KNIGHT -> "skoczek";
            case BISHOP -> "goniec";
            case ROOK -> "wieża";
            case QUEEN -> "hetman";
            case KING -> "król";
        };
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
