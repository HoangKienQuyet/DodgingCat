package com.dodgingcat.shop;

import com.dodgingcat.persistence.PersistenceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GachaSequenceTest {
    @TempDir Path directory;

    @Test void everySkinLandsOnItsActualPrizeAndWaitsForAudioEnd() {
        var shop = new ShopManager(new PersistenceManager(directory.resolve("profile.json")));
        for (var winner : shop.getSkinCatalog()) {
            for (int seed = 0; seed < 20; seed++) {
                var sequence = new GachaSequence(shop.getSkinCatalog(), winner, new Random(seed));
                double previous = sequence.getReelPosition();
                for (int i = 0; i <= 100; i++) {
                    sequence.update(.2, i / 100.0, false, true);
                    assertEquals(GachaSequence.Phase.SPINNING, sequence.getPhase(), "Neither elapsed time nor click may cut off spin audio");
                    assertTrue(sequence.getReelPosition() >= previous);
                    previous = sequence.getReelPosition();
                }
                sequence.update(0, 1, true, false);
                assertEquals(GachaSequence.Phase.LANDED, sequence.getPhase());
                assertEquals(sequence.getWinningIndex(), sequence.getReelPosition(), 1e-9);
                assertSame(winner, sequence.getReel().get(sequence.getWinningIndex()));
                sequence.update(GachaSequence.LANDING_SECONDS, 1, true, false);
                if (winner.getRarity() == SkinRarity.LEGENDARY) {
                    assertEquals(GachaSequence.Phase.LEGENDARY_REVEAL, sequence.getPhase());
                    sequence.update(GachaSequence.LEGENDARY_SECONDS - .01, 1, true, true);
                    assertEquals(GachaSequence.Phase.LEGENDARY_REVEAL, sequence.getPhase());
                    sequence.update(.02, 1, true, false);
                }
                assertEquals(GachaSequence.Phase.RESULT, sequence.getPhase());
                sequence.update(1.99, 1, true, false);
                assertTrue(sequence.isActive());
                sequence.update(.02, 1, true, false);
                assertFalse(sequence.isActive());
            }
        }
    }

    @Test void resultCanBeDismissedByClick() {
        var shop = new ShopManager(new PersistenceManager(directory.resolve("profile.json")));
        var sequence = new GachaSequence(shop.getSkinCatalog(), shop.findItemById("snow_white"), new Random(9));
        sequence.update(0, 1, true, false);
        sequence.update(GachaSequence.LANDING_SECONDS, 1, true, false);
        sequence.update(0, 1, true, true);
        assertEquals(GachaSequence.Phase.DONE, sequence.getPhase());
    }

    @Test void prizeDrawRetainsRarityWeightsAndNeverAwardsOwnedSkins() {
        // Fresh profile: 3 common, 4 uncommon, 3 rare, 3 legendary remain (total weight 541).
        double[] rolls = {0, 299.9 / 541, 300.1 / 541, 459.9 / 541, 460.1 / 541, 519.9 / 541, 520.1 / 541, .999999};
        SkinRarity[] expected = {SkinRarity.COMMON, SkinRarity.COMMON, SkinRarity.UNCOMMON, SkinRarity.UNCOMMON,
                SkinRarity.RARE, SkinRarity.RARE, SkinRarity.LEGENDARY, SkinRarity.LEGENDARY};
        for (int i = 0; i < rolls.length; i++) {
            var persistence = new PersistenceManager(directory.resolve("profile-" + i + ".json"));
            persistence.getData().setTotalFishes(100_000);
            var shop = new ShopManager(persistence);
            double roll = rolls[i];
            Random random = new Random(42) {
                @Override public double nextDouble() { return roll; }
                @Override public int nextInt(int bound) { return 0; }
            };
            var winner = shop.pullSkin(random);
            assertNotNull(winner);
            assertEquals(expected[i], winner.getRarity());
            assertNotEquals("ginger", winner.getId()); assertNotEquals("tuxedo", winner.getId());
            assertEquals(99_950, persistence.getData().getTotalFishes());
            for (int j = 0; j < 12; j++) {
                var next = shop.pullSkin(random);
                assertNotNull(next); assertNotEquals(winner.getId(), next.getId());
            }
            int balance = persistence.getData().getTotalFishes();
            assertNull(shop.pullSkin(random), "Full collection must not charge a pull");
            assertEquals(balance, persistence.getData().getTotalFishes());
        }
    }
}
