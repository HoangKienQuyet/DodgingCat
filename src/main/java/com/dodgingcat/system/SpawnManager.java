package com.dodgingcat.system;

import com.dodgingcat.entity.FallingFlowerPot;
import com.dodgingcat.entity.Food;
import com.dodgingcat.entity.GameObject;
import com.dodgingcat.entity.GoldenFood;
import com.dodgingcat.entity.NormalFood;
import com.dodgingcat.entity.Obstacle;
import com.dodgingcat.entity.PaperPlane;
import com.dodgingcat.entity.Platform;
import com.dodgingcat.entity.RobotVacuum;
import com.dodgingcat.entity.RollingBall;
import com.dodgingcat.entity.Yoyo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Creates occasional food and household obstacles using an injectable RNG. */
public final class SpawnManager {

    public static final double GOLDEN_FOOD_INTERVAL = 12;
    public static final double GOLDEN_FOOD_CHANCE = 0.25;
    public static final double FOOD_SPAWN_CLEARANCE = 52;
    private static final double FALLING_POT_SPEED = 165;
    private static final double PAPER_PLANE_SPEED = 190;
    private static final double YOYO_SPEED = 140;
    private static final double YOYO_AMPLITUDE = 26;
    private static final double YOYO_ANGULAR_SPEED = Math.PI * 1.5;
    private static final double VACUUM_SPEED = 115;
    private static final double ROLLING_BALL_SPEED = 220;
    public static final double PLAYER_SPAWN_CLEARANCE = 120;
    public static final int MAX_ACTIVE_GOLDEN_FOOD = 1;
    public static final int MAX_ACTIVE_OBSTACLES = 5;

    private final RandomGenerator random;
    private final DifficultyManager difficultyManager;
    private final CollisionDetector collisionDetector = new CollisionDetector();
    private double goldenFoodTimer;
    private double obstacleTimer;

    public SpawnManager(RandomGenerator random, DifficultyManager difficultyManager) {
        this.random = Objects.requireNonNull(random, "random generator must not be null");
        this.difficultyManager = Objects.requireNonNull(difficultyManager,
                "difficulty manager must not be null");
    }

    public SpawnBatch update(double deltaSeconds, double elapsedSeconds,
                             double worldWidth, double groundY, List<Platform> platforms,
                             int activeFoodCount, int activeObstacleCount) {
        return update(deltaSeconds, elapsedSeconds, worldWidth, groundY, platforms,
                List.of(), List.of(), null, activeObstacleCount);
    }

    /**
     * Creates spawns for the current world while considering the player and active hazards.
     * The original count-based overload remains for callers that do not own those objects.
     */
    public SpawnBatch update(double deltaSeconds, double elapsedSeconds,
                             double worldWidth, double groundY, List<Platform> platforms,
                             List<Food> activeFoods, List<Obstacle> activeObstacles,
                             GameObject player) {
        return update(deltaSeconds, elapsedSeconds, worldWidth, groundY, platforms,
                activeFoods, activeObstacles, player, activeObstacles.size());
    }

    private SpawnBatch update(double deltaSeconds, double elapsedSeconds,
                              double worldWidth, double groundY, List<Platform> platforms,
                              List<Food> activeFoods, List<Obstacle> activeObstacles,
                              GameObject player, int activeObstacleCount) {
        Objects.requireNonNull(platforms, "platforms must not be null");
        Objects.requireNonNull(activeFoods, "active foods must not be null");
        Objects.requireNonNull(activeObstacles, "active obstacles must not be null");
        goldenFoodTimer += deltaSeconds;
        obstacleTimer += deltaSeconds;
        List<Food> food = new ArrayList<>(1);
        List<Obstacle> obstacles = new ArrayList<>(1);

        if (goldenFoodTimer >= GOLDEN_FOOD_INTERVAL) {
            goldenFoodTimer = 0;
            if (countGoldenFood(activeFoods) < MAX_ACTIVE_GOLDEN_FOOD
                    && random.nextDouble() < GOLDEN_FOOD_CHANCE) {
                food.add(createGoldenFood(worldWidth, groundY, platforms, player,
                        activeObstacles, activeFoods));
            }
        }
        double obstacleInterval = difficultyManager.obstacleSpawnInterval(elapsedSeconds);
        if (obstacleTimer >= obstacleInterval) {
            obstacleTimer = 0;
            if (activeObstacleCount < MAX_ACTIVE_OBSTACLES) {
                Obstacle obstacle = createObstacle(worldWidth, groundY, player);
                if (hasSafeEntrance(obstacle, activeObstacles, player)) {
                    obstacles.add(obstacle);
                }
            }
        }
        return new SpawnBatch(food, obstacles);
    }

    /**
     * Creates the one normal collectible for a run. This method is called only
     * at run creation and immediately after the previous normal food is removed.
     */
    public NormalFood spawnNormalFood(double worldWidth, double groundY, List<Platform> platforms,
                                      GameObject player, List<Obstacle> obstacles,
                                      List<Food> otherFoods, Food previousFood) {
        FoodPosition position = findFoodPosition(NormalFood.SIZE, worldWidth, groundY, platforms,
                player, obstacles, otherFoods, previousFood);
        return new NormalFood(position.x(), position.y());
    }

    private GoldenFood createGoldenFood(double worldWidth, double groundY, List<Platform> platforms,
                                        GameObject player, List<Obstacle> obstacles,
                                        List<Food> existingFoods) {
        FoodPosition position = findFoodPosition(GoldenFood.SIZE, worldWidth, groundY, platforms,
                player, obstacles, existingFoods, null);
        return new GoldenFood(position.x(), position.y());
    }

    private FoodPosition findFoodPosition(double size, double worldWidth, double groundY,
                                          List<Platform> platforms, GameObject player,
                                          List<Obstacle> obstacles, List<Food> existingFoods,
                                          Food previousFood) {
        for (int attempt = 0; attempt < 16; attempt++) {
            FoodPosition position = randomFoodPosition(size, worldWidth, groundY, platforms);
            if (isSafeFoodPosition(position, size, player, obstacles, existingFoods, previousFood)) {
                return position;
            }
        }

        // A deterministic fallback keeps the item in-bounds even when the random attempts fail.
        for (int supportIndex = 0; supportIndex <= platforms.size(); supportIndex++) {
            for (double ratio : new double[]{0.15, 0.5, 0.85}) {
                FoodPosition position = foodPositionOnSupport(size, worldWidth, groundY, platforms,
                        supportIndex, ratio);
                if (isSafeFoodPosition(position, size, player, obstacles, existingFoods, previousFood)) {
                    return position;
                }
            }
        }

        // A standard-sized game area always finds a safe point above. This final position is still
        // inside the playable floor support and avoids an exact previous-food duplicate where possible.
        return foodPositionOnSupport(size, worldWidth, groundY, platforms, platforms.size(), 0.5);
    }

    private FoodPosition randomFoodPosition(double size, double worldWidth, double groundY,
                                            List<Platform> platforms) {
        return foodPositionOnSupport(size, worldWidth, groundY, platforms,
                random.nextInt(platforms.size() + 1), random.nextDouble());
    }

    private FoodPosition foodPositionOnSupport(double size, double worldWidth, double groundY,
                                               List<Platform> platforms, int supportIndex,
                                               double horizontalRatio) {
        double supportX = 0;
        double supportWidth = worldWidth;
        double supportY = groundY;
        if (supportIndex < platforms.size()) {
            Platform platform = platforms.get(supportIndex);
            supportX = platform.x();
            supportWidth = platform.width();
            supportY = platform.y();
        }
        double maxOffset = Math.max(0, supportWidth - size);
        double x = supportX + Math.max(0, Math.min(1, horizontalRatio)) * maxOffset;
        return new FoodPosition(x, supportY - size);
    }

    private boolean isSafeFoodPosition(FoodPosition position, double size, GameObject player,
                                       List<Obstacle> obstacles, List<Food> existingFoods,
                                       Food previousFood) {
        Food candidate = size == NormalFood.SIZE
                ? new NormalFood(position.x(), position.y())
                : new GoldenFood(position.x(), position.y());
        if (player != null && collisionDetector.intersects(candidate, player)) {
            return false;
        }
        if (obstacles.stream().anyMatch(obstacle -> collisionDetector.intersects(candidate, obstacle))) {
            return false;
        }
        if (existingFoods.stream().anyMatch(food -> collisionDetector.intersects(candidate, food))) {
            return false;
        }
        return previousFood == null || distanceBetweenCenters(candidate, previousFood)
                >= FOOD_SPAWN_CLEARANCE;
    }

    private int countGoldenFood(List<Food> foods) {
        return (int) foods.stream().filter(GoldenFood.class::isInstance).count();
    }

    private double distanceBetweenCenters(GameObject first, GameObject second) {
        double xDistance = first.getX() + first.getWidth() / 2 - second.getX() - second.getWidth() / 2;
        double yDistance = first.getY() + first.getHeight() / 2 - second.getY() - second.getHeight() / 2;
        return Math.hypot(xDistance, yDistance);
    }

    private record FoodPosition(double x, double y) {
    }

    private Obstacle createObstacle(double worldWidth, double groundY, GameObject player) {
        return switch (random.nextInt(5)) {
            case 0 -> new FallingFlowerPot(selectSafeFallingPotX(worldWidth, player), -42,
                    FALLING_POT_SPEED);
            case 1 -> createPaperPlane(worldWidth, groundY);
            case 2 -> createYoyo(worldWidth, groundY);
            case 3 -> {
                boolean startsLeft = random.nextBoolean();
                double x = startsLeft ? -54 : worldWidth;
                double direction = startsLeft ? 1 : -1;
                yield new RobotVacuum(x, groundY - 28, VACUUM_SPEED, direction);
            }
            default -> new RollingBall(worldWidth + 10, groundY - 36, ROLLING_BALL_SPEED);
        };
    }

    private PaperPlane createPaperPlane(double worldWidth, double groundY) {
        boolean startsLeft = random.nextBoolean();
        double x = startsLeft ? -PaperPlane.WIDTH - 8 : worldWidth + 8;
        double direction = startsLeft ? 1 : -1;
        double y = groundY - random.nextDouble(120, 220);
        return new PaperPlane(x, y, PAPER_PLANE_SPEED, direction);
    }

    private Yoyo createYoyo(double worldWidth, double groundY) {
        boolean startsLeft = random.nextBoolean();
        double x = startsLeft ? -Yoyo.SIZE - 12 : worldWidth + 12;
        double direction = startsLeft ? 1 : -1;
        double baseY = groundY - random.nextDouble(155, 255);
        return new Yoyo(x, baseY, YOYO_SPEED, direction, YOYO_AMPLITUDE,
                YOYO_ANGULAR_SPEED, random.nextDouble(0, Math.PI * 2));
    }

    private double selectSafeFallingPotX(double worldWidth, GameObject player) {
        double minimumX = 16;
        double maximumX = worldWidth - 54;
        for (int attempt = 0; attempt < 6; attempt++) {
            double candidateX = random.nextDouble(minimumX, maximumX);
            if (hasPlayerClearance(candidateX, 38, player)) {
                return candidateX;
            }
        }
        return player == null || player.getX() + player.getWidth() / 2 < worldWidth / 2
                ? maximumX : minimumX;
    }

    private boolean hasSafeEntrance(Obstacle candidate, List<Obstacle> activeObstacles,
                                     GameObject player) {
        if (player != null && collisionDetector.intersects(candidate, player)) {
            return false;
        }
        return activeObstacles.stream().noneMatch(existing -> collisionDetector.intersects(candidate,
                existing));
    }

    private boolean hasPlayerClearance(double hazardX, double hazardWidth, GameObject player) {
        if (player == null) {
            return true;
        }
        double hazardCenter = hazardX + hazardWidth / 2;
        double playerCenter = player.getX() + player.getWidth() / 2;
        return Math.abs(hazardCenter - playerCenter) >= PLAYER_SPAWN_CLEARANCE;
    }
}
