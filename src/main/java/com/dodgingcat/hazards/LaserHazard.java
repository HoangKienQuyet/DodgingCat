package com.dodgingcat.hazards;

import com.dodgingcat.game.Player;

/**
 * Universal laser hazard with telegraph warning line, supporting both Horizontal and Vertical orientations.
 */
public class LaserHazard extends Hazard {
    private final HazardOrientation orientation;
    private double linePos; // Y if Horizontal, X if Vertical
    private final double beamThickness;
    private final double totalWarningTime;
    private double warningTimeRemaining;
    private boolean firing = false;
    private double fireDuration = 0.45;

    public LaserHazard(HazardOrientation orientation, double linePos, double beamThickness, double warningDuration) {
        super(orientation == HazardOrientation.HORIZONTAL ? 0 : linePos - beamThickness / 2.0,
              orientation == HazardOrientation.HORIZONTAL ? linePos - beamThickness / 2.0 : 0,
              orientation == HazardOrientation.HORIZONTAL ? 960.0 : beamThickness,
              orientation == HazardOrientation.HORIZONTAL ? beamThickness : 640.0,
              HazardType.HORIZONTAL,
              "laser");
        this.orientation = orientation;
        this.linePos = linePos;
        this.beamThickness = beamThickness;
        this.totalWarningTime = warningDuration;
        this.warningTimeRemaining = warningDuration;
    }

    @Override
    public void update(double dt, double screenWidth, double screenHeight) {
        if (!active) return;

        if (warningTimeRemaining > 0) {
            warningTimeRemaining -= dt;
            if (warningTimeRemaining <= 0) {
                firing = true;
            }
            return;
        }

        fireDuration -= dt;
        if (fireDuration <= 0) {
            active = false;
        }
    }

    @Override
    public boolean collidesWith(Player player) {
        if (!active || !firing || !player.isAlive()) return false;

        double pLeft = player.getX() + player.getHitbox().getLeft(0);
        double pRight = player.getX() + player.getHitbox().getRight(0);
        double pTop = player.getY() + player.getHitbox().getTop(0);
        double pBottom = player.getY() + player.getHitbox().getBottom(0);

        if (orientation == HazardOrientation.HORIZONTAL) {
            double laserTop = linePos - beamThickness / 2.0;
            double laserBottom = linePos + beamThickness / 2.0;
            return pBottom >= laserTop && pTop <= laserBottom;
        } else {
            double laserLeft = linePos - beamThickness / 2.0;
            double laserRight = linePos + beamThickness / 2.0;
            return pRight >= laserLeft && pLeft <= laserRight;
        }
    }

    public HazardOrientation getOrientation() {
        return orientation;
    }

    public double getLinePos() {
        return linePos;
    }

    public double getBeamThickness() {
        return beamThickness;
    }

    public boolean isWarning() {
        return warningTimeRemaining > 0;
    }

    public boolean isFiring() {
        return firing;
    }

    public double getWarningProgress() {
        return Math.clamp(1.0 - (warningTimeRemaining / totalWarningTime), 0.0, 1.0);
    }

    @Override
    public void scaleHorizontally(double scale, double newScreenWidth) {
        if (orientation == HazardOrientation.HORIZONTAL) {
            width = newScreenWidth;
        } else {
            linePos *= scale;
            x = linePos - beamThickness / 2.0;
        }
    }
}
