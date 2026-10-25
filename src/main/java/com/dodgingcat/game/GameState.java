package com.dodgingcat.game;

public enum GameState {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER;

    /**
     * Describes the navigation paths supported by the game coordinator.
     * Keeping this rule here makes state changes easy to reason about and test
     * without creating a JavaFX window.
     */
    public boolean canTransitionTo(GameState target) {
        return switch (this) {
            case MENU -> target == MENU || target == PLAYING;
            case PLAYING -> target == MENU || target == PAUSED || target == GAME_OVER;
            case PAUSED -> target == MENU || target == PLAYING;
            case GAME_OVER -> target == MENU || target == PLAYING;
        };
    }
}
