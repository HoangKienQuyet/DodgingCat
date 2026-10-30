package com.dodgingcat.hazards;

import com.dodgingcat.collision.CollisionDetector;
import com.dodgingcat.collision.Hitbox;
import com.dodgingcat.game.Player;

/**
 * Horizontal hazard (laser beam, cannonball, horizontal dart) with pre-firing telegraph warning.
 */
public class HorizontalHazard extends Hazard {
    private final boolean comesFromLeft;
    private final double speed;
    private double warningTimeRemaining;
    private final double totalWarningTime;
    private boolean isFiring = false;
    private final Hitbox hitbox;
    private final boolean isLaserBeam;

    public HorizontalHazard(double y, double height, String subType, boolean comesFromLeft, double warningTime, double speed, boolean isLaserBeam) {
        super(comesFromLeft ? -80.0 : 1000.0, y, isLaserBeam ? 960.0 : 40.0, height, HazardType.HORIZONTAL, subType);
        this.comesFromLeft = comesFromLeft;
        this.totalWarningTime = warningTime;
        this.warningTimeRemaining = warningTime;
        this.speed = speed;
        this.isLaserBeam = isLaserBeam;
        this.hitbox = new Hitbox(0, 0, width, height);
    }

    @Override
    public void update(double dt, double screenWidth, double screenHeight) {
        if (!active) return;

        if (warningTimeRemaining > 0) {
            warningTimeRemaining -= dt;
            if (warningTimeRemaining <= 0) {
                isFiring = true;
                if (!isLaserBeam) {
                    x = comesFromLeft ? 0 : screenWidth;
                }
            }
            return;
        }

        // Firing phase
        if (isLaserBeam) {
            // Laser beam fires for 0.75 seconds then dissipates
            warningTimeRemaining -= dt;
            if (warningTimeRemaining < -0.75) {
                active = false;
            }
        } else {
            // Moving projectile
            x += (comesFromLeft ? 1 : -1) * speed * dt;
            if (comesFromLeft && x > screenWidth + 50) {
                active = false;
            } else if (!comesFromLeft && x < -50) {
                active = false;
            }
        }
    }

    @Override
    public boolean collidesWith(Player player) {
        if (!active || !isFiring || !player.isAlive()) return false;

        if (isLaserBeam) {
            // Full-width horizontal laser line check
            double playerTop = player.getY() + player.getHitbox().getTop(0);
            double playerBottom = player.getY() + player.getHitbox().getBottom(0);
            return y + height >= playerTop && y <= playerBottom;
        } else {
            return hitbox.intersects(x, y, player.getHitbox(), player.getX(), player.getY());
        }
    }

    public boolean isWarning() {
        return warningTimeRemaining > 0;
    }

    public boolean isFiring() {
        return isFiring;
    }

    public double getWarningProgress() {
        return Math.clamp(1.0 - (warningTimeRemaining / totalWarningTime), 0.0, 1.0);
    }

    public boolean isComesFromLeft() {
        return comesFromLeft;
    }

    public boolean isLaserBeam() {
        return isLaserBeam;
    }
}
