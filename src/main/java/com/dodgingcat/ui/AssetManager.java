package com.dodgingcat.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;


import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

/**
 * Pluggable asset loader for images and sprites.
 * Gracefully returns null if an asset file is not present, allowing procedural vector rendering fallback.
 */
public class AssetManager {
    private static final Map<String, Image> imageCache = java.util.Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, int[]> contentBoundsCache = new ConcurrentHashMap<>();
    private static final Map<String, Image> isolatedFrameCache = new HashMap<>();
    private static final Map<String, Integer> bottomBaselineCache = new ConcurrentHashMap<>();
    private static final Map<String, Image> previewFrames = new ConcurrentHashMap<>();
    private static boolean gameplayPreloadStarted;
    private static final ExecutorService boundsWorker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "dodgingcat-image-bounds");
        thread.setDaemon(true);
        return thread;
    });
    private static final java.util.concurrent.atomic.AtomicLong nativeWarmupRequest = new java.util.concurrent.atomic.AtomicLong();

    /** Prepare native-sized cached blits before their first animation/pickup frame. */
    public static java.util.concurrent.CompletableFuture<Void> prepareJava2D(double scale) {
        long request = nativeWarmupRequest.incrementAndGet();
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
            var target = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
            var graphics = target.createGraphics();
            try {
                java.util.List<String> paths;
                synchronized(imageCache) { paths=new java.util.ArrayList<>(imageCache.keySet()); }
                for(String path:paths) {
                    if(request!=nativeWarmupRequest.get())return;
                    awaitDecoded(path); DrawContext.prepareImage(getImage(path));
                }
                graphics.scale(scale, scale);
                var gc = new DrawContext(graphics);
                for (String skin : new String[]{"ginger", "tuxedo", "snow_white", "classic_gray", "calico",
                        "siamese", "bengal", "mochi", "blue_russian", "lava", "super", "pirate", "cyber", "ghost", "santa"}) {
                    if (request != nativeWarmupRequest.get()) return;
                    String path = "/assets/images/characters/" + skin + "_sheet.png";
                    for (int frame = 0; frame < 6; frame++) {
                        for (double[] box : new double[][]{{122,70},{102,70},{96,90},{220,138}})
                            drawCharacterPreview(gc,path,frame,0,0,box[0],box[1]);
                    }
                }
                for (String type : new String[]{"shield", "invincibility", "heart", "double_score", "bomb"}) {
                    if (request != nativeWarmupRequest.get()) return;
                    String path = "/assets/images/buffs/" + type + ".png";
                    awaitDecoded(path);
                    // Include every integer native size used by the short spawn pop.
                    for (int pixels = (int)Math.floor(44 * scale * .72); pixels <= Math.ceil(44 * scale); pixels++) {
                        double size = pixels / scale;
                        if (type.equals("heart")) drawImageContentFit(gc,path,0,0,size*1.2,size*1.2);
                        else drawImage(gc,path,0,0,size,size);
                    }
                }
                for (String skin : new String[]{"", "ghost", "cyber", "santa"}) {
                    if (request != nativeWarmupRequest.get()) return;
                    for (boolean can : new boolean[]{false,true}) {
                        String path = skin.isEmpty() ? "/assets/images/currency/" + (can ? "fish_can" : "fish") + ".png"
                                : "/assets/images/currency/skins/" + skin + "_" + (can ? "can" : "fish") + ".png";
                        awaitDecoded(path); drawImageContent(gc,path,0,0,can ? 58 : 62,50);
                    }
                }
            } finally { graphics.dispose(); }
        }, boundsWorker);
    }

    private static void awaitDecoded(String path) {
        Image image = getImage(path);
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (image != null && !image.isError() && image.getProgress() < 1 && System.nanoTime() < deadline) {
            try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }

    /** Starts decoding gameplay art during the boot screen, before pickups can spawn. */
    public static void preloadGameplayAssets() {
        if (gameplayPreloadStarted) return;
        gameplayPreloadStarted = true;
        String[] paths = {
                "/assets/images/ui/title_dodging_cat.png", "/assets/images/ui/button_plaque.png",
                "/assets/images/ui/button_plaque_hover.png",
                "/assets/images/ui/team_tadq.png",
                "/assets/images/ui/countdown/3.png", "/assets/images/ui/countdown/2.png",
                "/assets/images/ui/countdown/1.png", "/assets/images/ui/countdown/go_en.png",
                "/assets/images/ui/countdown/go_vi.png",
                "/assets/images/currency/fish.png", "/assets/images/currency/fish_can.png",
                "/assets/images/buffs/shield.png", "/assets/images/buffs/invincibility.png",
                "/assets/images/buffs/heart.png", "/assets/images/buffs/double_score.png",
                "/assets/images/buffs/bomb.png", "/assets/images/hazards/warning_sign.png",
                "/assets/images/hazards/pendulum.png", "/assets/images/hazards/pendulum_chain.png",
                "/assets/images/hazards/cannonball.png", "/assets/images/hazards/laser_beam_horizontal.png",
                "/assets/images/hazards/laser_beam_vertical.png", "/assets/images/platforms/kitchen_counter.png",
                "/assets/images/platforms/living_room_platform.png", "/assets/images/platforms/bedroom_platform.png",
                "/assets/images/platforms/garden_platform.png", "/assets/images/platforms/rooftop_platform.png"
        };
        for (String path : paths) {
            getImage(path);
            boundsWorker.execute(() -> preloadContentBounds(path));
        }
        for (String skin : new String[]{"ginger", "tuxedo", "snow_white", "classic_gray",
                "calico", "siamese", "bengal", "mochi", "blue_russian", "lava",
                "super", "pirate", "ghost", "santa", "cyber"}) {
            String path = "/assets/images/characters/" + skin + "_sheet.png";
            getImage(path);
            boundsWorker.execute(() -> preloadSpriteMetadata(path, 6, 4));
        }
        for (String map : new String[]{"kitchen", "living_room", "bedroom", "garden", "rooftop"}) {
            getImage("/assets/images/backgrounds/" + map + ".png");
            String path = "/assets/images/hazards/" + map + "_hazards.png";
            getImage(path);
            boundsWorker.execute(() -> preloadSpriteMetadata(path, 2, 3));
        }
        for (String skin : new String[]{"ghost", "cyber", "santa"}) {
            for (String denomination : new String[]{"fish", "can"}) {
                String path = "/assets/images/currency/skins/" + skin + "_" + denomination + ".png";
                getImage(path);
                boundsWorker.execute(() -> preloadContentBounds(path));
            }
        }
    }

    private static void preloadContentBounds(String path) {
        try (var stream = AssetManager.class.getResourceAsStream(path)) {
            if (stream == null) return;
            var bitmap = ImageIO.read(stream);
            if (bitmap == null) return;
            int minX = bitmap.getWidth(), minY = bitmap.getHeight(), maxX = -1, maxY = -1;
            int[] pixels = bitmap.getRGB(0, 0, bitmap.getWidth(), bitmap.getHeight(), null, 0, bitmap.getWidth());
            for (int y = 0; y < bitmap.getHeight(); y++) {
                for (int x = 0; x < bitmap.getWidth(); x++) {
                    if ((pixels[y * bitmap.getWidth() + x] >>> 24) > 5) {
                        minX = Math.min(minX, x); minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                    }
                }
            }
            if (maxX >= minX && maxY >= minY) {
                contentBoundsCache.putIfAbsent(path, new int[]{
                        minX, minY, maxX + 1, maxY + 1});
            }
        } catch (Exception ignored) {
            // Normal draw-time bounds detection remains available if prewarming fails.
        }
    }

    private static void preloadSpriteMetadata(String path, int columns, int rows) {
        try (var stream = AssetManager.class.getResourceAsStream(path)) {
            if (stream == null) return;
            var bitmap = ImageIO.read(stream);
            if (bitmap == null) return;
            int cw = bitmap.getWidth() / columns, ch = bitmap.getHeight() / rows;
            if (columns == 6) {
                int left = cw, top = ch, right = -1, bottom = -1;
                int[] idlePixels = bitmap.getRGB(0, 0, bitmap.getWidth(), ch, null, 0, bitmap.getWidth());
                for (int frame = 0; frame < 6; frame++) {
                    for (int y = 1; y < ch - 1; y++) for (int x = 1; x < cw - 1; x++) {
                        if ((idlePixels[y * bitmap.getWidth() + frame * cw + x] >>> 24) > 20) {
                            left = Math.min(left, x); top = Math.min(top, y);
                            right = Math.max(right, x); bottom = Math.max(bottom, y);
                        }
                    }
                }
                if (right >= left) {
                    int sw = right - left + 1, sh = bottom - top + 1;
                    contentBoundsCache.put(path + "#idle-bounds", new int[]{left, top, sw, sh});
                    // Small individual idle images avoid uploading fifteen complete atlases in the shop.
                    int previewW = sw, previewH = sh;
                    for (int frame = 0; frame < 6; frame++) {
                        var crop = new java.awt.image.BufferedImage(previewW, previewH, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        var graphics = crop.createGraphics();
                        graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        graphics.drawImage(bitmap, 0, 0, previewW, previewH, frame * cw + left, top,
                                frame * cw + left + sw, top + sh, null);
                        graphics.dispose();
                        previewFrames.put(path + "#idle-" + frame, javafx.embed.swing.SwingFXUtils.toFXImage(crop, null));
                    }
                }
            }
            for (int frame = 0; frame < columns * rows; frame++) {
                int sx = (frame % columns) * cw, sy = (frame / columns) * ch;
                if (columns == 6) {
                    int bottom = -1;
                    for (int y = ch - 2; y > 0 && bottom < 0; y--) {
                        for (int x = 1; x < cw - 1; x++) {
                            if ((bitmap.getRGB(sx + x, sy + y) >>> 24) > 30) {
                                bottom = y;
                                break;
                            }
                        }
                    }
                    bottomBaselineCache.putIfAbsent(path + "#baseline-bottom-6x4-" + frame + "-false", bottom);
                } else {
                    int minX = sx + cw - 10, minY = sy + ch - 10, maxX = -1, maxY = -1;
                    for (int y = sy + 10; y < sy + ch - 10; y++) {
                        for (int x = sx + 10; x < sx + cw - 10; x++) {
                            if ((bitmap.getRGB(x, y) >>> 24) > 5) {
                                minX = Math.min(minX, x); minY = Math.min(minY, y);
                                maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                            }
                        }
                    }
                    if (maxX >= minX) contentBoundsCache.putIfAbsent(path + "#frame-2x3-" + frame,
                            new int[]{minX, minY, maxX + 1, maxY + 1});
                }
            }
        } catch (Exception ignored) {
            // Metadata can be calculated by the normal drawing path if unavailable.
        }
    }

    public static Image getImage(String resourcePath) {
        if (imageCache.containsKey(resourcePath)) {
            return imageCache.get(resourcePath);
        }

        try {
            URL resource = AssetManager.class.getResource(resourcePath);
            if (resource != null) {
                // Decode image files off the JavaFX render thread. Before this, the
                // first appearance of each pickup/hazard synchronously decoded its
                // PNG and could stall an otherwise steady gameplay frame.
                Image img = new Image(resource.toExternalForm(), true);
                imageCache.put(resourcePath, img);
                return img;
            }
        } catch (Exception ignored) {
            // Resource not available; fallback to procedural vector rendering
        }

        imageCache.put(resourcePath, null);
        return null;
    }

    public static boolean drawImage(DrawContext gc, String resourcePath,
                                    double x, double y, double width, double height) {
        Image image = getImage(resourcePath);
        if (image == null || image.isError() || image.getProgress() < 1.0) return false;
        gc.drawImage(image, x, y, width, height);
        return true;
    }

    public static boolean drawCharacterPreview(DrawContext gc, String path, int frame,
                                                double x, double y, double w, double h) {
        frame = Math.floorMod(frame, 6);
        Image preview = previewFrames.get(path + "#idle-" + frame);
        if (preview == null) return false;
        double scale = Math.min(w / preview.getWidth(), h / preview.getHeight());
        double dw = preview.getWidth() * scale, dh = preview.getHeight() * scale;
        gc.drawImage(preview, x + (w - dw) / 2.0, y + (h - dh) / 2.0, dw, dh);
        return true;
    }

    /** Draws a selected source area from an image, useful for trimming transparent sprite padding. */
    public static boolean drawImageRegion(DrawContext gc, String resourcePath,
                                          double sourceX, double sourceY, double sourceWidth, double sourceHeight,
                                          double x, double y, double width, double height) {
        Image image = getImage(resourcePath);
        if (image == null || image.isError() || sourceWidth <= 0 || sourceHeight <= 0) return false;
        if (sourceX < 0 || sourceY < 0 || sourceX + sourceWidth > image.getWidth()
                || sourceY + sourceHeight > image.getHeight()) return false;
        gc.drawImage(image, sourceX, sourceY, sourceWidth, sourceHeight, x, y, width, height);
        return true;
    }

    /** Draws the non-transparent content of an image, trimming generator-added transparent margins. */
    public static boolean drawImageContent(DrawContext gc, String resourcePath,
                                           double x, double y, double width, double height) {
        Image image = getImage(resourcePath);
        if (image == null || image.isError() || image.getPixelReader() == null) return false;
        int[] bounds = contentBoundsCache.computeIfAbsent(resourcePath, ignored -> findContentBounds(image));
        if (bounds == null) return false;
        gc.drawImage(image, bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1],
                x, y, width, height);
        return true;
    }

    /** Draws non-transparent image content without changing its aspect ratio. */
    public static boolean drawImageContentFit(DrawContext gc, String resourcePath,
                                              double x, double y, double width, double height) {
        Image image = getImage(resourcePath);
        if (image == null || image.isError() || image.getPixelReader() == null) return false;
        int[] bounds = contentBoundsCache.computeIfAbsent(resourcePath, ignored -> findContentBounds(image));
        if (bounds == null) return false;
        double sourceWidth = bounds[2] - bounds[0];
        double sourceHeight = bounds[3] - bounds[1];
        double scale = Math.min(width / sourceWidth, height / sourceHeight);
        double drawWidth = sourceWidth * scale;
        double drawHeight = sourceHeight * scale;
        gc.drawImage(image, bounds[0], bounds[1], sourceWidth, sourceHeight,
                x + (width - drawWidth) / 2.0, y + (height - drawHeight) / 2.0,
                drawWidth, drawHeight);
        return true;
    }

    private static int[] findContentBounds(Image image) {
        PixelReader reader = image.getPixelReader();
        if (reader == null) return null;
        int minX = (int) image.getWidth(), minY = (int) image.getHeight();
        int maxX = -1, maxY = -1;
        for (int y = 0; y < (int) image.getHeight(); y++) {
            for (int x = 0; x < (int) image.getWidth(); x++) {
                if ((reader.getArgb(x, y) >>> 24) > 5) {
                    minX = Math.min(minX, x); minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < minX || maxY < minY ? null : new int[]{minX, minY, maxX + 1, maxY + 1};
    }

    /** Draws one frame from a horizontal/vertical sprite sheet, falling back cleanly if absent. */
    public static boolean drawSpriteFrame(DrawContext gc, String resourcePath,
                                          int columns, int rows, int frameIndex,
                                          double x, double y, double width, double height) {
        return drawSpriteFrame(gc, resourcePath, columns, rows, frameIndex, 0.0,
                x, y, width, height);
    }

    /** Draws a sprite frame with a small source inset to prevent neighboring-cell bleed during scaling. */
    public static boolean drawSpriteFrame(DrawContext gc, String resourcePath,
                                          int columns, int rows, int frameIndex, double sourceInset,
                                          double x, double y, double width, double height) {
        Image image = getImage(resourcePath);
        if (image == null || image.isError() || columns <= 0 || rows <= 0) return false;
        double frameWidth = image.getWidth() / columns;
        double frameHeight = image.getHeight() / rows;
        if (frameWidth <= sourceInset * 2 || frameHeight <= sourceInset * 2) return false;
        int frameCount = columns * rows;
        int frame = Math.floorMod(frameIndex, frameCount);
        int column = frame % columns;
        int row = frame / columns;
        gc.drawImage(image,
                column * frameWidth + sourceInset, row * frameHeight + sourceInset,
                frameWidth - sourceInset * 2, frameHeight - sourceInset * 2,
                x, y, width, height);
        return true;
    }

    /** Draws a fixed-grid character frame while removing only tiny detached alpha debris. */
    public static boolean drawSpriteFrameIsolated(DrawContext gc, String resourcePath,
                                                  int columns, int rows, int frameIndex,
                                                  double x, double y, double width, double height) {
        Image sheet = getImage(resourcePath);
        if (sheet == null || sheet.isError() || sheet.getPixelReader() == null
                || columns <= 0 || rows <= 0) return false;
        int cellWidth = (int) Math.round(sheet.getWidth() / columns);
        int cellHeight = (int) Math.round(sheet.getHeight() / rows);
        int frame = Math.floorMod(frameIndex, columns * rows);
        int col = frame % columns, row = frame / columns;
        int sourceX = col * cellWidth, sourceY = row * cellHeight;
        String key = resourcePath + "#isolated-" + columns + "x" + rows + "-" + frame;
        Image cleanFrame = isolatedFrameCache.computeIfAbsent(key, ignored ->
                isolateAlphaDebris(sheet, sourceX, sourceY, cellWidth, cellHeight));
        if (cleanFrame == null) return false;
        gc.drawImage(cleanFrame, x, y, width, height);
        return true;
    }

    /** Draws a full sprite-sheet cell at its normal size, shifting only its vertical
     * placement so the opaque feet line up with the shared ground baseline. */
    public static boolean drawSpriteFrameBottomAligned(DrawContext gc, String resourcePath,
                                                       int columns, int rows, int frameIndex,
                                                       double x, double y, double width, double height,
                                                       boolean isolateDebris) {
        Image sheet = getImage(resourcePath);
        if (sheet == null || sheet.isError() || sheet.getPixelReader() == null
                || columns <= 0 || rows <= 0) return false;
        double cellWidth = sheet.getWidth() / columns;
        double cellHeight = sheet.getHeight() / rows;
        int frame = Math.floorMod(frameIndex, columns * rows);
        int column = frame % columns, row = frame / columns;
        int sourceX = (int) Math.floor(column * cellWidth);
        int sourceY = (int) Math.floor(row * cellHeight);
        int sourceRight = (int) Math.ceil((column + 1) * cellWidth);
        int sourceBottom = (int) Math.ceil((row + 1) * cellHeight);
        Image image = sheet;
        if (isolateDebris) {
            String key = resourcePath + "#ground-isolated-" + columns + "x" + rows + "-" + frame;
            final int cleanSourceX = sourceX, cleanSourceY = sourceY;
            final int cleanWidth = sourceRight - sourceX, cleanHeight = sourceBottom - sourceY;
            image = isolatedFrameCache.computeIfAbsent(key, ignored ->
                    isolateAlphaDebris(sheet, cleanSourceX, cleanSourceY, cleanWidth, cleanHeight));
            if (image == null) return false;
            sourceX = 0;
            sourceY = 0;
            sourceRight = (int) image.getWidth();
            sourceBottom = (int) image.getHeight();
            cellHeight = sourceBottom;
        }
        final int scanX = sourceX, scanY = sourceY, scanRight = sourceRight, scanBottom = sourceBottom;
        final double scanCellHeight = cellHeight;
        final Image scanImage = image;
        String baselineKey = resourcePath + "#baseline-bottom-" + columns + "x" + rows + "-" + frame
                + "-" + isolateDebris;
        int bottomMost = bottomBaselineCache.computeIfAbsent(baselineKey, ignored -> {
            int lowest = -1;
            PixelReader reader = scanImage.getPixelReader();
            for (int py = scanBottom - 2; py > scanY; py--) {
                for (int px = scanX + 1; px < scanRight - 1; px++) {
                    if ((reader.getArgb(px, py) >>> 24) > 30) return py - scanY;
                }
            }
            return lowest;
        });
        if (bottomMost < 0) return false;
        double contentBottomRatio = Math.clamp(bottomMost / scanCellHeight, 0.0, 1.0);
        double alignedY = y + height - contentBottomRatio * height;
        gc.drawImage(image, sourceX + 1, sourceY + 1,
                Math.max(1, sourceRight - sourceX - 2), Math.max(1, sourceBottom - sourceY - 2),
                x, alignedY, width, height);
        return true;
    }

    /** Draw a cell with a small horizontal bleed gutter, retaining the dominant
     * connected sprite pixels that cross the generated sheet's cell boundary. */
    public static boolean drawSpriteFrameBleedAligned(DrawContext gc, String resourcePath,
                                                       int columns, int rows, int frameIndex,
                                                       double x, double y, double width, double height,
                                                       boolean alignBottom) {
        return drawSpriteFrameBleedAligned(gc, resourcePath, columns, rows, frameIndex,
                x, y, width, height, alignBottom, 1);
    }

    /** Draw a bleed-aware sprite cell, optionally ignoring tiny detached pixels when
     * finding the foot baseline. Spirit sheets contain small droplets beneath some
     * poses; those should not pull the cat body upward from platforms. */
    public static boolean drawSpriteFrameBleedAligned(DrawContext gc, String resourcePath,
                                                       int columns, int rows, int frameIndex,
                                                       double x, double y, double width, double height,
                                                       boolean alignBottom, int minimumBaselinePixels) {
        Image sheet = getImage(resourcePath);
        if (sheet == null || sheet.isError() || sheet.getPixelReader() == null
                || columns <= 0 || rows <= 0) return false;
        int cellWidth = (int) Math.round(sheet.getWidth() / columns);
        int cellHeight = (int) Math.round(sheet.getHeight() / rows);
        int frame = Math.floorMod(frameIndex, columns * rows);
        int col = frame % columns, row = frame / columns;
        int sourceX = col * cellWidth, sourceY = row * cellHeight;
        int pad = 18;
        int expandedX = Math.max(0, sourceX - pad);
        int expandedRight = Math.min((int) sheet.getWidth(), sourceX + cellWidth + pad);
        int expandedWidth = expandedRight - expandedX;
        int sourceOffsetX = sourceX - expandedX;
        String key = resourcePath + "#bleed-aligned-" + columns + "x" + rows + "-" + frame;
        Image isolated = isolatedFrameCache.computeIfAbsent(key, ignored -> {
            PixelReader reader = sheet.getPixelReader();
            int count = expandedWidth * cellHeight;
            boolean[] opaque = new boolean[count];
            int[] labels = new int[count];
            int[] queue = new int[count];
            for (int py = 0; py < cellHeight; py++) for (int px = 0; px < expandedWidth; px++)
                opaque[py * expandedWidth + px] = (reader.getArgb(expandedX + px, sourceY + py) >>> 24) > 20;
            int nextLabel = 0, selectedLabel = 0, selectedInsideArea = 0;
            for (int i = 0; i < count; i++) {
                if (!opaque[i] || labels[i] != 0) continue;
                int label = ++nextLabel, head = 0, tail = 0, insideArea = 0;
                queue[tail++] = i;
                labels[i] = label;
                while (head < tail) {
                    int at = queue[head++], px = at % expandedWidth, py = at / expandedWidth;
                    if (px >= sourceOffsetX && px < sourceOffsetX + cellWidth) insideArea++;
                    for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                        if ((dx == 0 && dy == 0) || px + dx < 0 || px + dx >= expandedWidth
                                || py + dy < 0 || py + dy >= cellHeight) continue;
                        int n = (py + dy) * expandedWidth + px + dx;
                        if (opaque[n] && labels[n] == 0) { labels[n] = label; queue[tail++] = n; }
                    }
                }
                if (insideArea > selectedInsideArea) { selectedInsideArea = insideArea; selectedLabel = label; }
            }
            if (selectedLabel == 0) return null;
            WritableImage frameImage = new WritableImage(expandedWidth, cellHeight);
            PixelWriter writer = frameImage.getPixelWriter();
            for (int py = 0; py < cellHeight; py++) for (int px = 0; px < expandedWidth; px++) {
                if (labels[py * expandedWidth + px] == selectedLabel)
                    writer.setArgb(px, py, reader.getArgb(expandedX + px, sourceY + py));
            }
            return frameImage;
        });
        if (isolated == null) return false;
        final Image baselineImage = isolated;
        String baselineKey = key + "#baseline-" + minimumBaselinePixels;
        int bottomMost = bottomBaselineCache.computeIfAbsent(baselineKey, ignored -> {
            int lowest = -1;
            PixelReader reader = baselineImage.getPixelReader();
            for (int py = 0; py < cellHeight; py++) {
                int opaquePixels = 0;
                for (int px = 0; px < expandedWidth; px++)
                    if ((reader.getArgb(px, py) >>> 24) > 20) opaquePixels++;
                if (opaquePixels >= Math.max(1, minimumBaselinePixels)) lowest = py;
            }
            if (lowest < 0) {
                for (int py = 0; py < cellHeight; py++) for (int px = 0; px < expandedWidth; px++)
                    if ((reader.getArgb(px, py) >>> 24) > 20) lowest = Math.max(lowest, py);
            }
            return lowest;
        });
        if (bottomMost < 0) return false;
        double alignedY = alignBottom
                ? y + height - (bottomMost / (double) cellHeight) * height
                : y;
        double scaleX = width / cellWidth;
        double destX = x - sourceOffsetX * scaleX;
        gc.drawImage(isolated, 0, 0, expandedWidth, cellHeight, destX, alignedY,
                expandedWidth * scaleX, height);
        return true;
    }

    private static Image isolateAlphaDebris(Image sheet, int sourceX, int sourceY,
                                            int cellWidth, int cellHeight) {
        PixelReader reader = sheet.getPixelReader();
        int count = cellWidth * cellHeight;
        boolean[] opaque = new boolean[count];
        int[] labels = new int[count];
        int[] queue = new int[count];
        for (int py = 0; py < cellHeight; py++) for (int px = 0; px < cellWidth; px++)
            opaque[py * cellWidth + px] = (reader.getArgb(sourceX + px, sourceY + py) >>> 24) > 63;

        int label = 0, largestLabel = 0, largestArea = 0;
        for (int i = 0; i < count; i++) {
            if (!opaque[i] || labels[i] != 0) continue;
            int currentLabel = ++label, head = 0, tail = 0;
            queue[tail++] = i;
            labels[i] = currentLabel;
            while (head < tail) {
                int at = queue[head++], px = at % cellWidth, py = at / cellWidth;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    if ((dx == 0 && dy == 0) || px + dx < 0 || px + dx >= cellWidth
                            || py + dy < 0 || py + dy >= cellHeight) continue;
                    int next = (py + dy) * cellWidth + px + dx;
                    if (opaque[next] && labels[next] == 0) {
                        labels[next] = currentLabel;
                        queue[tail++] = next;
                    }
                }
            }
            if (tail > largestArea) { largestLabel = currentLabel; largestArea = tail; }
        }
        if (largestArea == 0) return null;

        WritableImage result = new WritableImage(cellWidth, cellHeight);
        PixelWriter writer = result.getPixelWriter();
        for (int py = 0; py < cellHeight; py++) for (int px = 0; px < cellWidth; px++) {
            int component = labels[py * cellWidth + px];
            if (component == largestLabel) {
                writer.setArgb(px, py, reader.getArgb(sourceX + px, sourceY + py));
            }
        }
        return result;
    }

    /** Crops transparent padding from one sprite-sheet frame, preserving the shared frame grid. */
    public static boolean drawSpriteFrameContent(DrawContext gc, String resourcePath,
                                                 int columns, int rows, int frameIndex, double sourceInset,
                                                 double x, double y, double width, double height) {
        Image image = getImage(resourcePath);
        if (image == null || image.isError() || image.getPixelReader() == null || columns <= 0 || rows <= 0) return false;
        double frameWidth = image.getWidth() / columns;
        double frameHeight = image.getHeight() / rows;
        if (frameWidth <= sourceInset * 2 || frameHeight <= sourceInset * 2) return false;
        int frame = Math.floorMod(frameIndex, columns * rows);
        int column = frame % columns;
        int row = frame / columns;
        String key = resourcePath + "#frame-" + columns + "x" + rows + "-" + frame;
        int[] bounds = contentBoundsCache.computeIfAbsent(key, ignored -> {
            PixelReader reader = image.getPixelReader();
            int left = (int) Math.floor(column * frameWidth + sourceInset);
            int top = (int) Math.floor(row * frameHeight + sourceInset);
            int right = (int) Math.ceil((column + 1) * frameWidth - sourceInset);
            int bottom = (int) Math.ceil((row + 1) * frameHeight - sourceInset);
            int minX = right, minY = bottom, maxX = left - 1, maxY = top - 1;
            for (int py = top; py < bottom; py++) {
                for (int px = left; px < right; px++) {
                    if ((reader.getArgb(px, py) >>> 24) > 5) {
                        minX = Math.min(minX, px); minY = Math.min(minY, py);
                        maxX = Math.max(maxX, px); maxY = Math.max(maxY, py);
                    }
                }
            }
            return maxX < minX || maxY < minY ? null : new int[]{minX, minY, maxX + 1, maxY + 1};
        });
        if (bounds == null) return false;
        gc.drawImage(image, bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1],
                x, y, width, height);
        return true;
    }

    public static void clearCache() {
        gameplayPreloadStarted = false;
        imageCache.clear();
        contentBoundsCache.clear();
        isolatedFrameCache.clear();
        bottomBaselineCache.clear();
        previewFrames.clear();
    }
}
