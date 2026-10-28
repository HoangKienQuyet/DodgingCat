package com.dodgingcat.generation;

import com.dodgingcat.game.Platform;
import com.dodgingcat.game.PlatformType;
import com.dodgingcat.game.Player;
import com.dodgingcat.hazards.Hazard;
import com.dodgingcat.hazards.HorizontalHazard;
import com.dodgingcat.hazards.TopDownHazard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FairnessValidatorTest {
    private FairnessValidator validator;
    private FairnessMetrics metrics;
    private List<Player> players;
    private List<Hazard> hazards;
    private List<Platform> platforms;

    @BeforeEach
    void setUp() {
        metrics = new FairnessMetrics();
        validator = new FairnessValidator(metrics);
        players = new ArrayList<>();
        hazards = new ArrayList<>();
        platforms = new ArrayList<>();

        players.add(new Player(1, 400.0, 500.0, "ginger"));
        platforms.add(new Platform(0, 540, 960, 40, PlatformType.SOLID_FLOOR));
    }

    @Test
    @DisplayName("Rule 1: Rejects candidate if minimum spawn delay has not elapsed")
    void testMinimumSpawnDelayRule() {
        Hazard candidate = new TopDownHazard(100.0, -20.0, 32.0, 32.0, "knife", 200.0, 400.0);
        double tooFastDelay = 0.1; // Less than MIN_SPAWN_DELAY_BASE (0.35s)

        FairnessResult result = validator.validate(candidate, tooFastDelay, 0, 10.0, players, hazards, platforms);
        assertFalse(result.accepted());
        assertTrue(result.rejectionReason().contains("Minimum Spawn Delay"));
        assertEquals(1, metrics.getCandidatesRejected());
    }

    @Test
    @DisplayName("Rule 2: Rejects candidate when active hazard count reaches on-screen cap")
    void testOnScreenLimitRule() {
        Hazard candidate = new TopDownHazard(100.0, -20.0, 32.0, 32.0, "knife", 200.0, 400.0);
        double validDelay = 1.0;
        double earlySurvivalTime = 15.0; // Tier 1: max 3 hazards
        int currentActive = 6;

        FairnessResult result = validator.validate(candidate, validDelay, currentActive, earlySurvivalTime, players, hazards, platforms);
        assertFalse(result.accepted());
        assertTrue(result.rejectionReason().contains("On-screen Limit"));
    }

    @Test
    @DisplayName("Rule 3: Rejects candidate spawning directly inside player safe-zone bubble")
    void testSafeZonePaddingRule() {
        // Player is at x = 400, y = 500
        // Spawn top-down hazard right at x = 405, y = 460 (inside 90x80 buffer)
        Hazard unfairCandidate = new TopDownHazard(405.0, 460.0, 32.0, 32.0, "anvil", 200.0, 400.0);
        double validDelay = 1.0;

        FairnessResult result = validator.validate(unfairCandidate, validDelay, 0, 10.0, players, hazards, platforms);
        assertFalse(result.accepted());
        assertTrue(result.rejectionReason().contains("Safe-Zone"));
    }

    @Test
    @DisplayName("Rule 4: Rejects candidate with insufficient reaction time window")
    void testReactionWindowRule() {
        // Player is at y = 500. Hazard is at y = 380 (distance 88px > SAFE_ZONE_PADDING_Y 70px to pass Rule 3),
        // falling at vy = 600 px/s. timeToImpact = 88 / 600 = 0.146s < MIN_REACTION_TIME_SECONDS (0.28s)
        Hazard fastUnfairCandidate = new TopDownHazard(95.0, 380.0, 32.0, 32.0, "dart", 600.0, 0.0);
        // Put player in path
        players.getFirst().setPosition(95.0, 500.0);

        FairnessResult result = validator.validate(fastUnfairCandidate, 1.0, 0, 10.0, players, hazards, platforms);
        assertFalse(result.accepted());
        assertTrue(result.rejectionReason().contains("Reaction Time Window"));
    }

    @Test
    @DisplayName("Fair candidate satisfies all rules and increments acceptance metric")
    void testFairCandidateAccepted() {
        // Hazard spawns high above at x = 100 (far from player at x = 400)
        Hazard fairCandidate = new TopDownHazard(100.0, -30.0, 32.0, 32.0, "plant_pot", 150.0, 350.0);

        FairnessResult result = validator.validate(fairCandidate, 1.5, 1, 10.0, players, hazards, platforms);
        assertTrue(result.accepted());
        assertNull(result.rejectionReason());
        assertEquals(1, metrics.getCandidatesAccepted());
        assertEquals(0, metrics.getCandidatesRejected());
        assertEquals(1.0, metrics.getAcceptanceRate());
    }

    @Test
    @DisplayName("Deterministic seeded generator produces reproducible hazard sequence")
    void testSeededGenerationDeterminism() {
        ProceduralGenerator gen1 = new ProceduralGenerator(42L);
        ProceduralGenerator gen2 = new ProceduralGenerator(42L);

        List<Hazard> list1 = new ArrayList<>();
        List<Hazard> list2 = new ArrayList<>();

        gen1.update(1.0, 10.0, 960, 640, players, list1, platforms, new ArrayList<>(), new ArrayList<>(), false);
        gen2.update(1.0, 10.0, 960, 640, players, list2, platforms, new ArrayList<>(), new ArrayList<>(), false);

        assertEquals(list1.size(), list2.size());
        if (!list1.isEmpty()) {
            assertEquals(list1.getFirst().getType(), list2.getFirst().getType());
            assertEquals(list1.getFirst().getX(), list2.getFirst().getX(), 0.001);
            assertEquals(list1.getFirst().getY(), list2.getFirst().getY(), 0.001);
        }
    }
}
