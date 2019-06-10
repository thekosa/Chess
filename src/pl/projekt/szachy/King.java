package pl.projekt.szachy;

import java.awt.*;

public class King extends Figure {
    King(Color color, int coordX, int coordY) {
        super(color, "king");
    }

    public void setPossibleMoves(int coordinateX, int coordinateY) {
        resetPossibleMoves();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i * i + j * j >= 1) {
                    if (areCoordsInChessBoard(coordinateX + i, coordinateY + j)) {
                        possibleMoves[coordinateX + i][coordinateY + j] = true;
                    }
                }
            }
        }

    }
}
