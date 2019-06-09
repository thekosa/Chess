package pl.projekt.szachy;

import javafx.scene.paint.Color;

public class GameState {
    private static Figure[][] gameState = new Figure[Board.getN()][Board.getN()];
    private static Round round = new Round();
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
    }

    private void setQueens() {
        gameState[0][4] = new Queen(Color.BLACK);
        gameState[7][4] = new Queen(Color.WHITE);
    }

    private void setKings() {
        gameState[0][3] = new King(Color.BLACK);
        gameState[7][3] = new King(Color.WHITE);
    }

    private void setBishops() {
        gameState[0][2] = new Bishop(Color.BLACK);
        gameState[0][5] = new Bishop(Color.BLACK);
        gameState[7][2] = new Bishop(Color.WHITE);
        gameState[7][5] = new Bishop(Color.WHITE);
    }

    private void setKnights() {
        gameState[0][1] = new Knight(Color.BLACK);
        gameState[0][6] = new Knight(Color.BLACK);
        gameState[7][1] = new Knight(Color.WHITE);
        gameState[7][6] = new Knight(Color.WHITE);
    }

    private void setRooks() {
        gameState[0][0] = new Rook(Color.BLACK);
        gameState[0][7] = new Rook(Color.BLACK);
        gameState[7][0] = new Rook(Color.WHITE);
        gameState[7][7] = new Rook(Color.WHITE);
    }

    private void setPawns() {
        for (int i = 0; i < Board.getN(); i++) {
            gameState[1][i] = new Pawn(Color.BLACK);
        }
        for (int i = 0; i < Board.getN(); i++) {
            gameState[6][i] = new Pawn(Color.WHITE);
        }
    }
}
