package com.dodgingcat.app;

import javafx.scene.canvas.Canvas;

/** Native-resolution rendering; an explicit height limit is available only for diagnostics. */
public final class RenderSurface {
    public static final int DEFAULT_HEIGHT_LIMIT = 0;
    private RenderSurface() {}

    public static void resize(Canvas canvas, double viewportWidth, double viewportHeight, int heightLimit) {
        if (viewportWidth <= 0 || viewportHeight <= 0) return;
        double bufferHeight = heightLimit > 0 ? Math.min(viewportHeight, heightLimit) : viewportHeight;
        double bufferWidth = bufferHeight * viewportWidth / viewportHeight;
        // Resizing clears the backing texture, so only resize when the viewport changes.
        if (Math.abs(canvas.getWidth() - bufferWidth) > .01) canvas.setWidth(bufferWidth);
        if (Math.abs(canvas.getHeight() - bufferHeight) > .01) canvas.setHeight(bufferHeight);
        canvas.setScaleX(viewportWidth / bufferWidth);
        canvas.setScaleY(viewportHeight / bufferHeight);
    }

    /** Must run before JavaFX toolkit initialization. Explicit JVM settings retain precedence. */
    public static void configureRuntime() {
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            // Canvas rounds a 125% desktop DPI up to a 2x backing texture. The game
            // already scales its logical 640px layout itself; avoid doing both.
            System.setProperty("glass.win.uiScale", System.getProperty("glass.win.uiScale", "1.0"));
            // AnimationTimer remains paced; let the Windows compositor present frames.
            System.setProperty("prism.vsync", System.getProperty("prism.vsync", "false"));
        }
    }
}
