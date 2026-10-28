package com.dodgingcat.ui;


import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lightweight 2D particle simulation for visual effects.
 */
public class ParticleSystem {

    private static class Particle {
        double x, y;
        double vx, vy;
        double radius;
        Color color;
        double life;
        double maxLife;

        Particle(double x, double y, double vx, double vy, double radius, Color color, double maxLife) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
            this.color = color;
            this.life = maxLife;
            this.maxLife = maxLife;
        }

        void update(double dt) {
            x += vx * dt;
            y += vy * dt;
            life -= dt;
        }

        void render(DrawContext gc) {
            double alpha = Math.clamp(life / maxLife, 0.0, 1.0);
            gc.setGlobalAlpha(alpha);
            gc.setFill(color);
            gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        }
    }

    private final List<Particle> particles = new ArrayList<>();

    public void update(double dt) {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.update(dt);
            if (p.life <= 0) {
                iterator.remove();
            }
        }
    }

    public void render(DrawContext gc) {
        gc.save();
        for (Particle p : particles) {
            p.render(gc);
        }
        gc.restore();
    }


    public void spawnBurst(double x, double y, Color color, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double spd = (0.3 + Math.random() * 0.7) * speed;
            double vx = Math.cos(angle) * spd;
            double vy = Math.sin(angle) * spd;
            double radius = 2.0 + Math.random() * 3.5;
            double life = 0.4 + Math.random() * 0.5;
            particles.add(new Particle(x, y, vx, vy, radius, color, life));
        }
    }

    public void spawnShockwave(double centerX, double centerY, Color color) {
        spawnBurst(centerX, centerY, color, 40, 280.0);
    }

    public void clear() {
        particles.clear();
    }
}
