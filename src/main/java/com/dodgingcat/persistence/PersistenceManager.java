package com.dodgingcat.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Robust JSON persistence manager with fault tolerance.
 */
public class PersistenceManager {
    private static final String DEFAULT_FILE_NAME = "dodging_cat_profile.json";
    private final Path savePath;
    private final Gson gson;
    private GameData currentData;

    public PersistenceManager() {
        this(defaultSavePath());
    }

    private static Path defaultSavePath() {
        // Read Unicode paths from UTF-8 rather than the Windows launcher's ANSI arguments.
        String pathFile = System.getProperty("dodgingcat.profilePathFile");
        if (pathFile != null && !pathFile.isBlank()) {
            try {
                return Path.of(Files.readString(Path.of(pathFile)).strip());
            } catch (IOException e) {
                throw new java.io.UncheckedIOException("Cannot read portable profile location", e);
            }
        }
        String portablePath = System.getProperty("dodgingcat.profile");
        return portablePath == null || portablePath.isBlank()
                ? Paths.get(System.getProperty("user.home"), ".dodgingcat", DEFAULT_FILE_NAME)
                : Paths.get(portablePath);
    }

    public PersistenceManager(Path savePath) {
        this.savePath = savePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.currentData = load();
    }

    public synchronized GameData load() {
        if (!Files.exists(savePath)) {
            currentData = new GameData();
            save(currentData);
            return currentData;
        }

        try (Reader reader = Files.newBufferedReader(savePath)) {
            GameData loaded = gson.fromJson(reader, GameData.class);
            if (loaded == null) {
                currentData = new GameData();
            } else {
                currentData = loaded;
                if (currentData.getUnlockedMaps() == null || currentData.getUnlockedMaps().isEmpty()) {
                    currentData.setUnlockedMaps(new java.util.ArrayList<>(java.util.List.of("kitchen")));
                }
                if (currentData.getUnlockedSkins() == null || currentData.getUnlockedSkins().isEmpty()) {
                    currentData.setUnlockedSkins(new java.util.ArrayList<>(java.util.List.of("ginger", "tuxedo")));
                }
                if (currentData.getSelectedMap() == null) {
                    currentData.setSelectedMap("kitchen");
                }
                if (currentData.getP1Skin() == null) {
                    currentData.setP1Skin("ginger");
                }
                if (currentData.getP2Skin() == null) {
                    currentData.setP2Skin("tuxedo");
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Corrupted or unreadable save file. Resetting to defaults. " + e.getMessage());
            currentData = new GameData();
            save(currentData);
        }
        return currentData;
    }

    public synchronized boolean save(GameData data) {
        try {
            if (savePath.getParent() != null) {
                Files.createDirectories(savePath.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(savePath)) {
                gson.toJson(data, writer);
            }
            this.currentData = data;
            return true;
        } catch (IOException e) {
            System.err.println("Error saving game profile: " + e.getMessage());
            return false;
        }
    }

    public synchronized void save() {
        if (currentData != null) {
            save(currentData);
        }
    }

    public GameData getData() {
        if (currentData == null) {
            currentData = load();
        }
        return currentData;
    }

    public Path getSavePath() {
        return savePath;
    }
}
