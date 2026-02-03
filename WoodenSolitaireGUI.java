import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class WoodenSolitaireGUI extends JFrame implements GameUIController {
    private static final Color COLOR_BG = new Color(39, 30, 112);
    private static final int BASE_LEFT_WIDTH = 220;
    private static final int BASE_RIGHT_WIDTH = 260;
    private static final int BASE_BOARD = 512;
    private static final int BASE_HEIGHT = 560;

    private final AssetManager assets = new AssetManager();
    private final GameModel model = new GameModel();
    private final LeaderboardManager leaderboard = new LeaderboardManager();
    private final GamePanel gamePanel;
    private final UIPanelLeftPowerups leftPanel;
    private final UIPanelRightLeaderboard rightPanel;
    private boolean gameOverHandled;

    public WoodenSolitaireGUI() {
        super("Wooden Solitaire Pixelart");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout());

        gamePanel = new GamePanel(assets, model, this);
        leftPanel = new UIPanelLeftPowerups(assets, model, this);
        rightPanel = new UIPanelRightLeaderboard(assets, model, leaderboard, this);

        add(leftPanel, BorderLayout.WEST);
        add(gamePanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateScale();
            }
        });

        updateScale();
        pack();
        setLocationRelativeTo(null);
        enableFullscreen();
        updateScale();

        Timer repaintTimer = new Timer(1000 / 30, event -> repaint());
        repaintTimer.start();
    }

    private void enableFullscreen() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (device.isFullScreenSupported()) {
            device.setFullScreenWindow(this);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setVisible(true);
        }
    }

    private void updateScale() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        int baseWidth = BASE_LEFT_WIDTH + BASE_BOARD + BASE_RIGHT_WIDTH + 64;
        int baseHeight = BASE_HEIGHT;
        double scale = Math.max(1, Math.min(width / (double) baseWidth, height / (double) baseHeight));
        leftPanel.setScale(scale);
        rightPanel.setScale(scale);
        gamePanel.setScale(scale);
        revalidate();
    }

    @Override
    public void requestLaserDirection() {
        int result = JOptionPane.showOptionDialog(this,
                "Laser Richtung auswählen",
                "Laser",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"Horizontal", "Vertikal", "Abbrechen"},
                "Horizontal");
        if (result == 0) {
            model.applyLaserDirection(true);
        } else if (result == 1) {
            model.applyLaserDirection(false);
        } else {
            model.cancelPowerup();
            model.clearLaserPending();
        }
        onModelUpdated();
    }

    @Override
    public void onModelUpdated() {
        repaint();
        if (model.isGameOver()) {
            if (!gameOverHandled) {
                handleGameOver();
                gameOverHandled = true;
            }
        } else {
            gameOverHandled = false;
        }
    }

    @Override
    public void requestMenu() {
        int result = showOptionDialog("Menü", "Menu", new String[]{"Resume", "New Game", "Quit"}, "Resume");
        if (result == 1) {
            model.resetGame();
            gameOverHandled = false;
        } else if (result == 2) {
            System.exit(0);
        }
        onModelUpdated();
    }

    @Override
    public void requestRestart() {
        model.resetGame();
        gameOverHandled = false;
        onModelUpdated();
    }

    @Override
    public void requestPlayerName() {
        String name = showInputDialog("Name für Leaderboard:", model.getPlayerName());
        if (name != null && !name.trim().isEmpty()) {
            model.setPlayerName(name);
        }
        onModelUpdated();
    }

    private void handleGameOver() {
        LeaderboardManager.Entry entry = new LeaderboardManager.Entry(
                "",
                model.getPegsLeft(),
                model.getMovesCount(),
                System.currentTimeMillis(),
                true);
        if (!leaderboard.isTop10Candidate(entry)) {
            return;
        }
        String name = showInputDialog("Name für Leaderboard:", model.getPlayerName());
        if (name == null || name.trim().isEmpty()) {
            name = model.getPlayerName();
        }
        model.setPlayerName(name);
        entry = new LeaderboardManager.Entry(name.trim(), entry.pegsLeft(), entry.moves(), entry.timestamp(), entry.powerupsEnabled());
        leaderboard.addEntry(entry);
        repaint();
    }

    private int showOptionDialog(String message, String title, String[] options, String initialValue) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, initialValue);
        JDialog dialog = pane.createDialog(this, title);
        dialog.setAlwaysOnTop(true);
        dialog.setLocationRelativeTo(this);
        dialog.setModal(true);
        dialog.setVisible(true);
        Object value = pane.getValue();
        dialog.dispose();
        if (value == null) {
            return -1;
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    private String showInputDialog(String message, String initialValue) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        pane.setWantsInput(true);
        pane.setInitialSelectionValue(initialValue);
        JDialog dialog = pane.createDialog(this, "Input");
        dialog.setAlwaysOnTop(true);
        dialog.setLocationRelativeTo(this);
        dialog.setModal(true);
        dialog.setVisible(true);
        Object value = pane.getInputValue();
        dialog.dispose();
        if (value == JOptionPane.UNINITIALIZED_VALUE) {
            return null;
        }
        return value == null ? null : value.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WoodenSolitaireGUI gui = new WoodenSolitaireGUI();
            gui.setVisible(true);
        });
    }
}
