package pl.projekt.szachy.core.model;

/** Shared coordinates: file 0 = a, rank 0 = 1, irrespective of the player's side. */
public record Square(int file, int rank) {
    public static final int BOARD_SIZE = 8;

    public Square {
        if (file < 0 || file >= BOARD_SIZE || rank < 0 || rank >= BOARD_SIZE) {
            throw new IllegalArgumentException("Square must be within a1-h8");
        }
    }

    @Override
    public String toString() {
        return String.valueOf((char) ('a' + file)) + (rank + 1);
    }
}
