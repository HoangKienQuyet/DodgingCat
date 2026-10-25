package com.dodgingcat.entity;

import java.util.List;

/** Player movement and jumping physics, independent of JavaFX rendering. */
public final class Cat extends GameObject {

    private static final double WIDTH = 56;
    private static final double HEIGHT = 46;
    private static final double MOVE_SPEED = 300;
    private static final double GRAVITY = 1_250;
    private static final double JUMP_SPEED = 520;

    private double verticalVelocity;
    private boolean grounded = true;

    public Cat(double x, double groundY) {
        super(x, groundY - HEIGHT, WIDTH, HEIGHT);
    }

    public void update(double deltaSeconds, boolean movingLeft, boolean movingRight,
                       double worldWidth, double groundY, List<Platform> platforms) {
        double direction = (movingRight ? 1 : 0) - (movingLeft ? 1 : 0);
        double nextX = getX() + direction * MOVE_SPEED * deltaSeconds;
        setX(Math.max(0, Math.min(worldWidth - getWidth(), nextX)));

        double previousBottom = getY() + getHeight();
        verticalVelocity += GRAVITY * deltaSeconds;
        setY(getY() + verticalVelocity * deltaSeconds);

        // Platforms are one-way: only a falling cat crossing a platform top can land.
        Platform landingSurface = null;
        if (verticalVelocity >= 0) {
            for (Platform platform : platforms) {
                boolean horizontalOverlap = getX() + getWidth() > platform.x()
                        && getX() < platform.x() + platform.width();
                boolean crossedTop = previousBottom <= platform.y()
                        && getY() + getHeight() >= platform.y();
                if (horizontalOverlap && crossedTop
                        && (landingSurface == null || platform.y() < landingSurface.y())) {
                    landingSurface = platform;
                }
            }
        }

        if (landingSurface != null) {
            setY(landingSurface.y() - getHeight());
            verticalVelocity = 0;
            grounded = true;
            return;
        }
        if (getY() + getHeight() >= groundY) {
            setY(groundY - getHeight());
            verticalVelocity = 0;
            grounded = true;
        } else {
            grounded = false;
        }
    }

    /** Starts a jump only when the cat is standing on a platform or the floor. */
    public boolean jump() {
        if (!grounded) {
            return false;
        }
        grounded = false;
        verticalVelocity = -JUMP_SPEED;
        return true;
    }

    public boolean isGrounded() {
        return grounded;
    }
}
