package com.dodgingcat.ui;

import com.dodgingcat.shop.GachaSequence;

import javafx.scene.paint.Color;

/** Bounded, deterministic particle effects; no blur shaders or per-frame random allocations. */
final class GachaEffects {
    private static final Color GOLD = Color.web("#FFD36A");
    private static final Color CYBER = Color.web("#24DAFF");
    private static final Color STAR = Color.web("#E1FAFF");
    private static final Color RED = Color.web("#FF5353");
    private GachaEffects() {}

    static void renderRare(DrawContext gc, GachaSequence sequence, double cx, double cy) {
        double t = sequence.getPhaseElapsed();
        if (t > 1.5) return;
        gc.save();
        gc.setStroke(GOLD); gc.setFill(GOLD); gc.setLineWidth(2);
        for (int burst = 0; burst < 3; burst++) {
            double age = t - burst * 0.16;
            if (age < 0 || age > 1.15) continue;
            double bx = cx + (burst - 1) * 193, by = cy - 105 + (burst == 1 ? -65 : 0);
            gc.setGlobalAlpha(Math.max(0, 1 - age / 1.15));
            for (int i = 0; i < 24; i++) {
                double angle = i * Math.PI * 2 / 24 + burst * 0.3;
                double speed = 110 + (i % 4) * 16;
                double radius = age * speed;
                double px = bx + Math.cos(angle) * radius, py = by + Math.sin(angle) * radius + age * age * 45;
                gc.strokeLine(px, py, px - Math.cos(angle) * 9, py - Math.sin(angle) * 9);
                gc.fillOval(px - 1.5, py - 1.5, 3, 3);
            }
        }
        gc.restore();
    }

    static void renderLegendary(DrawContext gc, GachaSequence sequence, double cx, double cy) {
        double t = sequence.getPhaseElapsed();
        double collapse = Math.clamp(t / 1.12, 0, 1);
        double burst = Math.clamp((t - 1.12) / 0.68, 0, 1);
        String skin = sequence.getWinner().getId();
        gc.save();
        gc.setLineWidth(1.6);
        for (int i = 0; i < 96; i++) {
            double offset = (sequence.getEffectSeed() & 255) * 0.01;
            double angle = i * 2.399963 + offset + t * (3.2 + (i % 3) * 0.3);
            double radius = burst == 0 ? (70 + (i % 11) * 14) * (1 - collapse) + 8
                    : 8 + burst * (180 + (i % 9) * 31);
            double px = cx + Math.cos(angle) * radius;
            double py = cy + Math.sin(angle) * radius * 0.78;
            double size = 2.5 + (i % 4);
            Color color = skin.equals("cyber") ? CYBER : skin.equals("santa") ? (i % 2 == 0 ? RED : Color.WHITE) : STAR;
            gc.setFill(color); gc.setStroke(color);
            gc.setGlobalAlpha((0.55 + 0.4 * Math.sin(t * 12 + i) * Math.sin(t * 12 + i)) * (1 - burst));
            if (skin.equals("cyber")) {
                gc.strokeLine(px - size * 2, py + size, px, py + size);
                gc.strokeLine(px, py + size, px, py - size);
                gc.fillRect(px - 1.5, py - size - 1.5, 3, 3);
            } else if (skin.equals("santa")) {
                gc.fillRect(px - size, py - size, size * 2, size * 2);
                gc.setStroke(i % 2 == 0 ? Color.WHITE : RED);
                gc.strokeLine(px, py - size, px, py + size);
                gc.strokeLine(px - size, py, px + size, py);
            } else {
                gc.strokeLine(px - size, py, px + size, py);
                gc.strokeLine(px, py - size, px, py + size);
                gc.fillOval(px - 1, py - 1, 2, 2);
            }
        }
        if (burst > 0) {
            gc.setGlobalAlpha(1 - burst);
            gc.setStroke(skin.equals("cyber") ? CYBER : skin.equals("santa") ? RED : STAR);
            double r = 35 + burst * 260;
            gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
        }
        gc.restore();
    }
}
