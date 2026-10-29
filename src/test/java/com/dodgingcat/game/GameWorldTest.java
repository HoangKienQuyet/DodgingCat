package com.dodgingcat.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.dodgingcat.game.GameWorld.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks physics, damage and the documented local spawn constraints. These tests deliberately
 * do not claim to prove that all possible hazard sequences are globally avoidable.
 */
class GameWorldTest {
    private static final double STEP = GameWorld.FIXED_STEP;
    private static final Input JUMP = new Input(false, false, true, false);

    private static GameWorld running() {
        GameWorld world = new GameWorld(2026);
        world.start();
        return world;
    }

    private static void run(GameWorld world, double seconds, Input input) {
        int steps = (int) Math.round(seconds / STEP);
        for (int i = 0; i < steps; i++) world.update(STEP, input);
    }

    private static Hazard stationaryHit(GameWorld world, double warning) {
        return new Hazard(HazardKind.KNIFE, world.player().x() + 8, world.player().y() + 8,
                14, 20, 0, 0, warning);
    }

    private static Hazard knife(double x, double warning) {
        return new Hazard(HazardKind.KNIFE, x, 6, 14, 34, 0, 250, warning);
    }

    @Test
    void initialStateAndPauseFreezeTimePhysicsAndTimers() {
        GameWorld world = new GameWorld(1);
        assertEquals(State.MENU, world.state());
        world.update(1, JUMP);
        assertEquals(0, world.elapsed());
        world.start();
        assertEquals(FLOOR_Y, world.player().y() + world.player().height());
        assertTrue(world.player().grounded());
        world.applyBuff(BuffKind.INVINCIBILITY);
        run(world, 1, Input.NONE);
        world.togglePause();
        double elapsed = world.elapsed();
        double protection = world.invincibilityRemaining();
        double y = world.player().y();
        run(world, 2, JUMP);
        assertEquals(State.PAUSED, world.state());
        assertEquals(elapsed, world.elapsed());
        assertEquals(protection, world.invincibilityRemaining());
        assertEquals(y, world.player().y());
        world.togglePause();
        world.update(STEP, Input.NONE);
        assertTrue(world.elapsed() > elapsed);
    }

    @Test
    void jumpPassesUpThroughPlatformThenLandsOnIt() {
        GameWorld world = running();
        world.placePlayer(210, FLOOR_Y, true);
        world.update(STEP, JUMP);
        run(world, 0.3, Input.NONE);
        assertTrue(world.player().y() + world.player().height() < 462,
                "Cat must pass through the first platform while moving upward");
        assertFalse(world.player().grounded());
        run(world, 0.5, Input.NONE);
        assertTrue(world.player().grounded());
        assertEquals(462, world.player().y() + world.player().height(), 1e-6);
    }

    @Test
    void middlePlatformIsReachableFromLowerPlatform() {
        GameWorld world = running();
        world.placePlayer(275, 462, true);
        Input rightJump = new Input(false, true, true, false);
        world.update(STEP, rightJump);
        run(world, 0.55, new Input(false, true, false, false));
        run(world, 0.15, Input.NONE);
        assertTrue(world.player().grounded());
        assertEquals(360, world.player().y() + world.player().height(), 1e-6);
    }

    @Test
    void dropPassesThroughSupportedPlatformAndFloorAlwaysCatchesPlayer() {
        GameWorld world = running();
        world.placePlayer(210, 462, true);
        Input drop = new Input(false, false, false, true);
        run(world, 0.1, drop);
        assertTrue(world.player().y() + world.player().height() > 462);
        assertFalse(world.player().grounded());
        run(world, 0.7, Input.NONE);
        assertTrue(world.player().grounded());
        assertEquals(FLOOR_Y, world.player().y() + world.player().height(), 1e-6);
        run(world, 0.2, drop);
        assertEquals(FLOOR_Y, world.player().y() + world.player().height(), 1e-6);
    }

    @Test
    void holdingJumpDoesNotRepeatAndHorizontalBoundsAreSolid() {
        GameWorld world = running();
        run(world, 1.3, JUMP);
        assertTrue(world.player().grounded());
        assertEquals(FLOOR_Y, world.player().y() + world.player().height(), 1e-6);
        world.update(STEP, Input.NONE);
        world.update(STEP, JUMP);
        assertTrue(world.player().vy() < 0);

        world = running();
        run(world, 2, new Input(false, true, false, false));
        assertEquals(WIDTH - 8 - world.player().width(), world.player().x(), 1e-6);
        assertEquals(0, world.player().vx());
        world = running();
        run(world, 2, new Input(true, false, false, false));
        assertEquals(8, world.player().x(), 1e-6);
    }

    @Test
    void warningIsHarmlessAndOneActiveHitEndsRound() {
        GameWorld world = running();
        world.addHazardForCollision(new Hazard(HazardKind.LASER, world.player().x() + 8,
                world.player().y() + 8, 14, 20, 0, 0, 1.1));
        run(world, 1, Input.NONE);
        assertEquals(State.PLAYING, world.state());
        run(world, 0.2, Input.NONE);
        assertEquals(State.GAME_OVER, world.state());
        double elapsed = world.elapsed();
        run(world, 2, Input.NONE);
        assertEquals(elapsed, world.elapsed());
    }

    @Test
    void shieldAbsorbsExactlyOneHitAndGivesShortContactGrace() {
        GameWorld world = running();
        world.applyBuff(BuffKind.SHIELD);
        world.addHazardForCollision(stationaryHit(world, 0));
        world.update(STEP, Input.NONE);
        assertEquals(State.PLAYING, world.state());
        assertFalse(world.player().shieldActive());
        assertEquals(1, world.hazards().size());
        world.addHazardForCollision(stationaryHit(world, 0));
        run(world, 0.5, Input.NONE);
        assertEquals(State.PLAYING, world.state());
        run(world, 0.5, Input.NONE);
        assertEquals(State.GAME_OVER, world.state());
    }

    @Test
    void invincibilityExpiresAfterFiveSimulationSeconds() {
        GameWorld world = running();
        world.applyBuff(BuffKind.INVINCIBILITY);
        world.addHazardForCollision(stationaryHit(world, 0));
        run(world, 4.9, Input.NONE);
        assertEquals(State.PLAYING, world.state());
        assertEquals(0.1, world.invincibilityRemaining(), 1e-6);
        run(world, 0.2, Input.NONE);
        assertEquals(State.GAME_OVER, world.state());
    }

    @Test
    void doubleFishBoostsOnlyFishForFiveSecondsAndScoreRemainsSurvivalTime() {
        GameWorld world = running();
        world.addPickup(new Pickup(world.player().x() + 16, FLOOR_Y - 18, null));
        world.applyBuff(BuffKind.DOUBLE_FISH);
        world.applyBuff(BuffKind.INVINCIBILITY);
        run(world, 1, Input.NONE);
        assertEquals(2, world.fishCollected());
        assertEquals(1, world.score());
        run(world, 3.9, Input.NONE);
        world.applyBuff(BuffKind.INVINCIBILITY);
        run(world, 0.2, Input.NONE);
        assertEquals(0, world.doubleFishRemaining());
        world.addPickup(new Pickup(world.player().x() + 16, FLOOR_Y - 18, null));
        world.update(STEP, Input.NONE);
        assertEquals(3, world.fishCollected());
        assertEquals(5, world.score());
        run(world, 1, Input.NONE);
        assertEquals(6, world.score());
    }

    @Test
    void bombClearsWarningsAndActiveObstaclesAndBuffPickupApplies() {
        GameWorld world = running();
        world.addHazardForCollision(knife(100, 1.1));
        world.addHazardForCollision(knife(400, 0));
        world.addPickup(new Pickup(world.player().x() + 16, FLOOR_Y - 18, BuffKind.BOMB));
        world.update(STEP, Input.NONE);
        assertTrue(world.hazards().isEmpty());
        assertTrue(world.pickups().isEmpty());
        assertEquals(0, world.fishCollected());
        world.addPickup(new Pickup(world.player().x() + 16, FLOOR_Y - 18, BuffKind.SHIELD));
        world.update(STEP, Input.NONE);
        assertTrue(world.player().shieldActive());
    }

    @Test
    void spawnDirectorRejectsUnwarnedDenseAndOverCapObstacles() {
        GameWorld world = running();
        assertFalse(world.offerHazard(new Hazard(HazardKind.LASER, 28, FLOOR_Y - 22,
                WIDTH - 56, 12, 0, 0, 0.2)));
        assertTrue(world.offerHazard(knife(100, 1.1)));
        assertFalse(world.offerHazard(knife(150, 1.1)));
        assertTrue(world.offerHazard(knife(350, 1.1)));
        assertTrue(world.offerHazard(knife(600, 1.1)));
        assertFalse(world.offerHazard(knife(850, 1.1)));
        assertEquals(3, world.acceptedSpawns());
        assertEquals(3, world.rejectedSpawns());
        assertEquals(3, world.hazards().size());
    }

    @Test
    void sweepingLaserAndDartCannotBeSpawnedTogether() {
        GameWorld world = running();
        assertTrue(world.offerHazard(new Hazard(HazardKind.DART, 8, FLOOR_Y - 28,
                28, 10, 1, 250, 1.1)));
        assertFalse(world.offerHazard(new Hazard(HazardKind.LASER, 28, FLOOR_Y - 22,
                WIDTH - 56, 12, 0, 0, 1.6)));
        world.applyBuff(BuffKind.BOMB);
        assertTrue(world.offerHazard(new Hazard(HazardKind.LASER, 28, FLOOR_Y - 22,
                WIDTH - 56, 12, 0, 0, 1.6)));
        assertFalse(world.offerHazard(new Hazard(HazardKind.DART, 8, FLOOR_Y - 28,
                28, 10, 1, 250, 1.1)));
    }

    @Test
    void laserWarnsWithoutDamageThenHitsAndExpiresAfterShortActiveWindow() {
        GameWorld world = running();
        Hazard beam = new Hazard(HazardKind.LASER, 28, FLOOR_Y - 22,
                WIDTH - 56, 12, 0, 0, 1.6);
        world.addHazardForCollision(beam);
        run(world, 1.5, Input.NONE);
        assertFalse(beam.active());
        assertEquals(State.PLAYING, world.state());
        run(world, 0.2, Input.NONE);
        assertTrue(beam.active());
        assertEquals(State.GAME_OVER, world.state());

        world = running();
        world.applyBuff(BuffKind.INVINCIBILITY);
        world.addHazardForCollision(new Hazard(HazardKind.LASER, 28, FLOOR_Y - 22,
                WIDTH - 56, 12, 0, 0, 1.6));
        run(world, 2.3, Input.NONE);
        assertTrue(world.hazards().isEmpty());
        assertEquals(State.PLAYING, world.state());
    }

    @Test
    void singleTimedJumpAvoidsGroundLaser() {
        GameWorld world = running();
        world.addHazardForCollision(new Hazard(HazardKind.LASER, 28, FLOOR_Y - 22,
                WIDTH - 56, 12, 0, 0, 1.6));
        run(world, 1.5, Input.NONE);
        world.update(STEP, JUMP);
        run(world, 0.95, Input.NONE);
        assertEquals(State.PLAYING, world.state());
        assertTrue(world.player().grounded());
        assertTrue(world.hazards().isEmpty());
    }

    @Test
    void knifeSpawnCannotSealPlayerIntoCornerDuringFallTravelBudget() {
        GameWorld world = running();
        world.placePlayer(8, 350, false);
        assertTrue(world.offerHazard(knife(34, 1.1)));
        assertTrue(world.offerHazard(knife(119, 1.1)));
        assertFalse(world.offerHazard(knife(204, 1.1)),
                "Third corridor leaves no sampled escape destination before the knife arrives");
        assertTrue(world.offerHazard(knife(600, 1.1)),
                "A distant corridor still leaves the player's nearby escape area clear");
    }

    @Test
    void sameSeedAndInputsProduceSameLongRunWithBoundedWarnedSpawns() {
        GameWorld first = new GameWorld(1947);
        GameWorld second = new GameWorld(1947);
        first.start();
        second.start();
        boolean sawDart = false;
        boolean sawPoison = false;
        for (int frame = 0; frame < 120 * 80; frame++) {
            if (frame % (120 * 4) == 0) {
                first.applyBuff(BuffKind.INVINCIBILITY);
                second.applyBuff(BuffKind.INVINCIBILITY);
            }
            Input input = new Input(frame % 600 < 240, frame % 600 >= 300 && frame % 600 < 540,
                    frame % 100 == 0, frame % 350 == 0);
            first.update(STEP, input);
            second.update(STEP, input);
            assertEquals(State.PLAYING, first.state());
            assertTrue(first.hazards().size() <= MAX_HAZARDS);
            for (Hazard hazard : first.hazards()) {
                if (hazard.age() < STEP / 2 && hazard.kind() == HazardKind.LASER) {
                    assertTrue(hazard.warningRemaining() >= MIN_WARNING_SECONDS);
                }
                if (hazard.kind() != HazardKind.LASER) {
                    assertEquals(0, hazard.warningRemaining());
                    assertTrue(hazard.active());
                }
                sawDart |= hazard.kind() == HazardKind.DART;
                assertNotEquals(HazardKind.LASER, hazard.kind(), "Themed maps never generate lasers");
                sawPoison |= hazard.kind() == HazardKind.POISON;
            }
        }
        assertTrue(sawDart, "Long run should exercise darts");
        assertTrue(sawPoison, "Garden long run should exercise drifting poison");
        assertEquals(first.player().x(), second.player().x());
        assertEquals(first.player().y(), second.player().y());
        assertEquals(first.elapsed(), second.elapsed());
        assertEquals(first.score(), second.score());
        assertEquals(first.acceptedSpawns(), second.acceptedSpawns());
        assertEquals(first.rejectedSpawns(), second.rejectedSpawns());
        assertEquals(snapshot(first), snapshot(second));
    }

    private static List<String> snapshot(GameWorld world) {
        List<String> result = new ArrayList<>();
        for (Hazard h : world.hazards()) {
            result.add(h.kind() + ":" + h.x() + ":" + h.y() + ":" + h.warningRemaining());
        }
        for (Pickup p : world.pickups()) result.add(p.kind() + ":" + p.x() + ":" + p.y());
        return result;
    }

    @Test
    void multipleSeedsRespectChangingCapSpawnIntervalAndKnifeCoverage() {
        for (long seed : new long[] { 7, 42, 9001 }) {
            GameWorld world = new GameWorld(seed, Mode.SOLO, java.util.Set.of(), MapTheme.KITCHEN);
            world.start();
            double previousSpawn = -100;
            for (int frame = 0; frame < 120 * 95; frame++) {
                if (frame % (120 * 4) == 0) world.applyBuff(BuffKind.INVINCIBILITY);
                int before = world.acceptedSpawns();
                world.update(STEP, Input.NONE);
                int cap = Math.min(MAX_HAZARDS, 3 + (int) (world.elapsed() / 30));
                assertTrue(world.hazards().size() <= cap);
                if (world.acceptedSpawns() > before) {
                    assertTrue(world.elapsed() - previousSpawn >= 0.72 - STEP,
                            "Accepted spawns must respect the minimum interval");
                    previousSpawn = world.elapsed();
                }
                // Each knife occupies its 14px body plus 35px padding on both sides. The
                // director also requires 85px spacing, so these 84px corridors cannot overlap.
                double covered = 0;
                for (Hazard hazard : world.hazards()) {
                    assertNotEquals(HazardKind.LASER, hazard.kind());
                    if (hazard.kind() == HazardKind.KNIFE) {
                        covered += Math.min(WIDTH, hazard.x() + hazard.width() + 35)
                                - Math.max(0, hazard.x() - 35);
                    }
                }
                assertTrue(covered <= WIDTH * 0.5 + 1e-6);
            }
            assertEquals(State.PLAYING, world.state());
            assertTrue(world.acceptedSpawns() > 25);
        }
    }

    @Test
    void restartResetsRunAndViewsCannotBeMutated() {
        GameWorld world = running();
        world.applyBuff(BuffKind.SHIELD);
        run(world, 1, Input.NONE);
        assertThrows(UnsupportedOperationException.class, () -> world.platforms().clear());
        assertThrows(UnsupportedOperationException.class, () -> world.hazards().clear());
        assertThrows(UnsupportedOperationException.class, () -> world.pickups().clear());
        world.start();
        assertEquals(State.PLAYING, world.state());
        assertEquals(0, world.elapsed());
        assertEquals(0, world.score());
        assertEquals(0, world.fishCollected());
        assertFalse(world.player().shieldActive());
        assertTrue(world.hazards().isEmpty());
        assertTrue(world.pickups().isEmpty());
    }

    @Test
    void invalidTimeDoesNotCorruptSimulationAndLongStallIsCapped() {
        GameWorld world = running();
        world.update(Double.NaN, Input.NONE);
        world.update(Double.POSITIVE_INFINITY, Input.NONE);
        world.update(-1, Input.NONE);
        assertEquals(0, world.elapsed());
        world.update(20, Input.NONE);
        assertEquals(0.25, world.elapsed(), 1e-6);
    }
}
