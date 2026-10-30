package com.dodgingcat.buffs;

import com.dodgingcat.core.GameMode;
import com.dodgingcat.game.*;
import com.dodgingcat.generation.ProceduralGenerator;
import com.dodgingcat.hazards.Hazard;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BuffLifecycleTest {
    private static class QuietGenerator extends ProceduralGenerator {
        @Override public void update(double dt, double time, String map, double w, double h,
                                     List<Player> players, List<Hazard> hazards, List<Platform> platforms,
                                     List<Buff> buffs, List<FishItem> fish, boolean coop) {
            super.update(dt, time, map, w, h, players, new ArrayList<>(), List.of(), buffs, fish, coop);
        }
    }
    private GameWorld world(GameMode mode) { return new GameWorld(mode, 960, 640, "ginger", "tuxedo", new QuietGenerator()); }
    private void collect(GameWorld world, BuffType type) {
        var player = world.getPlayers().getFirst();
        world.getBuffs().add(new Buff(type, player.getX(), player.getY()));
        world.update(0);
    }

    @Test void everyBuffAwardsExactlyFiftyPointsOnce() {
        for (var type : BuffType.values()) {
            var world = world(GameMode.LOCAL_COOP);
            if (type == BuffType.HEART) world.getPlayers().get(1).applyHit();
            collect(world, type);
            assertEquals(50, world.getScore(), type.toString());
            assertTrue(world.getBuffs().isEmpty(), "Collected pickup must be removed immediately");
            world.update(0);
            assertEquals(50, world.getScore(), "Removal must not award twice");
        }
    }

    @Test void uncollectedBuffStartsCooldownOnlyWhenItDisappears() {
        var world = world(GameMode.SINGLE_PLAYER);
        world.getBuffs().add(new Buff(BuffType.DOUBLE_SCORE, 80, 80, 2));
        world.update(1);
        assertEquals(0, world.getGenerator().getBuffCooldownRemaining(BuffType.DOUBLE_SCORE));
        world.update(1);
        assertEquals(25, world.getGenerator().getBuffCooldownRemaining(BuffType.DOUBLE_SCORE));
        world.update(1);
        assertEquals(24, world.getGenerator().getBuffCooldownRemaining(BuffType.DOUBLE_SCORE));
    }

    @Test void timedEffectsWaitUntilTheirEndAndShieldBreakStartsEarly() {
        for (var type : List.of(BuffType.DOUBLE_SCORE, BuffType.INVINCIBILITY, BuffType.SHIELD)) {
            var world = world(GameMode.SINGLE_PLAYER);
            collect(world, type);
            for (int i = 0; i < 4; i++) world.update(1);
            assertEquals(0, world.getGenerator().getBuffCooldownRemaining(type));
            world.update(1);
            assertEquals(type.getCooldownSeconds(), world.getGenerator().getBuffCooldownRemaining(type));
        }
        var world = world(GameMode.SINGLE_PLAYER);
        collect(world, BuffType.SHIELD);
        assertFalse(world.getPlayers().getFirst().applyHit());
        world.update(0);
        assertEquals(45, world.getGenerator().getBuffCooldownRemaining(BuffType.SHIELD));
    }

    @Test void bombIsImmediateAndHeartWaitsForReviveGraceOrFallbackShield() {
        var world = world(GameMode.SINGLE_PLAYER);
        collect(world, BuffType.BOMB);
        assertEquals(35, world.getGenerator().getBuffCooldownRemaining(BuffType.BOMB));
        world = world(GameMode.LOCAL_COOP);
        world.getPlayers().get(1).applyHit();
        collect(world, BuffType.HEART);
        world.update(3);
        assertEquals(0, world.getGenerator().getBuffCooldownRemaining(BuffType.HEART));
        world.update(.5);
        assertEquals(45, world.getGenerator().getBuffCooldownRemaining(BuffType.HEART));
        world = world(GameMode.LOCAL_COOP);
        collect(world, BuffType.HEART);
        assertTrue(world.getPlayers().getFirst().hasShield());
        world.update(5);
        assertEquals(45, world.getGenerator().getBuffCooldownRemaining(BuffType.HEART));
    }
}
