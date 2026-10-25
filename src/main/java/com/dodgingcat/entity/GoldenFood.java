package com.dodgingcat.entity;

public final class GoldenFood extends Food {

    public static final int SCORE_VALUE = 50;
    public static final double SIZE = 26;
    public static final double DEFAULT_LIFETIME_SECONDS = 7;

    private double remainingLifetimeSeconds;

    public GoldenFood(double x, double y) {
        this(x, y, DEFAULT_LIFETIME_SECONDS);
    }

    public GoldenFood(double x, double y, double lifetimeSeconds) {
        super(x, y, SIZE);
        if (lifetimeSeconds <= 0) {
            throw new IllegalArgumentException("Golden food lifetime must be positive");
        }
        this.remainingLifetimeSeconds = lifetimeSeconds;
    }

    @Override
    public int getScoreValue() {
        return SCORE_VALUE;
    }

    /** Advances the temporary bonus item's lifetime independently of rendering. */
    public void updateLifetime(double deltaSeconds) {
        if (deltaSeconds < 0) {
            throw new IllegalArgumentException("Lifetime delta cannot be negative");
        }
        remainingLifetimeSeconds = Math.max(0, remainingLifetimeSeconds - deltaSeconds);
    }

    public boolean isExpired() {
        return remainingLifetimeSeconds == 0;
    }

    public double getRemainingLifetimeSeconds() {
        return remainingLifetimeSeconds;
    }
}
