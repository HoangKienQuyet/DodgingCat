package com.dodgingcat.ui;

import java.util.Optional;

/** Canonical classpath locations for standalone pixel-art PNG resources. */
public final class PixelAssetPaths {

    public static final String ROOT = "/assets/pixel/";

    public static final String ROOM_BACKGROUND = ROOT + "world/cozy-room.png";
    public static final String PLATFORM = ROOT + "platforms/furniture-platform.png";

    public static final String CAT_IDLE = ROOT + "cat/idle.png";
    public static final String CAT_RUN_0 = ROOT + "cat/run-0.png";
    public static final String CAT_RUN_1 = ROOT + "cat/run-1.png";
    public static final String CAT_JUMP = ROOT + "cat/jump.png";
    public static final String CAT_FALL = ROOT + "cat/fall.png";

    public static final String NORMAL_FOOD = ROOT + "food/normal-food.png";
    public static final String GOLDEN_FOOD = ROOT + "food/golden-food.png";

    public static final String FLOWER_POT = ROOT + "hazards/flower-pot.png";
    public static final String PAPER_PLANE = ROOT + "hazards/paper-plane.png";
    public static final String ROBOT_VACUUM = ROOT + "hazards/robot-vacuum.png";
    public static final String ROLLING_BALL = ROOT + "hazards/rolling-ball.png";

    public static final String UI_ICON_LIFE = ROOT + "ui/icons/life.png";
    public static final String UI_ICON_PAUSE = ROOT + "ui/icons/pause.png";
    public static final String UI_ICON_MENU = ROOT + "ui/icons/menu.png";
    public static final String UI_BUTTON_PRIMARY = ROOT + "ui/buttons/primary.png";
    public static final String UI_BUTTON_BLUE = ROOT + "ui/buttons/blue.png";
    public static final String UI_BUTTON_GOLD = ROOT + "ui/buttons/gold.png";
    public static final String UI_BUTTON_PURPLE = ROOT + "ui/buttons/purple.png";
    public static final String UI_BUTTON_NAVY = ROOT + "ui/buttons/navy.png";
    public static final String UI_BUTTON_DANGER = ROOT + "ui/buttons/danger.png";

    private PixelAssetPaths() {
    }

    /** Returns the optional artwork path for a known CSS button color class. */
    public static Optional<String> buttonForStyle(String colorClass) {
        return switch (colorClass) {
            case "primary-button" -> Optional.of(UI_BUTTON_PRIMARY);
            case "blue-button" -> Optional.of(UI_BUTTON_BLUE);
            case "gold-button" -> Optional.of(UI_BUTTON_GOLD);
            case "purple-button" -> Optional.of(UI_BUTTON_PURPLE);
            case "navy-button" -> Optional.of(UI_BUTTON_NAVY);
            case "danger-button" -> Optional.of(UI_BUTTON_DANGER);
            default -> Optional.empty();
        };
    }
}
