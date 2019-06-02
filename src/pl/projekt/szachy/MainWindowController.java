package pl.projekt.szachy;

import com.sun.corba.se.impl.orbutil.graph.Node;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MainWindowController implements Initializable {
    public Rectangle tile_0_0;
    public Rectangle tile_1_0;
    public Rectangle tile_2_0;
    public Rectangle tile_3_0;
    public Rectangle tile_4_0;
    public Rectangle tile_5_0;
    public Rectangle tile_6_0;
    public Rectangle tile_7_0;
    public Rectangle tile_0_1;
    public Rectangle tile_1_1;
    public Rectangle tile_2_1;
    public Rectangle tile_3_1;
    public Rectangle tile_4_1;
    public Rectangle tile_5_1;
    public Rectangle tile_6_1;
    public Rectangle tile_7_1;
    public Rectangle tile_0_2;
    public Rectangle tile_1_2;
    public Rectangle tile_2_2;
    public Rectangle tile_3_2;
    public Rectangle tile_4_2;
    public Rectangle tile_5_2;
    public Rectangle tile_6_2;
    public Rectangle tile_7_2;
    public Rectangle tile_0_3;
    public Rectangle tile_1_3;
    public Rectangle tile_2_3;
    public Rectangle tile_3_3;
    public Rectangle tile_4_3;
    public Rectangle tile_5_3;
    public Rectangle tile_6_3;
    public Rectangle tile_7_3;
    public Rectangle tile_0_4;
    public Rectangle tile_1_4;
    public Rectangle tile_2_4;
    public Rectangle tile_3_4;
    public Rectangle tile_4_4;
    public Rectangle tile_5_4;
    public Rectangle tile_6_4;
    public Rectangle tile_7_4;
    public Rectangle tile_0_5;
    public Rectangle tile_1_5;
    public Rectangle tile_2_5;
    public Rectangle tile_3_5;
    public Rectangle tile_4_5;
    public Rectangle tile_5_5;
    public Rectangle tile_6_5;
    public Rectangle tile_7_5;
    public Rectangle tile_0_6;
    public Rectangle tile_1_6;
    public Rectangle tile_2_6;
    public Rectangle tile_3_6;
    public Rectangle tile_4_6;
    public Rectangle tile_5_6;
    public Rectangle tile_6_6;
    public Rectangle tile_7_6;
    public Rectangle tile_0_7;
    public Rectangle tile_1_7;
    public Rectangle tile_2_7;
    public Rectangle tile_3_7;
    public Rectangle tile_4_7;
    public Rectangle tile_5_7;
    public Rectangle tile_6_7;
    public Rectangle tile_7_7;

    public TextField saveGameNameText;

    public Button newGameButton;
    public Button loadGameButton;
    public Button saveGameButton;
    public GridPane chessBoardGridPane;


    public Button przycisk;

    private GameState gameState;

//todo jezeli bedzie opcja new game - przekazujemy do gamestate zeby odczytywac z odpowiedniego pliku,
// jezeli wybierzemy co innego to z tego innego

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void newGameButtonAction() {
        System.out.println("nowa gra");
        for (int i = 0; i < 8; i++) {
            Button button = new Button();
            button.setStyle("-fx-background-color: transparent;" +
                    " -fx-background-image: url(/pl/projekt/szachy/assets/black_pawn.png);" +
                    " -fx-background-repeat: stretch;" +
                    " -fx-background-position: center center;" +
                    " -fx-background-size: auto 100%;");

            button.setOnAction(figureButtonAction);
            button.setPrefSize(100, 100);
            button.setId("vutton" + i);
            System.out.println(button.idProperty());
            chessBoardGridPane.add(button, i, 0);

        }
        //    gameState = new GameState(true);
//        for (Figure figure : gameState.getGameState()) {
        //         Button button = new Button();
        //        button.setStyle(getButtonStyle(figure));
        //       System.out.println(button.idProperty());
        //  button.setPrefSize(100,100);
        // button.setId(getButtonId());

//            chessBoardGridPane.add(button, figure.getXCordinate(), figure.getYCordinate());
        //      }
    }

    public void loadGameButtonAction() {
        System.out.println("Wczytaj gre");
        //okno sie wyswietla z mozliwoscia wyboru poprzedniej gry
    }

    public void saveGameButtonAction() {
        System.out.println("Zapisz gre");
        String nameOfSavedGame = saveGameNameText.getText();
        //spisanie stanu gry do pliku
    }

    public void przyciskAkcja() {
        System.out.println("chujow sto");
        chessBoardGridPane.add(przycisk, 3,3);
        przycisk.setTranslateX(tile_6_6.getX());
    }

    private String getButtonId(Figure figure) {
        return figure.getColor().toString() + "_" + figure.getName() + "_" + figure.getXCordinate() + "_" + figure.getYCordinate();
    }

    private EventHandler<ActionEvent> figureButtonAction = event -> {
        System.out.println("chujow dwiescie");

    };

    private String getButtonStyle(Figure figure) {
        String figureColor = figure.getColor().toString();
        String figureName = figure.getName();

        return "-fx-background-color: transparent;" +
                " -fx-background-image: url(/pl/projekt/szachy/assets/" + figureColor + "_" + figureName + ".png);" +
                " -fx-background-repeat: stretch;" +
                " -fx-background-position: center center;" +
                " -fx-background-size: auto 100%;";

    }
}
