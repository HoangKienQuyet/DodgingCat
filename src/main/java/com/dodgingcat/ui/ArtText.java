package com.dodgingcat.ui;


import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.text.TextAlignment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Draws authored raster glyphs; JavaFX fonts only supply the requested text size. */
public final class ArtText {
    private static final int CELL_W = 80;
    private static final int CELL_H = 84;
    private static final int COLS = 16;
    private static final double MASTER_SIZE = 52.0;
    private static final Map<Integer, Glyph> GLYPHS = new HashMap<>();
    private static final Image ATLAS;
    private static final BufferedImage ATLAS_BITMAP;
    private static final Map<String, TextSprite> TEXT_CACHE = new LinkedHashMap<>(64, 0.75f, true);
    private static final long MAX_CACHED_PIXELS = 4_000_000;
    private static long cachedPixels;

    static {
        var imageUrl = ArtText.class.getResource("/assets/images/ui/art_glyphs.png");
        ATLAS = imageUrl == null ? null : new Image(imageUrl.toExternalForm(), false);
        try {
            ATLAS_BITMAP = imageUrl == null ? null : ImageIO.read(imageUrl);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load raster UI atlas", e);
        }
        try (var reader = new BufferedReader(new InputStreamReader(
                ArtText.class.getResourceAsStream("/assets/images/ui/art_glyphs.txt"), StandardCharsets.UTF_8))) {
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(" ");
                GLYPHS.put(Integer.parseInt(values[0]), new Glyph(index++, Integer.parseInt(values[1])));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load raster UI glyphs", e);
        }
    }

    private record Glyph(int index, int advance) {}
    private record TextSprite(Image image, int advance, int pixelWidth) {}
    private ArtText() {}

    public static void fillText(DrawContext gc, String text, double x, double baseline) {
        fillText(gc, text, x, baseline, Double.POSITIVE_INFINITY);
    }

    public static void fillText(DrawContext gc, String text, double x, double baseline, double maxWidth) {
        if (ATLAS == null || text == null || text.isEmpty()) return;
        double scale = gc.getFont().getSize() / MASTER_SIZE;
        TextSprite sprite = TEXT_CACHE.get(text);
        if (sprite == null) sprite = cacheText(text);
        double naturalWidth = sprite.advance * scale;
        if (Double.isFinite(maxWidth) && naturalWidth > maxWidth) {
            scale *= maxWidth / naturalWidth;
            naturalWidth = maxWidth;
        }
        double left = switch (gc.getTextAlign()) {
            case CENTER -> x - naturalWidth / 2.0;
            case RIGHT -> x - naturalWidth;
            default -> x;
        };
        // Submit one cached image rather than one Canvas command per letter every frame.
        gc.drawImage(sprite.image, left - 6 * scale, baseline - 62 * scale,
                sprite.pixelWidth * scale, CELL_H * scale);
    }

    private static TextSprite cacheText(String text) {
        int[] chars = text.codePoints().toArray();
        int advance = 0;
        for (int c : chars) {
            Glyph glyph = GLYPHS.get(c);
            advance += glyph == null ? 26 : glyph.advance;
        }
        int width = Math.max(1, advance + 14);
        BufferedImage composed = new BufferedImage(width, CELL_H, BufferedImage.TYPE_INT_ARGB);
        var graphics = composed.createGraphics();
        int cursor = 0;
        for (int c : chars) {
            Glyph glyph = GLYPHS.get(c);
            if (glyph == null) { cursor += 26; continue; }
            if (c != ' ') {
                int sx = (glyph.index % COLS) * CELL_W;
                int sy = (glyph.index / COLS) * CELL_H;
                int sourceWidth = Math.min(CELL_W, glyph.advance + 14);
                graphics.drawImage(ATLAS_BITMAP, cursor, 0, cursor + sourceWidth, CELL_H,
                        sx, sy, sx + sourceWidth, sy + CELL_H, null);
            }
            cursor += glyph.advance;
        }
        graphics.dispose();
        TextSprite sprite = new TextSprite(SwingFXUtils.toFXImage(composed, null), advance, width);
        TEXT_CACHE.put(text, sprite);
        cachedPixels += (long) width * CELL_H;
        var entries = TEXT_CACHE.entrySet().iterator();
        while (cachedPixels > MAX_CACHED_PIXELS && TEXT_CACHE.size() > 1 && entries.hasNext()) {
            var oldest = entries.next().getValue();
            cachedPixels -= (long) oldest.pixelWidth * CELL_H;
            entries.remove();
        }
        return sprite;
    }
}
