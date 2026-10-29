package com.dodgingcat.save;

import com.dodgingcat.game.GameWorld.Boost;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProfileTest {
    @Test
    void purchasesChargeOnceAndCannotSpendMoreThanBalance() {
        Profile profile = new Profile();
        assertTrue(profile.isUnlocked(0));
        assertFalse(profile.selectSkin(1));
        assertFalse(profile.buyAndSelectSkin(1));
        assertEquals(0, profile.selectedSkin());

        profile.recordRun(10.5, 30);
        assertTrue(profile.buyAndSelectSkin(1));
        assertEquals(5, profile.totalFish());
        assertEquals(1, profile.selectedSkin());
        assertTrue(profile.buyAndSelectSkin(1));
        assertEquals(5, profile.totalFish());
        assertFalse(profile.buyAndSelectSkin(2));

        profile.recordRun(4, 45);
        assertTrue(profile.buyAndSelectSkin(2));
        assertEquals(0, profile.totalFish());
        assertTrue(profile.selectSkin(0));
        assertTrue(profile.selectSkin(2));
        assertFalse(profile.selectSkin(-1));
        assertFalse(profile.buyAndSelectSkin(3));
        assertFalse(profile.isUnlocked(Integer.MAX_VALUE));
        assertEquals(10.5, profile.bestScore());
    }

    @Test
    void invalidRunDoesNotMutateProgress() {
        Profile profile = new Profile();
        profile.recordRun(12, 3);
        for (double invalid : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> profile.recordRun(invalid, 7));
        }
        assertThrows(IllegalArgumentException.class, () -> profile.recordRun(100, -1));
        assertEquals(12, profile.bestScore());
        assertEquals(3, profile.totalFish());
        assertEquals(1, profile.lastRuns().size());
    }

    @Test
    void historyIsBoundedImmutableAndFishCannotOverflow() {
        Profile profile = new Profile();
        profile.recordRun(1, Integer.MAX_VALUE);
        var snapshot = profile.lastRuns();
        for (int i = 2; i <= 20; i++) {
            profile.recordRun(i, 1);
        }
        assertEquals(Integer.MAX_VALUE, profile.totalFish());
        assertEquals(Profile.MAX_RECENT_RUNS, profile.lastRuns().size());
        assertEquals(20, profile.lastRuns().getFirst().score());
        assertEquals(1, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
    }

    @Test
    void boostUnlocksShareTheWalletAndNeverChargeTwice() {
        Profile profile = new Profile();
        assertTrue(profile.unlockedBoosts().isEmpty());
        assertFalse(profile.isBoostUnlocked(Boost.SHIELD));
        assertFalse(profile.unlockBoost(Boost.SHIELD));
        assertFalse(profile.unlockBoost(null));
        assertFalse(profile.isBoostUnlocked(null));
        assertThrows(IllegalArgumentException.class, () -> Profile.boostPrice(null));
        assertEquals(25, Profile.boostPrice(Boost.SHIELD));
        assertEquals(40, Profile.boostPrice(Boost.INVINCIBILITY));
        assertEquals(60, Profile.boostPrice(Boost.DOUBLE_FISH));
        assertEquals(50, Profile.boostPrice(Boost.BOMB));

        profile.recordRun(15, 100);
        assertTrue(profile.buyAndSelectSkin(1));
        assertTrue(profile.unlockBoost(Boost.SHIELD));
        assertEquals(50, profile.totalFish());
        Set<Boost> snapshot = profile.unlockedBoosts();
        assertTrue(profile.unlockBoost(Boost.SHIELD));
        assertEquals(50, profile.totalFish());
        assertTrue(profile.unlockBoost(Boost.INVINCIBILITY));
        assertEquals(10, profile.totalFish());
        assertFalse(profile.unlockBoost(Boost.DOUBLE_FISH));
        assertEquals(10, profile.totalFish());
        assertFalse(profile.isBoostUnlocked(Boost.DOUBLE_FISH));
        assertEquals(Set.of(Boost.SHIELD), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(Boost.DOUBLE_FISH));

        profile.recordRun(20, 50);
        assertTrue(profile.unlockBoost(Boost.DOUBLE_FISH));
        assertEquals(0, profile.totalFish());
        assertEquals(Set.of(Boost.SHIELD, Boost.INVINCIBILITY, Boost.DOUBLE_FISH),
                profile.unlockedBoosts());
        assertFalse(profile.isBoostUnlocked(Boost.BOMB));
        assertEquals(1, profile.selectedSkin());
        assertEquals(20, profile.bestScore());
    }

    @Test
    void bombUnlockRequiresFiftyFishAndNeverChargesAgain() {
        Profile profile = new Profile();
        profile.recordRun(10, 49);
        assertFalse(profile.unlockBoost(Boost.BOMB));
        assertFalse(profile.isBoostUnlocked(Boost.BOMB));
        assertEquals(49, profile.totalFish());

        profile.recordRun(12, 1);
        assertTrue(profile.unlockBoost(Boost.BOMB));
        assertTrue(profile.isBoostUnlocked(Boost.BOMB));
        assertEquals(Set.of(Boost.BOMB), profile.unlockedBoosts());
        assertEquals(0, profile.totalFish());
        assertTrue(profile.unlockBoost(Boost.BOMB));
        assertEquals(0, profile.totalFish());
    }
}
