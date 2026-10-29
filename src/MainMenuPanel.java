import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MainMenuPanel extends JPanel {
    private DodgingCatApp app;
    private BufferedImage bgImage;

    public MainMenuPanel(DodgingCatApp app) {
        this.app = app;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        bgImage = AssetLoader.loadImage("assests/ui/ui.png");

        // Nút 1 Người chơi
        JButton p1Button = createButton("1 PLAYER", new Color(16, 185, 129));
        p1Button.addActionListener(e -> app.startMapSelect(1));

        // Nút 2 Người chơi (Đã xóa chữ CO-OP)
        JButton p2Button = createButton("2 PLAYERS", new Color(37, 99, 235));
        p2Button.addActionListener(e -> app.startMapSelect(2));

        // Nút Thoát
        JButton exitButton = createButton("QUIT", new Color(239, 68, 68));
        exitButton.addActionListener(e -> System.exit(0));

        // Tăng khoảng cách đẩy từ mép trên xuống (350px) để không che Logo game
        add(Box.createVerticalStrut(350));
        add(p1Button);
        add(Box.createVerticalStrut(15));
        add(p2Button);
        add(Box.createVerticalStrut(15));
        add(exitButton);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            g.setColor(new Color(11, 15, 25));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 22));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(280, 50));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}