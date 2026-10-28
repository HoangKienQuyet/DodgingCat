package com.dodgingcat.ui;

import com.dodgingcat.audio.AudioManager;
import com.dodgingcat.core.GameMode;
import com.dodgingcat.core.GameState;
import com.dodgingcat.game.GameWorld;
import com.dodgingcat.input.InputManager;
import com.dodgingcat.persistence.GameData;
import com.dodgingcat.persistence.PersistenceManager;
import com.dodgingcat.shop.GachaSequence;
import com.dodgingcat.shop.ShopManager;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Screen;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in real Stage diagnostic. Run separately: JavaFX toolkit starts once per JVM. */
@EnabledIfSystemProperty(named = "dodgingcat.sceneProbe", matches = "true")
class SceneFrameProbeTest {
    @Test void measuresWindowedAndFullscreenScenes() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        com.dodgingcat.app.RenderSurface.configureRuntime();
        int heightCap = Integer.getInteger("dodgingcat.probe.heightCap", com.dodgingcat.app.RenderSurface.DEFAULT_HEIGHT_LIMIT);
        boolean borderless = Boolean.getBoolean("dodgingcat.probe.borderless");
        int firstScenario = Integer.getInteger("dodgingcat.probe.firstScenario", 0);
        int scenarioCount = Integer.getInteger("dodgingcat.probe.scenarioCount", 8 - firstScenario);
        int renderHeight = Integer.getInteger("dodgingcat.probe.renderHeight", 0);
        double sampleSeconds = Double.parseDouble(System.getProperty("dodgingcat.probe.sampleSeconds", "3"));
        String reportLabel = System.getProperty("dodgingcat.probe.label", "").replaceAll("[^a-zA-Z0-9_-]", "");
        Platform.startup(() -> {
            AudioManager.getInstance().setMuted(true);
            Stage stage = new Stage(borderless ? StageStyle.UNDECORATED : StageStyle.DECORATED);
            Canvas canvas = new Canvas(960, 640);
            StackPane root = new StackPane(canvas);
            stage.setScene(new Scene(root, 960, 640));
            stage.setFullScreenExitHint("");
            stage.show();
            if (firstScenario >= 4) stage.setFullScreen(true);
            stage.toFront(); stage.requestFocus();
            var data = new GameData();
            var persistence = new PersistenceManager(Path.of("target/scene-probe-profile.json"));
            var shop = new ShopManager(persistence);
            var renderer = new GameRenderer(960, 640);
            var input = new InputManager();
            PauseTransition load = new PauseTransition(Duration.seconds(5));
            load.setOnFinished(event -> new AnimationTimer() {
                final GameState[] states = {GameState.MENU, GameState.SHOP, GameState.PLAYING_COOP, GameState.SHOP};
                final StringBuilder report = new StringBuilder("Visible Stage; no readback in measurement; 1s warmup + " + sampleSeconds + "s samples.\n");
                final List<Double> intervals = new ArrayList<>(), submissions = new ArrayList<>();
                int scenario = firstScenario;
                long started, previous;
                boolean changing, inHandle;
                boolean countingPresentedFrames;
                Object tracker;
                java.lang.reflect.Method resetFps, averageFps;
                GameWorld world;
                GachaSequence gacha;

                private void prepare() {
                    double logicalWidth = root.getWidth() / root.getHeight() * 640;
                    renderer.resize(logicalWidth, 640);
                    world = new GameWorld(GameMode.LOCAL_COOP, "kitchen", logicalWidth, 640, "ginger", "tuxedo");
                    // Exercise actual pickups and several hazard textures, not just an empty arena.
                    int itemIndex = 0;
                    for (var type : com.dodgingcat.buffs.BuffType.values()) {
                        world.getBuffs().add(new com.dodgingcat.buffs.Buff(type, 70 + itemIndex++ * 165, 250, 30));
                    }
                    for (int i = 0; i < 5; i++) {
                        world.getFishes().add(new com.dodgingcat.game.FishItem(
                                i % 2 == 0 ? com.dodgingcat.game.FishType.SINGLE_FISH : com.dodgingcat.game.FishType.FISH_CAN,
                                80 + i * 160, 350));
                        world.getHazards().add(new com.dodgingcat.hazards.TopDownHazard(90 + i * 150, 70,
                                42, 50, new String[]{"knife", "frying_pan", "rolling_pin", "salt_shaker", "blender"}[i], 60, 0));
                    }
                    gacha = new GachaSequence(shop.getSkinCatalog(), shop.findItemById("cyber"), new Random(42));
                    started = 0; previous = 0; intervals.clear(); submissions.clear();
                    countingPresentedFrames = false;
                    try {
                        // Diagnostic only: this JavaFX tracker counts presented frames, independently of pulse callbacks.
                        var type = Class.forName("com.sun.javafx.perf.PerformanceTracker");
                        tracker = type.getMethod("getSceneTracker", Scene.class).invoke(null, stage.getScene());
                        resetFps = type.getMethod("resetAverageFPS"); averageFps = type.getMethod("getAverageFPS");
                    } catch (ReflectiveOperationException ignored) { tracker = null; }
                }

                @Override public void handle(long now) {
                    // Fullscreen transitions can run a nested Windows event loop.
                    if (changing || inHandle) return;
                    inHandle = true;
                    try {
                        if (started == 0) { prepare(); started = now; }
                        double age = (now - started) / 1e9;
                        if (age >= 1 && !countingPresentedFrames) {
                            if (tracker != null) resetFps.invoke(tracker);
                            countingPresentedFrames = true;
                        }
                        double dt = previous == 0 ? 1.0 / 60 : Math.min(.05, (now - previous) / 1e9);
                        if (previous != 0 && age >= 1) intervals.add((now - previous) / 1e6);
                        previous = now;
                        double viewportH = root.getHeight(), viewportW = root.getWidth();
                        if (renderHeight > 0) {
                            double sceneScale = Math.min(1, renderHeight / viewportH);
                            stage.setRenderScaleX(sceneScale); stage.setRenderScaleY(sceneScale);
                        }
                        com.dodgingcat.app.RenderSurface.resize(canvas, viewportW, viewportH, heightCap);
                        double bufferH = canvas.getHeight(), bufferW = canvas.getWidth();
                        renderer.resize(viewportW / viewportH * 640, 640);
                        renderer.updateAnimationTimer(dt);
                        int index = scenario % 4;
                        if (index == 2) {
                            world.getPlayers().forEach(player -> player.applyBuff(com.dodgingcat.buffs.BuffType.INVINCIBILITY));
                            world.update(dt);
                        }
                        if (index == 3) gacha.update(dt, Math.min(.995, age / 14), false, false);
                        var gc = new DrawContext(canvas.getGraphicsContext2D());
                        long drawStart = System.nanoTime();
                        gc.save(); gc.scale(bufferH / 640, bufferH / 640);
                        if (Boolean.getBoolean("dodgingcat.probe.backgroundOnly")) {
                            AssetManager.drawImage(gc, "/assets/images/backgrounds/kitchen.png", 0, 0, viewportW / viewportH * 640, 640);
                        } else {
                            renderer.render(gc, states[index], index == 2 ? world : null, Theme.KITCHEN, data, shop, input,
                                    0, 0, 0, 0, 0, 0, 1, false, false, 1, GameMode.LOCAL_COOP, false,
                                    0, 0, 0, 3, 0, index == 3 ? gacha : null, 100);
                        }
                        gc.restore();
                        if (age >= 1) submissions.add((System.nanoTime() - drawStart) / 1e6);
                        if (age >= 1 + sampleSeconds) {
                            double mean = intervals.stream().mapToDouble(Double::doubleValue).average().orElse(1000);
                            report.append(String.format(Locale.ROOT,
                                    "%s %-12s buffer=%.0fx%.0f focused=%s fps=%.2f presented=%.2f frameP95=%.2fms max=%.2fms submitMedian=%.3fms%n",
                                    scenario < 4 ? "Windowed" : "Fullscreen", index == 3 ? "SPIN" : states[index],
                                    bufferW, bufferH, stage.isFocused(), 1000 / mean, tracker == null ? Double.NaN : ((Number) averageFps.invoke(tracker)).doubleValue(),
                                    percentile(intervals, .95), Collections.max(intervals), percentile(submissions, .5)));
                            scenario++;
                            if (scenario >= firstScenario + scenarioCount) {
                                stop(); Files.writeString(Path.of("target/scene-frame-probe-" + heightCap
                                        + (reportLabel.isEmpty() ? "" : "-" + reportLabel) + ".txt"), report);
                                System.out.print(report); stage.close(); done.countDown(); Platform.runLater(Platform::exit);
                            } else {
                                changing = true;
                                Platform.runLater(() -> {
                                    if (scenario == 4) {
                                        if (borderless) {
                                            var bounds = Screen.getPrimary().getBounds();
                                            stage.setX(bounds.getMinX()); stage.setY(bounds.getMinY());
                                            stage.setWidth(bounds.getWidth()); stage.setHeight(bounds.getHeight());
                                        } else stage.setFullScreen(true);
                                        stage.toFront(); stage.requestFocus();
                                    }
                                    // Defer preparation until the new viewport has completed its layout.
                                    PauseTransition settle = new PauseTransition(Duration.millis(250));
                                    settle.setOnFinished(e -> { started = 0; changing = false; }); settle.play();
                                });
                            }
                        }
                    } catch (Throwable t) {
                        failure.set(t); stop(); stage.close(); done.countDown(); Platform.runLater(Platform::exit);
                    } finally { inHandle = false; }
                }
            }.start());
            load.play();
        });
        long timeoutSeconds = Math.max(65, (long) Math.ceil(20 + scenarioCount * (sampleSeconds + 2)));
        assertTrue(done.await(timeoutSeconds, TimeUnit.SECONDS), "Scene probe timed out");
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static double percentile(List<Double> values, double fraction) {
        var sorted = new ArrayList<>(values); Collections.sort(sorted);
        return sorted.get(Math.min(sorted.size() - 1, (int) (sorted.size() * fraction)));
    }
}
