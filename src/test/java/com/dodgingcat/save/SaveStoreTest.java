package com.dodgingcat.save;

import com.dodgingcat.game.GameWorld.Boost;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SaveStoreTest {
    @TempDir
    Path directory;

    @Test
    void missingSaveIsCleanAndDoesNotWriteAnything() {
        Path path = directory.resolve("not-created/profile.json");
        SaveStore store = new SaveStore(path);
        Profile profile = store.load();
        assertEquals(0, profile.bestScore());
        assertEquals(0, profile.totalFish());
        assertTrue(profile.isUnlocked(0));
        assertNull(store.warning());
        assertFalse(Files.exists(path.getParent()));
    }

    @Test
    void progressPurchasesAndHistorySurviveReplacingExistingSave() throws IOException {
        Path path = directory.resolve("nested/profile.json");
        SaveStore store = new SaveStore(path);
        Profile profile = new Profile();
        profile.recordRun(14.75, 80);
        assertTrue(profile.buyAndSelectSkin(1));
        assertTrue(store.save(profile), store::warning);
        profile.recordRun(20.125, 5);
        assertTrue(profile.buyAndSelectSkin(2));
        assertTrue(store.save(profile), store::warning);
        assertNull(store.warning());

        Profile reloaded = new SaveStore(path).load();
        assertEquals(20.125, reloaded.bestScore());
        assertEquals(10, reloaded.totalFish());
        assertEquals(2, reloaded.selectedSkin());
        assertTrue(reloaded.isUnlocked(1));
        assertTrue(reloaded.isUnlocked(2));
        assertEquals(profile.lastRuns(), reloaded.lastRuns());
        try (var siblings = Files.list(path.getParent())) {
            assertEquals(1, siblings.count(), "Successful writes must clean up temporary files.");
        }
    }

    @Test
    void malformedAndTruncatedJsonReturnDefaultsAndPreserveOriginals() throws IOException {
        for (String invalid : new String[] {"{", "{\"totalFish\": 90", "[]", "null",
                "{\"bestScore\":NaN}", "{} trailing"}) {
            Path path = directory.resolve("profile-" + Math.abs(invalid.hashCode()) + ".json");
            Files.writeString(path, invalid);
            SaveStore store = new SaveStore(path);
            Profile profile = store.load();
            assertEquals(0, profile.totalFish());
            assertEquals(0, profile.bestScore());
            assertNotNull(store.warning());
            assertEquals(invalid, Files.readString(path));
            try (var siblings = Files.list(directory)) {
                Path backup = siblings.filter(p -> p.getFileName().toString()
                        .startsWith(path.getFileName() + ".corrupt-")).findFirst().orElseThrow();
                assertEquals(invalid, Files.readString(backup));
            }
        }
    }

    @Test
    void invalidFieldsCannotGiveNegativeFishNonFiniteScoresOrLockedSelections() throws IOException {
        Path path = directory.resolve("profile.json");
        Files.writeString(path, """
                {
                  "schemaVersion": 1,
                  "bestScore": 1e309,
                  "totalFish": -100,
                  "selectedSkin": 2,
                  "unlockedSkins": [false, "true", false],
                  "lastRuns": [{"score": 12.5, "fish": 2}, {"score": -4, "fish": 99}]
                }
                """);
        SaveStore store = new SaveStore(path);
        Profile profile = store.load();
        assertEquals(0, profile.totalFish());
        assertEquals(12.5, profile.bestScore());
        assertEquals(0, profile.selectedSkin());
        assertTrue(profile.isUnlocked(0));
        assertFalse(profile.isUnlocked(1));
        assertFalse(profile.isUnlocked(2));
        assertEquals(1, profile.lastRuns().size());
        assertNotNull(store.warning());
    }

    @Test
    void missingFractionalAndOverflowingFieldsAreRepaired() throws IOException {
        Path path = directory.resolve("profile.json");
        Files.writeString(path, """
                {"totalFish":2147483648,"selectedSkin":0.5,"unlockedSkins":[true]}
                """);
        SaveStore store = new SaveStore(path);
        Profile profile = store.load();
        assertEquals(0, profile.totalFish());
        assertEquals(0, profile.selectedSkin());
        assertEquals(0, profile.bestScore());
        assertTrue(profile.lastRuns().isEmpty());
        assertNotNull(store.warning());
        assertTrue(store.save(profile), store::warning);
        assertNull(store.warning());
        store.load();
        assertNull(store.warning());
    }

    @Test
    void unsupportedSchemaIsPreservedInsteadOfGuessingItsMeaning() throws IOException {
        Path path = directory.resolve("profile.json");
        String futureSave = "{\"schemaVersion\":99,\"totalFish\":1000}";
        Files.writeString(path, futureSave);
        SaveStore store = new SaveStore(path);
        assertEquals(0, store.load().totalFish());
        assertTrue(store.warning().contains("Unsupported"));
        assertEquals(futureSave, Files.readString(path));
    }

    @Test
    void unwritableDestinationReportsFailureWithoutReplacingIt() throws IOException {
        Path directoryTarget = Files.createDirectory(directory.resolve("profile.json"));
        Files.writeString(directoryTarget.resolve("keep.txt"), "important");
        SaveStore store = new SaveStore(directoryTarget);
        assertFalse(store.save(new Profile()));
        assertNotNull(store.warning());
        assertEquals("important", Files.readString(directoryTarget.resolve("keep.txt")));
        assertEquals(0, store.load().totalFish());
        assertNotNull(store.warning());
    }

    @Test
    void versionOneMigratesWithoutWarningBackupOrLossOfExistingProgress() throws IOException {
        Path path = directory.resolve("legacy.json");
        String legacy = """
                {
                  "schemaVersion": 1,
                  "bestScore": 44.5,
                  "totalFish": 123,
                  "selectedSkin": 2,
                  "unlockedSkins": [true, true, true],
                  "lastRuns": [{"score": 12.5, "fish": 3}]
                }
                """;
        Files.writeString(path, legacy);
        SaveStore store = new SaveStore(path);
        Profile profile = store.load();
        assertNull(store.warning());
        assertEquals(44.5, profile.bestScore());
        assertEquals(123, profile.totalFish());
        assertEquals(2, profile.selectedSkin());
        assertTrue(profile.isUnlocked(1));
        assertTrue(profile.isUnlocked(2));
        assertEquals(new RunResult(12.5, 3), profile.lastRuns().getFirst());
        assertTrue(profile.unlockedBoosts().isEmpty());
        assertFalse(profile.isBoostUnlocked(Boost.BOMB));
        assertEquals(legacy, Files.readString(path), "Loading must not rewrite the legacy save.");
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count(), "Normal migration must not create a corruption backup.");
        }

        assertTrue(store.save(profile), store::warning);
        var saved = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals(2, saved.get("schemaVersion").getAsInt());
        assertTrue(saved.getAsJsonArray("unlockedBoosts").isEmpty());
        Profile reloaded = store.load();
        assertNull(store.warning());
        assertEquals(123, reloaded.totalFish());
        assertEquals(profile.lastRuns(), reloaded.lastRuns());
        assertTrue(reloaded.unlockedBoosts().isEmpty());
    }

    @Test
    void permanentPickupUnlocksRoundTripWithSkinsWalletAndHistory() throws IOException {
        Path path = directory.resolve("boosts.json");
        SaveStore store = new SaveStore(path);
        Profile profile = new Profile();
        profile.recordRun(30, 220);
        assertTrue(profile.buyAndSelectSkin(1));
        assertTrue(profile.unlockBoost(Boost.SHIELD));
        assertTrue(profile.unlockBoost(Boost.INVINCIBILITY));
        assertTrue(profile.unlockBoost(Boost.DOUBLE_FISH));
        assertTrue(profile.unlockBoost(Boost.BOMB));
        profile.recordRun(45.5, 7);
        assertTrue(store.save(profile), store::warning);

        Profile reloaded = store.load();
        assertNull(store.warning());
        assertEquals(27, reloaded.totalFish());
        assertEquals(45.5, reloaded.bestScore());
        assertEquals(1, reloaded.selectedSkin());
        assertEquals(profile.lastRuns(), reloaded.lastRuns());
        assertEquals(Set.of(Boost.SHIELD, Boost.INVINCIBILITY, Boost.DOUBLE_FISH, Boost.BOMB),
                reloaded.unlockedBoosts());
        assertTrue(reloaded.unlockBoost(Boost.INVINCIBILITY));
        assertEquals(27, reloaded.totalFish());
    }

    @Test
    void malformedVersionTwoUnlocksOnlyPreserveRecognizedEntries() throws IOException {
        Path path = directory.resolve("boosts.json");
        Files.writeString(path, """
                {
                  "schemaVersion": 2,
                  "bestScore": 0,
                  "totalFish": 10,
                  "selectedSkin": 0,
                  "unlockedSkins": [true, false, false],
                  "unlockedBoosts": ["SHIELD", "UNKNOWN", null, 1, "SHIELD"],
                  "lastRuns": []
                }
                """);
        SaveStore store = new SaveStore(path);
        Profile profile = store.load();
        assertNotNull(store.warning());
        assertEquals(Set.of(Boost.SHIELD), profile.unlockedBoosts());
        assertEquals(10, profile.totalFish());
        assertFalse(profile.isBoostUnlocked(Boost.INVINCIBILITY));
        assertFalse(profile.isBoostUnlocked(Boost.DOUBLE_FISH));
    }

    @Test
    void olderVersionTwoKeepsProgressAndExistingUnlocksWithoutGrantingBomb() throws IOException {
        Path path = directory.resolve("before-bomb.json");
        Files.writeString(path, """
                {
                  "schemaVersion": 2,
                  "bestScore": 91.5,
                  "totalFish": 203,
                  "selectedSkin": 1,
                  "unlockedSkins": [true, true, false],
                  "unlockedBoosts": ["SHIELD", "INVINCIBILITY", "DOUBLE_FISH"],
                  "lastRuns": [{"score": 88.5, "fish": 17}]
                }
                """);
        SaveStore store = new SaveStore(path);
        Profile profile = store.load();
        assertNull(store.warning());
        assertEquals(91.5, profile.bestScore());
        assertEquals(203, profile.totalFish());
        assertEquals(1, profile.selectedSkin());
        assertTrue(profile.isUnlocked(1));
        assertFalse(profile.isUnlocked(2));
        assertEquals(new RunResult(88.5, 17), profile.lastRuns().getFirst());
        assertEquals(Set.of(Boost.SHIELD, Boost.INVINCIBILITY, Boost.DOUBLE_FISH),
                profile.unlockedBoosts());
        assertFalse(profile.isBoostUnlocked(Boost.BOMB));

        assertTrue(store.save(profile), store::warning);
        var saved = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals(2, saved.get("schemaVersion").getAsInt());
        Profile reloaded = store.load();
        assertNull(store.warning());
        assertFalse(reloaded.isBoostUnlocked(Boost.BOMB));
        assertEquals(profile.unlockedBoosts(), reloaded.unlockedBoosts());
        assertEquals(profile.totalFish(), reloaded.totalFish());
        assertEquals(profile.lastRuns(), reloaded.lastRuns());
    }
}
