import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Platform {
    public int x, y, w, h;

    public Platform(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public void draw(Graphics2D g2d, BufferedImage pfTile) {
        if (pfTile != null) {
            int tileSize = 24;
            for (int px = x; px < x + w; px += tileSize) {
                g2d.drawImage(pfTile, px, y, tileSize, Math.min(tileSize, h), null);
            }
        } else {
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(x, y, w, h);
        }
    }
}