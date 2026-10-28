package com.dodgingcat.hazards;

import com.dodgingcat.collision.Hitbox;
import com.dodgingcat.game.Player;

/**
 * Base class for all hazards.
 */
public abstract class Hazard {
    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected final HazardType type;
    protected final String subType;
    protected boolean active = true;

    public Hazard(double x, double y, double width, double height, HazardType type, String subType) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.subType = subType;
    }

    public abstract void update(double dt, double screenWidth, double screenHeight);

    public abstract boolean collidesWith(Player player);

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
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

    public HazardType getType() {
        return type;
    }

    public String getSubType() {
        return subType;
    }

    public void scaleHorizontally(double scale, double newScreenWidth) {
        x *= scale;
    }
}
