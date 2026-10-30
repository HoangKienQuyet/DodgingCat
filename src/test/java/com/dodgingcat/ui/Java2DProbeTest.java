package com.dodgingcat.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;

/** Core-Java presentation prototype; no JavaFX toolkit and no external renderer. */
@EnabledIfSystemProperty(named="dodgingcat.awtProbe", matches="true")
class Java2DProbeTest {
    @Test void measureNativeBufferPresentation() throws Exception {
        var background = ImageIO.read(getClass().getResource("/assets/images/backgrounds/kitchen.png"));
        var cat = ImageIO.read(getClass().getResource("/assets/images/characters/ginger_sheet.png"));
        Frame[] window = new Frame[1]; Canvas[] surface = new Canvas[1];
        SwingUtilities.invokeAndWait(() -> {
            var frame = new Frame("Dodging Cat — Java2D diagnostic");
            var canvas = new Canvas(); canvas.setIgnoreRepaint(true); canvas.setPreferredSize(new Dimension(960,640));
            frame.setUndecorated(true); frame.setIgnoreRepaint(true); frame.add(canvas); frame.pack(); frame.setVisible(true);
            canvas.createBufferStrategy(2); window[0]=frame; surface[0]=canvas;
        });
        StringBuilder report = new StringBuilder("Core Java2D BufferStrategy; full source art; 1s warmup + 3s measurement.\n");
        try {
            for (boolean fullscreen : new boolean[]{false,true}) {
                if (fullscreen) SwingUtilities.invokeAndWait(() -> window[0].getGraphicsConfiguration().getDevice().setFullScreenWindow(window[0]));
                var canvas = surface[0];
                var strategy = canvas.getBufferStrategy();
                var deviceTransform=canvas.getGraphicsConfiguration().getDefaultTransform();
                int nativeW=(int)Math.round(canvas.getWidth()*deviceTransform.getScaleX());
                int nativeH=(int)Math.round(canvas.getHeight()*deviceTransform.getScaleY());
                var nativeBackground=canvas.getGraphicsConfiguration().createCompatibleImage(nativeW,nativeH,Transparency.OPAQUE);
                var preload=nativeBackground.createGraphics();
                preload.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                preload.drawImage(background,0,0,nativeW,nativeH,null); preload.dispose();
                int spriteSize=(int)Math.round(92*deviceTransform.getScaleX());
                BufferedImage[] nativeCats=new BufferedImage[6];
                for(int cell=0;cell<6;cell++) {
                    nativeCats[cell]=canvas.getGraphicsConfiguration().createCompatibleImage(spriteSize,spriteSize,Transparency.TRANSLUCENT);
                    var cg=nativeCats[cell].createGraphics();
                    cg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    cg.drawImage(cat,0,0,spriteSize,spriteSize,cell*256,0,(cell+1)*256,256,null); cg.dispose();
                }
                var intervals = new ArrayList<Double>();
                long start=System.nanoTime(), previous=0;
                int frames=0;
                while (System.nanoTime()-start < 4_000_000_000L) {
                    long now=System.nanoTime();
                    if (now-start>=1_000_000_000L && previous!=0) intervals.add((now-previous)/1e6);
                    previous=now;
                    do {
                        do {
                            var g=(Graphics2D)strategy.getDrawGraphics();
                            try {
                                g.setTransform(new java.awt.geom.AffineTransform());
                                g.drawImage(nativeBackground,0,0,null);
                                for (int i=0;i<30;i++) {
                                    int x=20+i*nativeW/31;
                                    int y=(int)((110+(i%4)*100)*deviceTransform.getScaleY());
                                    int cell=(frames/10)%6;
                                    g.drawImage(nativeCats[cell],x,y,null);
                                }
                            } finally {g.dispose();}
                        } while(strategy.contentsRestored());
                        strategy.show(); Toolkit.getDefaultToolkit().sync();
                    } while(strategy.contentsLost());
                    frames++;
                }
                Collections.sort(intervals);
                var transform=canvas.getGraphicsConfiguration().getDefaultTransform();
                report.append(String.format(Locale.ROOT,"%s config=%s accelerated=%s native=%.0fx%.0f fps=%.2f p95=%.2fms max=%.2fms%n",fullscreen?"Fullscreen":"Windowed",
                        canvas.getGraphicsConfiguration().getClass().getName(),strategy.getCapabilities().getBackBufferCapabilities().isAccelerated(),
                        canvas.getWidth()*transform.getScaleX(),canvas.getHeight()*transform.getScaleY(),
                        1000/intervals.stream().mapToDouble(Double::doubleValue).average().orElseThrow(),
                        intervals.get((int)(intervals.size()*.95)),Collections.max(intervals)));
            }
            Files.writeString(Path.of("target/java2d-probe.txt"),report); System.out.print(report);
        } finally { SwingUtilities.invokeAndWait(() -> window[0].dispose()); }
        assertTrue(report.toString().contains("Fullscreen"));
    }
}
