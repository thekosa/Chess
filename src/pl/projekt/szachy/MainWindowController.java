package pl.projekt.szachy;

import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.io.*;
import java.net.URL;
import java.util.*;

public class MainWindowController implements Initializable {
    public Pane chessBoardPane;

    public Button newGameButton;

    public Button loadGameButton;
    public MenuButton loadGameMenuButton;

    public Button saveGameButton;
    public TextField saveGameNameText;
    public Label wrongSaveGameNameLabel;
    public Label successfullySavedGameLabel;

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
    private int[] moveCoordinates = new int[4];

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadMenuButton();
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
        successfullySavedGameLabel.setVisible(false);
    }

    public void newGameButtonAction() {
        System.out.println("nowa gra");
        gameState = new GameState();
        chessBoardRefresh();
        activateSystems();
    }

    public void loadGameButtonAction() {
        System.out.println("Wczytaj gre");
//tutaj powinien wziąć opcje z listy menuButton
        String savedGameFile = "super-gra";
        gameState = new GameState(savedGameFile);
        chessBoardRefresh();
        activateSystems();
    }

    public void saveGameButtonAction() {
        System.out.println("Zapisz gre");
        String nameOfSavedGame;
        if (saveGameNameText.getText().compareTo("") != 0) {
            nameOfSavedGame = saveGameNameText.getText();
        } else {
            wrongSavedGameNameMessage();
            return;
        }
        gameState.saveGame(nameOfSavedGame);
        successfullySavedGameMessage();
    }

    private boolean isMovePermitted() {
        if (GameState.getGameState()[moveCoordinates[0]][moveCoordinates[1]].getJavaFXColor()
                != GameState.getRound().getSideColor()) {
            return false;
        }
        if (GameState.getGameState()[moveCoordinates[2]][moveCoordinates[3]] != null) {
            if (GameState.getGameState()[moveCoordinates[2]][moveCoordinates[3]].getJavaFXColor()
                    == GameState.getRound().getSideColor()) {
                return false;
            }
        }
        return GameState.getGameState()[moveCoordinates[0]][moveCoordinates[1]]
                .getPossibleMoves()[moveCoordinates[2]][moveCoordinates[3]];
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
                ImageView imageView = new ImageView();
                imageView.setImage(getFigureImage(i, j));
                imageView.setFitHeight(tileSize);
                imageView.setFitWidth(tileSize);
                imageView.setLayoutX(j * tileSize);
                imageView.setLayoutY(i * tileSize);
                chessBoardPane.getChildren().add(imageView);
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
            return null;
        }
    }

    private void loadMenuButton() {
        File directory = new File("Saves/");
        try {
            File[] children = directory.listFiles();
            for (File child : children) {
                if (child.isFile()) {
                    String string = child.toString();
                    if (!string.contains("_r")) {
                        string = string.substring(6, string.length() - 7);
                        loadGameMenuButton.getItems().add(new MenuItem(string));
                    }
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void clearTextFields() {
        moveToRow.setText("");
        moveToColumn.setText("");
        moveFromColumn.setText("");
        moveFromRow.setText("");
    }

    private void successfullySavedGameMessage() {
        successfullySavedGameLabel.setVisible(true);
    }

    private void wrongSavedGameNameMessage() {
        wrongSaveGameNameLabel.setVisible(true);
        wrongSaveGameNameLabel.setText("");
    }

    private void wrongCoordinatesMessage() {
        wrongMoveCoordinatesLabel.setVisible(true);
        clearTextFields();
    }

    private void moveNotPermittedMessage() {
        moveNotPermittedLabel.setVisible(true);
        clearTextFields();
    }

    private void activateSystems() {
        sideInfoSquare.setVisible(true);
        moveToRow.setEditable(true);
        moveToColumn.setEditable(true);
        moveFromColumn.setEditable(true);
        moveFromRow.setEditable(true);
        makeMoveButton.setDisable(false);

        saveGameNameText.setDisable(false);
        saveGameButton.setDisable(false);
        successfullySavedGameLabel.setVisible(false);
    }
}