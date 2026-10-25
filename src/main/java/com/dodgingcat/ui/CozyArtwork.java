package com.dodgingcat.ui;

import com.dodgingcat.util.Constants;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/** Shared original Canvas artwork for the cozy-room presentation. */
public final class CozyArtwork {

    private static final Color NAVY = Color.web("#10243B");
    private static final Color BLUE = Color.web("#183653");
    private static final Color CREAM = Color.web("#FFF0D5");
    private static final Color OUTLINE = Color.web("#172535");

    private CozyArtwork() {
    }

    public static Canvas createMenuBackdrop() {
        Canvas canvas = new Canvas(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        drawMenuBackdrop(canvas.getGraphicsContext2D(), Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        return canvas;
    }

    public static void drawMenuBackdrop(GraphicsContext graphics, double width, double height) {
        double groundY = height - 62;
        drawRoom(graphics, width, height, groundY);
        drawLargeMenuCat(graphics, 90, groundY - 142);
        drawPaw(graphics, 74, 86, 1.15, Color.web("#F6C5B5"));
        drawPaw(graphics, 492, 510, 0.8, Color.web("#F6C5B5"));
    }

    public static void drawRoom(GraphicsContext graphics, double width, double height, double groundY) {
        graphics.setFill(BLUE);
        graphics.fillRect(0, 0, width, height);

        graphics.setFill(Color.web("#214766"));
        graphics.fillRect(18, 20, width - 36, groundY - 20);
        drawWallPanels(graphics, width, groundY);
        drawWindow(graphics, width - 280, 62);
        drawBookcase(graphics, 24, 68, 166, groundY - 92);
        drawFramedArt(graphics, 258, 88, 98, 78, Color.web("#E89966"));
        drawFramedArt(graphics, 388, 128, 72, 61, Color.web("#78B7B2"));
        drawLamp(graphics, width * 0.54, 0);
        drawCouch(graphics, width - 232, groundY - 84);
        drawPlant(graphics, width - 310, groundY - 94, 1.05);
        drawPlant(graphics, 214, groundY - 61, 0.72);
        drawFloor(graphics, width, height, groundY);
    }

    public static void drawCat(GraphicsContext graphics, double x, double y, double scale) {
        graphics.save();
        graphics.translate(x, y);
        graphics.scale(scale, scale);

        graphics.setStroke(OUTLINE);
        graphics.setLineWidth(4);
        graphics.strokeArc(-10, 17, 28, 25, 20, 220, ArcType.OPEN);

        graphics.setFill(Color.web("#303A53"));
        graphics.fillPolygon(new double[]{31, 36, 42}, new double[]{13, 0, 16}, 3);
        graphics.fillPolygon(new double[]{44, 51, 55}, new double[]{15, 1, 20}, 3);
        graphics.fillRoundRect(4, 17, 48, 29, 18, 18);
        graphics.fillOval(29, 8, 27, 27);

        graphics.setFill(Color.web("#F9E9D6"));
        graphics.fillOval(35, 13, 19, 17);
        graphics.fillOval(11, 30, 20, 16);
        graphics.fillOval(37, 38, 13, 9);
        graphics.setFill(Color.web("#F3A8A7"));
        graphics.fillPolygon(new double[]{36, 38, 40}, new double[]{8, 4, 10}, 3);
        graphics.fillPolygon(new double[]{47, 50, 52}, new double[]{9, 5, 13}, 3);

        graphics.setFill(OUTLINE);
        graphics.fillOval(39, 16, 3.5, 5);
        graphics.fillOval(50, 16, 3.5, 5);
        graphics.setFill(Color.web("#F17C87"));
        graphics.fillOval(45, 23, 5, 3.5);
        graphics.setStroke(Color.web("#233247"));
        graphics.setLineWidth(1.2);
        graphics.strokeLine(46, 27, 37, 28);
        graphics.strokeLine(48, 27, 56, 29);

        graphics.setFill(Color.web("#E34E5F"));
        graphics.fillRoundRect(8, 28, 39, 4, 2, 2);
        graphics.setFill(Color.web("#FFD669"));
        graphics.fillOval(27, 28, 6, 6);
        graphics.restore();
    }

    private static void drawWallPanels(GraphicsContext graphics, double width, double groundY) {
        graphics.setStroke(Color.web("#35617E"));
        graphics.setLineWidth(2);
        for (int x = 28; x < width - 20; x += 92) {
            graphics.strokeLine(x, 24, x, groundY - 12);
        }
        graphics.setFill(Color.rgb(255, 216, 138, 0.12));
        graphics.fillOval(width * 0.38, 34, 250, 270);
    }

    private static void drawWindow(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.web("#0E2138"));
        graphics.fillRoundRect(x - 9, y - 9, 206, 172, 12, 12);
        graphics.setFill(Color.web("#D8ECF4"));
        graphics.fillRoundRect(x, y, 188, 154, 7, 7);
        graphics.setFill(Color.web("#75B7D2"));
        graphics.fillRect(x + 8, y + 8, 172, 138);
        graphics.setFill(Color.web("#F7C77A"));
        graphics.fillOval(x + 109, y + 24, 44, 44);
        graphics.setFill(Color.web("#4A7794"));
        graphics.fillPolygon(new double[]{x + 8, x + 65, x + 112, x + 180},
                new double[]{y + 139, y + 85, y + 135, y + 71}, 4);
        graphics.setStroke(CREAM);
        graphics.setLineWidth(5);
        graphics.strokeLine(x + 94, y + 6, x + 94, y + 148);
        graphics.strokeLine(x + 5, y + 77, x + 183, y + 77);
    }

    private static void drawBookcase(GraphicsContext graphics, double x, double y, double width,
                                     double height) {
        graphics.setFill(Color.web("#7D4E45"));
        graphics.fillRoundRect(x, y, width, height, 10, 10);
        graphics.setFill(Color.web("#B77959"));
        graphics.fillRoundRect(x + 9, y + 9, width - 18, height - 18, 6, 6);
        for (int shelf = 1; shelf < 4; shelf++) {
            double shelfY = y + shelf * (height - 12) / 4 + 5;
            graphics.setFill(Color.web("#5E3C39"));
            graphics.fillRect(x + 8, shelfY, width - 16, 7);
        }
        Color[] books = {Color.web("#F49A65"), Color.web("#80C2BF"), Color.web("#F2D06B"),
                Color.web("#8F75B8"), Color.web("#DE6D78")};
        for (int row = 0; row < 4; row++) {
            for (int book = 0; book < 5; book++) {
                double bookX = x + 18 + book * 25;
                double bookY = y + 18 + row * (height - 12) / 4;
                graphics.setFill(books[(row + book) % books.length]);
                graphics.fillRoundRect(bookX, bookY, 15, 36 - (book % 2) * 5, 3, 3);
            }
        }
    }

    private static void drawFramedArt(GraphicsContext graphics, double x, double y, double width,
                                      double height, Color accent) {
        graphics.setFill(Color.web("#E9C996"));
        graphics.fillRoundRect(x, y, width, height, 6, 6);
        graphics.setFill(Color.web("#FFF0D5"));
        graphics.fillRect(x + 7, y + 7, width - 14, height - 14);
        graphics.setFill(accent);
        graphics.fillOval(x + width * 0.32, y + height * 0.28, width * 0.36, height * 0.36);
        graphics.setStroke(Color.web("#5D7190"));
        graphics.strokeLine(x + 12, y + height - 13, x + width - 12, y + 14);
    }

    private static void drawLamp(GraphicsContext graphics, double x, double y) {
        graphics.setStroke(Color.web("#0B192B"));
        graphics.setLineWidth(3);
        graphics.strokeLine(x, y, x, 54);
        graphics.setFill(Color.web("#E18A4D"));
        graphics.fillPolygon(new double[]{x - 42, x + 42, x + 27, x - 27},
                new double[]{54, 54, 80, 80}, 4);
        graphics.setFill(Color.rgb(255, 205, 118, 0.20));
        graphics.fillOval(x - 110, 69, 220, 150);
    }

    private static void drawCouch(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.web("#315D76"));
        graphics.fillRoundRect(x, y, 208, 79, 24, 24);
        graphics.setFill(Color.web("#477E91"));
        graphics.fillRoundRect(x + 20, y - 37, 165, 61, 23, 23);
        graphics.setFill(Color.web("#6096A3"));
        graphics.fillRoundRect(x + 24, y + 17, 73, 43, 14, 14);
        graphics.fillRoundRect(x + 104, y + 17, 76, 43, 14, 14);
        graphics.setFill(Color.web("#1C405A"));
        graphics.fillRect(x + 18, y + 70, 12, 27);
        graphics.fillRect(x + 174, y + 70, 12, 27);
    }

    private static void drawPlant(GraphicsContext graphics, double x, double y, double scale) {
        graphics.save();
        graphics.translate(x, y);
        graphics.scale(scale, scale);
        graphics.setFill(Color.web("#C97955"));
        graphics.fillPolygon(new double[]{0, 42, 35, 8}, new double[]{30, 30, 69, 69}, 4);
        graphics.setFill(Color.web("#5AB27E"));
        graphics.fillOval(-11, -18, 28, 45);
        graphics.fillOval(11, -30, 30, 52);
        graphics.fillOval(28, -13, 29, 40);
        graphics.restore();
    }

    private static void drawFloor(GraphicsContext graphics, double width, double height, double groundY) {
        graphics.setFill(Color.web("#B9785A"));
        graphics.fillRect(0, groundY, width, height - groundY);
        graphics.setStroke(Color.web("#E5A47A"));
        graphics.setLineWidth(2);
        for (int x = -30; x < width; x += 74) {
            graphics.strokeLine(x, groundY, x + 52, height);
        }
        graphics.setFill(Color.web("#805144"));
        graphics.fillRect(0, groundY, width, 8);
    }

    private static void drawLargeMenuCat(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(7, 21, 38, 0.34));
        graphics.fillOval(x - 8, y + 129, 210, 28);
        drawCat(graphics, x, y, 3);
    }

    private static void drawPaw(GraphicsContext graphics, double x, double y, double scale, Color color) {
        graphics.save();
        graphics.translate(x, y);
        graphics.scale(scale, scale);
        graphics.setFill(color);
        graphics.fillOval(7, 14, 28, 25);
        graphics.fillOval(0, 0, 12, 17);
        graphics.fillOval(15, -6, 12, 18);
        graphics.fillOval(30, 0, 12, 17);
        graphics.restore();
    }
}
