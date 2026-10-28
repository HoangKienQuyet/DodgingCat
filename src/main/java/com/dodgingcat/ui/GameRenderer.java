package com.dodgingcat.ui;

import com.dodgingcat.buffs.Buff;
import com.dodgingcat.buffs.BuffType;
import com.dodgingcat.audio.AudioManager;
import com.dodgingcat.core.GameMode;
import com.dodgingcat.core.GameState;
import com.dodgingcat.game.FishItem;
import com.dodgingcat.game.GameWorld;
import com.dodgingcat.game.Platform;
import com.dodgingcat.game.Player;
import com.dodgingcat.hazards.CannonHazard;
import com.dodgingcat.hazards.Hazard;
import com.dodgingcat.hazards.HazardOrientation;
import com.dodgingcat.hazards.LaserHazard;
import com.dodgingcat.hazards.OrbitalHazard;
import com.dodgingcat.hazards.TopDownHazard;
import com.dodgingcat.i18n.I18n;
import com.dodgingcat.i18n.Language;
import com.dodgingcat.input.InputManager;
import com.dodgingcat.persistence.GameData;
import com.dodgingcat.persistence.LeaderboardEntry;
import com.dodgingcat.shop.ItemCategory;
import com.dodgingcat.shop.ShopItem;
import com.dodgingcat.shop.ShopManager;
import com.dodgingcat.shop.GachaSequence;
import com.dodgingcat.shop.SkinRarity;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Shared Java2D / JavaFX drawing logic featuring Brotato-inspired main menus,
 * thematic background illustrations, animated cat skins with rarities, and desktop mouse support.
 */
public class GameRenderer {
    private static final double CAT_RENDER_SCALE = 2.0;
    private double width;
    private double height;
    private double globalAnimTimer = 0.0;
    private final Map<Integer, Boolean> cyberGroundedState = new HashMap<>();
    private final Map<Integer, Double> cyberImpactFlashUntil = new HashMap<>();
    private double measuredFps;
    private String fpsLabel = "FPS 0";
    private String fpsDetailLabel = "UPD 0.0ms  DRAW 0.0ms";
    private double updateTimeMs;
    private double drawTimeMs;
    private boolean showFpsCounter;
    private GameState currentHoverScope = GameState.MENU;
    private Set<String> hoveredControlsLastFrame = new HashSet<>();
    private Set<String> hoveredControlsThisFrame = new HashSet<>();
    private final Player previewCat = new Player(99, 0, 0, "ginger");
    private final String[] hudLabels = new String[4];
    private long hudTimeTenths = -1;
    private int hudScore = -1, hudFishes = -1, hudBest = -1;
    private boolean hudLanguageVi;
    private boolean shopBackdropDimmed;
    private boolean modalBackdropDimmed;
    private javafx.scene.image.Image shopBackdrop;

    public GameRenderer(double width, double height) {
        this.width = width;
        this.height = height;
        AssetManager.preloadGameplayAssets();
    }

    public void updateAnimationTimer(double dt) {
        globalAnimTimer += dt;
    }

    public void resize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void setMeasuredFps(double measuredFps) {
        this.measuredFps = measuredFps;
        this.fpsLabel = "FPS " + Math.round(measuredFps);
    }

    public void setFrameMetrics(double measuredFps, double updateTimeMs, double drawTimeMs) {
        setMeasuredFps(measuredFps);
        this.updateTimeMs = updateTimeMs;
        this.drawTimeMs = drawTimeMs;
        this.fpsDetailLabel = String.format(java.util.Locale.ROOT,
                "UPD %.1fms  DRAW %.1fms", updateTimeMs, drawTimeMs);
    }

    public void toggleFpsCounter() {
        showFpsCounter = !showFpsCounter;
    }

    public void render(DrawContext gc,
                       GameState state,
                       GameWorld world,
                       Theme theme,
                       GameData data,
                       ShopManager shopManager,
                       InputManager input,
                       int menuSelectedIndex,
                       int shopTab,
                       int shopSelectedIndex,
                       int pregameTab,
                       int pregameSelectedMapIndex,
                       int pregameSelectedSkinIndex,
                       int pregameP2SkinIndex,
                       boolean pregameP1SkinReady,
                       boolean pregameP2SkinReady,
                       int pregameActiveSkinPlayer,
                       GameMode pendingGameMode,
                       boolean hasSuspendedWorld,
                       double shopScrollOffset,
                       double pregameSkinScrollOffset,
                       double startCountdownElapsed,
                       double startCountdownDuration,
                       int newLeaderboardRank,
                       GachaSequence gacha,
                       int gachaPrice) {
        gc.save();
        hoveredControlsThisFrame.clear();
        currentHoverScope = state;
        shopBackdropDimmed = false;
        modalBackdropDimmed = false;
        shopBackdrop = null;

        boolean isVi = I18n.getLanguage() == Language.VI;

        if (world != null && (state == GameState.COUNTDOWN || state == GameState.PLAYING_SINGLE || state == GameState.PLAYING_COOP || state == GameState.PAUSED || state == GameState.GAME_OVER)) {
            // Apply camera shake
            double shakeX = world.getScreenShake().getOffsetX();
            double shakeY = world.getScreenShake().getOffsetY();
            gc.translate(shakeX, shakeY);

            // Simulation rendering
            renderThematicBackground(gc, theme);
            renderPlatforms(gc, world.getPlatforms(), theme);
            renderFishItems(gc, world.getFishes());
            renderBuffs(gc, world.getBuffs());
            renderHazards(gc, world.getHazards());
            renderPlayers(gc, world.getPlayers(), world.getPlatforms(), theme);

            world.getParticleSystem().render(gc);
            renderFloatingTexts(gc, world.getFloatingTexts());
            renderHUD(gc, world, data, isVi);

            gc.restore();
            gc.save();
        } else {
            // Ambient Menu / Setup Background
            String backgroundMap = state == GameState.MENU ? data.getLastPlayedMap() : data.getSelectedMap();
            if (state == GameState.SHOP && shopTab == 1 && !shopManager.getMapCatalog().isEmpty()) {
                int previewIndex = Math.clamp(shopSelectedIndex, 0, shopManager.getMapCatalog().size() - 1);
                backgroundMap = shopManager.getMapCatalog().get(previewIndex).getId();
            }
            if (state == GameState.SHOP && gacha != null && gacha.isActive()) {
                // Spin audio gives the worker time to prepare the later reveal/result
                // frames, so the prize transition does not allocate a native backdrop.
                for (var upcoming : new MapBackdropCache.Shade[]{MapBackdropCache.Shade.SPIN,
                        MapBackdropCache.Shade.REVEAL, MapBackdropCache.Shade.RESULT})
                    MapBackdropCache.getPrepared(gc,backgroundMap,width,height,upcoming);
                var shade = switch (gacha.getPhase()) {
                    case LEGENDARY_REVEAL -> MapBackdropCache.Shade.REVEAL;
                    case RESULT -> MapBackdropCache.Shade.RESULT;
                    default -> MapBackdropCache.Shade.SPIN;
                };
                modalBackdropDimmed = MapBackdropCache.draw(gc, backgroundMap, width, height, shade);
                if (!modalBackdropDimmed) renderThematicBackground(gc, Theme.fromId(backgroundMap));
            } else {
                renderMenuStageBackground(gc, backgroundMap,
                        state == GameState.PRE_GAME_SELECT || state == GameState.MENU ? null : data.getP1Skin(), state == GameState.SHOP);
            }
        }

        // State Overlays
        switch (state) {
            case BOOT -> renderBootSequence(gc, globalAnimTimer);
            case MENU -> renderBrotatoMainMenu(gc, input, menuSelectedIndex, data, isVi, hasSuspendedWorld);
            case PRE_GAME_SELECT -> renderPreGameSelectScreen(gc, input, shopManager, data, pregameTab, pregameSelectedMapIndex, pregameSelectedSkinIndex, pregameP2SkinIndex, pregameP1SkinReady, pregameP2SkinReady, pregameActiveSkinPlayer, pendingGameMode, pregameSkinScrollOffset, isVi);
            case COUNTDOWN -> renderStartCountdown(gc, startCountdownElapsed, startCountdownDuration, isVi);
            case PAUSED -> renderPauseOverlay(gc, input, isVi);
            case GAME_OVER -> {
                if (world != null) renderGameOverOverlay(gc, input, world, data, isVi, newLeaderboardRank);
            }
            case SHOP -> renderShopScreen(gc, input, shopManager, data, shopTab, shopSelectedIndex, shopScrollOffset, isVi, gacha, gachaPrice);
            case LEADERBOARD -> renderLeaderboardOverlay(gc, input, data, isVi);
            case ABOUT_US -> renderAboutUsOverlay(gc, input, isVi);
            default -> {}
        }

        if (state == GameState.SHOP) {
            renderCurrencyPill(gc, data.getTotalFishes());
        }

        if (showFpsCounter) {
            renderFpsCounter(gc);
        }

        Set<String> previousHoverSet = hoveredControlsLastFrame;
        hoveredControlsLastFrame = hoveredControlsThisFrame;
        hoveredControlsThisFrame = previousHoverSet;
        hoveredControlsThisFrame.clear();
        gc.restore();
    }

    private void renderFpsCounter(DrawContext gc) {
        double x = width - 164.0, y = 105.0;
        Color color = measuredFps >= 60.0 ? Color.web("#59E391")
                : measuredFps >= 45.0 ? Color.web("#FFD166") : Color.web("#FF7272");
        gc.save();
        gc.setGlobalAlpha(0.88);
        gc.setFill(Color.rgb(20, 22, 30, 0.82));
        gc.fillRoundRect(x, y, 156, 36, 8, 8);
        gc.setStroke(color.deriveColor(0, 1, 1, 0.75));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(x, y, 156, 36, 8, 8);
        gc.setFill(color);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.LEFT);
        ArtText.fillText(gc, fpsLabel, x + 8, y + 14);
        gc.setFill(Color.web("#D7DDE8"));
        gc.setFont(Font.font("Segoe UI", 9));
        ArtText.fillText(gc, fpsDetailLabel, x + 8, y + 28);
        gc.restore();
    }


    private void renderStartCountdown(DrawContext gc, double elapsed, double duration, boolean isVi) {
        double phaseTime = Math.max(0.0, Math.min(duration, elapsed));
        String label;
        double phaseProgress;
        if (phaseTime < 1.0) {
            label = "3";
            phaseProgress = phaseTime;
        } else if (phaseTime < 2.0) {
            label = "2";
            phaseProgress = phaseTime - 1.0;
        } else if (phaseTime < 2.7) {
            label = "1";
            phaseProgress = (phaseTime - 2.0) / 0.7;
        } else {
            label = isVi ? "go_vi" : "go_en";
            phaseProgress = (phaseTime - 2.7) / Math.max(0.01, duration - 2.7);
        }

        double pulse = 1.0 + Math.sin(phaseProgress * Math.PI) * 0.06;
        gc.save();
        gc.setFill(Color.rgb(8, 10, 18, 0.20));
        gc.fillRoundRect(width / 2.0 - 88, height / 2.0 - 71, 176, 142, 28, 28);
        gc.setStroke(Color.web("#F3C56B", 0.62));
        gc.setLineWidth(2.0);
        gc.strokeRoundRect(width / 2.0 - 88, height / 2.0 - 71, 176, 142, 28, 28);
        gc.translate(width / 2.0, height / 2.0);
        gc.scale(pulse, pulse);
        boolean numeral = label.length() == 1;
        double artW = numeral ? 62 : 142;
        double artH = numeral ? 82 : 44;
        drawImageFit(gc, "/assets/images/ui/countdown/" + label + ".png",
                -artW / 2.0, -artH / 2.0, artW, artH);
        gc.restore();
    }

    /* =========================================================================
     * THEMATIC GAME BACKGROUNDS (Point 12)
     * ========================================================================= */
    private void renderThematicBackground(DrawContext gc, Theme theme) {
        if (drawMapBackground(gc, theme.getId())) return;
        // Sky / Wall base gradient
        LinearGradient gradient = new LinearGradient(0, 0, 0, height, false, CycleMethod.NO_CYCLE,
                new Stop(0, theme.getSkyTop()),
                new Stop(1, theme.getSkyBottom()));
        gc.setFill(gradient);
        gc.fillRect(0, 0, width, height);

        switch (theme) {
            case KITCHEN -> renderKitchenDecor(gc);
            case LIVING_ROOM -> renderLivingRoomDecor(gc);
            case BEDROOM -> renderBedroomDecor(gc);
            case GARDEN -> renderGardenDecor(gc);
            case ROOFTOP -> renderRooftopDecor(gc);
        }
    }

    private void renderKitchenDecor(DrawContext gc) {
        // Tiled wall pattern
        gc.setStroke(Color.rgb(200, 160, 120, 0.25));
        gc.setLineWidth(1.0);
        for (int y = 50; y < 450; y += 45) {
            gc.strokeLine(0, y, width, y);
        }
        for (int x = 0; x < width; x += 60) {
            gc.strokeLine(x, 50, x, 450);
        }

        // Range hood & cupboard silhouettes
        gc.setFill(Color.rgb(141, 110, 99, 0.25));
        gc.fillRect(140, 50, 180, 70);
        gc.fillRect(width - 320, 50, 180, 70);

        // Hanging utensil rack
        gc.setStroke(Color.rgb(93, 64, 55, 0.5));
        gc.setLineWidth(3.0);
        gc.strokeLine(width / 2.0 - 120, 90, width / 2.0 + 120, 90);
        for (int i = -2; i <= 2; i++) {
            double hx = width / 2.0 + i * 45;
            gc.strokeLine(hx, 90, hx, 120);
            gc.setFill(Color.rgb(215, 120, 60, 0.6));
            gc.fillOval(hx - 8, 115, 16, 20); // Hanging copper pans
        }
    }

    private void renderLivingRoomDecor(DrawContext gc) {
        // Striped cozy wallpaper
        gc.setFill(Color.rgb(186, 104, 200, 0.08));
        for (int x = 0; x < width; x += 35) {
            gc.fillRect(x, 40, 18, 480);
        }

        // Bookshelf silhouette on left
        gc.setFill(Color.rgb(93, 64, 55, 0.35));
        gc.fillRect(40, 80, 90, 360);
        gc.setFill(Color.rgb(123, 31, 162, 0.4));
        for (int shelf = 120; shelf < 420; shelf += 50) {
            gc.fillRect(45, shelf, 80, 6);
            for (int b = 0; b < 4; b++) {
                gc.fillRect(50 + b * 18, shelf - 28, 14, 28);
            }
        }

        // Framed painting in center
        gc.setStroke(Color.rgb(141, 110, 99, 0.7));
        gc.setLineWidth(4.0);
        gc.strokeRect(width / 2.0 - 60, 90, 120, 80);
        gc.setFill(Color.rgb(255, 243, 224, 0.6));
        gc.fillRect(width / 2.0 - 58, 92, 116, 76);
        gc.setFill(Color.rgb(255, 179, 0, 0.7));
        gc.fillOval(width / 2.0 - 15, 115, 30, 30); // Golden cat portrait in painting
    }

    private void renderBedroomDecor(DrawContext gc) {
        // Starry arched window in center
        double winX = width / 2.0 - 100;
        double winY = 60;
        gc.setFill(Color.rgb(26, 35, 126, 0.85));
        gc.fillRoundRect(winX, winY, 200, 240, 60, 60);

        // Crescent moon & stars inside window
        gc.setFill(Color.web("#FFF59D"));
        gc.fillOval(winX + 130, winY + 40, 36, 36);
        gc.setFill(Color.rgb(26, 35, 126, 0.85));
        gc.fillOval(winX + 120, winY + 36, 34, 34);

        // Window frame
        gc.setStroke(Color.rgb(197, 202, 233, 0.8));
        gc.setLineWidth(4.0);
        gc.strokeRoundRect(winX, winY, 200, 240, 60, 60);
        gc.strokeLine(winX, winY + 130, winX + 200, winY + 130);
        gc.strokeLine(winX + 100, winY, winX + 100, winY + 240);

        // Twinkling stars
        for (int i = 0; i < 6; i++) {
            double sx = winX + 25 + (i * 28) % 150;
            double sy = winY + 30 + (i * 37) % 90;
            double alpha = 0.4 + Math.sin(globalAnimTimer * 4.0 + i) * 0.4;
            gc.setFill(Color.rgb(255, 255, 255, alpha));
            gc.fillOval(sx, sy, 3, 3);
        }
    }

    private void renderGardenDecor(DrawContext gc) {
        // Drifting clouds
        gc.setFill(Color.rgb(255, 255, 255, 0.5));
        for (int i = 0; i < 4; i++) {
            double cx = (i * 260.0 + globalAnimTimer * 12.0) % (width + 160) - 80;
            double cy = 60.0 + (i * 35.0);
            gc.fillOval(cx, cy, 90, 35);
            gc.fillOval(cx + 25, cy - 15, 60, 40);
        }

        // Distant trees & wooden picket fence above floor
        double floorY = height - 40;
        gc.setFill(Color.rgb(46, 125, 50, 0.25));
        for (int x = -20; x < width + 40; x += 90) {
            gc.fillOval(x, floorY - 110, 110, 110);
        }

        // Picket fence
        gc.setFill(Color.rgb(245, 245, 245, 0.65));
        gc.fillRect(0, floorY - 35, width, 6);
        for (int fx = 15; fx < width; fx += 35) {
            gc.fillPolygon(new double[]{fx, fx + 8, fx + 16, fx + 16, fx},
                    new double[]{floorY - 50, floorY - 60, floorY - 50, floorY, floorY}, 5);
        }
    }

    private void renderRooftopDecor(DrawContext gc) {
        // Night sky with big glowing moon
        gc.setFill(Color.rgb(255, 245, 157, 0.9));
        gc.fillOval(width - 160, 60, 64, 64);
        gc.setFill(Color.rgb(255, 245, 157, 0.2));
        gc.fillOval(width - 170, 50, 84, 84);

        // Distant skyscraper skyline
        gc.setFill(Color.rgb(18, 18, 30, 0.85));
        double[] bldgWidths = {65, 80, 55, 90, 70, 85, 60, 100, 75, 90, 70, 80};
        double bx = 0;
        int idx = 0;
        while (bx < width) {
            double bw = bldgWidths[idx % bldgWidths.length];
            double bh = 140.0 + ((idx * 37) % 180);
            double by = height - 40 - bh;
            gc.fillRect(bx, by, bw, bh);

            // Glowing yellow window dots
            gc.setFill(Color.rgb(255, 214, 0, 0.6));
            for (double wy = by + 15; wy < height - 55; wy += 22) {
                for (double wx = bx + 8; wx < bx + bw - 10; wx += 14) {
                    if ((int)(wx + wy) % 3 != 0) {
                        gc.fillRect(wx, wy, 6, 8);
                    }
                }
            }
            gc.setFill(Color.rgb(18, 18, 30, 0.85));
            bx += bw + 8;
            idx++;
        }
    }

    /* =========================================================================
     * PLATFORMS RENDERING
     * ========================================================================= */
    private void renderPlatforms(DrawContext gc, List<Platform> platforms, Theme theme) {
        for (Platform p : platforms) {
            if (p.isCollapsed()) {
                if (p.getBreakAnimationTimer() > 0.0) {
                    double progress = p.getBreakAnimationTimer() / 0.42;
                    gc.save();
                    gc.setGlobalAlpha(progress);
                    gc.setFill(theme.getPlatformColor().deriveColor(0, 0.9, 0.9, 1));
                    double cx = p.getX() + p.getWidth() / 2.0;
                    double cy = p.getY() + 5.0;
                    for (int i = 0; i < 5; i++) {
                        double dx = (i - 2) * p.getWidth() * 0.11;
                        double dy = (1.0 - progress) * (12 + (i % 3) * 5);
                        gc.fillRoundRect(cx + dx - 5, cy + dy, 10, 5, 2, 2);
                    }
                    gc.restore();
                }
                continue;
            }

            double px = p.getX() + p.getShakeOffset();
            double py = p.getY();
            double pw = p.getWidth();
            double ph = p.getHeight();

            if (p.getType() == com.dodgingcat.game.PlatformType.SOLID_FLOOR) {
                String backgroundAsset = "/assets/images/backgrounds/" + theme.getId() + ".png";
                if (AssetManager.getImage(backgroundAsset) != null) continue;
                if (AssetManager.drawImage(gc, "/assets/images/platforms/floor.png", px, py, pw, ph)) continue;
                // Ground Floor
                gc.setFill(theme.getFloorColor());
                gc.fillRect(px, py, pw, ph);

                // Distinct thematic floor top boundary
                if (theme == Theme.GARDEN) {
                    gc.setFill(Color.web("#4CAF50"));
                    gc.fillRect(px, py, pw, 7); // Fresh green grass
                    for (int x = 5; x < pw; x += 15) {
                        gc.fillPolygon(new double[]{x, x + 3, x + 6}, new double[]{py, py - 6, py}, 3);
                    }
                } else if (theme == Theme.ROOFTOP) {
                    gc.setFill(Color.web("#BF360C"));
                    gc.fillRect(px, py, pw, 6); // Terracotta chimney edge
                } else {
                    gc.setFill(theme.getAccentColor().deriveColor(0, 1, 1, 0.5));
                    gc.fillRect(px, py, pw, 5);
                }
            } else {
                String platformAsset = "/assets/images/platforms/" + theme.getId() + "_platform.png";
                boolean emphasize = theme == Theme.KITCHEN || theme == Theme.LIVING_ROOM;
                if (emphasize) {
                    gc.setFill(Color.rgb(16, 19, 27, 0.34));
                    gc.fillRoundRect(px + 1, py + ph * 0.18, Math.max(0, pw - 2), ph * 0.84 + 2, 8, 8);
                }
                boolean drawn = theme == Theme.KITCHEN
                        ? AssetManager.drawImageRegion(gc, "/assets/images/platforms/kitchen_counter.png",
                                0, 295, 2172, 145, px, py - 2, pw, ph + 4)
                        : AssetManager.drawImageContent(gc, platformAsset, px, py - 2, pw, ph + 4);
                if (drawn) {
                    if (emphasize) {
                        gc.save();
                        gc.setStroke(theme == Theme.KITCHEN
                                ? Color.rgb(91, 43, 27, 0.82) : Color.rgb(17, 37, 53, 0.84));
                        gc.setLineWidth(1.6);
                        gc.strokeRoundRect(px + 0.8, py - 1, Math.max(0, pw - 1.6), ph + 2, 8, 8);
                        gc.setStroke(Color.rgb(255, 237, 198, 0.58));
                        gc.setLineWidth(1.0);
                        gc.strokeLine(px + 10, py + 1, px + pw - 10, py + 1);
                        gc.restore();
                    }
                    if (p.isWarning()) {
                        gc.setStroke(Color.web("#D84315"));
                        gc.setLineWidth(2.0);
                        gc.strokeLine(px + pw * 0.48, py + 1, px + pw * 0.43, py + ph - 2);
                        gc.strokeLine(px + pw * 0.43, py + ph - 2, px + pw * 0.52, py + ph - 5);
                    }
                    continue;
                }
                // Elevated platform
                Color baseCol = p.isWarning() ? Color.web("#FF5722") : theme.getPlatformColor();
                gc.setFill(baseCol);
                gc.fillRoundRect(px, py, pw, ph, 8, 8);

                // Top highlight
                gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.45));
                gc.fillRoundRect(px + 2, py + 1, pw - 4, 3, 4, 4);

                if (p.getType() == com.dodgingcat.game.PlatformType.CRUMBLING) {
                    gc.setStroke(Color.web("#BF360C"));
                    gc.setLineWidth(1.5);
                    gc.strokeLine(px + pw * 0.25, py + 2, px + pw * 0.4, py + ph - 2);
                    gc.strokeLine(px + pw * 0.65, py + 2, px + pw * 0.78, py + ph - 2);
                }
            }
        }
    }

    /* =========================================================================
     * CAT SKINS RENDERING WITH RARITIES & ACCESSORIES (Point 11)
     * ========================================================================= */
    private void renderPlayers(DrawContext gc, List<Player> players, List<Platform> platforms, Theme theme) {
        for (Player player : players) {
            if (!player.isAlive()) {
                renderDefeatedPlayer(gc, player);
                continue;
            }

            double baseW = player.getSpriteWidth();
            double baseH = player.getSpriteHeight();
            double pw = baseW * CAT_RENDER_SCALE;
            double ph = baseH * CAT_RENDER_SCALE;
            double px = player.getX() - (pw - baseW) / 2.0;
            double py = player.getY() - (ph - baseH);

            // Garden platform art places the visible grass lip a couple of pixels above
            // the collision edge. Keep the whole rendered cat (and its shadow) on that
            // visual surface; the previous +9px offset lowered every cat into the grass.
            double visualYOffset = theme == Theme.GARDEN && player.isGrounded() ? 2.5 : 0.0;
            py += visualYOffset;

            // A contact shadow is only visible while the cat is standing on a surface.
            if (player.isGrounded()) {
                gc.setFill(Color.rgb(0, 0, 0, 0.24));
                gc.fillOval(px + 1, py + ph - 2, pw - 2, 5);
            }

            renderLegendaryMotionTrail(gc, player, px, py, pw, ph);

            if (player.getSkinId().equalsIgnoreCase("santa")) {
                renderSantaGiftBoxes(gc, px, py, pw, ph, false);
            }

            // The shield's rear glass and rear rim sit behind the cat.
            if (player.hasShield()) {
                renderShieldBack(gc, px, py, pw, ph);
            }

            // Invincibility is shown by translucency, distinct from the cyan shield.
            boolean catVisible = true;
            gc.save();
            if (player.isInvincible()) {
                double remaining = player.getInvincibilityTimer();
                boolean blinkPhase = remaining <= 1.5 && ((int) (globalAnimTimer * 12.0) % 2 == 0);
                if (blinkPhase) {
                    catVisible = false;
                } else {
                    gc.setGlobalAlpha(0.48);
                }
            }
            if (catVisible) renderAnimatedCatSkin(gc, player, false, visualYOffset);
            gc.restore();

            // Front glass, highlights and gift parcels wrap over the silhouette to
            // create depth without changing collision geometry.
            if (player.hasShield()) renderShieldFront(gc, px, py, pw, ph);
            if (player.getSkinId().equalsIgnoreCase("santa")) {
                renderSantaGiftBoxes(gc, px, py, pw, ph, true);
            }

            if (player.hasDoubleScore()) {
                gc.setFill(Color.web("#FFD700"));
                gc.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 14));
                gc.setTextAlign(TextAlignment.CENTER);
                ArtText.fillText(gc, "x2 score", px + pw / 2.0, py - 9);
            }
        }
    }

    private void renderShieldBack(DrawContext gc, double x, double y, double w, double h) {
        double bx = x - 7.0, by = y - 5.0, bw = w + 14.0, bh = h + 10.0;
        gc.save();
        gc.setFill(new LinearGradient(bx, by, bx + bw, by + bh, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(117, 239, 255, 0.05)),
                new Stop(0.48, Color.rgb(47, 209, 244, 0.13)),
                new Stop(1.0, Color.rgb(14, 127, 211, 0.08))));
        gc.fillOval(bx, by, bw, bh);
        gc.setStroke(Color.rgb(91, 229, 255, 0.62));
        gc.setLineWidth(1.4);
        gc.strokeOval(bx, by, bw, bh);
        gc.restore();
    }

    private void renderShieldFront(DrawContext gc, double x, double y, double w, double h) {
        double bx = x - 7.0, by = y - 5.0, bw = w + 14.0, bh = h + 10.0;
        gc.save();
        gc.setLineCap(StrokeLineCap.ROUND);
        // Bubble highlights sit along its upper surface, never under the cat.
        gc.setStroke(Color.rgb(150, 246, 255, 0.92));
        gc.setLineWidth(2.0);
        gc.strokeArc(bx + 1.5, by + 1.5, bw - 3.0, bh - 3.0, 24, 132, ArcType.OPEN);
        gc.setStroke(Color.rgb(231, 255, 255, 0.82));
        gc.setLineWidth(1.0);
        gc.strokeArc(bx + 4.0, by + 3.0, bw - 8.0, bh - 6.0, 42, 92, ArcType.OPEN);
        gc.setFill(Color.rgb(241, 255, 255, 0.92));
        gc.fillOval(bx + bw * 0.24, by + bh * 0.12, 3.2, 5.0);
        gc.setFill(Color.rgb(160, 244, 255, 0.75));
        gc.fillOval(bx + bw * 0.76, by + bh * 0.25, 2.2, 3.0);
        double glint = (globalAnimTimer * 0.34) % 1.0;
        double angle = Math.PI * (0.08 + glint * 0.84);
        double gx = x + w / 2.0 + Math.cos(angle) * bw * 0.47;
        double gy = y + h / 2.0 - Math.sin(angle) * bh * 0.47;
        gc.fillOval(gx - 1.7, gy - 1.7, 3.4, 3.4);
        gc.restore();
    }

    private void renderSantaGiftBoxes(DrawContext gc, double x, double y, double w, double h,
                                      boolean frontLayer) {
        // Four parcels follow staggered, tilted ellipses. Depth decides whether each
        // parcel passes behind or in front of the cat, with perspective size changes.
        for (int i = 0; i < 4; i++) {
            double phase = globalAnimTimer * (1.15 + i * 0.09) + i * Math.PI * 0.72;
            double depth = Math.cos(phase);
            if ((depth >= 0.0) != frontLayer) continue;
            double radiusX = w * (0.43 + (i % 2) * 0.07);
            double radiusY = h * (0.39 + ((i + 1) % 2) * 0.06);
            double gx = x + w * 0.5 + Math.sin(phase) * radiusX;
            double gy = y + h * 0.50 + depth * radiusY + Math.sin(phase * 1.7 + i) * 1.4;
            double perspective = 0.70 + (depth + 1.0) * 0.22;
            double size = (6.8 + (i % 2) * 1.2) * perspective;
            double alpha = 0.58 + (depth + 1.0) * 0.18;
            drawGiftBox(gc, gx, gy, size, alpha);
            if (frontLayer && depth > 0.48) {
                // Tiny ground-facing glint makes foreground gifts read as nearby objects.
                gc.setFill(Color.rgb(255, 225, 154, 0.42));
                gc.fillOval(gx - size * 0.42, gy + size * 0.43, size * 0.84, 1.2);
            }
        }
    }

    private void renderLegendaryMotionTrail(DrawContext gc, Player player,
                                            double x, double y, double w, double h) {
        String skin = player.getSkinId().toLowerCase();
        if (!skin.equals("cyber") && !skin.equals("ghost") && !skin.equals("santa")) return;

        if (skin.equals("cyber")) {
            boolean grounded = player.isGrounded();
            Boolean wasGrounded = cyberGroundedState.put(player.getId(), grounded);
            if (wasGrounded != null && wasGrounded != grounded) {
                cyberImpactFlashUntil.put(player.getId(), globalAnimTimer + 0.12);
            }
        }

        double direction = player.getFacingDirection();
        double tailX = direction > 0 ? x - 2 : x + w + 2;
        boolean moving = Math.abs(player.getVx()) >= 35.0;
        if (!moving && (skin.equals("ghost") || skin.equals("santa"))) return;
        gc.save();
        if (skin.equals("cyber") && moving) {
            // Angular, stepped traces read as tiny circuit fragments trailing at speed.
            for (int i = 0; i < 3; i++) {
                double phase = (globalAnimTimer * 4.2 + i * 0.34) % 1.0;
                double fragmentX = tailX - direction * (phase * 18.0 + i * 5.0);
                double fragmentY = y + h * (0.37 + i * 0.085)
                        + Math.sin(globalAnimTimer * 7.0 + i) * 1.2;
                double length = 10.0 + (1.0 - phase) * 5.0;
                gc.setGlobalAlpha(0.92 * (1.0 - phase * 0.30));
                gc.setStroke(i == 1 ? Color.WHITE : Color.web("#00E5FF"));
                gc.setLineWidth(2.2);
                gc.beginPath();
                gc.moveTo(fragmentX, fragmentY);
                gc.lineTo(fragmentX - direction * length * 0.55, fragmentY);
                gc.lineTo(fragmentX - direction * length * 0.55, fragmentY + 3.0);
                gc.lineTo(fragmentX - direction * length, fragmentY + 3.0);
                gc.stroke();
                gc.setFill(Color.web("#8AF8FF"));
                gc.fillRect(fragmentX - direction * length - 1.5, fragmentY + 1.0, 4.0, 4.0);
            }
        }

        if (skin.equals("ghost") && moving) {
            // A brighter, longer spectral acceleration trail made of crisp lights.
            gc.setGlobalAlpha(0.76);
            gc.setStroke(Color.rgb(157, 235, 255, 0.78));
            gc.setLineWidth(3.0);
            gc.beginPath();
            gc.moveTo(tailX, y + h * 0.51);
            gc.quadraticCurveTo(tailX - direction * 16.0, y + h * 0.42,
                    tailX - direction * 34.0, y + h * 0.53);
            gc.stroke();
            gc.setStroke(Color.rgb(245, 255, 255, 0.92));
            gc.setLineWidth(1.1);
            gc.beginPath();
            gc.moveTo(tailX - direction * 2.0, y + h * 0.51);
            gc.quadraticCurveTo(tailX - direction * 16.0, y + h * 0.44,
                    tailX - direction * 32.0, y + h * 0.53);
            gc.stroke();
            for (int i = 0; i < 9; i++) {
                double age = (globalAnimTimer * 2.5 + i * 0.112) % 1.0;
                double moteX = tailX - direction * (3.0 + age * 34.0);
                double moteY = y + h * 0.52
                        + Math.sin(globalAnimTimer * 5.0 + i * 1.3) * 4.0;
                double size = 6.2 * (1.0 - age * 0.38);
                gc.setGlobalAlpha(0.98 * (1.0 - age * 0.35));
                gc.setFill(i % 2 == 0 ? Color.web("#E9FDFF") : Color.web("#A8F3FF"));
                gc.fillOval(moteX - size / 2.0, moteY - size / 2.0, size, size);
                gc.setStroke(Color.rgb(235, 253, 255, 0.92));
                gc.setLineWidth(1.1);
                gc.strokeLine(moteX - size * 0.75, moteY, moteX + size * 0.75, moteY);
                gc.strokeLine(moteX, moteY - size * 0.75, moteX, moteY + size * 0.75);
            }
        }

        if (skin.equals("santa") && moving) {
            // Dense but small falling flakes accelerate and spread behind the tail.
            double speed = Math.min(1.0, Math.abs(player.getVx()) / 260.0);
            double trailLength = 24.0 + speed * 34.0;
            gc.setGlobalAlpha(0.78);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.0);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.beginPath();
            for (int i = 0; i < 12; i++) {
                double seed = i * 17.173 + 4.27;
                double randomX = fract(Math.sin(seed * 12.9898) * 43758.5453) - 0.5;
                double randomY = fract(Math.sin(seed * 78.233) * 19341.172);
                double randomSize = fract(Math.sin(seed * 39.425) * 9812.331);
                double fallSpeed = 0.85 + (i % 5) * 0.12;
                double age = (globalAnimTimer * fallSpeed + i * 0.173) % 1.0;
                double flakeX = tailX - direction * (3.0 + age * trailLength) + randomX * 12.0
                        + Math.sin(globalAnimTimer * 2.2 + seed) * 2.0;
                double flakeY = y + h * (0.39 + randomY * 0.15)
                        + age * age * (h * 0.30 + 12.0);
                double size = 2.6 + randomSize * 2.0;
                double arm = size * (1.0 - age * 0.22);
                for (int axis = 0; axis < 3; axis++) {
                    double angle = axis * Math.PI / 3.0;
                    double dx = Math.cos(angle) * arm, dy = Math.sin(angle) * arm;
                    gc.moveTo(flakeX - dx, flakeY - dy);
                    gc.lineTo(flakeX + dx, flakeY + dy);
                }
            }
            gc.stroke();
        }

        // A brief, non-colliding pixel burst at takeoff and landing.
        double flashUntil = cyberImpactFlashUntil.getOrDefault(player.getId(), 0.0);
        if (globalAnimTimer < flashUntil) {
            double fade = (flashUntil - globalAnimTimer) / 0.12;
            gc.setGlobalAlpha(fade);
            gc.setFill(Color.web("#7CF7FF"));
            for (int i = 0; i < 4; i++) {
                double side = (i % 2 == 0) ? 1.0 : -1.0;
                double px = x + w * (i < 2 ? 0.32 : 0.68) + side * (4.0 + (1.0 - fade) * 5.0);
                double py = y + h * 0.86 - (1.0 - fade) * 5.0;
                gc.fillRect(px, py, 3.0, 3.0);
            }
        }
        gc.restore();
    }

    private void renderAnimatedCatSkin(DrawContext gc, Player player) {
        renderAnimatedCatSkin(gc, player, false, 0.0);
    }

    private void renderAnimatedCatSkin(DrawContext gc, Player player, boolean idlePreview) {
        renderAnimatedCatSkin(gc, player, idlePreview, 0.0);
    }

    private void renderAnimatedCatSkin(DrawContext gc, Player player, boolean idlePreview,
                                       double visualYOffset) {
        double baseW = player.getSpriteWidth();
        double baseH = player.getSpriteHeight();
        double pw = baseW * CAT_RENDER_SCALE;
        double ph = baseH * CAT_RENDER_SCALE;
        double px = player.getX() - (pw - baseW) / 2.0;
        double py = player.getY() + visualYOffset - (ph - baseH);
        int dir = player.getFacingDirection();
        String skin = player.getSkinId().toLowerCase();
        // Animation timing

        int walkFrame = !idlePreview && player.isGrounded() && Math.abs(player.getVx()) > 10.0
                ? (int) (player.getWalkAnimTime() / 0.15) % 6
                : 0;
        if (AssetManager.getImage("/assets/images/characters/" + skin + "_sheet.png") != null) {
            int row;
            int frame;
            if (idlePreview) {
                row = 0;
                frame = (int) (globalAnimTimer * 2.5) % 6;
            } else if (!player.isGrounded()) {
                if (player.getVy() < -35.0) {
                    row = 2;
                    frame = Math.clamp((int) (((player.getVy() + 520.0) / 485.0) * 5.0), 0, 5);
                } else {
                    row = 3;
                    frame = (int) (globalAnimTimer * 5.5) % 6;
                }
            } else if (Math.abs(player.getVx()) > 10.0) {
                row = 1;
                frame = walkFrame;
            } else {
                row = 0;
                frame = (int) (globalAnimTimer * 2.5) % 6;
            }
            gc.save();
            if (dir < 0) {
                gc.translate(px * 2.0 + pw, 0);
                gc.scale(-1, 1);
            }
            String sheetPath = "/assets/images/characters/" + skin + "_sheet.png";
            boolean groundedPose = idlePreview || (player.isGrounded() && row <= 1);
            gc.save();
            // All cat atlases now share an exact 6x4 transparent grid with
            // safe gutters. Ground rows use the same per-frame foot baseline;
            // airborne rows are cropped strictly to their own cell.
            boolean drawn = groundedPose
                    ? AssetManager.drawSpriteFrameBottomAligned(gc, sheetPath, 6, 4, row * 6 + frame,
                            px - 6, py - 8, pw + 12, ph + 8, false)
                    : AssetManager.drawSpriteFrame(gc, sheetPath, 6, 4, row * 6 + frame, 1.0,
                            px - 6, py - 8, pw + 12, ph + 8);
            gc.restore();
            gc.restore();
            if (drawn) {
                if (idlePreview && skin.equals("santa")) {
                    renderSantaGiftBoxes(gc, px - 6, py - 8, pw + 12, ph + 8, true);
                }
                renderSkinSignatureEffect(gc, player, skin, px - 6, py - 8, pw + 12, ph + 8);
                return;
            }
        }
        // Wait for the sprite sheet to finish loading; never show a procedural cat.
    }

    private void renderSkinSignatureEffect(DrawContext gc, Player player, String skin,
                                           double x, double y, double w, double h) {
        double t = globalAnimTimer;
        double rearX = player.getFacingDirection() > 0 ? x + 2 : x + w - 2;
        double step = player.getFacingDirection() > 0 ? -1.0 : 1.0;
        gc.save();
        gc.setLineCap(StrokeLineCap.ROUND);
        switch (skin) {
            case "lava" -> {
                gc.setGlobalAlpha(0.68);
                for (int i = 0; i < 3; i++) {
                    double lift = Math.sin(t * 5.0 + i * 2.1) * 4.0;
                    gc.setFill(i % 2 == 0 ? Color.web("#FFB52E") : Color.web("#FF5722"));
                    gc.fillOval(rearX + step * (5 + i * 5), y + h * 0.56 - i * 3 + lift,
                            3.2 - i * 0.45, 3.2 - i * 0.45);
                }
            }
            case "cyber" -> {
                double pulse = 0.84 + (Math.sin(t * 4.0) + 1.0) * 0.08;
                gc.setGlobalAlpha(pulse);
                gc.setStroke(Color.web("#00E5FF"));
                gc.setLineWidth(2.0);
                // Angular circuit traces with square contact pads, kept close to the coat.
                gc.beginPath();
                gc.moveTo(x + w * 0.25, y + h * 0.54);
                gc.lineTo(x + w * 0.39, y + h * 0.54);
                gc.lineTo(x + w * 0.39, y + h * 0.42);
                gc.lineTo(x + w * 0.67, y + h * 0.42);
                gc.lineTo(x + w * 0.67, y + h * 0.56);
                gc.moveTo(x + w * 0.34, y + h * 0.70);
                gc.lineTo(x + w * 0.34, y + h * 0.64);
                gc.lineTo(x + w * 0.55, y + h * 0.64);
                gc.lineTo(x + w * 0.55, y + h * 0.72);
                gc.stroke();
                gc.setFill(Color.web("#7DF7FF"));
                gc.fillRect(x + w * 0.23, y + h * 0.52, 4, 4);
                gc.fillRect(x + w * 0.65, y + h * 0.40, 4, 4);
                gc.fillRect(x + w * 0.53, y + h * 0.70, 4, 4);
                // A bright current pulse travels along the top circuit without blurring the sprite.
                double current = (t * 0.72) % 1.0;
                double currentX = x + w * (0.40 + current * 0.24);
                gc.setFill(Color.WHITE);
                gc.fillRoundRect(currentX - 2.0, y + h * 0.405, 4.0, 4.0, 1.5, 1.5);
            }
            case "ghost" -> {
                // Nine stable pseudo-random points distributed through an ellipse
                // around Spirit. They twinkle in place rather than tracing its edge.
                double cx = x + w * 0.5;
                double cy = y + h * 0.52;
                double seed = player.getId() * 31.73;
                for (int i = 0; i < 9; i++) {
                    double radial = Math.sqrt(fract(Math.sin((i + 1) * 91.17 + seed) * 43758.5453));
                    double angle = fract(Math.sin((i + 1) * 47.31 + seed * 1.7) * 24634.6345)
                            * Math.PI * 2.0;
                    double sx = cx + Math.cos(angle) * w * 0.56 * radial;
                    double sy = cy + Math.sin(angle) * h * 0.58 * radial;
                    double pulsePhase = fract(Math.sin((i + 1) * 17.91 + seed * 0.7) * 13579.246);
                    double pulse = (Math.sin(t * (3.4 + pulsePhase * 2.0) + pulsePhase * Math.PI * 2.0) + 1.0) * 0.5;
                    double radius = 1.5 + pulse * 1.4;
                    gc.setGlobalAlpha(0.30 + pulse * 0.66);
                    gc.setFill(i % 2 == 0 ? Color.web("#F8FFFF") : Color.web("#BCEFFF"));
                    gc.fillOval(sx - radius, sy - radius, radius * 2.0, radius * 2.0);
                    if (pulse > 0.68) {
                        gc.setStroke(Color.rgb(245, 255, 255, 0.92));
                        gc.setLineWidth(0.9);
                        gc.strokeLine(sx - radius - 1.1, sy, sx + radius + 1.1, sy);
                        gc.strokeLine(sx, sy - radius - 1.1, sx, sy + radius + 1.1);
                    }
                }
            }
            case "santa" -> { /* Gifts are layered around the cat in renderSantaGiftBoxes. */ }
            case "super" -> {
                gc.setGlobalAlpha(Math.abs(player.getVx()) > 10.0 || !player.isGrounded() ? 0.68 : 0.38);
                gc.setStroke(Color.web("#E94745"));
                gc.setLineWidth(2.2);
                gc.beginPath();
                gc.moveTo(rearX, y + h * 0.36);
                gc.quadraticCurveTo(rearX + step * 8, y + h * 0.55 + Math.sin(t * 8.0) * 3,
                        rearX + step * (Math.abs(player.getVx()) > 10.0 ? 17 : 10), y + h * 0.86);
                gc.stroke();
                gc.setFill(Color.web("#FFD56F"));
                gc.fillOval(rearX + step * (Math.abs(player.getVx()) > 10.0 ? 15 : 8), y + h * 0.80, 2.8, 2.8);
            }
            case "pirate" -> {
                double gleam = 0.35 + (Math.sin(t * 5.5) + 1.0) * 0.22;
                double sx = x + w * (player.getFacingDirection() > 0 ? 0.66 : 0.34);
                double sy = y + h * 0.18;
                gc.setGlobalAlpha(gleam);
                gc.setStroke(Color.web("#FFE7A8"));
                gc.setLineWidth(1.0);
                gc.strokeLine(sx - 3, sy, sx + 3, sy);
                gc.strokeLine(sx, sy - 3, sx, sy + 3);
                gc.setFill(Color.web("#FFD56F"));
                gc.fillOval(sx - 1.0, sy - 1.0, 2.0, 2.0);
            }
            default -> { }
        }
        gc.restore();
    }

    /** Six-armed snowflake with small forked branches at each arm tip. */
    private void drawSnowflake(DrawContext gc, double cx, double cy, double radius, double alpha) {
        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setStroke(Color.WHITE);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int axis = 0; axis < 3; axis++) {
            double angle = axis * Math.PI / 3.0;
            double ux = Math.cos(angle), uy = Math.sin(angle);
            double vx = -uy, vy = ux;
            gc.moveTo(cx - ux * radius, cy - uy * radius);
            gc.lineTo(cx + ux * radius, cy + uy * radius);
            for (int side = -1; side <= 1; side += 2) {
                double bx = cx + ux * radius * 0.58 * side;
                double by = cy + uy * radius * 0.58 * side;
                double branch = radius * 0.35;
                for (int fork = -1; fork <= 1; fork += 2) {
                    gc.moveTo(bx, by);
                    gc.lineTo(bx + ux * branch * side + vx * branch * fork,
                            by + uy * branch * side + vy * branch * fork);
                }
            }
        }
        gc.stroke();
        gc.restore();
    }

    private void drawGiftBox(DrawContext gc, double cx, double cy, double size, double alpha) {
        gc.save();
        gc.setGlobalAlpha(alpha);
        double left = cx - size / 2.0;
        double top = cy - size * 0.32;
        gc.setFill(Color.web("#D83A46"));
        gc.fillRoundRect(left, top, size, size * 0.72, 1.5, 1.5);
        gc.setStroke(Color.web("#FFE6A0"));
        gc.setLineWidth(0.8);
        gc.strokeRoundRect(left, top, size, size * 0.72, 1.5, 1.5);
        gc.setFill(Color.web("#FFD36A"));
        gc.fillRect(cx - 0.8, top, 1.6, size * 0.72);
        gc.fillRect(left, top + size * 0.26, size, 1.4);
        gc.fillOval(cx - 3.0, top - 2.5, 3.0, 2.0);
        gc.fillOval(cx, top - 2.5, 3.0, 2.0);
        gc.restore();
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private void renderDefeatedPlayer(DrawContext gc, Player player) {
        double px = player.getX();
        double py = player.getY();
        double sx = px + 3.0;
        double sy = py + 7.0;
        double bob = Math.sin(globalAnimTimer * 3.0) * 3.0;
        gc.save();

        // Shared grave marker at the exact position where this player fell.
        gc.setFill(Color.rgb(0, 0, 0, 0.24));
        gc.fillOval(sx - 1, sy + 31, 34, 5);
        gc.beginPath();
        gc.moveTo(sx + 2, sy + 31);
        gc.lineTo(sx + 2, sy + 13);
        gc.quadraticCurveTo(sx + 2, sy + 2, sx + 17, sy + 2);
        gc.quadraticCurveTo(sx + 32, sy + 2, sx + 32, sy + 13);
        gc.lineTo(sx + 32, sy + 31);
        gc.closePath();
        gc.setFill(new LinearGradient(sx, sy, sx + 34, sy + 30, false,
                CycleMethod.NO_CYCLE, new Stop(0, Color.web("#71828A")),
                new Stop(0.55, Color.web("#4C5A62")), new Stop(1, Color.web("#303A42"))));
        gc.fill();
        gc.setStroke(Color.web("#C4A878"));
        gc.setLineWidth(1.4);
        gc.stroke();
        gc.setStroke(Color.rgb(231, 220, 190, 0.34));
        gc.setLineWidth(0.8);
        gc.strokeRoundRect(sx + 5, sy + 7, 24, 20, 7, 7);
        gc.setFill(Color.web("#E8D7B7"));
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 7));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, "RIP", sx + 17, sy + 19);
        gc.setStroke(Color.rgb(27, 37, 43, 0.65));
        gc.setLineWidth(1.3);
        gc.strokeLine(sx + 11, sy + 22, sx + 9, sy + 26);
        gc.strokeLine(sx + 23, sy + 22, sx + 25, sy + 26);

        // A small shared spirit floats beside the marker regardless of the chosen skin.
        double gx = sx + 23.0;
        double gy = sy - 19.0 + bob;
        gc.setGlobalAlpha(0.88);
        gc.beginPath();
        gc.moveTo(gx, gy + 16);
        gc.lineTo(gx, gy + 8);
        gc.quadraticCurveTo(gx, gy, gx + 8, gy);
        gc.quadraticCurveTo(gx + 16, gy, gx + 16, gy + 8);
        gc.lineTo(gx + 16, gy + 16);
        gc.quadraticCurveTo(gx + 13, gy + 13, gx + 11, gy + 16);
        gc.quadraticCurveTo(gx + 8, gy + 13, gx + 5, gy + 16);
        gc.quadraticCurveTo(gx + 2, gy + 13, gx, gy + 16);
        gc.closePath();
        gc.setFill(new LinearGradient(gx, gy, gx + 16, gy + 16, false,
                CycleMethod.NO_CYCLE, new Stop(0, Color.rgb(231, 252, 255, 0.98)),
                new Stop(1, Color.rgb(149, 220, 237, 0.78))));
        gc.fill();
        gc.setStroke(Color.rgb(239, 255, 255, 0.94));
        gc.setLineWidth(1.0);
        gc.stroke();
        gc.setFill(Color.web("#344954"));
        gc.fillOval(gx + 4, gy + 6, 1.8, 2.6);
        gc.fillOval(gx + 10, gy + 6, 1.8, 2.6);
        gc.setGlobalAlpha(0.70);
        gc.setStroke(Color.web("#C9F8FF"));
        gc.setLineWidth(1.1);
        gc.strokeArc(gx - 4, gy - 4, 24, 24, globalAnimTimer * 40.0, 105.0, ArcType.OPEN);
        gc.restore();
    }

    /* =========================================================================
     * HAZARDS RENDERING (Point 10)
     * ========================================================================= */
    private void renderHazards(DrawContext gc, List<Hazard> hazards) {
        for (Hazard h : hazards) {
            if (!h.isActive()) continue;

            if (h instanceof TopDownHazard td) {
                renderTopDownHazard(gc, td);
            } else if (h instanceof LaserHazard lh) {
                renderLaserHazard(gc, lh);
            } else if (h instanceof CannonHazard ch) {
                renderCannonHazard(gc, ch);
            } else if (h instanceof OrbitalHazard oh) {
                renderOrbitalHazard(gc, oh);
            }
        }
    }

    private void renderLaserHazard(DrawContext gc, LaserHazard lh) {
        boolean horiz = lh.getOrientation() == HazardOrientation.HORIZONTAL;
        double pos = lh.getLinePos();
        double thick = lh.getBeamThickness();

        if (lh.isWarning()) {
            double alpha = 0.35 + Math.sin(globalAnimTimer * 22.0) * 0.35;
            gc.setStroke(Color.rgb(255, 23, 68, alpha));
            gc.setLineWidth(2.5);
            gc.setLineDashes(8.0, 6.0);

            if (horiz) {
                gc.strokeLine(0, pos, width, pos);
                renderWarningTriangle(gc, 42, Math.max(24, pos - 25), 36);
                renderWarningTriangle(gc, width - 42, Math.max(24, pos - 25), 36);
            } else {
                gc.strokeLine(pos, 49, pos, height);
                renderWarningTriangle(gc, Math.min(width - 24, pos + 26), 70, 36);
                renderWarningTriangle(gc, Math.min(width - 24, pos + 26), height - 42, 36);
            }
            gc.setLineDashes(null);
        } else if (lh.isFiring()) {
            // Lethal Beam
            if (horiz) {
                if (!AssetManager.drawImage(gc, "/assets/images/hazards/laser_beam_horizontal.png", 0, pos - thick / 2.0, width, thick)) {
                    gc.setFill(Color.rgb(255, 23, 68, 0.85));
                    gc.fillRect(0, pos - thick / 2.0, width, thick);
                    gc.setFill(Color.WHITE);
                    gc.fillRect(0, pos - thick * 0.2, width, thick * 0.4);
                }
            } else {
                if (!AssetManager.drawImage(gc, "/assets/images/hazards/laser_beam_vertical.png", pos - thick / 2.0, 0, thick, height)) {
                    gc.setFill(Color.rgb(255, 23, 68, 0.85));
                    gc.fillRect(pos - thick / 2.0, 0, thick, height);
                    gc.setFill(Color.WHITE);
                    gc.fillRect(pos - thick * 0.2, 0, thick * 0.4, height);
                }
            }
        }
    }

    private void renderCannonHazard(DrawContext gc, CannonHazard ch) {
        boolean horiz = ch.getOrientation() == HazardOrientation.HORIZONTAL;
        double cal = ch.getCaliber();

        if (ch.isWarning()) {
            double alpha = 0.35 + Math.sin(globalAnimTimer * 20.0) * 0.35;
            gc.setStroke(Color.rgb(255, 109, 0, alpha));
            gc.setLineWidth(2.0);
            gc.setLineDashes(6.0, 6.0);

            if (horiz) {
                double y = ch.getY() + cal / 2.0;
                gc.strokeLine(0, y, width, y);
                double entryX = ch.isPositiveDirection() ? 42 : width - 42;
                renderWarningTriangle(gc, entryX, Math.max(68, y - 25), 38);
            } else {
                double x = ch.getX() + cal / 2.0;
                gc.strokeLine(x, 49, x, height);
                double entryY = ch.isPositiveDirection() ? 70 : height - 42;
                renderWarningTriangle(gc, Math.min(width - 24, x + 28), entryY, 38);
            }
            gc.setLineDashes(null);
        } else if (ch.isFiring()) {
            // Flying Cannonball
            if (!AssetManager.drawImage(gc, "/assets/images/hazards/cannonball.png", ch.getX(), ch.getY(), cal, cal)) {
                gc.setFill(Color.web("#263238"));
                gc.fillOval(ch.getX(), ch.getY(), cal, cal);
                // Specular sheen
                gc.setFill(Color.web("#78909C"));
                gc.fillOval(ch.getX() + cal * 0.25, ch.getY() + cal * 0.25, cal * 0.4, cal * 0.4);
                // Fiery spark
                gc.setFill(Color.web("#FF6D00"));
                gc.fillOval(ch.getX() + cal * 0.35, ch.getY() + cal * 0.35, cal * 0.3, cal * 0.3);
            }
        }
    }

    private void renderWarningTriangle(DrawContext gc, double centerX, double centerY, double size) {
        if (AssetManager.drawImage(gc, "/assets/images/hazards/warning_sign.png",
                centerX - size / 2.0, centerY - size / 2.0, size, size)) return;
        double half = size / 2.0;
        double[] xs = {centerX, centerX - half, centerX + half};
        double[] ys = {centerY - half, centerY + half, centerY + half};
        gc.save();
        gc.setFill(Color.rgb(255, 171, 0, 0.98));
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.2);
        gc.fillPolygon(xs, ys, 3);
        gc.strokePolygon(xs, ys, 3);
        gc.setFill(Color.web("#3A2200"));
        gc.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, size * 0.68));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, "!", centerX, centerY + size * 0.27);
        gc.restore();
    }

    private void renderOrbitalHazard(DrawContext gc, OrbitalHazard oh) {
        double ax = oh.getAnchorX();
        double ay = oh.getAnchorY();
        double wx = oh.getWeightCenterX();
        double wy = oh.getWeightCenterY();
        double r = oh.getWeightRadius();

        double dx = wx - ax;
        double dy = wy - ay;
        double chainLength = Math.hypot(dx, dy);
        double angle = Math.atan2(dy, dx);
        double visibleChainLength = Math.max(0.0, chainLength - r * 0.72);
        gc.save();
        gc.translate(ax, ay);
        gc.rotate(Math.toDegrees(angle) - 90.0);
        if (!AssetManager.drawImageContent(gc, "/assets/images/hazards/pendulum_chain.png",
                -6.5, 0.0, 13.0, visibleChainLength)) {
            gc.setStroke(Color.web("#8999A2"));
            gc.setLineWidth(5.0);
            gc.strokeLine(0.0, 0.0, 0.0, visibleChainLength);
        }
        gc.restore();

        gc.setFill(Color.web("#37474F"));
        gc.fillOval(ax - 5, ay - 5, 10, 10);

        gc.save();
        gc.translate(wx, wy);
        gc.rotate(Math.toDegrees(angle) - 90.0);
        if (!AssetManager.drawImage(gc, "/assets/images/hazards/pendulum.png", -r, -r, r * 2, r * 2)) {
            gc.setFill(Color.web("#78909C"));
            gc.fillOval(-r, -r, r * 2, r * 2);
        }
        gc.restore();
    }

    private void renderTopDownHazard(DrawContext gc, TopDownHazard td) {
        double x = td.getX();
        double y = td.getY();
        double w = td.getWidth();
        double h = td.getHeight();
        String sub = td.getSubType();

        gc.save();
        gc.translate(x + w / 2.0, y + h / 2.0);
        gc.rotate(td.getRotationAngle());

        HazardSprite sprite = hazardSprite(sub);
        boolean drawn = sprite != null
                ? AssetManager.drawSpriteFrameContent(gc, "/assets/images/hazards/" + sprite.sheet() + ".png",
                        2, 3, sprite.frame(), 10.0, -w / 2.0, -h / 2.0, w, h)
                : AssetManager.drawImageContent(gc, "/assets/images/hazards/" + sub + ".png",
                        -w / 2.0, -h / 2.0, w, h);
        if (drawn) {
            gc.restore();
            return;
        }

        switch (sub) {
            case "knife" -> {
                gc.setFill(Color.web("#CFD8DC"));
                gc.fillRect(-3, -h / 2.0, 6, h * 0.65);
                gc.setFill(Color.web("#5D4037"));
                gc.fillRect(-4, h * 0.15, 8, h * 0.35);
            }
            case "frying_pan" -> {
                gc.setFill(Color.web("#212121"));
                gc.fillOval(-w * 0.4, -h * 0.4, w * 0.8, h * 0.8);
                gc.fillRect(-3, h * 0.3, 6, h * 0.4);
            }
            case "rolling_pin" -> {
                gc.setFill(Color.web("#D7CCC8"));
                gc.fillRoundRect(-w / 2.0, -h * 0.25, w, h * 0.5, 4, 4);
                gc.setFill(Color.web("#8D6E63"));
                gc.fillRect(-w / 2.0 - 4, -h * 0.15, 4, h * 0.3);
                gc.fillRect(w / 2.0, -h * 0.15, 4, h * 0.3);
            }
            case "plant_pot" -> {
                gc.setFill(Color.web("#D84315"));
                gc.fillPolygon(new double[]{-w / 2.0, w / 2.0, w * 0.35, -w * 0.35},
                        new double[]{-h / 2.0 + 4, -h / 2.0 + 4, h / 2.0, h / 2.0}, 4);
                gc.setFill(Color.web("#2E7D32"));
                gc.fillOval(-w * 0.4, -h / 2.0 - 6, w * 0.8, 12);
            }
            case "remote_control" -> {
                gc.setFill(Color.web("#37474F"));
                gc.fillRoundRect(-w / 2.0, -h / 2.0, w, h, 4, 4);
                gc.setFill(Color.RED);
                gc.fillOval(-w * 0.3, -h * 0.35, 6, 6);
            }
            case "alarm_clock" -> {
                gc.setFill(Color.web("#C62828"));
                gc.fillOval(-w / 2.0, -h / 2.0, w, h);
                gc.setFill(Color.WHITE);
                gc.fillOval(-w * 0.35, -h * 0.35, w * 0.7, h * 0.7);
            }
            case "apple" -> {
                gc.setFill(Color.web("#E53935"));
                gc.fillOval(-w / 2.0, -h / 2.0, w, h);
                gc.setFill(Color.web("#2E7D32"));
                gc.fillOval(-3, -h / 2.0 - 4, 6, 8);
            }
            case "brick" -> {
                gc.setFill(Color.web("#C62828"));
                gc.fillRect(-w / 2.0, -h / 2.0, w, h);
                gc.setStroke(Color.web("#B71C1C"));
                gc.strokeRect(-w / 2.0, -h / 2.0, w, h);
            }
            default -> {
                gc.setFill(Color.web("#E64A19"));
                gc.fillRoundRect(-w / 2.0, -h / 2.0, w, h, 6, 6);
            }
        }
        gc.restore();
    }

    private HazardSprite hazardSprite(String subtype) {
        return switch (subtype) {
            case "knife" -> new HazardSprite("kitchen_hazards", 0);
            case "frying_pan" -> new HazardSprite("kitchen_hazards", 1);
            case "rolling_pin" -> new HazardSprite("kitchen_hazards", 2);
            case "salt_shaker" -> new HazardSprite("kitchen_hazards", 3);
            case "blender" -> new HazardSprite("kitchen_hazards", 4);
            case "plant_pot" -> new HazardSprite("living_room_hazards", 0);
            case "remote_control" -> new HazardSprite("living_room_hazards", 1);
            case "book" -> new HazardSprite("living_room_hazards", 2);
            case "cushion" -> new HazardSprite("living_room_hazards", 3);
            case "lamp" -> new HazardSprite("living_room_hazards", 4);
            case "alarm_clock" -> new HazardSprite("bedroom_hazards", 0);
            case "pillow" -> new HazardSprite("bedroom_hazards", 1);
            case "slipper" -> new HazardSprite("bedroom_hazards", 2);
            case "mirror_shard" -> new HazardSprite("bedroom_hazards", 3);
            case "toy_block" -> new HazardSprite("bedroom_hazards", 4);
            case "apple" -> new HazardSprite("garden_hazards", 0);
            case "bee_hive" -> new HazardSprite("garden_hazards", 1);
            case "trowel" -> new HazardSprite("garden_hazards", 2);
            case "watermelon" -> new HazardSprite("garden_hazards", 3);
            case "acorn" -> new HazardSprite("garden_hazards", 4);
            case "brick" -> new HazardSprite("rooftop_hazards", 0);
            case "satellite_dish" -> new HazardSprite("rooftop_hazards", 1);
            case "antenna" -> new HazardSprite("rooftop_hazards", 2);
            case "weather_vane" -> new HazardSprite("rooftop_hazards", 3);
            case "lightning" -> new HazardSprite("rooftop_hazards", 4);
            default -> null;
        };
    }

    private record HazardSprite(String sheet, int frame) {}

    /* =========================================================================
     * FISH CURRENCY & BUFFS RENDERING (Point 8 & 9)
     * ========================================================================= */
    private void renderFishItems(DrawContext gc, List<FishItem> fishes) {
        for (FishItem fi : fishes) {
            double x = fi.getX();
            double y = fi.getY();
            double w = fi.getWidth();
            double h = fi.getHeight();
            gc.save();
            double age = fi.getSpawnAge();
            double pop = age < 0.20 ? 0.72 + 0.28 * Math.sin(Math.min(1.0, age / 0.20) * Math.PI / 2.0) : 1.0;
            boolean blink = fi.getRemainingLifetime() < 1.2 && ((int)(globalAnimTimer * 10) % 2 == 0);
            gc.setGlobalAlpha(blink ? 0.38 : 1.0);
            gc.translate(x+w/2.0,y+h/2.0); gc.scale(pop,pop); gc.translate(-(x+w/2.0),-(y+h/2.0));
            String fishAsset = fi.getType() == com.dodgingcat.game.FishType.SINGLE_FISH
                    ? "/assets/images/currency/fish.png"
                    : "/assets/images/currency/fish_can.png";
            String skinTheme = fi.getCurrencySkinId() == null
                    ? "" : fi.getCurrencySkinId().toLowerCase(java.util.Locale.ROOT);

            Color edgeLight = switch (skinTheme) {
                case "ghost" -> Color.rgb(108, 232, 255, 0.92);
                case "santa" -> Color.rgb(255, 202, 91, 0.90);
                case "cyber" -> Color.rgb(0, 226, 255, 0.92);
                default -> Color.rgb(255, 212, 82, 0.88);
            };
            gc.save();
            double drawX = x - 5.0, drawY = y - 4.0;
            double drawW = w + 10.0, drawH = h + 8.0;
            // A low-cost flat tint keeps currency legible without invoking the
            // per-image convolution shadow shader on every frame.
            gc.setGlobalAlpha(0.18);
            gc.setFill(edgeLight);
            gc.fillOval(drawX, drawY, drawW, drawH);
            gc.setGlobalAlpha(blink ? 0.38 : 1.0);
            boolean drawn = renderLegendaryCurrencyAsset(gc, fi, skinTheme,
                    drawX, drawY, drawW, drawH)
                    || AssetManager.drawImageContent(gc, fishAsset, drawX, drawY, drawW, drawH);
            gc.restore();
            if (drawn) { gc.restore(); continue; }

            if (fi.getType() == com.dodgingcat.game.FishType.SINGLE_FISH) {
                // Golden Fish
                gc.setFill(Color.web("#FFB300"));
                gc.fillOval(drawX, drawY + 2, drawW * 0.75, drawH - 4);
                gc.fillPolygon(new double[]{drawX + drawW * 0.65, drawX + drawW, drawX + drawW},
                        new double[]{drawY + drawH / 2.0, drawY + 1, drawY + drawH - 1}, 3);
                gc.setFill(Color.BLACK);
                gc.fillOval(drawX + 4, drawY + 5, 2.5, 2.5); // eye
            } else {
                // Canned Tuna Box (Value 5)
                gc.setFill(Color.web("#B0BEC5"));
                gc.fillRoundRect(drawX, drawY, drawW, drawH, 6, 6);
                gc.setFill(Color.web("#00ACC1"));
                gc.fillRect(drawX + 2, drawY + 4, drawW - 4, drawH - 8);
                // Golden Fish icon on can
                gc.setFill(Color.web("#FFD54F"));
                gc.fillOval(drawX + 6, drawY + 7, drawW - 12, drawH - 14);
            }
            gc.restore();
        }
    }

    private boolean renderLegendaryCurrencyAsset(DrawContext gc, FishItem item, String skin,
                                                 double x, double y, double w, double h) {
        if (!skin.equals("ghost") && !skin.equals("santa") && !skin.equals("cyber")) return false;
        String denomination = item.getType() == com.dodgingcat.game.FishType.SINGLE_FISH
                ? "fish" : "can";
        String path = "/assets/images/currency/skins/" + skin + "_" + denomination + ".png";
        return AssetManager.drawImageContent(gc, path, x, y, w, h);
    }

    private void renderBuffs(DrawContext gc, List<Buff> buffs) {
        for (Buff b : buffs) {
            double x = b.getX();
            double y = b.getY();
            double w = b.getWidth();
            double h = b.getHeight();
            gc.save();
            double age = b.getSpawnAge();
            double pop = age < 0.20 ? 0.72 + 0.28 * Math.sin(Math.min(1.0, age / 0.20) * Math.PI / 2.0) : 1.0;
            boolean blink = b.getRemainingLifetime() < 1.2 && ((int)(globalAnimTimer * 10) % 2 == 0);
            gc.setGlobalAlpha(blink ? 0.38 : 1.0);
            gc.translate(x+w/2.0,y+h/2.0); gc.scale(pop,pop); gc.translate(-(x+w/2.0),-(y+h/2.0));

            String buffImage = "/assets/images/buffs/" + b.getType().name().toLowerCase() + ".png";
            boolean imageDrawn = b.getType() == com.dodgingcat.buffs.BuffType.HEART
                    ? AssetManager.drawImageContentFit(gc, buffImage,
                    x - 1 - w * 0.10, y - 1 - h * 0.10, (w + 2) * 1.20, (h + 2) * 1.20)
                    : AssetManager.drawImage(gc, buffImage, x - 1, y - 1, w + 2, h + 2);
            if (imageDrawn) { gc.restore(); continue; }

            switch (b.getType()) {
                case HEART -> {
                    gc.setFill(Color.web("#E91E63"));
                    gc.fillOval(x + 2, y + 2, w / 2, h / 2);
                    gc.fillOval(x + w / 2 - 2, y + 2, w / 2, h / 2);
                    gc.fillPolygon(new double[]{x + 2, x + w - 2, x + w / 2},
                            new double[]{y + h / 2 - 2, y + h / 2 - 2, y + h - 2}, 3);
                }
                case BOMB -> {
                    gc.setFill(Color.web("#212121"));
                    gc.fillOval(x + 3, y + 6, w - 6, h - 8);
                    gc.setFill(Color.web("#FF9100"));
                    gc.fillOval(x + w / 2 - 2, y, 4, 6);
                }
                case SHIELD -> {
                    gc.setFill(Color.web("#00E5FF"));
                    gc.fillRoundRect(x + 3, y + 3, w - 6, h - 6, 8, 8);
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(2.0);
                    gc.strokeRoundRect(x + 3, y + 3, w - 6, h - 6, 8, 8);
                }
                case INVINCIBILITY -> {
                    gc.setFill(Color.web("#FFD700"));
                    gc.fillOval(x + 2, y + 2, w - 4, h - 4);
                    gc.setFill(Color.WHITE);
                    gc.fillOval(x + 7, y + 7, w - 14, h - 14);
                }
                case DOUBLE_SCORE -> {
                    gc.setFill(Color.web("#76FF03"));
                    gc.fillRoundRect(x + 2, y + 2, w - 4, h - 4, 6, 6);
                    gc.setFill(Color.BLACK);
                    gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                    gc.setTextAlign(TextAlignment.CENTER);
                    ArtText.fillText(gc, "2x", x + w / 2, y + h - 7);
                }
            }
            gc.restore();
        }
    }

    private void renderFloatingTexts(DrawContext gc, List<FloatingText> texts) {
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.CENTER);
        for (FloatingText ft : texts) {
            gc.setFill(ft.getColor());
            ArtText.fillText(gc, ft.getText(), ft.getX(), ft.getY());
        }
    }

    /* =========================================================================
     * IN-GAME HUD (Point 5)
     * ========================================================================= */
    private void renderHUD(DrawContext gc, GameWorld world, GameData data, boolean isVi) {
        gc.setFill(Color.rgb(18, 22, 32, 0.28));
        gc.fillRoundRect(15, 12, width - 30, 36, 9, 9);
        gc.setStroke(Color.rgb(255, 222, 164, 0.48));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(15, 12, width - 30, 36, 9, 9);

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        int best = (world.getMode() == GameMode.SINGLE_PLAYER) ? data.getSingleHighScore() : data.getCoopHighScore();
        long tenths = Math.round(world.getSurvivalTime() * 10);
        boolean languageChanged = hudLanguageVi != isVi;
        if (tenths != hudTimeTenths || languageChanged) {
            hudLabels[0] = I18n.get("hud.time") + ": " + tenths / 10 + "." + tenths % 10 + "s";
            hudTimeTenths = tenths;
        }
        if (hudScore != world.getScore() || languageChanged) {
            hudLabels[1] = I18n.get("hud.score") + ": " + world.getScore();
            hudScore = world.getScore();
        }
        if (hudFishes != world.getFishesCollected() || languageChanged) {
            hudLabels[2] = I18n.get("hud.fishes") + ": +" + world.getFishesCollected();
            hudFishes = world.getFishesCollected();
        }
        if (hudBest != best || languageChanged) {
            hudLabels[3] = I18n.get("hud.best") + ": " + best;
            hudBest = best;
        }
        hudLanguageVi = isVi;
        Color[] colors = {Color.WHITE, Color.web("#69F0AE"), Color.web("#FFD54F"), Color.web("#B0BEC5")};
        int itemCount = 4;
        double slotWidth = (width - 64.0) / itemCount;
        gc.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < hudLabels.length; i++) {
            double slotCenter = 32.0 + slotWidth * (i + 0.5);
            gc.setFill(colors[i]);
            ArtText.fillText(gc, hudLabels[i], slotCenter, 35, slotWidth - 12);
            if (i > 0) {
                gc.setStroke(Color.rgb(255, 226, 184, 0.20));
                gc.setLineWidth(1.0);
                gc.strokeLine(32.0 + slotWidth * i, 19, 32.0 + slotWidth * i, 41);
            }
        }

    }

    /* =========================================================================
     * BROTATO-STYLE MAIN MENU (Point 1 & 2)
     * ========================================================================= */
    private void renderMenuStageBackground(DrawContext gc, String mapId, String previewSkin, boolean shop) {
        Theme activeTheme = Theme.fromId(mapId);
        var preparedImage = MapBackdropCache.getPrepared(gc, activeTheme.getId(), width, height,
                shop ? MapBackdropCache.Shade.SHOP : MapBackdropCache.Shade.MENU);
        boolean prepared = preparedImage != null;
        if (prepared) gc.drawImage(preparedImage, 0, 0, width, height);
        shopBackdrop = shop ? preparedImage : null;
        shopBackdropDimmed = prepared && shop;
        if (!prepared && !drawMapBackground(gc, activeTheme.getId())) {
            LinearGradient bg = new LinearGradient(0, 0, 0, height, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#596879")), new Stop(1, Color.web("#322A2A")));
            gc.setFill(bg);
            gc.fillRect(0, 0, width, height);
        }
        if (!prepared) {
            gc.setFill(Color.rgb(18, 22, 30, 0.38));
            gc.fillRect(0, 0, width, height);
        }

        if (shop || previewSkin == null) return; // Only show the mascot outside the catalog/loadout grids.

        double floorRatio = 0.8555;
        double cx = width / 2.0;
        previewCat.setPosition(cx - 22, height * floorRatio - 36);
        previewCat.setSkinId(previewSkin);
        renderAnimatedCatSkin(gc, previewCat, true);
    }

    private void renderBootSequence(DrawContext gc, double elapsed) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);
        double cx = width / 2.0, cy = height / 2.0;
        if (elapsed < com.dodgingcat.core.GameController.BOOT_TITLE_SECONDS) {
            drawImageFit(gc, "/assets/images/ui/title_dodging_cat.png",
                    cx - Math.min(360, width * 0.42), cy - 100,
                    Math.min(720, width * 0.84), 200);
        } else {
            drawImageFit(gc, "/assets/images/ui/team_tadq.png",
                    cx - 120, cy - 120, 240, 240);
        }
    }

    private void renderBrotatoMainMenu(DrawContext gc, InputManager input, int selectedIndex, GameData data, boolean isVi, boolean ignoredResume) {
        double cx = width / 2.0;
        double buttonW = 192, buttonH = 46, gap = 9, firstY = 24;
        double leftX = 24, rightX = width - 24 - buttonW;
        String[] leftLabels = {isVi ? "CỬA HÀNG" : "SHOP", "BGM", "SFX"};
        String[] rightLabels = {I18n.get("menu.leaderboard"), isVi ? "NGÔN NGỮ" : "LANGUAGE", isVi ? "GIỚI THIỆU" : "ABOUT US"};
        for (int row = 0; row < 3; row++) {
            double y = firstY + row * (buttonH + gap);
            if (row == 1 || row == 2) {
                String label = row == 1 ? "BGM" : "SFX";
                double volume = row == 1 ? data.getBgmVolume() : data.getSfxVolume();
                renderMenuVolumeButton(gc, input, label, (int) Math.round(volume * 100) + "%",
                        leftX, y, buttonW, buttonH, row == 1 ? "volume:bgm" : "volume:sfx");
            } else {
                boolean hover = input.isMouseHovering(leftX, y, buttonW, buttonH);
                renderMenuButton(gc, leftLabels[row], leftX, y, buttonW, buttonH, hover, false);
            }
            boolean rightHover = input.isMouseHovering(rightX, y, buttonW, buttonH);
            renderMenuButton(gc, rightLabels[row], rightX, y, buttonW, buttonH, rightHover, false);
        }

        double titleW = Math.min(860, width * 0.78);
        drawImageFit(gc, "/assets/images/ui/title_dodging_cat.png",
                cx - titleW / 2.0, 182, titleW, 180);

        double buttonY = height - 198;
        double modeButtonW = 220, modeButtonH = 76;
        boolean oneHover = input.isMouseHovering(cx-280,buttonY,modeButtonW,modeButtonH);
        boolean twoHover = input.isMouseHovering(cx+60,buttonY,modeButtonW,modeButtonH);
        renderMenuButton(gc, "1P", cx-280, buttonY, modeButtonW, modeButtonH, oneHover, false);
        renderMenuButton(gc, "2P", cx+60, buttonY, modeButtonW, modeButtonH, twoHover, false);
        boolean quitHover=input.isMouseHovering(width-154,height-56,130,40);
        renderMenuButton(gc,isVi?"THOÁT":"QUIT",width-154,height-56,130,40,quitHover,false);
    }

    private void renderMenuButton(DrawContext gc, String text, double x, double y, double w, double h, boolean hovered, boolean selected) {
        if (hovered) {
            String key = "button:" + text + ":" + Math.round(x) + ":" + Math.round(y)
                    + ":" + Math.round(w) + ":" + Math.round(h);
            trackHover(key, true);
        }
        drawButtonSprite(gc, x, y, w, h, hovered || selected);
        gc.setFill(hovered || selected ? Color.web("#FFE4A8") : Color.web("#FFF4DE"));
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, text.equals("1P") || text.equals("2P") ? 30 : Math.min(21, h * 0.43)));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, text, x + w / 2.0, y + h / 2.0 + h * 0.14, w * 0.76);
    }

    private void renderMenuVolumeButton(DrawContext gc, InputManager input, String label, String value,
                                        double x, double y, double w, double h, String hoverId) {
        boolean hovered = input.isMouseHovering(x, y, w, h);
        trackHover(hoverId, hovered);
        drawButtonSprite(gc, x, y, w, h, hovered);
        gc.setFill(hovered ? Color.web("#FFE4A8") : Color.web("#FFF4DE"));
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, label + " " + value, x + w / 2.0, y + h / 2.0 + 6, w * 0.76);
    }

    private void drawButtonSprite(DrawContext gc, double x, double y, double w, double h, boolean highlighted) {
        gc.save();
        gc.setGlobalAlpha(1.0);
        gc.setEffect(null);
        boolean drawn = AssetManager.drawImage(gc, highlighted ? "/assets/images/ui/button_plaque_hover.png"
                : "/assets/images/ui/button_plaque.png", x, y, w, h);
        gc.restore();
        if (!drawn) {
            gc.setFill(new LinearGradient(x, y, x, y + h, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(76, 58, 48, 0.96)), new Stop(1, Color.rgb(43, 34, 31, 0.96))));
            gc.fillRoundRect(x, y, w, h, 12, 12);
            gc.setStroke(Color.rgb(255, 225, 176, 0.72));
            gc.strokeRoundRect(x, y, w, h, 12, 12);
        }
        // The plaque alpha defines its contour; a rectangular hover stroke leaks outside it.
    }

    private void drawImageFit(DrawContext gc, String resourcePath, double x, double y,
                              double maxWidth, double maxHeight) {
        AssetManager.drawImageContentFit(gc, resourcePath, x, y, maxWidth, maxHeight);
    }

    /* =========================================================================
     * PRE-GAME LOADOUT SCREEN (Point 9)
     * ========================================================================= */
    private void renderPreGameSelectScreen(DrawContext gc, InputManager input, ShopManager shopManager, GameData data,
                                          int tab, int selectedMapIdx, int selectedSkinIdx, int p2SkinIdx,
                                          boolean p1Ready, boolean p2Ready, int activeSkinPlayer, GameMode mode, double skinScrollOffset, boolean isVi) {
        renderDimmer(gc, 0.50);
        double cx = width / 2.0;

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        gc.setFill(Color.web("#FFE0A0"));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, I18n.get("pregame.title"), cx, 65);

        // Tabs: [ SELECT MAP ] and [ SELECT SKIN ]
        double tabW = 180;
        double tabH = 38;
        double tab1X = cx - 190;
        double tab2X = cx + 10;
        double tabY = 95;

        renderSubTab(gc, I18n.get("pregame.map_tab"), tab1X, tabY, tabW, tabH, tab == 0);
        String skinTabLabel = I18n.get("pregame.cat_skin_tab");
        renderSubTab(gc, skinTabLabel, tab2X, tabY, tabW, tabH, tab == 1);
        trackHover("loadout:map-tab", input.isMouseHovering(tab1X, tabY, tabW, tabH));
        trackHover("loadout:skin-tab", input.isMouseHovering(tab2X, tabY, tabW, tabH));

        // Grid Cards Display
        if (tab == 0) {
            // Display Owned Maps
            List<ShopItem> ownedMaps = shopManager.getMapCatalog().stream()
                    .filter(m -> shopManager.isUnlocked(m.getId(), ItemCategory.THEME))
                    .toList();

            double cardW = 160;
            double cardH = 160;
            double startX = cx - (ownedMaps.size() * (cardW + 20) - 20) / 2.0;
            double cardY = 165;

            for (int i = 0; i < ownedMaps.size(); i++) {
                ShopItem mapItem = ownedMaps.get(i);
                double mx = startX + i * (cardW + 20);
                boolean isSelected = mapItem.getId().equalsIgnoreCase(data.getSelectedMap());
                boolean hovered = input.isMouseHovering(mx, cardY, cardW, cardH);
                trackHover("loadout:map:" + mapItem.getId(), hovered);

                renderMapCard(gc, mapItem, mx, cardY, cardW, cardH, isSelected, hovered, isVi);
            }
        } else {
            // Display Owned Skins
            List<ShopItem> ownedSkins = shopManager.getSkinCatalog();

            double cardW = 120;
            double cardH = 160;
            int cols = 5;
            double startX = cx - (cols * (cardW + 15) - 15) / 2.0;
            double viewportTop = mode == GameMode.LOCAL_COOP ? 170.0 : 155.0;
            double startY = viewportTop + 5.0;

            if (mode == GameMode.LOCAL_COOP) {
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFill(p1Ready ? Color.web("#69F0AE") : Color.WHITE);
                ArtText.fillText(gc, I18n.get(p1Ready ? "pregame.p1_ready" : "pregame.p1_controls"), cx - 172, 154, 320);
                gc.setFill(p2Ready ? Color.web("#FF80AB") : Color.WHITE);
                ArtText.fillText(gc, I18n.get(p2Ready ? "pregame.p2_ready" : "pregame.p2_controls"), cx + 172, 154, 320);
            }

            double viewportBottom = height - 105.0;
            int rows = (ownedSkins.size() + cols - 1) / cols;
            double contentHeight = rows == 0 ? 0.0 : 5.0 + rows * cardH + (rows - 1) * 15.0;
            double maxScroll = Math.max(0.0, contentHeight - (viewportBottom - viewportTop));
            skinScrollOffset = Math.clamp(skinScrollOffset, 0.0, maxScroll);

            gc.save();
            clipToPixels(gc, 0, viewportTop, width, viewportBottom - viewportTop);

            for (int i = 0; i < ownedSkins.size(); i++) {
                ShopItem skinItem = ownedSkins.get(i);
                int row = i / cols;
                int col = i % cols;
                double sx = startX + col * (cardW + 15);
                double sy = startY + row * (cardH + 15) - skinScrollOffset;
                if (sy + cardH < viewportTop || sy > viewportBottom) continue;

                boolean isOwned = shopManager.isUnlocked(skinItem.getId(), ItemCategory.SKIN);
                boolean isSelected = isOwned && skinItem.getId().equalsIgnoreCase(data.getP1Skin());
                boolean hovered = input.getMouseY() >= viewportTop && input.getMouseY() < viewportBottom
                        && input.isMouseHovering(sx, sy, cardW, cardH);
                trackHover("loadout:skin:" + skinItem.getId(), hovered);

                renderSkinCard(gc, skinItem, sx, sy, cardW, cardH, isSelected, hovered, isOwned, isVi);
                if (mode == GameMode.LOCAL_COOP) {
                    if (isOwned && skinItem.getId().equalsIgnoreCase(data.getP2Skin())) {
                        renderPlayerSkinMarker(gc, sx, sy, cardW, cardH, "P2", Color.web("#FF80AB"), true);
                    }
                    if (isOwned && skinItem.getId().equalsIgnoreCase(data.getP1Skin())) {
                        renderPlayerSkinMarker(gc, sx, sy, cardW, cardH, "P1", Color.web("#69F0AE"), false);
                    }
                    if (isOwned && i == selectedSkinIdx) renderSkinCursor(gc, sx, sy, cardW, cardH, Color.web("#40C4FF"), activeSkinPlayer == 1);
                    if (isOwned && i == p2SkinIdx) renderSkinCursor(gc, sx, sy, cardW, cardH, Color.web("#FF4081"), activeSkinPlayer == 2);
                }
            }
            gc.restore();

            if (maxScroll > 0.0) {
                double trackX = width - 22.0;
                double trackHeight = viewportBottom - viewportTop;
                double thumbHeight = Math.max(34.0, trackHeight * trackHeight / contentHeight);
                double thumbY = viewportTop + (skinScrollOffset / maxScroll) * (trackHeight - thumbHeight);
                gc.setFill(Color.rgb(255, 255, 255, 0.16));
                gc.fillRoundRect(trackX, viewportTop, 5, trackHeight, 5, 5);
                gc.setFill(Color.web("#FFD54F"));
                gc.fillRoundRect(trackX, thumbY, 5, thumbHeight, 5, 5);
            }
        }

        // Big START GAME button
        double startBtnW = 260;
        double startBtnH = 50;
        double startBtnX = cx - startBtnW / 2.0;
        double startBtnY = tab == 0 ? 355 : height - 90;
        boolean startHover = input.isMouseHovering(startBtnX, startBtnY, startBtnW, startBtnH);
        trackHover("loadout:start", startHover);

        drawButtonSprite(gc, startBtnX, startBtnY, startBtnW, startBtnH, startHover);
        gc.setFill(Color.web("#FFF4DE"));
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, I18n.get("pregame.start"), cx, startBtnY + 32, startBtnW * 0.76);

        // Back button
        renderBackButton(gc, input, 40, height - 55);
    }

    private void renderPlayerSkinMarker(DrawContext gc, double x, double y, double w, double h,
                                        String label, Color color, boolean rightSide) {
        gc.save();
        gc.setGlobalAlpha(1.0);
        double markerX = rightSide ? x + w - 42 : x + 6;
        double markerY = y + h - 62;
        gc.setFill(Color.web("#241C1A"));
        gc.fillRoundRect(markerX, markerY, 36, 23, 7, 7);
        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.strokeRoundRect(markerX, markerY, 36, 23, 7, 7);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, label, markerX + 18, markerY + 17, 28);
        gc.restore();
    }

    private void renderSkinCursor(DrawContext gc, double x, double y, double w, double h, Color color, boolean active) {
        gc.save();
        gc.setStroke(color);
        gc.setLineWidth(active ? 4.0 : 2.2);
        gc.strokeRoundRect(x + 1.5, y + 1.5, w - 3, h - 3, 8, 8);
        gc.restore();
    }

    private void renderMapCard(DrawContext gc, ShopItem mapItem, double x, double y, double w, double h, boolean isSelected, boolean hovered, boolean isVi) {
        Theme mapTheme = Theme.fromId(mapItem.getId());

        gc.setFill(Color.rgb(49, 39, 34, 0.96));
        gc.fillRoundRect(x, y, w, h, 13, 13);
        double imageX = x + 5, imageY = y + 5, imageW = w - 10, imageH = 112;
        if (!drawImageCover(gc, "/assets/images/backgrounds/" + mapTheme.getId() + ".png",
                imageX, imageY, imageW, imageH)) {
            gc.setFill(mapTheme.getSkyTop());
            gc.fillRoundRect(imageX, imageY, imageW, imageH, 9, 9);
        }
        gc.setFill(Color.rgb(44, 34, 30, 0.96));
        gc.fillRect(x + 5, y + 112, w - 10, h - 117);

        // Card outline
        if (isSelected) {
            gc.setStroke(Color.web("#F5C46B"));
            gc.setLineWidth(3.0);
            gc.strokeRoundRect(x + 1.5, y + 1.5, w - 3, h - 3, 13, 13);
            gc.setFill(Color.rgb(48, 35, 28, 0.88));
            gc.fillRoundRect(x + 8, y + 8, 62, 18, 7, 7);
            gc.setFill(Color.web("#FFE4A8"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
            gc.setTextAlign(TextAlignment.CENTER);
            ArtText.fillText(gc, "EQUIPPED", x + 39, y + 21);
        } else if (hovered) {
            gc.setStroke(Color.web("#F5C46B"));
            gc.setLineWidth(2.0);
            gc.strokeRoundRect(x, y, w, h, 13, 13);
        }

        gc.setFill(Color.web("#FFF4DE"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, mapItem.getDisplayName(isVi), x + w / 2.0, y + 143, w - 14);

    }

    private void renderSkinCard(DrawContext gc, ShopItem skinItem, double x, double y, double w, double h, boolean isSelected, boolean hovered, boolean owned, boolean isVi) {
        gc.setFill(Color.rgb(49, 39, 34, 0.96));
        gc.fillRoundRect(x, y, w, h, 8, 8);

        // Mini preview of the cat skin
        renderSkinPreview(gc, skinItem.getId(), x + 9, y + 14, w - 18, 70);
        if (!owned) {
            gc.setFill(Color.rgb(8, 9, 14, 0.64)); gc.fillRoundRect(x+1,y+1,w-2,h-2,8,8);
            drawPadlock(gc,x+w/2.0,y+h-61,0.62);
        }

        // Rarity accent line
        gc.setFill(skinItem.getRarity().getColor());
        gc.fillRect(x + 6, y + h - 6, w - 12, 3);

        if (isSelected) {
            gc.setStroke(Color.web("#F5C46B"));
            gc.setLineWidth(2.5);
            gc.strokeRoundRect(x, y, w, h, 8, 8);
            // The selected border identifies the equipped skin without covering its face.
        } else if (hovered) {
            gc.setStroke(Color.web("#F5C46B"));
            gc.strokeRoundRect(x, y, w, h, 8, 8);
        }

        // Name
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, skinItem.getDisplayName(isVi), x + w / 2.0, y + h - 22, w - 12);
    }

    /* =========================================================================
     * SHOP SCREEN (Point 3)
     * ========================================================================= */
    private void renderShopScreen(DrawContext gc, InputManager input, ShopManager shopManager, GameData data,
                                  int tab, int selectedIndex, double scrollOffset, boolean isVi,
                                  GachaSequence gacha, int gachaPrice) {
        // The modal owns input. Avoid animating and drawing the entire hidden catalog
        // beneath its dimmer on every spin/reveal frame.
        if (gacha != null && gacha.isActive()) {
            switch (gacha.getPhase()) {
                case SPINNING, LANDED -> renderGachaOverlay(gc, gacha, isVi);
                case LEGENDARY_REVEAL -> {
                    if (!modalBackdropDimmed) renderDimmer(gc, 0.88);
                    GachaEffects.renderLegendary(gc, gacha, width / 2.0, height / 2.0);
                }
                case RESULT -> renderGachaResult(gc, gacha, isVi);
                default -> { }
            }
            return;
        }
        if (!shopBackdropDimmed) renderDimmer(gc, 0.50);
        double cx = width / 2.0;

        // Tabs: [ SKINS ] and [ MAPS ]
        double tabW = 160;
        double tabH = 36;
        double tab1X = cx - 170;
        double tab2X = cx + 10;
        double tabY = 115;

        trackHover("shop:skins-tab", input.isMouseHovering(tab1X, tabY, tabW, tabH));
        trackHover("shop:maps-tab", input.isMouseHovering(tab2X, tabY, tabW, tabH));

        // Display Catalog Cards
        List<ShopItem> items = (tab == 0) ? shopManager.getSkinCatalog() : shopManager.getMapCatalog();
        double startY = 170;

        double viewportTop = 160;
        double viewportBottom = height - 75;
        gc.save();
        if (shopBackdrop == null) {
            clipToPixels(gc, 0, viewportTop, width, viewportBottom - viewportTop);
        }

        if (tab == 0) {
            // Skins Grid (5 columns)
            int cols = 5;
            double cardW = 140;
            double cardH = 175;
            double startX = cx - (cols * (cardW + 15) - 15) / 2.0;

            for (int i = 0; i < items.size(); i++) {
                ShopItem item = items.get(i);
                int r = i / cols;
                int c = i % cols;
                double sx = startX + c * (cardW + 15);
                double sy = startY + r * (cardH + 15) - scrollOffset;
                if (sy + cardH < viewportTop || sy > viewportBottom) continue;

                boolean owned = shopManager.isUnlocked(item.getId(), ItemCategory.SKIN);
                boolean hovered = input.getMouseY() >= viewportTop
                        && input.getMouseY() < viewportBottom && input.isMouseHovering(sx, sy, cardW, cardH);
                trackHover("shop:skin:" + item.getId(), hovered);

                renderShopSkinCard(gc, input, item, sx, sy, cardW, cardH, owned, hovered, isVi);
            }
        } else {
            // Maps Row
            double cardW = 160;
            double cardH = 200;
            double startX = cx - (items.size() * (cardW + 18) - 18) / 2.0;

            for (int i = 0; i < items.size(); i++) {
                ShopItem item = items.get(i);
                double mx = startX + i * (cardW + 18);
                boolean owned = shopManager.isUnlocked(item.getId(), ItemCategory.THEME);
                boolean hovered = input.isMouseHovering(mx, startY, cardW, cardH);
                trackHover("shop:map:" + item.getId(), hovered);

                renderShopMapCard(gc, input, item, mx, startY, cardW, cardH, owned, hovered, isVi, i == selectedIndex);
            }
        }

        gc.restore();

        if (shopBackdrop != null) {
            // Opaque native-resolution bands hide partially scrolled cards without
            // applying a large alpha-mask clip to every card/image/text draw.
            double iw = shopBackdrop.getWidth(), ih = shopBackdrop.getHeight();
            gc.drawImage(shopBackdrop, 0, 0, iw, ih * viewportTop / height, 0, 0, width, viewportTop);
            gc.drawImage(shopBackdrop, 0, ih * viewportBottom / height, iw, ih * (height - viewportBottom) / height,
                    0, viewportBottom, width, height - viewportBottom);
        }
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        gc.setFill(Color.web("#FFD54F")); gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, I18n.get("shop.title"), cx, 60);
        renderSubTab(gc, I18n.get("shop.skins_tab"), tab1X, tabY, tabW, tabH, tab == 0);
        renderSubTab(gc, I18n.get("shop.maps_tab"), tab2X, tabY, tabW, tabH, tab == 1);

        if (tab == 0) {
            int rows = (items.size() + 4) / 5;
            double contentHeight = 170 + rows * 190.0 - 15.0;
            double visibleHeight = viewportBottom - viewportTop;
            if (contentHeight > visibleHeight) {
                double trackX = width - 24;
                double trackY = viewportTop;
                double trackHeight = visibleHeight;
                gc.setFill(Color.rgb(255, 255, 255, 0.14));
                gc.fillRoundRect(trackX, trackY, 6, trackHeight, 6, 6);
                double thumbHeight = Math.max(36, trackHeight * visibleHeight / contentHeight);
                double maxScroll = contentHeight - viewportBottom;
                double thumbY = trackY + (maxScroll == 0 ? 0 : scrollOffset / maxScroll) * (trackHeight - thumbHeight);
                gc.setFill(Color.web("#FFAB00"));
                gc.fillRoundRect(trackX, thumbY, 6, thumbHeight, 6, 6);
            }
            // The visible scroll rail communicates the remaining catalog.
        }
        if (tab == 0) {
            String label = (isVi ? "QUAY GACHA  •  FISH " : "GACHA PULL  •  FISH ") + gachaPrice;
            boolean hover = input.isMouseHovering(width/2.0-150, height-66, 300, 48);
            renderMenuButton(gc, label, width/2.0-150, height-66, 300, 48, hover, false);
        }
        renderBackButton(gc, input, 40, height - 50);
    }

    private void renderSkinPreview(DrawContext gc, String skin, double x, double y, double w, double h) {
        int frame = (int) (globalAnimTimer * 2.5) % 6;
        AssetManager.drawCharacterPreview(gc, "/assets/images/characters/" + skin + "_sheet.png", frame, x, y, w, h);
    }

    private void renderShopSkinCard(DrawContext gc, InputManager input, ShopItem item, double x, double y,
                                    double w, double h, boolean owned, boolean hovered, boolean isVi) {
        renderSkinCard(gc, item, x, y, w, h, false, hovered, owned, isVi);
        if (owned) {
            gc.setFont(Font.font("Georgia", FontWeight.BOLD, 10));
            gc.setTextAlign(TextAlignment.CENTER);
            ArtText.fillText(gc, I18n.get("shop.owned"), x + w / 2.0, y + h - 44, w - 16);
        }
    }

    private void renderShopMapCard(DrawContext gc, InputManager input, ShopItem item, double x, double y, double w, double h, boolean owned, boolean hovered, boolean isVi, boolean selected) {
        Theme mapTheme = Theme.fromId(item.getId());

        gc.setFill(Color.rgb(49, 39, 34, 0.96));
        gc.fillRoundRect(x, y, w, h, 12, 12);

        if (!drawImageCover(gc, "/assets/images/backgrounds/" + mapTheme.getId() + ".png", x + 6, y + 6, w - 12, 108)) {
            gc.setFill(mapTheme.getSkyTop());
            gc.fillRoundRect(x + 6, y + 6, w - 12, 108, 8, 8);
        }

        // Map cards have no rarity tier; keep a neutral outline and highlight hover.
        if (!owned) {
            gc.setFill(Color.rgb(8, 9, 14, 0.70)); gc.fillRoundRect(x+2,y+2,w-4,h-4,11,11);
            drawPadlock(gc,x+w/2.0,y+62,0.9);
        }
        gc.setStroke(selected ? Color.web("#FFE0A0") : hovered ? Color.web("#F5C46B") : Color.rgb(255, 225, 176, 0.52));
        gc.setLineWidth(selected || hovered ? 2.5 : 1.0);
        gc.strokeRoundRect(x, y, w, h, 12, 12);

        gc.setFill(Color.web("#FFF4DE"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, item.getDisplayName(isVi), x + w / 2.0, y + 138, w - 20);

        if (owned) {
            gc.setFill(Color.web("#FFE0A0"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            ArtText.fillText(gc, I18n.get("shop.owned"), x + w / 2.0, y + h - 18);
        } else {
            double btnW = w - 28;
            double btnH = 30;
            double btnX = x + 14;
            double btnY = y + h - 42;
            boolean btnHover = hovered && input.isMouseHovering(btnX, btnY, btnW, btnH);

            drawButtonSprite(gc, btnX, btnY, btnW, btnH, btnHover);
            gc.setFill(Color.web("#FFF0C9"));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            ArtText.fillText(gc, "FISH " + item.getPrice(), x + w / 2.0, btnY + 22);
        }
    }

    private void renderSubTab(DrawContext gc, String title, double x, double y, double w, double h, boolean active) {
        drawButtonSprite(gc, x, y, w, h, active);
        gc.setFill(Color.web("#FFF4DE"));
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 17));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, title, x + w / 2.0, y + h / 2.0 + 6, w * 0.74);
    }

    private void drawPadlock(DrawContext gc, double cx, double cy, double scale) {
        gc.save();
        gc.setLineWidth(3.0 * scale);
        gc.setStroke(Color.web("#FFE2A0"));
        gc.strokeRoundRect(cx-9*scale,cy-16*scale,18*scale,19*scale,9*scale,9*scale);
        gc.setFill(Color.web("#F3B54F"));
        gc.fillRoundRect(cx-14*scale,cy-5*scale,28*scale,22*scale,5*scale,5*scale);
        gc.setFill(Color.web("#3B261C"));
        gc.fillOval(cx-2.5*scale,cy+2*scale,5*scale,5*scale);
        gc.fillRoundRect(cx-1.4*scale,cy+5*scale,2.8*scale,6*scale,2*scale,2*scale);
        gc.restore();
    }

    private void renderGachaOverlay(DrawContext gc, GachaSequence gacha, boolean isVi) {
        gc.save();
        if (!modalBackdropDimmed) renderDimmer(gc, 0.84);
        double panelW = Math.min(760, width - 60), panelH = 236;
        double x = (width - panelW) / 2.0, y = (height - panelH) / 2.0;
        drawUiPanel(gc, x, y, panelW, panelH);
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, isVi ? "QUAY TRANG PHỤC" : "SKIN GACHA", width / 2.0, y + 38, panelW - 40);
        double cellW = 112, cellH = 138, pitch = 124;
        double position = gacha.getReelPosition();
        double frameX = width / 2.0 - cellW / 2.0, cardY = y + 64;
        gc.save();
        clipToPixels(gc, x + 12, cardY - 1, panelW - 24, cellH + 2);
        int first = Math.max(0, (int) Math.floor(position) - 4);
        int last = Math.min(gacha.getReel().size() - 1, (int) Math.ceil(position) + 4);
        for (int i = first; i <= last; i++) {
            ShopItem item = gacha.getReel().get(i);
            double cardX = frameX + (i - position) * pitch;
            gc.setFill(Color.web("#3A2C26"));
            gc.fillRoundRect(cardX, cardY, cellW, cellH, 9, 9);
            gc.setStroke(item.getRarity().getColor()); gc.setLineWidth(1.5);
            gc.strokeRoundRect(cardX, cardY, cellW, cellH, 9, 9);
            renderSkinPreview(gc, item.getId(), cardX + 8, cardY + 10, cellW - 16, 90);
            gc.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
            ArtText.fillText(gc, item.getDisplayName(isVi), cardX + cellW / 2.0, cardY + 121, cellW - 12);
        }
        gc.restore();
        gc.setStroke(Color.web("#FFE08A")); gc.setLineWidth(3);
        gc.strokeRoundRect(frameX - 3, cardY - 3, cellW + 6, cellH + 6, 11, 11);
        gc.setFill(Color.web("#FFD45E"));
        gc.fillPolygon(new double[]{width / 2.0 - 8, width / 2.0 + 8, width / 2.0},
                new double[]{cardY - 12, cardY - 12, cardY - 3}, 3);
        gc.restore();
    }

    private void renderGachaResult(DrawContext gc, GachaSequence gacha, boolean isVi) {
        ShopItem winner = gacha.getWinner();
        String winnerName = winner.getDisplayName(isVi);
        gc.save();
        if (!modalBackdropDimmed) renderDimmer(gc, 0.78);
        double panelW = Math.min(380, width - 48), panelH = 310;
        double x = (width - panelW) / 2.0, y = (height - panelH) / 2.0;
        if (winner.getRarity() == SkinRarity.RARE) GachaEffects.renderRare(gc, gacha, width / 2.0, height / 2.0);
        gc.setFill(Color.web("#2B221F"));
        gc.fillRoundRect(x, y, panelW, panelH, 18, 18);
        drawUiPanel(gc, x, y, panelW, panelH);
        gc.setStroke(winner.getRarity().getColor());
        gc.setLineWidth(2.0);
        gc.strokeRoundRect(x + 5, y + 5, panelW - 10, panelH - 10, 15, 15);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        ArtText.fillText(gc, isVi ? "CHÚC MỪNG!" : "CONGRATULATIONS!", width / 2.0, y + 43, panelW - 32);

        renderSkinPreview(gc, winner.getId(), width / 2.0 - 110, y + 70, 220, 138);

        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 23));
        ArtText.fillText(gc, winnerName, width / 2.0, y + 249, panelW - 32);
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 12));
        ArtText.fillText(gc, isVi ? "Nhấn chuột để tiếp tục" : "Click to continue", width / 2.0, y + 286, panelW - 40);
        gc.restore();
    }

    private static void clipToPixels(DrawContext gc, double x, double y, double w, double h) {
        var transform = gc.getTransform();
        if (transform.getMxy() == 0 && transform.getMyx() == 0 && transform.getMxx() > 0 && transform.getMyy() > 0) {
            double left = Math.floor(x * transform.getMxx() + transform.getTx());
            double top = Math.floor(y * transform.getMyy() + transform.getTy());
            double right = Math.ceil((x + w) * transform.getMxx() + transform.getTx());
            double bottom = Math.ceil((y + h) * transform.getMyy() + transform.getTy());
            gc.setTransform(1, 0, 0, 1, 0, 0);
            gc.beginPath(); gc.rect(left, top, right - left, bottom - top); gc.clip();
            gc.setTransform(transform);
        } else {
            gc.beginPath(); gc.rect(x, y, w, h); gc.clip();
        }
    }

    /* =========================================================================
     * PAUSE MENU (Point 4)
     * ========================================================================= */
    private void renderPauseOverlay(DrawContext gc, InputManager input, boolean isVi) {
        renderDimmer(gc, 0.75);
        double cx = width / 2.0;

        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 36));
        gc.setFill(Color.web("#FFE0A0"));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, I18n.get("pause.title"), cx, height / 2.0 - 154);

        String[] pauseOptions = {I18n.get("pause.resume"), I18n.get("pause.restart"),
                "BGM  " + Math.round(AudioManager.getInstance().getBgmVolume() * 100) + "%",
                "SFX  " + Math.round(AudioManager.getInstance().getSfxVolume() * 100) + "%",
                I18n.get("pause.menu")};

        double btnW = 240;
        double btnH = 44;
        double startY = height / 2.0 - 115;

        for (int i = 0; i < pauseOptions.length; i++) {
            double by = startY + i * 50;
            boolean hovered = input.isMouseHovering(cx - btnW / 2.0, by, btnW, btnH);
            renderMenuButton(gc, pauseOptions[i], cx - btnW / 2.0, by, btnW, btnH, hovered, false);
        }
    }

    /* =========================================================================
     * GAME OVER SCREEN & SURVIVAL TITLES (Point 7)
     * ========================================================================= */
    private void renderGameOverOverlay(DrawContext gc, InputManager input, GameWorld world, GameData data,
                                       boolean isVi, int newLeaderboardRank) {
        renderDimmer(gc, 0.50);
        double cx = width / 2.0;

        // Big Game Over Banner
        gc.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 48));
        gc.setFill(Color.web("#F1A58B"));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, I18n.get("gameover.title"), cx, 130, width * 0.72);

        // Survival Title / Rank badge (Point 7)
        String survivalRank = world.getMode() == GameMode.LOCAL_COOP
                ? I18n.getCoopTitleByTime(world.getSurvivalTime())
                : I18n.getTitleByTime(world.getSurvivalTime());
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        gc.setFill(Color.web("#FFE0A0"));
        ArtText.fillText(gc, "« " + survivalRank + " »", cx, 175, width * 0.72);

        if (newLeaderboardRank > 0) {
            gc.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 17));
            gc.setFill(Color.web("#FFD45E"));
            ArtText.fillText(gc, String.format(I18n.get("gameover.new_record"), newLeaderboardRank), cx, 207, width * 0.72);
        }

        // Stats Box
        drawUiPanel(gc, cx - 145, 225, 290, 82);

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        gc.setFill(Color.WHITE);
        ArtText.fillText(gc, String.format("%s: %.1fs", I18n.get("gameover.survival_time"), world.getSurvivalTime()), cx, 249, 270);
        ArtText.fillText(gc, String.format("%s: %d", I18n.get("gameover.score"), world.getScore()), cx, 270, 270);

        // Current Run Fishes Only (Point 7)
        gc.setFill(Color.web("#FFD54F"));
        ArtText.fillText(gc, String.format("%s: +%d", I18n.get("gameover.fishes"), world.getFishesCollected()), cx, 291, 270);

        // Buttons
        String[] actions = {I18n.get("gameover.retry"), I18n.get("gameover.menu")};

        double btnW = 200;
        double btnH = 42;
        double startY = 340;

        for (int i = 0; i < actions.length; i++) {
            double by = startY + i * 50;
            boolean hovered = input.isMouseHovering(cx - btnW / 2.0, by, btnW, btnH);
            renderMenuButton(gc, actions[i], cx - btnW / 2.0, by, btnW, btnH, hovered, false);
        }
    }

    /* =========================================================================
     * LEADERBOARD SCREEN
     * ========================================================================= */
    private void renderLeaderboardOverlay(DrawContext gc, InputManager input, GameData data, boolean isVi) {
        renderDimmer(gc, 0.50);
        double cx = width / 2.0;

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        gc.setFill(Color.web("#FFE0A0"));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, I18n.get("menu.leaderboard"), cx, 80);

        double panelY = 100;
        double panelW = 400;
        double panelH = 350;
        drawUiPanel(gc, cx - 420, panelY, panelW, panelH);
        drawUiPanel(gc, cx + 20, panelY, panelW, panelH);
        for (double panelX : new double[]{cx - 420, cx + 20}) {
            gc.setFill(Color.rgb(255, 194, 92, 0.16));
            gc.fillRoundRect(panelX + 2, panelY + 2, panelW - 4, 48, 14, 14);
            gc.setStroke(Color.rgb(255, 222, 164, 0.28));
            gc.strokeLine(panelX + 24, panelY + 50, panelX + panelW - 24, panelY + 50);
        }

        // Single Column
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        gc.setFill(Color.WHITE);
        ArtText.fillText(gc, I18n.get("menu.single_player"), cx - 420 + panelW / 2.0, 130);

        List<LeaderboardEntry> single = data.getSingleLeaderboard();
        for (int i = 0; i < Math.min(6, single.size()); i++) {
            LeaderboardEntry e = single.get(i);
            renderLeaderboardRow(gc, cx - 390, 162 + i * 42, 340, i, e, isVi);
        }

        // Co-op Column
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        gc.setFill(Color.WHITE);
        ArtText.fillText(gc, I18n.get("menu.local_coop"), cx + 20 + panelW / 2.0, 130);

        List<LeaderboardEntry> coop = data.getCoopLeaderboard();
        for (int i = 0; i < Math.min(6, coop.size()); i++) {
            LeaderboardEntry e = coop.get(i);
            renderLeaderboardRow(gc, cx + 50, 162 + i * 42, 340, i, e, isVi);
        }

        renderBackButton(gc, input, 40, height - 55);
    }

    private void renderLeaderboardRow(DrawContext gc, double x, double y, double w, int rank,
                                      LeaderboardEntry entry, boolean isVi) {
        gc.setFill(rank == 0 ? Color.rgb(255, 190, 64, 0.18)
                : Color.rgb(255, 255, 255, rank % 2 == 0 ? 0.055 : 0.025));
        gc.fillRoundRect(x, y, w, 32, 8, 8);
        gc.setFill(rank == 0 ? Color.web("#FFD56F") : Color.web("#DBCBB7"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        gc.setTextAlign(TextAlignment.LEFT);
        String medal = String.format("%02d", rank + 1);
        ArtText.fillText(gc, medal, x + 12, y + 21);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Segoe UI", rank == 0 ? FontWeight.BOLD : FontWeight.NORMAL, 13));
        ArtText.fillText(gc, String.format("%d pts", entry.getScore()), x + 52, y + 21);
        gc.setFill(Color.web("#D8C5AB"));
        gc.setTextAlign(TextAlignment.RIGHT);
        ArtText.fillText(gc, String.format("%.1fs", entry.getSurvivalTime()), x + w - 12, y + 21);
        gc.setTextAlign(TextAlignment.CENTER);
    }

    /* =========================================================================
     * HELP & CONTROLS SCREEN
     * ========================================================================= */
    private void renderAboutUsOverlay(DrawContext gc, InputManager input, boolean isVi) {
        renderDimmer(gc, 0.50);
        double cx = width / 2.0;

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        gc.setFill(Color.web("#FFE0A0"));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, isVi ? "VỀ CHÚNG TÔI" : "ABOUT US", cx, 95);

        drawUiPanel(gc, cx - 270, 125, 540, 375);
        drawImageFit(gc, "/assets/images/ui/team_tadq.png", cx - 70, 145, 140, 140);
        drawImageFit(gc, "/assets/images/ui/title_dodging_cat.png", cx - 190, 288, 380, 78);
        gc.setFill(Color.web("#FFF4DE"));
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        gc.setTextAlign(TextAlignment.CENTER);
        String[] credits = {
                "Hoang Kien Quyet - 2301040209",
                "Tran Quang Anh - 2301040013",
                "Nguyen Cong Doan - 2301040042",
                "Tran Gia Thanh - 2301040171"
        };
        for (int i = 0; i < credits.length; i++) {
            ArtText.fillText(gc, credits[i], cx, 399 + i * 24);
        }

        renderBackButton(gc, input, 40, height - 55);
    }

    private void renderBackButton(DrawContext gc, InputManager input, double x, double y) {
        double w = 120;
        double h = 36;
        boolean hovered = input.isMouseHovering(x, y, w, h);
        trackHover("back", hovered);
        drawButtonSprite(gc, x, y, w, h, hovered);
        gc.setFill(Color.web("#FFF4DE"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.CENTER);
        ArtText.fillText(gc, I18n.get("shop.back"), x + w / 2.0, y + h / 2.0 + 5, w * 0.76);
    }

    private void trackHover(String controlId, boolean hovered) {
        if (!hovered) return;
        String key = currentHoverScope + ":" + controlId;
        if (!hoveredControlsLastFrame.contains(key) && hoveredControlsThisFrame.add(key)) {
            AudioManager.getInstance().playSfx("ui_hover.wav");
        } else {
            hoveredControlsThisFrame.add(key);
        }
    }

    private void renderDimmer(DrawContext gc, double opacity) {
        gc.setFill(Color.rgb(0, 0, 0, opacity));
        gc.fillRect(0, 0, width, height);
    }

    private void drawUiPanel(DrawContext gc, double x, double y, double w, double h) {
        gc.setFill(new LinearGradient(x,y,x,y+h,false,CycleMethod.NO_CYCLE,
                new Stop(0,Color.rgb(62,48,40,0.92)),new Stop(1,Color.rgb(31,29,31,0.92))));
        gc.fillRoundRect(x, y, w, h, 18, 18);
        gc.setStroke(Color.rgb(255, 225, 176, 0.58));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(x, y, w, h, 18, 18);
        gc.setStroke(Color.rgb(255,245,216,0.12));gc.setLineWidth(1.0);
        gc.strokeRoundRect(x+5,y+5,w-10,h-10,14,14);
    }

    private void renderCurrencyPill(DrawContext gc, int balance) {
        double x = width - 166, y = 18, w = 148, h = 42;
        gc.setFill(Color.rgb(47, 37, 32, 0.92));
        gc.fillRoundRect(x, y, w, h, 13, 13);
        gc.setStroke(Color.rgb(255, 219, 157, 0.78));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(x, y, w, h, 13, 13);
        AssetManager.drawImage(gc, "/assets/images/currency/fish.png", x + 12, y + 10, 24, 22);
        gc.setFill(Color.web("#FFE08A"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.LEFT);
        ArtText.fillText(gc, Integer.toString(balance), x + 46, y + 27);
    }

    /** Center-crops scene art into a thumbnail so map cards have no empty letterbox area. */
    private boolean drawImageCover(DrawContext gc, String resourcePath,
                                   double x, double y, double destWidth, double destHeight) {
        javafx.scene.image.Image image = AssetManager.getImage(resourcePath);
        if (image == null || image.isError() || destWidth <= 0 || destHeight <= 0) return false;
        double sourceWidth = image.getWidth();
        double sourceHeight = image.getHeight();
        double sourceAspect = sourceWidth / sourceHeight;
        double destAspect = destWidth / destHeight;
        double sx = 0, sy = 0, sw = sourceWidth, sh = sourceHeight;
        if (sourceAspect > destAspect) {
            sw = sourceHeight * destAspect;
            sx = (sourceWidth - sw) / 2.0;
        } else {
            sh = sourceWidth / destAspect;
            sy = (sourceHeight - sh) / 2.0;
        }
        return AssetManager.drawImageRegion(gc, resourcePath, sx, sy, sw, sh,
                x, y, destWidth, destHeight);
    }

    /** Uses the same map art at every window size, keeping its proportions and anchoring the floor. */
    private boolean drawMapBackground(DrawContext gc, String mapId) {
        if (MapBackdropCache.draw(gc, mapId, width, height, MapBackdropCache.Shade.NORMAL)) return true;
        String path = "/assets/images/backgrounds/" + mapId + ".png";
        javafx.scene.image.Image image = AssetManager.getImage(path);
        if (image == null || image.isError() || width <= 0 || height <= 0) return false;
        double sourceWidth = image.getWidth();
        double sourceHeight = image.getHeight();
        double groundY = sourceHeight * switch (mapId) {
            case "kitchen" -> 0.8555;
            case "living_room" -> 0.780;
            case "garden" -> 0.765;
            case "rooftop" -> 0.8555;
            default -> 0.8555;
        };
        double floorY = height * 0.8555;
        double scale = Math.max(width / sourceWidth, height / sourceHeight);
        scale = Math.max(scale, floorY / groundY);
        scale = Math.max(scale, (height - floorY) / (sourceHeight - groundY));
        double drawWidth = sourceWidth * scale;
        double drawX = (width - drawWidth) / 2.0;
        double drawY = floorY - groundY * scale;

        // Crop the source instead of installing a full-viewport Canvas path clip.
        // Fractional fullscreen/DPI transforms otherwise create large alpha masks.
        // The same uniform scale and floor anchor still determine the visible art.
        double sx = Math.max(0, -drawX / scale), sy = Math.max(0, -drawY / scale);
        return AssetManager.drawImageRegion(gc, path, sx, sy,
                Math.min(width / scale, sourceWidth - sx), Math.min(height / scale, sourceHeight - sy),
                0, 0, width, height);
    }
}

