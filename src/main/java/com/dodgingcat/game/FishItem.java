package com.dodgingcat.game;

import com.dodgingcat.collision.Hitbox;

/**
 * World pickup item representing fish currency.
 */
public class FishItem {
    private final FishType type;
    private final String currencySkinId;
    private double x;
    private double y;
    private final Hitbox hitbox;
    private double lifeTime = 5.0;
    private final double fullLifetime = 5.0;
    private double animTimer = 0.0;
    private boolean collected = false;

    public FishItem(FishType type, double x, double y) {
        this(type, x, y, null);
    }

    public FishItem(FishType type, double x, double y, String currencySkinId) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.currencySkinId = currencySkinId;
        this.hitbox = new Hitbox(0, 0, type.getWidth(), type.getHeight());
    }

    public void update(double dt) {
        animTimer += dt * 4.0;
        lifeTime -= dt;
    }

    public boolean isExpired() {
        return lifeTime <= 0.0 || collected;
    }

    public FishType getType() {
        return type;
    }

    /** Legendary cosmetic currency theme selected once when this item spawns. */
    public String getCurrencySkinId() {
        return currencySkinId;
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

    public double getRemainingLifetime() { return lifeTime; }
    public double getFullLifetime() { return fullLifetime; }
    public double getSpawnAge() { return fullLifetime - lifeTime; }

    public void scaleX(double scale) { x *= scale; }

    public void collect() {
        this.collected = true;
    }
}
