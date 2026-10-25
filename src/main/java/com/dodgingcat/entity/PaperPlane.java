package com.dodgingcat.entity;

/** A fast, horizontal hazard whose direction and speed are chosen at spawn time. */
public final class PaperPlane extends Obstacle {

    public static final double WIDTH = 54;
    public static final double HEIGHT = 28;

    private final double horizontalSpeed;
    private final double direction;

    public PaperPlane(double x, double y, double horizontalSpeed, double direction) {
        super(x, y, WIDTH, HEIGHT);
        if (horizontalSpeed <= 0) {
            throw new IllegalArgumentException("Paper plane speed must be positive");
        }
        if (direction == 0) {
            throw new IllegalArgumentException("Paper plane direction must not be zero");
        }
        this.horizontalSpeed = horizontalSpeed;
        this.direction = Math.signum(direction);
    }

    @Override
    public ObstacleKind getKind() {
        return ObstacleKind.PAPER_PLANE;
    }

    @Override
    public void update(double deltaSeconds, double worldWidth,
                       double elapsedSeconds, double speedMultiplier) {
        setX(getX() + direction * horizontalSpeed * speedMultiplier * deltaSeconds);
    }
}
