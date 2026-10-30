package com.dodgingcat.ui;

import com.dodgingcat.core.GameMode;
import com.dodgingcat.core.GameState;
import com.dodgingcat.core.GameController;
import com.dodgingcat.audio.AudioManager;
import com.dodgingcat.game.GameWorld;
import com.dodgingcat.input.InputManager;
import com.dodgingcat.i18n.I18n;
import com.dodgingcat.i18n.Language;
import com.dodgingcat.persistence.GameData;
import com.dodgingcat.persistence.PersistenceManager;
import com.dodgingcat.shop.ShopManager;
import com.dodgingcat.shop.GachaSequence;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UiSnapshotTest {
    @Test
    void rendersMainUiStatesAtReferenceSize() throws Exception {
        Path out = Path.of("target", "ui-preview");
        Files.createDirectories(out);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        com.dodgingcat.app.RenderSurface.configureRuntime();
        Platform.startup(() -> {
            Language previousLanguage = I18n.getLanguage();
            boolean previouslyMuted = AudioManager.getInstance().isMuted();
            var persistence = new PersistenceManager(out.resolve("profile.json"));
            GameData data = persistence.getData();
            var shop = new ShopManager(persistence);
            var renderer = new GameRenderer(960, 640);
            var input = new InputManager();
            Canvas canvas = new Canvas(960, 640);
            PauseTransition waitForAssets = new PauseTransition(Duration.seconds(4));
            waitForAssets.setOnFinished(event -> {
                try {
                    for (String phase : new String[]{"1", "2", "3", "go_en", "go_vi"}) {
                        var image = AssetManager.getImage("/assets/images/ui/countdown/" + phase + ".png");
                        assertNotNull(image, "Missing countdown artwork for " + phase);
                        assertFalse(image.isError(), "Invalid countdown artwork for " + phase);
                        assertEquals(1.0, image.getProgress(), 0.001,
                                "Countdown artwork must be loaded during boot before gameplay");
                    }
                    for (String asset : new String[]{"buffs/shield", "currency/fish", "ui/team_tadq"}) {
                        String path = "/assets/images/" + asset + ".png";
                        var original = ImageIO.read(UiSnapshotTest.class.getResource(path));
                        var decoded = AssetManager.getImage(path);
                        assertNotNull(decoded);
                        assertEquals(original.getWidth(), decoded.getWidth(), .001, "Keep original decode detail: " + asset);
                        assertEquals(original.getHeight(), decoded.getHeight(), .001);
                    }
                    render(out, "menu", canvas, renderer, GameState.MENU, shop, data, input, 0, 0, 0);
                    render(out, "shop-skins", canvas, renderer, GameState.SHOP, shop, data, input, 0, 0, 0);
                    render(out, "shop-maps-locked-preview", canvas, renderer, GameState.SHOP, shop, data, input, 1, 3, 0);
                    render(out, "loadout-map", canvas, renderer, GameState.PRE_GAME_SELECT, shop, data, input, 0, 0, 0);
                    render(out, "loadout-skin", canvas, renderer, GameState.PRE_GAME_SELECT, shop, data, input, 0, 0, 1);
                    render(out, "pause", canvas, renderer, GameState.PAUSED, shop, data, input, 0, 0, 0);
                    renderGameOver(out, canvas, renderer, shop, data, input);
                    render(out, "leaderboard", canvas, renderer, GameState.LEADERBOARD, shop, data, input, 0, 0, 0);
                    render(out, "about", canvas, renderer, GameState.ABOUT_US, shop, data, input, 0, 0, 0);
                    renderer.resize(1138, 640);
                    Canvas wideCanvas = new Canvas(1138, 640);
                    render(out, "wide-menu", wideCanvas, renderer, GameState.MENU, shop, data, input, 0, 0, 0);
                    input.onMouseMoved(115, 47);
                    render(out, "wide-menu-hover", wideCanvas, renderer, GameState.MENU, shop, data, input, 0, 0, 0);
                    input.onMouseMoved(-1, -1);
                    render(out, "wide-shop-maps", wideCanvas, renderer, GameState.SHOP, shop, data, input, 1, 3, 0);
                    render(out, "wide-loadout", wideCanvas, renderer, GameState.PRE_GAME_SELECT, shop, data, input, 0, 0, 0);
                    render(out, "wide-pause", wideCanvas, renderer, GameState.PAUSED, shop, data, input, 0, 0, 0);
                    render(out, "wide-about", wideCanvas, renderer, GameState.ABOUT_US, shop, data, input, 0, 0, 0);
                    I18n.setLanguage(Language.VI);
                    render(out, "wide-menu-vi", wideCanvas, renderer, GameState.MENU, shop, data, input, 0, 0, 0);
                    render(out, "wide-shop-vi", wideCanvas, renderer, GameState.SHOP, shop, data, input, 1, 3, 0);
                    render(out, "wide-loadout-vi", wideCanvas, renderer, GameState.PRE_GAME_SELECT, shop, data, input, 0, 0, 0);
                    render(out, "wide-pause-vi", wideCanvas, renderer, GameState.PAUSED, shop, data, input, 0, 0, 0);
                    render(out, "wide-about-vi", wideCanvas, renderer, GameState.ABOUT_US, shop, data, input, 0, 0, 0);
                    I18n.setLanguage(Language.EN);
                    renderNativeScenarios(out, renderer, shop, data, input);
                    renderNativeWorlds(out, renderer, shop, data, input);
                    renderPrizeScenarios(out, renderer, shop, data, input);
                    verifyScaledFullscreen(out, renderer, shop, data, input);
                    renderPickupDiagnostic(out);
                    verifyCoopMouseScroll(out);
                    verifyGachaResultDismissal(out);
                    verifyDevResetDuringSpin(out);
                } catch (Throwable e) {
                    failure.set(e);
                } finally {
                    I18n.setLanguage(previousLanguage);
                    AudioManager.getInstance().setMuted(previouslyMuted);
                    done.countDown();
                    Platform.runLater(Platform::exit);
                }
            });
            waitForAssets.play();
        });
        assertTrue(done.await(45, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError("UI snapshot failed", failure.get());
    }

    private void render(Path out, String name, Canvas canvas, GameRenderer renderer, GameState state,
                        ShopManager shop, GameData data, InputManager input, int shopTab,
                        int selectedMap, int loadoutTab) throws Exception {
        renderer.render(new DrawContext(canvas.getGraphicsContext2D()), state, null, Theme.KITCHEN, data, shop, input,
                0, shopTab, selectedMap, loadoutTab, 0, 0, 0, false, false, 1,
                GameMode.SINGLE_PLAYER, false, 0, 0, 0, 3, 0, null, 100);
        ImageIO.write(SwingFXUtils.fromFXImage(canvas.snapshot(new SnapshotParameters(), null), null),
                "png", out.resolve(name + ".png").toFile());
        saveJava2D(out, name, (int)canvas.getWidth(), (int)canvas.getHeight(), 1, gc ->
                renderer.render(gc, state, null, Theme.KITCHEN, data, shop, input,
                        0, shopTab, selectedMap, loadoutTab, 0, 0, 0, false, false, 1,
                        GameMode.SINGLE_PLAYER, false, 0, 0, 0, 3, 0, null, 100));
    }

    private void renderGameOver(Path out, Canvas canvas, GameRenderer renderer, ShopManager shop,
                                GameData data, InputManager input) throws Exception {
        GameWorld world = new GameWorld(GameMode.SINGLE_PLAYER, "kitchen", 960, 640, "ginger", "tuxedo");
        renderer.render(new DrawContext(canvas.getGraphicsContext2D()), GameState.GAME_OVER, world, Theme.KITCHEN,
                data, shop, input, 0, 0, 0, 0, 0, 0, 0, false, false, 1,
                GameMode.SINGLE_PLAYER, false, 0, 0, 0, 3, 0, null, 100);
        ImageIO.write(SwingFXUtils.fromFXImage(canvas.snapshot(new SnapshotParameters(), null), null),
                "png", out.resolve("game-over.png").toFile());
    }

    private void renderNativeScenarios(Path out, GameRenderer renderer, ShopManager shop,
                                       GameData data, InputManager input) throws Exception {
        for (Language language : Language.values()) {
            I18n.setLanguage(language);
            String suffix = language == Language.VI ? "vi" : "en";
            renderNative(out, "native-about-" + suffix, renderer, shop, data, input,
                    GameState.ABOUT_US, GameMode.SINGLE_PLAYER, 0, 0, false, 0, "");
            renderNative(out, "native-countdown-3-" + suffix, renderer, shop, data, input,
                    GameState.COUNTDOWN, GameMode.SINGLE_PLAYER, 0, 0.35, false, 0, "");
            renderNative(out, "native-countdown-go-" + suffix, renderer, shop, data, input,
                    GameState.COUNTDOWN, GameMode.SINGLE_PLAYER, 0, 2.85, false, 0, "");
            renderNative(out, "native-coop-skins-" + suffix, renderer, shop, data, input,
                    GameState.PRE_GAME_SELECT, GameMode.LOCAL_COOP, 1, 0, false, 0, "");
            renderNative(out, "native-gacha-result-" + suffix, renderer, shop, data, input,
                    GameState.SHOP, GameMode.SINGLE_PLAYER, 0, 0, true, 3.2,
                    shop.getSkinCatalog().get(3).getDisplayName(language == Language.VI));
        }
        String previousP2 = data.getP2Skin();
        data.setP2Skin(data.getP1Skin());
        I18n.setLanguage(Language.EN);
        renderNative(out, "native-coop-shared-skin", renderer, shop, data, input,
                GameState.PRE_GAME_SELECT, GameMode.LOCAL_COOP, 1, 0, false, 0, "");
        data.setP2Skin(previousP2);
        var logoRenderer = new GameRenderer(1138, 640);
        logoRenderer.updateAnimationTimer(GameController.BOOT_TITLE_SECONDS + 0.5);
        renderNative(out, "native-boot-logo", logoRenderer, shop, data, input,
                GameState.BOOT, GameMode.SINGLE_PLAYER, 0, 0, false, 0, "");
    }

    private void renderPickupDiagnostic(Path out) throws Exception {
        Canvas canvas = new Canvas(960, 250);
        var gc = new DrawContext(canvas.getGraphicsContext2D());
        gc.setFill(javafx.scene.paint.Color.web("#312722"));
        gc.fillRect(0, 0, 960, 250);
        gc.setFont(javafx.scene.text.Font.font(16));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        String[] names = {"Fish", "Can", "Shield", "Invincible", "Heart", "Double", "Bomb"};
        String[] paths = {"currency/fish", "currency/fish_can", "buffs/shield", "buffs/invincibility",
                "buffs/heart", "buffs/double_score", "buffs/bomb"};
        for (int i = 0; i < paths.length; i++) {
            double x = 32 + i * 130;
            gc.setStroke(javafx.scene.paint.Color.web("#BB9461"));
            gc.strokeRect(x - 2, 48, 108, 108);
            assertTrue(AssetManager.drawImageContent(gc, "/assets/images/" + paths[i] + ".png",
                    x, 50, 104, 104), "Pickup artwork must load with valid content bounds: " + paths[i]);
            ArtText.fillText(gc, names[i], x + 52, 190, 116);
        }
        save(out, "pickup-crop-diagnostic", canvas);
    }

    private void renderNativeWorlds(Path out, GameRenderer renderer, ShopManager shop,
                                    GameData data, InputManager input) throws Exception {
        double scale=1080.0/640, logicalWidth=1920/scale;
        renderer.resize(logicalWidth,640);
        for (Theme theme : new Theme[]{Theme.KITCHEN,Theme.GARDEN,Theme.ROOFTOP}) {
            var world=new GameWorld(GameMode.LOCAL_COOP,theme.getId(),logicalWidth,640,"cyber","santa");
            world.getPlayers().get(0).applyBuff(com.dodgingcat.buffs.BuffType.SHIELD);
            world.getPlayers().get(1).applyBuff(com.dodgingcat.buffs.BuffType.INVINCIBILITY);
            world.update(1.0/60);
            world.getBuffs().add(new com.dodgingcat.buffs.Buff(com.dodgingcat.buffs.BuffType.HEART,300,450));
            world.getFishes().add(new com.dodgingcat.game.FishItem(com.dodgingcat.game.FishType.SINGLE_FISH,450,450));
            java.util.function.Consumer<DrawContext> scene=gc -> renderer.render(gc,GameState.PLAYING_COOP,world,theme,
                    data,shop,input,0,0,0,0,0,0,1,false,false,1,GameMode.LOCAL_COOP,false,
                    0,0,0,3,0,null,100);
            String name="native-gameplay-"+theme.getId();
            Canvas canvas=new Canvas(1920,1080);
            var gc=new DrawContext(canvas.getGraphicsContext2D());gc.save();gc.scale(scale,scale);scene.accept(gc);gc.restore();
            save(out,name,canvas);saveJava2D(out,name,1920,1080,scale,scene);
        }
    }

    private void renderPrizeScenarios(Path out, GameRenderer renderer, ShopManager shop,
                                      GameData data, InputManager input) throws Exception {
        I18n.setLanguage(Language.EN);
        renderer.resize(1138, 640);
        Canvas canvas = new Canvas(1138, 640);
        for (String skin : new String[]{"snow_white", "lava", "cyber", "ghost", "santa"}) {
            var sequence = new GachaSequence(shop.getSkinCatalog(), shop.findItemById(skin), new java.util.Random(23));
            sequence.update(0, 1, true, false);
            renderPrize(out, "landed-" + skin, canvas, renderer, shop, data, input, sequence);
            sequence.update(GachaSequence.LANDING_SECONDS, 1, true, false);
            if (sequence.getPhase() == GachaSequence.Phase.LEGENDARY_REVEAL) {
                sequence.update(.65, 1, true, false);
                renderPrize(out, "vortex-" + skin, canvas, renderer, shop, data, input, sequence);
                sequence.update(.65, 1, true, false);
                renderPrize(out, "burst-" + skin, canvas, renderer, shop, data, input, sequence);
                sequence.update(.51, 1, true, false);
            }
            sequence.update(.35, 1, true, false);
            renderPrize(out, "prize-" + skin, canvas, renderer, shop, data, input, sequence);
        }
        var singles = data.getSingleLeaderboard(); var coop = data.getCoopLeaderboard();
        var originalSingles = new java.util.ArrayList<>(singles);
        var originalCoop = new java.util.ArrayList<>(coop);
        try {
            singles.clear(); coop.clear();
            for (int i = 0; i < 5; i++) {
                singles.add(new com.dodgingcat.persistence.LeaderboardEntry("P1", 244 - i * 23, 19.5 - i));
                coop.add(new com.dodgingcat.persistence.LeaderboardEntry("P1 + P2", 500 - i * 45, 32.5 - i));
            }
            render(out, "leaderboard-populated", canvas, renderer, GameState.LEADERBOARD, shop, data, input, 0, 0, 0);
        } finally { singles.clear(); singles.addAll(originalSingles); coop.clear(); coop.addAll(originalCoop); }
    }

    private void renderPrize(Path out, String name, Canvas canvas, GameRenderer renderer, ShopManager shop,
                              GameData data, InputManager input, GachaSequence sequence) throws Exception {
        renderer.render(new DrawContext(canvas.getGraphicsContext2D()), GameState.SHOP, null, Theme.KITCHEN, data, shop, input,
                0, 0, 0, 0, 0, 0, 1, false, false, 1, GameMode.SINGLE_PLAYER, false,
                0, 0, 0, 3, 0, sequence, 100);
        save(out, name, canvas);
        saveJava2D(out, name, (int)canvas.getWidth(), (int)canvas.getHeight(), 1, gc ->
                renderer.render(gc, GameState.SHOP, null, Theme.KITCHEN, data, shop, input,
                        0, 0, 0, 0, 0, 0, 1, false, false, 1, GameMode.SINGLE_PLAYER, false,
                        0, 0, 0, 3, 0, sequence, 100));
    }

    private void verifyScaledFullscreen(Path out, GameRenderer renderer, ShopManager shop,
                                         GameData data, InputManager input) throws Exception {
        Canvas canvas = new Canvas(960, 640);
        var root = new javafx.scene.layout.StackPane(canvas);
        root.resize(1920, 1080);
        assertEquals(0, com.dodgingcat.app.RenderSurface.DEFAULT_HEIGHT_LIMIT, "Production must render at native resolution");
        com.dodgingcat.app.RenderSurface.resize(canvas, 1920, 1080, com.dodgingcat.app.RenderSurface.DEFAULT_HEIGHT_LIMIT);
        root.layout();
        assertEquals(1920, canvas.getWidth(), .001);
        assertEquals(1080, canvas.getHeight(), .001);
        assertEquals(1, canvas.getScaleX(), .001);
        assertEquals(1, canvas.getScaleY(), .001);
        assertEquals(0, canvas.getBoundsInParent().getMinX(), .001);
        assertEquals(0, canvas.getBoundsInParent().getMinY(), .001);
        assertEquals(1920, canvas.getBoundsInParent().getWidth(), .001);
        assertEquals(1080, canvas.getBoundsInParent().getHeight(), .001);
        renderer.resize(1920.0 / 1080 * 640, 640);
        for (var state : new GameState[]{GameState.MENU, GameState.SHOP}) {
            var gc = new DrawContext(canvas.getGraphicsContext2D());
            gc.save(); gc.scale(1080.0 / 640, 1080.0 / 640);
            renderer.render(gc, state, null, Theme.KITCHEN, data, shop, input,
                    0, 0, 0, 0, 0, 0, 1, false, false, 1, GameMode.SINGLE_PLAYER,
                    false, 0, 0, 0, 3, 0, null, 100);
            gc.restore();
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(new SnapshotParameters(), null), null),
                    "png", out.resolve("native-fullscreen-1080-" + state.name().toLowerCase() + ".png").toFile());
        }
        // F11 back to the ordinary viewport must remove the upscale and preserve the full hit area.
        root.resize(960, 640);
        com.dodgingcat.app.RenderSurface.resize(canvas, 960, 640, com.dodgingcat.app.RenderSurface.DEFAULT_HEIGHT_LIMIT);
        root.layout();
        assertEquals(960, canvas.getBoundsInParent().getWidth(), .001);
        assertEquals(640, canvas.getBoundsInParent().getHeight(), .001);
        assertEquals(1, canvas.getScaleX(), .001);
        assertEquals(1, canvas.getScaleY(), .001);
    }

    private void renderNative(Path out, String name, GameRenderer renderer, ShopManager shop,
                              GameData data, InputManager input, GameState state, GameMode mode,
                              int loadoutTab, double countdownElapsed, boolean gachaVisible,
                              double gachaElapsed, String winnerName) throws Exception {
        Canvas canvas = new Canvas(1920, 1080);
        double scale = 1080.0 / 640.0;
        renderer.resize(1920.0 / scale, 640);
        var gc = new DrawContext(canvas.getGraphicsContext2D());
        gc.save();
        gc.scale(scale, scale);
        GachaSequence sequence = null;
        if (gachaVisible) {
            var winner = shop.getSkinCatalog().stream().filter(item -> item.getDisplayName(I18n.getLanguage() == Language.VI).equals(winnerName)).findFirst().orElseThrow();
            sequence = new GachaSequence(shop.getSkinCatalog(), winner, new java.util.Random(17));
            sequence.update(0, 1, true, false);
            sequence.update(GachaSequence.LANDING_SECONDS, 1, true, false);
            if (sequence.getPhase() == GachaSequence.Phase.LEGENDARY_REVEAL) sequence.update(GachaSequence.LEGENDARY_SECONDS, 1, true, false);
        }
        renderer.render(gc, state, null, Theme.KITCHEN, data, shop, input,
                0, 0, 0, loadoutTab, 0, 0, 1, false, false, 1,
                mode, false, 0, 0, countdownElapsed, 3, 0, sequence, 100);
        gc.restore();
        save(out, name, canvas);
        GachaSequence renderedSequence = sequence;
        saveJava2D(out, name, 1920, 1080, scale, context ->
                renderer.render(context, state, null, Theme.KITCHEN, data, shop, input,
                        0, 0, 0, loadoutTab, 0, 0, 1, false, false, 1,
                        mode, false, 0, 0, countdownElapsed, 3, 0, renderedSequence, 100));
    }

    private void saveJava2D(Path out, String name, int width, int height, double scale,
                           java.util.function.Consumer<DrawContext> drawing) throws Exception {
        var image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try { graphics.scale(scale, scale); drawing.accept(new DrawContext(graphics)); }
        finally { graphics.dispose(); }
        ImageIO.write(image, "png", out.resolve("java2d-" + name + ".png").toFile());
        // Allow AA and native-pixel rounding differences, but reject missing artwork,
        // transform/clip mistakes, and unintended color changes between backends.
        var reference = ImageIO.read(out.resolve(name + ".png").toFile());
        long error = 0;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int actual = image.getRGB(x, y), expected = reference.getRGB(x, y);
            for (int channel = 0; channel < 3; channel++)
                error += Math.abs(((actual >> channel * 8) & 255) - ((expected >> channel * 8) & 255));
        }
        double mean = error / (double)(width * height * 3);
        assertTrue(mean < 5, "Rendering changed for " + name + "; mean RGB error=" + mean);
    }

    private void verifyCoopMouseScroll(Path out) throws Exception {
        GameController controller = isolatedController(out.resolve("coop-regression-profile.json"));
        try {
            controller.update(4.1);
            click(controller, 617, 485);
            assertEquals(GameState.PRE_GAME_SELECT, controller.getCurrentState());
            click(controller, 580, 114);
            InputManager input = controller.getInputManager();
            input.onMouseMoved(480, 300);
            input.onMouseScroll(-120);
            controller.update(1.0 / 60.0);
            double mouseOffset = privateDouble(controller, "pregameSkinScrollOffset");
            assertTrue(mouseOffset > 0, "Mouse wheel must scroll the co-op skin grid");
            for (int i = 0; i < 20; i++) controller.update(1.0 / 60.0);
            assertEquals(mouseOffset, privateDouble(controller, "pregameSkinScrollOffset"), 0.001,
                    "Idle keyboard cursors must not snap the mouse-scrolled grid back");
            for (int i = 0; i < 2; i++) {
                input.onKeyPressed(KeyCode.S);
                controller.update(1.0 / 60.0);
                input.onKeyReleased(KeyCode.S);
            }
            assertTrue(privateDouble(controller, "pregameSkinScrollOffset") > mouseOffset,
                    "Keyboard navigation must still reveal the newly selected row");
            Canvas canvas = new Canvas(960, 640);
            controller.render(canvas.getGraphicsContext2D());
            save(out, "coop-after-mouse-and-keyboard-scroll", canvas);
        } finally {
            controller.onApplicationExit();
        }
    }

    private void verifyGachaResultDismissal(Path out) throws Exception {
        GameController controller = isolatedController(out.resolve("gacha-regression-profile.json"));
        try {
            controller.update(4.1);
            click(controller, 100, 47);
            click(controller, 480, 598);
            assertEquals(1, controller.getPersistenceManager().getData().getSkinGachaPulls());
            finishSpin(controller);
            var sequence = controller.getGachaSequence();
            assertEquals(GachaSequence.Phase.LANDED, sequence.getPhase());
            assertSame(sequence.getWinner(), sequence.getReel().get(sequence.getWinningIndex()));
            controller.update(GachaSequence.LANDING_SECONDS);
            if (sequence.getPhase() == GachaSequence.Phase.LEGENDARY_REVEAL) controller.update(GachaSequence.LEGENDARY_SECONDS);
            assertEquals(GachaSequence.Phase.RESULT, sequence.getPhase());
            Canvas canvas = new Canvas(960, 640);
            controller.render(canvas.getGraphicsContext2D());
            save(out, "gacha-controller-result", canvas);
            controller.update(0.75);
            click(controller, 480, 598);
            assertFalse(sequence.isActive());
            assertEquals(1, controller.getPersistenceManager().getData().getSkinGachaPulls(), "Dismiss cannot buy a second pull");
            click(controller, 480, 598);
            finishSpin(controller);
            sequence = controller.getGachaSequence();
            controller.update(GachaSequence.LANDING_SECONDS);
            if (sequence.getPhase() == GachaSequence.Phase.LEGENDARY_REVEAL) controller.update(GachaSequence.LEGENDARY_SECONDS);
            controller.update(1.99);
            assertEquals(GachaSequence.Phase.RESULT, sequence.getPhase());
            controller.update(0.02);
            assertFalse(sequence.isActive());
            controller.render(canvas.getGraphicsContext2D());
            save(out, "gacha-controller-after-dismiss", canvas);
        } finally { controller.onApplicationExit(); }
    }

    private void finishSpin(GameController controller) throws Exception {
        Field field = GameController.class.getDeclaredField("gachaSound");
        field.setAccessible(true);
        var sound = (AudioManager.SfxPlayback) field.get(controller);
        long deadline = System.nanoTime() + 2_000_000_000L;
        while (!sound.isReady() && System.nanoTime() < deadline) Thread.sleep(5);
        assertTrue(sound.isReady(), "Spin WAV must decode off the game thread");
        assertEquals(11.312, sound.getDurationSeconds(), 0.001);
        controller.update(sound.getDurationSeconds() - 0.1);
        assertEquals(GachaSequence.Phase.SPINNING, controller.getGachaSequence().getPhase());
        controller.update(0.11);
    }

    private void verifyDevResetDuringSpin(Path out) {
        GameController controller = isolatedController(out.resolve("dev-reset-profile.json"));
        try {
            controller.update(4.1);
            pressF9(controller);
            assertEquals(1_000_000, controller.getPersistenceManager().getData().getTotalFishes());
            click(controller, 100, 47); click(controller, 480, 598);
            assertNotNull(controller.getGachaSequence());
            pressF9(controller);
            var data = controller.getPersistenceManager().getData();
            assertEquals(GameState.MENU, controller.getCurrentState());
            assertNull(controller.getGachaSequence(), "Reset must cancel pending native spin audio and modal state");
            assertEquals(0, data.getTotalFishes());
            assertEquals(0, data.getSkinGachaPulls());
            assertEquals(java.util.List.of("ginger", "tuxedo"), data.getUnlockedSkins());
            assertEquals(java.util.List.of("kitchen"), data.getUnlockedMaps());
            assertTrue(data.getSingleLeaderboard().isEmpty());
            assertTrue(data.getCoopLeaderboard().isEmpty());
        } finally { controller.onApplicationExit(); }
    }

    private void pressF9(GameController controller) {
        controller.getInputManager().onKeyPressed(KeyCode.F9);
        controller.update(1.0 / 60);
        controller.getInputManager().onKeyReleased(KeyCode.F9);
        controller.update(1.0 / 60);
    }

    private GameController isolatedController(Path profile) {
        PersistenceManager persistence = new PersistenceManager(profile);
        GameData freshData = new GameData();
        freshData.setTotalFishes(10_000);
        persistence.save(freshData);
        AudioManager.getInstance().setMuted(true);
        return new GameController(960, 640, persistence);
    }

    private void click(GameController controller, double x, double y) {
        controller.getInputManager().onMousePressed(x, y);
        controller.update(1.0 / 60.0);
        controller.getInputManager().onMouseReleased(x, y);
    }

    private double privateDouble(GameController controller, String name) throws Exception {
        Field field = GameController.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getDouble(controller);
    }

    private void save(Path out, String name, Canvas canvas) throws Exception {
        ImageIO.write(SwingFXUtils.fromFXImage(canvas.snapshot(new SnapshotParameters(), null), null),
                "png", out.resolve(name + ".png").toFile());
    }
}
