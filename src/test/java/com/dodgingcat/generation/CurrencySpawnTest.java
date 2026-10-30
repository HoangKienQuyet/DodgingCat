package com.dodgingcat.generation;

import com.dodgingcat.game.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CurrencySpawnTest {
    @Test void fourSecondCadenceFiveSecondLifetimeAndEightyFivePercentBoundary() {
        for (double roll : new double[]{0, .849999, .85, .999999}) {
            var random = new Random(1) { @Override public double nextDouble() { return roll; } };
            var generator = new ProceduralGenerator(random, new FairnessValidator());
            var fish = new ArrayList<FishItem>();
            var platforms = List.of(new Platform(0, 550, 960, 90, PlatformType.SOLID_FLOOR));
            generator.update(3.99, 0, 960, 640, List.of(), new ArrayList<>(), platforms, new ArrayList<>(), fish, false);
            assertTrue(fish.isEmpty());
            generator.update(.011, 0, 960, 640, List.of(), new ArrayList<>(), platforms, new ArrayList<>(), fish, false);
            assertEquals(1, fish.size());
            assertEquals(roll < .85 ? FishType.SINGLE_FISH : FishType.FISH_CAN, fish.getFirst().getType());
            assertEquals(5, fish.getFirst().getFullLifetime());
            fish.getFirst().update(4.99);
            assertFalse(fish.getFirst().isExpired());
            fish.getFirst().update(.011);
            assertTrue(fish.getFirst().isExpired());
            for (int i = 0; i < 10; i++) generator.update(4, 0, 960, 640, List.of(), new ArrayList<>(), platforms, new ArrayList<>(), fish, false);
            assertEquals(5, fish.size(), "The generator must never exceed five pickups");
        }
    }

    @Test void cooldownRunsWhileThePreviousFishIsStillVisible() {
        var generator = new ProceduralGenerator(1);
        var fish = new ArrayList<FishItem>();
        var platforms = List.of(new Platform(0, 550, 960, 90, PlatformType.SOLID_FLOOR));
        generator.update(4, 0, 960, 640, List.of(), new ArrayList<>(), platforms, new ArrayList<>(), fish, false);
        assertEquals(1, fish.size());
        fish.getFirst().update(4);
        assertFalse(fish.getFirst().isExpired());
        generator.update(4, 0, 960, 640, List.of(), new ArrayList<>(), platforms, new ArrayList<>(), fish, false);
        assertEquals(2, fish.size(), "Second spawn is at 8s even though the first has not expired");
    }

    @Test void anUnavailablePlatformDoesNotConsumeTheSpawnCooldown() {
        var generator = new ProceduralGenerator(1);
        var fish = new ArrayList<FishItem>();
        generator.update(4, 0, 960, 640, List.of(), new ArrayList<>(), List.of(), new ArrayList<>(), fish, false);
        assertTrue(fish.isEmpty());
        var platforms = List.of(new Platform(0, 550, 960, 90, PlatformType.SOLID_FLOOR));
        generator.update(.01, 0, 960, 640, List.of(), new ArrayList<>(), platforms, new ArrayList<>(), fish, false);
        assertEquals(1, fish.size());
    }
}
