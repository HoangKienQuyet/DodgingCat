package com.dodgingcat.entity;

/**
 * A large swinging hazard. Its position is derived from elapsed time, so its
 * path remains consistent regardless of rendering frequency.
 */
public final class Yoyo extends Obstacle {

    public static final double SIZE = 64;

    private final double horizontalSpeed;
    private final double direction;
    private final double baseY;
    private final double verticalAmplitude;
    private final double angularSpeed;
    private final double phase;

    public Yoyo(double x, double baseY, double horizontalSpeed, double direction,
                double verticalAmplitude, double angularSpeed, double phase) {
        super(x, baseY, SIZE, SIZE);
        if (horizontalSpeed <= 0) {
            throw new IllegalArgumentException("Yoyo speed must be positive");
        }
        if (direction == 0) {
            throw new IllegalArgumentException("Yoyo direction must not be zero");
        }
        if (verticalAmplitude < 0 || angularSpeed <= 0) {
            throw new IllegalArgumentException("Yoyo movement range and rate must be positive");
        }
        this.horizontalSpeed = horizontalSpeed;
        this.direction = Math.signum(direction);
        this.baseY = baseY;
        this.verticalAmplitude = verticalAmplitude;
        this.angularSpeed = angularSpeed;
        this.phase = phase;
    }

    @Override
    public ObstacleKind getKind() {
        return ObstacleKind.YOYO;
    }

    @Override
    public void update(double deltaSeconds, double worldWidth,
                       double elapsedSeconds, double speedMultiplier) {
        setX(getX() + direction * horizontalSpeed * speedMultiplier * deltaSeconds);
        setY(baseY + Math.sin(elapsedSeconds * angularSpeed + phase) * verticalAmplitude);
    }
}
