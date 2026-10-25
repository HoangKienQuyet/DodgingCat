package com.dodgingcat.ui;

import com.dodgingcat.entity.Food;
import com.dodgingcat.entity.GoldenFood;
import com.dodgingcat.entity.Obstacle;
import com.dodgingcat.entity.Platform;
import com.dodgingcat.game.GameEngine;
import com.dodgingcat.util.Constants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/** Draws the game world only; it does not alter the game model or collision bounds. */
public final class GameCanvasRenderer {

    public void render(GraphicsContext graphics, GameEngine engine) {
        CozyArtwork.drawRoom(graphics, Constants.GAME_WIDTH, Constants.GAME_HEIGHT, engine.getGroundY());
        engine.getPlatforms().forEach(platform -> drawPlatform(graphics, platform));
        engine.getFoods().forEach(food -> drawFood(graphics, food));
        engine.getObstacles().forEach(obstacle -> drawObstacle(graphics, obstacle));
        CozyArtwork.drawCat(graphics, engine.getCat().getX(), engine.getCat().getY(), 1);
    }

    private void drawPlatform(GraphicsContext graphics, Platform platform) {
        double x = platform.x();
        double y = platform.y();
        double width = platform.width();
        double height = platform.height();
        graphics.setFill(Color.web("#553A37"));
        graphics.fillRoundRect(x - 4, y + 6, width + 8, height + 8, 8, 8);
        graphics.setFill(Color.web("#B9704D"));
        graphics.fillRoundRect(x, y, width, height, 7, 7);
        graphics.setFill(Color.web("#F0B06C"));
        graphics.fillRoundRect(x + 4, y + 3, width - 8, 5, 4, 4);
        graphics.setFill(Color.web("#8A4D3F"));
        graphics.fillRect(x + 15, y + height, 11, 21);
        graphics.fillRect(x + width - 26, y + height, 11, 21);
        graphics.setStroke(Color.web("#7B433A"));
        graphics.setLineWidth(1.5);
        for (double seam = x + 34; seam < x + width - 18; seam += 38) {
            graphics.strokeLine(seam, y + 3, seam - 7, y + height - 2);
        }
    }

    private void drawFood(GraphicsContext graphics, Food food) {
        double x = food.getX();
        double y = food.getY();
        if (food instanceof GoldenFood) {
            graphics.setFill(Color.rgb(255, 213, 88, 0.25));
            graphics.fillOval(x - 8, y - 8, food.getWidth() + 16, food.getHeight() + 16);
            graphics.setFill(Color.web("#F3B738"));
            graphics.fillOval(x, y, food.getWidth(), food.getHeight());
            drawStar(graphics, x + food.getWidth() / 2, y + food.getHeight() / 2, 8, Color.web("#FFF3A9"));
            return;
        }
        graphics.setFill(Color.web("#E7814C"));
        graphics.fillOval(x, y + 3, food.getWidth(), food.getHeight() - 3);
        graphics.setFill(Color.web("#70B86A"));
        graphics.fillOval(x + food.getWidth() / 2 - 2, y, 7, 8);
        graphics.setFill(Color.web("#FFE4A9"));
        graphics.fillOval(x + 5, y + 9, 6, 6);
    }

    private void drawObstacle(GraphicsContext graphics, Obstacle obstacle) {
        double x = obstacle.getX();
        double y = obstacle.getY();
        switch (obstacle.getKind()) {
            case FLOWER_POT -> drawFlowerPot(graphics, x, y);
            case PAPER_PLANE -> drawPaperPlane(graphics, obstacle);
            case YOYO -> drawYoyo(graphics, obstacle);
            case ROBOT_VACUUM -> drawRobotVacuum(graphics, obstacle);
            case ROLLING_BALL -> drawRollingBall(graphics, obstacle);
        }
    }

    private void drawFlowerPot(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.web("#4AAA75"));
        graphics.fillOval(x + 3, y - 13, 17, 25);
        graphics.fillOval(x + 18, y - 18, 19, 28);
        graphics.setFill(Color.web("#D26E4D"));
        graphics.fillPolygon(new double[]{x + 2, x + 37, x + 31, x + 8},
                new double[]{y + 10, y + 10, y + 41, y + 41}, 4);
        graphics.setFill(Color.web("#F2A06E"));
        graphics.fillRect(x + 4, y + 10, 31, 5);
    }

    private void drawPaperPlane(GraphicsContext graphics, Obstacle obstacle) {
        double x = obstacle.getX();
        double y = obstacle.getY();
        graphics.setFill(Color.web("#FFF4DE"));
        graphics.fillPolygon(new double[]{x, x + obstacle.getWidth(), x + 15},
                new double[]{y + obstacle.getHeight() / 2, y, y + obstacle.getHeight()}, 3);
        graphics.setStroke(Color.web("#75BCE4"));
        graphics.setLineWidth(2.5);
        graphics.strokeLine(x + 15, y + obstacle.getHeight(), x + 24, y + obstacle.getHeight() / 2);
        graphics.strokeLine(x + 24, y + obstacle.getHeight() / 2, x + obstacle.getWidth(), y);
    }

    private void drawYoyo(GraphicsContext graphics, Obstacle obstacle) {
        double x = obstacle.getX();
        double y = obstacle.getY();
        double centerX = x + obstacle.getWidth() / 2;
        graphics.setStroke(Color.web("#F5D9A7"));
        graphics.setLineWidth(3);
        graphics.strokeLine(centerX, y - 52, centerX, y + 7);
        graphics.setFill(Color.web("#D95365"));
        graphics.fillOval(x, y, obstacle.getWidth(), obstacle.getHeight());
        graphics.setFill(Color.web("#F58D8A"));
        graphics.fillOval(x + 10, y + 10, obstacle.getWidth() - 20, obstacle.getHeight() - 20);
        drawStar(graphics, centerX, y + obstacle.getHeight() / 2, 9, Color.web("#FFE066"));
    }

    private void drawRobotVacuum(GraphicsContext graphics, Obstacle obstacle) {
        double x = obstacle.getX();
        double y = obstacle.getY();
        graphics.setFill(Color.web("#273A4D"));
        graphics.fillRoundRect(x, y + 2, obstacle.getWidth(), obstacle.getHeight() - 2, 16, 16);
        graphics.setFill(Color.web("#7497A9"));
        graphics.fillOval(x + 18, y + 6, 18, 17);
        graphics.setFill(Color.web("#BCE8EF"));
        graphics.fillOval(x + 23, y + 10, 8, 8);
        graphics.setFill(Color.web("#F6CC4F"));
        graphics.fillOval(x + 7, y + 18, 6, 5);
    }

    private void drawRollingBall(GraphicsContext graphics, Obstacle obstacle) {
        double x = obstacle.getX();
        double y = obstacle.getY();
        graphics.setFill(Color.web("#D97356"));
        graphics.fillOval(x, y, obstacle.getWidth(), obstacle.getHeight());
        graphics.setStroke(Color.web("#F6C09B"));
        graphics.setLineWidth(3);
        graphics.strokeArc(x + 5, y + 5, obstacle.getWidth() - 10,
                obstacle.getHeight() - 10, 35, 205, ArcType.OPEN);
    }

    private void drawStar(GraphicsContext graphics, double centerX, double centerY, double radius,
                          Color color) {
        double[] xPoints = new double[10];
        double[] yPoints = new double[10];
        for (int index = 0; index < 10; index++) {
            double pointRadius = index % 2 == 0 ? radius : radius * 0.45;
            double angle = -Math.PI / 2 + index * Math.PI / 5;
            xPoints[index] = centerX + Math.cos(angle) * pointRadius;
            yPoints[index] = centerY + Math.sin(angle) * pointRadius;
        }
        graphics.setFill(color);
        graphics.fillPolygon(xPoints, yPoints, xPoints.length);
    }
}
