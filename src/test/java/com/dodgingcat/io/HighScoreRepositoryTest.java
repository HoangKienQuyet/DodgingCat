package com.dodgingcat.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HighScoreRepositoryTest {

    private Path temporaryDirectory;

    @BeforeEach
    void createTemporaryDirectory() throws IOException {
        Path testDataDirectory = Path.of("target", "test-data");
        Files.createDirectories(testDataDirectory);
        temporaryDirectory = Files.createTempDirectory(testDataDirectory, "high-score-");
    }

    @AfterEach
    void removeTemporaryDirectory() throws IOException {
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(temporaryDirectory)) {
            paths = walk.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void missingHighScoreDefaultsToZero() {
        HighScoreRepository repository = new HighScoreRepository(
                temporaryDirectory.resolve("missing/highscore.txt"));

        assertEquals(0, repository.load());
    }

    @Test
    void savesAndLoadsScoreAndCreatesItsParentDirectory() throws IOException {
        HighScoreRepository repository = new HighScoreRepository(
                temporaryDirectory.resolve("nested/data/highscore.txt"));

        repository.save(270);

        assertEquals(270, repository.load());
    }

    @Test
    void corruptOrNegativeFileContentFallsBackToZero() throws IOException {
        Path file = temporaryDirectory.resolve("highscore.txt");
        HighScoreRepository repository = new HighScoreRepository(file);

        Files.writeString(file, "not-a-number");
        assertEquals(0, repository.load());

        Files.writeString(file, "-12");
        assertEquals(0, repository.load());
    }

    @Test
    void refusesToSaveNegativeScores() {
        HighScoreRepository repository = new HighScoreRepository(
                temporaryDirectory.resolve("highscore.txt"));

        assertThrows(IllegalArgumentException.class, () -> repository.save(-1));
    }
}
