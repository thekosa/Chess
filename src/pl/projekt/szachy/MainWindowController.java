package pl.projekt.szachy;

import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.net.URL;
import java.util.*;

public class MainWindowController implements Initializable {
    public Pane chessBoardPane;

    public Button newGameButton;
    public Button loadGameButton;
    public Button saveGameButton;
    public TextField saveGameNameText;
    public ImageView url;

    // public Button przycisk;

    private final int tileSize = 100;

//todo jezeli bedzie opcja new game - przekazujemy do gamestate zeby odczytywac z odpowiedniego pliku,
// jezeli wybierzemy co innego to z tego innego

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void newGameButtonAction() {
        System.out.println("nowa gra");/*
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
        }*/
        GameState gameState = new GameState();

        for (int i = 0; i < Board.getN(); i++) {
            for (int j = 0; j < Board.getN() && GameState.getGameState()[i][j] != null; j++) {

                ImageView imageView = new ImageView();
                imageView.setImage(getFigureImage(i, j));
                imageView.setFitHeight(tileSize);
                imageView.setFitWidth(tileSize);
                imageView.setLayoutX(i * tileSize);
                imageView.setLayoutY(j * tileSize);
                chessBoardPane.getChildren().add(imageView);
                System.out.println("piekne spierdolenie");
            }
        }
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

    private Image getFigureImage(int row, int column) {
        Figure figure = GameState.getGameState()[row][column];
        String figureColor = figure.getColorToString();
        String figureName = figure.getName();
        String string = "src/pl/projekt/szachy/assets/" + figureColor + "_" + figureName + ".png";
        return new Image(getClass().getResource(string).toExternalForm());
    }
/*
    private String getLabelStyle(int row, int column) {
        Figure figure = GameState.getGameState()[row][column];
        String figureColor = figure.getColor().toString();
        String figureName = figure.getName();

        return "-fx-background-color: transparent;" +
                " -fx-background-image: url(/pl/projekt/szachy/assets/" + figureColor + "_" + figureName + ".png);" +
                " -fx-background-repeat: stretch;" +
                " -fx-background-position: center center;" +
                " -fx-background-size: auto 100%;";
    }
*/
}