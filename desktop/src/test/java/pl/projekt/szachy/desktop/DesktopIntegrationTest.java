package pl.projekt.szachy.desktop;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pl.projekt.szachy.core.LocalGame;
import pl.projekt.szachy.core.api.GameApi;
import pl.projekt.szachy.core.model.GameState;
import pl.projekt.szachy.core.model.Piece;
import pl.projekt.szachy.core.model.PieceType;
import pl.projekt.szachy.core.model.Side;
import pl.projekt.szachy.core.model.Square;
import pl.projekt.szachy.core.model.Move;
import pl.projekt.szachy.core.model.MoveError;
import pl.projekt.szachy.core.model.MoveResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Opt in on a machine with a graphical environment; no window is displayed. */
@EnabledIfSystemProperty(named = "chess.gui.tests", matches = "true")
class DesktopIntegrationTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        System.setProperty("javafx.cachedir",
                Path.of("target", "javafx-cache").toAbsolutePath().toString());
        CompletableFuture<Void> ready = new CompletableFuture<>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            ready.complete(null);
        });
        ready.get(10, TimeUnit.SECONDS);
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void loadsAllResourcesAndRendersOnlyTheStateReturnedByTheApi() throws Exception {
        FutureTask<Void> check = new FutureTask<>(() -> {
            StubGame api = new StubGame();
            FXMLLoader loader = new FXMLLoader(MainWindow.class.getResource("mainWindow.fxml"));
            loader.setControllerFactory(type -> new MainWindowController(api));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainWindow.class.getResource("chess.css").toExternalForm());
            root.applyCss();
            root.layout();

            assertEquals(64, countSquares(root));
            assertEquals(32, countImages(root));
            assertEquals("Ruch: białe", ((Label) root.lookup("#turnLabel")).getText());
            for (PieceType type : PieceType.values()) {
                for (Side side : Side.values()) {
                    String filename = side.name().toLowerCase(java.util.Locale.ROOT) + "_"
                            + type.name().toLowerCase(java.util.Locale.ROOT) + ".png";
                    assertNotNull(MainWindow.class.getResource("assets/" + filename));
                }
            }
            savePreview(root);

            Button reset = (Button) root.lookupAll(".button").stream()
                    .filter(node -> node instanceof Button button
                            && button.getText().equals("Nowa gra")).findFirst().orElseThrow();
            reset.fire();
            assertEquals(1, api.resets);
            assertEquals(64, countSquares(root));
            assertEquals(1, countImages(root));
            assertEquals(1, countImages(root.lookup("#square-e4")));
            assertEquals("Ruch: czarne", ((Label) root.lookup("#turnLabel")).getText());
            return null;
        });
        Platform.runLater(check);
        check.get(15, TimeUnit.SECONDS);
    }

    private static int countSquares(Parent root) {
        return root.lookupAll(".light-tile").size() + root.lookupAll(".dark-tile").size();
    }

    private static int countImages(Node node) {
        if (node instanceof ImageView view) {
            assertFalse(view.getImage().isError());
            return 1;
        }
        return node instanceof Parent parent
                ? parent.getChildrenUnmodifiable().stream().mapToInt(DesktopIntegrationTest::countImages).sum()
                : 0;
    }

    private static void savePreview(Parent root) throws Exception {
        WritableImage snapshot = root.snapshot(null, null);
        BufferedImage image = new BufferedImage((int) snapshot.getWidth(),
                (int) snapshot.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
            }
        }
        assertTrue(ImageIO.write(image, "png", Path.of("target", "board-preview.png").toFile()));
    }

    private static final class StubGame implements GameApi {
        private GameState state = new LocalGame().getState();
        private int resets;
        private List<Move> advertisedMoves = List.of();

        @Override
        public GameState getState() {
            return state;
        }

        @Override
        public List<Move> getLegalMoves(Square from) {
            return advertisedMoves;
        }

        @Override
        public MoveResult makeMove(Move move) {
            return new MoveResult(MoveError.ILLEGAL_MOVE, state);
        }

        @Override
        public GameState newGame() {
            resets++;
            // An intentionally distinct snapshot proves that rendering follows the API.
            state = new GameState(Map.of(new Square(4, 3),
                    new Piece(Side.BLACK, PieceType.BISHOP)), Side.BLACK);
            return state;
        }
    }
}
