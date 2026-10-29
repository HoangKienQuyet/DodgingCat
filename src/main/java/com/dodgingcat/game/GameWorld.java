package com.dodgingcat.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/** Java-only deterministic 120 Hz simulation; distance is pixels and time is seconds. */
public final class GameWorld {
    public static final double WIDTH = 960;
    public static final double HEIGHT = 640;
    public static final double FLOOR_Y = 576;
    public static final double FIXED_STEP = 1.0 / 120.0;
    public static final double MIN_WARNING_SECONDS = 0.9;
    public static final double MIN_REACTION_SECONDS = 0.45;
    public static final double BUFF_SECONDS = 5;
    public static final int MAX_HAZARDS = 6;
    public static final double VERSUS_DURATION_SECONDS = 60;
    public static final double PICKUP_NOTICE_SECONDS = 1.2;
    public static final double FLAME_SAFE_DISTANCE = 120;
    private static final int MAX_PICKUP_NOTICES = 32;

    public enum State { MENU, PLAYING, PAUSED, GAME_OVER }
    public enum Mode { SOLO, VERSUS }
    public enum MapTheme { GARDEN, KITCHEN }
    /** Permanent shop unlocks enable matching random pickups, without starting effects. */
    public enum Boost { SHIELD, INVINCIBILITY, DOUBLE_FISH, BOMB }
    public enum HazardKind { KNIFE, DART, LASER, POISON, SCISSORS, FLAME }
    public enum BuffKind { SHIELD, INVINCIBILITY, DOUBLE_FISH, BOMB }
    public record Input(boolean left, boolean right, boolean jump, boolean drop) {
        public static final Input NONE = new Input(false, false, false, false);
    }
    public record Platform(double x, double y, double width) { }
    /** Immutable visual event anchored to the collected item's center; null kind means fish. */
    public record PickupNotice(long id, int playerId, double x, double y, String text,
                               BuffKind kind, double bornAt) { }

    private static final double GRAVITY = 1650;
    private static final double JUMP_SPEED = 650;
    private static final double MOVE_SPEED = 300;
    private static final List<Platform> GARDEN_PLATFORMS = List.of(
            new Platform(130, 462, 185), new Platform(645, 462, 185),
            new Platform(385, 360, 190),
            new Platform(130, 258, 185), new Platform(645, 258, 185));
    private static final List<Platform> KITCHEN_PLATFORMS = List.of(
            new Platform(95, 466, 210), new Platform(655, 466, 210),
            new Platform(365, 356, 230),
            new Platform(150, 248, 180), new Platform(630, 248, 180));

    private final long seed;
    private final Mode mode;
    private final MapTheme mapTheme;
    private final List<Platform> platforms;
    private final Set<Boost> unlockedBoosts;
    private final List<BuffKind> randomBuffPool;
    private Random random;
    private State state = State.MENU;
    private final List<Player> players = new ArrayList<>();
    private final List<Hazard> hazards = new ArrayList<>();
    private final List<Pickup> pickups = new ArrayList<>();
    private final List<PickupNotice> pickupNotices = new ArrayList<>();
    private final List<Player> playerView = Collections.unmodifiableList(players);
    private final List<Hazard> hazardView = Collections.unmodifiableList(hazards);
    private final List<Pickup> pickupView = Collections.unmodifiableList(pickups);
    private final List<PickupNotice> pickupNoticeView = Collections.unmodifiableList(pickupNotices);
    private long nextPickupNoticeId;
    private double accumulator;
    private double elapsed;
    private double hazardTimer;
    private double fishTimer;
    private double buffTimer;
    private int acceptedSpawns;
    private int rejectedSpawns;
    private String lastEvent = "Ready to dodge!";
    private long eventVersion;

    public GameWorld(long seed) { this(seed, Mode.SOLO, Set.of()); }

    public GameWorld(long seed, Mode mode, Set<Boost> unlockedBoosts) {
        this(seed, mode, unlockedBoosts, MapTheme.GARDEN);
    }

    public GameWorld(long seed, Mode mode, Set<Boost> unlockedBoosts, MapTheme mapTheme) {
        this.seed = seed;
        this.mode = Objects.requireNonNull(mode);
        this.mapTheme = Objects.requireNonNull(mapTheme);
        platforms = mapTheme == MapTheme.GARDEN ? GARDEN_PLATFORMS : KITCHEN_PLATFORMS;
        Objects.requireNonNull(unlockedBoosts);
        this.unlockedBoosts = mode == Mode.VERSUS ? Set.of(Boost.values()) : Set.copyOf(unlockedBoosts);
        List<BuffKind> pool = new ArrayList<>();
        for (Boost boost : Boost.values()) {
            if (this.unlockedBoosts.contains(boost)) pool.add(BuffKind.valueOf(boost.name()));
        }
        randomBuffPool = List.copyOf(pool);
        random = new Random(seed);
        resetPlayers();
    }

    private void resetPlayers() {
        players.clear();
        players.add(new Player(1, mode == Mode.SOLO ? (WIDTH - Player.WIDTH) / 2 : 340,
                FLOOR_Y - Player.HEIGHT));
        if (mode == Mode.VERSUS) players.add(new Player(2, 588, FLOOR_Y - Player.HEIGHT));
    }

    /** Restarting repeats the seed and clears all collected, spent and temporary effects. */
    public void start() {
        random = new Random(seed);
        resetPlayers();
        hazards.clear();
        pickups.clear();
        pickupNotices.clear();
        accumulator = elapsed = 0;
        acceptedSpawns = rejectedSpawns = 0;
        hazardTimer = 2.8;
        fishTimer = 0.8;
        buffTimer = 8;
        state = State.PLAYING;
        announce(mode == Mode.SOLO ? "Survive and collect fish!" : "Compete! Who can collect more fish?");
    }

    public void menu() {
        state = State.MENU;
        accumulator = 0;
        pickupNotices.clear();
        announce("Ready to dodge!");
    }

    public void togglePause() {
        if (state == State.PLAYING) { state = State.PAUSED; accumulator = 0; }
        else if (state == State.PAUSED) state = State.PLAYING;
    }

    public void update(double dt, Input input) { update(dt, input, Input.NONE); }

    /** Inputs and jump/drop edge state are independent for each cat. Dead cats remain visible. */
    public void update(double dt, Input first, Input second) {
        if (!Double.isFinite(dt) || dt <= 0 || state != State.PLAYING) return;
        Input[] inputs = { first == null ? Input.NONE : first, second == null ? Input.NONE : second };
        for (int i = 0; i < players.size(); i++) prepareInput(players.get(i), inputs[i]);
        // Discard long stalls so resuming a window cannot advance hazards by several seconds.
        accumulator += Math.min(dt, 0.25);
        while (accumulator + 1e-10 >= FIXED_STEP && state == State.PLAYING) {
            step(FIXED_STEP, inputs);
            accumulator -= FIXED_STEP;
        }
        accumulator = Math.max(0, accumulator);
    }

    private void prepareInput(Player player, Input input) {
        if (!player.alive) return;
        boolean dropPressed = input.drop() && !player.dropWasHeld;
        if (input.jump() && !player.jumpWasHeld) player.jumpBufferRemaining = 0.12;
        if (dropPressed && player.grounded && player.support != null) {
            player.ignorePlatformsRemaining = 0.24;
            player.y += 3;
            player.vy = 90;
            player.grounded = false;
            player.support = null;
            player.coyoteRemaining = player.jumpBufferRemaining = 0;
        }
        player.jumpWasHeld = input.jump();
        player.dropWasHeld = input.drop();
    }

    private void step(double dt, Input[] inputs) {
        elapsed = mode == Mode.VERSUS ? Math.min(VERSUS_DURATION_SECONDS, elapsed + dt) : elapsed + dt;
        pickupNotices.removeIf(notice -> elapsed - notice.bornAt() + 1e-10 >= PICKUP_NOTICE_SECONDS);
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (!player.alive) continue;
            player.invincibilityRemaining = decay(player.invincibilityRemaining, dt);
            player.doubleFishRemaining = decay(player.doubleFishRemaining, dt);
            player.damageGraceRemaining = decay(player.damageGraceRemaining, dt);
            updatePlayer(player, dt, inputs[i]);
        }
        for (Hazard hazard : hazards) hazard.update(dt);
        hazards.removeIf(Hazard::expired);
        for (Pickup pickup : pickups) pickup.age += dt;
        pickups.removeIf(pickup -> pickup.age > 15);
        collectPickups();
        checkDamage();
        if (state == State.PLAYING && mode == Mode.VERSUS && elapsed + 1e-8 >= VERSUS_DURATION_SECONDS) {
            elapsed = VERSUS_DURATION_SECONDS;
            finishRound();
        }
        if (state != State.PLAYING) return;

        hazardTimer -= dt;
        fishTimer -= dt;
        buffTimer -= dt;
        if (hazardTimer <= 0) {
            boolean spawned = false;
            for (int attempt = 0; attempt < 6 && !spawned; attempt++) spawned = offerHazard(createHazard());
            hazardTimer = hazardInterval() * (spawned ? 1 : 0.55);
        }
        if (fishTimer <= 0) { spawnPickup(null); fishTimer = 2.8; }
        if (buffTimer <= 0) {
            if (!randomBuffPool.isEmpty()) spawnPickup(randomBuffPool.get(random.nextInt(randomBuffPool.size())));
            buffTimer = 9 + random.nextDouble() * 3;
        }
    }

    private void updatePlayer(Player player, double dt, Input input) {
        player.ignorePlatformsRemaining = decay(player.ignorePlatformsRemaining, dt);
        if (player.grounded) player.coyoteRemaining = 0.10;
        else player.coyoteRemaining = decay(player.coyoteRemaining, dt);
        if (player.jumpBufferRemaining > 0 && (player.grounded || player.coyoteRemaining > 0)) {
            player.vy = -JUMP_SPEED;
            player.grounded = false;
            player.support = null;
            player.coyoteRemaining = player.jumpBufferRemaining = 0;
        }
        player.jumpBufferRemaining = decay(player.jumpBufferRemaining, dt);
        double target = input.left() == input.right() ? 0 : (input.left() ? -MOVE_SPEED : MOVE_SPEED);
        player.vx = approach(player.vx, target, (player.grounded ? 2800 : 2100) * dt);
        if (target != 0) player.facingRight = target > 0;
        player.x += player.vx * dt;
        if (player.x < 8) { player.x = 8; player.vx = 0; }
        if (player.x + Player.WIDTH > WIDTH - 8) { player.x = WIDTH - 8 - Player.WIDTH; player.vx = 0; }
        double oldBottom = player.y + Player.HEIGHT;
        player.vy += GRAVITY * dt;
        player.y += player.vy * dt;
        player.grounded = false;
        player.support = null;
        if (player.vy >= 0) {
            double landingY = FLOOR_Y;
            Platform landingPlatform = null;
            if (player.ignorePlatformsRemaining <= 0) {
                for (Platform platform : platforms) {
                    if (oldBottom <= platform.y() + 0.5 && player.y + Player.HEIGHT >= platform.y()
                            && player.x + Player.WIDTH - 3 > platform.x()
                            && player.x + 3 < platform.x() + platform.width() && platform.y() < landingY) {
                        landingY = platform.y();
                        landingPlatform = platform;
                    }
                }
            }
            if (player.y + Player.HEIGHT >= landingY) {
                player.y = landingY - Player.HEIGHT;
                player.vy = 0;
                player.grounded = true;
                player.support = landingPlatform;
            }
        }
    }

    private void collectPickups() {
        Iterator<Pickup> iterator = pickups.iterator();
        while (iterator.hasNext()) {
            Pickup pickup = iterator.next();
            Player collector = null;
            double nearestCenter = Double.POSITIVE_INFINITY;
            for (Player player : players) {
                if (!player.alive) continue;
                double dx = pickup.x() - clamp(pickup.x(), player.x, player.x + Player.WIDTH);
                double dy = pickup.y() - clamp(pickup.y(), player.y, player.y + Player.HEIGHT);
                if (dx * dx + dy * dy > pickup.radius() * pickup.radius()) continue;
                double cx = pickup.x() - player.x - Player.WIDTH / 2;
                double cy = pickup.y() - player.y - Player.HEIGHT / 2;
                double distance = cx * cx + cy * cy;
                // Nearest cat claims a shared pickup once; exact ties use the lower player id.
                if (distance < nearestCenter) { nearestCenter = distance; collector = player; }
            }
            if (collector == null) continue;
            iterator.remove();
            if (pickup.kind() == null) {
                int amount = collector.doubleFishRemaining > 0 ? 2 : 1;
                collector.fishCollected += amount;
                addPickupNotice(collector, pickup, "+" + amount + " fish");
            } else {
                applyBuff(collector, pickup.kind());
                String text = switch (pickup.kind()) {
                    case SHIELD -> "Shield · 1 hit";
                    case INVINCIBILITY -> "Invincible · 5s";
                    case DOUBLE_FISH -> "Fish ×2 · 5s";
                    case BOMB -> "Traps cleared";
                };
                addPickupNotice(collector, pickup, text);
            }
        }
    }

    private void addPickupNotice(Player collector, Pickup pickup, String text) {
        if (pickupNotices.size() >= MAX_PICKUP_NOTICES) pickupNotices.removeFirst();
        pickupNotices.add(new PickupNotice(++nextPickupNoticeId, collector.id(), pickup.x(), pickup.y(),
                text, pickup.kind(), elapsed));
    }

    private void checkDamage() {
        for (Player player : players) {
            if (!player.alive || player.invincibilityRemaining > 0 || player.damageGraceRemaining > 0) continue;
            for (int i = 0; i < hazards.size(); i++) {
                Hazard hazard = hazards.get(i);
                if (!hazard.active() || !overlaps(player.x + 4, player.y + 3,
                        Player.WIDTH - 8, Player.HEIGHT - 5,
                        hazard.x(), hazard.y(), hazard.width(), hazard.height())) continue;
                if (player.shieldActive) {
                    player.shieldActive = false;
                    player.damageGraceRemaining = 0.85;
                    announce("P" + player.id() + "'s shield blocked a hit!");
                } else {
                    player.alive = false;
                    player.vx = player.vy = 0;
                    announce("P" + player.id() + " was eliminated!");
                }
                break;
            }
        }
        if (players.stream().noneMatch(Player::alive)) {
            finishRound();
        }
    }

    private void finishRound() {
        state = State.GAME_OVER;
        announce(mode == Mode.SOLO ? "Game over — press Enter to retry!"
                : winnerId() == 0 ? "Tie! Press Enter for a rematch."
                : "P" + winnerId() + " wins with more fish! Press Enter for a rematch.");
    }

    /** Testable collection behavior. Permanent unlocks affect spawn eligibility, not this effect. */
    void applyBuff(BuffKind kind) { applyBuff(player(), kind); }
    void applyBuff(Player player, BuffKind kind) {
        switch (kind) {
            case SHIELD -> player.shieldActive = true;
            case INVINCIBILITY -> player.invincibilityRemaining = BUFF_SECONDS;
            case DOUBLE_FISH -> player.doubleFishRemaining = BUFF_SECONDS;
            case BOMB -> hazards.clear();
        }
    }

    private Hazard createHazard() {
        int level = difficultyLevel();
        double roll = random.nextDouble();
        if (mapTheme == MapTheme.GARDEN) {
            return roll < 0.58
                    ? horizontalHazard(HazardKind.DART, 28, 10, 28, 240 + Math.min(level - 1, 8) * 16)
                    : horizontalHazard(HazardKind.POISON, 58, 28, 36, 105 + Math.min(level - 1, 8) * 8);
        }
        if (roll < 0.5) {
            return new Hazard(HazardKind.KNIFE, 34 + random.nextDouble() * (WIDTH - 82),
                    6, 14, 34, 0, 215 + Math.min(level - 1, 8) * 18, 0);
        }
        if (roll < 0.8) return horizontalHazard(HazardKind.SCISSORS, 34, 22, 32,
                225 + Math.min(level - 1, 8) * 16);
        int supportIndex = random.nextInt(platforms.size() + 1);
        double x, baseY;
        if (supportIndex == 0) {
            x = 36 + random.nextDouble() * (WIDTH - 108);
            baseY = FLOOR_Y;
        } else {
            Platform support = platforms.get(supportIndex - 1);
            x = support.x() + 24 + random.nextDouble() * (support.width() - 84);
            baseY = support.y();
        }
        return new Hazard(HazardKind.FLAME, x, baseY - 56, 36, 56, 0, 0, 0);
    }

    private Hazard horizontalHazard(HazardKind kind, double width, double height, double laneOffset, double speed) {
        int direction = random.nextBoolean() ? 1 : -1;
        List<Double> levels = new ArrayList<>();
        levels.add(FLOOR_Y);
        for (Platform platform : platforms) if (!levels.contains(platform.y())) levels.add(platform.y());
        double y = levels.get(random.nextInt(levels.size())) - laneOffset;
        return new Hazard(kind, direction > 0 ? -width : WIDTH, y, width, height, direction, speed, 0);
    }

    /**
     * Local constraints, not a full trajectory proof: cap/density, spaced knife corridors,
     * at least half the field outside knife projections, and a sampled escape for EVERY living
     * cat using actual fall time. Horizontal hazards leave at least 450ms travel reaction in
     * a cat's current vertical lane; flames have supported geometry and a 120px safe gap.
     * Lasers remain available to fixtures with warnings but are not generated by either map.
     */
    boolean offerHazard(Hazard candidate) {
        if (!canSpawn(candidate)) { rejectedSpawns++; return false; }
        hazards.add(candidate);
        acceptedSpawns++;
        return true;
    }

    private boolean canSpawn(Hazard candidate) {
        int cap = Math.min(MAX_HAZARDS, 3 + (int) (elapsed / 30));
        if (hazards.size() >= cap) return false;
        if (candidate.kind() == HazardKind.LASER && candidate.warningRemaining() + 1e-9 < MIN_WARNING_SECONDS) return false;
        if (horizontal(candidate) && !horizontalHasReactionTime(candidate)) return false;
        if (candidate.kind() == HazardKind.FLAME && !safeFlame(candidate)) return false;
        for (Player player : players) {
            if (player.alive && candidate.active() && overlaps(player.x - 3, player.y - 3,
                    Player.WIDTH + 6, Player.HEIGHT + 6, candidate.x(), candidate.y(),
                    candidate.width(), candidate.height())) return false;
        }
        for (Hazard existing : hazards) {
            if (candidate.kind() == HazardKind.LASER || existing.kind() == HazardKind.LASER) {
                if (horizontal(candidate) || horizontal(existing) || candidate.kind() == HazardKind.FLAME
                        || existing.kind() == HazardKind.FLAME
                        || candidate.kind() == existing.kind() || hazards.size() >= 2) return false;
            }
            if (candidate.kind() == HazardKind.KNIFE && existing.kind() == HazardKind.KNIFE
                    && Math.abs(candidate.x() - existing.x()) < 85) return false;
            if (horizontal(candidate) && horizontal(existing)
                    && Math.abs(candidate.y() - existing.y()) < 60) return false;
            if (candidate.kind() == HazardKind.FLAME && existing.kind() == HazardKind.FLAME
                    && Math.abs(candidate.x() - existing.x()) < 110
                    && Math.abs(candidate.y() - existing.y()) < 80) return false;
        }
        if (candidate.kind() == HazardKind.KNIFE) {
            List<double[]> blocked = new ArrayList<>();
            for (Hazard hazard : hazards) if (hazard.kind() == HazardKind.KNIFE) blocked.add(knifeCorridor(hazard));
            blocked.add(knifeCorridor(candidate));
            blocked.sort(Comparator.comparingDouble(a -> a[0]));
            double covered = 0, end = 0;
            for (double[] segment : blocked) {
                covered += Math.max(0, segment[1] - Math.max(end, segment[0]));
                end = Math.max(end, segment[1]);
            }
            if (covered > WIDTH * 0.5) return false;
            for (Player player : players) {
                if (player.alive && !hasHorizontalEscape(player, candidate, blocked)) return false;
            }
        }
        return true;
    }

    private static boolean horizontal(Hazard hazard) {
        return hazard.kind() == HazardKind.DART || hazard.kind() == HazardKind.POISON
                || hazard.kind() == HazardKind.SCISSORS;
    }

    private boolean horizontalHasReactionTime(Hazard dart) {
        for (Player player : players) {
            if (!player.alive || player.y + Player.HEIGHT + 12 <= dart.y()
                    || player.y - 12 >= dart.y() + dart.height()) continue;
            double distance = dart.direction() > 0 ? player.x - dart.x() - dart.width()
                    : dart.x() - player.x - Player.WIDTH;
            // A dart already behind the cat and travelling away cannot reach its current box.
            boolean movingAway = dart.direction() > 0 ? dart.x() > player.x + Player.WIDTH
                    : dart.x() + dart.width() < player.x;
            if (!movingAway && (dart.speed() <= 0 || distance / dart.speed() < MIN_REACTION_SECONDS)) return false;
        }
        return true;
    }

    private boolean safeFlame(Hazard flame) {
        double bottom = flame.y() + flame.height();
        boolean supported = Math.abs(bottom - FLOOR_Y) < 0.5 && flame.x() >= 16
                && flame.x() + flame.width() <= WIDTH - 16;
        for (Platform platform : platforms) {
            supported |= Math.abs(bottom - platform.y()) < 0.5 && flame.x() >= platform.x() + 8
                    && flame.x() + flame.width() <= platform.x() + platform.width() - 8;
        }
        if (!supported || flame.y() < 0) return false;
        for (Player player : players) {
            if (!player.alive) continue;
            double dx = Math.max(0, Math.max(player.x - flame.x() - flame.width(),
                    flame.x() - player.x - Player.WIDTH));
            double dy = Math.max(0, Math.max(player.y - flame.y() - flame.height(),
                    flame.y() - player.y - Player.HEIGHT));
            if (Math.hypot(dx, dy) < FLAME_SAFE_DISTANCE) return false;
        }
        return true;
    }

    private boolean hasHorizontalEscape(Player player, Hazard candidate, List<double[]> blocked) {
        // Travel is real and visible, never an invisible delay. Reserve reaction/acceleration margin.
        double fallTime = candidate.speed() > 0
                ? Math.max(0, player.y - candidate.y() - candidate.height()) / candidate.speed() : 0;
        double budget = Math.max(0, Math.min(fallTime, 1.5) - 0.15) * MOVE_SPEED * 0.8;
        double center = player.x + Player.WIDTH / 2;
        double minimum = Math.max(8 + Player.WIDTH / 2, center - budget);
        double maximum = Math.min(WIDTH - 8 - Player.WIDTH / 2, center + budget);
        for (double destination = minimum; destination <= maximum; destination += 8) {
            if (outsideCorridors(destination, blocked)) return true;
        }
        return outsideCorridors(maximum, blocked);
    }

    private static boolean outsideCorridors(double destination, List<double[]> blocked) {
        for (double[] corridor : blocked) if (destination >= corridor[0] && destination <= corridor[1]) return false;
        return true;
    }

    private static double[] knifeCorridor(Hazard hazard) {
        return new double[] { Math.max(0, hazard.x() - 35), Math.min(WIDTH, hazard.x() + hazard.width() + 35) };
    }
    private double hazardInterval() { return Math.max(0.72, 2.15 - elapsed * 0.017); }

    private void spawnPickup(BuffKind kind) {
        if (pickups.size() >= 7) return;
        int supportCount = elapsed < 12 ? 3 : platforms.size() + 1;
        for (int attempt = 0; attempt < 8; attempt++) {
            int supportIndex = random.nextInt(supportCount);
            double x, y;
            if (supportIndex == 0) {
                x = 45 + random.nextDouble() * (WIDTH - 90);
                y = FLOOR_Y - 18;
            } else {
                Platform support = platforms.get(supportIndex - 1);
                x = support.x() + 24 + random.nextDouble() * (support.width() - 48);
                y = support.y() - 18;
            }
            boolean crowded = false;
            for (Pickup pickup : pickups) {
                if (Math.abs(pickup.x() - x) < 32 && Math.abs(pickup.y() - y) < 32) crowded = true;
            }
            for (Hazard hazard : hazards) {
                if (hazard.active() && overlaps(x - 18, y - 18, 36, 36,
                        hazard.x() - 12, hazard.y() - 12, hazard.width() + 24, hazard.height() + 24)) crowded = true;
            }
            if (!crowded) { pickups.add(new Pickup(x, y, kind)); return; }
        }
    }

    private void announce(String message) { lastEvent = message; eventVersion++; }
    private static double decay(double value, double dt) { return value <= dt + 1e-10 ? 0 : value - dt; }
    private static double approach(double value, double target, double amount) {
        return value < target ? Math.min(target, value + amount) : Math.max(target, value - amount);
    }
    private static double clamp(double value, double low, double high) { return Math.max(low, Math.min(high, value)); }
    private static boolean overlaps(double ax, double ay, double aw, double ah,
                                    double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    // Package-private fixtures keep exact domain tests independent of JavaFX and reflection.
    void placePlayer(double x, double bottom, boolean grounded) { placePlayer(1, x, bottom, grounded); }
    void placePlayer(int id, double x, double bottom, boolean grounded) {
        Player player = players.get(id - 1);
        player.x = x;
        player.y = bottom - Player.HEIGHT;
        player.vx = player.vy = 0;
        player.grounded = grounded;
        player.support = null;
        for (Platform p : platforms) {
            if (Math.abs(bottom - p.y()) < 0.1 && x + Player.WIDTH > p.x() && x < p.x() + p.width()) player.support = p;
        }
    }
    void addPickup(Pickup pickup) { pickups.add(pickup); }
    void addHazardForCollision(Hazard hazard) { hazards.add(hazard); }
    List<BuffKind> randomBuffPool() { return randomBuffPool; }

    public State state() { return state; }
    public Mode mode() { return mode; }
    public MapTheme mapTheme() { return mapTheme; }
    public Set<Boost> unlockedBoosts() { return unlockedBoosts; }
    public Player player() { return players.getFirst(); }
    public List<Player> players() { return playerView; }
    public List<Platform> platforms() { return platforms; }
    public List<Hazard> hazards() { return hazardView; }
    public List<Pickup> pickups() { return pickupView; }
    public List<PickupNotice> pickupNotices() { return pickupNoticeView; }
    public double elapsed() { return elapsed; }
    public double timeRemaining() { return mode == Mode.VERSUS ? Math.max(0, VERSUS_DURATION_SECONDS - elapsed) : 0; }
    public int score() { return (int) Math.floor(elapsed + 1e-8); }
    public int fishCollected() { return players.stream().mapToInt(Player::fishCollected).sum(); }
    public double invincibilityRemaining() { return player().invincibilityRemaining(); }
    public double doubleFishRemaining() { return player().doubleFishRemaining(); }
    public int difficultyLevel() { return Math.min(12, 1 + (int) (elapsed / 15)); }
    public int acceptedSpawns() { return acceptedSpawns; }
    public int rejectedSpawns() { return rejectedSpawns; }
    public String lastEvent() { return lastEvent; }
    public long eventVersion() { return eventVersion; }
    /** Pending games and solo games return zero. A final versus tie also returns zero. */
    public int winnerId() {
        if (mode != Mode.VERSUS || state != State.GAME_OVER) return 0;
        int comparison = Integer.compare(players.get(0).fishCollected, players.get(1).fishCollected);
        return comparison == 0 ? 0 : comparison > 0 ? 1 : 2;
    }
}
