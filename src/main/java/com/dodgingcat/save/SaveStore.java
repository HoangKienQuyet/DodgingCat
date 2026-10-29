package com.dodgingcat.save;

import com.dodgingcat.game.GameWorld.Boost;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Reads validated JSON progress and replaces saves using a completed temporary file. */
public final class SaveStore {
    private static final int SCHEMA_VERSION = 2;
    private static final long MAX_PROFILE_BYTES = 256 * 1024;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().setStrictness(Strictness.STRICT).create();

    private final Path path;
    private String warning;

    public SaveStore(Path path) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }

    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".dodgingcat", "profile.json");
    }

    /** Returns null after a clean operation, or a recoverable diagnostic for the UI. */
    public String warning() {
        return warning;
    }

    /**
     * Missing saves start clean. Malformed saves are copied to a separate backup
     * before defaults are returned; readable saves with damaged fields are repaired.
     * No failure removes the original file.
     */
    public Profile load() {
        warning = null;
        try {
            if (Files.notExists(path)) {
                return new Profile();
            }
            if (!Files.isRegularFile(path) || Files.isSymbolicLink(path)) {
                warning = "Progress could not be loaded: the save path is not a regular file.";
                return new Profile();
            }
            if (Files.size(path) > MAX_PROFILE_BYTES) {
                return recover("The progress file is too large");
            }
            JsonElement parsed;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                parsed = GSON.fromJson(reader, JsonElement.class);
            }
            if (parsed == null || !parsed.isJsonObject()) {
                return recover("The progress file does not contain a JSON object");
            }
            return decode(parsed.getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            return recover("Progress could not be loaded (" + e.getClass().getSimpleName() + ")");
        }
    }

    /**
     * Writes and flushes a sibling temporary file before replacement. Uses an
     * atomic move where supported, and a normal replacement otherwise.
     */
    public boolean save(Profile profile) {
        Objects.requireNonNull(profile, "profile");
        warning = null;
        Path temporary = null;
        try {
            if (Files.isSymbolicLink(path)) {
                warning = "Progress could not be saved: the save path is a symbolic link.";
                return false;
            }
            Path parent = path.getParent();
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, path.getFileName() + ".tmp-", ".json");
            byte[] bytes = (GSON.toJson(encode(profile)) + System.lineSeparator())
                    .getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            warning = "Progress could not be saved (" + e.getClass().getSimpleName() + ").";
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException | SecurityException ignored) {
                    // Only our temporary file is left behind; the original save is untouched.
                }
            }
        }
    }

    private Profile decode(JsonObject object) {
        Repairs repairs = new Repairs();
        int version = integer(object.get("schemaVersion"), 1, repairs);
        if (version < 1 || version > SCHEMA_VERSION) {
            return recover("Unsupported progress schema version: " + version);
        }
        double bestScore = score(object.get("bestScore"), repairs);
        int totalFish = integer(object.get("totalFish"), 0, repairs);
        int selectedSkin = integer(object.get("selectedSkin"), 0, repairs);
        boolean[] unlocked = new boolean[] {true, false, false};
        JsonElement unlockedElement = object.get("unlockedSkins");
        if (unlockedElement != null && unlockedElement.isJsonArray()) {
            JsonArray array = unlockedElement.getAsJsonArray();
            if (array.size() != Profile.SKIN_COUNT) {
                repairs.changed = true;
            }
            for (int i = 0; i < Math.min(array.size(), Profile.SKIN_COUNT); i++) {
                JsonElement entry = array.get(i);
                if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isBoolean()) {
                    unlocked[i] = entry.getAsBoolean();
                } else {
                    repairs.changed = true;
                }
            }
        } else {
            repairs.changed = true;
        }
        if (!unlocked[0]) {
            unlocked[0] = true;
            repairs.changed = true;
        }
        if (selectedSkin >= Profile.SKIN_COUNT || !unlocked[selectedSkin]) {
            selectedSkin = 0;
            repairs.changed = true;
        }
        EnumSet<Boost> unlockedBoosts = EnumSet.noneOf(Boost.class);
        // Version 1 predates the shop's pickup unlocks. Its empty pool is a normal migration.
        if (version >= 2) {
            JsonElement boostElement = object.get("unlockedBoosts");
            if (boostElement != null && boostElement.isJsonArray()) {
                for (JsonElement entry : boostElement.getAsJsonArray()) {
                    if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                        repairs.changed = true;
                        continue;
                    }
                    try {
                        if (!unlockedBoosts.add(Boost.valueOf(entry.getAsString()))) {
                            repairs.changed = true;
                        }
                    } catch (IllegalArgumentException ignored) {
                        repairs.changed = true;
                    }
                }
            } else {
                repairs.changed = true;
            }
        }
        List<RunResult> runs = new ArrayList<>();
        JsonElement runsElement = object.get("lastRuns");
        if (runsElement != null && runsElement.isJsonArray()) {
            JsonArray array = runsElement.getAsJsonArray();
            for (JsonElement entry : array) {
                if (runs.size() >= Profile.MAX_RECENT_RUNS) {
                    repairs.changed = true;
                    break;
                }
                if (!entry.isJsonObject()) {
                    repairs.changed = true;
                    continue;
                }
                JsonObject runObject = entry.getAsJsonObject();
                Repairs runRepairs = new Repairs();
                double runScore = score(runObject.get("score"), runRepairs);
                int fish = integer(runObject.get("fish"), 0, runRepairs);
                if (runRepairs.changed) {
                    repairs.changed = true;
                    continue;
                }
                runs.add(new RunResult(runScore, fish));
                if (runScore > bestScore) {
                    bestScore = runScore;
                    repairs.changed = true;
                }
            }
        } else {
            repairs.changed = true;
        }
        if (repairs.changed) {
            warning = "Some progress fields were invalid or missing and were repaired.";
        }
        return new Profile(bestScore, totalFish, selectedSkin, unlocked, runs, unlockedBoosts);
    }

    private static JsonObject encode(Profile profile) {
        JsonObject object = new JsonObject();
        object.addProperty("schemaVersion", SCHEMA_VERSION);
        object.addProperty("bestScore", profile.bestScore());
        object.addProperty("totalFish", profile.totalFish());
        object.addProperty("selectedSkin", profile.selectedSkin());
        JsonArray unlocked = new JsonArray();
        for (int i = 0; i < Profile.SKIN_COUNT; i++) {
            unlocked.add(profile.isUnlocked(i));
        }
        object.add("unlockedSkins", unlocked);
        JsonArray boosts = new JsonArray();
        for (Boost boost : Boost.values()) {
            if (profile.isBoostUnlocked(boost)) {
                boosts.add(boost.name());
            }
        }
        object.add("unlockedBoosts", boosts);
        JsonArray runs = new JsonArray();
        for (RunResult run : profile.lastRuns()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("score", run.score());
            entry.addProperty("fish", run.fish());
            runs.add(entry);
        }
        object.add("lastRuns", runs);
        return object;
    }

    private Profile recover(String reason) {
        String preservation = " The original file was left unchanged.";
        try {
            if (Files.isRegularFile(path) && !Files.isSymbolicLink(path)) {
                Path backup = path.resolveSibling(path.getFileName() + ".corrupt-" + UUID.randomUUID() + ".bak");
                Files.copy(path, backup);
                preservation = " A backup was saved as " + backup.getFileName() + ".";
            }
        } catch (IOException | SecurityException ignored) {
            // Recovery must still return a playable default profile when copying is impossible.
        }
        warning = reason + ". Starting with default progress." + preservation;
        return new Profile();
    }

    private static int integer(JsonElement element, int fallback, Repairs repairs) {
        try {
            if (element != null && element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    int value = new BigDecimal(primitive.getAsString()).intValueExact();
                    if (value >= 0) {
                        return value;
                    }
                }
            }
        } catch (ArithmeticException | NumberFormatException ignored) {
            // Fractional, non-finite, or overflowing values are invalid integer fields.
        }
        repairs.changed = true;
        return fallback;
    }

    private static double score(JsonElement element, Repairs repairs) {
        if (element != null && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isNumber()) {
            double value = element.getAsDouble();
            if (Double.isFinite(value) && value >= 0) {
                return value;
            }
        }
        repairs.changed = true;
        return 0;
    }

    private static final class Repairs {
        private boolean changed;
    }
}
