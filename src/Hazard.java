import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

public class Hazard {
    public boolean isHorizontal;
    public BufferedImage img;
    public double x, y, vx, vy;
    public int size = 32;
    public int warningTimer = 60;
    public double rotation = 0;

    public Hazard(boolean isHorizontal, BufferedImage img, double x, double y, double vx, double vy) {
        this.isHorizontal = isHorizontal;
        this.img = img;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }

    public void update() {
        if (warningTimer > 0) {
            warningTimer--;
        } else {
            x += vx;
            y += vy;
            rotation += 0.03;
        }
    }

    public boolean checkCollision(Player player) {
        if (warningTimer > 0)
            return false;
        double dx = (player.x + player.w / 2.0) - x;
        double dy = (player.y + player.h / 2.0) - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        return dist < (size / 2.0 + player.w / 3.5);
    }

    public boolean isOutOfBounds(int width, int height) {
        return x < -100 || x > width + 100 || y > height + 100;
    }

    public void draw(Graphics2D g2d, int width) {
        if (warningTimer > 0) {
            g2d.setColor(new Color(239, 68, 68));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 22));

            // Đặt vị trí Y = 45 cho vật thể rơi để dấu ! hiện rõ ràng ngay bên dưới thanh
            // HUD
            int wx = isHorizontal ? (vx > 0 ? 20 : width - 40) : (int) x;
            int wy = isHorizontal ? (int) y : 45;

            g2d.drawString("!", wx, wy);
        } else {
            AffineTransform oldTransform = g2d.getTransform();

            g2d.translate(x, y);
            g2d.rotate(rotation);
            if (img != null) {
                g2d.drawImage(img, -size / 2, -size / 2, size, size, null);
            } else {
                g2d.setColor(Color.RED);
                g2d.fillOval(-size / 2, -size / 2, size, size);
            }

            g2d.setTransform(oldTransform);
        }
    }
}