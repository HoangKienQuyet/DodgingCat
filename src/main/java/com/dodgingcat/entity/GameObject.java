package com.dodgingcat.entity;

/** A rectangular object in the game's logical world. */
public abstract class GameObject {

    private double x;
    private double y;
    private final double width;
    private final double height;

    protected GameObject(double x, double y, double width, double height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Object dimensions must be positive");
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public final double getX() {
        return x;
    }

    public final double getY() {
        return y;
    }

    public final double getWidth() {
        return width;
    }

    public final double getHeight() {
        return height;
    }

    protected final void setX(double x) {
        this.x = x;
    }

    protected final void setY(double y) {
        this.y = y;
    }

    /** Returns true once a moving object has left the visible world. */
    public boolean isOutside(double worldWidth, double worldHeight) {
        return x + width < 0 || x > worldWidth || y > worldHeight;
    }
}
