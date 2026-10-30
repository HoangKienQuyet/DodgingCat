package com.dodgingcat.buffs;

import com.dodgingcat.collision.Hitbox;

/**
 * World pickup item that grants temporary or immediate buffs.
 */
public class Buff {
    private final BuffType type;
    private double x;
    private double y;
    private final double baseWidth = 42.0;
    private final double baseHeight = 42.0;
    private final Hitbox hitbox;
    private double lifeTime;
    private final double fullLifetime;
    private double floatTimer = 0.0;
    private boolean collected = false;

    public Buff(BuffType type, double x, double y) {
        this(type, x, y, 5.0);
    }

    public Buff(BuffType type, double x, double y, double lifetime) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.lifeTime = lifetime;
        this.fullLifetime = lifetime;
        this.hitbox = new Hitbox(0, 0, baseWidth, baseHeight);
    }

    public void update(double dt) {
        floatTimer += dt * 3.0;
        lifeTime -= dt;
    }

    public boolean isExpired() {
        return lifeTime <= 0.0 || collected;
    }

    public BuffType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        // Subtle vertical floating wave
        return y + Math.sin(floatTimer) * 3.5;
    }

    public double getBaseY() {
        return y;
    }

    public double getWidth() {
        return baseWidth;
    }

    public double getHeight() {
        return baseHeight;
    }

    public Hitbox getHitbox() {
        return hitbox;
    }

    public boolean isCollected() {
        return collected;
    }

    public void collect() {
        this.collected = true;
    }

    public double getRemainingLifetime() {
        return lifeTime;
    }

    public double getFullLifetime() { return fullLifetime; }
    public double getSpawnAge() { return fullLifetime - lifeTime; }

    public void scaleX(double scale) { x *= scale; }
}
