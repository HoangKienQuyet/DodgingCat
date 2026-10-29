import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener {

    public static final int BASE_WIDTH = 960;
    public static final int BASE_HEIGHT = 540;

    private GameState gameState = GameState.TITLE;
    private final Timer timer;
    private final Random random = new Random();

    private int selectedMapIndex = 0;
    private int score = 0;
    private int fishesCollected = 0;
    private double gameTime = 0;
    private int lastDifficultyIncrease = 0;
    private double hazardSpeedMultiplier = 1.0;
    private int spawnInterval = 1800;
    private long lastSpawnTime = 0;
    private boolean showSpeedUpWarning = false;
    private long warningHideTime = 0;

    private final List<MapConfig> mapsConfig = new ArrayList<>();
    private final Player player = new Player();
    private final List<Platform> platforms = new ArrayList<>();
    private final List<Hazard> hazards = new ArrayList<>();

    private Fish activeFish = null;
    private int fishRespawnTimer = 0;
    private BufferedImage fishImage;

    private boolean keyLeft = false, keyRight = false, keyJump = false;

    public GamePanel() {
        setPreferredSize(new Dimension(BASE_WIDTH, BASE_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);

        initMaps();
        initPlatforms();
        player.loadAssets();

        fishImage = AssetLoader.loadImage("assests/platforms/fish/fish.png");

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    private void initMaps() {
        mapsConfig.add(new MapConfig("Green Forest", "assests/backgrounds/forest.png", "assests/platforms/forest/2.png",
                new String[] { "assests/platforms/forest/Mushroom_1.png", "assests/platforms/forest/Mushroom_2.png",
                        "assests/platforms/forest/Crate.png", "assests/platforms/forest/Stone.png" }));
        mapsConfig.add(new MapConfig("Desert Dunes", "assests/backgrounds/desert.png", "assests/platforms/desert/2.png",
                new String[] { "assests/platforms/desert/Cactus (1).png", "assests/platforms/desert/Cactus (2).png",
                        "assests/platforms/desert/Cactus (3).png", "assests/platforms/desert/Skeleton.png",
                        "assests/platforms/desert/StoneBlock.png" }));
        mapsConfig.add(new MapConfig("Graveyard", "assests/backgrounds/graveyard.png",
                "assests/platforms/graveyard/Tile (2).png",
                new String[] { "assests/platforms/graveyard/TombStone (1).png",
                        "assests/platforms/graveyard/TombStone (2).png", "assests/platforms/graveyard/Skeleton.png",
                        "assests/platforms/graveyard/Bone (1).png", "assests/platforms/graveyard/DeadBush.png" }));
        mapsConfig.add(new MapConfig("Snowy Winter", "assests/backgrounds/winter.png", "assests/platforms/winter/2.png",
                new String[] { "assests/platforms/winter/SnowMan.png", "assests/platforms/winter/Crystal.png",
                        "assests/platforms/winter/IceBox.png", "assests/platforms/winter/Igloo.png" }));

        for (MapConfig m : mapsConfig) {
            m.loadAssets();
        }
    }

    private void initPlatforms() {
        platforms.clear();
        platforms.add(new Platform(0, 480, 960, 60));
        platforms.add(new Platform(60, 380, 180, 24));
        platforms.add(new Platform(390, 380, 180, 24));
        platforms.add(new Platform(720, 380, 180, 24));
        platforms.add(new Platform(220, 280, 200, 24));
        platforms.add(new Platform(540, 280, 200, 24));
        platforms.add(new Platform(80, 190, 160, 24));
        platforms.add(new Platform(400, 180, 160, 24));
        platforms.add(new Platform(720, 190, 160, 24));
    }

    private void startGame() {
        gameState = GameState.PLAYING;
        score = 0;
        fishesCollected = 0;
        gameTime = 0;
        lastDifficultyIncrease = 0;
        hazardSpeedMultiplier = 1.0;
        spawnInterval = 1800;
        player.reset();
        hazards.clear();

        fishRespawnTimer = 0;
        spawnSingleFish();
        lastSpawnTime = System.currentTimeMillis();
    }

    private void spawnSingleFish() {
        if (platforms.size() <= 1)
            return;
        Platform target = platforms.get(1 + random.nextInt(platforms.size() - 1));
        double x = target.x + 20 + random.nextInt(Math.max(10, target.w - 40));
        double y = target.y - 18;
        activeFish = new Fish(x, y);
    }

    private void spawnHazard() {
        boolean isHorizontal = random.nextBoolean();
        List<BufferedImage> available = mapsConfig.get(selectedMapIndex).hazardImages;
        BufferedImage hImg = available.isEmpty() ? null : available.get(random.nextInt(available.size()));

        if (isHorizontal) {
            boolean fromLeft = random.nextBoolean();
            double x = fromLeft ? -40 : 1000;
            double y = 120 + random.nextInt(300);
            double vx = (fromLeft ? 1 : -1) * (2 + random.nextDouble() * 1.5) * hazardSpeedMultiplier;
            hazards.add(new Hazard(true, hImg, x, y, vx, 0));
        } else {
            double x = 80 + random.nextInt(800);
            double y = -40;
            double vy = (1.5 + random.nextDouble() * 2) * hazardSpeedMultiplier;
            hazards.add(new Hazard(false, hImg, x, y, 0, vy));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.PLAYING) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        gameTime += 0.016;
        score = (int) (gameTime * 10);

        int currentSec = (int) gameTime;
        if (currentSec > 0 && currentSec % 5 == 0 && currentSec != lastDifficultyIncrease) {
            lastDifficultyIncrease = currentSec;
            hazardSpeedMultiplier += 0.05;
            spawnInterval = Math.max(800, spawnInterval - 60);
            showSpeedUpWarning = true;
            warningHideTime = System.currentTimeMillis() + 1200;
        }

        if (showSpeedUpWarning && System.currentTimeMillis() > warningHideTime) {
            showSpeedUpWarning = false;
        }

        long now = System.currentTimeMillis();
        if (now - lastSpawnTime > spawnInterval) {
            spawnHazard();
            lastSpawnTime = now;
        }

        player.update(keyLeft, keyRight, keyJump, platforms, BASE_WIDTH);

        for (int i = hazards.size() - 1; i >= 0; i--) {
            Hazard h = hazards.get(i);
            h.update();
            if (h.checkCollision(player)) {
                gameState = GameState.GAMEOVER;
            }
            if (h.isOutOfBounds(BASE_WIDTH, BASE_HEIGHT)) {
                hazards.remove(i);
            }
        }

        if (activeFish != null) {
            if (activeFish.checkCollect(player)) {
                fishesCollected++;
                activeFish = null;
                fishRespawnTimer = 60;
            }
        } else {
            if (fishRespawnTimer > 0) {
                fishRespawnTimer--;
                if (fishRespawnTimer <= 0) {
                    spawnSingleFish();
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Tự động Scale theo độ phân giải màn hình khi phóng to/thu nhỏ
        double scaleX = (double) getWidth() / BASE_WIDTH;
        double scaleY = (double) getHeight() / BASE_HEIGHT;
        g2d.scale(scaleX, scaleY);

        MapConfig currentMap = mapsConfig.get(selectedMapIndex);
        if (currentMap.bgImage != null) {
            g2d.drawImage(currentMap.bgImage, 0, 0, BASE_WIDTH, BASE_HEIGHT, null);
        }

        for (Platform p : platforms) {
            p.draw(g2d, currentMap.platformImage);
        }

        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
            if (activeFish != null) {
                activeFish.draw(g2d, fishImage);
            }

            for (Hazard h : hazards)
                h.draw(g2d, BASE_WIDTH);
            player.draw(g2d);
            drawHUD(g2d);

            // Vẽ màn hình Pause đè lên nếu đang mở Menu Pause
            if (gameState == GameState.PAUSED) {
                drawPauseScreen(g2d);
            }
        } else if (gameState == GameState.TITLE)
            drawTitleScreen(g2d);
        else if (gameState == GameState.MAP_SELECT)
            drawMapSelectScreen(g2d);
        else if (gameState == GameState.HOW_TO_PLAY)
            drawHowToPlayScreen(g2d);
        else if (gameState == GameState.GAMEOVER)
            drawGameOverScreen(g2d);

        Toolkit.getDefaultToolkit().sync();
    }

    private void drawHUD(Graphics2D g2d) {
        // Tăng độ trong suốt nền HUD (alpha = 150) để vẫn quan sát được vật thể phía
        // sau
        g2d.setColor(new Color(15, 23, 42, 150));

        // Thu nhỏ kích thước 3 hộp thông số (Chiều cao giảm từ 36px -> 26px, đẩy sát
        // mép trên y = 8)
        g2d.fillRoundRect(15, 8, 110, 26, 10, 10); // TIME
        g2d.fillRoundRect(BASE_WIDTH / 2 - 70, 8, 140, 26, 10, 10); // SCORE
        g2d.fillRoundRect(BASE_WIDTH - 125, 8, 110, 26, 10, 10); // FISH

        // Font chữ thu nhỏ còn 12px gọn gàng
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.drawString(String.format("TIME: %.1fs", gameTime), 25, 25);
        g2d.drawString("SCORE: " + score, BASE_WIDTH / 2 - 42, 25);
        g2d.drawString("FISH: " + fishesCollected, BASE_WIDTH - 105, 25);

        // Thu nhỏ bảng SPEED UP! (chiều cao chỉ còn 22px, đẩy lên gần Score)
        if (showSpeedUpWarning) {
            g2d.setColor(new Color(239, 68, 68, 220));
            g2d.fillRoundRect(BASE_WIDTH / 2 - 60, 38, 120, 22, 10, 10);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2d.drawString("SPEED UP!", BASE_WIDTH / 2 - 30, 53);
        }
    }

    private void drawPauseScreen(Graphics2D g2d) {
        drawOverlayBackground(g2d);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 42));
        g2d.setColor(new Color(251, 191, 36));
        g2d.drawString("GAME PAUSED", BASE_WIDTH / 2 - 150, 150);

        drawButton(g2d, BASE_WIDTH / 2 - 100, 210, 200, 42, "RESUME", new Color(16, 185, 129));
        drawButton(g2d, BASE_WIDTH / 2 - 100, 265, 200, 42, "RESTART", new Color(37, 99, 235));
        drawButton(g2d, BASE_WIDTH / 2 - 100, 320, 200, 42, "MAIN MENU", new Color(51, 65, 85));
        drawButton(g2d, BASE_WIDTH / 2 - 100, 375, 200, 42, "QUIT", new Color(239, 68, 68));
    }

    private void drawTitleScreen(Graphics2D g2d) {
        drawOverlayBackground(g2d);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 54));
        g2d.setColor(new Color(251, 191, 36));
        g2d.drawString("DODGING CAT", BASE_WIDTH / 2 - 180, 180);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2d.setColor(new Color(148, 163, 184));
        g2d.drawString("Java Edition Platformer", BASE_WIDTH / 2 - 80, 215);

        drawButton(g2d, BASE_WIDTH / 2 - 100, 270, 200, 45, "PLAY GAME", new Color(16, 185, 129));
        drawButton(g2d, BASE_WIDTH / 2 - 100, 330, 200, 45, "HOW TO PLAY", new Color(51, 65, 85));
    }

    private void drawMapSelectScreen(Graphics2D g2d) {
        drawOverlayBackground(g2d);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
        g2d.setColor(Color.WHITE);
        g2d.drawString("SELECT MAP", BASE_WIDTH / 2 - 100, 120);

        for (int i = 0; i < mapsConfig.size(); i++) {
            int x = 120 + i * 190;
            int y = 180;
            boolean isSelected = (i == selectedMapIndex);

            g2d.setColor(isSelected ? new Color(251, 191, 36) : new Color(51, 65, 85));
            g2d.setStroke(new BasicStroke(isSelected ? 4 : 2));
            g2d.drawRoundRect(x, y, 160, 110, 12, 12);

            BufferedImage bgThumb = mapsConfig.get(i).bgImage;
            if (bgThumb != null) {
                g2d.drawImage(bgThumb, x + 4, y + 4, 152, 102, null);
            }

            g2d.setColor(new Color(15, 23, 42, 220));
            g2d.fillRect(x + 4, y + 80, 152, 26);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2d.drawString(mapsConfig.get(i).name, x + 20, y + 97);
        }

        drawButton(g2d, BASE_WIDTH / 2 - 170, 360, 150, 45, "BACK", new Color(51, 65, 85));
        drawButton(g2d, BASE_WIDTH / 2 + 20, 360, 150, 45, "START", new Color(16, 185, 129));
    }

    private void drawHowToPlayScreen(Graphics2D g2d) {
        drawOverlayBackground(g2d);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
        g2d.setColor(new Color(251, 191, 36));
        g2d.drawString("HOW TO PLAY", BASE_WIDTH / 2 - 110, 130);

        g2d.setColor(new Color(30, 41, 59, 220));
        g2d.fillRoundRect(BASE_WIDTH / 2 - 220, 160, 440, 180, 15, 15);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g2d.drawString("- Di chuyển: A / D hoặc Phím mũi tên", BASE_WIDTH / 2 - 180, 200);
        g2d.drawString("- Nhảy cao: W / Space / Phím mũi tên lên", BASE_WIDTH / 2 - 180, 235);
        g2d.drawString("- Tạm dừng: Bấm phím ESC", BASE_WIDTH / 2 - 180, 270);
        g2d.drawString("- Mục tiêu: Ăn cá 🐟 và sinh tồn lâu nhất có thể", BASE_WIDTH / 2 - 180, 305);

        drawButton(g2d, BASE_WIDTH / 2 - 80, 370, 160, 45, "GOT IT!", new Color(51, 65, 85));
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        drawOverlayBackground(g2d);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
        g2d.setColor(new Color(239, 68, 68));
        g2d.drawString("GAME OVER", BASE_WIDTH / 2 - 140, 180);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Final Score: " + score, BASE_WIDTH / 2 - 60, 230);

        drawButton(g2d, BASE_WIDTH / 2 - 170, 300, 150, 45, "MENU", new Color(51, 65, 85));
        drawButton(g2d, BASE_WIDTH / 2 + 20, 300, 150, 45, "TRY AGAIN", new Color(16, 185, 129));
    }

    private void drawOverlayBackground(Graphics2D g2d) {
        g2d.setColor(new Color(11, 15, 25, 220));
        g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
    }

    private void drawButton(Graphics2D g2d, int x, int y, int w, int h, String text, Color bg) {
        g2d.setColor(bg);
        g2d.fillRoundRect(x, y, w, h, 12, 12);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(text, tx, ty);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Bắt phím ESC để bật/tắt Tạm dừng game
        if (code == KeyEvent.VK_ESCAPE) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
            } else if (gameState == GameState.PAUSED) {
                gameState = GameState.PLAYING;
            }
        }

        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT)
            keyLeft = true;
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT)
            keyRight = true;
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_UP)
            keyJump = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT)
            keyLeft = false;
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT)
            keyRight = false;
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_UP)
            keyJump = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Tự động chuyển đổi tọa độ chuột khi phóng to/thu nhỏ cửa sổ
        double scaleX = (double) getWidth() / BASE_WIDTH;
        double scaleY = (double) getHeight() / BASE_HEIGHT;
        int mx = (int) (e.getX() / scaleX);
        int my = (int) (e.getY() / scaleY);

        if (gameState == GameState.TITLE) {
            if (isInBounds(mx, my, BASE_WIDTH / 2 - 100, 270, 200, 45))
                gameState = GameState.MAP_SELECT;
            else if (isInBounds(mx, my, BASE_WIDTH / 2 - 100, 330, 200, 45))
                gameState = GameState.HOW_TO_PLAY;
        } else if (gameState == GameState.MAP_SELECT) {
            for (int i = 0; i < mapsConfig.size(); i++) {
                int x = 120 + i * 190;
                if (isInBounds(mx, my, x, 180, 160, 110))
                    selectedMapIndex = i;
            }
            if (isInBounds(mx, my, BASE_WIDTH / 2 - 170, 360, 150, 45))
                gameState = GameState.TITLE;
            else if (isInBounds(mx, my, BASE_WIDTH / 2 + 20, 360, 150, 45))
                startGame();
        } else if (gameState == GameState.HOW_TO_PLAY) {
            if (isInBounds(mx, my, BASE_WIDTH / 2 - 80, 370, 160, 45))
                gameState = GameState.TITLE;
        } else if (gameState == GameState.PAUSED) {
            if (isInBounds(mx, my, BASE_WIDTH / 2 - 100, 210, 200, 42))
                gameState = GameState.PLAYING; // Resume
            else if (isInBounds(mx, my, BASE_WIDTH / 2 - 100, 265, 200, 42))
                startGame(); // Restart
            else if (isInBounds(mx, my, BASE_WIDTH / 2 - 100, 320, 200, 42))
                gameState = GameState.TITLE; // Main Menu
            else if (isInBounds(mx, my, BASE_WIDTH / 2 - 100, 375, 200, 42))
                System.exit(0); // Quit
        } else if (gameState == GameState.GAMEOVER) {
            if (isInBounds(mx, my, BASE_WIDTH / 2 - 170, 300, 150, 45))
                gameState = GameState.TITLE;
            else if (isInBounds(mx, my, BASE_WIDTH / 2 + 20, 300, 150, 45))
                startGame();
        }
    }

    private boolean isInBounds(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}