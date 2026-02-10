import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

public class WoodenSolitaireGUI extends JFrame implements GameUIController {
    private static final Color COLOR_BG = new Color(39, 30, 112);
    private static final int BASE_LEFT_WIDTH = 220;
    private static final int BASE_RIGHT_WIDTH = 240;
    private static final int BASE_BOARD = 256;
    private static final int BASE_GAP = 28;
    private static final int BASE_HEIGHT = 512;

    private final AssetManager assets = new AssetManager();
    private final GameModel model = new GameModel();
    private final LeaderboardManager leaderboard = new LeaderboardManager();
    private final GamePanel gamePanel;
    private final UIPanelLeftPowerups leftPanel;
    private final UIPanelRightLeaderboard rightPanel;
    private final OverlayPanel overlayPanel;
    private int scale = 2;
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
        overlayPanel = new OverlayPanel(assets, model, leaderboard, this);

        assets.preload(
                "ui_overlay_dim_1920x1080.png",
                "ui_menu_panel_640x420.png",
                "ui_gameover_panel_640x420.png",
                "ui_banner_520x120.png",
                "ui_name_input_560x104.png",
                "ui_name_input_focus_560x104.png",
                "ui_button_normal_260x72.png",
                "ui_button_hover_260x72.png",
                "ui_button_pressed_260x72.png",
                "ui_button_disabled_260x72.png",
                "menu_button_normal_240x64.png",
                "menu_button_hover_240x64.png",
                "menu_button_pressed_240x64.png",
                "icon_hint_16.png",
                "icon_bomb_16.png",
                "icon_swap_16.png",
                "icon_freeze_16.png",
                "icon_laser_16.png",
                "icon_shield_16.png");

        add(leftPanel, BorderLayout.WEST);
        add(gamePanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        setGlassPane(overlayPanel);
        overlayPanel.setVisible(true);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateScale();
            }
        });

        setupKeyBindings();
        updateScale();
        pack();
        setLocationRelativeTo(null);
        enableFullscreen();
        updateScale();

        overlayPanel.setOverlayState(OverlayState.PRE_GAME_NAME);

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
        int baseWidth = BASE_LEFT_WIDTH + BASE_BOARD + BASE_RIGHT_WIDTH + BASE_GAP;
        double scaleValue = Math.min(width / (double) baseWidth, height / (double) BASE_HEIGHT);
        int nextScale = (int) Math.floor(scaleValue);
        nextScale = Math.max(1, Math.min(6, nextScale));
        if (nextScale != scale) {
            scale = nextScale;
        }
        leftPanel.setScale(scale);
        rightPanel.setScale(scale);
        gamePanel.setScale(scale);
        revalidate();
    }

    private void setupKeyBindings() {
        JRootPane root = getRootPane();
        root.getInputMap(JRootPane.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "toggleMenu");
        root.getActionMap().put("toggleMenu", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleEscape();
            }
        });

        root.getInputMap(JRootPane.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "overlayEnter");
        root.getActionMap().put("overlayEnter", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleEnter();
            }
        });

        root.getInputMap(JRootPane.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), "laserHorizontal");
        root.getActionMap().put("laserHorizontal", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.hasLaserPending() && overlayPanel.getOverlayState() == OverlayState.NONE) {
                    model.applyLaserDirection(true);
                    onModelUpdated();
                }
            }
        });

        root.getInputMap(JRootPane.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_V, 0), "laserVertical");
        root.getActionMap().put("laserVertical", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.hasLaserPending() && overlayPanel.getOverlayState() == OverlayState.NONE) {
                    model.applyLaserDirection(false);
                    onModelUpdated();
                }
            }
        });

        root.getInputMap(JRootPane.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "laserHorizontalLeft");
        root.getActionMap().put("laserHorizontalLeft", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.hasLaserPending() && overlayPanel.getOverlayState() == OverlayState.NONE) {
                    model.applyLaserDirection(true);
                    onModelUpdated();
                }
            }
        });

        root.getInputMap(JRootPane.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "laserHorizontalRight");
        root.getActionMap().put("laserHorizontalRight", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.hasLaserPending() && overlayPanel.getOverlayState() == OverlayState.NONE) {
                    model.applyLaserDirection(true);
                    onModelUpdated();
                }
            }
        });

        root.getInputMap(JRootPane.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "laserVerticalUp");
        root.getActionMap().put("laserVerticalUp", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.hasLaserPending() && overlayPanel.getOverlayState() == OverlayState.NONE) {
                    model.applyLaserDirection(false);
                    onModelUpdated();
                }
            }
        });

        root.getInputMap(JRootPane.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "laserVerticalDown");
        root.getActionMap().put("laserVerticalDown", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.hasLaserPending() && overlayPanel.getOverlayState() == OverlayState.NONE) {
                    model.applyLaserDirection(false);
                    onModelUpdated();
                }
            }
        });
    }

    public void setOverlayState(OverlayState state) {
        overlayPanel.setOverlayState(state);
    }

    public void handleEscape() {
        if (overlayPanel.getOverlayState() == OverlayState.NONE) {
            overlayPanel.setOverlayState(OverlayState.MENU);
        } else if (overlayPanel.getOverlayState() == OverlayState.MENU) {
            overlayPanel.setOverlayState(OverlayState.NONE);
        }
    }

    public void handleEnter() {
        overlayPanel.handleEnterKey();
    }

    @Override
    public void requestLaserDirection() {
        onModelUpdated();
    }

    @Override
    public void onModelUpdated() {
        repaint();
        if (model.isGameOver()) {
            if (!gameOverHandled) {
                overlayPanel.setOverlayState(OverlayState.GAME_OVER);
                gameOverHandled = true;
            }
        } else {
            gameOverHandled = false;
        }
    }

    @Override
    public void requestMenu() {
        overlayPanel.setOverlayState(OverlayState.MENU);
    }

    @Override
    public void requestRestart() {
        model.resetGame();
        gameOverHandled = false;
        overlayPanel.setOverlayState(OverlayState.NONE);
        onModelUpdated();
    }

    @Override
    public boolean isOverlayActive() {
        return overlayPanel.isOverlayActive();
    }

    @Override
    public OverlayState getOverlayState() {
        return overlayPanel.getOverlayState();
    }

    public int getScale() {
        return scale;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WoodenSolitaireGUI gui = new WoodenSolitaireGUI();
            gui.setVisible(true);
        });
    }
}
