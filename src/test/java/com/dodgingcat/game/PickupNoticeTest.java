package com.dodgingcat.game;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dodgingcat.game.GameWorld.*;
import static org.junit.jupiter.api.Assertions.*;

class PickupNoticeTest {
    private static GameWorld running() {
        GameWorld world = new GameWorld(4242, Mode.VERSUS, Set.of());
        world.start();
        return world;
    }

    private static void run(GameWorld world, double seconds) {
        for (int i = 0; i < Math.round(seconds / FIXED_STEP); i++) world.update(FIXED_STEP, Input.NONE, Input.NONE);
    }

    private static Pickup at(Player player, double dx, double dy, BuffKind kind) {
        return new Pickup(player.x() + dx, player.y() + dy, kind);
    }

    @Test
    void simultaneousPickupsKeepEveryExactLocationBeneficiaryKindAndTextWithoutTopToast() {
        GameWorld world = running();
        Player first = world.player();
        Player second = world.players().get(1);
        List<Pickup> items = List.of(at(first, 12, 20, BuffKind.SHIELD),
                at(second, 16, 18, BuffKind.INVINCIBILITY), at(first, 20, 20, BuffKind.DOUBLE_FISH),
                at(second, 18, 20, BuffKind.BOMB), at(first, 10, 18, null));
        List<String> texts = List.of("Shield · 1 hit", "Invincible · 5s", "Fish ×2 · 5s", "Traps cleared", "+2 fish");
        List<Integer> collectors = List.of(1, 2, 1, 2, 1);
        String event = world.lastEvent();
        long version = world.eventVersion();
        items.forEach(world::addPickup);
        world.update(FIXED_STEP, Input.NONE, Input.NONE);
        assertEquals(5, world.pickupNotices().size());
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            PickupNotice notice = world.pickupNotices().get(i);
            assertTrue(ids.add(notice.id()), "Simultaneous notices need different identifiers");
            assertEquals(items.get(i).x(), notice.x());
            assertEquals(items.get(i).y(), notice.y());
            assertEquals(items.get(i).kind(), notice.kind());
            assertEquals(texts.get(i), notice.text());
            assertEquals(collectors.get(i).intValue(), notice.playerId());
            assertEquals(world.elapsed(), notice.bornAt());
        }
        assertEquals(event, world.lastEvent());
        assertEquals(version, world.eventVersion());
        assertEquals(2, first.fishCollected());
        assertTrue(first.shieldActive());
        assertEquals(BUFF_SECONDS, second.invincibilityRemaining());
        assertThrows(UnsupportedOperationException.class, () -> world.pickupNotices().clear());
    }

    @Test
    void fishNoticeReportsActualAwardForEachCatAndApplyingEffectsAloneEmitsNothing() {
        GameWorld world = running();
        Player first = world.player();
        Player second = world.players().get(1);
        String event = world.lastEvent();
        long version = world.eventVersion();
        for (BuffKind kind : BuffKind.values()) world.applyBuff(second, kind);
        assertTrue(world.pickupNotices().isEmpty(), "Only collecting an item creates a visual notice");
        assertEquals(event, world.lastEvent());
        assertEquals(version, world.eventVersion());
        world.addPickup(at(first, 16, 18, null));
        world.addPickup(at(second, 16, 18, null));
        world.update(FIXED_STEP, Input.NONE, Input.NONE);
        assertEquals("+1 fish", world.pickupNotices().get(0).text());
        assertEquals(1, world.pickupNotices().get(0).playerId());
        assertEquals("+2 fish", world.pickupNotices().get(1).text());
        assertEquals(2, world.pickupNotices().get(1).playerId());
        assertNull(world.pickupNotices().get(0).kind());
        assertEquals(3, world.fishCollected());
        assertEquals(0, world.score());
    }

    @Test
    void noticeExpiresAfterOnePointTwoSimulationSecondsAndPauseFreezesItsAge() {
        GameWorld world = running();
        world.addPickup(at(world.player(), 16, 18, null));
        world.update(FIXED_STEP, Input.NONE, Input.NONE);
        PickupNotice notice = world.pickupNotices().getFirst();
        run(world, 0.5);
        world.togglePause();
        double elapsed = world.elapsed();
        run(world, 4);
        assertEquals(elapsed, world.elapsed());
        assertTrue(world.pickupNotices().contains(notice));
        world.togglePause();
        run(world, 0.69);
        assertTrue(world.pickupNotices().contains(notice));
        run(world, 0.02);
        assertFalse(world.pickupNotices().contains(notice));
    }

    @Test
    void menuAndRestartClearTransientNotices() {
        GameWorld world = running();
        world.addPickup(at(world.player(), 16, 18, null));
        world.update(FIXED_STEP, Input.NONE, Input.NONE);
        assertFalse(world.pickupNotices().isEmpty());
        world.menu();
        assertTrue(world.pickupNotices().isEmpty());
        world.start();
        world.addPickup(at(world.player(), 16, 18, BuffKind.BOMB));
        world.update(FIXED_STEP, Input.NONE, Input.NONE);
        assertFalse(world.pickupNotices().isEmpty());
        world.start();
        assertTrue(world.pickupNotices().isEmpty());
    }

    @Test
    void burstCapDropsOldestVisualEntriesWithoutLosingFishAwards() {
        GameWorld world = running();
        for (int i = 0; i < 40; i++) world.addPickup(at(world.player(), 16, 18, null));
        world.update(FIXED_STEP, Input.NONE, Input.NONE);
        assertEquals(40, world.fishCollected(), "Visual limits must not limit gameplay effects");
        assertEquals(32, world.pickupNotices().size());
        assertEquals(9, world.pickupNotices().getFirst().id());
        assertEquals(40, world.pickupNotices().getLast().id());
    }

    @Test
    void overlappingCatsStillCreateOnlyOneNoticeForSharedItem() {
        GameWorld world = running();
        world.placePlayer(1, 450, FLOOR_Y, true);
        world.placePlayer(2, 450, FLOOR_Y, true);
        world.addPickup(at(world.player(), 16, 18, null));
        world.update(FIXED_STEP, Input.NONE, Input.NONE);
        assertEquals(1, world.pickupNotices().size());
        assertEquals(1, world.pickupNotices().getFirst().playerId());
        assertEquals(1, world.fishCollected());
        assertTrue(world.pickups().isEmpty());
    }
}
