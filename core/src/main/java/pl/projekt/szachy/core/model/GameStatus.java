package pl.projekt.szachy.core.model;

public enum GameStatus {
    ACTIVE, CHECK, CHECKMATE, STALEMATE;

    public boolean isFinished() {
        return this == CHECKMATE || this == STALEMATE;
    }
}
