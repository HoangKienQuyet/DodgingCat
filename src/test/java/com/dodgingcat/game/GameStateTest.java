package com.dodgingcat.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateTest {

    @Test
    void playingCanPauseAndPausedCanResumeTheSameRun() {
        assertTrue(GameState.PLAYING.canTransitionTo(GameState.PAUSED));
        assertTrue(GameState.PAUSED.canTransitionTo(GameState.PLAYING));
    }

    @Test
    void pausedRunCannotSkipDirectlyToGameOver() {
        assertFalse(GameState.PAUSED.canTransitionTo(GameState.GAME_OVER));
    }

    @Test
    void menuAndGameOverCanStartANewRun() {
        assertTrue(GameState.MENU.canTransitionTo(GameState.PLAYING));
        assertTrue(GameState.GAME_OVER.canTransitionTo(GameState.PLAYING));
    }
}
