package pl.projekt.szachy.core.model;

public record CastlingRights(boolean whiteKingSide, boolean whiteQueenSide,
                             boolean blackKingSide, boolean blackQueenSide) {
    public static final CastlingRights ALL = new CastlingRights(true, true, true, true);
    public static final CastlingRights NONE = new CastlingRights(false, false, false, false);

    public boolean allows(Side side, boolean kingSide) {
        return side == Side.WHITE
                ? (kingSide ? whiteKingSide : whiteQueenSide)
                : (kingSide ? blackKingSide : blackQueenSide);
    }

    public CastlingRights withoutKing(Side side) {
        return side == Side.WHITE
                ? new CastlingRights(false, false, blackKingSide, blackQueenSide)
                : new CastlingRights(whiteKingSide, whiteQueenSide, false, false);
    }

    public CastlingRights withoutRook(Side side, Square square) {
        int homeRank = side == Side.WHITE ? 0 : 7;
        if (square.rank() != homeRank || (square.file() != 0 && square.file() != 7)) {
            return this;
        }
        boolean kingSide = square.file() == 7;
        return side == Side.WHITE
                ? new CastlingRights(whiteKingSide && !kingSide, whiteQueenSide && kingSide,
                        blackKingSide, blackQueenSide)
                : new CastlingRights(whiteKingSide, whiteQueenSide,
                        blackKingSide && !kingSide, blackQueenSide && kingSide);
    }
}
