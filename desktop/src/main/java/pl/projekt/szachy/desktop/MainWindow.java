package pl.projekt.szachy.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.projekt.szachy.core.LocalGame;
import pl.projekt.szachy.core.api.GameApi;

import java.util.Objects;

/** Composition root: the frontend receives the engine through its public API. */
public class MainWindow extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        GameApi game = new LocalGame();
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("mainWindow.fxml")));
        loader.setControllerFactory(type -> {
            if (type == MainWindowController.class) {
                return new MainWindowController(game);
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
