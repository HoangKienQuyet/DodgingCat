package com.dodgingcat.collision;

/**
 * Axis-aligned bounding box representation for entities.
 */
public class Hitbox {
    private double offsetX;
    private double offsetY;
    private double width;
    private double height;

    public Hitbox(double offsetX, double offsetY, double width, double height) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;
    }

    public double getLeft(double entityX) {
        return entityX + offsetX;
    }

    public double getRight(double entityX) {
        return entityX + offsetX + width;
    }

    public double getTop(double entityY) {
        return entityY + offsetY;
    }

    public double getBottom(double entityY) {
        return entityY + offsetY + height;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getCenterX(double entityX) {
        return entityX + offsetX + width / 2.0;
    }

    public double getCenterY(double entityY) {
        return entityY + offsetY + height / 2.0;
    }

    public boolean intersects(double x1, double y1, Hitbox other, double x2, double y2) {
        return getLeft(x1) < other.getRight(x2) &&
               getRight(x1) > other.getLeft(x2) &&
               getTop(y1) < other.getBottom(y2) &&
               getBottom(y1) > other.getTop(y2);
    }

    public boolean intersectsRect(double entityX, double entityY, double rx, double ry, double rw, double rh) {
        return getLeft(entityX) < rx + rw &&
               getRight(entityX) > rx &&
               getTop(entityY) < ry + rh &&
               getBottom(entityY) > ry;
    }

    public boolean intersectsCircle(double entityX, double entityY, double cx, double cy, double radius) {
        double closestX = Math.clamp(cx, getLeft(entityX), getRight(entityX));
        double closestY = Math.clamp(cy, getTop(entityY), getBottom(entityY));
        double dx = cx - closestX;
        double dy = cy - closestY;
        return (dx * dx + dy * dy) <= (radius * radius);
    }
}
