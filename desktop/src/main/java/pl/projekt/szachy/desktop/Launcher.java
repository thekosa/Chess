package pl.projekt.szachy.desktop;

import javafx.application.Application;

/** Entry point shared by IntelliJ and the Maven JavaFX runner. */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(MainWindow.class, args);
    }
}
