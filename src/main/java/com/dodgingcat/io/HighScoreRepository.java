package com.dodgingcat.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/** Reads and writes the player's best score in a small local text file. */
public final class HighScoreRepository {

    private static final System.Logger LOGGER = System.getLogger(HighScoreRepository.class.getName());
    private static final String DIRECTORY_NAME = ".dodging-cat";
    private static final String FILE_NAME = "highscore.txt";

    private final Path file;

    /** Stores the score in the current user's home directory. */
    public HighScoreRepository() {
        this(Path.of(System.getProperty("user.home"), DIRECTORY_NAME, FILE_NAME));
    }

    /** Allows tests and other callers to choose a storage location. */
    public HighScoreRepository(Path file) {
        this.file = Objects.requireNonNull(file, "score file must not be null");
    }

    /** Returns zero when no usable score is stored, and logs malformed or unreadable data. */
    public int load() {
        if (!Files.exists(file)) {
            return 0;
        }

        try {
            int score = Integer.parseInt(Files.readString(file, StandardCharsets.UTF_8).trim());
            if (score < 0) {
                throw new NumberFormatException("High score cannot be negative");
            }
            return score;
        } catch (IOException | NumberFormatException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Could not read a valid high score from " + file + "; using 0. "
                            + exception.getMessage());
            return 0;
        }
    }

    /** Saves a non-negative score, creating the storage directory when needed. */
    public void save(int score) throws IOException {
        if (score < 0) {
            throw new IllegalArgumentException("High score cannot be negative");
        }

        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, Integer.toString(score), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }
}
