package pl.projekt.szachy;

import java.awt.*;

public class GameState {
    private static Figure[][] gameState = new Figure[Board.getN()][Board.getN()];
    // private String gameStateFileName;

    //todo: skonfigurowac obsluge pliku csv,
// w pliku bedzie caly stan gry zapisany - czyli: cordx,cordy,kolor,figura
    //todo przed rozpoczeciem pracy sprawdzamy czy plik istnieje zeby rozpoczac gre tworzac nowy plik z nowym stanem, czy czytac stary

    GameState() {
        setFirstGameState();
    }

    GameState(String gameStateFileName) {
        //this.gameStateFileName = gameStateFileName;
        setFirstGameState();
    }

    public static Figure[][] getGameState() {
        return gameState;
    }

    public void move(int sourceRow, int sourceColumn, int destinationRow, int destinationColumn) {
        gameState[sourceRow][sourceColumn].incrementMovesQuantity();
        gameState[destinationRow][destinationColumn] = gameState[sourceRow][sourceColumn];
        gameState[sourceRow][sourceColumn] = null;
    }

    public void pawnPromotion(String newFigureName, int row, int col) {
        Figure figure = gameState[row][col];
        Color color = figure.getColor();
        gameState[row][col] = null;
        if (newFigureName.compareTo("pawn") == 0) {
            gameState[row][col] = new Pawn(color);
        } else if (newFigureName.compareTo("queen") == 0) {
            gameState[row][col] = new Queen(color);
        } else if (newFigureName.compareTo("bishop") == 0) {
            gameState[row][col] = new Bishop(color);
        } else if (newFigureName.compareTo("rook") == 0) {
            gameState[row][col] = new Rook(color);
        } else if (newFigureName.compareTo("king") == 0) {
            gameState[row][col] = new King(color);
        } else if (newFigureName.compareTo("knight") == 0) {
            gameState[row][col] = new Knight(color);
        } else {
            System.out.println("To ja nie wiem czego chcesz");
        }
    }

    public void saveGame(String nameOfSavedGame) {
    }

    private void setFirstGameState() {
        setPawns();
        setRooks();
        setKnights();
        setBishops();
        setKings();
        setQueens();
        for (int i = 0; i < Board.getN(); i++) {
            for (int j = 2; j <= 5; j++) {
                gameState[i][j] = null;
            }
        }
    }


    private void setQueens() {
        gameState[0][4] = new Queen(Color.black);
        gameState[7][4] = new Queen(Color.white);
    }

    private void setKings() {
        gameState[0][3] = new King(Color.black);
        gameState[7][3] = new King(Color.white);
    }

    private void setBishops() {
        gameState[0][2] = new Bishop(Color.black);
        gameState[0][5] = new Bishop(Color.black);
        gameState[7][2] = new Bishop(Color.white);
        gameState[7][5] = new Bishop(Color.white);
    }

    private void setKnights() {
        gameState[0][1] = new Knight(Color.black);
        gameState[0][6] = new Knight(Color.black);
        gameState[7][1] = new Knight(Color.white);
        gameState[7][6] = new Knight(Color.white);
    }

    private void setRooks() {
        gameState[0][0] = new Rook(Color.black);
        gameState[0][7] = new Rook(Color.black);
        gameState[7][0] = new Rook(Color.white);
        gameState[7][7] = new Rook(Color.white);
    }

    private void setPawns() {
        for (int i = 0; i < Board.getN(); i++) {
            gameState[1][i] = new Pawn(Color.black);
        }
        for (int i = 0; i < Board.getN(); i++) {
            gameState[6][i] = new Pawn(Color.white);
        }
    }
}
