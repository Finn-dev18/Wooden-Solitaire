import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

public class OverlayPanel extends JPanel {
    private static final Color COLOR_TEXT = new Color(245, 241, 235);
    private static final Color COLOR_TEXT_MUTED = new Color(224, 230, 255);
    private static final Color COLOR_TEXT_DARK = new Color(30, 30, 30);
    private static final int MAX_NAME_LENGTH = 12;

    private final AssetManager assets;
    private final GameModel model;
    private final LeaderboardManager leaderboard;
    private final WoodenSolitaireGUI controller;
    private OverlayState state = OverlayState.NONE;

    private final Map<OverlayButton, Rectangle> buttonRects = new EnumMap<>(OverlayButton.class);
    private OverlayButton hovered;
    private OverlayButton pressed;
    private Rectangle nameInputRect;
    private boolean nameInputFocused = true;
    private String nameInput = "";
    private boolean cursorOn = true;
    private Timer cursorTimer;
    private boolean top10Candidate;

    public OverlayPanel(AssetManager assets, GameModel model, LeaderboardManager leaderboard, WoodenSolitaireGUI controller) {
        this.assets = assets;
        this.model = model;
        this.leaderboard = leaderboard;
        this.controller = controller;
        setOpaque(false);
        setFocusable(true);
        initListeners();
        startCursorTimer();
    }

    public void setOverlayState(OverlayState state) {
        this.state = state;
        if (state == OverlayState.PRE_GAME_NAME) {
            nameInput = "";
            nameInputFocused = true;
        }
        if (state == OverlayState.GAME_OVER) {
            nameInput = model.getPlayerName();
            nameInputFocused = true;
            top10Candidate = isTop10Candidate();
        }
        setVisible(state != OverlayState.NONE);
        if (state != OverlayState.NONE) {
            requestFocusInWindow();
        }
        repaint();
    }

    public void handleEnterKey() {
        if (state == OverlayState.PRE_GAME_NAME && canStart()) {
            submitNameAndStart();
        } else if (state == OverlayState.GAME_OVER && top10Candidate) {
            submitGameOverName();
        }
    }

    public OverlayState getOverlayState() {
        return state;
    }

    public boolean isOverlayActive() {
        return state != OverlayState.NONE;
    }

    private void initListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressed = findButton(e.getX(), e.getY());
                if (nameInputRect != null) {
                    nameInputFocused = nameInputRect.contains(e.getPoint());
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                OverlayButton released = findButton(e.getX(), e.getY());
                if (pressed != null && pressed == released) {
                    handleButtonClick(pressed);
                }
                pressed = null;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = null;
                repaint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hovered = findButton(e.getX(), e.getY());
                repaint();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (state == OverlayState.NONE) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    controller.handleEscape();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    controller.handleEnter();
                    return;
                }
                if (!isNameEntryActive()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    if (!nameInput.isEmpty()) {
                        nameInput = nameInput.substring(0, nameInput.length() - 1);
                        repaint();
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (!isNameEntryActive()) {
                    return;
                }
                if (!nameInputFocused) {
                    return;
                }
                char ch = e.getKeyChar();
                if (Character.isISOControl(ch)) {
                    return;
                }
                if (nameInput.length() >= MAX_NAME_LENGTH) {
                    return;
                }
                nameInput += ch;
                repaint();
            }
        });
    }

    private void startCursorTimer() {
        cursorTimer = new Timer(500, event -> {
            cursorOn = !cursorOn;
            if (isNameEntryActive()) {
                repaint();
            }
        });
        cursorTimer.start();
    }

    private OverlayButton findButton(int x, int y) {
        for (Map.Entry<OverlayButton, Rectangle> entry : buttonRects.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isNameEntryActive() {
        return state == OverlayState.PRE_GAME_NAME || (state == OverlayState.GAME_OVER && top10Candidate);
    }

    private void handleButtonClick(OverlayButton button) {
        switch (button) {
            case START:
                if (canStart()) {
                    submitNameAndStart();
                }
                break;
            case QUIT:
                System.exit(0);
                break;
            case RESUME:
                controller.setOverlayState(OverlayState.NONE);
                break;
            case NEW_GAME:
                model.resetGame();
                controller.setOverlayState(OverlayState.NONE);
                break;
            case MENU:
                controller.setOverlayState(OverlayState.MENU);
                break;
            case RESTART:
                model.resetGame();
                controller.setOverlayState(OverlayState.NONE);
                break;
            case SAVE:
                submitGameOverName();
                break;
            default:
                break;
        }
        controller.onModelUpdated();
    }

    private void submitNameAndStart() {
        String trimmed = nameInput == null ? "" : nameInput.trim();
        if (trimmed.isEmpty()) {
            trimmed = "Player";
        }
        model.setPlayerName(trimmed);
        model.resetGame();
        controller.setOverlayState(OverlayState.NONE);
    }

    private void submitGameOverName() {
        if (!top10Candidate) {
            return;
        }
        String trimmed = nameInput == null ? "" : nameInput.trim();
        if (trimmed.isEmpty()) {
            trimmed = model.getPlayerName();
        }
        if (trimmed.isEmpty()) {
            trimmed = "Player";
        }
        model.setPlayerName(trimmed);
        int score = ScoreCalculator.calculate(model.getElapsedDuration(), model.getPegsLeft(), model.getMovesCount());
        long durationSeconds = model.getElapsedDuration().getSeconds();
        LeaderboardManager.Entry entry = new LeaderboardManager.Entry(
                trimmed,
                score,
                model.getPegsLeft(),
                durationSeconds,
                System.currentTimeMillis(),
                true);
        leaderboard.addEntry(entry);
        top10Candidate = false;
    }

    private boolean isTop10Candidate() {
        int score = ScoreCalculator.calculate(model.getElapsedDuration(), model.getPegsLeft(), model.getMovesCount());
        long durationSeconds = model.getElapsedDuration().getSeconds();
        LeaderboardManager.Entry entry = new LeaderboardManager.Entry(
                model.getPlayerName(),
                score,
                model.getPegsLeft(),
                durationSeconds,
                System.currentTimeMillis(),
                true);
        return leaderboard.isTop10Candidate(entry);
    }

    public boolean canStart() {
        String trimmed = nameInput == null ? "" : nameInput.trim();
        return !trimmed.isEmpty();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (state == OverlayState.NONE) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        drawDimLayer(g2d);
        drawPanelAndContents(g2d);

        g2d.dispose();
    }

    private void drawDimLayer(Graphics2D g2d) {
        BufferedImage dim = assets.getImage("ui_overlay_dim_1920x1080.png");
        g2d.drawImage(dim, 0, 0, getWidth(), getHeight(), null);
    }

    private void drawPanelAndContents(Graphics2D g2d) {
        buttonRects.clear();
        nameInputRect = null;

        BufferedImage panelImage = assets.getImage(getPanelAsset());
        int panelWidth = (int) Math.round(panelImage.getWidth() * controller.getScale());
        int panelHeight = (int) Math.round(panelImage.getHeight() * controller.getScale());
        int panelX = (getWidth() - panelWidth) / 2;
        int panelY = (getHeight() - panelHeight) / 2;
        Rectangle panelRect = new Rectangle(panelX, panelY, panelWidth, panelHeight);
        g2d.drawImage(panelImage, panelRect.x, panelRect.y, panelRect.width, panelRect.height, null);

        drawBanner(g2d, panelRect);
        drawOverlayContent(g2d, panelRect);
    }

    private String getPanelAsset() {
        return state == OverlayState.GAME_OVER ? "ui_gameover_panel_640x420.png" : "ui_menu_panel_640x420.png";
    }

    private void drawBanner(Graphics2D g2d, Rectangle panelRect) {
        BufferedImage banner = assets.getImage("ui_banner_520x120.png");
        int bannerWidth = (int) Math.round(banner.getWidth() * controller.getScale());
        int bannerHeight = (int) Math.round(banner.getHeight() * controller.getScale());
        int bannerX = panelRect.x + (panelRect.width - bannerWidth) / 2;
        int bannerY = panelRect.y + (int) Math.round(12 * controller.getScale());
        g2d.drawImage(banner, bannerX, bannerY, bannerWidth, bannerHeight, null);
    }

    private void drawOverlayContent(Graphics2D g2d, Rectangle panelRect) {
        float fontScale = (float) controller.getScale();
        int contentX = panelRect.x + (int) Math.round(40 * controller.getScale());
        int contentY = panelRect.y + (int) Math.round(150 * controller.getScale());

        if (state == OverlayState.PRE_GAME_NAME) {
            drawPreGame(g2d, panelRect, contentX, contentY, fontScale);
        } else if (state == OverlayState.MENU) {
            drawMenu(g2d, panelRect, contentX, contentY, fontScale);
        } else if (state == OverlayState.GAME_OVER) {
            drawGameOver(g2d, panelRect, contentX, contentY, fontScale);
        }
    }

    private void drawPreGame(Graphics2D g2d, Rectangle panelRect, int contentX, int contentY, float fontScale) {
        g2d.setColor(COLOR_TEXT);
        g2d.setFont(getFont().deriveFont(Font.BOLD, 16f * fontScale));
        g2d.drawString("PLAYER NAME", contentX, contentY);

        int inputY = contentY + (int) Math.round(24 * controller.getScale());
        drawNameInput(g2d, panelRect, inputY, fontScale);

        int buttonY = panelRect.y + panelRect.height - (int) Math.round(120 * controller.getScale());
        drawButtonRow(g2d, panelRect, buttonY, OverlayButton.START, OverlayButton.QUIT);
    }

    private void drawMenu(Graphics2D g2d, Rectangle panelRect, int contentX, int contentY, float fontScale) {
        g2d.setColor(COLOR_TEXT);
        g2d.setFont(getFont().deriveFont(Font.BOLD, 16f * fontScale));
        g2d.drawString("MENU", contentX, contentY);

        int buttonY = contentY + (int) Math.round(40 * controller.getScale());
        drawButtonColumn(g2d, panelRect, buttonY, new OverlayButton[]{
                OverlayButton.RESUME,
                OverlayButton.NEW_GAME,
                OverlayButton.QUIT
        });
    }

    private void drawGameOver(Graphics2D g2d, Rectangle panelRect, int contentX, int contentY, float fontScale) {
        g2d.setColor(COLOR_TEXT);
        g2d.setFont(getFont().deriveFont(Font.BOLD, 16f * fontScale));
        g2d.drawString("GAME OVER", contentX, contentY);

        int statY = contentY + (int) Math.round(26 * controller.getScale());
        g2d.setFont(getFont().deriveFont(Font.PLAIN, 12f * fontScale));
        g2d.setColor(COLOR_TEXT_MUTED);
        g2d.drawString("Moves: " + model.getMovesCount(), contentX, statY);
        statY += (int) Math.round(18 * controller.getScale());
        g2d.drawString("Pegs: " + model.getPegsLeft(), contentX, statY);
        statY += (int) Math.round(18 * controller.getScale());
        int score = ScoreCalculator.calculate(model.getElapsedDuration(), model.getPegsLeft(), model.getMovesCount());
        g2d.drawString("Score: " + score, contentX, statY);

        int inputY = statY + (int) Math.round(20 * controller.getScale());
        if (top10Candidate) {
            drawNameInput(g2d, panelRect, inputY, fontScale);
        }

        int buttonY = panelRect.y + panelRect.height - (int) Math.round(120 * controller.getScale());
        if (top10Candidate) {
            drawButtonRow(g2d, panelRect, buttonY, OverlayButton.SAVE, OverlayButton.RESTART);
            int secondaryY = buttonY + (int) Math.round(80 * controller.getScale());
            drawButtonRow(g2d, panelRect, secondaryY, OverlayButton.MENU);
        } else {
            drawButtonRow(g2d, panelRect, buttonY, OverlayButton.RESTART, OverlayButton.MENU);
        }
    }

    private void drawNameInput(Graphics2D g2d, Rectangle panelRect, int inputY, float fontScale) {
        BufferedImage inputImage = assets.getImage(nameInputFocused ? "ui_name_input_focus_560x104.png" : "ui_name_input_560x104.png");
        int inputWidth = (int) Math.round(inputImage.getWidth() * controller.getScale());
        int inputHeight = (int) Math.round(inputImage.getHeight() * controller.getScale());
        int inputX = panelRect.x + (panelRect.width - inputWidth) / 2;
        nameInputRect = new Rectangle(inputX, inputY, inputWidth, inputHeight);
        g2d.drawImage(inputImage, inputX, inputY, inputWidth, inputHeight, null);

        String text = nameInput == null ? "" : nameInput;
        g2d.setFont(getFont().deriveFont(Font.BOLD, 14f * fontScale));
        g2d.setColor(COLOR_TEXT_DARK);
        int textX = inputX + (int) Math.round(24 * controller.getScale());
        int textY = inputY + (int) Math.round(58 * controller.getScale());
        g2d.drawString(text, textX, textY);

        if (nameInputFocused && cursorOn) {
            int textWidth = g2d.getFontMetrics().stringWidth(text);
            int cursorX = textX + textWidth + (int) Math.round(4 * controller.getScale());
            int cursorY = textY - (int) Math.round(12 * controller.getScale());
            g2d.fillRect(cursorX, cursorY, (int) Math.round(2 * controller.getScale()), (int) Math.round(14 * controller.getScale()));
        }
    }

    private void drawButtonRow(Graphics2D g2d, Rectangle panelRect, int y, OverlayButton... buttons) {
        int buttonWidth = (int) Math.round(260 * controller.getScale());
        int buttonHeight = (int) Math.round(72 * controller.getScale());
        int gap = (int) Math.round(20 * controller.getScale());
        int totalWidth = buttonWidth * buttons.length + gap * (buttons.length - 1);
        int startX = panelRect.x + (panelRect.width - totalWidth) / 2;
        for (int i = 0; i < buttons.length; i++) {
            int x = startX + i * (buttonWidth + gap);
            Rectangle rect = new Rectangle(x, y, buttonWidth, buttonHeight);
            buttonRects.put(buttons[i], rect);
            drawButton(g2d, rect, buttons[i]);
        }
    }

    private void drawButtonColumn(Graphics2D g2d, Rectangle panelRect, int startY, OverlayButton[] buttons) {
        int buttonWidth = (int) Math.round(260 * controller.getScale());
        int buttonHeight = (int) Math.round(72 * controller.getScale());
        int gap = (int) Math.round(16 * controller.getScale());
        int x = panelRect.x + (panelRect.width - buttonWidth) / 2;
        for (int i = 0; i < buttons.length; i++) {
            int y = startY + i * (buttonHeight + gap);
            Rectangle rect = new Rectangle(x, y, buttonWidth, buttonHeight);
            buttonRects.put(buttons[i], rect);
            drawButton(g2d, rect, buttons[i]);
        }
    }

    private void drawButton(Graphics2D g2d, Rectangle rect, OverlayButton button) {
        boolean disabled = button == OverlayButton.START && !canStart();
        BufferedImage image;
        if (disabled) {
            image = assets.getImage("ui_button_disabled_260x72.png");
        } else if (pressed == button) {
            image = assets.getImage("ui_button_pressed_260x72.png");
        } else if (hovered == button) {
            image = assets.getImage("ui_button_hover_260x72.png");
        } else {
            image = assets.getImage("ui_button_normal_260x72.png");
        }
        g2d.drawImage(image, rect.x, rect.y, rect.width, rect.height, null);

        g2d.setFont(getFont().deriveFont(Font.BOLD, 14f * (float) controller.getScale()));
        g2d.setColor(COLOR_TEXT_DARK);
        String label = button.getLabel();
        int textWidth = g2d.getFontMetrics().stringWidth(label);
        int textX = rect.x + (rect.width - textWidth) / 2;
        int textY = rect.y + rect.height / 2 + (int) Math.round(6 * controller.getScale());
        g2d.drawString(label, textX, textY);
    }

    enum OverlayButton {
        START("START"),
        QUIT("QUIT"),
        RESUME("RESUME"),
        NEW_GAME("NEW GAME"),
        MENU("MENU"),
        RESTART("RESTART"),
        SAVE("SAVE");

        private final String label;

        OverlayButton(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
