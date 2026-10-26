package com.dodgingcat.ui;

import com.dodgingcat.entity.Food;
import com.dodgingcat.entity.GoldenFood;
import com.dodgingcat.entity.Obstacle;
import com.dodgingcat.entity.Platform;
import com.dodgingcat.game.GameEngine;
import com.dodgingcat.util.Constants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/** Draws the game world only; it does not alter the game model or collision bounds. */
public final class GameCanvasRenderer {

    private final ClasspathImageLoader assets = new ClasspathImageLoader();

    public void render(GraphicsContext graphics, GameEngine engine) {
        graphics.setImageSmoothing(false);
        drawRoom(graphics, engine);
        engine.getPlatforms().forEach(platform -> drawPlatform(graphics, platform));
        engine.getFoods().forEach(food -> drawFood(graphics, food));
        engine.getObstacles().forEach(obstacle -> drawObstacle(graphics, obstacle));
        drawCat(graphics, engine);
    }

    private void drawRoom(GraphicsContext graphics, GameEngine engine) {
        assets.loadPng(PixelAssetPaths.ROOM_BACKGROUND).ifPresentOrElse(
                image -> graphics.drawImage(image, 0, 0, Constants.GAME_WIDTH, Constants.GAME_HEIGHT),
                () -> CozyArtwork.drawRoom(graphics, Constants.GAME_WIDTH, Constants.GAME_HEIGHT,
                        engine.getGroundY()));
    }

    private void drawPlatform(GraphicsContext graphics, Platform platform) {
        Image sprite = assets.loadPng(PixelAssetPaths.PLATFORM)
                .orElseGet(PixelSpriteLibrary::platform);
        graphics.drawImage(sprite, platform.x(), platform.y(),
                platform.width(), platform.height());
    }

    private void drawFood(GraphicsContext graphics, Food food) {
        Image sprite = food instanceof GoldenFood
                ? assets.loadPng(PixelAssetPaths.GOLDEN_FOOD)
                        .orElseGet(PixelSpriteLibrary::goldenFood)
                : assets.loadPng(PixelAssetPaths.NORMAL_FOOD)
                        .orElseGet(PixelSpriteLibrary::normalFood);
        graphics.drawImage(sprite, food.getX(), food.getY(), food.getWidth(), food.getHeight());
    }

    private void drawObstacle(GraphicsContext graphics, Obstacle obstacle) {
        Image sprite = switch (obstacle.getKind()) {
            case FLOWER_POT -> assets.loadPng(PixelAssetPaths.FLOWER_POT)
                    .orElseGet(PixelSpriteLibrary::flowerPot);
            case PAPER_PLANE -> assets.loadPng(PixelAssetPaths.PAPER_PLANE)
                    .orElseGet(PixelSpriteLibrary::paperPlane);
            case ROBOT_VACUUM -> assets.loadPng(PixelAssetPaths.ROBOT_VACUUM)
                    .orElseGet(PixelSpriteLibrary::robotVacuum);
            case ROLLING_BALL -> assets.loadPng(PixelAssetPaths.ROLLING_BALL)
                    .orElseGet(PixelSpriteLibrary::rollingBall);
        };
        graphics.drawImage(sprite, obstacle.getX(), obstacle.getY(), obstacle.getWidth(),
                obstacle.getHeight());
    }

    private void drawCat(GraphicsContext graphics, GameEngine engine) {
        double x = engine.getCat().getX();
        double y = engine.getCat().getY();
        Image sprite = assets.loadPng(catPath(engine)).orElseGet(
                () -> PixelSpriteLibrary.cat(engine.getCat(), engine.getElapsedSeconds()));
        if (engine.getCat().getFacingDirection() < 0) {
            graphics.save();
            graphics.translate(x + engine.getCat().getWidth(), y);
            graphics.scale(-1, 1);
            graphics.drawImage(sprite, 0, 0,
                    engine.getCat().getWidth(), engine.getCat().getHeight());
            graphics.restore();
            return;
        }
        graphics.drawImage(sprite, x, y,
                engine.getCat().getWidth(), engine.getCat().getHeight());
    }

    private String catPath(GameEngine engine) {
        if (!engine.getCat().isGrounded()) {
            return engine.getCat().isRising() ? PixelAssetPaths.CAT_JUMP : PixelAssetPaths.CAT_FALL;
        }
        if (engine.getCat().isMovingHorizontally()) {
            return ((int) (engine.getElapsedSeconds() * 10) & 1) == 0
                    ? PixelAssetPaths.CAT_RUN_0 : PixelAssetPaths.CAT_RUN_1;
        }
        return PixelAssetPaths.CAT_IDLE;
    }

}
