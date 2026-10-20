package pl.projekt.szachy.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.projekt.szachy.computer.ComputerGame;
import pl.projekt.szachy.computer.GameOptions;
import pl.projekt.szachy.web.ChessWebServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/** Composition root: the frontend receives the engine through its public API. */
public class MainWindow extends Application {
    private ChessWebServer webServer;
    private ComputerGame desktopGame;
    private MainWindowController controller;

    @Override
    public void start(Stage stage) throws Exception {
        ModeSelectionView.show(stage, options -> {
            try {
                openDesktop(stage, options);
            } catch (Exception exception) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR,
                        "Nie udało się otworzyć planszy: " + exception.getMessage());
                alert.initOwner(stage);
                alert.showAndWait();
            }
        }, this::openWeb, getParameters().getRaw().contains("--console"),
                getParameters().getRaw().contains("--web"));
    }

    private String openWeb(GameOptions options) {
        try {
            if (webServer == null) {
                webServer = ChessWebServer.start(0);
            }
            String address = webServer.address() + "?launch=" + java.util.UUID.randomUUID()
                    + "&opponent=" + options.opponent().name() + "&difficulty=" + options.difficulty().name()
                    + "&humanSide=" + options.humanSide().name();
            getHostServices().showDocument(address);
            return address;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void stop() {
        if (controller != null) controller.close();
        if (desktopGame != null) desktopGame.close();
        if (webServer != null) {
            webServer.close();
        }
    }

    private void openDesktop(Stage stage, GameOptions options) throws Exception {
        if (desktopGame != null) desktopGame.close();
        if (controller != null) controller.close();
        desktopGame = new ComputerGame(options);
        controller = new MainWindowController(desktopGame);
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("mainWindow.fxml")));
        loader.setControllerFactory(type -> {
            if (type == MainWindowController.class) {
                return controller;
            }
            throw new IllegalArgumentException("Unknown controller: " + type.getName());
        });
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("chess.css")).toExternalForm());
        stage.setTitle("Szachy");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.centerOnScreen();
        stage.show();
    }
}
