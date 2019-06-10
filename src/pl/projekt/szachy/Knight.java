package pl.projekt.szachy;

import javafx.scene.paint.Color;

public class Knight extends Figure {
    private boolean[][] possibleMoves = new boolean[Board.getN()][Board.getN()];

    Knight(Color color) {
        super(color, "knight");
        setPossibleMoves();
    }

    public void setPossibleMoves() {
        resetPossibleMoves();

    }

    public boolean[][] getPossibleMoves() {
        return possibleMoves;
    }

    private void resetPossibleMoves() {
        for (int i = 0; i < Board.getN(); i++) {
            for (int j = 0; j < Board.getN(); j++) {
                possibleMoves[i][j] = false;
            }
        }
    }
}
