package com.dodgingcat.generation;

/**
 * Encapsulates the quantitative constraints and thresholds for the Fairness Validator.
 */
public class GenerationRules {
    // Fixed reaction window shared by telegraphed laser and cannon hazards.
    public static final double HAZARD_WARNING_DURATION_SECONDS = 1.0;
    // Align the crumble warning interval with the 1.5s crumble sound.
    public static final double PLATFORM_CRUMBLE_WARNING_SECONDS = 1.5;

    // Minimum seconds required between consecutive hazard spawns
    public static final double MIN_SPAWN_DELAY_BASE = 0.35;

    // Minimum physical distance (pixels) between player and a newly spawned hazard
    public static final double SAFE_ZONE_PADDING_X = 125.0;
    public static final double SAFE_ZONE_PADDING_Y = 110.0;

    // Human reaction time floor in seconds (candidate cannot hit player faster than this)
    public static final double MIN_REACTION_TIME_SECONDS = 0.40;

    // Maximum hazard limits by survival time bracket
    public static int getMaxHazardsForTime(double survivalSeconds) {
        if (survivalSeconds < 30.0) {
            return 6;
        } else if (survivalSeconds < 60.0) {
            return 9;
        } else if (survivalSeconds < 120.0) {
            return 12;
        } else {
            return 15;
        }
    }

    // Spawn interval targets by survival time bracket
    public static double getTargetSpawnInterval(double survivalSeconds) {
        if (survivalSeconds < 30.0) {
            return 1.9;
        } else if (survivalSeconds < 60.0) {
            return 1.5;
        } else if (survivalSeconds < 120.0) {
            return 1.15;
        } else {
            return 0.75;
        }
    }

    // Hazard speed multiplier as time advances
    public static double getSpeedMultiplier(double survivalSeconds) {
        if (survivalSeconds < 30.0) return 1.10;
        if (survivalSeconds < 60.0) return 1.20;
        if (survivalSeconds < 120.0) return 1.40;
        return 1.55;
    }
}
