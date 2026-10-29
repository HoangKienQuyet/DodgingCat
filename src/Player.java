import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

public class Player {
    public double x = 100, y = 350;
    public int w = 44, h = 44;
    public double vx = 0, vy = 0;
    public double speed = 2.9;
    public double jumpPower = -12.0;
    public double gravity = 0.38;
    public boolean isGrounded = false;

    public int frame = 0;
    public int frameTimer = 0;
    public int facing = 1;

    private final List<BufferedImage> runFrames = new ArrayList<>();
    private final List<BufferedImage> jumpFrames = new ArrayList<>();

    public void loadAssets() {
        for (int i = 1; i <= 8; i++) {
            runFrames.add(AssetLoader.loadImage("assests/player/Run (" + i + ").png"));
            jumpFrames.add(AssetLoader.loadImage("assests/player/Jump (" + i + ").png"));
        }
    }

    public void reset() {
        x = 100;
        y = 350;
        vx = 0;
        vy = 0;
        isGrounded = false;
        frame = 0;
        frameTimer = 0;
    }

    public void update(boolean keyLeft, boolean keyRight, boolean keyJump, List<Platform> platforms, int screenWidth) {
        if (keyLeft) {
            vx = -speed;
            facing = -1;
        } else if (keyRight) {
            vx = speed;
            facing = 1;
        } else {
            vx = 0;
        }

        if (keyJump && isGrounded) {
            vy = jumpPower;
            isGrounded = false;
        }

        vy += gravity;
        x += vx;
        y += vy;

        isGrounded = false;
        for (Platform p : platforms) {
            if (x + w > p.x && x < p.x + p.w) {
                if (vy >= 0 && y + h >= p.y && y + h <= p.y + p.h + vy) {
                    y = p.y - h;
                    vy = 0;
                    isGrounded = true;
                }
            }
        }

        x = Math.max(0, Math.min(screenWidth - w, x));

        if (isGrounded) {
            if (vx != 0) {
                frameTimer++;
                if (frameTimer % 6 == 0)
                    frame = (frame + 1) % 8;
            } else {
                frame = 0;
                frameTimer = 0;
            }
        } else {
            frameTimer++;
            if (frameTimer % 6 == 0)
                frame = (frame + 1) % 8;
        }
    }

    public void draw(Graphics2D g2d) {
        List<BufferedImage> currentAnim = isGrounded ? runFrames : jumpFrames;
        BufferedImage sprite = (currentAnim.isEmpty() || currentAnim.get(frame % currentAnim.size()) == null)
                ? null
                : currentAnim.get(frame % currentAnim.size());

        if (sprite != null) {
            // Thay thế g2d.save() bằng dòng dưới:
            AffineTransform oldTransform = g2d.getTransform();

            g2d.translate(x + w / 2.0, y + h / 2.0);
            if (facing == -1)
                g2d.scale(-1, 1);
            g2d.drawImage(sprite, -w / 2 - 8, -h / 2 - 8, w + 16, h + 16, null);

            // Thay thế g2d.restore() bằng dòng dưới:
            g2d.setTransform(oldTransform);
        } else {
            g2d.setColor(Color.ORANGE);
            g2d.fillRect((int) x, (int) y, w, h);
        }
    }
}