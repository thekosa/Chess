package pl.projekt.szachy;

import java.awt.*;

public class Rook extends Figure {
    Rook(Color color, int coordX, int coordY) {
        super(color, "rook");
    }

    public void setPossibleMoves(int coordinateX, int coordinateY) {
        resetPossibleMoves();

        for (int i = 0; i < Board.getN(); i++) {
            if (GameState.getGameState()[i][coordinateY] == null) {
                possibleMoves[i][coordinateY] = true;
            }
            if (GameState.getGameState()[coordinateX][i] == null) {
                possibleMoves[coordinateX][i] = true;
            }
        }
        possibleMoves[coordinateX][coordinateY] = false;
    }
}
