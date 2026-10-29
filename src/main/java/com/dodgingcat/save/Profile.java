package com.dodgingcat.save;

import com.dodgingcat.game.GameWorld.Boost;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Local progress. Call {@link #recordRun(double, int)} once when a run ends. */
public final class Profile {
    public static final int SKIN_COUNT = 3;
    public static final int MAX_RECENT_RUNS = 10;
    private static final int[] SKIN_PRICES = {0, 25, 50};

    private double bestScore;
    private int totalFish;
    private int selectedSkin;
    private final boolean[] unlockedSkins;
    private final EnumSet<Boost> unlockedBoosts;
    private final List<RunResult> lastRuns;

    public Profile() {
        this(0, 0, 0, new boolean[] {true, false, false}, List.of());
    }

    Profile(double bestScore, int totalFish, int selectedSkin,
            boolean[] unlockedSkins, List<RunResult> lastRuns) {
        this(bestScore, totalFish, selectedSkin, unlockedSkins, lastRuns, Set.of());
    }

    Profile(double bestScore, int totalFish, int selectedSkin,
            boolean[] unlockedSkins, List<RunResult> lastRuns, Set<Boost> unlockedBoosts) {
        this.bestScore = bestScore;
        this.totalFish = totalFish;
        this.selectedSkin = selectedSkin;
        this.unlockedSkins = unlockedSkins.clone();
        this.lastRuns = new ArrayList<>(lastRuns);
        this.unlockedBoosts = EnumSet.noneOf(Boost.class);
        this.unlockedBoosts.addAll(unlockedBoosts);
    }

    public double bestScore() {
        return bestScore;
    }

    /** Spendable fish, after purchases. */
    public int totalFish() {
        return totalFish;
    }

    /** 0 = orange, 1 = cream, 2 = charcoal. */
    public int selectedSkin() {
        return selectedSkin;
    }

    public boolean isUnlocked(int skin) {
        return validSkin(skin) && unlockedSkins[skin];
    }

    public boolean isBoostUnlocked(Boost boost) {
        return boost != null && unlockedBoosts.contains(boost);
    }

    /**
     * An immutable snapshot of the permanently unlocked random-pickup pool.
     * Unlocking a boost does not equip or activate its effect at the start of a run.
     */
    public Set<Boost> unlockedBoosts() {
        return Set.copyOf(unlockedBoosts);
    }

    /** Purchases a permanent pickup unlock; an owned boost never charges fish again. */
    public boolean unlockBoost(Boost boost) {
        if (boost == null) {
            return false;
        }
        if (unlockedBoosts.contains(boost)) {
            return true;
        }
        int price = boostPrice(boost);
        if (totalFish < price) {
            return false;
        }
        totalFish -= price;
        unlockedBoosts.add(boost);
        return true;
    }

    public static int boostPrice(Boost boost) {
        if (boost == null) {
            throw new IllegalArgumentException("Unknown boost: null");
        }
        return switch (boost) {
            case SHIELD -> 25;
            case INVINCIBILITY -> 40;
            case DOUBLE_FISH -> 60;
            case BOMB -> 50;
        };
    }

    /** An immutable snapshot ordered from newest to oldest. */
    public List<RunResult> lastRuns() {
        return List.copyOf(lastRuns);
    }

    /**
     * Awards a completed run's fish and updates the best score. Invalid input
     * throws before any progress changes; the fish balance saturates at Integer.MAX_VALUE.
     */
    public void recordRun(double score, int fish) {
        RunResult run = new RunResult(score, fish);
        bestScore = Math.max(bestScore, score);
        totalFish = (int) Math.min(Integer.MAX_VALUE, (long) totalFish + fish);
        lastRuns.add(0, run);
        if (lastRuns.size() > MAX_RECENT_RUNS) {
            lastRuns.remove(lastRuns.size() - 1);
        }
    }

    /** An already owned skin can be selected again without charging fish. */
    public boolean buyAndSelectSkin(int skin) {
        if (!validSkin(skin)) {
            return false;
        }
        if (!unlockedSkins[skin]) {
            int price = SKIN_PRICES[skin];
            if (totalFish < price) {
                return false;
            }
            totalFish -= price;
            unlockedSkins[skin] = true;
        }
        selectedSkin = skin;
        return true;
    }

    public boolean selectSkin(int skin) {
        if (!isUnlocked(skin)) {
            return false;
        }
        selectedSkin = skin;
        return true;
    }

    public static int skinPrice(int skin) {
        if (!validSkin(skin)) {
            throw new IllegalArgumentException("Unknown skin: " + skin);
        }
        return SKIN_PRICES[skin];
    }

    private static boolean validSkin(int skin) {
        return skin >= 0 && skin < SKIN_COUNT;
    }
}
