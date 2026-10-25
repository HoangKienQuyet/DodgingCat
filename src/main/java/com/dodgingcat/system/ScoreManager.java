package com.dodgingcat.system;

/** Tracks points earned by collecting food. */
public final class ScoreManager {

    private int score;

    public void addFoodScore(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Score points cannot be negative");
        }
        score += points;
    }

    public int getScore() {
        return score;
    }
}
