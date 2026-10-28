package com.dodgingcat.app;

import com.dodgingcat.audio.AudioManager;
import com.dodgingcat.core.GameController;
import com.dodgingcat.persistence.PersistenceManager;
import com.dodgingcat.ui.DrawContext;
import com.dodgingcat.ui.AssetManager;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;

/** Native-resolution core-Java rendering and windowing; model/assets are shared with the FX backend. */
public final class Java2DApp {
    static {
        if (System.getProperty("os.name", "").startsWith("Windows"))
            System.setProperty("sun.java2d.d3d", System.getProperty("sun.java2d.d3d", "true"));
    }
    private final ConcurrentLinkedQueue<Runnable> events = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Canvas surface = new Canvas();
    private volatile Frame window;
    private volatile GameController controller;
    private final PersistenceManager profile;
    private boolean fullscreen, fullscreenHeld;
    private volatile boolean changingWindow;
    private Dimension windowedSize;
    private double lastPaintMs, lastPresentMs;
    private int warmupFrames;
    private long bootStartedAt, bootFinishedAt;

    public Java2DApp() { this(null); }
    public Java2DApp(PersistenceManager profile) { this.profile = profile; }

    public static void main(String[] args) throws Exception { new Java2DApp().run(); }

    public void run() throws Exception {
        // JavaFX remains the existing MP3 decoder and image loader, not the rendering/window backend.
        RenderSurface.configureRuntime();
        CountDownLatch mediaReady = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); mediaReady.countDown(); });
        if (!mediaReady.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Media toolkit startup timed out");
        var clock = new FramePacer(60);
        long previous = 0, metricsStart = 0, updateNanos = 0, drawNanos = 0;
        int frames = 0;
        long paintNanos = 0, presentNanos = 0;
        double preparedScale = 0;
        try {
            controller = new GameController(960, 640, profile == null ? new PersistenceManager() : profile);
            controller.setQuitAction(() -> running.set(false));
            SwingUtilities.invokeAndWait(this::createWindow);
            while (running.get()) {
                long now = clock.awaitFrame();
                for (Runnable event; (event = events.poll()) != null;) event.run();
                if (!running.get()) break;
                int logicalW = surface.getWidth(), logicalH = surface.getHeight();
                if (logicalW <= 0 || logicalH <= 0 || !surface.isDisplayable() || (window.getExtendedState() & Frame.ICONIFIED) != 0) {
                    previous = 0; clock.reset(); java.util.concurrent.locks.LockSupport.parkNanos(50_000_000); continue;
                }
                var device = surface.getGraphicsConfiguration().getDefaultTransform();
                int pixelW = (int) Math.round(logicalW * device.getScaleX());
                int pixelH = (int) Math.round(logicalH * device.getScaleY());
                double scale = pixelH / 640.0;
                if (scale != preparedScale) {
                    AssetManager.prepareJava2D(scale);
                    preparedScale = scale;
                }
                controller.resizeViewport(pixelW / scale, 640);
                double elapsed = previous == 0 ? 0 : (now - previous) / 1e9; previous = now;
                // Splash durations follow real time, independently of simulation's hitch clamp.
                double dt = controller.getCurrentState() == com.dodgingcat.core.GameState.BOOT ? elapsed : Math.min(.05, elapsed);
                long start = System.nanoTime(); controller.update(dt); updateNanos += System.nanoTime() - start;
                if (bootStartedAt != 0 && bootFinishedAt == 0 && controller.getCurrentState() != com.dodgingcat.core.GameState.BOOT)
                    bootFinishedAt = now;
                var buffers = surface.getBufferStrategy();
                if (buffers == null) continue;
                start = System.nanoTime();
                int presentations = 0;
                do {
                    int restores = 0;
                    do {
                        var graphics = (Graphics2D) buffers.getDrawGraphics();
                        long paintStart = System.nanoTime();
                        try {
                            graphics.setTransform(new AffineTransform());
                            var world = controller.getActiveWorld();
                            if (world != null && (Math.abs(world.getScreenShake().getOffsetX()) > .01 || Math.abs(world.getScreenShake().getOffsetY()) > .01)) {
                                graphics.setColor(new Color(10, 10, 15)); graphics.fillRect(0, 0, pixelW, pixelH);
                            }
                            graphics.scale(scale, scale);
                            var drawing = new DrawContext(graphics);
                            if (controller.getCurrentState() == com.dodgingcat.core.GameState.BOOT
                                    && bootStartedAt != 0 && warmupFrames < 120)
                                controller.warmupRenderer(drawing, warmupFrames++);
                            // The boot layer covers warmup draws before presentation.
                            controller.render(drawing);
                        } finally { graphics.dispose(); }
                        paintNanos += System.nanoTime() - paintStart;
                    } while (buffers.contentsRestored() && ++restores < 3 && running.get());
                    long presentStart = System.nanoTime();
                    buffers.show(); Toolkit.getDefaultToolkit().sync();
                    presentNanos += System.nanoTime() - presentStart;
                } while (buffers.contentsLost() && ++presentations < 3 && running.get());
                if (bootStartedAt == 0) {
                    bootStartedAt = System.nanoTime();
                    previous = bootStartedAt; // Count the four seconds from the first visible title frame.
                }
                drawNanos += System.nanoTime() - start;
                if (metricsStart == 0) { metricsStart = now; updateNanos = 0; drawNanos = 0; paintNanos = 0; presentNanos = 0; }
                else frames++;
                if (now - metricsStart >= 1_000_000_000L) {
                    controller.setFrameMetrics(frames * 1e9 / (now - metricsStart), updateNanos / (double) frames / 1e6, drawNanos / (double) frames / 1e6);
                    lastPaintMs = paintNanos / (double) frames / 1e6; lastPresentMs = presentNanos / (double) frames / 1e6;
                    paintNanos = 0; presentNanos = 0;
                    frames = 0; metricsStart = now; updateNanos = 0; drawNanos = 0;
                }
            }
        } finally {
            if (controller != null) controller.onApplicationExit();
            AudioManager.getInstance().shutdown();
            SwingUtilities.invokeAndWait(() -> {
                if (window == null) return;
                window.getGraphicsConfiguration().getDevice().setFullScreenWindow(null);
                window.dispose();
            });
            Platform.runLater(Platform::exit);
        }
    }

    private void createWindow() {
        window = new Frame("Dodging Cat"); window.setIgnoreRepaint(true);
        surface.setIgnoreRepaint(true); surface.setFocusable(true); surface.setFocusTraversalKeysEnabled(false);
        var dpi = window.getGraphicsConfiguration().getDefaultTransform();
        windowedSize = new Dimension((int) Math.round(960 / dpi.getScaleX()), (int) Math.round(640 / dpi.getScaleY()));
        surface.setPreferredSize(windowedSize); window.add(surface);
        window.addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent event) { running.set(false); } });
        window.addWindowFocusListener(new WindowAdapter() {
            @Override public void windowLostFocus(WindowEvent event) {
                if (!changingWindow && !window.isFocused()) { fullscreenHeld = false; events.add(controller::onWindowFocusLost); }
            }
        });
        surface.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_F11) {
                    if (!fullscreenHeld) { fullscreenHeld = true; events.add(Java2DApp.this::toggleFullscreen); }
                    return;
                }
                KeyCode code = key(event.getKeyCode()); if (code != null) events.add(() -> controller.getInputManager().onKeyPressed(code));
            }
            @Override public void keyReleased(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_F11) { fullscreenHeld = false; return; }
                KeyCode code = key(event.getKeyCode()); if (code != null) events.add(() -> controller.getInputManager().onKeyReleased(code));
            }
        });
        surface.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { pointer(e, 0); }
            @Override public void mouseDragged(MouseEvent e) { pointer(e, 0); }
        });
        surface.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (e.getButton() == MouseEvent.BUTTON1) { surface.requestFocusInWindow(); pointer(e, 1); } }
            @Override public void mouseReleased(MouseEvent e) { if (e.getButton() == MouseEvent.BUTTON1) pointer(e, 2); }
        });
        surface.addMouseWheelListener(e -> { double delta = -e.getPreciseWheelRotation() * Math.max(1,e.getScrollAmount()) * 40; events.add(() -> controller.getInputManager().onMouseScroll(delta)); });
        setWindowMode(true);
    }

    private void pointer(MouseEvent event, int action) {
        var dpi = surface.getGraphicsConfiguration().getDefaultTransform();
        double scale = Math.max(1, surface.getHeight()) * dpi.getScaleY() / 640.0;
        double x = event.getX() * dpi.getScaleX() / scale, y = event.getY() * dpi.getScaleY() / scale;
        events.add(() -> {
            if (action == 1) controller.getInputManager().onMousePressed(x, y);
            else if (action == 2) controller.getInputManager().onMouseReleased(x, y);
            else controller.getInputManager().onMouseMoved(x, y);
        });
    }

    private void toggleFullscreen() {
        try { SwingUtilities.invokeAndWait(() -> setWindowMode(!fullscreen)); }
        catch (Exception e) { throw new IllegalStateException("Window mode switch failed", e); }
    }
    private void setWindowMode(boolean target) {
        changingWindow = true;
        try {
            var device = window.getGraphicsConfiguration().getDevice();
            if (window.isDisplayable()) {
                if (!fullscreen) windowedSize = surface.getSize();
                device.setFullScreenWindow(null); window.dispose();
            }
            fullscreen = target; window.setUndecorated(target);
            if (target) {
                if (device.isFullScreenSupported()) device.setFullScreenWindow(window);
                else { window.setBounds(device.getDefaultConfiguration().getBounds()); window.setVisible(true); }
            } else {
                surface.setPreferredSize(windowedSize); window.pack(); window.setLocationRelativeTo(null); window.setVisible(true);
            }
            surface.createBufferStrategy(2); surface.requestFocusInWindow();
        } finally { changingWindow = false; }
    }

    private static KeyCode key(int code) {
        return switch (code) {
            case KeyEvent.VK_UP -> KeyCode.UP; case KeyEvent.VK_DOWN -> KeyCode.DOWN;
            case KeyEvent.VK_LEFT -> KeyCode.LEFT; case KeyEvent.VK_RIGHT -> KeyCode.RIGHT;
            case KeyEvent.VK_ENTER -> KeyCode.ENTER; case KeyEvent.VK_ESCAPE -> KeyCode.ESCAPE;
            case KeyEvent.VK_SPACE -> KeyCode.SPACE; case KeyEvent.VK_TAB -> KeyCode.TAB;
            case KeyEvent.VK_F9 -> KeyCode.F9; case KeyEvent.VK_F10 -> KeyCode.F10;
            default -> code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z ? KeyCode.valueOf(Character.toString((char) code)) : null;
        };
    }
}
