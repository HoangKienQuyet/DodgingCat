import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Generates raster lettering and button art; illustrated branding assets are authored separately. */
public class GenerateUiArt {
    private static final int CELL_W = 80, CELL_H = 84, COLS = 16, BASELINE = 62;
    private static final Path OUT = Path.of("src/main/resources/assets/images/ui");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        SortedSet<Integer> glyphs = new TreeSet<>();
        String basic = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 !?.,:;+-/%()[]{}<>«»◂✦•🐟₫'\"=×–—";
        basic.codePoints().forEach(glyphs::add);
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                Files.readString(path, StandardCharsets.UTF_8).codePoints()
                        .filter(c -> c > 32 && c < 65536).forEach(glyphs::add);
            }
        }
        Font font = new Font("Georgia", Font.BOLD, 52);
        Font fallback = new Font("Arial", Font.BOLD, 52);
        glyphs.removeIf(c -> !font.canDisplay(c) && !fallback.canDisplay(c));
        int rows = (glyphs.size() + COLS - 1) / COLS;
        BufferedImage atlas = new BufferedImage(COLS * CELL_W, rows * CELL_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();
        quality(g);
        g.setFont(font);
        StringBuilder meta = new StringBuilder();
        int index = 0;
        for (int codepoint : glyphs) {
            String ch = new String(Character.toChars(codepoint));
            Font glyphFont = font.canDisplay(codepoint) ? font : fallback;
            g.setFont(glyphFont);
            int advance = Math.min(72, Math.max(14, g.getFontMetrics().stringWidth(ch) + 2));
            int x = (index % COLS) * CELL_W, y = (index / COLS) * CELL_H;
            GlyphVector vector = glyphFont.createGlyphVector(g.getFontRenderContext(), ch);
            Shape shape = vector.getOutline(x + 6, y + BASELINE);
            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(55, 29, 19, 235));
            g.draw(shape);
            g.setPaint(new GradientPaint(x, y + 9, new Color(255, 248, 225), x, y + 70, new Color(239, 192, 117)));
            g.fill(shape);
            meta.append(codepoint).append(' ').append(advance).append('\n');
            index++;
        }
        g.dispose();
        ImageIO.write(atlas, "png", OUT.resolve("art_glyphs.png").toFile());
        Files.writeString(OUT.resolve("art_glyphs.txt"), meta.toString(), StandardCharsets.UTF_8);
        makeButton();
        // Preserve separately authored ImageGen title and logo assets.

    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static void makeButton() throws Exception {
        BufferedImage image = new BufferedImage(960, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics(); quality(g);
        RoundRectangle2D outer = new RoundRectangle2D.Double(9, 11, 942, 218, 102, 102);
        g.setPaint(new GradientPaint(0, 11, new Color(99, 60, 39), 0, 229, new Color(46, 31, 29)));
        g.fill(outer);
        g.setStroke(new BasicStroke(8)); g.setColor(new Color(41, 26, 24)); g.draw(outer);
        g.setStroke(new BasicStroke(5)); g.setColor(new Color(209, 155, 88));
        g.draw(new RoundRectangle2D.Double(17, 19, 926, 202, 96, 96));
        g.setStroke(new BasicStroke(3)); g.setColor(new Color(255, 224, 162, 185));
        g.draw(new RoundRectangle2D.Double(25, 27, 910, 186, 88, 88));
        g.setPaint(new GradientPaint(0, 32, new Color(152, 101, 62, 125), 0, 155, new Color(67, 43, 37, 18)));
        g.fill(new RoundRectangle2D.Double(32, 34, 896, 172, 80, 80));
        g.dispose();
        ImageIO.write(image, "png", OUT.resolve("button_plaque.png").toFile());
    }

}
