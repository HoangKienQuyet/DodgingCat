package com.dodgingcat;

import com.dodgingcat.game.GameWorld;
import com.dodgingcat.game.Player;
import com.dodgingcat.save.Profile;
import com.dodgingcat.save.SaveStore;
import com.dodgingcat.ui.GameRenderer;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

/** JavaFX owns input and presentation; the simulation and save system are independent. */
public final class DodgingCatApp extends Application {
    private static final double VIEW_WIDTH = 1240, VIEW_HEIGHT = 752;
    private static final String[] SKINS = {"Ginger cat", "Cream cat", "Charcoal cat"};
    private final GameRenderer renderer = new GameRenderer();
    private final Canvas canvas = new Canvas(960, 640);
    private final StackPane arena = new StackPane();
    private final StackPane overlay = new StackPane();
    private final EnumSet<KeyCode> keys = EnumSet.noneOf(KeyCode.class);
    private final Label score = label("000", "hud-value");
    private final Label clock = label("0.0s", "hud-value");
    private final Label fish = label("0", "hud-value amber");
    private final Label scoreCaption = label("SCORE", "hud-caption");
    private final Label fishCaption = label("RUN FISH", "hud-caption");
    private final Label clockCaption = label("TIME", "hud-caption");
    private final Label edition = label("●  SOLO / VERSUS     /     v1.3", "edition");
    private final Label best = label("0", "stat-value");
    private final Label wallet = label("0 fish", "stat-value amber");
    private final Label status = label("READY", "status");
    private final Label mapCaption = label("01  /  GARDEN", "eyebrow amber");
    private final Label mapTitle = label("Trouble in\nthe garden.", "side-title");
    private final Label mapDescription = label("Arrows and poison clouds.\nWatch your step.", "description");
    private final Label buffs = label("Collect power-ups for a helping paw.", "muted small");
    private final Label toast = label("", "toast");
    private final Label saveWarning = label("", "save-warning");
    private final Canvas catPreview = new Canvas(190, 84);
    private final Label selectedCat = label("Ginger cat", "cat-name");
    private final VBox controlsPanel = new VBox(8);
    private final VBox catCard = new VBox(1);
    private final VBox versusCard = new VBox(8);
    private final Label playerOneStatus = label("", "player-one small");
    private final Label playerTwoStatus = label("", "player-two small");
    private SaveStore saves;
    private Profile profile;
    private GameWorld world;
    private Scene scene;
    private Stage stage;
    private AnimationTimer timer;
    private GameWorld.State shownState;
    private boolean shopOpen;
    private boolean boostShop;
    private GameWorld.Mode selectedMode = GameWorld.Mode.SOLO;
    private GameWorld.MapTheme selectedMap = GameWorld.MapTheme.GARDEN;
    private boolean runRecorded;
    private double visualTime;
    private long lastFrame;
    private long seenEvent = -1;
    private double toastUntil;
    private Long fixedSeed;
    private boolean smokeTest;
    private double smokeMinY = Double.POSITIVE_INFINITY;
    private double smokeMinY2 = Double.POSITIVE_INFINITY;

    public static void main(String[] args) { launch(args); }

    @Override public void start(Stage stage) {
        this.stage = stage;
        Map<String, String> args = getParameters().getNamed();
        smokeTest = getParameters().getRaw().contains("--smoke-test");
        if (args.containsKey("seed")) fixedSeed = Long.parseLong(args.get("seed"));
        Path savePath = args.containsKey("save") ? Path.of(args.get("save")) : SaveStore.defaultPath();
        saves = new SaveStore(savePath);
        profile = saves.load();
        world = new GameWorld(seed(), selectedMode, profile.unlockedBoosts(), selectedMap);

        BorderPane content = new BorderPane();
        content.getStyleClass().add("app");
        content.setPrefSize(VIEW_WIDTH, VIEW_HEIGHT);
        content.setMinSize(VIEW_WIDTH, VIEW_HEIGHT);
        content.setMaxSize(VIEW_WIDTH, VIEW_HEIGHT);
        content.setTop(header());
        content.setCenter(body());
        Label footer = label("DODGING CAT   /   JAVA EDITION                         One little cat. One more try.", "footer");
        footer.setPadding(new Insets(0, 24, 0, 24));
        content.setBottom(footer);

        Group scaled = new Group(content);
        StackPane viewport = new StackPane(scaled);
        viewport.setStyle("-fx-background-color: #0f2524;");
        scene = new Scene(viewport, VIEW_WIDTH, VIEW_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/com/dodgingcat/style.css").toExternalForm());
        content.scaleXProperty().bind(Bindings.min(viewport.widthProperty().divide(VIEW_WIDTH), viewport.heightProperty().divide(VIEW_HEIGHT)));
        content.scaleYProperty().bind(content.scaleXProperty());
        stage.setTitle("Dodging Cat — Garden & Kitchen");
        stage.setScene(scene);
        stage.setMinWidth(930);
        stage.setMinHeight(590);
        double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        if (screenWidth < 1260 || screenHeight < 810) {
            stage.setWidth(Math.min(1260, screenWidth * .94));
            stage.setHeight(Math.min(810, screenHeight * .94));
        }
        installInput();
        stage.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) {
                keys.clear();
                if (world.state() == GameWorld.State.PLAYING) world.togglePause();
            }
        });
        stage.show();
        showSaveWarning();
        refreshProfile();
        refreshOverlay();
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                double dt = lastFrame == 0 ? 0 : Math.min(.25, (now - lastFrame) / 1_000_000_000.0);
                lastFrame = now;
                visualTime += dt;
                boolean versus = world.mode() == GameWorld.Mode.VERSUS;
                GameWorld.Input first = versus
                        ? new GameWorld.Input(held(KeyCode.A), held(KeyCode.D), held(KeyCode.SPACE, KeyCode.W), held(KeyCode.S))
                        : new GameWorld.Input(held(KeyCode.A, KeyCode.LEFT), held(KeyCode.D, KeyCode.RIGHT),
                                held(KeyCode.SPACE, KeyCode.W, KeyCode.UP), held(KeyCode.S, KeyCode.DOWN));
                GameWorld.Input second = new GameWorld.Input(held(KeyCode.LEFT), held(KeyCode.RIGHT), held(KeyCode.UP), held(KeyCode.DOWN));
                world.update(dt, first, second);
                if (smokeTest && world.state() == GameWorld.State.PLAYING) smokeMinY = Math.min(smokeMinY, world.player().y());
                if (smokeTest && versus && world.state() == GameWorld.State.PLAYING) smokeMinY2 = Math.min(smokeMinY2, world.players().get(1).y());
                if (world.state() == GameWorld.State.GAME_OVER && !runRecorded) {
                    runRecorded = true;
                    if (world.mode() == GameWorld.Mode.SOLO) {
                        profile.recordRun(world.score(), world.fishCollected());
                        persist();
                    }
                }
                if (shownState != world.state()) refreshOverlay();
                renderer.render(canvas.getGraphicsContext2D(), world, visualTime, profile.selectedSkin());
                renderPreview(catPreview, profile.selectedSkin(), visualTime);
                refreshHud();
            }
        };
        timer.start();
        if (smokeTest) smokeCheck(Path.of(args.getOrDefault("screenshot-dir", "docs/screenshots")));
    }

    private HBox header() {
        Label mark = label("dc", "brand-mark");
        VBox brand = new VBox(0, label("dodging cat", "brand"), label("THE LITTLE SURVIVAL CLUB", "eyebrow"));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(12, mark, brand, spacer, edition);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(12, 24, 12, 24));
        bar.setPrefHeight(76);
        return bar;
    }

    private HBox body() {
        arena.setPrefSize(960, 640); arena.setMinSize(960, 640); arena.setMaxSize(960, 640);
        arena.getStyleClass().add("arena");
        HBox hud = new HBox(24, new VBox(1, scoreCaption, score), new VBox(1, clockCaption, clock), new VBox(1, fishCaption, fish));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button pause = button("Ⅱ", "pause-button", () -> { keys.clear(); world.togglePause(); });
        pause.setId("pause");
        hud.getChildren().addAll(spacer, pause); hud.setAlignment(Pos.CENTER_LEFT);
        hud.setMaxHeight(70); hud.setPadding(new Insets(12, 22, 10, 22));
        StackPane.setAlignment(hud, Pos.TOP_CENTER);
        overlay.setPickOnBounds(false);
        toast.setMouseTransparent(true); toast.setMaxWidth(600);
        StackPane.setAlignment(toast, Pos.TOP_CENTER); StackPane.setMargin(toast, new Insets(78, 0, 0, 0));
        arena.getChildren().addAll(canvas, hud, toast, overlay);
        HBox body = new HBox(16, arena, sidebar());
        body.setPadding(new Insets(0, 24, 0, 24));
        body.setAlignment(Pos.TOP_LEFT);
        return body;
    }

    private VBox sidebar() {
        mapDescription.setWrapText(true);
        VBox map = new VBox(8, mapCaption, mapTitle, mapDescription, status);
        map.getStyleClass().add("side-card");
        HBox stats = new HBox(16, new VBox(6, label("SOLO BEST", "eyebrow"), best), new VBox(6, label("SOLO WALLET", "eyebrow"), wallet));
        stats.getStyleClass().add("side-card");
        controlsPanel.getStyleClass().add("side-card");
        catCard.getChildren().addAll(label("YOUR CAT", "eyebrow"), catPreview, selectedCat);
        catCard.setAlignment(Pos.CENTER); catCard.getStyleClass().add("side-card");
        playerOneStatus.setWrapText(true); playerTwoStatus.setWrapText(true);
        versusCard.getChildren().addAll(label("FISH LEADERBOARD", "eyebrow"), playerOneStatus, playerTwoStatus);
        versusCard.getStyleClass().add("side-card");
        buffs.setWrapText(true);
        saveWarning.setWrapText(true); saveWarning.setVisible(false); saveWarning.setManaged(false);
        refreshModeInfo();
        VBox sidebar = new VBox(12, map, stats, controlsPanel, catCard, versusCard, buffs, saveWarning);
        sidebar.setPrefWidth(216); sidebar.setMinWidth(216);
        return sidebar;
    }

    private void installInput() {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode key = event.getCode();
            boolean firstPress = keys.add(key);
            if (firstPress && (key == KeyCode.ESCAPE || key == KeyCode.P)) {
                if (shopOpen) { shopOpen = false; refreshOverlay(); }
                else world.togglePause();
                keys.clear();
            } else if (firstPress && key == KeyCode.ENTER && !shopOpen) {
                if (world.state() == GameWorld.State.MENU || world.state() == GameWorld.State.GAME_OVER) startRun();
                else if (world.state() == GameWorld.State.PAUSED) { world.togglePause(); keys.clear(); }
            } else if (firstPress && key == KeyCode.R && world.state() == GameWorld.State.GAME_OVER) startRun();
            if (gameKey(key)) event.consume();
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> { keys.remove(event.getCode()); if (gameKey(event.getCode())) event.consume(); });
    }

    private boolean gameKey(KeyCode key) {
        return switch (key) { case A, D, W, S, LEFT, RIGHT, UP, DOWN, SPACE, ESCAPE, P, ENTER, R -> true; default -> false; };
    }

    private boolean held(KeyCode... alternatives) {
        for (KeyCode key : alternatives) if (keys.contains(key)) return true;
        return false;
    }

    private long seed() { return fixedSeed == null ? System.nanoTime() : fixedSeed; }

    private void startRun() {
        world = new GameWorld(seed(), selectedMode, profile.unlockedBoosts(), selectedMap);
        world.start(); keys.clear(); shopOpen = false; runRecorded = false;
        seenEvent = -1;
        smokeMinY = smokeMinY2 = Double.POSITIVE_INFINITY;
        refreshOverlay(); arena.requestFocus();
    }

    private void selectMode(GameWorld.Mode mode) {
        selectedMode = mode;
        world = new GameWorld(seed(), mode, profile.unlockedBoosts(), selectedMap);
        keys.clear(); refreshModeInfo(); refreshOverlay();
    }

    private void selectMap(GameWorld.MapTheme map) {
        selectedMap = map;
        world = new GameWorld(seed(), selectedMode, profile.unlockedBoosts(), selectedMap);
        keys.clear(); refreshModeInfo(); refreshOverlay();
    }

    private void refreshModeInfo() {
        boolean versus = selectedMode == GameWorld.Mode.VERSUS;
        boolean kitchen = selectedMap == GameWorld.MapTheme.KITCHEN;
        mapCaption.setText(kitchen ? "02  /  KITCHEN" : "01  /  GARDEN");
        mapTitle.setText(kitchen ? "Too hot\nto handle." : "Trouble in\nthe garden.");
        mapDescription.setText(kitchen ? "Knives, scissors and gas flames.\nStay out of the heat."
                : "Arrows and poison clouds.\nWatch your step.");
        catCard.setVisible(!versus); catCard.setManaged(!versus);
        versusCard.setVisible(versus); versusCard.setManaged(versus);
        edition.setText(versus ? "●  2 PLAYERS · FISH RACE     /     v1.3" : "●  1 PLAYER · SURVIVAL     /     v1.3");
        controlsPanel.getChildren().setAll(label("CONTROLS", "eyebrow"));
        if (versus) {
            controlsPanel.getChildren().addAll(control("P1  A D", "Move"), control("W / SPACE", "Jump · S drop"),
                    control("P2  ← →", "Move"), control("↑ / ↓", "Jump / Drop"), control("ESC / P", "Pause"));
        } else {
            controlsPanel.getChildren().addAll(control("A D / ← →", "Move"), control("SPACE / ↑", "Jump"),
                    control("S / ↓", "Drop through"), control("ESC / P", "Pause"));
        }
    }

    private void refreshOverlay() {
        shownState = world.state(); overlay.getChildren().clear();
        if (shownState == GameWorld.State.PLAYING) { overlay.setVisible(false); return; }
        overlay.setVisible(true);
        Region shade = new Region(); shade.getStyleClass().add("scrim");
        VBox card = new VBox(11); card.setAlignment(Pos.CENTER);
        card.setMaxWidth(shopOpen ? 670 : 460); card.setMaxHeight(Region.USE_PREF_SIZE);
        card.getStyleClass().add("menu-card");
        if (shopOpen) shopCard(card);
        else if (shownState == GameWorld.State.MENU) {
            card.getChildren().addAll(label("A TINY CAT. A BIG CHALLENGE.", "eyebrow amber"),
                    label("DODGING\nCAT", "game-title"), label("Nine lives? You only get one here.", "menu-subtitle"));
            HBox modes = new HBox(10,
                    button("1 PLAYER", selectedMode == GameWorld.Mode.SOLO ? "selected mode-button" : "secondary mode-button", () -> selectMode(GameWorld.Mode.SOLO)),
                    button("2 PLAYERS · FISH RACE", selectedMode == GameWorld.Mode.VERSUS ? "selected mode-button" : "secondary mode-button", () -> selectMode(GameWorld.Mode.VERSUS)));
            modes.setAlignment(Pos.CENTER); modes.getChildren().get(0).setId("mode-solo"); modes.getChildren().get(1).setId("mode-versus");
            HBox maps = new HBox(10,
                    button("01 · GARDEN", selectedMap == GameWorld.MapTheme.GARDEN ? "selected map-button" : "secondary map-button", () -> selectMap(GameWorld.MapTheme.GARDEN)),
                    button("02 · KITCHEN", selectedMap == GameWorld.MapTheme.KITCHEN ? "selected map-button" : "secondary map-button", () -> selectMap(GameWorld.MapTheme.KITCHEN)));
            maps.setAlignment(Pos.CENTER); maps.getChildren().get(0).setId("map-garden"); maps.getChildren().get(1).setId("map-kitchen");
            Canvas portrait = new Canvas(130, 54); renderPreview(portrait, profile.selectedSkin(), 0);
            Button play = button("START PLAYING    →", "primary", this::startRun); play.setId("play");
            card.getChildren().addAll(portrait, modes, maps, play, shopButton(),
                    label(selectedMode == GameWorld.Mode.VERSUS ? "60 seconds · All power-ups · No wallet rewards" : "Earn fish in Solo to unlock shop items", "muted small"));
        } else if (shownState == GameWorld.State.PAUSED) {
            card.getChildren().addAll(label("TAKE A BREATHER", "eyebrow amber"), label("Paused", "overlay-title"),
                    label("Your cat is waiting for you.", "menu-subtitle"),
                    button("RESUME    →", "primary", () -> { world.togglePause(); keys.clear(); refreshOverlay(); }),
                    button("Leave run · Main menu", "secondary", () -> { world.menu(); keys.clear(); refreshOverlay(); }),
                    label("ESC / ENTER to resume", "muted small"));
        } else {
            boolean versus = world.mode() == GameWorld.Mode.VERSUS;
            String result = versus ? (world.winnerId() == 0 ? "It’s a tie!" : "Player " + world.winnerId() + " wins!") : "Oops, got caught!";
            String resultNumber = versus ? world.player().fishCollected() + " : " + world.players().get(1).fishCollected() : Integer.toString(world.score());
            card.getChildren().addAll(label(versus ? "FISH RACE RESULTS" : "SEE YOU NEXT RUN", "eyebrow amber"), label(result, "overlay-title"),
                    label(resultNumber, "result-score"), label(versus ? "P1 FISH                  P2 FISH" : "SURVIVAL SCORE", "eyebrow"),
                    label(versus ? (world.timeRemaining() <= 0 ? "Time’s up · " : "Both cats are out · ") + "Versus fish do not enter your wallet"
                            : String.format(Locale.ROOT, "%.1f seconds   ·   +%d fish in your Solo wallet", world.elapsed(), world.fishCollected()), "menu-subtitle"),
                    button("PLAY AGAIN    ↻", "primary", this::startRun), shopButton(),
                    button("Main menu", "text-button", () -> { world.menu(); refreshOverlay(); }));
        }
        overlay.getChildren().addAll(shade, card);
    }

    private Button shopButton() {
        Button shop = button("Shop · Cats & power-ups", "secondary", () -> { shopOpen = true; keys.clear(); refreshOverlay(); });
        shop.setId("shop"); return shop;
    }

    private void shopCard(VBox card) {
        card.getChildren().addAll(label("EARN FISH. UNLOCK GOODIES.", "eyebrow amber"), label("The cat’s shop", "overlay-title"),
                label("Solo wallet: " + profile.totalFish() + " fish · Versus fish cannot be spent", "menu-subtitle"));
        Button skins = button("CAT SKINS", boostShop ? "secondary shop-tab" : "selected shop-tab", () -> { boostShop = false; refreshOverlay(); });
        Button boosts = button("POWER-UPS", boostShop ? "selected shop-tab" : "secondary shop-tab", () -> { boostShop = true; refreshOverlay(); });
        skins.setId("shop-skins"); boosts.setId("shop-boosts");
        HBox tabs = new HBox(10, skins, boosts); tabs.setAlignment(Pos.CENTER); card.getChildren().add(tabs);
        if (boostShop) {
            card.getChildren().add(label("Unlock for Solo · All power-ups are available in Versus", "muted small"));
            HBox items = new HBox(12); items.setAlignment(Pos.CENTER);
            for (GameWorld.Boost boost : GameWorld.Boost.values()) {
                boolean owned = profile.isBoostUnlocked(boost);
                String icon = switch (boost) { case SHIELD -> "◇"; case INVINCIBILITY -> "★"; case DOUBLE_FISH -> "×2"; case BOMB -> "✹"; };
                Label badge = label(icon, "boost-badge boost-" + boost.name().toLowerCase(Locale.ROOT));
                Label detail = label(boostDescription(boost), "boost-description"); detail.setWrapText(true); detail.setMaxWidth(128); detail.setMinHeight(50);
                Button buy = button(owned ? "Unlocked" : Profile.boostPrice(boost) + " fish · Unlock", owned ? "selected" : "secondary", () -> {
                    if (profile.unlockBoost(boost)) { persist(); refreshOverlay(); }
                });
                buy.setId("boost-" + boost.name()); buy.setDisable(owned || profile.totalFish() < Profile.boostPrice(boost));
                VBox tile = new VBox(14, badge, label(boostTitle(boost), "cat-name"), detail, buy);
                tile.setAlignment(Pos.CENTER); tile.getStyleClass().add("skin-card"); tile.setPrefWidth(145); tile.setMinWidth(145); tile.setMaxWidth(145);
                items.getChildren().add(tile);
            }
            card.getChildren().add(items);
        } else {
            card.getChildren().add(label("Looks only · Both players always have different colors", "muted small"));
            card.getChildren().add(skinChoices());
        }
        card.getChildren().add(button("←  Back", "text-button", () -> { shopOpen = false; refreshOverlay(); }));
    }

    private HBox skinChoices() {
        HBox choices = new HBox(12); choices.setAlignment(Pos.CENTER);
        for (int i = 0; i < Profile.SKIN_COUNT; i++) {
            final int skin = i;
            Canvas preview = new Canvas(158, 115); renderPreview(preview, skin, 0);
            boolean owned = profile.isUnlocked(skin), selected = profile.selectedSkin() == skin;
            String text = selected ? "Selected" : owned ? "Select cat" : Profile.skinPrice(skin) + " fish · Unlock";
            Button buy = button(text, selected ? "selected" : "secondary", () -> {
                if (profile.buyAndSelectSkin(skin)) { persist(); refreshOverlay(); }
            });
            buy.setId("skin-" + skin);
            buy.setDisable(selected || (!owned && profile.totalFish() < Profile.skinPrice(skin)));
            VBox tile = new VBox(12, preview, label(SKINS[i], "cat-name"), buy);
            tile.setAlignment(Pos.CENTER); tile.getStyleClass().add("skin-card");
            choices.getChildren().add(tile);
        }
        return choices;
    }

    private void refreshHud() {
        boolean versus = world.mode() == GameWorld.Mode.VERSUS;
        scoreCaption.setText(versus ? "P1 · FISH" : "SCORE"); fishCaption.setText(versus ? "P2 · FISH" : "RUN FISH");
        clockCaption.setText(versus ? "TIME LEFT" : "TIME");
        score.setText(String.format(Locale.ROOT, "%03d", versus ? world.player().fishCollected() : world.score()));
        clock.setText(String.format(Locale.ROOT, "%.1fs", versus ? world.timeRemaining() : world.elapsed()));
        fish.setText(Integer.toString(versus ? world.players().get(1).fishCollected() : world.fishCollected()));
        status.setText(switch (world.state()) { case MENU -> "●  READY"; case PAUSED -> "Ⅱ  PAUSED"; case GAME_OVER -> "↻  TRY AGAIN"; case PLAYING -> "●  LEVEL " + world.difficultyLevel(); });
        if (versus) {
            playerOneStatus.setText(playerStatus(world.player())); playerTwoStatus.setText(playerStatus(world.players().get(1)));
            buffs.setText("60 seconds · All power-ups.\nVersus fish stay in this match.");
        } else {
            String active = activeEffects(world.player());
            buffs.setText(active.isEmpty() ? "Unlock power-ups in the shop\nto find them during Solo runs." : active);
        }
        if (world.eventVersion() != seenEvent) { seenEvent = world.eventVersion(); toastUntil = visualTime + 2.4; toast.setText(world.lastEvent()); }
        toast.setVisible(world.state() == GameWorld.State.PLAYING && visualTime < toastUntil);
    }

    private static String playerStatus(Player player) {
        String effects = activeEffects(player);
        return "P" + player.id() + " · " + player.fishCollected() + " fish · " + (player.alive() ? "Playing" : "Eliminated")
                + (player.alive() && !effects.isEmpty() ? "\n" + effects : "");
    }

    private static String activeEffects(Player player) {
        StringBuilder active = new StringBuilder();
        if (player.shieldActive()) active.append("Shield · blocks 1 hit\n");
        if (player.invincibilityRemaining() > 0) active.append(String.format(Locale.ROOT, "Invincible · %.1fs\n", player.invincibilityRemaining()));
        if (player.doubleFishRemaining() > 0) active.append(String.format(Locale.ROOT, "Fish ×2 · %.1fs", player.doubleFishRemaining()));
        return active.toString().strip();
    }

    private static String boostTitle(GameWorld.Boost boost) {
        return switch (boost) { case SHIELD -> "Shield"; case INVINCIBILITY -> "Invincibility"; case DOUBLE_FISH -> "Double fish"; case BOMB -> "Trap bomb"; };
    }

    private static String boostDescription(GameWorld.Boost boost) {
        return switch (boost) {
            case SHIELD -> "Blocks one hit.\nLasts until you get hit.";
            case INVINCIBILITY -> "Avoid all damage\nfor 5 seconds.";
            case DOUBLE_FISH -> "Each fish counts twice\nfor 5 seconds.";
            case BOMB -> "Clears every trap\ncurrently on the map.";
        };
    }

    private void refreshProfile() {
        best.setText(String.format(Locale.ROOT, "%.0f", profile.bestScore())); wallet.setText(profile.totalFish() + " fish");
        selectedCat.setText(SKINS[profile.selectedSkin()]);
    }
    private void persist() { saves.save(profile); showSaveWarning(); refreshProfile(); }
    private void showSaveWarning() {
        String warning = saves.warning(); boolean present = warning != null && !warning.isBlank();
        saveWarning.setText(present ? "Could not fully load or save progress. " + warning : "");
        saveWarning.setVisible(present); saveWarning.setManaged(present);
    }
    private void renderPreview(Canvas target, int skin, double time) {
        var g = target.getGraphicsContext2D(); g.clearRect(0, 0, target.getWidth(), target.getHeight());
        renderer.renderPreview(g, target.getWidth() / 2, target.getHeight() - 10,
                Math.min(2.1, (target.getHeight() - 18) / 42.0), skin, time);
    }
    private static VBox hudStat(String name, Label value) { return new VBox(1, label(name, "hud-caption"), value); }
    private static HBox control(String key, String action) {
        Label keyLabel = label(key, "key"); keyLabel.setMinWidth(90);
        return new HBox(4, keyLabel, label(action, "small"));
    }
    private static Label label(String text, String classes) {
        Label label = new Label(text); label.getStyleClass().addAll(classes.split(" "));
        if (classes.contains("menu-subtitle")) {
            label.setWrapText(true); label.setMaxWidth(380);
            label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        }
        return label;
    }
    private static Button button(String text, String style, Runnable action) {
        Button button = new Button(text); button.getStyleClass().addAll(style.split(" ")); button.setFocusTraversable(false);
        button.setOnAction(event -> action.run()); return button;
    }

    /** Optional desktop integration check: real scene input, buttons, and rendered snapshots. */
    private void smokeCheck(Path output) {
        after(.4, () -> {
            require(world.mapTheme() == GameWorld.MapTheme.GARDEN && scoreCaption.getText().equals("SCORE"), "English Garden menu");
            snapshot(output, "menu");
            key(KeyCode.ENTER, true); key(KeyCode.ENTER, false); key(KeyCode.A, true); key(KeyCode.SPACE, true);
            after(.35, () -> {
                require(world.state() == GameWorld.State.PLAYING && world.player().x() < 460 && smokeMinY < 470,
                        "keyboard movement/jump: state=" + world.state() + " x=" + world.player().x() + " y=" + world.player().y() + " t=" + world.elapsed());
                keys.clear(); snapshot(output, "playing"); key(KeyCode.ESCAPE, true); key(KeyCode.ESCAPE, false);
                double pausedAt = world.elapsed();
                after(.3, () -> {
                    require(world.state() == GameWorld.State.PAUSED && world.elapsed() == pausedAt, "pause freezes simulation");
                    snapshot(output, "paused"); world.menu(); refreshOverlay(); ((Button) scene.lookup("#shop")).fire();
                    after(.3, () -> {
                        require(shopOpen && scene.lookup("#skin-1").isDisabled() == (profile.isUnlocked(1) && profile.selectedSkin() == 1
                                || !profile.isUnlocked(1) && profile.totalFish() < Profile.skinPrice(1)), "skin shop balance gating");
                        snapshot(output, "shop"); smokeBoostShop(output);
                    });
                });
            });
        });
    }

    private void smokeBoostShop(Path output) {
        ((Button) scene.lookup("#shop-boosts")).fire();
        GameWorld.Boost shield = GameWorld.Boost.SHIELD;
        if (!profile.isBoostUnlocked(shield)) {
            require(scene.lookup("#boost-SHIELD").isDisabled() == (profile.totalFish() < Profile.boostPrice(shield)), "boost shop balance gating");
            // Test-only earned currency goes into the isolated --save profile.
            profile.recordRun(0, Profile.boostPrice(shield));
            refreshProfile(); refreshOverlay();
            int balanceBefore = profile.totalFish();
            ((Button) scene.lookup("#boost-SHIELD")).fire();
            require(profile.isBoostUnlocked(shield) && profile.totalFish() == balanceBefore - Profile.boostPrice(shield), "boost purchase");
        }
        require(scene.lookup("#boost-SHIELD").isDisabled(), "unlocked boost cannot charge again");
        require(saves.load().isBoostUnlocked(shield), "boost unlock persisted");
        GameWorld.Boost bomb = GameWorld.Boost.BOMB;
        if (!profile.isBoostUnlocked(bomb)) {
            require(scene.lookup("#boost-BOMB").isDisabled() == (profile.totalFish() < Profile.boostPrice(bomb)), "bomb purchase balance gating");
            profile.recordRun(0, Profile.boostPrice(bomb));
            refreshProfile(); refreshOverlay();
            int balanceBefore = profile.totalFish();
            ((Button) scene.lookup("#boost-BOMB")).fire();
            require(profile.isBoostUnlocked(bomb) && profile.totalFish() == balanceBefore - Profile.boostPrice(bomb), "bomb purchase");
        }
        require(scene.lookup("#boost-BOMB").isDisabled() && saves.load().isBoostUnlocked(bomb), "bomb unlock persisted without repeat purchase");
        snapshot(output, "buff-shop");
        shopOpen = false; world.menu(); refreshOverlay();
        ((Button) scene.lookup("#map-kitchen")).fire();
        require(world.mapTheme() == GameWorld.MapTheme.KITCHEN && mapCaption.getText().contains("KITCHEN"), "kitchen map selector and sidebar");
        snapshot(output, "kitchen-menu");
        ((Button) scene.lookup("#play")).fire();
        require(world.mode() == GameWorld.Mode.SOLO && world.mapTheme() == GameWorld.MapTheme.KITCHEN, "Solo uses selected kitchen map");
        for (int step = 0; step < 35; step++) world.update(GameWorld.FIXED_STEP, new GameWorld.Input(false, true, true, false));
        snapshot(output, "kitchen-playing");
        world.menu(); refreshOverlay();
        ((Button) scene.lookup("#mode-versus")).fire();
        require(selectedMode == GameWorld.Mode.VERSUS && world.players().size() == 2
                && world.mapTheme() == GameWorld.MapTheme.KITCHEN, "versus selector preserves selected map");
        snapshot(output, "versus-menu");
        ((Button) scene.lookup("#play")).fire();
        double x1 = world.player().x(), x2 = world.players().get(1).x();
        key(KeyCode.A, true); key(KeyCode.W, true); key(KeyCode.RIGHT, true); key(KeyCode.UP, true);
        after(.35, () -> {
            require(world.player().x() < x1 - 5 && world.players().get(1).x() > x2 + 5 && smokeMinY < 470 && smokeMinY2 < 470,
                    "independent two-player keyboard movement and jump");
            keys.clear(); snapshot(output, "versus");
            int previousRuns = profile.lastRuns().size(), walletBefore = profile.totalFish();
            double bestBefore = profile.bestScore();
            boolean capturedNotice = false;
            for (int step = 0; step < 120 * 180 && world.state() == GameWorld.State.PLAYING; step++) {
                world.update(GameWorld.FIXED_STEP, smokeChaseFish(world.player()), smokeChaseFish(world.players().get(1)));
                if (!capturedNotice && !world.pickupNotices().isEmpty()) {
                    snapshot(output, "pickup-notice"); capturedNotice = true;
                }
            }
            require(capturedNotice, "pickup feedback rendered at collection location");
            require(world.state() == GameWorld.State.GAME_OVER && (world.timeRemaining() <= 0 || world.players().stream().noneMatch(Player::alive))
                    && world.elapsed() <= GameWorld.VERSUS_DURATION_SECONDS + .01, "versus ends by timeout or elimination");
            int winner = Integer.compare(world.player().fishCollected(), world.players().get(1).fishCollected());
            require(world.winnerId() == (winner == 0 ? 0 : winner > 0 ? 1 : 2), "winner uses fish totals");
            after(.3, () -> {
                require(runRecorded && profile.lastRuns().size() == previousRuns && profile.totalFish() == walletBefore
                        && profile.bestScore() == bestBefore, "versus cannot change solo wallet, best score or history");
                Profile reloaded = saves.load();
                require(reloaded.bestScore() == profile.bestScore() && reloaded.totalFish() == profile.totalFish()
                        && reloaded.isBoostUnlocked(shield) && saves.warning() == null, "saved progress reloads");
                snapshot(output, "game-over");
                int versusFish = world.fishCollected();
                startRun();
                require(world.mapTheme() == GameWorld.MapTheme.KITCHEN && world.mode() == GameWorld.Mode.VERSUS, "replay keeps map and mode");
                world.menu(); refreshOverlay();
                ((Button) scene.lookup("#map-garden")).fire();
                ((Button) scene.lookup("#play")).fire();
                require(world.mapTheme() == GameWorld.MapTheme.GARDEN && world.players().size() == 2, "Garden also supports Versus");
                snapshot(output, "garden-versus");
                System.out.println("UI_SMOKE_OK v1.3: English UI, both selectable maps in Solo/Versus, map-preserving replay, local pickup notice, bomb shop/persistence, versus inputs/time limit/fish winner, solo currency isolation; versusFish=" + versusFish);
                timer.stop(); stage.close(); Platform.exit();
            });
        });
    }

    private GameWorld.Input smokeChaseFish(Player player) {
        if (!player.alive()) return GameWorld.Input.NONE;
        var target = world.pickups().stream().filter(pickup -> pickup.kind() == null)
                .min(java.util.Comparator.comparingDouble(pickup -> Math.abs(pickup.x() - player.x()) + Math.abs(pickup.y() - player.y())));
        if (target.isEmpty()) return GameWorld.Input.NONE;
        double dx = target.get().x() - (player.x() + player.width() / 2);
        return new GameWorld.Input(dx < -10, dx > 10,
                player.grounded() && target.get().y() < player.y() + 10, false);
    }
    private void key(KeyCode key, boolean pressed) {
        javafx.event.Event.fireEvent(scene, new KeyEvent(pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED, "", "", key, false, false, false, false));
    }
    private void after(double seconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.seconds(seconds));
        pause.setOnFinished(event -> {
            try { action.run(); }
            catch (Throwable failure) { failure.printStackTrace(); Platform.exit(); System.exit(1); }
        }); pause.play();
    }
    private void snapshot(Path folder, String name) {
        try {
            Files.createDirectories(folder);
            renderer.render(canvas.getGraphicsContext2D(), world, visualTime, profile.selectedSkin());
            refreshHud(); scene.getRoot().applyCss(); scene.getRoot().layout();
            ImageIO.write(SwingFXUtils.fromFXImage(scene.snapshot(null), null), "png", folder.resolve(name + ".png").toFile());
        } catch (Exception failure) { throw new IllegalStateException("Cannot capture scene", failure); }
    }
    private static void require(boolean condition, String name) { if (!condition) throw new IllegalStateException("UI smoke failed: " + name); }
    @Override public void stop() { if (timer != null) timer.stop(); }
}
