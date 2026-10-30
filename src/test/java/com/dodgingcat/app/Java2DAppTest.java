package com.dodgingcat.app;

import com.dodgingcat.audio.AudioManager;
import com.dodgingcat.core.GameController;
import com.dodgingcat.core.GameState;
import com.dodgingcat.persistence.GameData;
import com.dodgingcat.persistence.PersistenceManager;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Frame;
import java.awt.event.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Runs alone: exercises the actual native window/input/media bridge with a disposable profile. */
@EnabledIfSystemProperty(named="dodgingcat.awtAppProbe", matches="true")
class Java2DAppTest {
    @Test void nativeWindowHandlesInputModeSwitchesAndAudio() throws Exception {
        var profile = new PersistenceManager(Path.of("target/java2d-app-profile.json"));
        profile.save(new GameData());
        var app = new Java2DApp(profile);
        var failure = new AtomicReference<Throwable>();
        var thread = new Thread(() -> { try { app.run(); } catch (Throwable e) { failure.set(e); } }, "native-app-test");
        thread.start();
        try {
            waitFor(() -> get(app,"window") != null, 15);
            waitFor(() -> state(app) == GameState.BOOT, 5);
            waitFor(() -> state(app) == GameState.MENU, 20);
            double bootSeconds=query(app, () -> ((long)get(app,"bootFinishedAt")-(long)get(app,"bootStartedAt"))/1e9);
            assertTrue(bootSeconds>=4&&bootSeconds<4.5,"Boot must last about 4s, including warmup: "+bootSeconds);
            System.out.printf(java.util.Locale.ROOT,"Native boot to menu: %.3fs%n",bootSeconds);
            var surface = (Canvas)get(app,"surface");
            var window = (Frame)get(app,"window");
            assertTrue(window.isUndecorated());
            double fullWidth = surface.getWidth(), fullHeight = surface.getHeight();
            verifyBgm("BGM-menu.mp3");

            key(surface, KeyEvent.VK_F11);
            waitFor(() -> !window.isUndecorated(), 5);
            assertTrue(surface.getWidth() < fullWidth || surface.getHeight() < fullHeight);
            click(surface, width(surface)/2 + 120, 480);
            waitFor(() -> state(app) == GameState.PRE_GAME_SELECT, 5);
            key(surface, KeyEvent.VK_TAB);
            move(surface, width(surface)/2, 300);
            SwingUtilities.invokeAndWait(() -> {
                var wheel = new MouseWheelEvent(surface,MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),0,
                        surface.getWidth()/2,surface.getHeight()/2,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,3,1);
                for(var listener:surface.getMouseWheelListeners()) listener.mouseWheelMoved(wheel);
            });
            waitFor(() -> query(app, () -> (double)get(controller(app),"pregameSkinScrollOffset")) > 0, 5);
            key(surface, KeyEvent.VK_TAB);
            click(surface, width(surface)/2, 380);
            waitFor(() -> state(app) == GameState.PLAYING_COOP, 7);
            verifyBgm("kitchen_Pixabay.mp3");
            double before = query(app, () -> controller(app).getActiveWorld().getPlayers().get(0).getX());
            keyEvent(surface, KeyEvent.VK_D, true); Thread.sleep(150); keyEvent(surface, KeyEvent.VK_D, false);
            assertTrue(query(app, () -> controller(app).getActiveWorld().getPlayers().get(0).getX()) > before);
            key(surface, KeyEvent.VK_ESCAPE);
            waitFor(() -> state(app) == GameState.PAUSED, 5);
            key(surface, KeyEvent.VK_F11);
            waitFor(window::isUndecorated, 5);
            assertEquals(fullWidth, surface.getWidth(), 1); assertEquals(fullHeight, surface.getHeight(), 1);
            assertEquals(GameState.PAUSED, state(app));
            key(surface, KeyEvent.VK_M); waitFor(() -> state(app) == GameState.MENU, 5);

            click(surface,100,47); waitFor(() -> state(app) == GameState.SHOP,5);
            click(surface,width(surface)/2+90,133);
            click(surface,width(surface)/2,250); // Locked bedroom preview still changes BGM.
            verifyBgm("Bedroom.mp3");
            key(surface,KeyEvent.VK_F9);
            waitFor(() -> query(app, () -> profile.getData().getTotalFishes()) == 1_000_000,5);
            AudioManager.getInstance().setSfxVolume(.001);
            click(surface,width(surface)/2-90,133);
            click(surface,width(surface)/2,598);
            waitFor(() -> query(app, () -> controller(app).getGachaSequence()) != null,5);
            var fps = new java.util.ArrayList<Double>();
            var timings = new StringBuilder();
            long spinStart=System.nanoTime();
            while(query(app, () -> controller(app).getGachaSequence().getPhase()) == com.dodgingcat.shop.GachaSequence.Phase.SPINNING
                    && System.nanoTime()-spinStart<20_000_000_000L) {
                if(System.nanoTime()-spinStart>2_000_000_000L) {
                    fps.add(query(app, () -> (double)get(get(controller(app),"renderer"),"measuredFps")));
                    timings.append(query(app, () -> get(get(controller(app),"renderer"),"fpsLabel") + " "
                            + get(get(controller(app),"renderer"),"fpsDetailLabel") + " PAINT " + get(app,"lastPaintMs")
                            + " PRESENT " + get(app,"lastPresentMs"))).append('\n');
                }
                Thread.sleep(500);
            }
            assertFalse(fps.isEmpty());
            double average=fps.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            String report=String.format(java.util.Locale.ROOT,
                    "Actual Java2DApp fullscreen spin + native WAV: canvas=%dx%d logical AWT units, FPS mean=%.2f min=%.2f; %d samples after 2s warmup.%n",
                    surface.getWidth(),surface.getHeight(),average,java.util.Collections.min(fps),fps.size());
            Files.writeString(Path.of("target/java2d-app-probe.txt"),report+timings);System.out.print(report);
            var winner=query(app, () -> controller(app).getGachaSequence());
            assertSame(winner.getWinner(),winner.getReel().get(winner.getWinningIndex()));
            waitFor(() -> query(app, () -> controller(app).getGachaSequence().getPhase()) == com.dodgingcat.shop.GachaSequence.Phase.RESULT,5);
            click(surface,width(surface)/2,320);
            waitFor(() -> !query(app, () -> controller(app).getGachaSequence().isActive()),5);
            assertEquals(1,profile.getData().getSkinGachaPulls());
            key(surface,KeyEvent.VK_F9);
            waitFor(() -> state(app) == GameState.MENU,5);
            assertEquals(0,query(app, () -> profile.getData().getTotalFishes()));
            assertEquals(java.util.List.of("ginger","tuxedo"),profile.getData().getUnlockedSkins());
        } finally {
            var window=(Frame)get(app,"window");
            if(window!=null) SwingUtilities.invokeAndWait(() -> window.dispatchEvent(new WindowEvent(window,WindowEvent.WINDOW_CLOSING)));
            thread.join(15_000);
        }
        assertFalse(thread.isAlive(),"Window close must stop render/media threads");
        assertNull(failure.get(), () -> "Native app failed: " + failure.get());
    }

    private static void verifyBgm(String expected) throws Exception {
        waitFor(() -> expected.equals(get(AudioManager.getInstance(),"currentBgmTrack")),8);
        var checked = new CompletableFuture<Void>();
        Platform.runLater(() -> {
            try {
                var player=(javafx.scene.media.MediaPlayer)get(AudioManager.getInstance(),"currentBgmPlayer");
                assertNotNull(player); assertNull(player.getError()); checked.complete(null);
            } catch(Throwable e) { checked.completeExceptionally(e); }
        });
        checked.get(3,TimeUnit.SECONDS);
    }
    private static double width(Canvas surface) { return surface.getWidth()*640.0/surface.getHeight(); }
    private static GameController controller(Java2DApp app) { return (GameController)get(app,"controller"); }
    private static GameState state(Java2DApp app) { return query(app, () -> controller(app).getCurrentState()); }
    @SuppressWarnings("unchecked")
    private static <T> T query(Java2DApp app, Supplier<T> action) {
        var answer=new CompletableFuture<T>();
        ((ConcurrentLinkedQueue<Runnable>)get(app,"events")).add(() -> {
            try { answer.complete(action.get()); } catch(Throwable e) { answer.completeExceptionally(e); }
        });
        try { return answer.get(3,TimeUnit.SECONDS); } catch(Exception e) { throw new AssertionError(e); }
    }
    private static Object get(Object owner,String name) {
        try { var field=owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner); }
        catch(Exception e) { throw new AssertionError(e); }
    }
    private static void waitFor(BooleanSupplier condition,int seconds) throws InterruptedException {
        long until=System.nanoTime()+seconds*1_000_000_000L;
        while(!condition.getAsBoolean()&&System.nanoTime()<until)Thread.sleep(40);
        assertTrue(condition.getAsBoolean(),"Timed out waiting for native app state");
    }
    private static void key(Canvas surface,int code) throws Exception {
        keyEvent(surface,code,true);Thread.sleep(65);keyEvent(surface,code,false);Thread.sleep(65);
    }
    private static void keyEvent(Canvas surface,int code,boolean pressed) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var event=new KeyEvent(surface,pressed?KeyEvent.KEY_PRESSED:KeyEvent.KEY_RELEASED,System.currentTimeMillis(),0,code,KeyEvent.CHAR_UNDEFINED);
            for(var listener:surface.getKeyListeners()) { if(pressed)listener.keyPressed(event);else listener.keyReleased(event); }
        });
    }
    private static void move(Canvas surface,double x,double y) throws Exception { mouse(surface,x,y,MouseEvent.MOUSE_MOVED);Thread.sleep(65); }
    private static void click(Canvas surface,double x,double y) throws Exception {
        mouse(surface,x,y,MouseEvent.MOUSE_PRESSED);Thread.sleep(65);mouse(surface,x,y,MouseEvent.MOUSE_RELEASED);Thread.sleep(65);
    }
    private static void mouse(Canvas surface,double x,double y,int action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            double scale=surface.getHeight()/640.0;
            var event=new MouseEvent(surface,action,System.currentTimeMillis(),0,(int)Math.round(x*scale),(int)Math.round(y*scale),1,false,MouseEvent.BUTTON1);
            if(action==MouseEvent.MOUSE_MOVED)for(var listener:surface.getMouseMotionListeners())listener.mouseMoved(event);
            else for(var listener:surface.getMouseListeners()) { if(action==MouseEvent.MOUSE_PRESSED)listener.mousePressed(event);else listener.mouseReleased(event); }
        });
    }
}
