package com.dodgingcat.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DifficultyScalingTest {

    @Test
    @DisplayName("Hazard caps match the living document design targets across time brackets")
    void testHazardCapProgression() {
        assertEquals(6, GenerationRules.getMaxHazardsForTime(0.0));
        assertEquals(6, GenerationRules.getMaxHazardsForTime(29.9));

        assertEquals(9, GenerationRules.getMaxHazardsForTime(30.0));
        assertEquals(9, GenerationRules.getMaxHazardsForTime(59.9));

        assertEquals(12, GenerationRules.getMaxHazardsForTime(60.0));
        assertEquals(12, GenerationRules.getMaxHazardsForTime(119.9));

        assertEquals(15, GenerationRules.getMaxHazardsForTime(120.0));
        assertEquals(15, GenerationRules.getMaxHazardsForTime(300.0));
    }

    @Test
    @DisplayName("Target spawn interval tightens progressively as survival time elapses")
    void testSpawnIntervalProgression() {
        double intervalTier1 = GenerationRules.getTargetSpawnInterval(15.0);
        double intervalTier2 = GenerationRules.getTargetSpawnInterval(45.0);
        double intervalTier3 = GenerationRules.getTargetSpawnInterval(90.0);
        double intervalTier4 = GenerationRules.getTargetSpawnInterval(150.0);

        assertTrue(intervalTier1 > intervalTier2);
        assertTrue(intervalTier2 > intervalTier3);
        assertTrue(intervalTier3 > intervalTier4);
    }

    @Test
    @DisplayName("Speed multiplier increases progressively over time")
    void testSpeedMultiplierProgression() {
        double speedTier1 = GenerationRules.getSpeedMultiplier(10.0);
        double speedTier2 = GenerationRules.getSpeedMultiplier(40.0);
        double speedTier3 = GenerationRules.getSpeedMultiplier(80.0);
        double speedTier4 = GenerationRules.getSpeedMultiplier(140.0);

        assertTrue(speedTier1 < speedTier2);
        assertTrue(speedTier2 < speedTier3);
        assertTrue(speedTier3 < speedTier4);
    }
}
