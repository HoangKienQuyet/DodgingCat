package com.dodgingcat.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** One immutable prize/reel and its presentation phases. Audio completion controls landing. */
public final class GachaSequence {
    public enum Phase { SPINNING, LANDED, LEGENDARY_REVEAL, RESULT, DONE }
    public static final double LANDING_SECONDS = 0.55;
    public static final double LEGENDARY_SECONDS = 1.8;
    public static final double RESULT_SECONDS = 2.0;
    private static final int START_INDEX = 3;
    private final ShopItem winner;
    private final List<ShopItem> reel;
    private final int winningIndex;
    private final long effectSeed;
    private Phase phase = Phase.SPINNING;
    private double phaseElapsed;
    private double progress;

    public GachaSequence(List<ShopItem> catalog, ShopItem winner, Random random) {
        if (catalog.isEmpty() || winner == null) throw new IllegalArgumentException("A gacha needs a prize and catalog");
        this.winner = winner;
        winningIndex = 42 + random.nextInt(12);
        effectSeed = random.nextLong();
        List<ShopItem> strip = new ArrayList<>();
        for (int i = 0; i < winningIndex + 5; i++) strip.add(catalog.get(random.nextInt(catalog.size())));
        strip.set(winningIndex, winner);
        reel = List.copyOf(strip);
    }

    public void update(double dt, double audioProgress, boolean audioFinished, boolean dismiss) {
        if (phase == Phase.DONE) return;
        if (phase == Phase.SPINNING) {
            progress = Math.max(progress, Math.clamp(audioProgress, 0.0, 1.0));
            if (audioFinished) {
                progress = 1.0;
                enter(Phase.LANDED);
            }
            return;
        }
        phaseElapsed += Math.max(0, dt);
        switch (phase) {
            case LANDED -> {
                if (phaseElapsed >= LANDING_SECONDS) {
                    enter(winner.getRarity() == SkinRarity.LEGENDARY ? Phase.LEGENDARY_REVEAL : Phase.RESULT);
                }
            }
            case LEGENDARY_REVEAL -> {
                if (phaseElapsed >= LEGENDARY_SECONDS) enter(Phase.RESULT);
            }
            case RESULT -> {
                if (dismiss || phaseElapsed >= RESULT_SECONDS) enter(Phase.DONE);
            }
            default -> { }
        }
    }

    private void enter(Phase next) { phase = next; phaseElapsed = 0; }
    public Phase getPhase() { return phase; }
    public double getPhaseElapsed() { return phaseElapsed; }
    public ShopItem getWinner() { return winner; }
    public List<ShopItem> getReel() { return reel; }
    public int getWinningIndex() { return winningIndex; }
    public long getEffectSeed() { return effectSeed; }
    public boolean isActive() { return phase != Phase.DONE; }
    public double getReelPosition() {
        // Continuous translation: the prize is a real cell in the strip, never swapped at the end.
        double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
        return START_INDEX + (winningIndex - START_INDEX) * eased;
    }
}
