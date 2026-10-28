package com.dodgingcat.hazards;

import com.dodgingcat.collision.Hitbox;
import com.dodgingcat.game.Player;

/**
 * Orbital / pendulum hazard consisting of a chained swinging weight attached to a ceiling/platform anchor.
 */
public class OrbitalHazard extends Hazard {
    private double anchorX;
    private final double anchorY;
    private double chainLength;
    private final double maxAngle; // radians
    private final double angularFrequency;
    private double currentAngle;
    private double timeElapsed = 0.0;
    private final double weightRadius;
    private double lifeDuration;
    private final boolean circularOrbit;
    private boolean swingSoundEvent;

    public OrbitalHazard(double anchorX, double anchorY, double chainLength, double maxAngleDegrees, double frequency, double weightRadius, double lifeDuration) {
        this(anchorX, anchorY, chainLength, maxAngleDegrees, frequency, weightRadius, lifeDuration, false);
    }

    public OrbitalHazard(double anchorX, double anchorY, double chainLength, double maxAngleDegrees, double frequency,
                         double weightRadius, double lifeDuration, boolean circularOrbit) {
        super(anchorX, anchorY, weightRadius * 2, weightRadius * 2, HazardType.ORBITAL, "pendulum");
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.chainLength = chainLength;
        this.maxAngle = Math.toRadians(maxAngleDegrees);
        this.angularFrequency = frequency;
        this.weightRadius = weightRadius;
        this.lifeDuration = lifeDuration;
        this.circularOrbit = circularOrbit;
        this.currentAngle = this.maxAngle;
        recalculatePosition();
    }

    private void recalculatePosition() {
        if (circularOrbit) {
            currentAngle = angularFrequency * timeElapsed;
            this.x = anchorX + chainLength * Math.cos(currentAngle) - weightRadius;
            this.y = anchorY + chainLength * Math.sin(currentAngle) - weightRadius;
        } else {
            currentAngle = maxAngle * Math.cos(angularFrequency * timeElapsed);
            this.x = anchorX + chainLength * Math.sin(currentAngle) - weightRadius;
            this.y = anchorY + chainLength * Math.cos(currentAngle) - weightRadius;
        }
    }

    @Override
    public void update(double dt, double screenWidth, double screenHeight) {
        if (!active) return;

        double previousAngle = currentAngle;
        timeElapsed += dt;
        lifeDuration -= dt;
        if (lifeDuration <= 0) {
            active = false;
            return;
        }

        recalculatePosition();
        if (circularOrbit) {
            // Play once when the weight enters the lower semicircle.
            swingSoundEvent = Math.sin(previousAngle) <= 0.0 && Math.sin(currentAngle) > 0.0;
        } else {
            // The pendulum crosses its lowest point once per one-way sweep.
            swingSoundEvent = previousAngle * currentAngle < 0.0;
        }
    }

    @Override
    public boolean collidesWith(Player player) {
        if (!active || !player.isAlive()) return false;

        double weightCenterX = x + weightRadius;
        double weightCenterY = y + weightRadius;

        // Check collision with the swinging weight ball
        Hitbox pHitbox = player.getHitbox();
        return pHitbox.intersectsCircle(player.getX(), player.getY(), weightCenterX, weightCenterY, weightRadius);
    }

    public double getAnchorX() {
        return anchorX;
    }

    @Override
    public void scaleHorizontally(double scale, double newScreenWidth) {
        anchorX *= scale;
        recalculatePosition();
    }

    public double getAnchorY() {
        return anchorY;
    }

    public double getWeightCenterX() {
        return x + weightRadius;
    }

    public double getWeightCenterY() {
        return y + weightRadius;
    }

    public double getWeightRadius() {
        return weightRadius;
    }

    public double getCurrentAngle() {
        return currentAngle;
    }

    public boolean isCircularOrbit() {
        return circularOrbit;
    }

    public boolean consumeSwingSoundEvent() {
        boolean event = swingSoundEvent;
        swingSoundEvent = false;
        return event;
    }

    /** Minimum distance between the moving weight and a player's center over a short reaction window. */
    public double getMinimumClearanceFrom(Player player, double reactionWindowSeconds) {
        double playerCenterX = player.getX() + player.getSpriteWidth() / 2.0;
        double playerCenterY = player.getY() + player.getSpriteHeight() / 2.0;
        double playerRadius = Math.hypot(player.getSpriteWidth(), player.getSpriteHeight()) / 2.0;
        double minimumClearance = Double.POSITIVE_INFINITY;
        int samples = 16;

        for (int i = 0; i <= samples; i++) {
            double sampleTime = timeElapsed + reactionWindowSeconds * i / samples;
            double angle = circularOrbit
                    ? angularFrequency * sampleTime
                    : maxAngle * Math.cos(angularFrequency * sampleTime);
            double weightX = circularOrbit
                    ? anchorX + chainLength * Math.cos(angle)
                    : anchorX + chainLength * Math.sin(angle);
            double weightY = circularOrbit
                    ? anchorY + chainLength * Math.sin(angle)
                    : anchorY + chainLength * Math.cos(angle);

            double clearance = Math.hypot(weightX - playerCenterX, weightY - playerCenterY)
                    - playerRadius - weightRadius;
            minimumClearance = Math.min(minimumClearance, clearance);
        }
        return minimumClearance;
    }
}
