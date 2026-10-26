package com.dodgingcat.ui;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.image.Image;

/** Loads optional PNG artwork from the application classpath and caches each result. */
public final class ClasspathImageLoader {

    private static final System.Logger LOGGER = System.getLogger(ClasspathImageLoader.class.getName());

    private final Map<String, Optional<Image>> cache = new ConcurrentHashMap<>();

    /**
     * Loads a PNG at an absolute classpath resource path. A missing or invalid image returns empty
     * so presentation code can retain its existing fallback without changing game state.
     */
    public Optional<Image> loadPng(String resourcePath) {
        validatePngPath(resourcePath);
        return cache.computeIfAbsent(resourcePath, this::readPng);
    }

    private Optional<Image> readPng(String resourcePath) {
        try (InputStream input = ClasspathImageLoader.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Optional pixel asset is missing: " + resourcePath);
                return Optional.empty();
            }

            Image image = new Image(input);
            if (image.isError()) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Optional pixel asset could not be decoded: " + resourcePath,
                        image.getException());
                return Optional.empty();
            }
            return Optional.of(image);
        } catch (Exception exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Optional pixel asset could not be loaded: " + resourcePath, exception);
            return Optional.empty();
        }
    }

    private void validatePngPath(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resource path must not be null");
        if (!resourcePath.startsWith(PixelAssetPaths.ROOT) || !resourcePath.endsWith(".png")) {
            throw new IllegalArgumentException("Pixel asset paths must be PNG files below "
                    + PixelAssetPaths.ROOT + ": " + resourcePath);
        }
    }
}
