package com.dodgingcat.core;

import com.dodgingcat.audio.AudioManager;
import com.dodgingcat.game.GameWorld;
import com.dodgingcat.game.Player;
import com.dodgingcat.i18n.I18n;
import com.dodgingcat.i18n.Language;
import com.dodgingcat.input.InputManager;
import com.dodgingcat.persistence.GameData;
import com.dodgingcat.persistence.PersistenceManager;
import com.dodgingcat.shop.ItemCategory;
import com.dodgingcat.shop.ShopItem;
import com.dodgingcat.shop.ShopManager;
import com.dodgingcat.shop.GachaSequence;
import com.dodgingcat.ui.GameRenderer;
import com.dodgingcat.ui.Theme;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

import java.util.List;

/**
 * Controller managing game states, input processing (keyboard + mouse), and simulation transitions.
 */
public class GameController {
    public static final double BOOT_TITLE_SECONDS = 2.0;
    public static final double BOOT_LOGO_SECONDS = 2.0;
    private static final String MENU_BGM = "BGM-menu.mp3";
    private static final String KITCHEN_BGM = "kitchen_Pixabay.mp3";
    private static final String LIVING_ROOM_BGM = "Living room.mp3";
    private static final String BEDROOM_BGM = "Bedroom.mp3";
    private static final String GARDEN_BGM = "Garden.mp3";
    private static final String ROOFTOP_BGM = "Rooftop.mp3";
    private static final int VISIBLE_LEADERBOARD_ROWS = 6;
    private static final double GAME_OVER_DELAY_SECONDS = 0.85;
    private double screenWidth;
    private double screenHeight;

    private GameState currentState = GameState.BOOT;
    private GameWorld activeWorld = null;
    private double bootSequenceElapsed;
    private GachaSequence gacha;
    private AudioManager.SfxPlayback gachaSound;
    private final java.util.Random gachaRandom = new java.util.Random();

    private final InputManager inputManager;
    private final PersistenceManager persistenceManager;
    private final ShopManager shopManager;
    private final GameRenderer renderer;
    private final AudioManager audioManager;

    private GameMode pendingGameMode = GameMode.SINGLE_PLAYER;
    private int menuSelectedIndex = 0;
    private int shopTab = 0; // 0 = Skins, 1 = Maps
    private int shopSelectedIndex = 0;
    private int pregameTab = 0; // 0 = Map, 1 = Skin
    private int pregameSelectedMapIndex = 0;
    private int pregameSelectedSkinIndex = 0;
    private int pregameP2SkinIndex = 1;
    private boolean pregameP1SkinReady;
    private boolean pregameP2SkinReady;
    private boolean pregameSkinCursorInitialized;
    private int pregameActiveSkinPlayer = 1;
    private double shopScrollOffset = 0.0;
    private double pregameSkinScrollOffset = 0.0;
    private boolean devMode;
    private Runnable quitAction = () -> System.exit(0);
    private GachaSequence rendererWarmup;
    private final InputManager warmupInput = new InputManager();

    private boolean gameOverSaved = false;
    private int newLeaderboardRank;
    private boolean gameOverTransitionPending;
    private double gameOverDelayTimer;
    private final double[] footstepTimers = new double[2];
    private final String[] activeMovementSounds = new String[2];
    private static final double START_COUNTDOWN_SECONDS = 3.0;
    private double startCountdownElapsed;

    public GameController(double screenWidth, double screenHeight) {
        this(screenWidth, screenHeight, new PersistenceManager());
    }

    public GameController(double screenWidth, double screenHeight, PersistenceManager persistenceManager) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        this.inputManager = new InputManager();
        this.persistenceManager = persistenceManager;
        this.shopManager = new ShopManager(persistenceManager);
        this.renderer = new GameRenderer(screenWidth, screenHeight);
        this.audioManager = AudioManager.getInstance();
        this.audioManager.preloadShopSfx();

        // Sync settings
        GameData data = persistenceManager.getData();
        if (data.getLanguage() != null) {
            I18n.setLanguage(Language.fromCode(data.getLanguage()));
        }
        audioManager.setBgmVolume(data.getBgmVolume());
        audioManager.setSfxVolume(data.getSfxVolume());
    }

    public void update(double dt) {
        renderer.updateAnimationTimer(dt);

        if (inputManager.isMouseJustClicked() && isUiInteractionState(currentState)) {
            audioManager.playSfx("ui_select.wav");
        }

        if (inputManager.isKeyJustPressed(KeyCode.F10)) {
            renderer.toggleFpsCounter();
        }
        if (inputManager.isKeyJustPressed(KeyCode.F9)) toggleDevMode();

        switch (currentState) {
            case BOOT -> updateBoot(dt);
            case MENU -> updateMenu();
            case PRE_GAME_SELECT -> updatePreGameSelect();
            case COUNTDOWN -> updateCountdown(dt);
            case PLAYING_SINGLE, PLAYING_COOP -> updateGameplay(dt);
            case PAUSED -> updatePaused();
            case GAME_OVER -> updateGameOver();
            case SHOP -> updateShop(dt);
            case LEADERBOARD, ABOUT_US -> updateSimpleSubmenu();
        }

        if (devMode) persistenceManager.getData().setTotalFishes(1_000_000);
        syncBgmForState();

        inputManager.clearFrameTransitions();
    }

    private boolean isUiInteractionState(GameState state) {
        return switch (state) {
            case MENU, PRE_GAME_SELECT, PAUSED, GAME_OVER, SHOP, LEADERBOARD, ABOUT_US -> true;
            default -> false;
        };
    }

    private void syncBgmForState() {
        switch (currentState) {
            case MENU, PRE_GAME_SELECT, LEADERBOARD, ABOUT_US, BOOT -> audioManager.playBgm(MENU_BGM);
            case SHOP -> {
                if (shopTab == 1) {
                    List<ShopItem> maps = shopManager.getMapCatalog();
                    int idx = Math.clamp(shopSelectedIndex, 0, maps.size() - 1);
                    String track = getMapBgm(maps.get(idx).getId());
                    if (track == null) audioManager.playBgm(MENU_BGM); else audioManager.playBgm(track);
                } else audioManager.playBgm(MENU_BGM);
            }
            case COUNTDOWN, PLAYING_SINGLE, PLAYING_COOP -> {
                if (activeWorld == null) audioManager.stopBgm();
                else {
                    String mapBgm = getMapBgm(activeWorld.getMapId());
                    if (mapBgm == null) audioManager.stopBgm();
                    else audioManager.playBgm(mapBgm);
                }
            }
            default -> audioManager.stopBgm();
        }
    }

    private void toggleDevMode() {
        devMode = !devMode;
        if (devMode) {
            persistenceManager.getData().setTotalFishes(1_000_000);
            persistenceManager.save();
        } else {
            // F9 is an explicit full profile reset, including scores and unlocks.
            persistenceManager.save(new GameData());
            activeWorld = null;
            currentState = GameState.MENU;
            shopTab = 0;
            shopSelectedIndex = 0;
            shopScrollOffset = 0.0;
            pregameSkinScrollOffset = 0.0;
            cancelGacha();
            audioManager.setBgmVolume(0.7);
            audioManager.setSfxVolume(0.8);
            I18n.setLanguage(Language.EN);
        }
    }

    private void updateBoot(double dt) {
        bootSequenceElapsed += dt;
        if (bootSequenceElapsed >= BOOT_TITLE_SECONDS + BOOT_LOGO_SECONDS) {
            currentState = GameState.MENU;
            rendererWarmup = null;
        }
    }

    private String getMapBgm(String mapId) {
        if (mapId == null) return null;
        return switch (mapId.toLowerCase()) {
            case "kitchen" -> KITCHEN_BGM;
            case "living_room" -> LIVING_ROOM_BGM;
            case "bedroom" -> BEDROOM_BGM;
            case "garden" -> GARDEN_BGM;
            case "rooftop" -> ROOFTOP_BGM;
            default -> null;
        };
    }

    private void updateMenu() {
        double xLeft = 24.0;
        double buttonWidth = 192.0;
        double buttonHeight = 46.0;
        double yTop = 24.0;
        double rowGap = 55.0;
        double xRight = screenWidth - 24.0 - buttonWidth;

        if (inputManager.isClickedInside(xLeft, yTop, buttonWidth, buttonHeight)) {
            currentState = GameState.SHOP;
            shopTab = 0;
            return;
        }
        if (inputManager.isClickedInside(xLeft, yTop + rowGap, buttonWidth, buttonHeight)) { cycleVolume(true); return; }
        if (inputManager.isClickedInside(xLeft, yTop + rowGap * 2.0, buttonWidth, buttonHeight)) { cycleVolume(false); return; }
        if (inputManager.isClickedInside(xRight, yTop, buttonWidth, buttonHeight)) { currentState = GameState.LEADERBOARD; return; }
        if (inputManager.isClickedInside(xRight, yTop + rowGap, buttonWidth, buttonHeight)) {
            I18n.toggleLanguage();
            persistenceManager.getData().setLanguage(I18n.getLanguage().getCode());
            persistenceManager.save();
            return;
        }
        if (inputManager.isClickedInside(xRight, yTop + rowGap * 2.0, buttonWidth, buttonHeight)) { currentState = GameState.ABOUT_US; return; }
        if (inputManager.isClickedInside(screenWidth - 154, screenHeight - 56, 130, 40)) { quitGame(); return; }
        double cx = screenWidth / 2.0;
        if (inputManager.isClickedInside(cx - 280, screenHeight - 198, 220, 76)) executeMenuAction(0);
        else if (inputManager.isClickedInside(cx + 60, screenHeight - 198, 220, 76)) executeMenuAction(1);
    }

    private void cycleVolume(boolean bgm) {
        GameData data = persistenceManager.getData();
        double current = bgm ? audioManager.getBgmVolume() : audioManager.getSfxVolume();
        int nextStep = ((int) Math.round(current * 5.0) + 1) % 6;
        double next = nextStep / 5.0;
        if (bgm) { audioManager.setBgmVolume(next); data.setBgmVolume(next); }
        else { audioManager.setSfxVolume(next); data.setSfxVolume(next); }
        persistenceManager.save();
    }

    private void executeMenuAction(int index) {
        switch (index) {
            case 0 -> {
                pendingGameMode = GameMode.SINGLE_PLAYER;
                currentState = GameState.PRE_GAME_SELECT;
                pregameSkinCursorInitialized = false;
                pregameP1SkinReady = false;
                pregameP2SkinReady = false;
                pregameActiveSkinPlayer = 1;
            }
            case 1 -> {
                pendingGameMode = GameMode.LOCAL_COOP;
                currentState = GameState.PRE_GAME_SELECT;
                pregameSkinCursorInitialized = false;
                pregameP1SkinReady = false;
                pregameP2SkinReady = false;
                pregameActiveSkinPlayer = 1;
            }
            case 2 -> { currentState = GameState.SHOP; shopTab = 0; }
            case 3 -> currentState = GameState.LEADERBOARD;
            case 4 -> currentState = GameState.ABOUT_US;
            case 5 -> quitGame();
        }
    }

    private void quitGame() {
        activeWorld = null;
        cancelGacha();
        audioManager.stopBgm();
        quitAction.run();
    }

    public void setQuitAction(Runnable action) { quitAction = java.util.Objects.requireNonNull(action); }

    /** Exercise native drawing/JIT/texture promotion under the opaque boot screen.
     * This is presentation-only: it never buys/unlocks a skin or plays spin audio. */
    public void warmupRenderer(com.dodgingcat.ui.DrawContext gc, int frame) {
        if (currentState != GameState.BOOT) return;
        if (rendererWarmup == null) rendererWarmup = new GachaSequence(shopManager.getSkinCatalog(),
                shopManager.findItemById("cyber"), new java.util.Random(17));
        if (frame < 90) rendererWarmup.update(0, frame / 90.0, false, false);
        else rendererWarmup.update(.10, 1, true, false);
        renderer.render(gc, GameState.SHOP, null, Theme.fromId(persistenceManager.getData().getSelectedMap()),
                persistenceManager.getData(), shopManager, warmupInput,
                0, 0, 0, 0, 0, 0, 1, false, false, 1, GameMode.SINGLE_PLAYER,
                false, 0, 0, 0, 3, 0, rendererWarmup, 100);
    }

    private void updatePreGameSelect() {
        double cx = screenWidth / 2.0;

        // Tabs click
        if (inputManager.isClickedInside(cx - 190, 95, 180, 38)) {
            pregameTab = 0; // Map
        } else if (inputManager.isClickedInside(cx + 10, 95, 180, 38)) {
            pregameTab = 1; // Skin
        }
        if (inputManager.isKeyJustPressed(KeyCode.TAB)) pregameTab = 1 - pregameTab;

        double skinViewportTop = pendingGameMode == GameMode.LOCAL_COOP ? 170.0 : 155.0;
        double skinGridTop = skinViewportTop + 5.0;
        double skinViewportBottom = screenHeight - 105.0;
        if (pregameTab == 1) {
            List<ShopItem> unlockedSkins = shopManager.getSkinCatalog();
            int rows = (unlockedSkins.size() + 4) / 5;
            double contentHeight = rows == 0 ? 0.0 : 5.0 + rows * 160.0 + (rows - 1) * 15.0;
            double maxScroll = Math.max(0.0, contentHeight - (skinViewportBottom - skinViewportTop));
            if (inputManager.isMouseHovering(0, skinViewportTop, screenWidth, skinViewportBottom - skinViewportTop)) {
                pregameSkinScrollOffset -= inputManager.consumeScrollDeltaY() * 0.65;
            } else {
                inputManager.consumeScrollDeltaY();
            }
            pregameSkinScrollOffset = Math.max(0.0, Math.min(maxScroll, pregameSkinScrollOffset));
        } else {
            pregameSkinScrollOffset = 0.0;
            inputManager.consumeScrollDeltaY();
        }

        GameData data = persistenceManager.getData();

        if (pregameTab == 0) {
            // Select Map
            List<ShopItem> ownedMaps = shopManager.getMapCatalog().stream()
                    .filter(m -> shopManager.isUnlocked(m.getId(), ItemCategory.THEME))
                    .toList();

            double cardW = 160;
            double cardH = 160;
            double startX = cx - (ownedMaps.size() * (cardW + 20) - 20) / 2.0;

            for (int i = 0; i < ownedMaps.size(); i++) {
                double mx = startX + i * (cardW + 20);
                if (inputManager.isClickedInside(mx, 165, cardW, cardH)) {
                    data.setSelectedMap(ownedMaps.get(i).getId());
                    persistenceManager.save();
                    break;
                }
            }
        } else {
            // Select Skin
            List<ShopItem> ownedSkins = shopManager.getSkinCatalog();

            if (pendingGameMode == GameMode.LOCAL_COOP && !ownedSkins.isEmpty()) {
                if (!pregameSkinCursorInitialized) {
                    pregameSelectedSkinIndex = findSkinIndex(ownedSkins, data.getP1Skin());
                    pregameP2SkinIndex = findSkinIndex(ownedSkins, data.getP2Skin());
                    pregameSkinCursorInitialized = true;
                }
                pregameSelectedSkinIndex = Math.clamp(pregameSelectedSkinIndex, 0, ownedSkins.size() - 1);
                pregameP2SkinIndex = Math.clamp(pregameP2SkinIndex, 0, ownedSkins.size() - 1);
                if (inputManager.isKeyJustPressed(KeyCode.A) || inputManager.isKeyJustPressed(KeyCode.D)
                        || inputManager.isKeyJustPressed(KeyCode.W) || inputManager.isKeyJustPressed(KeyCode.S)
                        || inputManager.isKeyJustPressed(KeyCode.SPACE)) pregameActiveSkinPlayer = 1;
                if (inputManager.isKeyJustPressed(KeyCode.LEFT) || inputManager.isKeyJustPressed(KeyCode.RIGHT)
                        || inputManager.isKeyJustPressed(KeyCode.UP) || inputManager.isKeyJustPressed(KeyCode.DOWN)
                        || inputManager.isKeyJustPressed(KeyCode.ENTER)) pregameActiveSkinPlayer = 2;
                int p1Move = getSkinGridMove(ownedSkins.size(), pregameSelectedSkinIndex,
                        KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S);
                int p2Move = getSkinGridMove(ownedSkins.size(), pregameP2SkinIndex,
                        KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN);
                boolean cursorMoved = p1Move != pregameSelectedSkinIndex || p2Move != pregameP2SkinIndex;
                if (p1Move != pregameSelectedSkinIndex) {
                    pregameSelectedSkinIndex = p1Move;
                    pregameP1SkinReady = false;
                }
                if (p2Move != pregameP2SkinIndex) {
                    pregameP2SkinIndex = p2Move;
                    pregameP2SkinReady = false;
                }
                if (inputManager.isKeyJustPressed(KeyCode.SPACE)) {
                    audioManager.playSfx("ui_select.wav");
                    if (pregameP1SkinReady) pregameP1SkinReady = false;
                    else if (shopManager.isUnlocked(ownedSkins.get(pregameSelectedSkinIndex).getId(), ItemCategory.SKIN)) {
                        data.setP1Skin(ownedSkins.get(pregameSelectedSkinIndex).getId());
                        pregameP1SkinReady = true;
                        persistenceManager.save();
                    }
                }
                if (inputManager.isKeyJustPressed(KeyCode.ENTER)) {
                    audioManager.playSfx("ui_select.wav");
                    if (pregameP2SkinReady) pregameP2SkinReady = false;
                    else if (shopManager.isUnlocked(ownedSkins.get(pregameP2SkinIndex).getId(), ItemCategory.SKIN)) {
                        data.setP2Skin(ownedSkins.get(pregameP2SkinIndex).getId());
                        pregameP2SkinReady = true;
                        persistenceManager.save();
                    }
                }
                if (cursorMoved) {
                    int activeCursor = pregameActiveSkinPlayer == 1 ? pregameSelectedSkinIndex : pregameP2SkinIndex;
                    updatePregameSkinScroll(ownedSkins.size(), activeCursor, skinGridTop, skinViewportTop, skinViewportBottom);
                }
            }

            double cardW = 120;
            double cardH = 160;
            int cols = 5;
            double startX = cx - (cols * (cardW + 15) - 15) / 2.0;

            for (int i = 0; i < ownedSkins.size(); i++) {
                int row = i / cols;
                int col = i % cols;
                double sx = startX + col * (cardW + 15);
                double sy = skinGridTop + row * (cardH + 15) - pregameSkinScrollOffset;

                boolean cursorInSkinViewport = inputManager.getMouseY() >= skinViewportTop
                        && inputManager.getMouseY() < skinViewportBottom;
                if (cursorInSkinViewport && inputManager.isClickedInside(sx, sy, cardW, cardH)) {
                    if (!shopManager.isUnlocked(ownedSkins.get(i).getId(), ItemCategory.SKIN)) continue;
                    data.setP1Skin(ownedSkins.get(i).getId());
                    pregameSelectedSkinIndex = i;
                    pregameP1SkinReady = false;
                    pregameActiveSkinPlayer = 1;
                    persistenceManager.save();
                    break;
                }
            }
        }

        // Big START GAME button
        double startBtnW = 260;
        double startBtnH = 50;
        double startBtnX = cx - startBtnW / 2.0;
        double startBtnY = pregameTab == 0 ? 355 : screenHeight - 90;

        if (inputManager.isClickedInside(startBtnX, startBtnY, startBtnW, startBtnH)) {
            startNewGame(pendingGameMode);
            return;
        }

        // Back button
        if (inputManager.isClickedInside(40, screenHeight - 55, 120, 36) || inputManager.isKeyJustPressed(KeyCode.ESCAPE)) {
            currentState = GameState.MENU;
        }
    }

    private void startNewGame(GameMode mode) {
        GameData data = persistenceManager.getData();
        activeWorld = new GameWorld(mode, data.getSelectedMap(), screenWidth, screenHeight, data.getP1Skin(), data.getP2Skin());
        data.setLastPlayedMap(data.getSelectedMap());
        persistenceManager.save();
        startCountdownElapsed = 0.0;
        currentState = GameState.COUNTDOWN;
        gameOverSaved = false;
        newLeaderboardRank = 0;
        gameOverTransitionPending = false;
        gameOverDelayTimer = 0.0;
        footstepTimers[0] = 0.0;
        footstepTimers[1] = 0.0;
        audioManager.playSfx("countdown_tick.wav");
    }

    private void updateCountdown(double dt) {
        if (activeWorld == null) return;
        startCountdownElapsed += dt;
        if (startCountdownElapsed >= START_COUNTDOWN_SECONDS) {
            currentState = activeWorld.getMode() == GameMode.SINGLE_PLAYER
                    ? GameState.PLAYING_SINGLE : GameState.PLAYING_COOP;
        }
    }

    private int getSkinGridMove(int itemCount, int currentIndex, KeyCode left, KeyCode right, KeyCode up, KeyCode down) {
        int cols = 5;
        if (inputManager.isKeyJustPressed(left) && currentIndex % cols > 0) return currentIndex - 1;
        if (inputManager.isKeyJustPressed(right) && currentIndex % cols < cols - 1 && currentIndex + 1 < itemCount) return currentIndex + 1;
        if (inputManager.isKeyJustPressed(up)) return Math.max(0, currentIndex - cols);
        if (inputManager.isKeyJustPressed(down)) return Math.min(itemCount - 1, currentIndex + cols);
        return currentIndex;
    }

    private int findSkinIndex(List<ShopItem> skins, String skinId) {
        for (int i = 0; i < skins.size(); i++) {
            if (skins.get(i).getId().equalsIgnoreCase(skinId)) return i;
        }
        return 0;
    }

    private void updatePregameSkinScroll(int itemCount, int selectedIndex, double gridTop,
                                         double viewportTop, double viewportBottom) {
        int row = selectedIndex / 5;
        double cardTop = gridTop + row * 175.0 - pregameSkinScrollOffset;
        double cardBottom = cardTop + 160.0;
        if (cardTop < viewportTop) pregameSkinScrollOffset -= viewportTop - cardTop;
        else if (cardBottom > viewportBottom) pregameSkinScrollOffset += cardBottom - viewportBottom;
        int rows = (itemCount + 4) / 5;
        double contentHeight = rows == 0 ? 0.0 : gridTop - viewportTop + rows * 160.0 + (rows - 1) * 15.0;
        double maxScroll = Math.max(0.0, contentHeight - (viewportBottom - viewportTop));
        pregameSkinScrollOffset = Math.max(0.0, Math.min(maxScroll, pregameSkinScrollOffset));
    }

    private void updateGameplay(double dt) {
        if (activeWorld == null) return;

        if (activeWorld.isGameOver()) {
            stopMovementAudio();
            if (!gameOverTransitionPending) {
                gameOverTransitionPending = true;
                gameOverDelayTimer = GAME_OVER_DELAY_SECONDS;
            }
            gameOverDelayTimer -= dt;
            if (gameOverDelayTimer <= 0.0) {
                currentState = GameState.GAME_OVER;
                saveRunResults();
            }
            return;
        }

        if (inputManager.isPausePressed()) {
            audioManager.playSfx("ui_select.wav");
            stopMovementAudio();
            currentState = GameState.PAUSED;
            return;
        }

        if (inputManager.isRestartPressed()) {
            startNewGame(activeWorld.getMode());
            return;
        }

        // Player 1 Controls (WASD)
        Player p1 = activeWorld.getPlayers().getFirst();
        boolean[] wasGrounded = new boolean[activeWorld.getPlayers().size()];
        for (int i = 0; i < activeWorld.getPlayers().size(); i++) {
            wasGrounded[i] = activeWorld.getPlayers().get(i).isGrounded();
        }
        if (inputManager.isP1Left() && !inputManager.isP1Right()) {
            p1.moveLeft();
        } else if (inputManager.isP1Right() && !inputManager.isP1Left()) {
            p1.moveRight();
        } else {
            p1.stopHorizontal();
        }
        if (inputManager.isP1Jump()) {
            boolean wasOnGround = p1.isGrounded();
            p1.jump();
            if (wasOnGround) audioManager.playSfx("jump.wav");
        }
        p1.setDroppingDown(inputManager.isP1DropDown());

        // Player 2 Controls (Arrow Keys) in Co-op
        if (activeWorld.getMode() == GameMode.LOCAL_COOP && activeWorld.getPlayers().size() > 1) {
            Player p2 = activeWorld.getPlayers().get(1);
            if (inputManager.isP2Left() && !inputManager.isP2Right()) {
                p2.moveLeft();
            } else if (inputManager.isP2Right() && !inputManager.isP2Left()) {
                p2.moveRight();
            } else {
                p2.stopHorizontal();
            }
            if (inputManager.isP2Jump()) {
                boolean wasOnGround = p2.isGrounded();
                p2.jump();
                if (wasOnGround) audioManager.playSfx("jump.wav");
            }
            p2.setDroppingDown(inputManager.isP2DropDown());
        }

        activeWorld.update(dt);

        for (int i = 0; i < activeWorld.getPlayers().size(); i++) {
            Player player = activeWorld.getPlayers().get(i);
            if (!wasGrounded[i] && player.isGrounded() && player.isAlive()) {
                audioManager.playSfx("land.wav");
            }
            updateFootstepAudio(player, i, dt);
        }

        if (activeWorld.isGameOver()) {
            gameOverTransitionPending = true;
            gameOverDelayTimer = GAME_OVER_DELAY_SECONDS;
        }
    }

    private void updateFootstepAudio(Player player, int playerIndex, double dt) {
        if (!player.isAlive() || !player.isGrounded() || Math.abs(player.getVx()) < 10.0) {
            if (activeMovementSounds[playerIndex] != null) audioManager.stopSfx(activeMovementSounds[playerIndex]);
            activeMovementSounds[playerIndex] = null;
            footstepTimers[playerIndex] = 0.0;
            return;
        }

        String movementSound = switch (player.getSkinId().toLowerCase(java.util.Locale.ROOT)) {
            case "cyber" -> "cyber_move.wav";
            case "ghost", "spirit" -> "spirit_move.wav";
            case "santa" -> "santa_move.wav";
            default -> "movement.wav";
        };
        if (activeMovementSounds[playerIndex] != null && !activeMovementSounds[playerIndex].equals(movementSound)) {
            audioManager.stopSfx(activeMovementSounds[playerIndex]);
            footstepTimers[playerIndex] = 0.0;
        }
        activeMovementSounds[playerIndex] = movementSound;
        footstepTimers[playerIndex] -= dt;
        if (footstepTimers[playerIndex] <= 0.0) {
            audioManager.playSfx(movementSound);
            footstepTimers[playerIndex] = 0.42;
        }
    }

    private void stopMovementAudio() {
        for (int i = 0; i < activeMovementSounds.length; i++) {
            if (activeMovementSounds[i] != null) audioManager.stopSfx(activeMovementSounds[i]);
            activeMovementSounds[i] = null;
            footstepTimers[i] = 0.0;
        }
    }

    private void saveRunResults() {
        if (gameOverSaved || activeWorld == null) return;
        gameOverSaved = true;

        GameData data = persistenceManager.getData();
        // Add fishes strictly once upon game over
        data.addFishes(activeWorld.getFishesCollected());

        int rank = activeWorld.getMode() == GameMode.SINGLE_PLAYER
                ? data.addSingleRun(activeWorld.getScore(), activeWorld.getSurvivalTime())
                : data.addCoopRun(activeWorld.getScore(), activeWorld.getSurvivalTime());
        newLeaderboardRank = rank > 0 && rank <= VISIBLE_LEADERBOARD_ROWS ? rank : 0;
        audioManager.playSfx(newLeaderboardRank > 0 ? "record_new.wav" : "game_over.wav");
        persistenceManager.save();
    }

    private void updatePaused() {
        double cx = screenWidth / 2.0;
        double btnW = 240;
        double btnH = 44;
        double startY = screenHeight / 2.0 - 115;

        // Resume
        if (inputManager.isClickedInside(cx - btnW / 2.0, startY, btnW, btnH) || inputManager.isPausePressed() || inputManager.isActionPressed()) {
            if (inputManager.isPausePressed() || inputManager.isActionPressed()) audioManager.playSfx("ui_select.wav");
            currentState = (activeWorld.getMode() == GameMode.SINGLE_PLAYER) ? GameState.PLAYING_SINGLE : GameState.PLAYING_COOP;
            return;
        }

        // Restart
        if (inputManager.isClickedInside(cx - btnW / 2.0, startY + 50, btnW, btnH) || inputManager.isRestartPressed()) {
            startNewGame(activeWorld.getMode());
            return;
        }

        if (inputManager.isClickedInside(cx - btnW / 2.0, startY + 100, btnW, btnH)) { cycleVolume(true); return; }
        if (inputManager.isClickedInside(cx - btnW / 2.0, startY + 150, btnW, btnH)) { cycleVolume(false); return; }

        // Return to Main Menu (Point 4)
        if (inputManager.isClickedInside(cx - btnW / 2.0, startY + 200, btnW, btnH) || inputManager.isKeyJustPressed(KeyCode.M)) {
            currentState = GameState.MENU;
            activeWorld = null;
            gameOverSaved = false;
        }
    }

    private void updateGameOver() {
        double cx = screenWidth / 2.0;
        double btnW = 200;
        double btnH = 42;
        // Keep hit targets exactly on the rendered Game Over buttons.
        double startY = 340;

        // Play Again
        if (inputManager.isClickedInside(cx - btnW / 2.0, startY, btnW, btnH) || inputManager.isRestartPressed()) {
            if (inputManager.isRestartPressed()) audioManager.playSfx("ui_select.wav");
            startNewGame(activeWorld != null ? activeWorld.getMode() : GameMode.SINGLE_PLAYER);
            return;
        }

        // Return to Menu
        if (inputManager.isClickedInside(cx - btnW / 2.0, startY + 50, btnW, btnH) || inputManager.isKeyJustPressed(KeyCode.ESCAPE)) {
            if (inputManager.isKeyJustPressed(KeyCode.ESCAPE)) audioManager.playSfx("ui_select.wav");
            currentState = GameState.MENU;
            activeWorld = null;
            gameOverSaved = false;
        }
    }

    private void updateShop(double dt) {
        if (gacha != null && gacha.isActive()) {
            GachaSequence.Phase before = gacha.getPhase();
            gachaSound.update(dt);
            gacha.update(dt, gachaSound.getProgress(), gachaSound.isFinished(),
                    inputManager.isMouseJustClicked() || inputManager.isKeyJustPressed(KeyCode.ESCAPE));
            if (before != GachaSequence.Phase.RESULT && gacha.getPhase() == GachaSequence.Phase.RESULT) {
                audioManager.playSfx("congrat.wav");
            }
            inputManager.consumeScrollDeltaY();
            return;
        }
        double cx = screenWidth / 2.0;

        // Tab selection
        if (inputManager.isClickedInside(cx - 170, 115, 160, 36)) {
            shopTab = 0; // Skins
        } else if (inputManager.isClickedInside(cx + 10, 115, 160, 36)) {
            shopTab = 1; // Maps
            shopSelectedIndex = Math.clamp(shopSelectedIndex, 0, shopManager.getMapCatalog().size() - 1);
        }

        if (shopTab == 0 && inputManager.isClickedInside(cx - 150, screenHeight - 66, 300, 48)) {
            if (devMode && persistenceManager.getData().getTotalFishes() < shopManager.getNextGachaPrice()) {
                persistenceManager.getData().setTotalFishes(1_000_000);
            }
            ShopItem winner = shopManager.pullSkin(gachaRandom);
            if (winner != null) {
                // Cosmetic reel randomness cannot change the separately weighted prize draw.
                gacha = new GachaSequence(shopManager.getSkinCatalog(), winner, new java.util.Random());
                audioManager.stopSfx("congrat.wav");
                gachaSound = audioManager.playSfxTracked("spin.wav");
            }
        }

        if (shopTab == 0 && inputManager.isMouseHovering(0, 160, screenWidth, screenHeight - 235)) {
            shopScrollOffset -= inputManager.consumeScrollDeltaY() * 0.65;
        } else {
            inputManager.consumeScrollDeltaY();
        }

        List<ShopItem> items = (shopTab == 0) ? shopManager.getSkinCatalog() : shopManager.getMapCatalog();
        int skinRows = (shopManager.getSkinCatalog().size() + 4) / 5;
        double skinContentHeight = 170 + skinRows * 190.0 - 15.0;
        double viewportBottom = screenHeight - 75.0;
        shopScrollOffset = shopTab == 0
                ? Math.clamp(shopScrollOffset, 0.0, Math.max(0.0, skinContentHeight - viewportBottom))
                : 0.0;

        if (shopTab == 0) {
            // Skins Buy clicks
            int cols = 5;
            double cardW = 140;
            double cardH = 175;
            double startX = cx - (cols * (cardW + 15) - 15) / 2.0;

            for (int i = 0; i < items.size(); i++) {
                ShopItem item = items.get(i);
                int r = i / cols;
                int c = i % cols;
                double sx = startX + c * (cardW + 15);
                double sy = 170 + r * (cardH + 15) - shopScrollOffset;

                // Skins are awarded through the gacha pull, without duplicates.
            }
        } else {
            // Maps Buy clicks
            double cardW = 160;
            double cardH = 200;
            double startX = cx - (items.size() * (cardW + 18) - 18) / 2.0;

            for (int i = 0; i < items.size(); i++) {
                ShopItem item = items.get(i);
                double mx = startX + i * (cardW + 18);
                if (inputManager.isClickedInside(mx, 170, cardW, cardH)) shopSelectedIndex = i;
                boolean owned = shopManager.isUnlocked(item.getId(), ItemCategory.THEME);
                if (!owned) {
                    if (inputManager.isClickedInside(mx + 14, 170 + cardH - 42, cardW - 28, 30)) {
                        if (devMode && persistenceManager.getData().getTotalFishes() < item.getPrice()) {
                            persistenceManager.getData().setTotalFishes(1_000_000);
                        }
                        shopManager.buyItem(item);
                        break;
                    }
                }
            }
        }

        // Back button
        if (inputManager.isClickedInside(40, screenHeight - 50, 120, 36) || inputManager.isKeyJustPressed(KeyCode.ESCAPE)) {
            currentState = GameState.MENU;
            shopTab = 0;
        }
    }

    private void updateSimpleSubmenu() {
        if (inputManager.isClickedInside(40, screenHeight - 55, 120, 36) || inputManager.isKeyJustPressed(KeyCode.ESCAPE)) {
            currentState = GameState.MENU;
        }
    }

    public void render(GraphicsContext gc) { render(new com.dodgingcat.ui.DrawContext(gc)); }

    public void render(com.dodgingcat.ui.DrawContext gc) {
        Theme activeTheme = Theme.fromId(persistenceManager.getData().getSelectedMap());
        renderer.render(gc, currentState, activeWorld, activeTheme, persistenceManager.getData(), shopManager, inputManager,
                menuSelectedIndex, shopTab, shopSelectedIndex, pregameTab, pregameSelectedMapIndex, pregameSelectedSkinIndex,
                pregameP2SkinIndex, pregameP1SkinReady, pregameP2SkinReady, pregameActiveSkinPlayer,
                pendingGameMode, false, shopScrollOffset, pregameSkinScrollOffset,
                startCountdownElapsed, START_COUNTDOWN_SECONDS, newLeaderboardRank,
                gacha, shopManager.getNextGachaPrice());
    }

    public void resizeViewport(double newWidth, double newHeight) {
        if (newWidth <= 0 || newHeight <= 0
                || Math.abs(newWidth - screenWidth) < 0.5 && Math.abs(newHeight - screenHeight) < 0.5) return;
        screenWidth = newWidth;
        screenHeight = newHeight;
        renderer.resize(newWidth, newHeight);
        if (activeWorld != null) activeWorld.resize(newWidth, newHeight);
    }

    public void onWindowFocusLost() {
        inputManager.reset();
        if (currentState == GameState.PLAYING_SINGLE || currentState == GameState.PLAYING_COOP) {
            stopMovementAudio();
            currentState = GameState.PAUSED;
        }
    }

    public void onApplicationExit() {
        activeWorld = null;
        cancelGacha();
        audioManager.stopBgm();
        audioManager.stopLoopingSfx();
    }

    private void cancelGacha() {
        if (gachaSound != null) gachaSound.cancel();
        audioManager.stopSfx("congrat.wav");
        gachaSound = null;
        gacha = null;
    }

    public GachaSequence getGachaSequence() { return gacha; }

    public void setFrameMetrics(double measuredFps, double updateTimeMs, double drawTimeMs) {
        renderer.setFrameMetrics(measuredFps, updateTimeMs, drawTimeMs);
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public GameWorld getActiveWorld() {
        return activeWorld;
    }

    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }
}

