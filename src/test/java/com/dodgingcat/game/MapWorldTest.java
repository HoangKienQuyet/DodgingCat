package com.dodgingcat.game;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.dodgingcat.game.GameWorld.*;
import static org.junit.jupiter.api.Assertions.*;

class MapWorldTest {
    private static final Input JUMP = new Input(false, false, true, false);
    private static final Input LEFT = new Input(true, false, false, false);
    private static final Input RIGHT = new Input(false, true, false, false);

    private static GameWorld running(MapTheme theme) {
        GameWorld world = new GameWorld(4242, Mode.SOLO, Set.of(), theme);
        world.start();
        return world;
    }

    private static void run(GameWorld world, double seconds, Input input) {
        for (int i = 0; i < Math.round(seconds / FIXED_STEP); i++) world.update(FIXED_STEP, input);
    }

    private static void protect(GameWorld world) {
        for (Player player : world.players()) world.applyBuff(player, BuffKind.INVINCIBILITY);
    }

    @Test
    void constructorsDefaultToGardenAndKitchenHasDistinctImmutablePlatforms() {
        GameWorld garden = new GameWorld(1);
        assertEquals(MapTheme.GARDEN, garden.mapTheme());
        assertEquals(MapTheme.GARDEN, new GameWorld(1, Mode.VERSUS, Set.of()).mapTheme());
        GameWorld kitchen = running(MapTheme.KITCHEN);
        assertEquals(MapTheme.KITCHEN, kitchen.mapTheme());
        assertEquals(5, garden.platforms().size());
        assertEquals(5, kitchen.platforms().size());
        assertNotEquals(garden.platforms(), kitchen.platforms());
        assertThrows(UnsupportedOperationException.class, () -> kitchen.platforms().clear());
        kitchen.start();
        assertEquals(MapTheme.KITCHEN, kitchen.mapTheme());
    }

    @Test
    void actualKitchenPhysicsCanReachBothLowerPlatformsFromFloor() {
        for (Platform target : running(MapTheme.KITCHEN).platforms().subList(0, 2)) {
            GameWorld world = running(MapTheme.KITCHEN);
            world.placePlayer(target.x() + target.width() / 2 - Player.WIDTH / 2, FLOOR_Y, true);
            world.update(FIXED_STEP, JUMP);
            run(world, 0.8, Input.NONE);
            assertTrue(world.player().grounded());
            assertEquals(target.y(), world.player().y() + Player.HEIGHT, 1e-6);
        }
    }

    @Test
    void actualKitchenJumpChainsReachMiddleAndBothUpperPlatforms() {
        GameWorld middle = running(MapTheme.KITCHEN);
        middle.placePlayer(280, 466, true);
        middle.update(FIXED_STEP, new Input(false, true, true, false));
        run(middle, 0.65, RIGHT);
        assertTrue(middle.player().grounded());
        assertEquals(356, middle.player().y() + Player.HEIGHT, 1e-6);

        GameWorld upperLeft = running(MapTheme.KITCHEN);
        upperLeft.placePlayer(385, 356, true);
        upperLeft.update(FIXED_STEP, new Input(true, false, true, false));
        run(upperLeft, 0.65, LEFT);
        assertTrue(upperLeft.player().grounded());
        assertEquals(248, upperLeft.player().y() + Player.HEIGHT, 1e-6);

        GameWorld upperRight = running(MapTheme.KITCHEN);
        upperRight.placePlayer(530, 356, true);
        upperRight.update(FIXED_STEP, new Input(false, true, true, false));
        run(upperRight, 0.65, RIGHT);
        assertTrue(upperRight.player().grounded());
        assertEquals(248, upperRight.player().y() + Player.HEIGHT, 1e-6);
    }

    @Test
    void actualGeneratedPoolsMatchEachMapWithoutAnyLaserOrAdvanceWarning() {
        for (MapTheme theme : MapTheme.values()) {
            Set<HazardKind> observed = EnumSet.noneOf(HazardKind.class);
            Set<HazardKind> expected = theme == MapTheme.GARDEN ? Set.of(HazardKind.DART, HazardKind.POISON)
                    : Set.of(HazardKind.KNIFE, HazardKind.SCISSORS, HazardKind.FLAME);
            for (long seed : new long[] { 7, 42, 4242 }) {
                GameWorld world = new GameWorld(seed, Mode.SOLO, Set.of(Boost.values()), theme);
                world.start();
                for (int frame = 0; frame < 120 * 65; frame++) {
                    if (frame % 480 == 0) protect(world);
                    world.update(FIXED_STEP, Input.NONE);
                    assertTrue(world.hazards().size() <= Math.min(MAX_HAZARDS, 3 + (int) (world.elapsed() / 30)));
                    for (Hazard hazard : world.hazards()) {
                        assertTrue(expected.contains(hazard.kind()));
                        assertEquals(0, hazard.warningRemaining());
                        assertTrue(hazard.active());
                        observed.add(hazard.kind());
                    }
                }
                assertEquals(State.PLAYING, world.state());
            }
            assertEquals(expected, observed, "Every advertised map hazard must actually appear");
        }
    }

    @Test
    void poisonAndScissorsMoveImmediatelyAndNewHazardsCauseOneHitDamage() {
        for (HazardKind kind : List.of(HazardKind.POISON, HazardKind.SCISSORS)) {
            Hazard hazard = new Hazard(kind, -58, 430, 58, 28, 1, 150, 99);
            assertEquals(0, hazard.warningRemaining());
            hazard.update(0.1);
            assertEquals(-43, hazard.x(), 1e-8);
            assertEquals(430, hazard.y());
        }
        for (HazardKind kind : List.of(HazardKind.POISON, HazardKind.SCISSORS, HazardKind.FLAME)) {
            GameWorld world = running(MapTheme.KITCHEN);
            Player player = world.player();
            world.addHazardForCollision(new Hazard(kind, player.x() + 8, player.y() + 8,
                    20, 20, 0, 0, 99));
            world.update(FIXED_STEP, Input.NONE);
            assertEquals(State.GAME_OVER, world.state(), kind + " must be dangerous from its first frame");
        }
    }

    @Test
    void flameIsStationaryAndExpiresAfterBoundedActiveDuration() {
        Hazard flame = new Hazard(HazardKind.FLAME, 100, FLOOR_Y - 56, 36, 56, 0, 0, 99);
        assertTrue(flame.active());
        flame.update(1.64);
        assertFalse(flame.expired());
        assertEquals(100, flame.x());
        assertEquals(FLOOR_Y - 56, flame.y());
        flame.update(0.02);
        assertTrue(flame.expired());
        GameWorld world = running(MapTheme.KITCHEN);
        assertTrue(world.offerHazard(new Hazard(HazardKind.FLAME, 100, FLOOR_Y - 56, 36, 56, 0, 0, 0)));
        run(world, 1.7, Input.NONE);
        assertTrue(world.hazards().isEmpty());
    }

    @Test
    void flameRequiresSupportedGeometryAndSafeDistanceFromEveryLivingCat() {
        GameWorld world = new GameWorld(1, Mode.VERSUS, Set.of(), MapTheme.KITCHEN);
        world.start();
        world.placePlayer(1, 700, FLOOR_Y, true);
        world.placePlayer(2, 100, FLOOR_Y, true);
        assertFalse(world.offerHazard(new Hazard(HazardKind.FLAME, 100, FLOOR_Y - 56, 36, 56, 0, 0, 0)));
        assertFalse(world.offerHazard(new Hazard(HazardKind.FLAME, 175, FLOOR_Y - 56, 36, 56, 0, 0, 0)),
                "P2 needs a safe gap even though P1 is far away");
        assertFalse(world.offerHazard(new Hazard(HazardKind.FLAME, 400, 200, 36, 56, 0, 0, 0)),
                "Floating flames must not appear in empty space");
        assertTrue(world.offerHazard(new Hazard(HazardKind.FLAME, 400, FLOOR_Y - 56, 36, 56, 0, 0, 0)));
        assertFalse(world.offerHazard(new Hazard(HazardKind.FLAME, 450, FLOOR_Y - 56, 36, 56, 0, 0, 0)),
                "Dense neighboring flames must be rejected");
    }

    @Test
    void horizontalTravelSafetyAndLaneDensityApplyToPoisonAndScissorsForBothCats() {
        for (HazardKind kind : List.of(HazardKind.POISON, HazardKind.SCISSORS)) {
            GameWorld world = new GameWorld(1, Mode.VERSUS, Set.of(), MapTheme.KITCHEN);
            world.start();
            world.placePlayer(1, 400, FLOOR_Y, true);
            world.placePlayer(2, 30, FLOOR_Y, true);
            assertFalse(world.offerHazard(new Hazard(kind, -58, FLOOR_Y - 32, 58, 28, 1, 150, 0)));
            assertTrue(world.offerHazard(new Hazard(kind, WIDTH, FLOOR_Y - 32, 58, 28, -1, 150, 0)));
            assertFalse(world.offerHazard(new Hazard(HazardKind.DART, -28, FLOOR_Y - 28, 28, 10, 1, 250, 0)));
        }
    }

    @Test
    void mapPickupsUseRealSupportsAndAvoidCurrentHazardGeometry() {
        for (MapTheme theme : MapTheme.values()) {
            GameWorld world = new GameWorld(19, Mode.SOLO, Set.of(Boost.values()), theme);
            world.start();
            boolean sawPlatformPickup = false;
            for (int frame = 0; frame < 120 * 50; frame++) {
                if (frame % 480 == 0) protect(world);
                world.update(FIXED_STEP, Input.NONE);
                for (Pickup pickup : world.pickups()) {
                    assertTrue(pickup.x() >= 24 && pickup.x() <= WIDTH - 24);
                    boolean floor = Math.abs(pickup.y() - FLOOR_Y + 18) < 1e-8;
                    boolean onPlatform = world.platforms().stream().anyMatch(platform ->
                            Math.abs(pickup.y() - platform.y() + 18) < 1e-8 && pickup.x() >= platform.x() + 24
                                    && pickup.x() <= platform.x() + platform.width() - 24);
                    assertTrue(floor || onPlatform);
                    sawPlatformPickup |= onPlatform;
                    if (pickup.age() == 0) {
                        for (Hazard hazard : world.hazards()) {
                            assertFalse(pickup.x() + 18 > hazard.x() - 12
                                    && pickup.x() - 18 < hazard.x() + hazard.width() + 12
                                    && pickup.y() + 18 > hazard.y() - 12
                                    && pickup.y() - 18 < hazard.y() + hazard.height() + 12);
                        }
                    }
                }
            }
            assertTrue(sawPlatformPickup);
        }
    }

    @Test
    void roundAndDamageEventsAreEnglishWhilePickupMessagesRemainLocal() {
        GameWorld world = running(MapTheme.GARDEN);
        assertEquals("Survive and collect fish!", world.lastEvent());
        world.applyBuff(BuffKind.SHIELD);
        world.addHazardForCollision(new Hazard(HazardKind.POISON, world.player().x() + 8,
                world.player().y() + 8, 20, 20, 0, 0, 0));
        world.update(FIXED_STEP, Input.NONE);
        assertEquals("P1's shield blocked a hit!", world.lastEvent());
        run(world, 1, Input.NONE);
        assertEquals("Game over — press Enter to retry!", world.lastEvent());
        world.menu();
        assertEquals("Ready to dodge!", world.lastEvent());
    }
}
