package pl.projekt.szachy;

import java.awt.*;
import java.io.Serializable;

public abstract class Figure implements Serializable {
    protected boolean[][] possibleMoves = new boolean[Board.getN()][Board.getN()];
    protected Color color;
    protected int movesQuantity = 0;
    private String name;

    Figure(Color color, String name) {
        this.color = color;
        this.name = name;
    }

    public void setPossibleMoves(int coordinateX, int coordinateY) {
    }

    public boolean[][] getPossibleMoves() {
        return possibleMoves;
    }

    public String getName() {
        return name;
    }

    public javafx.scene.paint.Color getJavaFXColor() {
        if (color == Color.white) {
            return javafx.scene.paint.Color.WHITE;
        } else {
            return javafx.scene.paint.Color.BLACK;
        }
    }

    public Color getColor() {
        return color;
    }

    public String getColorToString() {
        if (color == Color.BLACK) {
            return "black";
        } else {
            return "white";
        }
    }

    public void incrementMovesQuantity() {
        movesQuantity++;
    }

    protected void resetPossibleMoves() {
        for (int i = 0; i < Board.getN(); i++) {
            for (int j = 0; j < Board.getN(); j++) {
                possibleMoves[i][j] = false;
            }
        }
    }

    protected boolean areCoordsInChessBoard(int row, int col) {
        return !(row < 0 || col < 0 || row > 7 || col > 7);
    }
}
