import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

public class WoodenSolitaireGUI extends JFrame implements GameUIController {
    private static final Color COLOR_BG = new Color(39, 30, 112);

    private static final int BOARD_BASE_PX = 768;
    private static final int TOP_UI_MARGIN = 96;
    private static final int CENTER_MARGIN = 24;
    private static final int MAX_BOARD_SCALE = 1;
    private static final int BASE_LEFT_WIDTH = 420;
    private static final int BASE_RIGHT_WIDTH = 360;
    private static final int UI_SCALE = 1;
    private static final int GRID_SCALE = 3;

    private final AssetManager assets = new AssetManager();
    private final GameModel model = new GameModel();
    private final LeaderboardManager leaderboard = new LeaderboardManager();
    private final GamePanel gamePanel;
    private final UIPanelLeftPowerups leftPanel;
    private final UIPanelRightLeaderboard rightPanel;
    private final OverlayPanel overlayPanel;
    private final JPanel centerWrapper;

    private int uiScale = UI_SCALE;
    private int boardScale = 1;
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

        centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(true);
        centerWrapper.setBackground(COLOR_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        centerWrapper.add(gamePanel, gbc);

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
                "icon_bomb_16.png",
                "icon_swap_16.png",
                "icon_hint_16.png",
                "icon_laser_16.png",
                "icon_freeze_16.png");

        add(leftPanel, BorderLayout.WEST);
        add(centerWrapper, BorderLayout.CENTER);
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
        int frameWidth = getContentPane().getWidth();
        int frameHeight = getContentPane().getHeight();
        if (frameWidth <= 0 || frameHeight <= 0) {
            return;
        }

        int nextUiScale = UI_SCALE;

        int leftWidth = BASE_LEFT_WIDTH * nextUiScale;
        int rightWidth = BASE_RIGHT_WIDTH * nextUiScale;

        applyFixedPanelWidth(leftPanel, leftWidth, frameHeight);
        applyFixedPanelWidth(rightPanel, rightWidth, frameHeight);

        int availableW = Math.max(1, frameWidth - leftWidth - rightWidth - (CENTER_MARGIN * 2));
        int availableH = Math.max(1, frameHeight - TOP_UI_MARGIN - (CENTER_MARGIN * 2));
        int nextBoardScale = Math.min(availableW / BOARD_BASE_PX, availableH / BOARD_BASE_PX);
        nextBoardScale = clamp(nextBoardScale, 1, MAX_BOARD_SCALE);

        uiScale = nextUiScale;
        boardScale = nextBoardScale;

        leftPanel.setScale(uiScale);
        rightPanel.setScale(uiScale);
        gamePanel.setUiScale(UI_SCALE);
        gamePanel.setGridScale(GRID_SCALE);
        gamePanel.setBoardLayoutMargins(TOP_UI_MARGIN, CENTER_MARGIN);
        gamePanel.setBoardScale(boardScale);

        int boardSize = BOARD_BASE_PX * boardScale;

        Dimension boardDimension = new Dimension(boardSize, TOP_UI_MARGIN + boardSize + (CENTER_MARGIN * 2));
        gamePanel.setPreferredSize(boardDimension);
        gamePanel.setMinimumSize(boardDimension);
        gamePanel.setMaximumSize(boardDimension);
        gamePanel.revalidate();

        centerWrapper.revalidate();
        centerWrapper.repaint();
        revalidate();
        repaint();

        if (gamePanel.getPreferredSize().width <= 0) {
            Dimension fallback = new Dimension(BOARD_BASE_PX, TOP_UI_MARGIN + BOARD_BASE_PX + (CENTER_MARGIN * 2));
            gamePanel.setPreferredSize(fallback);
            gamePanel.setMinimumSize(fallback);
            gamePanel.setMaximumSize(fallback);
            gamePanel.revalidate();
            centerWrapper.revalidate();
        }
    }

    private void applyFixedPanelWidth(JPanel panel, int width, int frameHeight) {
        panel.setPreferredSize(new Dimension(width, frameHeight));
        panel.setMinimumSize(new Dimension(width, 0));
        panel.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

        registerPowerupHotkey(root, KeyEvent.VK_1, PowerupType.UNDO);
        registerPowerupHotkey(root, KeyEvent.VK_2, PowerupType.SWAP);
        registerPowerupHotkey(root, KeyEvent.VK_3, PowerupType.BOMB);
        registerPowerupHotkey(root, KeyEvent.VK_4, PowerupType.BRIDGEJUMP);
        registerPowerupHotkey(root, KeyEvent.VK_5, PowerupType.RANDSTURM);
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

    private void registerPowerupHotkey(JRootPane root, int keyCode, PowerupType type) {
        String actionName = "powerup_" + type.name();
        root.getInputMap(JRootPane.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(keyCode, 0), actionName);
        root.getActionMap().put(actionName, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (overlayPanel.getOverlayState() != OverlayState.NONE) {
                    return;
                }
                model.activatePowerup(type);
                onModelUpdated();
            }
        });
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
        overlayPanel.setOverlayState(OverlayState.PRE_GAME_NAME);
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
        return uiScale;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WoodenSolitaireGUI gui = new WoodenSolitaireGUI();
            gui.setVisible(true);
        });
    }
}
