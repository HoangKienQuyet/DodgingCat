import javax.swing.*;
import java.awt.*;

public class DodgingCatApp extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private GamePanel gamePanel;
    private MainMenuPanel menuPanel;

    public DodgingCatApp() {
        setTitle("Dodging Cat - Desktop Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(640, 360));
        setSize(960, 560);
        setResizable(true);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        menuPanel = new MainMenuPanel(this);
        gamePanel = new GamePanel(this);

        mainPanel.add(menuPanel, "MENU");
        mainPanel.add(gamePanel, "GAME");

        add(mainPanel);
        showMenu();
    }

    public void showMenu() {
        cardLayout.show(mainPanel, "MENU");
    }

    // Cập nhật hàm này để nhận số lượng người chơi (1 hoặc 2)
    public void startMapSelect(int players) {
        gamePanel.setNumPlayers(players); // Truyền số lượng người chơi vào GamePanel
        cardLayout.show(mainPanel, "GAME");
        gamePanel.openMapSelect();
        gamePanel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DodgingCatApp().setVisible(true);
        });
    }
}