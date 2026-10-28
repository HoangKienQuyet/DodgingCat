package com.dodgingcat.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.*;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;
import javafx.embed.swing.SwingFXUtils;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Shared drawing vocabulary; Java2D is implemented entirely with java.desktop APIs. */
public final class DrawContext {
    private final GraphicsContext fx;
    private Graphics2D awt;
    private Paint fill = Color.BLACK, stroke = Color.BLACK;
    private Font font;
    private TextAlignment alignment = TextAlignment.LEFT;
    private double lineWidth = 1;
    private StrokeLineCap cap = StrokeLineCap.SQUARE;
    private double[] dashes;
    private Path2D.Double path = new Path2D.Double();
    private final Deque<State> saved = new ArrayDeque<>();
    private record State(Graphics2D graphics, Paint fill, Paint stroke, Font font, TextAlignment alignment,
                         double width, StrokeLineCap cap, double[] dashes) {}
    private record Region(double x, double y, double w, double h, int pixelW, int pixelH, boolean flipX, boolean flipY) {}
    private static final class ImageCache {
        final BufferedImage source;
        final boolean opaque;
        final Map<Region, BufferedImage> variants = new LinkedHashMap<>(8,.75f,true);
        ImageCache(Image image) {
            source=SwingFXUtils.fromFXImage(image,null);
            boolean allOpaque=true;
            for(int pixel:((java.awt.image.DataBufferInt)source.getRaster().getDataBuffer()).getData()) {
                if((pixel>>>24)!=255){allOpaque=false;break;}
            }
            opaque=allOpaque;
        }
        ImageCache(BufferedImage nativeBitmap) { source=nativeBitmap; opaque=true; }
    }
    private static final Map<Image, ImageCache> BITMAPS = Collections.synchronizedMap(new WeakHashMap<>());
    private record Rounded(double w, double h, double arcW, double arcH, double sx, double sy,
                           Paint paint, boolean outlined, double lineWidth, StrokeLineCap cap) {}
    private static final Map<Rounded, BufferedImage> ROUNDED = new LinkedHashMap<>(32, .75f, true);
    private static long roundedPixels;

    public DrawContext(GraphicsContext graphics) { fx = Objects.requireNonNull(graphics); }
    public DrawContext(Graphics2D graphics) {
        fx = null; awt = Objects.requireNonNull(graphics);
        awt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        awt.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        awt.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
    public boolean isJava2D() { return fx == null; }
    /** CPU conversion/alpha inspection can run on the boot worker, away from gameplay. */
    public static void prepareImage(Image image) { if(image!=null&&image.getPixelReader()!=null) imageCache(image); }
    static void prepareNativeBackdrop(Image image, BufferedImage bitmap) {
        var cache=new ImageCache(bitmap);
        cache.variants.put(new Region(0,0,bitmap.getWidth(),bitmap.getHeight(),bitmap.getWidth(),bitmap.getHeight(),false,false),bitmap);
        BITMAPS.put(image,cache);
    }
    private static ImageCache imageCache(Image image) {
        var cache=BITMAPS.get(image);
        if(cache==null) {
            var decoded=new ImageCache(image);
            synchronized(BITMAPS) {
                cache=BITMAPS.get(image);
                if(cache==null) { cache=decoded; BITMAPS.put(image,cache); }
            }
        }
        return cache;
    }
    public void save() {
        if (fx != null) { fx.save(); return; }
        saved.push(new State(awt,fill,stroke,font,alignment,lineWidth,cap,dashes)); awt=(Graphics2D)awt.create();
    }
    public void restore() {
        if (fx != null) { fx.restore(); return; }
        awt.dispose(); var state=saved.pop(); awt=state.graphics; fill=state.fill; stroke=state.stroke;
        font=state.font; alignment=state.alignment; lineWidth=state.width; cap=state.cap; dashes=state.dashes;
    }
    public void translate(double x,double y) { if(fx!=null)fx.translate(x,y); else awt.translate(x,y); }
    public void scale(double x,double y) { if(fx!=null)fx.scale(x,y); else awt.scale(x,y); }
    public void rotate(double angle) { if(fx!=null)fx.rotate(angle); else awt.rotate(Math.toRadians(angle)); }
    public void setTransform(double a,double b,double c,double d,double x,double y) {
        if(fx!=null)fx.setTransform(a,b,c,d,x,y); else awt.setTransform(new AffineTransform(a,b,c,d,x,y));
    }
    public void setTransform(Affine transform) { setTransform(transform.getMxx(),transform.getMyx(),transform.getMxy(),transform.getMyy(),transform.getTx(),transform.getTy()); }
    public Affine getTransform() {
        if(fx!=null)return fx.getTransform(); var t=awt.getTransform();
        return new Affine(t.getScaleX(),t.getShearX(),t.getTranslateX(),t.getShearY(),t.getScaleY(),t.getTranslateY());
    }
    public void setFill(Paint value) { if(fx!=null)fx.setFill(value); else fill=value; }
    public void setStroke(Paint value) { if(fx!=null)fx.setStroke(value); else stroke=value; }
    public void setFont(Font value) { if(fx!=null)fx.setFont(value); else font=value; }
    public Font getFont() { return fx!=null?fx.getFont():font!=null?font:Font.getDefault(); }
    public void setTextAlign(TextAlignment value) { if(fx!=null)fx.setTextAlign(value); else alignment=value; }
    public TextAlignment getTextAlign() { return fx!=null?fx.getTextAlign():alignment; }
    public void setGlobalAlpha(double value) {
        if(fx!=null)fx.setGlobalAlpha(value); else awt.setComposite(AlphaComposite.SrcOver.derive((float)Math.clamp(value,0,1)));
    }
    public void setEffect(javafx.scene.effect.Effect value) {
        if(fx!=null)fx.setEffect(value); else if(value!=null)throw new IllegalArgumentException("Java2D effects must be drawn explicitly");
    }
    public void setLineWidth(double value) { if(fx!=null)fx.setLineWidth(value); else lineWidth=value; }
    public void setLineCap(StrokeLineCap value) { if(fx!=null)fx.setLineCap(value); else cap=value; }
    public void setLineDashes(double... values) { if(fx!=null)fx.setLineDashes(values); else dashes=values; }
    private static java.awt.Color color(Color c) {return new java.awt.Color((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),(float)c.getOpacity());}
    private java.awt.Paint paint(Paint value,Shape shape) {
        if(value instanceof Color c)return color(c);
        if(value instanceof LinearGradient gradient) {
            var stops=gradient.getStops(); float[] fractions=new float[stops.size()]; java.awt.Color[] colors=new java.awt.Color[stops.size()];
            for(int i=0;i<stops.size();i++){ fractions[i]=(float)stops.get(i).getOffset();colors[i]=color(stops.get(i).getColor()); }
            double x1=gradient.getStartX(), y1=gradient.getStartY(), x2=gradient.getEndX(), y2=gradient.getEndY();
            if(gradient.isProportional()){var bounds=shape.getBounds2D();x1=bounds.getX()+x1*bounds.getWidth();x2=bounds.getX()+x2*bounds.getWidth();y1=bounds.getY()+y1*bounds.getHeight();y2=bounds.getY()+y2*bounds.getHeight();}
            if(x1==x2&&y1==y2)return colors[colors.length-1];
            var cycle=switch(gradient.getCycleMethod()) {case REPEAT->java.awt.MultipleGradientPaint.CycleMethod.REPEAT;case REFLECT->java.awt.MultipleGradientPaint.CycleMethod.REFLECT;default->java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;};
            return new java.awt.LinearGradientPaint(new Point2D.Double(x1,y1),new Point2D.Double(x2,y2),fractions,colors,cycle);
        }
        throw new IllegalArgumentException("Unsupported paint: "+value);
    }
    private void fill(Shape shape) {awt.setPaint(paint(fill,shape));awt.fill(shape);}
    private void stroke(Shape shape) {
        float[] dash=null;if(dashes!=null&&dashes.length>0){dash=new float[dashes.length];for(int i=0;i<dash.length;i++)dash[i]=(float)dashes[i];}
        int lineCap=switch(cap){case ROUND->BasicStroke.CAP_ROUND;case BUTT->BasicStroke.CAP_BUTT;default->BasicStroke.CAP_SQUARE;};
        awt.setStroke(new BasicStroke((float)lineWidth,lineCap,BasicStroke.JOIN_MITER,10,dash,0));awt.setPaint(paint(stroke,shape));awt.draw(shape);
    }
    public void fillRect(double x,double y,double w,double h) {if(fx!=null)fx.fillRect(x,y,w,h);else fill(new Rectangle2D.Double(x,y,w,h));}
    public void strokeRect(double x,double y,double w,double h) {if(fx!=null)fx.strokeRect(x,y,w,h);else stroke(new Rectangle2D.Double(x,y,w,h));}
    public void fillRoundRect(double x,double y,double w,double h,double a,double b) {
        if(fx!=null)fx.fillRoundRect(x,y,w,h,a,b);
        else if(!cachedRoundRect(x,y,w,h,a,b,false))fill(new RoundRectangle2D.Double(x,y,w,h,a,b));
    }
    // Rasterize static panel/card shapes once at the exact native scale. Translucent
    // gradients otherwise force a large software paint on every presentation.
    private boolean cachedRoundRect(double x,double y,double w,double h,double a,double b,boolean outlined) {
        var t=awt.getTransform();
        if(t.getShearX()!=0||t.getShearY()!=0||t.getScaleX()<=0||t.getScaleY()<=0||w<=0||h<=0)return false;
        if(outlined&&dashes!=null&&dashes.length>0)return false;
        Paint localFill=outlined?stroke:fill;
        if(localFill instanceof LinearGradient gradient&&!gradient.isProportional())
            localFill=new LinearGradient(gradient.getStartX()-x,gradient.getStartY()-y,
                    gradient.getEndX()-x,gradient.getEndY()-y,false,gradient.getCycleMethod(),gradient.getStops());
        var key=new Rounded(w,h,a,b,t.getScaleX(),t.getScaleY(),localFill,outlined,outlined?lineWidth:0,outlined?cap:StrokeLineCap.SQUARE);
        int padding=outlined?(int)Math.ceil(lineWidth*Math.max(t.getScaleX(),t.getScaleY())/2)+2:1;
        BufferedImage bitmap;
        synchronized(ROUNDED) {
            bitmap=ROUNDED.get(key);
            if(bitmap==null) {
                int pw=(int)Math.ceil(w*t.getScaleX())+padding*2,ph=(int)Math.ceil(h*t.getScaleY())+padding*2;
                bitmap=new BufferedImage(pw,ph,BufferedImage.TYPE_INT_ARGB_PRE);
                var g=bitmap.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(padding,padding);g.scale(t.getScaleX(),t.getScaleY());
                var shape=new RoundRectangle2D.Double(0,0,w,h,a,b);
                g.setPaint(paint(localFill,shape));
                if(outlined) { g.setStroke(new BasicStroke((float)lineWidth)); g.draw(shape); }
                else g.fill(shape);
                g.dispose();
                ROUNDED.put(key,bitmap);roundedPixels+=(long)pw*ph;
                var entries=ROUNDED.entrySet().iterator();
                while(roundedPixels>8_000_000&&ROUNDED.size()>1&&entries.hasNext()) {
                    var oldest=entries.next().getValue();roundedPixels-=(long)oldest.getWidth()*oldest.getHeight();entries.remove();
                }
            }
        }
        var g=(Graphics2D)awt.create();g.setTransform(new AffineTransform());
        g.drawImage(bitmap,(int)Math.round(x*t.getScaleX()+t.getTranslateX())-padding,
                (int)Math.round(y*t.getScaleY()+t.getTranslateY())-padding,null);g.dispose();
        return true;
    }
    public void strokeRoundRect(double x,double y,double w,double h,double a,double b) {
        if(fx!=null)fx.strokeRoundRect(x,y,w,h,a,b);
        else if(!cachedRoundRect(x,y,w,h,a,b,true))stroke(new RoundRectangle2D.Double(x,y,w,h,a,b));
    }
    public void fillOval(double x,double y,double w,double h) {if(fx!=null)fx.fillOval(x,y,w,h);else fill(new Ellipse2D.Double(x,y,w,h));}
    public void strokeOval(double x,double y,double w,double h) {if(fx!=null)fx.strokeOval(x,y,w,h);else stroke(new Ellipse2D.Double(x,y,w,h));}
    public void strokeLine(double x,double y,double x2,double y2) {if(fx!=null)fx.strokeLine(x,y,x2,y2);else stroke(new Line2D.Double(x,y,x2,y2));}
    public void strokeArc(double x,double y,double w,double h,double start,double length,ArcType type) {
        if(fx!=null)fx.strokeArc(x,y,w,h,start,length,type);else stroke(new Arc2D.Double(x,y,w,h,start,length,switch(type){case ROUND->Arc2D.PIE;case CHORD->Arc2D.CHORD;default->Arc2D.OPEN;}));
    }
    private static Path2D polygon(double[] xs,double[] ys,int count) {var p=new Path2D.Double();if(count>0){p.moveTo(xs[0],ys[0]);for(int i=1;i<count;i++)p.lineTo(xs[i],ys[i]);p.closePath();}return p;}
    public void fillPolygon(double[] xs,double[] ys,int count) {if(fx!=null)fx.fillPolygon(xs,ys,count);else fill(polygon(xs,ys,count));}
    public void strokePolygon(double[] xs,double[] ys,int count) {if(fx!=null)fx.strokePolygon(xs,ys,count);else stroke(polygon(xs,ys,count));}
    public void beginPath(){if(fx!=null)fx.beginPath();else path=new Path2D.Double();}
    private Point2D devicePoint(double x,double y){return awt.getTransform().transform(new Point2D.Double(x,y),null);}
    public void moveTo(double x,double y){if(fx!=null)fx.moveTo(x,y);else{var p=devicePoint(x,y);path.moveTo(p.getX(),p.getY());}}
    public void lineTo(double x,double y){if(fx!=null)fx.lineTo(x,y);else{var p=devicePoint(x,y);path.lineTo(p.getX(),p.getY());}}
    public void quadraticCurveTo(double x,double y,double ex,double ey){if(fx!=null)fx.quadraticCurveTo(x,y,ex,ey);else{var p=devicePoint(x,y);var e=devicePoint(ex,ey);path.quadTo(p.getX(),p.getY(),e.getX(),e.getY());}}
    public void rect(double x,double y,double w,double h){if(fx!=null)fx.rect(x,y,w,h);else path.append(awt.getTransform().createTransformedShape(new Rectangle2D.Double(x,y,w,h)),false);}
    public void closePath(){if(fx!=null)fx.closePath();else path.closePath();}
    public void clip(){if(fx!=null)fx.clip();else{var transform=awt.getTransform();awt.setTransform(new AffineTransform());awt.clip(path);awt.setTransform(transform);}}
    private Shape userPath(){try{return awt.getTransform().createInverse().createTransformedShape(path);}catch(NoninvertibleTransformException e){return new Path2D.Double();}}
    public void fill(){if(fx!=null)fx.fill();else fill(userPath());}
    public void stroke(){if(fx!=null)fx.stroke();else stroke(userPath());}

    public void drawImage(Image image,double x,double y,double w,double h){drawImage(image,0,0,image.getWidth(),image.getHeight(),x,y,w,h);}
    public void drawImage(Image image,double sx,double sy,double sw,double sh,double x,double y,double w,double h) {
        if(fx!=null){fx.drawImage(image,sx,sy,sw,sh,x,y,w,h);return;}
        if(w<=0||h<=0||sw<=0||sh<=0||image.getPixelReader()==null)return;
        var t=awt.getTransform();
        double scaleX=Math.hypot(t.getScaleX(),t.getShearY()),scaleY=Math.hypot(t.getShearX(),t.getScaleY());
        int pw=Math.max(1,(int)Math.round(w*scaleX)),ph=Math.max(1,(int)Math.round(h*scaleY));
        boolean axial=Math.abs(t.getShearX())<1e-9&&Math.abs(t.getShearY())<1e-9;
        boolean flipX=axial&&t.getScaleX()<0,flipY=axial&&t.getScaleY()<0;
        Region region=new Region(sx,sy,sw,sh,pw,ph,flipX,flipY);
        BufferedImage bitmap;
        var cache=imageCache(image);
        synchronized(cache.variants) {
            var variants=cache.variants;
            bitmap=variants.get(region);
            if(bitmap==null){
                var source=cache.source;
                bitmap=new BufferedImage(pw,ph,cache.opaque?BufferedImage.TYPE_INT_RGB:BufferedImage.TYPE_INT_ARGB_PRE);
                var g=bitmap.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                var placement=new AffineTransform();
                if(flipX||flipY){placement.translate(flipX?pw:0,flipY?ph:0);placement.scale(flipX?-1:1,flipY?-1:1);}
                placement.scale(pw/sw,ph/sh);placement.translate(-sx,-sy);
                g.drawImage(source,placement,null);g.dispose();
                variants.put(region,bitmap);
                if(variants.size()>32){var keys=variants.keySet().iterator();keys.next();keys.remove();}
            }
        }
        if(axial){
            double dx=x*t.getScaleX()+t.getTranslateX(),dy=y*t.getScaleY()+t.getTranslateY();
            if(flipX)dx-=pw;if(flipY)dy-=ph;
            var g=(Graphics2D)awt.create();g.setTransform(new AffineTransform());g.drawImage(bitmap,(int)Math.round(dx),(int)Math.round(dy),null);g.dispose();
        }else{
            var placement=AffineTransform.getTranslateInstance(x,y);placement.scale(w/pw,h/ph);awt.drawImage(bitmap,placement,null);
        }
    }
}
