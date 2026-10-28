package com.dodgingcat.game;

import com.dodgingcat.buffs.Buff;
import com.dodgingcat.buffs.BuffType;
import com.dodgingcat.audio.AudioManager;
import com.dodgingcat.core.GameMode;
import com.dodgingcat.generation.ProceduralGenerator;
import com.dodgingcat.hazards.Hazard;
import com.dodgingcat.hazards.CannonHazard;
import com.dodgingcat.hazards.LaserHazard;
import com.dodgingcat.hazards.OrbitalHazard;
import com.dodgingcat.ui.FloatingText;
import com.dodgingcat.ui.ParticleSystem;
import com.dodgingcat.ui.ScreenShake;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Simulation model with randomized platform layouts, fish currency, and decoupled physics.
 */
public class GameWorld {
    private final GameMode mode;
    private final String mapId;
    private double width;
    private double height;
    private final ProceduralGenerator generator;
    private final Random random = new Random();

    private final List<Player> players = new ArrayList<>();
    private final List<Platform> platforms = new ArrayList<>();
    private final List<Hazard> hazards = new ArrayList<>();
    private final Set<Hazard> warnedHazards = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Buff> buffs = new ArrayList<>();
    private final List<FishItem> fishes = new ArrayList<>();

    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final ParticleSystem particleSystem = new ParticleSystem();
    private final ScreenShake screenShake = new ScreenShake();

    private double survivalTime = 0.0;
    private int score = 0;
    private double scoreAccumulator = 0.0;
    private int fishesCollected = 0;
    private boolean gameOver = false;

    public GameWorld(GameMode mode, double width, double height, String p1Skin, String p2Skin) {
        this(mode, "kitchen", width, height, p1Skin, p2Skin, new ProceduralGenerator());
    }

    public GameWorld(GameMode mode, double width, double height, String p1Skin, String p2Skin, ProceduralGenerator generator) {
        this(mode, "kitchen", width, height, p1Skin, p2Skin, generator);
    }

    public GameWorld(GameMode mode, String mapId, double width, double height, String p1Skin, String p2Skin) {
        this(mode, mapId, width, height, p1Skin, p2Skin, new ProceduralGenerator());
    }

    public GameWorld(GameMode mode, String mapId, double width, double height, String p1Skin, String p2Skin, ProceduralGenerator generator) {
        this.mode = mode;
        this.mapId = mapId != null ? mapId : "kitchen";
        this.width = width;
        this.height = height;
        this.generator = generator;

        initializePlayers(p1Skin, p2Skin);
        generateDynamicPlatforms();
    }

    private void initializePlayers(String p1Skin, String p2Skin) {
        players.clear();
        double groundY = getFloorY() - 36.0;
        if (mode == GameMode.SINGLE_PLAYER) {
            players.add(new Player(1, width / 2.0 - 18.0, groundY, p1Skin));
        } else {
            players.add(new Player(1, width / 3.0 - 18.0, groundY, p1Skin));
            players.add(new Player(2, (width * 2.0) / 3.0 - 18.0, groundY, p2Skin));
        }
    }

    /**
     * Procedurally generates dynamic, randomized platform distributions per run,
     * maintaining guaranteed jump reachability and fair escape corridors.
     * Enforces:
     * - Randomized initial elevated platform count (4 to 7), arranged across 3-4 tiers.
     * - Lowest platform is within the player's jump range from the safe floor.
     * - Layouts use 3-4 tiers with 55-95px vertical gaps, below the estimated jump height.
     * - Highest platform maintains a safe clearance from the ceiling (y >= 125px).
     * - Horizontal distribution across Left, Center, and Right zones without one-sided clustering.
     * - Graph reachability validation ensuring every platform is reachable via a chain of jumps.
     */
    private void generateDynamicPlatforms() {
        double floorY = getFloorY();
        double floorHeight = height - floorY;
        Platform floor = new Platform(0, floorY, width, floorHeight, PlatformType.SOLID_FLOOR);

        for (int attempt = 0; attempt < 250; attempt++) {
            int targetCount = 4 + random.nextInt(4);
            int tierCount = 3 + random.nextInt(2);
            int[] countByTier = new int[tierCount];
            java.util.Arrays.fill(countByTier, 1);
            for (int remaining = targetCount - tierCount; remaining > 0; remaining--) {
                countByTier[random.nextInt(tierCount)]++;
            }

            double[] tierY = new double[tierCount];
            tierY[0] = floorY - (65.0 + random.nextDouble() * 30.0);
            for (int tier = 1; tier < tierCount; tier++) {
                tierY[tier] = tierY[tier - 1] - (65.0 + random.nextDouble() * 30.0);
            }
            if (tierY[tierCount - 1] < 125.0) continue;

            List<Platform> candidates = new ArrayList<>();
            List<List<Platform>> tiers = new ArrayList<>();
            boolean valid = true;
            for (int tier = 0; tier < tierCount && valid; tier++) {
                List<Platform> thisTier = new ArrayList<>();
                for (int i = 0; i < countByTier[tier]; i++) {
                    double platformWidth = 135.0 + random.nextDouble() * 95.0;
                    double x;
                    if (tier == 0) {
                        x = 35.0 + random.nextDouble() * Math.max(1.0, width - platformWidth - 70.0);
                    } else {
                        List<Platform> lowerTier = tiers.get(tier - 1);
                        Platform parent = lowerTier.get(random.nextInt(lowerTier.size()));
                        boolean placeLeft = random.nextBoolean();
                        double horizontalGap = random.nextDouble() * 82.0;
                        x = placeLeft
                                ? parent.getX() - platformWidth - horizontalGap
                                : parent.getX() + parent.getWidth() + horizontalGap;
                        x = Math.clamp(x, 35.0, Math.max(35.0, width - platformWidth - 35.0));
                        Platform linkProbe = new Platform(x, tierY[tier], platformWidth, 16.0, PlatformType.ONE_WAY);
                        if (!canJumpTo(parent, linkProbe)) {
                            valid = false;
                            break;
                        }
                    }

                    Platform platform = new Platform(x, tierY[tier], platformWidth, 16.0,
                            PlatformType.CRUMBLING);
                    candidates.add(platform);
                    thisTier.add(platform);
                }
                tiers.add(thisTier);
            }
            if (!valid || candidates.size() != targetCount || !coversPlayArea(candidates)) continue;

            platforms.clear();
            platforms.add(floor);
            platforms.addAll(candidates);
            return;
        }

        generateSafeRandomFallback(floor);
    }

    private double getFloorY() {
        // Every map uses the Kitchen baseline so hazards, platforms, and player feet
        // share one consistent gameplay floor across themes.
        return height * 0.8555;
    }

    public void resize(double newWidth, double newHeight) {
        if (newWidth <= 0 || newHeight <= 0
                || Math.abs(newWidth - width) < 0.5 && Math.abs(newHeight - height) < 0.5) return;
        double sx = newWidth / width;
        double sy = newHeight / height;
        for (Player player : players) player.setPosition(player.getX() * sx, player.getY() * sy);
        for (Platform platform : platforms) {
            platform.setPosition(platform.getX() * sx, platform.getY() * sy);
            platform.setDimensions(platform.getWidth(), platform.getHeight() * sy);
        }
        for (Hazard hazard : hazards) hazard.scaleHorizontally(sx, newWidth);
        for (Buff buff : buffs) buff.scaleX(sx);
        for (FishItem fish : fishes) fish.scaleX(sx);
        width = newWidth;
        height = newHeight;
    }

    private boolean canJumpTo(Platform lower, Platform upper) {
        double rise = lower.getY() - upper.getY();
        if (rise < 0.0 || rise > 96.0) return false;
        double horizontalGap = Math.max(0.0,
                Math.max(lower.getX(), upper.getX())
                        - Math.min(lower.getX() + lower.getWidth(), upper.getX() + upper.getWidth()));
        return horizontalGap <= 82.0;
    }

    private boolean coversPlayArea(List<Platform> candidates) {
        boolean left = false, center = false, right = false;
        int maxInOneZone = 0;
        int[] zoneCounts = new int[3];
        for (Platform p : candidates) {
            double centerX = p.getX() + p.getWidth() / 2.0;
            int zone = centerX < width / 3.0 ? 0 : centerX < width * 2.0 / 3.0 ? 1 : 2;
            zoneCounts[zone]++;
            left |= zone == 0;
            center |= zone == 1;
            right |= zone == 2;
        }
        for (int count : zoneCounts) maxInOneZone = Math.max(maxInOneZone, count);
        int requiredZones = candidates.size() >= 4 ? 3 : 2;
        int coveredZones = (left ? 1 : 0) + (center ? 1 : 0) + (right ? 1 : 0);
        return coveredZones >= requiredZones && maxInOneZone <= Math.ceil(candidates.size() * 0.60);
    }

    private void generateSafeRandomFallback(Platform floor) {
        platforms.clear();
        platforms.add(floor);
        int targetCount = 4 + random.nextInt(4);
        double floorY = floor.getY();
        double firstY = floorY - (65.0 + random.nextDouble() * 24.0);
        double secondY = firstY - (58.0 + random.nextDouble() * 24.0);
        double thirdY = secondY - (58.0 + random.nextDouble() * 24.0);
        double[] laneCenters = {width * 0.18, width * 0.50, width * 0.82, width * 0.50};
        double[] ys = {firstY, secondY, thirdY, Math.max(125.0, thirdY - 60.0)};
        List<Platform> base = new ArrayList<>();
        for (int i = 0; i < Math.min(4, targetCount); i++) {
            double w = 190.0 + random.nextDouble() * 30.0;
            double x = Math.clamp(laneCenters[i] - w / 2.0, 35.0, width - w - 35.0);
            Platform p = new Platform(x, ys[i], w, 16.0, PlatformType.CRUMBLING);
            base.add(p);
            platforms.add(p);
        }
        for (int i = 4; i < targetCount; i++) {
            Platform parent = base.get((i - 4) % base.size());
            double w = 145.0 + random.nextDouble() * 70.0;
            double x = Math.clamp(parent.getX() + parent.getWidth() / 2.0 - w / 2.0
                    + (random.nextDouble() * 80.0 - 40.0), 35.0, width - w - 35.0);
            Platform extra = new Platform(x, parent.getY(), w, 16.0, PlatformType.CRUMBLING);
            platforms.add(extra);
            base.add(extra);
        }
    }

    public void update(double dt) {
        if (gameOver) return;

        survivalTime += dt;

        // Score accumulation (10 pts per sec, 20 if double score buff active)
        boolean hasDouble = players.stream().anyMatch(p -> p.isAlive() && p.hasDoubleScore());
        double scoreRate = hasDouble ? 20.0 : 10.0;
        scoreAccumulator += scoreRate * dt;
        if (scoreAccumulator >= 1.0) {
            int toAdd = (int) scoreAccumulator;
            score += toAdd;
            scoreAccumulator -= toAdd;
        }

        // 1. Update platforms
        for (Platform p : platforms) {
            p.update(dt);
        }

        // 2. Update players physics
        for (Player p : players) {
            p.updatePhysics(dt, width, platforms);
        }
        // 3. Update procedural generator
        generator.update(dt, survivalTime, mapId, width, height, players, hazards, platforms, buffs, fishes, mode == GameMode.LOCAL_COOP);

        // 4. Update hazards & collision checks
        boolean deathSoundPlayedThisFrame = false;
        Iterator<Hazard> hazardIter = hazards.iterator();
        while (hazardIter.hasNext()) {
            Hazard h = hazardIter.next();
            boolean wasWarning = h instanceof LaserHazard laser && laser.isWarning()
                    || h instanceof CannonHazard cannon && cannon.isWarning();
            if (wasWarning && warnedHazards.add(h)) {
                AudioManager.getInstance().playSfx("hazard_warning.wav");
            }
            h.update(dt, width, height);

            if (wasWarning && h instanceof LaserHazard laser && laser.isFiring()) {
                AudioManager.getInstance().playSfx("laser_shot.wav");
            } else if (wasWarning && h instanceof CannonHazard cannon && cannon.isFiring()) {
                AudioManager.getInstance().playSfx("cannon_shot.wav");
            }
            if (h instanceof OrbitalHazard orbital && orbital.consumeSwingSoundEvent()) {
                AudioManager.getInstance().playSfx("pendulum_swing.wav");
            }

            if (!h.isActive()) {
                hazardIter.remove();
                warnedHazards.remove(h);
                continue;
            }

            for (Player player : players) {
                // Invincible players are translucent and hazards pass through without being consumed.
                if (player.isAlive() && !player.isInvincible() && h.collidesWith(player)) {
                    boolean hadShield = player.hasShield();
                    boolean fatal = player.applyHit();
                    if (fatal) {
                        if (!deathSoundPlayedThisFrame) {
                            AudioManager.getInstance().playSfx("player_death.wav");
                            deathSoundPlayedThisFrame = true;
                        }
                        // The universal tombstone and ghost communicate the defeat without a red marker.
                        screenShake.trigger(12.0, 0.45);
                    } else {
                        if (hadShield) {
                            AudioManager.getInstance().playSfx("shield_break.wav");
                        }
                        // Shield absorbed the hit - spawn shield glass shatter particles.
                        particleSystem.spawnBurst(player.getX() + 18, player.getY() + 18, Color.CYAN, 20, 160);
                        screenShake.trigger(4.0, 0.2);
                    }
                    h.deactivate();
                    break;
                }
            }
        }

        // 5. Update and collect Buffs
        generator.refreshBuffEffects(); // Includes shields broken by hazards this frame.
        Iterator<Buff> buffIter = buffs.iterator();
        while (buffIter.hasNext()) {
            Buff b = buffIter.next();
            b.update(dt);

            if (b.isExpired()) {
                generator.onBuffFinished(b.getType());
                buffIter.remove();
                continue;
            }

            for (Player p : players) {
                if (p.isAlive() && b.getHitbox().intersects(b.getX(), b.getY(), p.getHitbox(), p.getX(), p.getY())) {
                    b.collect();
                    applyBuffPickup(b.getType(), p);
                    buffIter.remove();
                    break;
                }
            }
        }

        // 6. Update and collect Fishes (Currency)
        Iterator<FishItem> fishIter = fishes.iterator();
        while (fishIter.hasNext()) {
            FishItem fi = fishIter.next();
            fi.update(dt);

            if (fi.isExpired()) {
                fishIter.remove();
                continue;
            }

            for (Player p : players) {
                if (p.isAlive() && fi.getHitbox().intersects(fi.getX(), fi.getY(), p.getHitbox(), p.getX(), p.getY())) {
                    fi.collect();
                    fishesCollected += fi.getValue();
                    if (fi.getValue() > 1) AudioManager.getInstance().playSfxTwice("fish_collect.wav", 0.20);
                    else AudioManager.getInstance().playSfx("fish_collect.wav");
                    floatingTexts.add(new FloatingText("+" + fi.getValue() + " 🐟", fi.getX(), fi.getY() - 10, Color.GOLD, 0.8));
                    particleSystem.spawnBurst(fi.getX() + 10, fi.getY() + 10, Color.GOLD, 10, 100);
                    break;
                }
            }
        }

        // 7. Update visual effects
        Iterator<FloatingText> textIter = floatingTexts.iterator();
        while (textIter.hasNext()) {
            FloatingText ft = textIter.next();
            ft.update(dt);
            if (ft.isExpired()) textIter.remove();
        }
        particleSystem.update(dt);
        screenShake.update(dt);

        AudioManager.getInstance().syncLoopingSfx("shield_activation.wav",
                players.stream().anyMatch(Player::hasShield));

        // 8. Game Over check: All players dead
        boolean anyAlive = players.stream().anyMatch(Player::isAlive);
        if (!anyAlive) {
            gameOver = true;
        }
    }

    private void applyBuffPickup(BuffType type, Player collector) {
        score += 50;
        floatingTexts.add(new FloatingText("+50", collector.getX(), collector.getY() - 25, Color.GOLD, .8));
        particleSystem.spawnBurst(collector.getX() + 18, collector.getY() + 18, Color.LIGHTGOLDENRODYELLOW, 22, 160);

        if (type == BuffType.BOMB) {
            AudioManager.getInstance().playSfx("bomb_explode.wav");
            // Instant screen clear with shockwave
            for (Hazard h : hazards) {
                if (h.isActive()) {
                    h.deactivate();
                    particleSystem.spawnBurst(h.getX() + h.getWidth() / 2.0, h.getY() + h.getHeight() / 2.0, Color.ORANGE, 14, 180);
                }
            }
            screenShake.trigger(5.5, 0.24);
            generator.onBuffFinished(type);
        } else if (type == BuffType.HEART) {
            if (mode == GameMode.LOCAL_COOP) {
                boolean revived = false;
                for (Player p : players) {
                    if (!p.isAlive()) {
                        p.revive(collector.getX(), collector.getY() - 20);
                        generator.onBuffEffectStarted(type, p, BuffType.INVINCIBILITY);
                        AudioManager.getInstance().playSfx("revive.wav");
                        floatingTexts.add(new FloatingText("REVIVED!", p.getX(), p.getY() - 25, Color.HOTPINK, 1.4));
                        particleSystem.spawnBurst(p.getX() + 18, p.getY() + 18, Color.HOTPINK, 30, 220);
                        revived = true;
                        break;
                    }
                }
                // Avoid consuming a Heart pickup with no effect while both teammates are alive.
                if (!revived) {
                    AudioManager.getInstance().playSfx("buff_collect.wav");
                    AudioManager.getInstance().startLoopingSfxAfter("shield_activation.wav", 0.18);
                    collector.applyBuff(BuffType.SHIELD);
                    generator.onBuffEffectStarted(type, collector, BuffType.SHIELD);
                }
            } else generator.onBuffFinished(type);
        } else {
            AudioManager.getInstance().playSfx("buff_collect.wav");
            if (type == BuffType.SHIELD) {
                AudioManager.getInstance().startLoopingSfxAfter("shield_activation.wav", 0.18);
            }
            collector.applyBuff(type);
            generator.onBuffEffectStarted(type, collector, type);
        }
    }

    // Getters
    public GameMode getMode() { return mode; }
    public String getMapId() { return mapId; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public List<Player> getPlayers() { return players; }
    public List<Platform> getPlatforms() { return platforms; }
    public List<Hazard> getHazards() { return hazards; }
    public List<Buff> getBuffs() { return buffs; }
    public List<FishItem> getFishes() { return fishes; }
    public List<FloatingText> getFloatingTexts() { return floatingTexts; }
    public ParticleSystem getParticleSystem() { return particleSystem; }
    public ScreenShake getScreenShake() { return screenShake; }
    public double getSurvivalTime() { return survivalTime; }
    public int getScore() { return score; }
    public int getFishesCollected() { return fishesCollected; }
    public boolean isGameOver() { return gameOver; }
    public ProceduralGenerator getGenerator() { return generator; }
}
