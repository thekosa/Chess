package pl.projekt.szachy.core.model;

public enum Side {
    WHITE, BLACK;

    public Side opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    public int pawnDirection() {
        return this == WHITE ? 1 : -1;
    }
}
