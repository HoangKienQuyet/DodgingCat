package com.dodgingcat.system;

import com.dodgingcat.entity.Platform;
import java.util.List;

/** Defines the fixed, reachable household surfaces for one game area. */
public final class PlatformLayout {

    private PlatformLayout() {
    }

    public static List<Platform> create(double worldWidth, double groundY) {
        if (worldWidth <= 0) {
            throw new IllegalArgumentException("World width must be positive");
        }
        return List.of(
                new Platform(worldWidth * 0.10, groundY - 88, worldWidth * 0.25, 16),
                new Platform(worldWidth * 0.65, groundY - 88, worldWidth * 0.25, 16),
                new Platform(worldWidth * 0.38, groundY - 176, worldWidth * 0.24, 16));
    }
}
