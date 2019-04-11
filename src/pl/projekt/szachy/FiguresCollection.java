package pl.projekt.szachy;

public class FiguresCollection {
    private final int pawnQuantity = 8;
    private final int pair = 2;
    private boolean colour;

    private Pawn[] pawns;
    private Rook[] rooks;
    private Knight[] knights;
    private Bishop[] bishops;
    private King king;
    private Queen queen;

    FiguresCollection(boolean colour) {
        this.colour = colour;
        setPawns();
        setRooks();
        setKnights();
        setBishops();
        setKing();
        setQueen();
    }

    private void setQueen() {
        queen = new Queen(colour, 5, 0);
    }

    private void setKing() {
        king = new King(colour, 5, 0);
    }

    private void setBishops() {
        bishops = new Bishop[pair];
        bishops[0] = new Bishop(colour, 2, 0);
        bishops[1] = new Bishop(colour, 5, 0);
    }

    private void setKnights() {
        knights = new Knight[pair];
        knights[0] = new Knight(colour, 1, 0);
        knights[1] = new Knight(colour, 6, 0);
    }

    private void setRooks() {
        rooks = new Rook[pair];
        rooks[0] = new Rook(colour, 0, 0);
        rooks[1] = new Rook(colour, 7, 0);
    }

    private void setPawns() {
        pawns = new Pawn[pawnQuantity];
        for (int i = 0; i < pawnQuantity; i++) {
            pawns[i] = new Pawn(colour, i, 1);
        }
    }
}