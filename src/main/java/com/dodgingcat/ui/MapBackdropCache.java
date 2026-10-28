package com.dodgingcat.ui;

import javafx.embed.swing.SwingFXUtils;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/** Prepare viewport-sized, floor-aligned backdrops off the game thread. */
final class MapBackdropCache {
    enum Shade { NORMAL, MENU, SHOP, SPIN, REVEAL, RESULT }
    private record Key(String map, int width, int height, Shade shade) {}
    private static final Map<Key, Image> READY = new ConcurrentHashMap<>();
    private static final Set<Key> PENDING = ConcurrentHashMap.newKeySet();
    private static final Map<String, BufferedImage> SOURCES = new HashMap<>(); // Worker-owned.
    private static final ArrayDeque<Key> ORDER = new ArrayDeque<>();
    private static final java.util.concurrent.ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "dodgingcat-backdrops"); thread.setDaemon(true); return thread;
    });
    private MapBackdropCache() {}


    static boolean draw(DrawContext gc, String map, double width, double height, Shade shade) {
        Image image = getPrepared(gc, map, width, height, shade);
        if (image == null) return false;
        gc.drawImage(image, 0, 0, width, height);
        return true;
    }

    static Image getPrepared(DrawContext gc, String map, double width, double height, Shade shade) {
        var transform = gc.getTransform();
        int pixelsW = Math.max(1, (int) Math.round(width * Math.abs(transform.getMxx())));
        int pixelsH = Math.max(1, (int) Math.round(height * Math.abs(transform.getMyy())));
        Key key = new Key(map, pixelsW, pixelsH, shade);
        Image image = READY.get(key);
        if (image != null) return image;
        if (PENDING.add(key)) WORKER.execute(() -> prepare(key));
        return null;
    }

    private static void prepare(Key key) {
        try {
            BufferedImage source = SOURCES.get(key.map);
            if (source == null) {
                try (var stream = MapBackdropCache.class.getResourceAsStream("/assets/images/backgrounds/" + key.map + ".png")) {
                    if (stream == null) return;
                    source = ImageIO.read(stream);
                }
                if (source == null) return;
                SOURCES.put(key.map, source);
            }
            double groundRatio = switch (key.map) {
                case "living_room" -> .780;
                case "garden" -> .765;
                default -> .8555;
            };
            double floorY = key.height * .8555, sourceGround = source.getHeight() * groundRatio;
            double scale = Math.max(key.width / (double) source.getWidth(), key.height / (double) source.getHeight());
            scale = Math.max(scale, floorY / sourceGround);
            scale = Math.max(scale, (key.height - floorY) / (source.getHeight() - sourceGround));
            var normal = new BufferedImage(key.width, key.height, BufferedImage.TYPE_INT_RGB);
            var graphics = normal.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            var placement = java.awt.geom.AffineTransform.getTranslateInstance(
                    (key.width - source.getWidth() * scale) / 2, floorY - sourceGround * scale);
            placement.scale(scale, scale);
            graphics.drawImage(source, placement, null); graphics.dispose();
            BufferedImage output = switch (key.shade) {
                case NORMAL -> normal;
                case MENU -> shade(normal, new java.awt.Color(18, 22, 30), .38f);
                case SHOP -> shade(shade(normal, new java.awt.Color(18, 22, 30), .38f), java.awt.Color.BLACK, .50f);
                case SPIN -> shade(normal, java.awt.Color.BLACK, .84f);
                case REVEAL -> shade(normal, java.awt.Color.BLACK, .88f);
                case RESULT -> shade(normal, java.awt.Color.BLACK, .78f);
            };
            Image prepared=SwingFXUtils.toFXImage(output,null);
            // Keep a managed native bitmap ready before publishing the FX handle.
            // A phase transition must not convert/scan a full-HD image on the game thread.
            var nativeBitmap=new BufferedImage(key.width,key.height,BufferedImage.TYPE_INT_RGB);
            var nativeGraphics=nativeBitmap.createGraphics();nativeGraphics.drawImage(output,0,0,null);nativeGraphics.dispose();
            DrawContext.prepareNativeBackdrop(prepared,nativeBitmap);
            READY.put(key, prepared); ORDER.addLast(key);
            // A resized window must not retain every historical framebuffer size.
            while (ORDER.size() > 12) READY.remove(ORDER.removeFirst());
        } catch (Exception ignored) {
            // The original image path remains available while preparation is pending or fails.
        } finally { PENDING.remove(key); }
    }

    private static BufferedImage shade(BufferedImage source, java.awt.Color color, float alpha) {
        var output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = output.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.setComposite(AlphaComposite.SrcOver.derive(alpha)); graphics.setColor(color);
        graphics.fillRect(0, 0, output.getWidth(), output.getHeight()); graphics.dispose();
        return output;
    }
}
