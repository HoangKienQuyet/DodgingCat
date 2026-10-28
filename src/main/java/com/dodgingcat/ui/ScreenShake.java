package com.dodgingcat.ui;

/**
 * Procedural camera screen-shake effect generator.
 */
public class ScreenShake {
    private double intensity = 0.0;
    private double duration = 0.0;
    private double currentTimer = 0.0;
    private double elapsed = 0.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;

    public void trigger(double intensity, double duration) {
        this.intensity = Math.max(this.intensity, intensity);
        this.duration = Math.max(this.duration, duration);
        this.currentTimer = this.duration;
        this.elapsed = 0.0;
    }

    public void update(double dt) {
        if (currentTimer > 0) {
            double step = Math.max(0.0, Math.min(dt, 0.05));
            currentTimer = Math.max(0.0, currentTimer - step);
            elapsed += step;
            double progress = duration <= 0.0 ? 0.0 : currentTimer / duration;
            // Smooth envelope and repeatable oscillation avoid random frame-to-frame jumps,
            // which made the bomb pickup feel like a brief frame-rate hitch.
            double envelope = Math.sin(Math.PI * Math.max(0.0, Math.min(1.0, progress)));
            double currentIntensity = intensity * envelope;
            offsetX = Math.sin(elapsed * Math.PI * 2.0 * 15.0) * currentIntensity;
            offsetY = Math.sin(elapsed * Math.PI * 2.0 * 12.0 + 0.8) * currentIntensity * 0.58;
            if (currentTimer <= 0) {
                offsetX = 0.0;
                offsetY = 0.0;
                intensity = 0.0;
                duration = 0.0;
                elapsed = 0.0;
            }
        }
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }
}
