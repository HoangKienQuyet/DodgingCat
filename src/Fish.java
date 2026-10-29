import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Fish {
    public double x, y;
    public int size = 34; // Kích thước hiển thị ảnh con cá

    public Fish(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean checkCollect(Player player) {
        double dx = (player.x + player.w / 2.0) - x;
        double dy = (player.y + player.h / 2.0) - y;
        return Math.sqrt(dx * dx + dy * dy) < 25;
    }

    public void draw(Graphics2D g2d, BufferedImage fishImg) {
        if (fishImg != null) {
            g2d.drawImage(fishImg, (int) x - size / 2, (int) y - size / 2, size, size, null);
        } else {
            // Dự phòng nếu không tải được tệp ảnh
            g2d.setColor(new Color(251, 191, 36));
            g2d.fillOval((int) x - 9, (int) y - 9, 18, 18);
            g2d.setColor(new Color(120, 53, 15));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval((int) x - 9, (int) y - 9, 18, 18);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2d.drawString("F", (int) x - 3, (int) y + 4);
        }
    }
}