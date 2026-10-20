package pl.projekt.szachy.computer;

public enum Opponent {
    TWO_PLAYERS("Dwie osoby"), COMPUTER("Komputer — Stockfish");
    private final String label;
    Opponent(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
