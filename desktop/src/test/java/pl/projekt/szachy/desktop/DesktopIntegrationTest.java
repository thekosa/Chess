package pl.projekt.szachy.desktop;

import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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
import pl.projekt.szachy.core.model.GameStatus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

/** Opt in on a machine with a graphical environment; no window is displayed. */
@EnabledIfSystemProperty(named = "chess.gui.tests", matches = "true")
class DesktopIntegrationTest {
    private static final ConcurrentLinkedQueue<Throwable> fxFailures = new ConcurrentLinkedQueue<>();
    @BeforeAll
    static void startJavaFx() throws Exception {
        assertTrue(Platform.class.getModule().isNamed(), "JavaFX must run on the module path");
        assertTrue(MainWindow.class.getModule().isNamed(), "The frontend must run as a Java module");
        System.setProperty("javafx.cachedir",
                Path.of("target", "javafx-cache").toAbsolutePath().toString());
        CompletableFuture<Void> ready = new CompletableFuture<>();
        Platform.startup(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> fxFailures.add(failure));
            Platform.setImplicitExit(false);
            ready.complete(null);
        });
        ready.get(10, TimeUnit.SECONDS);
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @AfterEach
    void verifyNoBackgroundAnimationFailure() {
        Throwable failure = fxFailures.poll();
        if (failure != null) {
            fail("Unexpected failure on the JavaFX thread", failure);
        }
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
            root.applyCss();
            root.layout();
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


    @Test
    void clickingPiecesHighlightsMovesCapturesAndResetsTheGame() throws Exception {
        onFxThread(() -> {
            LocalGame game = new LocalGame();
            Parent root = load(game);
            click(root, "e7");
            assertTrue(root.lookupAll(".legal-move").isEmpty());
            click(root, "e2");
            assertEquals(2, root.lookupAll(".legal-move").size());
            assertTrue(root.lookup("#square-e3").getStyleClass().contains("legal-move"));
            assertTrue(root.lookup("#square-e4").getStyleClass().contains("legal-move"));
            GameState before = game.getState();
            click(root, "e5");
            assertSame(before, game.getState());
            click(root, "e4");
            assertEquals(Side.BLACK, game.getState().sideToMove());
            assertEquals(0, countImages(root.lookup("#square-e2")));
            assertEquals(1, countImages(root.lookup("#square-e4")));
            click(root, "e4");
            assertTrue(root.lookupAll(".legal-move").isEmpty());
            click(root, "d7");
            assertTrue(root.lookup("#square-d5").getStyleClass().contains("legal-move"));
            click(root, "d5");
            click(root, "e4");
            assertTrue(root.lookup("#square-d5").getStyleClass().contains("capture-move"));
            savePreview(root, "legal-moves-preview.png");
            click(root, "d5");
            assertEquals(31, countImages(root));
            assertEquals(1, game.getState().capturedPieces().size());
            assertEquals(3, game.getState().moveHistory().size());
            assertTrue(((Label) root.lookup("#capturedLabel")).getText().contains("pion ×1"));
            assertEquals("Ruch: czarne", ((Label) root.lookup("#turnLabel")).getText());
            savePreview(root, "capture-preview.png");

            ((Button) root.lookup("#newGameButton")).fire();
            root.applyCss();
            root.layout();
            assertEquals(32, countImages(root));
            assertEquals(64, countSquares(root));
            assertEquals(Side.WHITE, game.getState().sideToMove());
            assertTrue(game.getState().moveHistory().isEmpty());
            assertTrue(game.getState().capturedPieces().isEmpty());
            assertTrue(root.lookupAll(".legal-move").isEmpty());
            assertTrue(root.lookupAll(".selected-square").isEmpty());
            assertEquals("Nowa partia. Zaczynają białe.", ((Label) root.lookup("#messageLabel")).getText());
        });
    }

    @Test
    void checkmateIsShownAndFurtherClicksDoNotChangeThePosition() throws Exception {
        onFxThread(() -> {
            LocalGame game = new LocalGame();
            Parent root = load(game);
            for (String[] move : new String[][]{{"f2", "f3"}, {"e7", "e5"}, {"g2", "g4"}, {"d8", "h4"}}) {
                click(root, move[0]);
                click(root, move[1]);
            }
            assertEquals(GameStatus.CHECKMATE, game.getState().status());
            assertEquals("Mat! Wygrywają czarne.", ((Label) root.lookup("#turnLabel")).getText());
            assertTrue(root.lookup("#square-e1").getStyleClass().contains("checked-king"));
            GameState finished = game.getState();
            click(root, "e1");
            click(root, "e2");
            assertSame(finished, game.getState());
        });
    }

    @Test
    void frontendDoesNotApplyAnAdvertisedMoveWhenTheBackendRejectsIt() throws Exception {
        onFxThread(() -> {
            StubGame game = new StubGame();
            game.advertisedMoves = List.of(new Move(new Square(4, 1), new Square(4, 4)));
            Parent root = load(game);
            click(root, "e2");
            click(root, "e5");
            assertEquals(1, countImages(root.lookup("#square-e2")));
            assertEquals(0, countImages(root.lookup("#square-e5")));
            assertEquals("Ten ruch jest niedozwolony.", ((Label) root.lookup("#messageLabel")).getText());
        });
    }


    @Test
    void moveHasIntermediateFramesAndRapidClicksDoNotAddAnotherMove() throws Exception {
        onFxThread(() -> {
            LocalGame game = new LocalGame();
            Parent root = load(game);
            click(root, "e2");
            ((Button) root.lookup("#square-e4")).fire();
            BoardView board = (BoardView) root.lookup("#chess-board");
            assertTrue(board.isAnimating());
            assertEquals(1, game.getState().moveHistory().size());
            assertFalse(root.lookup("#piece-e2").isVisible());
            assertNotNull(root.lookup("#moving-piece-e2"));
            ((Button) root.lookup("#square-d7")).fire();
            ((Button) root.lookup("#square-d5")).fire();
            assertEquals(1, game.getState().moveHistory().size());
            awaitAnimation(root);
            root.applyCss();
            root.layout();
            assertNull(root.lookup("#moving-piece-e2"));
            assertNotNull(root.lookup("#piece-e4"));
            assertNull(root.lookup("#piece-e2"));
            assertEquals("Ruch: czarne", ((Label) root.lookup("#turnLabel")).getText());

            GameState before = game.getState();
            Move move = new Move(new Square(3, 6), new Square(3, 4));
            GameState after = game.makeMove(move).state();
            Animation animation = board.animateMove(before, after, move,
                    () -> board.render(after, null, Set.of()));
            animation.pause();
            animation.jumpTo(Duration.millis(150));
            ImageView sprite = (ImageView) root.lookup("#moving-piece-d7");
            assertEquals(80, sprite.getTranslateY(), 1);
            assertEquals(0, sprite.getTranslateX(), 1);
            assertSame(after, game.getState());
            animation.play();
            awaitAnimation(root);
            root.applyCss();
            root.layout();
            assertNull(root.lookup("#moving-piece-d7"));
            assertNotNull(root.lookup("#piece-d5"));
        });
    }

    @Test
    void capturedSpriteFadesAndShrinksBeforeItIsRemoved() throws Exception {
        onFxThread(() -> {
            LocalGame game = new LocalGame();
            game.makeMove(new Move(new Square(4, 1), new Square(4, 3)));
            game.makeMove(new Move(new Square(3, 6), new Square(3, 4)));
            GameState before = game.getState();
            Parent root = load(game);
            BoardView board = (BoardView) root.lookup("#chess-board");
            Move move = new Move(new Square(4, 3), new Square(3, 4));
            GameState after = game.makeMove(move).state();
            Animation animation = board.animateMove(before, after, move,
                    () -> board.render(after, null, Set.of()));
            animation.pause();
            animation.jumpTo(Duration.millis(230));
            ImageView victim = (ImageView) root.lookup("#captured-piece-d5");
            assertNotNull(victim);
            assertTrue(victim.getOpacity() > 0 && victim.getOpacity() < 1);
            assertTrue(victim.getScaleX() < 1);
            assertFalse(root.lookup("#piece-d5").isVisible());
            assertNotNull(root.lookup("#moving-piece-e4"));
            savePreview(root, "capture-animation-preview.png");
            animation.play();
            awaitAnimation(root);
            root.applyCss();
            root.layout();
            assertEquals(31, countImages(root));
            assertTrue(((javafx.scene.layout.Pane) root.lookup("#move-animation-layer")).getChildren().isEmpty());
        });
    }

    @Test
    void resetDuringAnimationCancelsTheOldCompletionCallback() throws Exception {
        onFxThread(() -> {
            LocalGame game = new LocalGame();
            Parent root = load(game);
            click(root, "e2");
            ((Button) root.lookup("#square-e4")).fire();
            BoardView board = (BoardView) root.lookup("#chess-board");
            assertTrue(board.isAnimating());
            ((Button) root.lookup("#newGameButton")).fire();
            assertFalse(board.isAnimating());
            waitFx(Duration.millis(400));
            root.applyCss();
            root.layout();
            assertEquals(32, countImages(root));
            assertNotNull(root.lookup("#piece-e2"));
            assertNull(root.lookup("#piece-e4"));
            assertTrue(game.getState().moveHistory().isEmpty());
            assertEquals("Ruch: białe", ((Label) root.lookup("#turnLabel")).getText());
            assertTrue(((javafx.scene.layout.Pane) root.lookup("#move-animation-layer")).getChildren().isEmpty());
        });
    }

    @Test
    void snapshotDifferencesAnimateCastlingEnPassantAndPromotion() throws Exception {
        onFxThread(() -> {
            Piece king = new Piece(Side.WHITE, PieceType.KING);
            Piece rook = new Piece(Side.WHITE, PieceType.ROOK);
            Piece pawn = new Piece(Side.WHITE, PieceType.PAWN);
            Piece blackPawn = new Piece(Side.BLACK, PieceType.PAWN);
            Square e1 = new Square(4, 0), h1 = new Square(7, 0);
            Square g1 = new Square(6, 0), f1 = new Square(5, 0);
            GameState castleBefore = new GameState(Map.of(e1, king, h1, rook), Side.WHITE);
            GameState castleAfter = new GameState(Map.of(g1, king, f1, rook), Side.BLACK);
            BoardView board = new BoardView(square -> {});
            javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(board);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainWindow.class.getResource("chess.css").toExternalForm());
            board.render(castleBefore, null, Set.of());
            root.applyCss();
            root.layout();
            Animation castle = board.animateMove(castleBefore, castleAfter, new Move(e1, g1),
                    () -> board.render(castleAfter, null, Set.of()));
            castle.pause();
            castle.jumpTo(Duration.millis(150));
            assertEquals(80, root.lookup("#moving-piece-e1").getTranslateX(), 1);
            assertEquals(-80, root.lookup("#moving-piece-h1").getTranslateX(), 1);
            castle.play();
            awaitAnimation(root);
            root.applyCss();
            root.layout();
            assertNotNull(root.lookup("#piece-g1"));
            assertNotNull(root.lookup("#piece-f1"));

            Square e5 = new Square(4, 4), d5 = new Square(3, 4), d6 = new Square(3, 5);
            GameState epBefore = new GameState(Map.of(e5, pawn, d5, blackPawn), Side.WHITE);
            GameState epAfter = new GameState(Map.of(d6, pawn), Side.BLACK);
            Animation ep = board.animateMove(epBefore, epAfter, new Move(e5, d6),
                    () -> board.render(epAfter, null, Set.of()));
            assertNotNull(root.lookup("#captured-piece-d5"));
            ep.pause();
            ep.jumpTo(Duration.millis(150));
            assertEquals(-40, root.lookup("#moving-piece-e5").getTranslateX(), 1);
            assertEquals(-40, root.lookup("#moving-piece-e5").getTranslateY(), 1);
            ep.play();
            awaitAnimation(root);

            Square a7 = new Square(0, 6), a8 = new Square(0, 7);
            GameState promotionBefore = new GameState(Map.of(a7, pawn), Side.WHITE);
            GameState promotionAfter = new GameState(Map.of(a8, new Piece(Side.WHITE, PieceType.QUEEN)), Side.BLACK);
            board.animateMove(promotionBefore, promotionAfter, new Move(a7, a8, PieceType.QUEEN),
                    () -> board.render(promotionAfter, null, Set.of()));
            assertTrue(((ImageView) root.lookup("#moving-piece-a7")).getImage().getUrl().contains("white_pawn"));
            awaitAnimation(root);
            root.applyCss();
            root.layout();
            assertTrue(((ImageView) root.lookup("#piece-a8")).getImage().getUrl().contains("white_queen"));
        });
    }

    private static Parent load(GameApi game) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainWindow.class.getResource("mainWindow.fxml"));
        loader.setControllerFactory(type -> new MainWindowController(game));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(MainWindow.class.getResource("chess.css").toExternalForm());
        root.applyCss();
        root.layout();
        return root;
    }

    private static void click(Parent root, String square) {
        ((Button) root.lookup("#square-" + square)).fire();
        awaitAnimation(root);
        root.applyCss();
        root.layout();
    }

    private static void awaitAnimation(Parent root) {
        BoardView board = (BoardView) root.lookup("#chess-board");
        if (!board.isAnimating()) {
            return;
        }
        Object key = new Object();
        boolean[] exited = {false};
        Timeline completion = new Timeline();
        completion.getKeyFrames().add(new KeyFrame(Duration.millis(10), event -> {
            if (!board.isAnimating() && !exited[0]) {
                exited[0] = true;
                completion.stop();
                Platform.exitNestedEventLoop(key, null);
            }
        }));
        completion.setCycleCount(Animation.INDEFINITE);
        PauseTransition timeout = new PauseTransition(Duration.seconds(3));
        timeout.setOnFinished(event -> {
            if (!exited[0]) {
                exited[0] = true;
                completion.stop();
                Platform.exitNestedEventLoop(key, "timeout");
            }
        });
        completion.play();
        timeout.play();
        Object result = Platform.enterNestedEventLoop(key);
        completion.stop();
        timeout.stop();
        assertNull(result, "Move animation did not finish");
    }

    private static void waitFx(Duration duration) {
        Object key = new Object();
        PauseTransition wait = new PauseTransition(duration);
        wait.setOnFinished(event -> Platform.exitNestedEventLoop(key, null));
        wait.play();
        Platform.enterNestedEventLoop(key);
    }

    private static void onFxThread(FxCheck action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            action.run();
            return null;
        });
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface FxCheck {
        void run() throws Exception;
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
        savePreview(root, "board-preview.png");
    }

    private static void savePreview(Parent root, String filename) throws Exception {
        WritableImage snapshot = root.snapshot(null, null);
        BufferedImage image = new BufferedImage((int) snapshot.getWidth(),
                (int) snapshot.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
            }
        }
        assertTrue(ImageIO.write(image, "png", Path.of("target", filename).toFile()));
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
