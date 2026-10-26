package com.dodgingcat.system;

import com.dodgingcat.entity.Cat;
import com.dodgingcat.entity.FallingFlowerPot;
import com.dodgingcat.entity.Food;
import com.dodgingcat.entity.GameObject;
import com.dodgingcat.entity.GoldenFood;
import com.dodgingcat.entity.NormalFood;
import com.dodgingcat.entity.ObstacleKind;
import com.dodgingcat.entity.PaperPlane;
import com.dodgingcat.entity.Platform;
import com.dodgingcat.entity.RobotVacuum;
import com.dodgingcat.entity.RollingBall;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreSystemsTest {

    @Test
    void scoreCountsCollectedFoodOnly() {
        ScoreManager score = new ScoreManager();
        score.addFoodScore(new NormalFood(0, 0).getScoreValue());
        score.addFoodScore(new GoldenFood(0, 0).getScoreValue());

        assertEquals(60, score.getScore());
    }

    @Test
    void collisionUsesRectangleOverlapAndAllowsEdgeContact() {
        CollisionDetector detector = new CollisionDetector();
        GameObject first = new TestObject(10, 10, 20, 20);

        assertTrue(detector.intersects(first, new TestObject(25, 25, 20, 20)));
        assertFalse(detector.intersects(first, new TestObject(30, 10, 10, 10)));
    }

    @Test
    void movingObjectsAreRemovedAfterLeavingEitherSideOfTheRoom() {
        RobotVacuum vacuum = new RobotVacuum(80, 100, 115, 1);
        vacuum.update(1, 100, 1, 1);

        assertTrue(vacuum.isOutside(100, 200));
    }

    @Test
    void paperPlaneUsesItsConfiguredHorizontalSpeedAndDirection() {
        PaperPlane plane = new PaperPlane(300, 220, 120, -1);

        plane.update(0.5, 900, 1, 1.5);

        assertEquals(210, plane.getX(), 0.001);
        assertEquals(220, plane.getY(), 0.001);
    }

    @Test
    void eachRemainingHazardUsesTheSameLethalCollisionRule() {
        CollisionDetector detector = new CollisionDetector();
        Cat cat = new Cat(100, 500);

        assertTrue(detector.intersects(cat, new PaperPlane(105, 460, 120, -1)));
        assertTrue(detector.intersects(cat, new RobotVacuum(100, 460, 115, 1)));
        assertTrue(detector.intersects(cat, new RollingBall(110, 460, 220)));
    }

    @Test
    void obstacleCatalogContainsExactlyTheFourSupportedHazardTypes() {
        assertEquals(Set.of(ObstacleKind.FLOWER_POT, ObstacleKind.PAPER_PLANE,
                        ObstacleKind.ROBOT_VACUUM, ObstacleKind.ROLLING_BALL),
                Set.of(ObstacleKind.values()));
    }

    @Test
    void catMovesWithinBoundsAndCanJumpOnlyOnceBeforeLanding() {
        Cat cat = new Cat(10, 300);
        cat.update(2, false, true, 500, 300, java.util.List.of());
        assertEquals(500 - cat.getWidth(), cat.getX());

        assertTrue(cat.jump());
        cat.update(0.1, true, false, 500, 300, java.util.List.of());
        assertTrue(cat.getY() < 300 - cat.getHeight());
        assertFalse(cat.jump());
        cat.update(1, false, false, 500, 300, java.util.List.of());
        assertTrue(cat.isGrounded());
        assertEquals(300 - cat.getHeight(), cat.getY());
    }

    @Test
    void catLandsOnFurnitureAndCanJumpAgain() {
        Cat cat = new Cat(100, 500);
        Platform table = new Platform(50, 410, 220, 16);
        assertTrue(cat.jump());

        for (int frame = 0; frame < 8; frame++) {
            cat.update(0.05, false, false, 500, 500, List.of(table));
        }
        assertTrue(cat.getY() + cat.getHeight() < table.y(),
                "The cat should jump upward through the one-way platform");

        for (int frame = 0; frame < 30 && !cat.isGrounded(); frame++) {
            cat.update(0.05, false, false, 500, 500, List.of(table));
        }

        assertTrue(cat.isGrounded());
        assertEquals(table.y() - cat.getHeight(), cat.getY(), 0.001);
        assertTrue(cat.jump());
    }

    @Test
    void difficultyRisesAtDefinedSurvivalMilestones() {
        DifficultyManager difficulty = new DifficultyManager();

        assertEquals(1, difficulty.levelAt(24.9));
        assertEquals(2, difficulty.levelAt(25));
        assertTrue(difficulty.obstacleSpeedMultiplier(25)
                > difficulty.obstacleSpeedMultiplier(0));
        assertTrue(difficulty.obstacleSpawnInterval(25)
                < difficulty.obstacleSpawnInterval(0));
    }

    @Test
    void spawnManagerUsesItsTimerForGoldenFoodOnlyAndStillCreatesHazards() {
        DifficultyManager difficulty = new DifficultyManager();
        SpawnManager spawner = new SpawnManager(new Random(42), difficulty);

        List<Platform> platforms = List.of(new Platform(200, 360, 220, 16));
        SpawnBatch early = spawner.update(1, 1, 900, 480, platforms, 0, 0);
        assertTrue(early.food().isEmpty());
        assertTrue(early.obstacles().isEmpty());

        SpawnBatch due = spawner.update(12, 12, 900, 480, platforms, 0, 0);

        assertEquals(1, due.obstacles().size());
        assertTrue(due.obstacles().getFirst().getY() < 480);
        assertTrue(due.obstacles().getFirst().getY() + due.obstacles().getFirst().getHeight() >= -50);
        assertTrue(due.food().stream().allMatch(GoldenFood.class::isInstance));
    }

    @Test
    void spawnerDoesNotExceedConfiguredActiveObjectLimits() {
        SpawnManager spawner = new SpawnManager(new Random(10), new DifficultyManager());
        SpawnBatch full = spawner.update(3.5, 3.5, 900, 480, java.util.List.of(),
                1, SpawnManager.MAX_ACTIVE_OBSTACLES);

        assertTrue(full.food().isEmpty());
        assertTrue(full.obstacles().isEmpty());
    }

    @Test
    void fallingPotsSpawnAwayFromTheCurrentCatPosition() {
        SpawnManager spawner = new SpawnManager(new FixedRandom(), new DifficultyManager());
        Cat cat = new Cat(420, 480);

        SpawnBatch batch = spawner.update(2.7, 2.7, 900, 480, List.of(), List.of(),
                List.of(), cat);

        assertEquals(1, batch.obstacles().size());
        assertTrue(batch.obstacles().getFirst() instanceof FallingFlowerPot);
        var pot = batch.obstacles().getFirst();
        double potCenter = pot.getX() + pot.getWidth() / 2;
        double catCenter = cat.getX() + cat.getWidth() / 2;
        assertTrue(Math.abs(potCenter - catCenter) >= SpawnManager.PLAYER_SPAWN_CLEARANCE);
    }

    @Test
    void foodAppearsOnPlatformsAsWellAsTheFloor() {
        SpawnManager spawner = new SpawnManager(new Random(42), new DifficultyManager());
        Platform shelf = new Platform(200, 360, 220, 16);
        boolean foundShelfFood = false;

        for (int spawn = 0; spawn < 20; spawn++) {
            Food food = spawner.spawnNormalFood(900, 480, List.of(shelf), null, List.of(),
                    List.of(), null);
            double bottom = food.getY() + food.getHeight();
            assertTrue(Math.abs(bottom - 360) < 0.001 || Math.abs(bottom - 480) < 0.001);
            if (Math.abs(bottom - 360) < 0.001) {
                foundShelfFood = true;
                assertTrue(food.getX() >= shelf.x());
                assertTrue(food.getX() + food.getWidth() <= shelf.x() + shelf.width());
            }
        }

        assertTrue(foundShelfFood, "Seeded spawns should place food on the shelf");
    }

    @Test
    void normalFoodSpawnAvoidsTheCatObstaclesAndPreviousFoodPosition() {
        SpawnManager spawner = new SpawnManager(new Random(8), new DifficultyManager());
        Cat cat = new Cat(360, 480);
        Food previousFood = new NormalFood(260, 458);
        FallingFlowerPot obstacle = new FallingFlowerPot(430, 438, 100);

        NormalFood food = spawner.spawnNormalFood(900, 480, List.of(), cat, List.of(obstacle),
                List.of(), previousFood);
        CollisionDetector detector = new CollisionDetector();

        assertFalse(detector.intersects(food, cat));
        assertFalse(detector.intersects(food, obstacle));
        assertTrue(food.getX() >= 0);
        assertTrue(food.getX() + food.getWidth() <= 900);
        assertEquals(480, food.getY() + food.getHeight(), 0.001);
        assertTrue(Math.hypot(food.getX() - previousFood.getX(), food.getY() - previousFood.getY())
                >= SpawnManager.FOOD_SPAWN_CLEARANCE);
    }

    private static final class TestObject extends GameObject {
        private TestObject(double x, double y, double width, double height) {
            super(x, y, width, height);
        }
    }

    private static final class FixedRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public double nextDouble(double origin, double bound) {
            return origin + (bound - origin) / 2;
        }
    }
}
