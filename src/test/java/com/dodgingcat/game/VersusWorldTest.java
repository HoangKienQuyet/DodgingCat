package com.dodgingcat.game;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.dodgingcat.game.GameWorld.*;
import static org.junit.jupiter.api.Assertions.*;

/** Regression tests for isolated timed versus, shop-enabled pickups and visible travel safety. */
class VersusWorldTest {
    private static final double STEP = FIXED_STEP;
    private static final Input RIGHT = new Input(false, true, false, false);
    private static final Input LEFT = new Input(true, false, false, false);
    private static final Input JUMP = new Input(false, false, true, false);

    private static GameWorld versus() {
        GameWorld world = new GameWorld(4242, Mode.VERSUS, Set.of());
        world.start();
        return world;
    }

    private static void run(GameWorld world, double seconds, Input first, Input second) {
        for (int i = 0; i < Math.round(seconds / STEP); i++) world.update(STEP, first, second);
    }

    private static Hazard hit(Player player) {
        return new Hazard(HazardKind.KNIFE, player.x() + 8, player.y() + 8, 14, 20, 0, 0, 0);
    }

    private static void protect(GameWorld world) {
        for (Player player : world.players()) world.applyBuff(player, BuffKind.INVINCIBILITY);
    }

    private static void fish(GameWorld world, Player player, int number) {
        for (int i = 0; i < number; i++) world.addPickup(new Pickup(player.x() + 16, player.y() + 18, null));
        world.update(STEP, Input.NONE, Input.NONE);
    }

    @Test
    void soloHasOneCatAndVersusHasTwoIndependentUnbuffedCats() {
        GameWorld solo = new GameWorld(1, Mode.SOLO, Set.of(Boost.SHIELD, Boost.INVINCIBILITY));
        solo.start();
        assertEquals(1, solo.players().size());
        assertFalse(solo.player().shieldActive(), "Unlocks must not grant starting effects");
        assertEquals(0, solo.player().invincibilityRemaining());
        GameWorld world = versus();
        assertEquals(2, world.players().size());
        assertEquals(1, world.player().id());
        assertEquals(2, world.players().get(1).id());
        assertThrows(UnsupportedOperationException.class, () -> world.players().clear());
        for (Player player : world.players()) {
            assertTrue(player.alive());
            assertFalse(player.shieldActive());
            assertEquals(0, player.invincibilityRemaining());
            assertEquals(0, player.doubleFishRemaining());
        }
    }

    @Test
    void twoInputStreamsMoveJumpAndDropIndependently() {
        GameWorld world = versus();
        double firstX = world.player().x();
        double secondX = world.players().get(1).x();
        run(world, 0.2, RIGHT, LEFT);
        assertTrue(world.player().x() > firstX);
        assertTrue(world.players().get(1).x() < secondX);
        world.update(STEP, JUMP, Input.NONE);
        assertTrue(world.player().vy() < 0);
        assertTrue(world.players().get(1).grounded());
        world.update(STEP, JUMP, JUMP);
        assertTrue(world.players().get(1).vy() < 0);

        world = versus();
        world.placePlayer(1, 210, 462, true);
        world.placePlayer(2, 700, 462, true);
        run(world, 0.1, new Input(false, false, false, true), Input.NONE);
        assertTrue(world.player().y() + Player.HEIGHT > 462);
        assertEquals(462, world.players().get(1).y() + Player.HEIGHT, 1e-6);
    }

    @Test
    void eliminatedCatStopsMovingAndCollectingWhileSurvivorContinues() {
        GameWorld world = versus();
        Player first = world.player();
        Player second = world.players().get(1);
        world.addHazardForCollision(hit(first));
        world.update(STEP, Input.NONE, Input.NONE);
        assertFalse(first.alive());
        assertTrue(second.alive());
        assertEquals(State.PLAYING, world.state());
        assertEquals(0, world.winnerId());
        double x = first.x();
        double elapsed = world.elapsed();
        world.addPickup(new Pickup(first.x() + 16, first.y() + 18, null));
        run(world, 0.2, RIGHT, RIGHT);
        assertEquals(x, first.x());
        assertEquals(0, first.fishCollected());
        assertTrue(world.elapsed() > elapsed);
        assertTrue(second.x() > 588);
    }

    @Test
    void finalWinnerUsesFishEvenWhenHigherScoringCatDiedFirst() {
        GameWorld world = versus();
        Player first = world.player();
        Player second = world.players().get(1);
        fish(world, first, 3);
        fish(world, second, 1);
        world.addHazardForCollision(hit(first));
        world.update(STEP, Input.NONE, Input.NONE);
        assertEquals(State.PLAYING, world.state());
        world.addHazardForCollision(hit(second));
        world.update(STEP, Input.NONE, Input.NONE);
        assertEquals(State.GAME_OVER, world.state());
        assertEquals(1, world.winnerId());
        assertEquals(4, world.fishCollected());
        assertTrue(world.elapsed() < VERSUS_DURATION_SECONDS);
    }

    @Test
    void bothCatsCanDieInSameStepAndEqualFishIsTie() {
        GameWorld world = versus();
        world.addHazardForCollision(new Hazard(HazardKind.LASER, 28, FLOOR_Y - 22,
                WIDTH - 56, 12, 0, 0, 0));
        world.update(STEP, Input.NONE, Input.NONE);
        assertTrue(world.players().stream().noneMatch(Player::alive));
        assertEquals(State.GAME_OVER, world.state());
        assertEquals(0, world.winnerId());
    }

    @Test
    void overlappingCatsClaimEachFishOnceAndNearestCatWinsContact() {
        GameWorld world = versus();
        world.placePlayer(1, 450, FLOOR_Y, true);
        world.placePlayer(2, 450, FLOOR_Y, true);
        fish(world, world.player(), 1);
        assertEquals(1, world.fishCollected());
        assertEquals(1, world.player().fishCollected());
        assertEquals(0, world.players().get(1).fishCollected());
        assertTrue(world.pickups().isEmpty());
        world.placePlayer(2, 470, FLOOR_Y, true);
        world.addPickup(new Pickup(487, FLOOR_Y - 18, null));
        world.update(STEP, Input.NONE, Input.NONE);
        assertEquals(1, world.players().get(1).fishCollected());
        assertEquals(2, world.fishCollected());
    }

    @Test
    void doubleFishAffectsOnlyCollectorAndNeverSurvivalScore() {
        GameWorld world = versus();
        protect(world);
        world.applyBuff(world.player(), BuffKind.DOUBLE_FISH);
        fish(world, world.player(), 1);
        fish(world, world.players().get(1), 1);
        assertEquals(2, world.player().fishCollected());
        assertEquals(1, world.players().get(1).fishCollected());
        assertEquals(3, world.fishCollected());
        run(world, 1, Input.NONE, Input.NONE);
        assertEquals(1, world.score());
        assertEquals(0, world.players().get(1).doubleFishRemaining());
    }

    @Test
    void shieldPersistsBeyondFiveSecondsAndProtectsOnlyItsCollector() {
        GameWorld world = versus();
        world.applyBuff(world.player(), BuffKind.SHIELD);
        protect(world);
        run(world, 4.8, Input.NONE, Input.NONE);
        protect(world);
        run(world, 1.3, Input.NONE, Input.NONE);
        assertTrue(world.player().shieldActive());
        assertFalse(world.players().get(1).shieldActive());

        world = versus();
        world.applyBuff(world.player(), BuffKind.SHIELD);
        world.addHazardForCollision(new Hazard(HazardKind.LASER, 28, FLOOR_Y - 22,
                WIDTH - 56, 12, 0, 0, 0));
        world.update(STEP, Input.NONE, Input.NONE);
        assertTrue(world.player().alive());
        assertFalse(world.player().shieldActive());
        assertFalse(world.players().get(1).alive(), "Shield must not remove a shared laser for the opponent");
    }

    @Test
    void shieldAndInvincibilityPickupsBelongToNearestCollectorAndBombIsShared() {
        GameWorld world = versus();
        Player second = world.players().get(1);
        world.addPickup(new Pickup(second.x() + 16, second.y() + 18, BuffKind.SHIELD));
        world.update(STEP, Input.NONE, Input.NONE);
        assertFalse(world.player().shieldActive());
        assertTrue(second.shieldActive());
        world.addPickup(new Pickup(second.x() + 16, second.y() + 18, BuffKind.INVINCIBILITY));
        world.update(STEP, Input.NONE, Input.NONE);
        assertEquals(0, world.player().invincibilityRemaining());
        assertEquals(BUFF_SECONDS, second.invincibilityRemaining());
        world.addHazardForCollision(new Hazard(HazardKind.KNIFE, 100, 6, 14, 34, 0, 250, 0));
        world.addPickup(new Pickup(second.x() + 16, second.y() + 18, BuffKind.BOMB));
        world.update(STEP, Input.NONE, Input.NONE);
        assertTrue(world.hazards().isEmpty());
    }

    @Test
    void soloPoolContainsOnlyPermanentlyUnlockedPickupsIncludingBomb() {
        GameWorld locked = new GameWorld(10);
        assertTrue(locked.randomBuffPool().isEmpty());
        for (Boost boost : Boost.values()) {
            GameWorld unlocked = new GameWorld(10, Mode.SOLO, Set.of(boost));
            assertEquals(Set.of(BuffKind.valueOf(boost.name())), new HashSet<>(unlocked.randomBuffPool()));
            assertThrows(UnsupportedOperationException.class, () -> unlocked.unlockedBoosts().clear());
        }
        locked.start();
        GameWorld bombUnlocked = new GameWorld(10, Mode.SOLO, Set.of(Boost.BOMB));
        bombUnlocked.start();
        boolean sawBomb = false;
        for (int frame = 0; frame < 120 * 75; frame++) {
            if (frame % 480 == 0) { protect(locked); protect(bombUnlocked); }
            locked.update(STEP, Input.NONE);
            bombUnlocked.update(STEP, Input.NONE);
            for (Pickup pickup : locked.pickups()) assertNull(pickup.kind(), "No solo buff is free before shop unlocks");
            for (Pickup pickup : bombUnlocked.pickups()) {
                if (pickup.kind() != null) {
                    assertEquals(BuffKind.BOMB, pickup.kind());
                    sawBomb = true;
                }
            }
        }
        assertEquals(State.PLAYING, locked.state(), "Empty pool must never call nextInt(0)");
        assertTrue(sawBomb);
    }

    @Test
    void versusEnablesEveryBoostWithEmptyShopUnlocksAndEndsAtSixtySeconds() {
        GameWorld world = versus();
        assertEquals(Set.of(BuffKind.values()), new HashSet<>(world.randomBuffPool()));
        assertEquals(VERSUS_DURATION_SECONDS, world.timeRemaining());
        for (int frame = 0; frame < 120 * 60; frame++) {
            if (frame % 480 == 0) protect(world);
            world.update(STEP, Input.NONE, Input.NONE);
        }
        assertEquals(State.GAME_OVER, world.state());
        assertTrue(world.players().stream().allMatch(Player::alive));
        assertEquals(60, world.elapsed(), 1e-8);
        assertEquals(60, world.score());
        assertEquals(0, world.timeRemaining());
        int comparison = Integer.compare(world.player().fishCollected(), world.players().get(1).fishCollected());
        assertEquals(comparison == 0 ? 0 : comparison > 0 ? 1 : 2, world.winnerId());
        world.update(0.25, RIGHT, LEFT);
        assertEquals(60, world.elapsed());
    }

    @Test
    void pauseFreezesVersusCountdownAndSoloHasNoSixtySecondLimit() {
        GameWorld world = versus();
        run(world, 1, Input.NONE, Input.NONE);
        world.togglePause();
        double remaining = world.timeRemaining();
        run(world, 3, RIGHT, JUMP);
        assertEquals(remaining, world.timeRemaining());
        world.togglePause();
        world.update(STEP, Input.NONE, Input.NONE);
        assertTrue(world.timeRemaining() < remaining);
        GameWorld solo = new GameWorld(5);
        solo.start();
        for (int frame = 0; frame < 120 * 61; frame++) {
            if (frame % 480 == 0) protect(solo);
            solo.update(STEP, Input.NONE);
        }
        assertEquals(State.PLAYING, solo.state());
        assertTrue(solo.elapsed() > 60);
    }

    @Test
    void ordinaryHazardsIgnoreAnyRequestedWarningAndMoveOnFirstTick() {
        Hazard knife = new Hazard(HazardKind.KNIFE, 200, 6, 14, 34, 0, 250, 99);
        Hazard dart = new Hazard(HazardKind.DART, -28, 450, 28, 10, 1, 300, 99);
        assertTrue(knife.active());
        assertTrue(dart.active());
        assertEquals(0, knife.warningRemaining());
        assertEquals(0, dart.warningRemaining());
        knife.update(0.1);
        dart.update(0.1);
        assertEquals(31, knife.y(), 1e-8);
        assertEquals(2, dart.x(), 1e-8);
        Hazard laser = new Hazard(HazardKind.LASER, 28, 550, 904, 12, 0, 0, 1.6);
        assertFalse(laser.active());
        laser.update(0.1);
        assertEquals(1.5, laser.warningRemaining(), 1e-8);
    }

    @Test
    void dartTravelSafetyChecksBothLivingCatsAndIgnoresEliminatedCats() {
        GameWorld world = versus();
        world.placePlayer(1, 400, FLOOR_Y, true);
        world.placePlayer(2, 30, FLOOR_Y, true);
        assertFalse(world.offerHazard(new Hazard(HazardKind.DART, -28, FLOOR_Y - 28,
                28, 10, 1, 300, 0)), "P2 is too close to the incoming left edge");
        assertTrue(world.offerHazard(new Hazard(HazardKind.DART, WIDTH, FLOOR_Y - 28,
                28, 10, -1, 300, 0)));
        world.applyBuff(BuffKind.BOMB);
        world.addHazardForCollision(hit(world.players().get(1)));
        world.update(STEP, Input.NONE, Input.NONE);
        world.applyBuff(BuffKind.BOMB);
        assertFalse(world.players().get(1).alive());
        assertTrue(world.offerHazard(new Hazard(HazardKind.DART, -28, FLOOR_Y - 28,
                28, 10, 1, 300, 0)));
    }

    @Test
    void knifeEscapeCheckAlsoProtectsSecondCatUsingActualFallTime() {
        GameWorld world = versus();
        world.placePlayer(1, 700, FLOOR_Y, true);
        world.placePlayer(2, 8, 350, false);
        assertTrue(world.offerHazard(new Hazard(HazardKind.KNIFE, 34, 6, 14, 34, 0, 250, 0)));
        assertTrue(world.offerHazard(new Hazard(HazardKind.KNIFE, 119, 6, 14, 34, 0, 250, 0)));
        assertFalse(world.offerHazard(new Hazard(HazardKind.KNIFE, 204, 6, 14, 34, 0, 250, 0)));
    }

    @Test
    void restartClearsBothScoresDeathsAndCollectedBuffs() {
        GameWorld world = versus();
        fish(world, world.players().get(1), 2);
        world.applyBuff(world.players().get(1), BuffKind.SHIELD);
        world.addHazardForCollision(hit(world.player()));
        world.update(STEP, Input.NONE, Input.NONE);
        world.start();
        assertEquals(VERSUS_DURATION_SECONDS, world.timeRemaining());
        assertEquals(State.PLAYING, world.state());
        assertEquals(0, world.fishCollected());
        assertEquals(0, world.winnerId());
        for (Player player : world.players()) {
            assertTrue(player.alive());
            assertFalse(player.shieldActive());
            assertEquals(0, player.fishCollected());
        }
    }
}
