package pl.projekt.szachy;

import java.awt.*;

public class Knight extends Figure {
    Knight(Color color, int coordX, int coordY) {
        super(color, "knight");
    }

    public void setPossibleMoves(int coordinateX, int coordinateY) {
        resetPossibleMoves();
        //int[] coords = GameState.getCoordsOfFigure(this);

        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i * i + j * j >= 4) {
                    if (!(i * i == j * j || i * i == 4 && j == 0 || j * j == 4 && i == 0)) {
                        if (areCoordsInChessBoard(coordinateX + i, coordinateY + j)) {
                            possibleMoves[coordinateX + i][coordinateY + j] = true;
                        }
                    }
                }
            }
        }
        //tutaj jeszcze by mozna sprawdzac czy na danym miejscu jest inna figura tej samej strony
        //narazie nie robie tego, bo to juz jest w mainWindowController
    }
}
