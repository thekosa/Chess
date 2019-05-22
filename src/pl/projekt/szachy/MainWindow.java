package pl.projekt.szachy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;


public class MainWindow extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("mainWindow.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("Chess");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        buildBoard();

        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    private void buildBoard() {
        Board board = new Board();
        GridPane grid = new GridPane();
        Button[][] button = new Button[board.getN()][board.getN()];

        grid.setHgap(20);
        grid.setVgap(20);
        for (int col = 0; col < board.getN(); col++) {
            for (int row = 0; row < board.getN(); row++) {
                button[col][row].setStyle("-fx-background-color: " + board.getTile(col, row).getTileColor().toString());
                button[col][row].setMaxSize(50, 50);
                grid.add(button[col][row], col, row, 1, 1);
            }
        }
    }
}
