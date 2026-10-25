package com.dodgingcat.entity;

public final class FallingFlowerPot extends Obstacle {

    private final double fallSpeed;

    public FallingFlowerPot(double x, double y, double fallSpeed) {
        super(x, y, 38, 42);
        this.fallSpeed = fallSpeed;
    }

    @Override
    public ObstacleKind getKind() {
        return ObstacleKind.FLOWER_POT;
    }

    @Override
    public void update(double deltaSeconds, double worldWidth,
                       double elapsedSeconds, double speedMultiplier) {
        setY(getY() + fallSpeed * speedMultiplier * deltaSeconds);
    }
}
