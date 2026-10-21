package pl.projekt.szachy.core.model;

public enum GameStatus {
    ACTIVE, CHECK, CHECKMATE, STALEMATE,
    DRAW_REPETITION, DRAW_FIFTY_MOVES, DRAW_INSUFFICIENT_MATERIAL,
    DRAW_FIVEFOLD_REPETITION, DRAW_SEVENTY_FIVE_MOVES;

    public boolean isFinished() {
        return this != ACTIVE && this != CHECK;
    }
}
