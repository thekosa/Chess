package pl.projekt.szachy;

import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.*;

public class MainWindowController implements Initializable {
    public Pane chessBoardPane;

    public Button newGameButton;
    public Button loadGameButton;
    public Button saveGameButton;
    public TextField saveGameNameText;

    public Button makeMoveButton;
    public TextField moveFromColumn;
    public TextField moveFromRow;
    public TextField moveToColumn;
    public TextField moveToRow;

    public Rectangle sideInfoSquere;

    private final int tileSize = 100;

    private GameState gameState;
//todo jezeli bedzie opcja new game - przekazujemy do gamestate zeby odczytywac z odpowiedniego pliku,
// jezeli wybierzemy co innego to z tego innego

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void makeMoveAction() {
        gameState.move(Integer.parseInt(moveFromRow.getText()),
                Integer.parseInt(moveFromColumn.getText()),
                Integer.parseInt(moveToRow.getText()),
                Integer.parseInt((moveToColumn.getText())));

        Round.changeSide();
    }

    public void newGameButtonAction() {
        System.out.println("nowa gra");
        gameState = new GameState();
        new Round();

        for (int i = 0; i < Board.getN(); i++) {
            for (int j = 0; j < Board.getN() && GameState.getGameState()[i][j] != null; j++) {

                ImageView imageView = new ImageView();
                imageView.setImage(getFigureImage(i, j));
                imageView.setFitHeight(tileSize);
                imageView.setFitWidth(tileSize);
                imageView.setLayoutX(j * tileSize);
                imageView.setLayoutY(i * tileSize);
                chessBoardPane.getChildren().add(imageView);
                System.out.println("piekne spierdolenie" + i + j);
            }
        }
        sideInfoSquere.setVisible(true);
        System.out.println("gowno ostatecnze");
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

    private void refreshChessBoard() {
        for (int i = 0; i < Board.getN(); i++) {
            for (int j = 0; j < Board.getN() && GameState.getGameState()[i][j] != null; j++) {
//nie wiem jak to przesuwac
            }
        }
        sideInfoSquere.setFill(Round.getSideColor());
    }

    private Image getFigureImage(int row, int column) {
        Figure figure = GameState.getGameState()[row][column];
        String figureColor = figure.getColorToString();
        String figureName = figure.getName();
        //F:\Michal\sprawy szkolne\Studia\WAT\IV\JTP\projekt\RepositoryClone\Chess\Chess\src\pl\projekt\szachy\assets\black_king.png
        String string = "src/pl/projekt/szachy/assets/" + figureColor + "_" + figureName + ".png";
        try {
            FileInputStream fileInputStream = new FileInputStream(string);
            return new Image(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null; //trzeba to zmienic
        }
    }
}