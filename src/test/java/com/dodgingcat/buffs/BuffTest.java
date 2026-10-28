package com.dodgingcat.buffs;

import com.dodgingcat.core.GameMode;
import com.dodgingcat.game.GameWorld;
import com.dodgingcat.game.Player;
import com.dodgingcat.hazards.TopDownHazard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BuffTest {

    @Test
    @DisplayName("Buff duration constants match requirements (5s for Shield, Invincibility, x2 Score)")
    void testBuffDurations() {
        assertEquals(5.0, BuffType.SHIELD.getDefaultDuration());
        assertEquals(5.0, BuffType.INVINCIBILITY.getDefaultDuration());
        assertEquals(5.0, BuffType.DOUBLE_SCORE.getDefaultDuration());
    }

    @Test
    @DisplayName("Bomb buff destroys all active hazards in the world")
    void testBombScreenClear() {
        GameWorld world = new GameWorld(GameMode.SINGLE_PLAYER, 960, 640, "ginger", "tuxedo");

        // Add hazards
        world.getHazards().add(new TopDownHazard(200, 100, 32, 32, "anvil", 100, 300));
        world.getHazards().add(new TopDownHazard(400, 100, 32, 32, "knife", 100, 300));
        assertEquals(2, world.getHazards().size());

        // Spawn and place bomb right on player
        Player p = world.getPlayers().getFirst();
        Buff bomb = new Buff(BuffType.BOMB, p.getX(), p.getY());
        world.getBuffs().add(bomb);

        // Update world to trigger collection
        world.update(0.016);

        long activeHazards = world.getHazards().stream().filter(h -> h.isActive()).count();
        assertEquals(0, activeHazards, "Bomb must deactivate all active hazards on screen");
    }

    @Test
    @DisplayName("Heart buff in Co-op mode revives fallen teammate")
    void testHeartReviveInCoop() {
        GameWorld world = new GameWorld(GameMode.LOCAL_COOP, 960, 640, "ginger", "tuxedo");
        assertEquals(2, world.getPlayers().size());

        Player p1 = world.getPlayers().get(0);
        Player p2 = world.getPlayers().get(1);

        // Knock out P2
        p2.applyHit();
        assertFalse(p2.isAlive());

        // P1 collects Heart buff
        Buff heart = new Buff(BuffType.HEART, p1.getX(), p1.getY());
        world.getBuffs().add(heart);

        world.update(0.016);

        assertTrue(p2.isAlive(), "Heart buff must revive the fallen teammate");
        assertTrue(p2.isInvincible(), "Revived teammate must receive invulnerability period");
    }
}
