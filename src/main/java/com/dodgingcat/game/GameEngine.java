package com.dodgingcat.game;

import com.dodgingcat.entity.Cat;
import com.dodgingcat.entity.Food;
import com.dodgingcat.entity.GoldenFood;
import com.dodgingcat.entity.NormalFood;
import com.dodgingcat.entity.Obstacle;
import com.dodgingcat.entity.Platform;
import com.dodgingcat.system.CollisionDetector;
import com.dodgingcat.system.DifficultyManager;
import com.dodgingcat.system.PlatformLayout;
import com.dodgingcat.system.ScoreManager;
import com.dodgingcat.system.SpawnBatch;
import com.dodgingcat.system.SpawnManager;
import com.dodgingcat.util.Constants;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Owns one play session and coordinates its model systems. */
public final class GameEngine {

    private final double worldWidth;
    private final double worldHeight;
    private final double groundY;
    private final Cat cat;
    private final List<Platform> platforms;
    private final List<Food> foods = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final CollisionDetector collisionDetector = new CollisionDetector();
    private final DifficultyManager difficultyManager = new DifficultyManager();
    private final ScoreManager scoreManager = new ScoreManager();
    private final SpawnManager spawnManager;

    private double elapsedSeconds;
    private boolean gameOver;

    public GameEngine(double worldWidth, double worldHeight, RandomGenerator random) {
        if (worldWidth <= 100 || worldHeight <= 100) {
            throw new IllegalArgumentException("Game area is too small");
        }
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.groundY = worldHeight - Constants.FLOOR_HEIGHT;
        this.platforms = PlatformLayout.create(worldWidth, groundY);
        this.cat = new Cat(worldWidth * 0.14, groundY);
        this.spawnManager = new SpawnManager(
                Objects.requireNonNull(random, "random generator must not be null"), difficultyManager);
        foods.add(spawnManager.spawnNormalFood(worldWidth, groundY, platforms, cat,
                obstacles, List.of(), null));
    }

    public void update(double deltaSeconds, boolean movingLeft, boolean movingRight) {
        if (deltaSeconds < 0) {
            throw new IllegalArgumentException("Frame time cannot be negative");
        }
        if (gameOver || deltaSeconds == 0) {
            return;
        }

        elapsedSeconds += deltaSeconds;
        cat.update(deltaSeconds, movingLeft, movingRight, worldWidth, groundY, platforms);

        SpawnBatch spawned = spawnManager.update(deltaSeconds, elapsedSeconds, worldWidth, groundY,
                platforms, foods, obstacles, cat);
        foods.addAll(spawned.food());
        obstacles.addAll(spawned.obstacles());

        double speedMultiplier = difficultyManager.obstacleSpeedMultiplier(elapsedSeconds);
        obstacles.forEach(obstacle -> obstacle.update(
                deltaSeconds, worldWidth, elapsedSeconds, speedMultiplier));

        Food removedNormalFood = null;
        Iterator<Food> foodIterator = foods.iterator();
        while (foodIterator.hasNext()) {
            Food food = foodIterator.next();
            if (collisionDetector.intersects(cat, food)) {
                scoreManager.addFoodScore(food.getScoreValue());
                if (food instanceof NormalFood) {
                    removedNormalFood = food;
                }
                foodIterator.remove();
                continue;
            }
            if (food instanceof GoldenFood goldenFood) {
                goldenFood.updateLifetime(deltaSeconds);
                if (goldenFood.isExpired()) {
                    foodIterator.remove();
                }
            } else if (food.isOutside(worldWidth, worldHeight)) {
                removedNormalFood = food;
                foodIterator.remove();
            }
        }

        ensureSingleNormalFood(removedNormalFood);

        gameOver = obstacles.stream().anyMatch(obstacle -> collisionDetector.intersects(cat, obstacle));
        obstacles.removeIf(obstacle -> obstacle.isOutside(worldWidth, worldHeight));
    }

    public boolean jump() {
        return !gameOver && cat.jump();
    }

    public Cat getCat() {
        return cat;
    }

    public List<Food> getFoods() {
        return List.copyOf(foods);
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public List<Obstacle> getObstacles() {
        return List.copyOf(obstacles);
    }

    public int getScore() {
        return scoreManager.getScore();
    }

    public int getDifficultyLevel() {
        return difficultyManager.levelAt(elapsedSeconds);
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public double getGroundY() {
        return groundY;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private void ensureSingleNormalFood(Food previousNormalFood) {
        NormalFood activeNormalFood = null;
        Iterator<Food> iterator = foods.iterator();
        while (iterator.hasNext()) {
            Food food = iterator.next();
            if (food instanceof NormalFood normalFood) {
                if (activeNormalFood == null) {
                    activeNormalFood = normalFood;
                } else {
                    iterator.remove();
                }
            }
        }
        if (activeNormalFood == null) {
            foods.add(spawnManager.spawnNormalFood(worldWidth, groundY, platforms, cat,
                    obstacles, foods, previousNormalFood));
        }
    }
}
