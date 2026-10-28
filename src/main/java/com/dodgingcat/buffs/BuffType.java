package com.dodgingcat.buffs;

/**
 * Available buff types in Dodging Cat.
 */
public enum BuffType {
    HEART("Heart", "Revive a fallen teammate (Co-op only)", 0.0, 45.0),
    BOMB("Bomb", "Clears all hazards on screen", 0.0, 35.0),
    SHIELD("Shield", "Blocks 1 lethal hit", 5.0, 45.0),
    INVINCIBILITY("Invincibility", "Immunity to all hazards", 5.0, 35.0),
    DOUBLE_SCORE("x2 Score", "Doubles score rate", 5.0, 25.0);

    private final String displayName;
    private final String description;
    private final double defaultDuration;
    private final double cooldownSeconds;

    BuffType(String displayName, String description, double defaultDuration, double cooldownSeconds) {
        this.displayName = displayName;
        this.description = description;
        this.defaultDuration = defaultDuration;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getDefaultDuration() {
        return defaultDuration;
    }

    public double getCooldownSeconds() { return cooldownSeconds; }
}
