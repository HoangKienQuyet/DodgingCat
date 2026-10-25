package com.dodgingcat.entity;

public final class RobotVacuum extends Obstacle {

    private final double horizontalSpeed;
    private double direction;

    public RobotVacuum(double x, double y, double horizontalSpeed, double direction) {
        super(x, y, 54, 28);
        this.horizontalSpeed = horizontalSpeed;
        this.direction = Math.signum(direction);
    }

    @Override
    public ObstacleKind getKind() {
        return ObstacleKind.ROBOT_VACUUM;
    }

    @Override
    public void update(double deltaSeconds, double worldWidth,
                       double elapsedSeconds, double speedMultiplier) {
        // A single patrol across the room keeps this moving hazard from lasting forever.
        setX(getX() + direction * horizontalSpeed * speedMultiplier * deltaSeconds);
    }
}
