package com.dodgingcat.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlatformTest {

    @Test
    @DisplayName("One-way platform allows jump-through when moving upward")
    void testOneWayJumpThrough() {
        Platform platform = new Platform(100, 300, 200, 16, PlatformType.ONE_WAY);

        // Player moving upward (vy < 0) from below platform
        double prevBottom = 320.0;
        double currBottom = 295.0;
        double vy = -400.0;
        boolean droppingDown = false;

        boolean solid = platform.isSolidToPlayer(prevBottom, currBottom, vy, droppingDown);
        assertFalse(solid, "Platform must allow passing through from underneath when vy < 0");
    }

    @Test
    @DisplayName("One-way platform catches player landing from above")
    void testOneWayLanding() {
        Platform platform = new Platform(100, 300, 200, 16, PlatformType.ONE_WAY);

        // Player falling downward (vy >= 0) and landing on top
        double prevBottom = 290.0;
        double currBottom = 302.0;
        double vy = 200.0;
        boolean droppingDown = false;

        boolean solid = platform.isSolidToPlayer(prevBottom, currBottom, vy, droppingDown);
        assertTrue(solid, "Platform must be solid when landing from above");
    }

    @Test
    @DisplayName("One-way platform lets player drop down when drop-down key is pressed")
    void testOneWayDropDown() {
        Platform platform = new Platform(100, 300, 200, 16, PlatformType.ONE_WAY);

        // Player standing on platform, but holding drop-down
        double prevBottom = 299.0;
        double currBottom = 301.0;
        double vy = 50.0;
        boolean droppingDown = true;

        boolean solid = platform.isSolidToPlayer(prevBottom, currBottom, vy, droppingDown);
        assertFalse(solid, "Platform must not be solid when dropping down");
    }

    @Test
    @DisplayName("Crumbling platform shakes during warning and becomes non-solid when collapsed")
    void testCrumblingPlatform() {
        Platform crumbling = new Platform(100, 250, 150, 16, PlatformType.CRUMBLING);

        crumbling.triggerCrumble(1.0);
        assertTrue(crumbling.isWarning());
        assertFalse(crumbling.isCollapsed());

        // Advance past warning duration
        crumbling.update(1.1);
        assertFalse(crumbling.isWarning());
        assertTrue(crumbling.isCollapsed());

        // While collapsed, platform is completely non-solid
        boolean solid = crumbling.isSolidToPlayer(245.0, 252.0, 100.0, false);
        assertFalse(solid, "Collapsed platform cannot support player");
    }
}
