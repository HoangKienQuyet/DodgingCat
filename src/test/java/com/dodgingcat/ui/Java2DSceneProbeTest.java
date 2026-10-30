package com.dodgingcat.ui;

import com.dodgingcat.app.RenderSurface;
import com.dodgingcat.audio.AudioManager;
import com.dodgingcat.core.*;
import com.dodgingcat.game.*;
import com.dodgingcat.input.InputManager;
import com.dodgingcat.persistence.*;
import com.dodgingcat.shop.*;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Frame;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="dodgingcat.awtSceneProbe", matches="true")
class Java2DSceneProbeTest {
    @Test void measureActualScenesAtNativeResolution() throws Exception {
        RenderSurface.configureRuntime();
        CountDownLatch ready=new CountDownLatch(1);
        Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});
        assertTrue(ready.await(10,TimeUnit.SECONDS));
        AudioManager.getInstance().setMuted(true);
        var renderer=new GameRenderer(960,640);
        var persistence=new PersistenceManager(Path.of("target/awt-scene-profile.json"));
        var shop=new ShopManager(persistence);
        var data=new GameData();var input=new InputManager();
        Thread.sleep(4000);
        Frame[] frame={null};Canvas[] canvas={null};
        SwingUtilities.invokeAndWait(()->{
            frame[0]=new Frame("Dodging Cat — native Java2D scenes"); frame[0].setUndecorated(true);
            canvas[0]=new Canvas();canvas[0].setIgnoreRepaint(true);canvas[0].setPreferredSize(new Dimension(960,640));
            frame[0].add(canvas[0]);frame[0].pack();frame[0].setVisible(true);canvas[0].createBufferStrategy(2);
        });
        StringBuilder report=new StringBuilder("Full game renderer through core Java2D; " + (Boolean.getBoolean("dodgingcat.awtSceneProbe.capped") ? "60 FPS deadlines" : "uncapped") + "; native buffers; 1s warmup + 3s samples.\n");
        try {
            for(boolean fullscreen:new boolean[]{false,true}) {
                if(fullscreen)SwingUtilities.invokeAndWait(()->frame[0].getGraphicsConfiguration().getDevice().setFullScreenWindow(frame[0]));
                var surface=canvas[0];var buffers=surface.getBufferStrategy();
                var device=surface.getGraphicsConfiguration().getDefaultTransform();
                int pw=(int)Math.round(surface.getWidth()*device.getScaleX()),ph=(int)Math.round(surface.getHeight()*device.getScaleY());
                double scale=ph/640.0,lw=pw/scale;
                renderer.resize(lw,640);
                for(int scenario=0;scenario<5;scenario++) {
                    var world=new GameWorld(GameMode.LOCAL_COOP,"kitchen",lw,640,"ginger","tuxedo");
                    int n=0;
                    for(var type:com.dodgingcat.buffs.BuffType.values())world.getBuffs().add(new com.dodgingcat.buffs.Buff(type,70+n++*165,250,30));
                    for(int i=0;i<5;i++) {
                        world.getFishes().add(new FishItem(i%2==0?FishType.SINGLE_FISH:FishType.FISH_CAN,80+i*160,350));
                        world.getHazards().add(new com.dodgingcat.hazards.TopDownHazard(90+i*150,70,42,50,
                                new String[]{"knife","frying_pan","rolling_pin","salt_shaker","blender"}[i],60,0));
                    }
                    var gacha=new GachaSequence(shop.getSkinCatalog(),shop.findItemById("cyber"),new Random(42));
                    if(scenario==4) { gacha.update(0,1,true,false);gacha.update(GachaSequence.LANDING_SECONDS,1,true,false); }
                    var intervals=new ArrayList<Double>();
                    var pacer=Boolean.getBoolean("dodgingcat.awtSceneProbe.capped")?new com.dodgingcat.app.FramePacer(60):null;
                    long start=System.nanoTime(),previous=0;
                    while(System.nanoTime()-start<4_000_000_000L) {
                        long now=pacer==null?System.nanoTime():pacer.awaitFrame();double age=(now-start)/1e9,dt=previous==0?1.0/60:Math.min(.05,(now-previous)/1e9);
                        if(age>=1&&previous!=0)intervals.add((now-previous)/1e6);previous=now;
                        renderer.updateAnimationTimer(dt);
                        if(scenario==2){world.getPlayers().forEach(p->p.applyBuff(com.dodgingcat.buffs.BuffType.INVINCIBILITY));world.update(dt);}
                        if(scenario==3)gacha.update(dt,Math.min(.995,age/14),false,false);
                        if(scenario==4)gacha.update(dt,1,true,false);
                        do {
                            do {
                                var graphics=(Graphics2D)buffers.getDrawGraphics();
                                try {
                                    graphics.setTransform(new AffineTransform());graphics.scale(scale,scale);
                                    var gc=new DrawContext(graphics);
                                    var state=scenario==0?GameState.MENU:scenario==2?GameState.PLAYING_COOP:GameState.SHOP;
                                    renderer.render(gc,state,scenario==2?world:null,Theme.KITCHEN,data,shop,input,
                                            0,0,0,0,0,0,1,false,false,1,GameMode.LOCAL_COOP,false,0,0,0,3,0,scenario>=3?gacha:null,100);
                                }finally{graphics.dispose();}
                            }while(buffers.contentsRestored());
                            buffers.show();Toolkit.getDefaultToolkit().sync();
                        }while(buffers.contentsLost());
                    }
                    Collections.sort(intervals);
                    report.append(String.format(Locale.ROOT,"%s %-12s native=%dx%d fps=%.2f p95=%.2fms max=%.2fms%n",fullscreen?"Fullscreen":"Windowed",
                            new String[]{"MENU","SHOP","PLAYING_COOP","SPIN","REVEAL_RESULT"}[scenario],pw,ph,
                            1000/intervals.stream().mapToDouble(Double::doubleValue).average().orElseThrow(),
                            intervals.get((int)(intervals.size()*.95)),Collections.max(intervals)));
                }
            }
            Files.writeString(Path.of(Boolean.getBoolean("dodgingcat.awtSceneProbe.capped")?"target/java2d-scenes-60fps.txt":"target/java2d-scenes.txt"),report);System.out.print(report);
        }finally{SwingUtilities.invokeAndWait(()->frame[0].dispose());Platform.exit();}
    }
}
