package com.dodgingcat.game;

/**
 * Denominations of Pate meta-progression currency.
 */
public enum PateType {
    CATNIP_PELLET("Catnip Pellet", 1, 18.0, 18.0),
    PATE_BAR("Pate Bar", 5, 24.0, 16.0),
    FISH("Golden Fish", 10, 26.0, 20.0),
    PREMIUM_BOX("Premium Pate Box", 50, 28.0, 26.0);

    private final String displayName;
    private final int value;
    private final double width;
    private final double height;

    PateType(String displayName, int value, double width, double height) {
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
