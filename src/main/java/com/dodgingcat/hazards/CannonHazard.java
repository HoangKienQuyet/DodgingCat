package com.dodgingcat.hazards;

import com.dodgingcat.collision.Hitbox;
import com.dodgingcat.game.Player;

/**
 * Universal cannon hazard with pre-firing telegraph aim line, supporting Horizontal and Vertical trajectories.
 */
public class CannonHazard extends Hazard {
    private final HazardOrientation orientation;
    private final boolean positiveDirection; // Right if horizontal, Down if vertical
    private final double speed;
    private final double totalWarningTime;
    private double warningTimeRemaining;
    private boolean firing = false;
    private final Hitbox hitbox;
    private final double caliber;

    public CannonHazard(HazardOrientation orientation, double lanePos, boolean positiveDirection,
                        double warningDuration, double speed, double caliber) {
        super(orientation == HazardOrientation.HORIZONTAL ? (positiveDirection ? -caliber : 960 + caliber) : lanePos - caliber / 2.0,
              orientation == HazardOrientation.HORIZONTAL ? lanePos - caliber / 2.0 : (positiveDirection ? -caliber : 640 + caliber),
              caliber,
              caliber,
              HazardType.HORIZONTAL,
              "cannonball");
        this.orientation = orientation;
        this.positiveDirection = positiveDirection;
        this.totalWarningTime = warningDuration;
        this.warningTimeRemaining = warningDuration;
        this.speed = speed;
        this.caliber = caliber;
        this.hitbox = new Hitbox(0, 0, caliber, caliber);
    }

    @Override
    public void update(double dt, double screenWidth, double screenHeight) {
        if (!active) return;

        if (warningTimeRemaining > 0) {
            warningTimeRemaining -= dt;
            if (warningTimeRemaining <= 0) {
                firing = true;
                if (orientation == HazardOrientation.HORIZONTAL) {
                    x = positiveDirection ? 0 : screenWidth - caliber;
                } else {
                    y = positiveDirection ? 0 : screenHeight - caliber;
                }
            }
            return;
        }

        // Projectile in motion
        if (orientation == HazardOrientation.HORIZONTAL) {
            x += (positiveDirection ? 1 : -1) * speed * dt;
            if (positiveDirection && x > screenWidth + caliber) active = false;
            else if (!positiveDirection && x < -caliber * 2) active = false;
        } else {
            y += (positiveDirection ? 1 : -1) * speed * dt;
            if (positiveDirection && y > screenHeight + caliber) active = false;
            else if (!positiveDirection && y < -caliber * 2) active = false;
        }
    }

    @Override
    public boolean collidesWith(Player player) {
        if (!active || !firing || !player.isAlive()) return false;
        return hitbox.intersects(x, y, player.getHitbox(), player.getX(), player.getY());
    }

    public HazardOrientation getOrientation() {
        return orientation;
    }

    public boolean isPositiveDirection() {
        return positiveDirection;
    }

    public boolean isWarning() {
        return warningTimeRemaining > 0;
    }

    public boolean isFiring() {
        return firing;
    }

    public double getCaliber() {
        return caliber;
    }

    public double getWarningProgress() {
        return Math.clamp(1.0 - (warningTimeRemaining / totalWarningTime), 0.0, 1.0);
    }
}
