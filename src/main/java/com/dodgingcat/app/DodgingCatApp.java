package com.dodgingcat.app;

import com.dodgingcat.core.GameController;
import com.dodgingcat.audio.AudioManager;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Main JavaFX Application with fullscreen support, responsive canvas scaling,
 * and coordinate-mapped mouse & keyboard listeners.
 */
public class DodgingCatApp extends Application {
    public static final double WINDOW_WIDTH = 960.0;
    public static final double WINDOW_HEIGHT = 640.0;
    private static Stage mainStage;
    private GameController gameController;
    private long lastNanoTime = 0;
    private long fpsWindowStartNanoTime = 0;
    private int fpsFrameCount = 0;
    private long updateTimeAccumNanos = 0;
    private long drawTimeAccumNanos = 0;

    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;

        Canvas canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gameController = new GameController(WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) gameController.onWindowFocusLost();
        });
        primaryStage.setOnCloseRequest(event -> {
            gameController.onApplicationExit();
        });

        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #0A0A0F;");
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

        int renderHeightLimit = Integer.getInteger("dodgingcat.renderHeight", RenderSurface.DEFAULT_HEIGHT_LIMIT);

        // Start with the game using the whole display. F11 still toggles windowed mode.
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // Keyboard listeners
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                toggleFullScreen();
                return;
            }
            gameController.getInputManager().onKeyPressed(event.getCode());
        });
        scene.setOnKeyReleased(event -> gameController.getInputManager().onKeyReleased(event.getCode()));

        // Mouse listeners map the uniform-scale, responsive game viewport to logical coordinates.
        scene.setOnMouseMoved(event -> {
            double vx = toVirtualX(event.getSceneX(), root.getWidth(), root.getHeight());
            double vy = toVirtualY(event.getSceneY(), root.getWidth(), root.getHeight());
            gameController.getInputManager().onMouseMoved(vx, vy);
        });
        scene.setOnMouseDragged(event -> {
            double vx = toVirtualX(event.getSceneX(), root.getWidth(), root.getHeight());
            double vy = toVirtualY(event.getSceneY(), root.getWidth(), root.getHeight());
            gameController.getInputManager().onMouseMoved(vx, vy);
        });
        scene.setOnMousePressed(event -> {
            double vx = toVirtualX(event.getSceneX(), root.getWidth(), root.getHeight());
            double vy = toVirtualY(event.getSceneY(), root.getWidth(), root.getHeight());
            gameController.getInputManager().onMousePressed(vx, vy);
        });
        scene.setOnMouseReleased(event -> {
            double vx = toVirtualX(event.getSceneX(), root.getWidth(), root.getHeight());
            double vy = toVirtualY(event.getSceneY(), root.getWidth(), root.getHeight());
            gameController.getInputManager().onMouseReleased(vx, vy);
        });
        scene.setOnScroll(event -> gameController.getInputManager().onMouseScroll(event.getDeltaY()));

        // 60 FPS game loop with aspect-preserving canvas scaling.
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanoTime == 0) {
                    lastNanoTime = now;
                    return;
                }

                // AnimationTimer is already paced by JavaFX. A second exact 16.67ms
                // threshold rejects normal pulses that arrive a few microseconds early
                // and can halve the visible frame rate on a 60Hz display.
                double dt = (now - lastNanoTime) / 1_000_000_000.0;
                lastNanoTime = now;
                if (gameController.getCurrentState() != com.dodgingcat.core.GameState.BOOT) dt = Math.min(dt, 0.05);

                long updateStart = System.nanoTime();
                gameController.update(dt);
                updateTimeAccumNanos += System.nanoTime() - updateStart;

                RenderSurface.resize(canvas, root.getWidth(), root.getHeight(), renderHeightLimit);
                double cw = canvas.getWidth();
                double ch = canvas.getHeight();
                if (cw > 0 && ch > 0) {
                    // Keep one uniform scale and adapt the logical playfield width to the
                    // actual window aspect ratio. This fills fullscreen without stretching.
                    double scale = ch / WINDOW_HEIGHT;
                    double logicalWidth = cw / scale;
                    gameController.resizeViewport(logicalWidth, WINDOW_HEIGHT);
                    // Every renderer state paints an opaque full-screen background.
                    // Avoid an extra full-canvas fill each frame; only clear during
                    // camera shake, when the shifted background can expose the edges.
                    var activeWorld = gameController.getActiveWorld();
                    if (activeWorld != null && (Math.abs(activeWorld.getScreenShake().getOffsetX()) > 0.01
                            || Math.abs(activeWorld.getScreenShake().getOffsetY()) > 0.01)) {
                        gc.setFill(Color.rgb(10, 10, 15));
                        gc.fillRect(0, 0, cw, ch);
                    }

                    gc.save();
                    gc.scale(scale, scale);

                    long drawStart = System.nanoTime();
                    gameController.render(gc);
                    drawTimeAccumNanos += System.nanoTime() - drawStart;

                    gc.restore();
                }

                // Measure pulse cadence and average game-thread update/draw-call time.
                if (fpsWindowStartNanoTime == 0) fpsWindowStartNanoTime = now;
                fpsFrameCount++;
                long fpsWindowNanos = now - fpsWindowStartNanoTime;
                if (fpsWindowNanos >= 1_000_000_000L) {
                    double measuredFps = fpsFrameCount * 1_000_000_000.0 / fpsWindowNanos;
                    double averageUpdateMs = updateTimeAccumNanos / (double) fpsFrameCount / 1_000_000.0;
                    double averageDrawMs = drawTimeAccumNanos / (double) fpsFrameCount / 1_000_000.0;
                    gameController.setFrameMetrics(measuredFps, averageUpdateMs, averageDrawMs);
                    fpsFrameCount = 0;
                    updateTimeAccumNanos = 0;
                    drawTimeAccumNanos = 0;
                    fpsWindowStartNanoTime = now;
                }
            }
        };

        primaryStage.setTitle("Dodging Cat");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
        primaryStage.setFullScreen(true);

        timer.start();
    }

    private static double toVirtualX(double screenX, double canvasW, double canvasH) {
        if (canvasW <= 0 || canvasH <= 0) return 0;
        return screenX / (canvasH / WINDOW_HEIGHT);
    }

    private static double toVirtualY(double screenY, double canvasW, double canvasH) {
        if (canvasW <= 0 || canvasH <= 0) return 0;
        double scale = canvasH / WINDOW_HEIGHT;
        return screenY / scale;
    }

    public static void toggleFullScreen() {
        if (mainStage != null) {
            mainStage.setFullScreen(!mainStage.isFullScreen());
        }
    }

    public static boolean isFullScreen() {
        return mainStage != null && mainStage.isFullScreen();
    }

    @Override
    public void stop() {
        if (gameController != null) gameController.onApplicationExit();
        AudioManager.getInstance().shutdown();
    }

    public static void main(String[] args) {
        RenderSurface.configureRuntime();
        // Print the selected JavaFX graphics pipeline at startup so fullscreen
        // performance can be traced to the actual renderer instead of guessed.
        System.setProperty("prism.verbose", "true");
        System.out.printf("Dodging Cat frame pacing: prism.vsync=%s, javafx.animation.fullspeed=%s%n",
                System.getProperty("prism.vsync", "true (default)"),
                System.getProperty("javafx.animation.fullspeed", "false (default)"));
        launch(args);
    }
}
