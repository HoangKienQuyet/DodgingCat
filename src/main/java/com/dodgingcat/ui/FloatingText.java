package com.dodgingcat.ui;

import javafx.scene.paint.Color;

/**
 * Floating combat and bonus notification text.
 */
public class FloatingText {
    private final String text;
    private double x;
    private double y;
    private final Color color;
    private double lifetime;
    private final double initialLifetime;
    private final double vy;

    public FloatingText(String text, double x, double y, Color color, double duration) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.lifetime = duration;
        this.initialLifetime = duration;
        this.vy = -35.0; // Float upwards
    }

    public void update(double dt) {
        y += vy * dt;
        lifetime -= dt;
    }

    public boolean isExpired() {
        return lifetime <= 0.0;
    }

    public String getText() {
        return text;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Color getColor() {
        double alpha = Math.clamp(lifetime / initialLifetime, 0.0, 1.0);
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
