package com.dodgingcat.game;

import com.dodgingcat.buffs.BuffType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {
    private Player player;
    private List<Platform> platforms;

    @BeforeEach
    void setUp() {
        player = new Player(1, 100.0, 100.0, "ginger");
        platforms = new ArrayList<>();
    }

    @Test
    @DisplayName("Player starts alive and with zero horizontal velocity")
    void testInitialState() {
        assertTrue(player.isAlive());
        assertEquals(0.0, player.getVx());
        assertFalse(player.hasShield());
        assertFalse(player.isInvincible());
    }

    @Test
    @DisplayName("Horizontal movement updates velocity and facing direction")
    void testHorizontalMovement() {
        player.moveRight();
        assertEquals(260.0, player.getVx());
        assertEquals(1, player.getFacingDirection());

        player.moveLeft();
        assertEquals(-260.0, player.getVx());
        assertEquals(-1, player.getFacingDirection());

        player.stopHorizontal();
        assertEquals(0.0, player.getVx());
    }

    @Test
    @DisplayName("Single jump sets negative vertical velocity when grounded")
    void testJumpWhenGrounded() {
        // Force grounded by landing on floor
        Platform floor = new Platform(0, 200, 500, 40, PlatformType.SOLID_FLOOR);
        platforms.add(floor);

        player.setPosition(100.0, 164.0); // Exactly on top of floor (200 - 36)
        player.updatePhysics(0.016, 800.0, platforms);
        assertTrue(player.isGrounded());

        player.jump();
        assertEquals(-520.0, player.getVy());
        assertFalse(player.isGrounded());
    }

    @Test
    @DisplayName("Jump fails when player is in the air")
    void testJumpFailsInAir() {
        player.setPosition(100.0, 50.0);
        player.updatePhysics(0.016, 800.0, platforms);
        assertFalse(player.isGrounded());

        double initialVy = player.getVy();
        player.jump();
        assertEquals(initialVy, player.getVy());
    }

    @Test
    @DisplayName("Shield absorbs one lethal hit and breaks")
    void testShieldAbsorption() {
        player.applyBuff(BuffType.SHIELD);
        assertTrue(player.hasShield());

        boolean lethal = player.applyHit();
        assertFalse(lethal, "Shield must absorb lethal damage");
        assertTrue(player.isAlive(), "Player must remain alive after shield hit");
        assertFalse(player.hasShield(), "Shield must break after absorbing hit");
        assertTrue(player.isInvincible(), "Player must receive brief grace invincibility");
    }

    @Test
    @DisplayName("Invincibility protects player without taking lethal damage")
    void testInvincibilityProtection() {
        player.applyBuff(BuffType.INVINCIBILITY);
        assertTrue(player.isInvincible());

        boolean lethal = player.applyHit();
        assertFalse(lethal);
        assertTrue(player.isAlive());
    }

    @Test
    @DisplayName("Unprotected hit causes death")
    void testUnprotectedFatalHit() {
        boolean lethal = player.applyHit();
        assertTrue(lethal);
        assertFalse(player.isAlive());
        assertEquals(0.0, player.getVx());
    }

    @Test
    @DisplayName("Revive restores living state, position and invulnerability timer")
    void testRevive() {
        player.applyHit();
        assertFalse(player.isAlive());

        player.revive(300.0, 200.0);
        assertTrue(player.isAlive());
        assertEquals(300.0, player.getX());
        assertEquals(200.0, player.getY());
        assertTrue(player.isInvincible());
        assertEquals(3.5, player.getInvincibilityTimer());
    }
}
