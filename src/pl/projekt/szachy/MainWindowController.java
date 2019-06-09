package pl.projekt.szachy;

import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
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

    public Rectangle sideInfoSquare;
    public Button makeMoveButton;
    public TextField moveFromColumn;
    public TextField moveFromRow;
    public TextField moveToColumn;
    public TextField moveToRow;
    public Label wrongMoveCoordinatesLabel;
    public Label moveNotPermittedLabel;


    private final int tileSize = 100;

    private GameState gameState;
    private ImageView[][] imageGameState = new ImageView[Board.getN()][Board.getN()];
    private int[] moveCoordinates = new int[4];

//todo jezeli bedzie opcja new game - przekazujemy do gamestate zeby odczytywac z odpowiedniego pliku,
// jezeli wybierzemy co innego to z tego innego

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void makeMoveAction() throws NumberFormatException {
        if (!areCoordinatesCompatible()) {
            wrongCoordinatesMessage();
            return;
        }

        if (!isMovePermitted()) {
            moveNotPermittedMessage();
            return;
        }

        gameState.move(moveCoordinates[0], moveCoordinates[1], moveCoordinates[2], moveCoordinates[3]);

        wrongMoveCoordinatesLabel.setVisible(false);
        moveNotPermittedLabel.setVisible(false);
        GameState.getRound().changeSide();
        clearTextFields();
        chessBoardRefresh();
    }

    public void newGameButtonAction() {
        System.out.println("nowa gra");
        gameState = new GameState();
        chessBoardRefresh();
        sideInfoSquare.setVisible(true);
        moveToRow.setEditable(true);
        moveToColumn.setEditable(true);
        moveFromColumn.setEditable(true);
        moveFromRow.setEditable(true);
        makeMoveButton.setDisable(false);
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

    private boolean isMovePermitted() {
        if (GameState.getGameState()[moveCoordinates[0]][moveCoordinates[1]].getColor() != GameState.getRound().getSideColor()) {
            return false;
        }
        if (GameState.getGameState()[moveCoordinates[2]][moveCoordinates[3]] != null) {
            if (GameState.getGameState()[moveCoordinates[2]][moveCoordinates[3]].getColor() == GameState.getRound().getSideColor()) {
                return false;
            }
        }
        return true;
    }

    private boolean areCoordinatesCompatible() throws NumberFormatException {
        try {
            moveCoordinates[0] = Integer.parseInt(moveFromRow.getText());
            moveCoordinates[1] = Integer.parseInt(moveFromColumn.getText());
            moveCoordinates[2] = Integer.parseInt(moveToRow.getText());
            moveCoordinates[3] = Integer.parseInt(moveToColumn.getText());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }

        if (!(moveCoordinates[0] >= 0 && moveCoordinates[0] <= 7
                || moveCoordinates[1] >= 0 && moveCoordinates[1] <= 7
                || moveCoordinates[2] >= 0 && moveCoordinates[2] <= 7
                || moveCoordinates[3] >= 0 && moveCoordinates[3] <= 7)) {
            return false;
        } else if (moveCoordinates[0] == moveCoordinates[2] && moveCoordinates[1] == moveCoordinates[3]) {
            return false;
        }

        if (GameState.getGameState()[moveCoordinates[0]][moveCoordinates[1]] == null) {
            return false;
        }

        return true;
    }

    private void chessBoardRefresh() {
        chessBoardPane.getChildren().clear();
        for (int i = 0; i < Board.getN(); i++) {
            for (int j = 0; j < Board.getN(); j++) {
                if (GameState.getGameState()[i][j] == null) {
                    continue;
                }
                imageGameState[i][j] = new ImageView();
                imageGameState[i][j].setImage(getFigureImage(i, j));
                imageGameState[i][j].setFitHeight(tileSize);
                imageGameState[i][j].setFitWidth(tileSize);
                imageGameState[i][j].setLayoutX(j * tileSize);
                imageGameState[i][j].setLayoutY(i * tileSize);
                chessBoardPane.getChildren().add(imageGameState[i][j]);
            }
        }
        sideInfoSquare.setFill(GameState.getRound().getSideColor());
    }

    private Image getFigureImage(int row, int column) {
        Figure figure = GameState.getGameState()[row][column];
        String figureColor = figure.getColorToString();
        String figureName = figure.getName();
        String string = "src/pl/projekt/szachy/assets/" + figureColor + "_" + figureName + ".png";
        try {
            FileInputStream fileInputStream = new FileInputStream(string);
            return new Image(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null; //trzeba to zmienic
        }
    }

    private void clearTextFields() {
        moveToRow.setText("");
        moveToColumn.setText("");
        moveFromColumn.setText("");
        moveFromRow.setText("");
    }

    private void wrongCoordinatesMessage() {
        wrongMoveCoordinatesLabel.setVisible(true);
        clearTextFields();
    }

    private void moveNotPermittedMessage() {
        moveNotPermittedLabel.setVisible(true);
        clearTextFields();
    }
}