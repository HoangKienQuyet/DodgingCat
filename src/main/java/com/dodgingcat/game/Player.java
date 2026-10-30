package com.dodgingcat.game;

import com.dodgingcat.buffs.BuffType;
import com.dodgingcat.collision.Hitbox;

/**
 * Represents the playable Cat character (P1 or P2).
 */
public class Player {
    private final int id;
    private double x;
    private double y;
    private double vx;
    private double vy;

    // Logical sprite dimensions remain the gameplay coordinate basis. Rendering scales
    // the cat by 2.0, so scale the collision box by the same amount and anchor its feet.
    private final double spriteWidth = 36.0;
    private final double spriteHeight = 36.0;
    // Keep the old 2px clearance below the logical feet while expanding upward.
    private final Hitbox hitbox = new Hitbox(-8.0, -26.0, 52.0, 60.0);

    // Movement tuning
    private final double moveSpeed = 260.0;
    private final double jumpVelocity = -520.0;
    private final double gravity = 1200.0;
    private final double maxFallSpeed = 750.0;

    // State flags
    private boolean grounded = false;
    private boolean droppingDown = false;
    private boolean alive = true;
    private int facingDirection = 1; // 1 = right, -1 = left
    private double walkAnimTime = 0.0;

    // Buff timers (seconds)
    private double shieldTimer = 0.0;
    private double invincibilityTimer = 0.0;
    private double doubleScoreTimer = 0.0;

    // Cosmetic skin identifier
    private String skinId = "ginger";

    public Player(int id, double startX, double startY, String skinId) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.skinId = skinId;
    }

    public void updatePhysics(double dt, double screenWidth, java.util.List<Platform> platforms) {
        if (!alive) return;

        // Update buff timers
        if (shieldTimer > 0) shieldTimer = Math.max(0, shieldTimer - dt);
        if (invincibilityTimer > 0) invincibilityTimer = Math.max(0, invincibilityTimer - dt);
        if (doubleScoreTimer > 0) doubleScoreTimer = Math.max(0, doubleScoreTimer - dt);

        double prevY = y;
        double prevBottom = prevY + spriteHeight;

        // Apply gravity
        vy += gravity * dt;
        if (vy > maxFallSpeed) vy = maxFallSpeed;

        // Apply velocities
        x += vx * dt;
        y += vy * dt;

        // Keep inside screen horizontal boundaries
        if (x + hitbox.getLeft(0) < 0) {
            x = -hitbox.getLeft(0);
            vx = 0;
        } else if (x + hitbox.getRight(0) > screenWidth) {
            x = screenWidth - hitbox.getRight(0);
            vx = 0;
        }

        double currBottom = y + spriteHeight;
        grounded = false;

        // Platform collision checks
        for (Platform platform : platforms) {
            // Landing support comes from the paws/body footprint, not the enlarged
            // hazard hitbox. Otherwise a cat can remain grounded with only its head
            // or tail hitbox overlapping the platform edge.
            double footLeft = x + spriteWidth * 0.20;
            double footRight = x + spriteWidth * 0.80;
            double pLeft = platform.getX();
            double pRight = platform.getX() + platform.getWidth();
            double supportOverlap = Math.min(footRight, pRight) - Math.max(footLeft, pLeft);

            if (supportOverlap >= spriteWidth * 0.22) {
                if (platform.isSolidToPlayer(prevBottom, currBottom, vy, droppingDown)) {
                    y = platform.getY() - spriteHeight;
                    vy = 0;
                    grounded = true;
                    break;
                }
            }
        }

        // Update walk animation
        if (Math.abs(vx) > 10.0 && grounded) {
            // Keep this clock in real seconds. The renderer converts it to a
            // frame index; multiplying by ten here caused the six-frame gait
            // to advance several poses per rendered frame.
            walkAnimTime += dt;
        } else {
            walkAnimTime = 0.0;
        }
    }

    public void moveLeft() {
        if (!alive) return;
        this.vx = -moveSpeed;
        this.facingDirection = -1;
    }

    public void moveRight() {
        if (!alive) return;
        this.vx = moveSpeed;
        this.facingDirection = 1;
    }

    public void stopHorizontal() {
        this.vx = 0;
    }

    public void jump() {
        if (!alive) return;
        if (grounded) {
            this.vy = jumpVelocity;
            this.grounded = false;
        }
    }

    public void setDroppingDown(boolean dropping) {
        this.droppingDown = dropping;
    }

    public boolean applyHit() {
        if (!alive) return false;
        if (isInvincible()) return false;

        if (shieldTimer > 0) {
            shieldTimer = 0.0; // Shield absorbed the lethal hit
            doubleScoreTimer = 0.0;
            invincibilityTimer = 1.0; // Brief grace period after shield break
            return false;
        }

        // Fatal hit
        clearTimedBuffs();
        alive = false;
        vx = 0;
        return true;
    }

    public void applyBuff(BuffType type) {
        switch (type) {
            case SHIELD -> {
                clearTimedBuffs();
                shieldTimer = 5.0;
            }
            case INVINCIBILITY -> {
                clearTimedBuffs();
                invincibilityTimer = 5.0;
            }
            case DOUBLE_SCORE -> {
                clearTimedBuffs();
                doubleScoreTimer = 5.0;
            }
            case HEART -> {} // Hearts are handled by GameWorld and only spawn in Co-op.
            default -> {}
        }
    }

    private void clearTimedBuffs() {
        shieldTimer = 0.0;
        invincibilityTimer = 0.0;
        doubleScoreTimer = 0.0;
    }

    public boolean hasActiveTimedBuff() {
        return shieldTimer > 0.0 || invincibilityTimer > 0.0 || doubleScoreTimer > 0.0;
    }

    public boolean hasBuffEffect(BuffType type) {
        return switch (type) {
            case SHIELD -> shieldTimer > 0;
            case INVINCIBILITY -> invincibilityTimer > 0;
            case DOUBLE_SCORE -> doubleScoreTimer > 0;
            default -> false;
        };
    }

    public void revive(double spawnX, double spawnY) {
        clearTimedBuffs();
        this.alive = true;
        this.x = spawnX;
        this.y = spawnY;
        this.vx = 0;
        this.vy = 0;
        this.invincibilityTimer = 3.5; // 3.5s of invincibility upon revive
    }

    public boolean isInvincible() {
        return invincibilityTimer > 0;
    }

    public boolean hasShield() {
        return shieldTimer > 0;
    }

    public boolean hasDoubleScore() {
        return doubleScoreTimer > 0;
    }

    // Getters and setters
    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public double getSpriteWidth() { return spriteWidth; }
    public double getSpriteHeight() { return spriteHeight; }
    public Hitbox getHitbox() { return hitbox; }
    public boolean isGrounded() { return grounded; }
    public boolean isDroppingDown() { return droppingDown; }
    public boolean isAlive() { return alive; }
    public int getFacingDirection() { return facingDirection; }
    public double getWalkAnimTime() { return walkAnimTime; }
    public double getShieldTimer() { return shieldTimer; }
    public double getInvincibilityTimer() { return invincibilityTimer; }
    public double getDoubleScoreTimer() { return doubleScoreTimer; }
    public String getSkinId() { return skinId; }
    public void setSkinId(String skinId) { this.skinId = skinId; }
}
