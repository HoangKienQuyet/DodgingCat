package com.dodgingcat.generation;

import com.dodgingcat.buffs.Buff;
import com.dodgingcat.buffs.BuffType;
import com.dodgingcat.game.FishItem;
import com.dodgingcat.game.FishType;
import com.dodgingcat.game.Platform;
import com.dodgingcat.game.PlatformType;
import com.dodgingcat.game.Player;
import com.dodgingcat.hazards.CannonHazard;
import com.dodgingcat.hazards.Hazard;
import com.dodgingcat.hazards.HazardOrientation;
import com.dodgingcat.hazards.LaserHazard;
import com.dodgingcat.hazards.OrbitalHazard;
import com.dodgingcat.hazards.TopDownHazard;

import java.util.List;
import java.util.Random;

/**
 * Procedural generation engine with map-specific hazard selection, universal hazards, and fairness checking.
 */
public class ProceduralGenerator {
    private final Random random;
    private final FairnessValidator fairnessValidator;

    private double timeSinceLastHazardSpawn = 0.0;
    private final java.util.EnumMap<BuffType, Double> buffCooldowns = new java.util.EnumMap<>(BuffType.class);
    private record ActiveBuff(Player player, BuffType effect) {}
    private final java.util.EnumMap<BuffType, ActiveBuff> activeBuffEffects = new java.util.EnumMap<>(BuffType.class);
    private double buffRollTimer = 0.5;
    private double fishSpawnCooldown = 4.0;
    private double crumbleCheckTimer = 5.0;

    // Map-specific hazard subtypes
    private static final String[] KITCHEN_HAZARDS = {"knife", "frying_pan", "rolling_pin", "salt_shaker", "blender"};
    private static final String[] LIVING_ROOM_HAZARDS = {"plant_pot", "remote_control", "book", "cushion", "lamp"};
    private static final String[] BEDROOM_HAZARDS = {"alarm_clock", "pillow", "slipper", "mirror_shard", "toy_block"};
    private static final String[] GARDEN_HAZARDS = {"apple", "bee_hive", "trowel", "watermelon", "acorn"};
    private static final String[] ROOFTOP_HAZARDS = {"brick", "satellite_dish", "antenna", "weather_vane", "lightning"};

    public ProceduralGenerator() {
        this(new Random(), new FairnessValidator());
    }

    public ProceduralGenerator(long seed) {
        this(new Random(seed), new FairnessValidator());
    }

    public ProceduralGenerator(Random random, FairnessValidator validator) {
        this.random = random;
        this.fairnessValidator = validator;
    }

    public void update(double dt,
                       double survivalTime,
                       double screenWidth,
                       double screenHeight,
                       List<Player> players,
                       List<Hazard> existingHazards,
                       List<Platform> platforms,
                       List<Buff> existingBuffs,
                       List<FishItem> existingFishes,
                       boolean isCoopMode) {
        update(dt, survivalTime, "kitchen", screenWidth, screenHeight, players, existingHazards, platforms, existingBuffs, existingFishes, isCoopMode);
    }

    public void update(double dt,
                       double survivalTime,
                       String activeMapId,
                       double screenWidth,
                       double screenHeight,
                       List<Player> players,
                       List<Hazard> existingHazards,
                       List<Platform> platforms,
                       List<Buff> existingBuffs,
                       List<FishItem> existingFishes,
                       boolean isCoopMode) {
        timeSinceLastHazardSpawn += dt;
        buffCooldowns.replaceAll((type, value) -> Math.max(0.0, value - dt));
        refreshBuffEffects();
        buffRollTimer -= dt;
        fishSpawnCooldown -= dt;

        // 1. Hazard Spawning
        double targetInterval = GenerationRules.getTargetSpawnInterval(survivalTime);
        int activeCount = (int) existingHazards.stream().filter(Hazard::isActive).count();
        int maxAllowed = GenerationRules.getMaxHazardsForTime(survivalTime);

        if (timeSinceLastHazardSpawn >= targetInterval && activeCount < maxAllowed) {
            Hazard candidate = tryGenerateFairHazard(survivalTime, activeMapId, screenWidth, screenHeight, players, existingHazards, platforms, activeCount);
            if (candidate != null) {
                existingHazards.add(candidate);
                timeSinceLastHazardSpawn = 0.0;
            }
        }

        // 2. Buff Spawning
        boolean playerAlreadyCarriesBuff = players.stream().anyMatch(Player::hasActiveTimedBuff);
        if (buffRollTimer <= 0.0 && existingBuffs.isEmpty() && !playerAlreadyCarriesBuff
                && players.stream().anyMatch(Player::isAlive)) {
            spawnRandomBuff(screenWidth, platforms, existingBuffs, isCoopMode, players);
            buffRollTimer = 0.5;
        }

        // 3. Fish Currency Spawning
        if (fishSpawnCooldown <= 0.0 && existingFishes.size() < 5) {
            int previousCount = existingFishes.size();
            spawnRandomFish(screenWidth, platforms, existingFishes, players);
            // Count from a successful spawn, independently of pickup/expiration.
            if (existingFishes.size() > previousCount) fishSpawnCooldown = 4.0;
        }

        // 4. Crumbling Platform Sequence (survival time > 60s)
        if (survivalTime >= 60.0) {
            relocateCollapsedPlatforms(screenWidth, screenHeight, players, platforms);
            crumbleCheckTimer -= dt;
            if (crumbleCheckTimer <= 0.0) {
                triggerSafePlatformCrumble(platforms, players);
                crumbleCheckTimer = 7.0 + random.nextDouble() * 5.0;
            }
        }
    }

    private Hazard tryGenerateFairHazard(double survivalTime,
                                         String activeMapId,
                                         double screenWidth,
                                         double screenHeight,
                                         List<Player> players,
                                         List<Hazard> existingHazards,
                                         List<Platform> platforms,
                                         int activeCount) {
        int maxAttempts = 6;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Hazard candidate = createCandidateHazard(survivalTime, activeMapId, screenWidth, screenHeight);
            FairnessResult result = fairnessValidator.validate(
                    candidate,
                    timeSinceLastHazardSpawn,
                    activeCount,
                    survivalTime,
                    players,
                    existingHazards,
                    platforms
            );
            if (result.accepted()) {
                return candidate;
            }
        }
        return null;
    }

    private Hazard createCandidateHazard(double survivalTime, String activeMapId, double screenWidth, double screenHeight) {
        double roll = random.nextDouble();
        double speedMult = GenerationRules.getSpeedMultiplier(survivalTime);
        double fallingObjectChance = survivalTime < 30.0 ? 0.50 : survivalTime < 60.0 ? 0.45 : 0.40;
        double remainingChance = 1.0 - fallingObjectChance;
        double laserLimit = fallingObjectChance + remainingChance / 3.0;
        double cannonLimit = fallingObjectChance + remainingChance * 2.0 / 3.0;

        if (roll < fallingObjectChance) {
            // Map-specific Top-down falling hazard
            String subType = getMapHazardSubtype(activeMapId);
            double x = 40.0 + random.nextDouble() * (screenWidth - 80.0);
            double startY = -40.0 - random.nextDouble() * 40.0;
            double[] dimensions = getHazardDimensions(subType);
            double width = dimensions[0] * 1.20;
            double height = dimensions[1] * 1.20;
            // Early falling speed now matches the original early cannonball pace;
            // the existing survival multiplier still ramps it up later.
            double initialVy = (325.0 + random.nextDouble() * 50.0) * speedMult;
            double gravity = (90.0 + random.nextDouble() * 55.0) * speedMult;

            return new TopDownHazard(x, startY, width, height, subType, initialVy, gravity);
        } else if (roll < laserLimit) {
            // Universal Hazard 1: Laser (Horizontal or Vertical)
            boolean isVertical = random.nextBoolean();
            double warningDuration = GenerationRules.HAZARD_WARNING_DURATION_SECONDS;
            double beamThickness = 24.0;

            if (isVertical) {
                double lineX = 100.0 + random.nextDouble() * (screenWidth - 200.0);
                return new LaserHazard(HazardOrientation.VERTICAL, lineX, beamThickness, warningDuration);
            } else {
                double floorY = screenHeight * 0.8555;
                double maxLaneY = Math.max(160.0, floorY - beamThickness / 2.0 - 24.0);
                double lineY = 120.0 + random.nextDouble() * Math.max(1.0, maxLaneY - 120.0);
                return new LaserHazard(HazardOrientation.HORIZONTAL, lineY, beamThickness, warningDuration);
            }
        } else if (roll < cannonLimit) {
            // Universal Hazard 2: Cannon (Horizontal or Vertical)
            boolean isVertical = random.nextBoolean();
            double warningDuration = GenerationRules.HAZARD_WARNING_DURATION_SECONDS;
            double speed = (340.0 + random.nextDouble() * 140.0) * speedMult;
            // Match the projectile diameter to the 40px pendulum weight.
            double caliber = 52.0;

            if (isVertical) {
                double laneX = 100.0 + random.nextDouble() * (screenWidth - 200.0);
                boolean shootsDown = random.nextBoolean();
                return new CannonHazard(HazardOrientation.VERTICAL, laneX, shootsDown, warningDuration, speed, caliber);
            } else {
                double floorY = screenHeight * 0.8555;
                double maxLaneY = Math.max(180.0, floorY - caliber / 2.0 - 24.0);
                double laneY = 140.0 + random.nextDouble() * Math.max(1.0, maxLaneY - 140.0);
                boolean shootsRight = random.nextBoolean();
                return new CannonHazard(HazardOrientation.HORIZONTAL, laneY, shootsRight, warningDuration, speed, caliber);
            }
        } else {
            // Universal Hazard 3: lower, longer pendulum or a circular-orbiting weight.
            boolean circularOrbit = random.nextBoolean();
            double chainLength = circularOrbit
                    ? 120.0 + random.nextDouble() * 65.0
                    : 190.0 + random.nextDouble() * 80.0;
            double maxAngle = 55.0 + random.nextDouble() * 25.0;
            double horizontalReach = circularOrbit ? chainLength : chainLength * Math.sin(Math.toRadians(maxAngle));
            double anchorX = horizontalReach + 25.0
                    + random.nextDouble() * Math.max(1.0, screenWidth - 2.0 * horizontalReach - 50.0);
            double anchorY = circularOrbit
                    ? screenHeight * (0.43 + random.nextDouble() * 0.12)
                    : 100.0 + random.nextDouble() * 85.0;
            // Match each one-way sweep through the lower arc to the 0.9s pendulum SFX.
            // For both oscillation and circular orbit, a half-turn spans PI radians.
            double frequency = Math.PI / 0.9;
            double weightRadius = 26.0;
            double duration = 3.5 + random.nextDouble() * 1.5; // never active longer than 5 seconds

            return new OrbitalHazard(anchorX, anchorY, chainLength, maxAngle, frequency, weightRadius, duration, circularOrbit);
        }
    }

    private String getMapHazardSubtype(String mapId) {
        String[] pool = switch (mapId != null ? mapId.toLowerCase() : "kitchen") {
            case "living_room" -> LIVING_ROOM_HAZARDS;
            case "bedroom" -> BEDROOM_HAZARDS;
            case "garden" -> GARDEN_HAZARDS;
            case "rooftop" -> ROOFTOP_HAZARDS;
            default -> KITCHEN_HAZARDS;
        };
        return pool[random.nextInt(pool.length)];
    }

    private double[] getHazardDimensions(String subtype) {
        return switch (subtype) {
            case "knife" -> new double[]{24.0, 44.0};
            case "frying_pan" -> new double[]{58.0, 40.0};
            case "rolling_pin" -> new double[]{50.0, 27.0};
            case "salt_shaker" -> new double[]{20.0, 28.0};
            case "blender" -> new double[]{42.0, 58.0};
            case "plant_pot" -> new double[]{38.0, 46.0};
            case "remote_control" -> new double[]{21.0, 34.0};
            case "book" -> new double[]{40.0, 34.0};
            case "cushion" -> new double[]{56.0, 38.0};
            case "lamp" -> new double[]{32.0, 52.0};
            case "alarm_clock" -> new double[]{38.0, 38.0};
            case "pillow" -> new double[]{56.0, 36.0};
            case "slipper" -> new double[]{40.0, 27.0};
            case "mirror_shard" -> new double[]{25.0, 40.0};
            case "toy_block" -> new double[]{34.0, 34.0};
            case "apple" -> new double[]{32.0, 34.0};
            case "bee_hive" -> new double[]{48.0, 50.0};
            case "trowel" -> new double[]{23.0, 46.0};
            case "watermelon" -> new double[]{58.0, 46.0};
            case "acorn" -> new double[]{25.0, 30.0};
            case "brick" -> new double[]{44.0, 30.0};
            case "satellite_dish" -> new double[]{60.0, 48.0};
            case "antenna" -> new double[]{25.0, 56.0};
            case "weather_vane" -> new double[]{50.0, 44.0};
            case "lightning" -> new double[]{38.0, 58.0};
            default -> new double[]{36.0, 36.0};
        };
    }

    private boolean spawnRandomBuff(double screenWidth, List<Platform> platforms, List<Buff> buffs,
                                    boolean isCoopMode, List<Player> players) {
        List<Platform> elevated = platforms.stream()
                .filter(p -> p.getType() != PlatformType.SOLID_FLOOR && !p.isCollapsed())
                .toList();
        if (elevated.isEmpty()) return false;

        Platform target = elevated.get(random.nextInt(elevated.size()));
        double spawnX = target.getX() + 15.0 + random.nextDouble() * Math.max(10.0, target.getWidth() - 40.0);
        // Buffs are 42px tall and float up to 3.5px in either direction.
        // Anchor their lower edge just above the platform so the art never sinks
        // into the collision surface at the bottom of its bob cycle.
        double spawnY = target.getY() - 46.0;

        boolean teammateDown = isCoopMode && players.stream().anyMatch(player -> !player.isAlive());
        java.util.EnumMap<BuffType, Double> weights = new java.util.EnumMap<>(BuffType.class);
        weights.put(BuffType.DOUBLE_SCORE, 40.0);
        weights.put(BuffType.BOMB, 25.0);
        weights.put(BuffType.INVINCIBILITY, 20.0);
        weights.put(BuffType.SHIELD, 15.0);
        if (teammateDown) {
            weights.replaceAll((type, rate) -> rate * 0.8);
            weights.put(BuffType.HEART, 20.0);
        }
        weights.entrySet().removeIf(entry -> entry.getKey() == BuffType.HEART && !teammateDown
                || buffCooldowns.getOrDefault(entry.getKey(), 0.0) > 0.0
                || activeBuffEffects.containsKey(entry.getKey()));
        double totalRate = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalRate <= 0.0) return false;
        double roll = random.nextDouble() * totalRate;
        BuffType chosenType = weights.keySet().iterator().next();
        for (var entry : weights.entrySet()) {
            if (roll < entry.getValue()) { chosenType = entry.getKey(); break; }
            roll -= entry.getValue();
        }
        double lifetime = switch (chosenType) {
            case DOUBLE_SCORE -> 8.0;
            case BOMB -> 5.0;
            case INVINCIBILITY, SHIELD, HEART -> 4.0;
        };
        buffs.add(new Buff(chosenType, spawnX, spawnY, lifetime));
        return true;
    }

    /** Uncollected pickup disappeared, or an instantaneous effect has completed. */
    public void onBuffFinished(BuffType type) {
        activeBuffEffects.remove(type);
        buffCooldowns.put(type, type.getCooldownSeconds());
    }

    /** A collected timed buff stays unavailable until its actual effect ends. */
    public void onBuffEffectStarted(BuffType source, Player target, BuffType effect) {
        activeBuffEffects.put(source, new ActiveBuff(target, effect));
    }

    public void refreshBuffEffects() {
        var iterator = activeBuffEffects.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ActiveBuff active = entry.getValue();
            if (!active.player.isAlive() || !active.player.hasBuffEffect(active.effect)) {
                buffCooldowns.put(entry.getKey(), entry.getKey().getCooldownSeconds());
                iterator.remove();
            }
        }
    }

    public double getBuffCooldownRemaining(BuffType type) { return buffCooldowns.getOrDefault(type, 0.0); }

    private void spawnRandomFish(double screenWidth, List<Platform> platforms, List<FishItem> fishes,
                                 List<Player> players) {
        List<Platform> available = platforms.stream().filter(p -> !p.isCollapsed()).toList();
        if (available.isEmpty()) return;

        Platform target = available.get(random.nextInt(available.size()));
        double spawnX = target.getX() + 10.0 + random.nextDouble() * Math.max(5.0, target.getWidth() - 35.0);
        // 85% single fish (1), 15% fish box (5).
        FishType chosenFish = (random.nextDouble() < 0.85) ? FishType.SINGLE_FISH : FishType.FISH_CAN;
        // Keep the full item above the platform through the entire 2.5px bob range.
        double spawnY = target.getY() - chosenFish.getHeight() - 9.0;
        List<String> legendaryCurrencySkins = players.stream()
                .map(Player::getSkinId)
                .filter(ProceduralGenerator::hasExclusiveCurrency)
                .toList();
        String currencySkin = legendaryCurrencySkins.isEmpty()
                ? null
                : legendaryCurrencySkins.get(random.nextInt(legendaryCurrencySkins.size()));
        fishes.add(new FishItem(chosenFish, spawnX, spawnY, currencySkin));
    }

    private static boolean hasExclusiveCurrency(String skinId) {
        return skinId != null && switch (skinId.toLowerCase(java.util.Locale.ROOT)) {
            case "ghost", "santa", "cyber" -> true;
            default -> false;
        };
    }

    private void triggerSafePlatformCrumble(List<Platform> platforms, List<Player> players) {
        List<Platform> candidates = platforms.stream()
                .filter(p -> p.getType() == PlatformType.CRUMBLING && !p.isCollapsed() && !p.isWarning())
                .filter(p -> canCollapseWithoutStrandingPlatforms(platforms, p))
                .toList();

        long collapsedCount = platforms.stream().filter(Platform::isCollapsed).count();
        if (!candidates.isEmpty() && collapsedCount < 2) {
            Platform chosen = candidates.get(random.nextInt(candidates.size()));
            chosen.triggerCrumble(GenerationRules.PLATFORM_CRUMBLE_WARNING_SECONDS);
        }
    }

    private void relocateCollapsedPlatforms(double screenWidth, double screenHeight, List<Player> players,
                                            List<Platform> platforms) {
        List<Platform> ready = platforms.stream().filter(Platform::isRespawnReady).toList();
        for (Platform platform : ready) {
            PlatformPosition replacement = findReachablePlatformPosition(screenWidth, screenHeight, players, platforms, platform);
            if (replacement == null) continue;
            platform.respawnAt(replacement.x(), replacement.y(), replacement.width());

            long elevatedCount = platforms.stream().filter(p -> p.getType() != PlatformType.SOLID_FLOOR).count();
            // Occasionally add a new branch as the run progresses, with a hard cap to keep layouts readable.
            if (elevatedCount < 10 && random.nextDouble() < 0.25) {
                PlatformPosition extra = findReachablePlatformPosition(screenWidth, screenHeight, players, platforms, null);
                if (extra != null) {
                    platforms.add(new Platform(extra.x(), extra.y(), extra.width(), 16.0, PlatformType.CRUMBLING));
                }
            }
        }
    }

    private PlatformPosition findReachablePlatformPosition(double screenWidth, double screenHeight, List<Player> players,
                                                            List<Platform> platforms, Platform excluded) {
        List<Platform> anchors = platforms.stream()
                .filter(p -> p != excluded && !p.isCollapsed() && !p.isWarning())
                .toList();
        if (anchors.isEmpty()) return null;

        double floorY = screenHeight - 40.0;
        for (int attempt = 0; attempt < 80; attempt++) {
            Platform parent = anchors.get(random.nextInt(anchors.size()));
            double platformWidth = 135.0 + random.nextDouble() * 95.0;
            double rise = 55.0 + random.nextDouble() * 40.0;
            boolean placeAbove = random.nextBoolean();
            double y = parent.getY() + (placeAbove ? -rise : rise);
            if (y < 125.0 || y > floorY - 60.0) continue;

            double gap = random.nextDouble() * 82.0;
            boolean placeLeft = random.nextBoolean();
            double x = placeLeft
                    ? parent.getX() - platformWidth - gap
                    : parent.getX() + parent.getWidth() + gap;
            if (x < 35.0 || x + platformWidth > screenWidth - 35.0) continue;

            boolean tooCloseToPlayer = players.stream().filter(Player::isAlive).anyMatch(player -> {
                boolean horizontalNear = x < player.getX() + player.getSpriteWidth() + 55.0
                        && x + platformWidth > player.getX() - 55.0;
                boolean verticalNear = Math.abs(y - (player.getY() + player.getSpriteHeight())) < 55.0;
                return horizontalNear && verticalNear;
            });
            if (tooCloseToPlayer) continue;

            boolean overlapsPlatform = platforms.stream()
                    .filter(other -> other != excluded && !other.isCollapsed())
                    .anyMatch(other -> Math.abs(y - other.getY()) < 24.0
                            && x < other.getX() + other.getWidth()
                            && x + platformWidth > other.getX());
            boolean tooCloseVertically = platforms.stream()
                    .filter(other -> other != excluded && !other.isCollapsed())
                    .anyMatch(other -> {
                        double verticalGap = Math.abs(y - other.getY());
                        return verticalGap > 0.0 && verticalGap < 55.0;
                    });
            if (overlapsPlatform || tooCloseVertically
                    || !keepsPlatformDistribution(screenWidth, platforms, excluded, x, platformWidth)
                    || !keepsReachablePlatformNetwork(platforms, excluded, x, y, platformWidth)) continue;

            return new PlatformPosition(x, y, platformWidth);
        }
        return null;
    }

    /** Rejects respawn locations that strand any active platform outside the floor-connected jump network. */
    private boolean keepsReachablePlatformNetwork(List<Platform> platforms, Platform excluded,
                                                   double candidateX, double candidateY, double candidateWidth) {
        List<Platform> active = new java.util.ArrayList<>();
        for (Platform platform : platforms) {
            if (platform != excluded && !platform.isCollapsed() && !platform.isWarning()) active.add(platform);
        }
        active.add(new Platform(candidateX, candidateY, candidateWidth, 16.0, PlatformType.CRUMBLING));

        return isPlatformNetworkReachable(active);
    }

    private boolean canCollapseWithoutStrandingPlatforms(List<Platform> platforms, Platform collapsing) {
        List<Platform> active = new java.util.ArrayList<>();
        for (Platform platform : platforms) {
            if (platform != collapsing && !platform.isCollapsed() && !platform.isWarning()) active.add(platform);
        }
        return isPlatformNetworkReachable(active);
    }

    private boolean isPlatformNetworkReachable(List<Platform> active) {
        int floorIndex = -1;
        for (int i = 0; i < active.size(); i++) {
            if (active.get(i).getType() == PlatformType.SOLID_FLOOR) {
                floorIndex = i;
                break;
            }
        }
        if (floorIndex < 0) return false;

        boolean[] reached = new boolean[active.size()];
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        reached[floorIndex] = true;
        queue.add(floorIndex);
        while (!queue.isEmpty()) {
            int from = queue.removeFirst();
            for (int to = 0; to < active.size(); to++) {
                if (!reached[to] && areJumpConnected(active.get(from), active.get(to))) {
                    reached[to] = true;
                    queue.addLast(to);
                }
            }
        }
        for (boolean connected : reached) if (!connected) return false;
        return true;
    }

    private boolean areJumpConnected(Platform a, Platform b) {
        if (a == b) return false;
        Platform lower = a.getY() >= b.getY() ? a : b;
        Platform upper = lower == a ? b : a;
        double rise = lower.getY() - upper.getY();
        if (rise > 96.0) return false;
        double horizontalGap = Math.max(0.0,
                Math.max(a.getX(), b.getX()) - Math.min(a.getX() + a.getWidth(), b.getX() + b.getWidth()));
        return horizontalGap <= 82.0;
    }

    private boolean keepsPlatformDistribution(double screenWidth, List<Platform> platforms, Platform excluded,
                                              double x, double platformWidth) {
        int[] zoneCounts = new int[3];
        int count = 1;
        int candidateZone = zoneFor((x + platformWidth / 2.0), screenWidth);
        zoneCounts[candidateZone]++;
        for (Platform platform : platforms) {
            if (platform == excluded || platform.isCollapsed() || platform.getType() == PlatformType.SOLID_FLOOR) continue;
            zoneCounts[zoneFor(platform.getX() + platform.getWidth() / 2.0, screenWidth)]++;
            count++;
        }
        int occupiedZones = 0;
        int maxInZone = 0;
        for (int zoneCount : zoneCounts) {
            if (zoneCount > 0) occupiedZones++;
            maxInZone = Math.max(maxInZone, zoneCount);
        }
        int minimumZones = count >= 4 ? 3 : count >= 3 ? 2 : 1;
        return occupiedZones >= minimumZones && maxInZone <= Math.ceil(count * 0.60);
    }

    private int zoneFor(double x, double screenWidth) {
        return x < screenWidth / 3.0 ? 0 : x < screenWidth * 2.0 / 3.0 ? 1 : 2;
    }

    private record PlatformPosition(double x, double y, double width) {}

    public FairnessValidator getFairnessValidator() {
        return fairnessValidator;
    }
}
