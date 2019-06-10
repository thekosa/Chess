package pl.projekt.szachy;

import java.awt.*;

public class Queen extends Figure {
    Queen(Color color, int coordX, int coordY) {
        super(color, "queen");
    }

    public void setPossibleMoves(int coordinateX, int coordinateY) {
        resetPossibleMoves();

        for (int i = 1; i <= Board.getN(); i++) {
            if (GameState.getGameState()[i - 1][coordinateY] == null) {
                possibleMoves[i - 1][coordinateY] = true;
            }
            if (GameState.getGameState()[coordinateX][i - 1] == null) {
                possibleMoves[coordinateX][i - 1] = true;
            }
            if (areCoordsInChessBoard(coordinateX + i, coordinateY + i)) {
                if (GameState.getGameState()[coordinateX + 1][coordinateY + 1] == null) {
                    possibleMoves[coordinateX + 1][coordinateY + 1] = true;
                }
            }
            if (areCoordsInChessBoard(coordinateX - i, coordinateY + i)) {
                if (GameState.getGameState()[coordinateX - 1][coordinateY + 1] == null) {
                    possibleMoves[coordinateX - i][coordinateY + i] = true;
                }
            }
            if (areCoordsInChessBoard(coordinateX - i, coordinateY - i)) {
                if (GameState.getGameState()[coordinateX - 1][coordinateY - 1] == null) {
                    possibleMoves[coordinateX - i][coordinateY - i] = true;
                }
            }
            if (areCoordsInChessBoard(coordinateX + i, coordinateY - i)) {
                if (GameState.getGameState()[coordinateX + 1][coordinateY - 1] == null) {
                    possibleMoves[coordinateX + i][coordinateY - i] = true;
                }
            }
            possibleMoves[coordinateX][coordinateY] = false;
        }
    }
}
