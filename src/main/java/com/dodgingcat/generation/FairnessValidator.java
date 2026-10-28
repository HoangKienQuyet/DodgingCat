package com.dodgingcat.generation;

import com.dodgingcat.game.Platform;
import com.dodgingcat.game.Player;
import com.dodgingcat.hazards.CannonHazard;
import com.dodgingcat.hazards.Hazard;
import com.dodgingcat.hazards.HazardOrientation;
import com.dodgingcat.hazards.HorizontalHazard;
import com.dodgingcat.hazards.LaserHazard;
import com.dodgingcat.hazards.OrbitalHazard;
import com.dodgingcat.hazards.TopDownHazard;

import java.util.List;

/**
 * Validates procedural hazard candidates against explicit fairness rules before admission.
 */
public class FairnessValidator {
    private final FairnessMetrics metrics;

    public FairnessValidator() {
        this(new FairnessMetrics());
    }

    public FairnessValidator(FairnessMetrics metrics) {
        this.metrics = metrics;
    }

    public FairnessResult validate(Hazard candidate,
                                  double timeSinceLastSpawn,
                                  int currentActiveHazards,
                                  double survivalSeconds,
                                  List<Player> players,
                                  List<Hazard> existingHazards,
                                  List<Platform> platforms) {
        // Rule 1: Minimum Spawn Delay
        if (timeSinceLastSpawn < GenerationRules.MIN_SPAWN_DELAY_BASE) {
            FairnessResult result = FairnessResult.reject("Rule 1: Minimum Spawn Delay Violated");
            metrics.recordCandidate(false, result.rejectionReason());
            return result;
        }

        // Rule 2: On-screen Limit Cap
        int maxAllowed = GenerationRules.getMaxHazardsForTime(survivalSeconds);
        if (currentActiveHazards >= maxAllowed) {
            FairnessResult result = FairnessResult.reject("Rule 2: On-screen Limit Reached (" + currentActiveHazards + "/" + maxAllowed + ")");
            metrics.recordCandidate(false, result.rejectionReason());
            return result;
        }

        if (candidate instanceof OrbitalHazard
                && existingHazards.stream().filter(Hazard::isActive).filter(OrbitalHazard.class::isInstance).count() >= 2) {
            FairnessResult result = FairnessResult.reject("Rule 2: Maximum Active Pendulums Reached (2)");
            metrics.recordCandidate(false, result.rejectionReason());
            return result;
        }

        // Rule 3: Safe-Zone Padding Check
        for (Player player : players) {
            if (!player.isAlive()) continue;

            double pCenterX = player.getX() + player.getSpriteWidth() / 2.0;
            double pCenterY = player.getY() + player.getSpriteHeight() / 2.0;

            if (candidate instanceof TopDownHazard topDown) {
                double dx = Math.abs((topDown.getX() + topDown.getWidth() / 2.0) - pCenterX);
                double dy = pCenterY - (topDown.getY() + topDown.getHeight() / 2.0);
                if (dx < GenerationRules.SAFE_ZONE_PADDING_X && dy > 0 && dy < GenerationRules.SAFE_ZONE_PADDING_Y) {
                    FairnessResult result = FairnessResult.reject("Rule 3: Top-Down Hazard in Player Safe-Zone");
                    metrics.recordCandidate(false, result.rejectionReason());
                    return result;
                }
            } else if (candidate instanceof LaserHazard laser) {
                if (!laser.isWarning()) {
                    FairnessResult result = FairnessResult.reject("Rule 3: Laser Firing without Warning");
                    metrics.recordCandidate(false, result.rejectionReason());
                    return result;
                }
            } else if (candidate instanceof CannonHazard cannon) {
                if (!cannon.isWarning()) {
                    FairnessResult result = FairnessResult.reject("Rule 3: Cannon Firing without Warning");
                    metrics.recordCandidate(false, result.rejectionReason());
                    return result;
                }
            } else if (candidate instanceof HorizontalHazard horiz) {
                double dy = Math.abs((horiz.getY() + horiz.getHeight() / 2.0) - pCenterY);
                if (dy < 24.0 && !horiz.isWarning()) {
                    FairnessResult result = FairnessResult.reject("Rule 3: Horizontal Hazard without Warning");
                    metrics.recordCandidate(false, result.rejectionReason());
                    return result;
                }
            } else if (candidate instanceof OrbitalHazard orbital) {
                double clearance = orbital.getMinimumClearanceFrom(player, 0.75);
                if (clearance < 30.0) {
                    FairnessResult result = FairnessResult.reject("Rule 3: Orbital Path Enters Player Safe-Zone");
                    metrics.recordCandidate(false, result.rejectionReason());
                    return result;
                }
            }
        }

        // Rule 4: Human Reaction Time Floor (< 280ms to impact is unfair)
        for (Player player : players) {
            if (!player.isAlive()) continue;
            if (candidate instanceof TopDownHazard topDown) {
                double distanceY = (player.getY() - (topDown.getY() + topDown.getHeight()));
                if (distanceY > 0 && topDown.getVy() > 0) {
                    double timeToImpact = distanceY / topDown.getVy();
                    if (timeToImpact < GenerationRules.MIN_REACTION_TIME_SECONDS) {
                        FairnessResult result = FairnessResult.reject("Rule 4: Reaction Time Window Under Threshold");
                        metrics.recordCandidate(false, result.rejectionReason());
                        return result;
                    }
                }
            }
        }

        // Rule 5: Density & Escape Corridor
        long warningCount = existingHazards.stream()
                .filter(h -> (h instanceof LaserHazard lh && lh.isWarning()) || (h instanceof CannonHazard ch && ch.isWarning()))
                .count();
        if ((candidate instanceof LaserHazard || candidate instanceof CannonHazard) && warningCount >= 2) {
            FairnessResult result = FairnessResult.reject("Rule 5: Max Warning Channels Reached");
            metrics.recordCandidate(false, result.rejectionReason());
            return result;
        }

        FairnessResult result = FairnessResult.accept();
        metrics.recordCandidate(true, null);
        return result;
    }

    public FairnessMetrics getMetrics() {
        return metrics;
    }
}
