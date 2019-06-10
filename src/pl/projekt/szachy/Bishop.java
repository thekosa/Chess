package pl.projekt.szachy;

import java.awt.*;

public class Bishop extends Figure {
    Bishop(Color color, int coordX, int coordY) {
        super(color, "bishop");
    }

    public void setPossibleMoves(int coordinateX, int coordinateY) {
        resetPossibleMoves();

        for (int i = 1; i <= Board.getN(); i++) {
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
        }
    }
}
