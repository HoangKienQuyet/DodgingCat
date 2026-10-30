package com.dodgingcat.game;

import com.dodgingcat.collision.Hitbox;

/**
 * An in-world collectible item representing Pate currency.
 */
public class PateItem {
    private final PateType type;
    private double x;
    private double y;
    private final Hitbox hitbox;
    private double lifeTime = 15.0; // Despawns after 15s if not grabbed
    private double animTimer = 0.0;
    private boolean collected = false;

    public PateItem(PateType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.hitbox = new Hitbox(0, 0, type.getWidth(), type.getHeight());
    }

    public void update(double dt) {
        animTimer += dt * 4.0;
        lifeTime -= dt;
    }

    public boolean isExpired() {
        return lifeTime <= 0.0 || collected;
    }

    public PateType getType() {
        return type;
    }

    public int getValue() {
        return type.getValue();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y + Math.sin(animTimer) * 2.5;
    }

    public double getBaseY() {
        return y;
    }

    public double getWidth() {
        return type.getWidth();
    }

    public double getHeight() {
        return type.getHeight();
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
}
