package com.dodgingcat.ui;

import com.dodgingcat.entity.Cat;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/** Original in-memory raster sprites drawn at integer scale without smoothing. */
public final class PixelSpriteLibrary {

    private static final int CAT_WIDTH = 28;
    private static final int CAT_HEIGHT = 23;
    private static final Image[] CAT_FRAMES = createCatFrames();
    private static final Image PLATFORM = createPlatform();
    private static final Image NORMAL_FOOD = createNormalFood();
    private static final Image GOLDEN_FOOD = createGoldenFood();
    private static final Image FLOWER_POT = createFlowerPot();
    private static final Image PAPER_PLANE = createPaperPlane();
    private static final Image ROBOT_VACUUM = createRobotVacuum();
    private static final Image ROLLING_BALL = createRollingBall();

    private PixelSpriteLibrary() {
    }

    public static Image cat(Cat cat, double elapsedSeconds) {
        if (!cat.isGrounded()) {
            return CAT_FRAMES[cat.isRising() ? 2 : 3];
        }
        if (cat.isMovingHorizontally()) {
            return CAT_FRAMES[((int) (elapsedSeconds * 10) & 1) == 0 ? 0 : 1];
        }
        return CAT_FRAMES[0];
    }

    public static Image platform() { return PLATFORM; }
    public static Image normalFood() { return NORMAL_FOOD; }
    public static Image goldenFood() { return GOLDEN_FOOD; }
    public static Image flowerPot() { return FLOWER_POT; }
    public static Image paperPlane() { return PAPER_PLANE; }
    public static Image robotVacuum() { return ROBOT_VACUUM; }
    public static Image rollingBall() { return ROLLING_BALL; }

    private static Image[] createCatFrames() {
        Image[] frames = new Image[4];
        for (int frame = 0; frame < frames.length; frame++) {
            WritableImage image = image(CAT_WIDTH, CAT_HEIGHT);
            PixelWriter pixels = image.getPixelWriter();
            Color fur = Color.web("#3A4259");
            Color cream = Color.web("#FFF0D5");
            block(pixels, 9, 1, 4, 5, fur);
            block(pixels, 16, 1, 4, 5, fur);
            block(pixels, 4, 8, 18, 11, fur);
            block(pixels, 14, 4, 10, 10, fur);
            block(pixels, 16, 8, 7, 6, cream);
            block(pixels, 7, 15, 8, 4, cream);
            block(pixels, 16, 18, 5, 3, cream);
            block(pixels, 15, 7, 1, 2, Color.web("#172535"));
            block(pixels, 21, 7, 1, 2, Color.web("#172535"));
            block(pixels, 18, 11, 2, 1, Color.web("#F17C87"));
            block(pixels, 5, 13, 14, 1, Color.web("#D95365"));
            block(pixels, 10, 13, 2, 2, Color.web("#FFD66A"));
            if (frame == 1) {
                block(pixels, 5, 19, 5, 2, fur);
                block(pixels, 16, 20, 5, 2, fur);
            } else if (frame == 2) {
                block(pixels, 4, 19, 5, 2, fur);
                block(pixels, 17, 19, 5, 2, fur);
            } else if (frame == 3) {
                block(pixels, 7, 20, 4, 2, fur);
                block(pixels, 15, 20, 4, 2, fur);
            } else {
                block(pixels, 6, 19, 4, 2, fur);
                block(pixels, 16, 19, 4, 2, fur);
            }
            frames[frame] = image;
        }
        return frames;
    }

    private static Image createPlatform() {
        WritableImage image = image(32, 8);
        PixelWriter pixels = image.getPixelWriter();
        fill(pixels, 32, 8, Color.web("#6B4039"));
        block(pixels, 0, 0, 32, 2, Color.web("#F0B06C"));
        block(pixels, 2, 3, 8, 2, Color.web("#B9704D"));
        block(pixels, 14, 3, 7, 2, Color.web("#C77950"));
        block(pixels, 25, 3, 5, 2, Color.web("#B9704D"));
        return image;
    }

    private static Image createNormalFood() {
        WritableImage image = image(11, 11);
        PixelWriter pixels = image.getPixelWriter();
        block(pixels, 4, 0, 3, 2, Color.web("#63AE68"));
        block(pixels, 2, 2, 7, 7, Color.web("#E7814C"));
        block(pixels, 3, 3, 5, 5, Color.web("#F59A5A"));
        block(pixels, 4, 4, 2, 2, Color.web("#FFE4A9"));
        return image;
    }

    private static Image createGoldenFood() {
        WritableImage image = image(13, 13);
        PixelWriter pixels = image.getPixelWriter();
        block(pixels, 4, 0, 5, 13, Color.web("#F3B738"));
        block(pixels, 0, 4, 13, 5, Color.web("#F3B738"));
        block(pixels, 2, 2, 9, 9, Color.web("#FFD65D"));
        block(pixels, 5, 3, 3, 7, Color.web("#FFF3A9"));
        return image;
    }

    private static Image createFlowerPot() {
        WritableImage image = image(19, 21);
        PixelWriter pixels = image.getPixelWriter();
        block(pixels, 2, 0, 7, 8, Color.web("#4AAA75"));
        block(pixels, 9, 0, 8, 10, Color.web("#62BE7B"));
        block(pixels, 1, 7, 17, 4, Color.web("#F2A06E"));
        block(pixels, 3, 11, 13, 9, Color.web("#C86449"));
        return image;
    }

    private static Image createPaperPlane() {
        WritableImage image = image(27, 14);
        PixelWriter pixels = image.getPixelWriter();
        for (int row = 0; row < 14; row++) {
            int width = 27 - Math.abs(7 - row) * 3;
            block(pixels, 0, row, width, 1, Color.web("#FFF0D5"));
        }
        block(pixels, 8, 8, 12, 1, Color.web("#75BCE4"));
        return image;
    }

    private static Image createRobotVacuum() {
        WritableImage image = image(27, 14);
        PixelWriter pixels = image.getPixelWriter();
        block(pixels, 1, 3, 25, 10, Color.web("#273A4D"));
        block(pixels, 8, 4, 10, 7, Color.web("#7497A9"));
        block(pixels, 11, 6, 4, 3, Color.web("#BCE8EF"));
        block(pixels, 3, 10, 3, 2, Color.web("#F6CC4F"));
        return image;
    }

    private static Image createRollingBall() {
        WritableImage image = image(18, 18);
        PixelWriter pixels = image.getPixelWriter();
        block(pixels, 3, 1, 12, 16, Color.web("#D97356"));
        block(pixels, 1, 4, 16, 10, Color.web("#D97356"));
        block(pixels, 5, 4, 8, 2, Color.web("#F6C09B"));
        block(pixels, 11, 6, 2, 7, Color.web("#F6C09B"));
        return image;
    }

    private static WritableImage image(int width, int height) { return new WritableImage(width, height); }
    private static void fill(PixelWriter pixels, int width, int height, Color color) {
        block(pixels, 0, 0, width, height, color);
    }
    private static void block(PixelWriter pixels, int x, int y, int width, int height, Color color) {
        for (int row = y; row < y + height; row++) {
            for (int column = x; column < x + width; column++) {
                pixels.setColor(column, row, color);
            }
        }
    }
}
