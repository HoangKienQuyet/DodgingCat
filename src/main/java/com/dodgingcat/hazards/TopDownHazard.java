package com.dodgingcat.hazards;

import com.dodgingcat.collision.Hitbox;
import com.dodgingcat.game.Player;

/**
 * Top-down falling hazard customized for specific map environments.
 */
public class TopDownHazard extends Hazard {
    private double vy;
    private final double gravity;
    private final Hitbox hitbox;
    private double rotationAngle = 0.0;
    private final double rotationSpeed;

    public TopDownHazard(double x, double startY, double width, double height, String subType, double initialVy, double gravity) {
        super(x, startY, width, height, HazardType.TOP_DOWN, subType);
        this.vy = initialVy;
        this.gravity = gravity;
        double widthFactor = switch (subType) {
            // Thin, elongated tools should not hit with the empty corners of their
            // sprite rectangle, especially while rotating.
            case "knife", "trowel", "antenna", "weather_vane" -> 0.62;
            case "remote_control", "mirror_shard", "salt_shaker" -> 0.70;
            // Broad soft objects occupy more of their artwork and can use a wider hitbox.
            case "frying_pan", "cushion", "pillow", "watermelon", "satellite_dish" -> 0.86;
            default -> 0.78;
        };
        double heightFactor = switch (subType) {
            case "knife", "trowel", "antenna", "weather_vane", "lightning" -> 0.82;
            case "frying_pan", "book", "cushion", "pillow", "brick" -> 0.76;
            default -> 0.80;
        };
        this.hitbox = new Hitbox(width * (1.0 - widthFactor) / 2.0,
                height * (1.0 - heightFactor) / 2.0,
                width * widthFactor, height * heightFactor);
        this.rotationSpeed = (subType.equals("anvil") || subType.equals("plant_pot") || subType.equals("blender") || subType.equals("satellite_dish")) ? 0.0 : 180.0;
    }

    @Override
    public void update(double dt, double screenWidth, double screenHeight) {
        if (!active) return;

        vy += gravity * dt;
        y += vy * dt;
        rotationAngle += rotationSpeed * dt;

        if (y > screenHeight + 50.0) {
            active = false;
        }
    }

    @Override
    public boolean collidesWith(Player player) {
        if (!active || !player.isAlive()) return false;
        return hitbox.intersects(x, y, player.getHitbox(), player.getX(), player.getY());
    }

    public double getRotationAngle() {
        return rotationAngle;
    }

    public double getVy() {
        return vy;
    }
}
