package com.dodgingcat.game;

import com.dodgingcat.audio.AudioManager;

/**
 * A platform entity supporting one-way jumping, drop-down, and crumbling behavior.
 */
public class Platform {
    private double x;
    private double y;
    private double width;
    private double height;
    private final PlatformType type;

    // Crumbling state
    private boolean warning = false;
    private boolean collapsed = false;
    private double warningTimer = 0.0;
    private double collapseDuration = 0.0;
    private double breakAnimationTimer = 0.0;
    private double shakeOffset = 0.0;
    private boolean respawnReady = false;

    public Platform(double x, double y, double width, double height, PlatformType type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
    }

    public void update(double dt) {
        if (type == PlatformType.CRUMBLING) {
            if (warning) {
                warningTimer -= dt;
                shakeOffset = (Math.random() - 0.5) * 4.0;
                if (warningTimer <= 0.0) {
                    warning = false;
                    collapsed = true;
                    collapseDuration = 4.0; // Collapsed for 4s
                    breakAnimationTimer = 0.42;
                }
            } else if (collapsed) {
                breakAnimationTimer = Math.max(0.0, breakAnimationTimer - dt);
                collapseDuration -= dt;
                if (collapseDuration <= 0.0) {
                    respawnReady = true;
                }
            } else {
                shakeOffset = 0.0;
            }
        }
    }

    /**
     * Triggers the warning / crumbling sequence.
     */
    public void triggerCrumble(double warningTimeSeconds) {
        if (type == PlatformType.CRUMBLING && !collapsed && !warning) {
            this.warning = true;
            this.warningTimer = warningTimeSeconds;
            AudioManager.getInstance().playSfx("platform_crumble.wav");
        }
    }

    public boolean isSolidToPlayer(double playerPrevBottom, double playerCurrBottom, double playerVy, boolean isDroppingDown) {
        if (collapsed) return false;

        if (type == PlatformType.SOLID_FLOOR) {
            return playerCurrBottom >= y;
        }

        if (type == PlatformType.ONE_WAY || type == PlatformType.CRUMBLING) {
            // If player is intentionally dropping down, ignore collision
            if (isDroppingDown) {
                return false;
            }
            // One-way landing condition: was above platform top previously, now reached or crossed platform top, and falling downwards
            boolean wasAbove = playerPrevBottom <= y + 6.0;
            boolean nowAtOrBelow = playerCurrBottom >= y;
            boolean falling = playerVy >= 0;
            return wasAbove && nowAtOrBelow && falling;
        }

        return false;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public PlatformType getType() {
        return type;
    }

    public boolean isWarning() {
        return warning;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public double getBreakAnimationTimer() {
        return breakAnimationTimer;
    }

    public boolean isRespawnReady() {
        return respawnReady;
    }

    /** Restores a collapsed platform at a newly selected, validated location. */
    public void respawnAt(double x, double y, double width) {
        if (!respawnReady) return;
        this.x = x;
        this.y = y;
        this.width = width;
        this.warning = false;
        this.collapsed = false;
        this.warningTimer = 0.0;
        this.collapseDuration = 0.0;
        this.breakAnimationTimer = 0.0;
        this.shakeOffset = 0.0;
        this.respawnReady = false;
    }

    public double getShakeOffset() {
        return shakeOffset;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setDimensions(double width, double height) {
        this.width = width;
        this.height = height;
    }
}
