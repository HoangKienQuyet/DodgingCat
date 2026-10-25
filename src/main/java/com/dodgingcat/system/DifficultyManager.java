package com.dodgingcat.system;

/** Converts survival time into predictable difficulty parameters. */
public final class DifficultyManager {

    private static final double SECONDS_PER_LEVEL = 25;
    private static final double INITIAL_OBSTACLE_INTERVAL = 2.6;
    private static final double MIN_OBSTACLE_INTERVAL = 0.9;

    public int levelAt(double elapsedSeconds) {
        requireNonNegative(elapsedSeconds);
        return 1 + (int) (elapsedSeconds / SECONDS_PER_LEVEL);
    }

    public double obstacleSpeedMultiplier(double elapsedSeconds) {
        int level = levelAt(elapsedSeconds);
        return 1 + Math.min(0.85, (level - 1) * 0.10);
    }

    public double obstacleSpawnInterval(double elapsedSeconds) {
        int level = levelAt(elapsedSeconds);
        return Math.max(MIN_OBSTACLE_INTERVAL,
                INITIAL_OBSTACLE_INTERVAL - (level - 1) * 0.25);
    }

    private void requireNonNegative(double elapsedSeconds) {
        if (elapsedSeconds < 0) {
            throw new IllegalArgumentException("Elapsed time cannot be negative");
        }
    }
}
