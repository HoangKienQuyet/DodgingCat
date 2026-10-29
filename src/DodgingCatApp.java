import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

public class DodgingCatApp extends JFrame {

    public DodgingCatApp() {
        setTitle("Dodging Cat - Java Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Cho phép phóng to/thu nhỏ cửa sổ
        setResizable(true);
        setMinimumSize(new Dimension(640, 360)); // Giới hạn kích thước nhỏ nhất

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DodgingCatApp::new);
    }
}