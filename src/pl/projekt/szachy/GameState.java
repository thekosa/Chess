package pl.projekt.szachy;

import javafx.scene.Scene;

import java.awt.*;
import java.util.ArrayList;

public class GameState {
    private Board board;
    private ArrayList<Figure> gameState;
    private final Color whiteColor = Color.white;
    private final Color blackColor = Color.black;
    // private String gameStateFileName;

    //todo: skonfigurowac obsluge pliku csv,
// w pliku bedzie caly stan gry zapisany - czyli: cordx,cordy,kolor,figura
    //todo przed rozpoczeciem pracy sprawdzamy czy plik istnieje zeby rozpoczac gre tworzac nowy plik z nowym stanem, czy czytac stary

    GameState(boolean isNewGame) {
        if (isNewGame) {
            setFirstGameState();
        }
    }

    GameState(String gameStateFileName, boolean isNewGame) {
        //    this.gameStateFileName = gameStateFileName;
        if (isNewGame) {
            setFirstGameState();
        }
    }

    public ArrayList<Figure> getGameState() {
        return gameState;
    }

    public void saveGame(String nameOfSavedGame) {

        //robimy nowy plik
        for (Figure figure : gameState) {
            //zapisujemy kazda po kolei w pliku
        }
    }

    public Board getBoard(){
        return board;
    }

    private void setFirstGameState() {
        FiguresCollection whiteFiguresCollection = new FiguresCollection(whiteColor);
        FiguresCollection blackFiguresCollection = new FiguresCollection(blackColor);
        gameState = new ArrayList<>(transformCordinatesOfBlacks(blackFiguresCollection.getFiguresCollectionList()));
        gameState.addAll(whiteFiguresCollection.getFiguresCollectionList());
    }

    private ArrayList<Figure> transformCordinatesOfBlacks(ArrayList<Figure> blackFiguresList) {
        for (Figure figure : blackFiguresList) {
            figure.transformCordinates(board.getN());
        }
        return blackFiguresList;
    }
}
