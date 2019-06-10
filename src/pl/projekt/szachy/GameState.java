package pl.projekt.szachy;

import java.awt.*;
import java.io.*;

public class GameState implements Serializable {
    private static Figure[][] gameState = new Figure[Board.getN()][Board.getN()];
    private static Round round = new Round();

    GameState() {
        setFirstGameState();
    }

    GameState(String gameStateFileName) {
        String savedGameFileGS = "Saves/" + gameStateFileName + "_GS.ser";
        String savedGameFileR = "Saves/" + gameStateFileName + "_R.ser";
        try {
            FileInputStream fileInGS = new FileInputStream(savedGameFileGS);
            FileInputStream fileInR = new FileInputStream(savedGameFileR);
            ObjectInputStream inGS = new ObjectInputStream(fileInGS);
            ObjectInputStream inR = new ObjectInputStream(fileInR);
            gameState = (Figure[][]) inGS.readObject();
            round = (Round) inR.readObject();
            inGS.close();
            inR.close();
            fileInGS.close();
            fileInR.close();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("GameState class not found");
            c.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new GameState();
        for (int i = 0; i < Board.getN(); i++) {
            for (int j = 0; j < Board.getN() && GameState.getGameState()[i][j] != null; j++) {
                System.out.println(i + "i" + j + GameState.getGameState()[i][j].getName() + GameState.getGameState()[i][j].getColorToString());
            }
        }
    }

    public static Round getRound() {
        return round;
    }

    public static Figure[][] getGameState() {
        return gameState;
    }

    public void move(int sourceRow, int sourceColumn, int destinationRow, int destinationColumn) {
        capture(destinationRow, destinationColumn);
        gameState[sourceRow][sourceColumn].incrementMovesQuantity();
        gameState[destinationRow][destinationColumn] = gameState[sourceRow][sourceColumn];
        gameState[sourceRow][sourceColumn] = null;
        refreshAllPossibleMoves();
    }

    public void pawnPromotion(String newFigureName, int row, int col) {
        Figure figure = gameState[row][col];
        Color color = figure.getColor();
        gameState[row][col] = null;
        if (newFigureName.compareTo("pawn") == 0) {
            gameState[row][col] = new Pawn(color, row, col);
        } else if (newFigureName.compareTo("queen") == 0) {
            gameState[row][col] = new Queen(color, row, col);
        } else if (newFigureName.compareTo("bishop") == 0) {
            gameState[row][col] = new Bishop(color, row, col);
        } else if (newFigureName.compareTo("rook") == 0) {
            gameState[row][col] = new Rook(color, row, col);
        } else if (newFigureName.compareTo("king") == 0) {
            gameState[row][col] = new King(color, row, col);
        } else if (newFigureName.compareTo("knight") == 0) {
            gameState[row][col] = new Knight(color, row, col);
        } else {
            System.out.println("To ja nie wiem czego chcesz");
        }
    }

    public void saveGame(String nameOfSavedGame) {
        String gameStateFile = "Saves/" + nameOfSavedGame + "_gs.ser";
        String roundFile = "Saves/" + nameOfSavedGame + "_r.ser";
        try {
            FileOutputStream fileOutGS = new FileOutputStream(gameStateFile);
            FileOutputStream fileOutR = new FileOutputStream(roundFile);
            ObjectOutputStream outGS = new ObjectOutputStream(fileOutGS);
            ObjectOutputStream outR = new ObjectOutputStream(fileOutR);
            outGS.writeObject(gameState);
            outR.writeObject(round);
            outGS.close();
            outR.close();
            fileOutGS.close();
            fileOutR.close();
            System.out.println("Serialized data is saved in " + nameOfSavedGame);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void refreshAllPossibleMoves() {
        for (int i = 0; i < Board.getN(); i++) {
            for (int j = 0; j < Board.getN(); j++) {
                if (gameState[i][j] != null) {
                    gameState[i][j].setPossibleMoves(i, j);
                }
            }
        }
    }

    private void capture(int i, int j) {
        if (isCapture(i, j)) {
            gameState[i][j] = null;
        }
    }

    private boolean isCapture(int i, int j) {
        return gameState[i][j] != null;
    }

    private void setFirstGameState() {
        setPawns();
        setRooks();
        setKnights();
        setBishops();
        setKings();
        setQueens();
        for (int i = 2; i <= 5; i++) {
            for (int j = 0; j < Board.getN(); j++) {
                gameState[i][j] = null;
            }
        }
        refreshAllPossibleMoves();
    }

    private void setQueens() {
        gameState[0][4] = new Queen(Color.BLACK, 0, 4);
        gameState[7][4] = new Queen(Color.WHITE, 7, 4);
    }

    private void setKings() {
        gameState[0][3] = new King(Color.BLACK, 0, 3);
        gameState[7][3] = new King(Color.WHITE, 7, 3);
    }

    private void setBishops() {
        gameState[0][2] = new Bishop(Color.BLACK, 0, 2);
        gameState[0][5] = new Bishop(Color.BLACK, 0, 5);
        gameState[7][2] = new Bishop(Color.WHITE, 7, 2);
        gameState[7][5] = new Bishop(Color.WHITE, 7, 5);
    }

    private void setKnights() {
        gameState[0][1] = new Knight(Color.BLACK, 0, 1);
        gameState[0][6] = new Knight(Color.BLACK, 0, 6);
        gameState[7][1] = new Knight(Color.WHITE, 7, 1);
        gameState[7][6] = new Knight(Color.WHITE, 7, 6);
    }

    private void setRooks() {
        gameState[0][0] = new Rook(Color.BLACK, 0, 0);
        gameState[0][7] = new Rook(Color.BLACK, 0, 7);
        gameState[7][0] = new Rook(Color.WHITE, 7, 0);
        gameState[7][7] = new Rook(Color.WHITE, 7, 7);
    }

    private void setPawns() {
        for (int i = 0; i < Board.getN(); i++) {
            gameState[1][i] = new Pawn(Color.BLACK, 1, i);
        }
        for (int i = 0; i < Board.getN(); i++) {
            gameState[6][i] = new Pawn(Color.WHITE, 6, i);
        }
    }
}
