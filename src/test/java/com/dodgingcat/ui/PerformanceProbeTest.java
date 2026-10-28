package com.dodgingcat.ui;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Explicit diagnostic: run separately from the UI toolkit smoke test. */
@EnabledIfSystemProperty(named = "dodgingcat.perfProbe", matches = "true")
class PerformanceProbeTest {
    @Test
    void recordsPulseGateLossAndPickupRendering() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            AssetManager.preloadGameplayAssets();
            Canvas canvas = new Canvas(1920, 1080);
            WritableImage pixels = new WritableImage(1920, 1080);
            PauseTransition load = new PauseTransition(Duration.seconds(4));
            load.setOnFinished(event -> new AnimationTimer() {
                long start, lastPulse, lastOldFrame;
                int pulses, oldFrames;
                boolean measuringDraws;
                String idleReport;
                final List<Double> intervals = new ArrayList<>();
                final List<Double> draws = new ArrayList<>();
                @Override public void handle(long now) {
                    try {
                        if (start == 0) start = now;
                        if (lastPulse != 0) intervals.add((now - lastPulse) / 1_000_000.0);
                        lastPulse = now;
                        pulses++;
                        if (lastOldFrame == 0 || now - lastOldFrame >= 1_000_000_000L / 60) {
                            oldFrames++;
                            lastOldFrame = now;
                        }
                        if (!measuringDraws) {
                            if (now - start < 2_000_000_000L) return;
                            double seconds = (now - start) / 1_000_000_000.0;
                            idleReport = String.format(java.util.Locale.ROOT,
                                    "Unloaded JavaFX pulses=%.2f/s; old exact60fps gate accepts=%.2f/s (%d/%d)\n",
                                    pulses / seconds, oldFrames / seconds, oldFrames, pulses);
                            measuringDraws = true;
                            start = 0; lastPulse = 0; lastOldFrame = 0;
                            pulses = 0; oldFrames = 0; intervals.clear();
                            return;
                        }
                        var gc = new DrawContext(canvas.getGraphicsContext2D());
                        long drawStart = System.nanoTime();
                        AssetManager.drawImage(gc, "/assets/images/backgrounds/kitchen.png", 0, 0, 1920, 1080);
                        for (int i = 0; i < 24; i++) {
                            AssetManager.drawImageContent(gc, "/assets/images/currency/fish.png",
                                    60 + i * 73, 550 + (i % 3) * 80, 44, 40);
                        }
                        String[] buffs = {"heart", "shield", "invincibility", "bomb", "double_score"};
                        for (int i = 0; i < buffs.length; i++) {
                            AssetManager.drawImageContent(gc, "/assets/images/buffs/" + buffs[i] + ".png",
                                    180 + i * 310, 240, 54, 54);
                        }
                        AssetManager.drawSpriteFrameBottomAligned(gc, "/assets/images/characters/ginger_sheet.png",
                                6, 4, 6 + (pulses / 9) % 6, 700, 800, 130, 120, false);
                        gc.setFont(Font.font(24));
                        ArtText.fillText(gc, "TIME: " + (pulses / 6) + "  SCORE: 120  FISH: 25", 30, 50);
                        canvas.snapshot(new SnapshotParameters(), pixels);
                        draws.add((System.nanoTime() - drawStart) / 1_000_000.0);
                        if (now - start >= 6_000_000_000L) {
                            stop();
                            double seconds = (now - start) / 1_000_000_000.0;
                            String report = idleReport + String.format(java.util.Locale.ROOT,
                                    "1920x1080 forced Canvas snapshot diagnostic, %.2fs\n" +
                                    "JavaFX pulses=%d (%.2f/s), old 60fps gate accepts=%d (%.2f/s)\n" +
                                    "Pulse interval median=%.2fms p95=%.2fms max=%.2fms\n" +
                                    "Draw+GPU readback median=%.2fms p95=%.2fms max=%.2fms\n",
                                    seconds, pulses, pulses / seconds, oldFrames, oldFrames / seconds,
                                    percentile(intervals, .5), percentile(intervals, .95), Collections.max(intervals),
                                    percentile(draws, .5), percentile(draws, .95), Collections.max(draws));
                            Files.createDirectories(Path.of("target"));
                            Files.writeString(Path.of("target/performance-probe.txt"), report);
                            System.out.println(report);
                            done.countDown();
                            Platform.runLater(Platform::exit);
                        }
                    } catch (Throwable t) {
                        failure.set(t); stop(); done.countDown(); Platform.runLater(Platform::exit);
                    }
                }
            }.start());
            load.play();
        });
        assertTrue(done.await(35, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static double percentile(List<Double> values, double fraction) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(Math.min(sorted.size() - 1, (int) (sorted.size() * fraction)));
    }
}
