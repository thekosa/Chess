package pl.projekt.szachy;

import java.awt.*;

public class FiguresCollection {
    private final int pawnQuantity = 8;
    private final int pair = 2;
    private Color color;

    private Pawn[] pawns;
    private Rook[] rooks;
    private Knight[] knights;
    private Bishop[] bishops;
    private King king;
    private Queen queen;

    FiguresCollection(Color color) {
        this.color = color;
        setPawns();
        setRooks();
        setKnights();
        setBishops();
        setKing();
        setQueen();
    }

    public Color getColour() {
        return color;
    }

    private void setQueen() {
        queen = new Queen(5, 0);
    }

    private void setKing() {
        king = new King(5, 0);
    }

    private void setBishops() {
        bishops = new Bishop[pair];
        bishops[0] = new Bishop(2, 0);
        bishops[1] = new Bishop(5, 0);
    }

    private void setKnights() {
        knights = new Knight[pair];
        knights[0] = new Knight(1, 0);
        knights[1] = new Knight(6, 0);
    }

    private void setRooks() {
        rooks = new Rook[pair];
        rooks[0] = new Rook(0, 0);
        rooks[1] = new Rook(7, 0);
    }

    private void setPawns() {
        pawns = new Pawn[pawnQuantity];
        for (int i = 0; i < pawnQuantity; i++) {
            pawns[i] = new Pawn(i, 1);
        }
    }
}