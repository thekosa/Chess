package pl.projekt.szachy;

public class Board {
    final private int N = 8; // number of tiles per row
    private Tile[][] tiles = new Tile[N][N];

    Board() {
        for (int col = 0; col < N; col++) {
            for (int row = 0; row < N; row++) {
                tiles[row][col] = new Tile(isEven(row) == isEven(col));
            }
        }
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public Tile getTile(int colValue, int rowValue) {
        return tiles[colValue][rowValue];
    }

    public int getN() {
        return N;
    }

    private boolean isEven(int number) {
        return (number & 1) == 0;
    }
}
