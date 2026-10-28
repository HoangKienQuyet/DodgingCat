package com.dodgingcat.game;

/**
 * Fish currency denominations:
 * - Single Fish = 1
 * - Canned Fish = 5
 */
public enum FishType {
    SINGLE_FISH("Fresh Fish", 1, 52.0, 42.0),
    FISH_CAN("Canned Fish", 5, 48.0, 42.0);

    private final String displayName;
    private final int value;
    private final double width;
    private final double height;

    FishType(String displayName, int value, double width, double height) {
        this.displayName = displayName;
        this.value = value;
        this.width = width;
        this.height = height;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getValue() {
        return value;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}
