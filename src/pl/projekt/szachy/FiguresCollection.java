package pl.projekt.szachy;

import java.awt.*;
import java.util.ArrayList;

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
//promocja piona - czyli wywalenie z piona i zastąpienie go nowym z klasy PromotedPawn

    public ArrayList<int[]> getCordinates() {
        ArrayList<int[]> figuresCordinatesList = new ArrayList<>();
        for (Pawn pawn : pawns) {
            figuresCordinatesList.add(cordinatesMerge(pawn));
        }
        for (Rook rook : rooks) {
            figuresCordinatesList.add(cordinatesMerge(rook));
        }
        for (Knight knight : knights) {
            figuresCordinatesList.add(cordinatesMerge(knight));
        }
        for (Bishop bishop : bishops) {
            figuresCordinatesList.add(cordinatesMerge(bishop));
        }
        figuresCordinatesList.add(cordinatesMerge(king));
        figuresCordinatesList.add(cordinatesMerge(queen));
        return figuresCordinatesList;
    }

    private int[] cordinatesMerge(Figure figure) {
        int[] tempCordinates = new int[2];
        tempCordinates[0] = figure.getXCordinate();
        tempCordinates[1] = figure.getYCordinate();
        return tempCordinates;
    }

    public Color getColour() {
        return color;
    }

    private void setQueen() {
        queen = new Queen(color, 5, 0);
    }

    private void setKing() {
        king = new King(color, 5, 0);
    }

    private void setBishops() {
        bishops = new Bishop[pair];
        bishops[0] = new Bishop(color, 2, 0);
        bishops[1] = new Bishop(color, 5, 0);
    }

    private void setKnights() {
        knights = new Knight[pair];
        knights[0] = new Knight(color, 1, 0);
        knights[1] = new Knight(color, 6, 0);
    }

    private void setRooks() {
        rooks = new Rook[pair];
        rooks[0] = new Rook(color, 0, 0);
        rooks[1] = new Rook(color, 7, 0);
    }

    private void setPawns() {
        pawns = new Pawn[pawnQuantity];
        for (int i = 0; i < pawnQuantity; i++) {
            pawns[i] = new Pawn(color, i, 1);
        }
    }
}