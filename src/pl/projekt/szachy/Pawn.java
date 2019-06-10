package pl.projekt.szachy;

import java.awt.*;

public class Pawn extends Figure {
    Pawn(Color color, int coordX, int coordY) {
        super(color, "pawn");
    }

    public void setPossibleMoves(int coordinateX, int coordinateY) {
        resetPossibleMoves();

        if (this.color == Color.BLACK) {
            possibleMoves[coordinateX + 1][coordinateY] = true;
            if (movesQuantity == 0) {
                if (GameState.getGameState()[coordinateX + 1][coordinateY] == null) {
                    possibleMoves[coordinateX + 2][coordinateY] = true;
                }
            }
            if (coordinateY != 0) {
                if (GameState.getGameState()[coordinateX + 1][coordinateY - 1] != null
                        && GameState.getGameState()[coordinateX + 1][coordinateY - 1].getColor() == Color.WHITE) {
                    possibleMoves[coordinateX + 1][coordinateY - 1] = true;
                }
            }
            if (coordinateY != 7) {
                if (GameState.getGameState()[coordinateX + 1][coordinateY + 1] != null
                        && GameState.getGameState()[coordinateX + 1][coordinateY + 1].getColor() == Color.WHITE) {
                    possibleMoves[coordinateX + 1][coordinateY + 1] = true;
                }
            }
        } else {
            possibleMoves[coordinateX - 1][coordinateY] = true;
            if (movesQuantity == 0) {
                if (GameState.getGameState()[coordinateX - 1][coordinateY] == null) {
                    possibleMoves[coordinateX - 2][coordinateY] = true;
                }
            }
            if (coordinateY != 0) {
                if (GameState.getGameState()[coordinateX - 1][coordinateY - 1] != null
                        && GameState.getGameState()[coordinateX - 1][coordinateY - 1].getColor() == Color.BLACK) {
                    possibleMoves[coordinateX - 1][coordinateY - 1] = true;
                }
            }
            if (coordinateY != 7) {
                if (GameState.getGameState()[coordinateX - 1][coordinateY + 1] != null
                        && GameState.getGameState()[coordinateX - 1][coordinateY + 1].getColor() == Color.BLACK) {
                    possibleMoves[coordinateX - 1][coordinateY + 1] = true;
                }
            }
        }
    }
}
