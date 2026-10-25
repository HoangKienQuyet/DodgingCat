package com.dodgingcat.entity;

/** A collectible with a score value. */
public abstract class Food extends GameObject {

    protected Food(double x, double y, double size) {
        super(x, y, size, size);
    }

    public abstract int getScoreValue();
}
