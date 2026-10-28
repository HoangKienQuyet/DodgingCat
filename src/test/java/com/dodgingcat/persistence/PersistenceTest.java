package com.dodgingcat.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PersistenceTest {
    @Test
    void portableProfileLocationFilePreservesVietnamesePath(@TempDir Path tempDir) throws IOException {
        String previous = System.getProperty("dodgingcat.profilePathFile");
        Path profile = tempDir.resolve("Máy trường/data/profile.json");
        Path location = tempDir.resolve("profile-location.txt");
        Files.writeString(location, profile.toString());
        try {
            System.setProperty("dodgingcat.profilePathFile", location.toString());
            var manager = new PersistenceManager();
            manager.getData().setTotalFishes(23);
            manager.save();
            assertTrue(Files.exists(profile));
            assertEquals(23, new PersistenceManager().getData().getTotalFishes());
        } finally {
            if (previous == null) System.clearProperty("dodgingcat.profilePathFile");
            else System.setProperty("dodgingcat.profilePathFile", previous);
        }
    }
    @Test
    void portableProfileCanBeStoredBesideTheApplication(@TempDir Path tempDir) {
        String previous=System.getProperty("dodgingcat.profile");
        Path file=tempDir.resolve("data/profile.json");
        try {
            System.setProperty("dodgingcat.profile",file.toString());
            var manager=new PersistenceManager();
            manager.getData().setTotalFishes(17);manager.save();
            assertTrue(Files.exists(file));
            assertEquals(17,new PersistenceManager().getData().getTotalFishes());
        } finally {
            if(previous==null)System.clearProperty("dodgingcat.profile");else System.setProperty("dodgingcat.profile",previous);
        }
    }
    @Test
    void lastPlayedMapSurvivesLoadoutChangesAndLegacySaves(@TempDir Path tempDir) throws IOException {
        Path file=tempDir.resolve("legacy.json");
        Files.writeString(file,"{\"selectedMap\":\"rooftop\"}");
        var persistence=new PersistenceManager(file);
        var data=persistence.getData();
        assertEquals("rooftop",data.getLastPlayedMap());
        data.setSelectedMap("garden");
        persistence.save();
        assertEquals("rooftop",new PersistenceManager(file).getData().getLastPlayedMap());
        data.setLastPlayedMap("garden");
        persistence.save();
        assertEquals("garden",new PersistenceManager(file).getData().getLastPlayedMap());
    }

    @Test
    @DisplayName("Saves and loads profile data correctly via JSON")
    void testSaveLoad(@TempDir Path tempDir) {
        Path saveFile = tempDir.resolve("test_profile.json");
        PersistenceManager pm = new PersistenceManager(saveFile);

        GameData data = pm.getData();
        data.setSingleHighScore(1250);
        data.setCoopHighScore(2400);
        data.setTotalPate(150);
        data.setSelectedTheme("garden");

        pm.save(data);
        assertTrue(Files.exists(saveFile));

        // Re-load from disk
        PersistenceManager reloader = new PersistenceManager(saveFile);
        GameData loaded = reloader.getData();

        assertEquals(1250, loaded.getSingleHighScore());
        assertEquals(2400, loaded.getCoopHighScore());
        assertEquals(150, loaded.getTotalFishes());
        assertEquals(150, loaded.getTotalPate());
        assertEquals("garden", loaded.getSelectedMap());
        assertEquals("garden", loaded.getSelectedTheme());
    }

    @Test
    @DisplayName("Gracefully recovers with defaults when save file is corrupted")
    void testCorrupted(@TempDir Path tempDir) throws IOException {
        Path corruptedFile = tempDir.resolve("corrupted.json");
        Files.writeString(corruptedFile, "{ this is not valid JSON :::: null }}}");

        PersistenceManager pm = new PersistenceManager(corruptedFile);
        GameData data = pm.getData();

        assertNotNull(data);
        assertEquals(0, data.getSingleHighScore());
        assertEquals(0, data.getTotalPate());
        assertTrue(data.getUnlockedThemes().contains("kitchen"));
    }

    @Test
    @DisplayName("Pate currency spending verifies balance")
    void testSpending(@TempDir Path tempDir) {
        Path saveFile = tempDir.resolve("wallet_test.json");
        PersistenceManager pm = new PersistenceManager(saveFile);
        GameData data = pm.getData();

        data.setTotalPate(100);
        boolean spentTooMuch = data.spendPate(150);
        assertFalse(spentTooMuch);
        assertEquals(100, data.getTotalPate());

        boolean validSpend = data.spendPate(40);
        assertTrue(validSpend);
        assertEquals(60, data.getTotalPate());
    }

    @Test
    @DisplayName("Leaderboard maintains top 10 descending order")
    void testLeaderboard(@TempDir Path tempDir) {
        Path saveFile = tempDir.resolve("lb_test.json");
        PersistenceManager pm = new PersistenceManager(saveFile);
        GameData data = pm.getData();

        data.addSingleRun(100, 10.0);
        data.addSingleRun(500, 50.0);
        data.addSingleRun(300, 30.0);

        assertEquals(3, data.getSingleLeaderboard().size());
        assertEquals(500, data.getSingleLeaderboard().get(0).getScore());
        assertEquals(300, data.getSingleLeaderboard().get(1).getScore());
        assertEquals(100, data.getSingleLeaderboard().get(2).getScore());
        assertEquals(500, data.getSingleHighScore());
    }
}
