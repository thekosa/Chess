package pl.projekt.szachy.core.api;

import pl.projekt.szachy.core.model.GameState;

/**
 * Entry point shared by all frontends.
 * Queries expose snapshots; commands change the state owned by the engine.
 */
public interface GameApi {
    GameState getState();

    GameState newGame();
}
