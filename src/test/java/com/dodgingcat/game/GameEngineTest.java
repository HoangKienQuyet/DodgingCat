package com.dodgingcat.game;

import com.dodgingcat.entity.Food;
import com.dodgingcat.entity.GoldenFood;
import com.dodgingcat.entity.NormalFood;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEngineTest {

    @Test
    void newRunStartsWithExactlyOneNormalFood() {
        GameEngine game = new GameEngine(900, 540, new Random(7));

        assertEquals(1, normalFoodCount(game));
        assertEquals(1, game.getFoods().size());
    }

    @Test
    void waitingDoesNotCreateAdditionalNormalFood() {
        GameEngine game = new GameEngine(900, 540, new SafeFoodRandom());

        for (int frame = 0; frame < 80; frame++) {
            game.update(0.1, false, false);
        }

        assertEquals(1, normalFoodCount(game));
        assertEquals(1, game.getFoods().size());
    }

    @Test
    void collectingNormalFoodAwardsOnceAndImmediatelyReplacesIt() {
        GameEngine game = new GameEngine(900, 540, new CollectibleFoodRandom());
        Food firstFood = onlyNormalFood(game);

        for (int frame = 0; frame < 14; frame++) {
            game.update(0.1, false, true);
        }

        assertEquals(NormalFood.SCORE_VALUE, game.getScore());
        assertEquals(1, normalFoodCount(game));
        Food replacement = onlyNormalFood(game);
        assertFalse(replacement.getX() == firstFood.getX() && replacement.getY() == firstFood.getY());

        game.update(0.1, false, false);
        assertEquals(NormalFood.SCORE_VALUE, game.getScore());
        assertEquals(1, normalFoodCount(game));
    }

    @Test
    void goldenFoodExpiresWithoutChangingTheNormalFoodInvariant() {
        GameEngine game = new GameEngine(900, 540, new GoldenFoodRandom());

        for (int frame = 0; frame < 121; frame++) {
            game.update(0.1, false, false);
        }
        assertEquals(1, goldenFoodCount(game));
        assertEquals(1, normalFoodCount(game));

        for (int frame = 0; frame < 71; frame++) {
            game.update(0.1, false, false);
        }
        assertEquals(0, goldenFoodCount(game));
        assertEquals(1, normalFoodCount(game));
    }

    @Test
    void jumpRaisesTheCatAndDifficultyStartsAtLevelOne() {
        GameEngine game = new GameEngine(900, 540, new Random(7));
        double startingY = game.getCat().getY();

        assertTrue(game.jump());
        game.update(0.05, false, false);

        assertTrue(game.getCat().getY() < startingY);
        assertEquals(1, game.getDifficultyLevel());
    }

    @Test
    void engineExposesThreeReachableHouseholdPlatforms() {
        GameEngine game = new GameEngine(900, 540, new Random(7));

        assertEquals(3, game.getPlatforms().size());
        assertTrue(game.getPlatforms().stream()
                .allMatch(platform -> platform.y() < game.getGroundY()));
    }

    @Test
    void collidingWithAHouseholdHazardEndsTheRun() {
        GameEngine game = new GameEngine(900, 540, new Random(7));

        for (int frame = 0; frame < 4_000 && !game.isGameOver(); frame++) {
            game.update(0.05, false, false);
        }

        assertTrue(game.isGameOver());

        int finalScore = game.getScore();
        double finalTime = game.getElapsedSeconds();
        double finalX = game.getCat().getX();
        assertFalse(game.jump(), "A run has one life and cannot continue after a lethal hit");

        game.update(2, true, false);

        assertEquals(finalScore, game.getScore());
        assertEquals(finalTime, game.getElapsedSeconds(), 0.0001);
        assertEquals(finalX, game.getCat().getX(), 0.0001);
    }

    private int normalFoodCount(GameEngine game) {
        return (int) game.getFoods().stream().filter(NormalFood.class::isInstance).count();
    }

    private int goldenFoodCount(GameEngine game) {
        return (int) game.getFoods().stream().filter(GoldenFood.class::isInstance).count();
    }

    private Food onlyNormalFood(GameEngine game) {
        return game.getFoods().stream().filter(NormalFood.class::isInstance).findFirst().orElseThrow();
    }

    private static class SafeFoodRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return bound == 5 ? 1 : bound - 1;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }

        @Override
        public double nextDouble() {
            return 0.99;
        }

        @Override
        public double nextDouble(double origin, double bound) {
            return bound;
        }
    }

    private static final class CollectibleFoodRandom extends SafeFoodRandom {
        @Override
        public double nextDouble() {
            return 0.5;
        }

        @Override
        public double nextDouble(double origin, double bound) {
            return origin + (bound - origin) / 2;
        }
    }

    private static final class GoldenFoodRandom extends SafeFoodRandom {
        @Override
        public double nextDouble() {
            return 0;
        }
    }
}
