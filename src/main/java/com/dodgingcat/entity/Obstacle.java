package com.dodgingcat.entity;

/** Shared obstacle bounds; each subtype supplies its own movement pattern. */
public abstract class Obstacle extends GameObject {

    protected Obstacle(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    public abstract ObstacleKind getKind();

    public abstract void update(double deltaSeconds, double worldWidth,
                                double elapsedSeconds, double speedMultiplier);
}
