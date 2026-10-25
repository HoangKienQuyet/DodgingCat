package com.dodgingcat.entity;

public final class RollingBall extends Obstacle {

    private final double horizontalSpeed;

    public RollingBall(double x, double y, double horizontalSpeed) {
        super(x, y, 36, 36);
        this.horizontalSpeed = horizontalSpeed;
    }

    @Override
    public ObstacleKind getKind() {
        return ObstacleKind.ROLLING_BALL;
    }

    @Override
    public void update(double deltaSeconds, double worldWidth,
                       double elapsedSeconds, double speedMultiplier) {
        setX(getX() - horizontalSpeed * speedMultiplier * deltaSeconds);
    }
}
