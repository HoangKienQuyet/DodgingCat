import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** High resolution raster countdown labels, drawn smaller than their source pixels in-game. */
public class GenerateCountdownArt {
    public static void main(String[] args) throws Exception {
        Path output = Path.of("src/main/resources/assets/images/ui/countdown");
        Files.createDirectories(output);
        for (String numeral : new String[]{"3", "2", "1"}) {
            write(output, numeral, numeral, 320, 384, 292, "Georgia");
        }
        write(output, "go_en", "GO!", 640, 320, 240, "Georgia");
        write(output, "go_vi", "BẮT ĐẦU!", 960, 320, 180, "Arial");
    }

    private static void write(Path output, String name, String text, int w, int h,
                              int fontSize, String family) throws Exception {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = new Font(family, Font.BOLD, fontSize);
        GlyphVector vector = font.createGlyphVector(g.getFontRenderContext(), text);
        Shape glyph = vector.getOutline();
        Rectangle2D bounds = glyph.getBounds2D();
        double scale = Math.min((w - 48.0) / bounds.getWidth(), (h - 48.0) / bounds.getHeight());
        AffineTransform transform = new AffineTransform();
        transform.translate(w / 2.0, h / 2.0);
        transform.scale(scale, scale);
        transform.translate(-bounds.getCenterX(), -bounds.getCenterY());
        Shape shape = transform.createTransformedShape(glyph);
        g.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(60, 33, 24));
        g.draw(shape);
        g.setPaint(new GradientPaint(0, 24, new Color(255, 246, 211), 0, h - 24, new Color(239, 181, 94)));
        g.fill(shape);
        g.dispose();
        ImageIO.write(image, "png", output.resolve(name + ".png").toFile());
    }
}
