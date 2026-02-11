import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
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
    private static final Color COLOR_TEXT_DARK = new Color(245, 241, 235);
    private static final Color COLOR_TEXT_INPUT = new Color(245, 241, 235);
    private static final int MAX_NAME_LENGTH = 12;
    private static final int PANEL_PADDING_PX = 48;
    private static final int HEADER_HEIGHT_PX = 96;
    private static final int MENU_BUTTON_WIDTH_PX = 360;
    private static final int ROW_BUTTON_WIDTH_PX = 260;
    private static final int BUTTON_HEIGHT_PX = 72;
    private static final int BUTTON_GAP_PX = 24;
    private static final int INPUT_TO_BUTTON_GAP_PX = 24;
    private static final int INPUT_TEXT_TOP_INSET_PX = 18;
    private static final int INPUT_TEXT_BOTTOM_INSET_PX = 12;

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
            nameInputFocused = false;
            top10Candidate = isTop10Candidate();
            submitGameOverEntry();
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
        return state == OverlayState.PRE_GAME_NAME;
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
                controller.setOverlayState(OverlayState.PRE_GAME_NAME);
                break;
            case MENU:
                controller.setOverlayState(OverlayState.MENU);
                break;
            case RESTART:
                model.resetGame();
                controller.setOverlayState(OverlayState.PRE_GAME_NAME);
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
        model.startTimerNow();
        controller.setOverlayState(OverlayState.NONE);
    }

    private void submitGameOverEntry() {
        if (!top10Candidate) {
            return;
        }
        String playerName = model.getPlayerName();
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Player";
        }
        int score = ScoreCalculator.calculate(model.getElapsedDuration(), model.getPegsLeft(), model.getMovesCount());
        long durationSeconds = model.getElapsedDuration().getSeconds();
        LeaderboardManager.Entry entry = new LeaderboardManager.Entry(
                playerName,
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
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

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

        Layout layout = buildLayout(panelRect);
        drawHeader(g2d, layout);
        drawOverlayContent(g2d, layout);
    }

    private String getPanelAsset() {
        return state == OverlayState.GAME_OVER ? "ui_gameover_panel_640x420.png" : "ui_menu_panel_640x420.png";
    }

    private Layout buildLayout(Rectangle panelRect) {
        int scale = controller.getScale();
        int panelPadding = (int) Math.round(PANEL_PADDING_PX * scale);
        int headerHeight = (int) Math.round(HEADER_HEIGHT_PX * scale);

        int contentX = panelRect.x + panelPadding;
        int contentWidth = panelRect.width - panelPadding * 2;
        int contentTop = panelRect.y + panelPadding + headerHeight;
        int contentBottom = panelRect.y + panelRect.height - panelPadding;
        int contentHeight = Math.max(0, contentBottom - contentTop);
        Rectangle contentRect = new Rectangle(contentX, contentTop, contentWidth, contentHeight);

        Rectangle headerRect = new Rectangle(contentX, panelRect.y + panelPadding, contentWidth, headerHeight);
        return new Layout(panelRect, headerRect, contentRect);
    }

    private void drawHeader(Graphics2D g2d, Layout layout) {
        BufferedImage banner = assets.getImage("ui_banner_520x120.png");
        g2d.drawImage(
                banner,
                layout.headerRect.x,
                layout.headerRect.y,
                layout.headerRect.width,
                layout.headerRect.height,
                null);

        g2d.setColor(COLOR_TEXT);
        g2d.setFont(UIFonts.h1(controller.getScale()));
        drawCenteredText(g2d, getOverlayTitle(), layout.headerRect);
    }

    private void drawOverlayContent(Graphics2D g2d, Layout layout) {
        if (state == OverlayState.PRE_GAME_NAME) {
            drawPreGame(g2d, layout);
        } else if (state == OverlayState.MENU) {
            drawMenu(g2d, layout);
        } else if (state == OverlayState.GAME_OVER) {
            drawGameOver(g2d, layout);
        }
    }

    private String getOverlayTitle() {
        if (state == OverlayState.MENU) {
            return "MENU";
        }
        if (state == OverlayState.PRE_GAME_NAME) {
            return "PLAYER NAME";
        }
        if (state == OverlayState.GAME_OVER) {
            return "GAME OVER";
        }
        return "";
    }

    private void drawPreGame(Graphics2D g2d, Layout layout) {
        int buttonY = layout.contentRect.y + layout.contentRect.height - (int) Math.round(BUTTON_HEIGHT_PX * controller.getScale());
        int inputImageHeight = (int) Math.round(104 * controller.getScale());
        int gap = (int) Math.round(INPUT_TO_BUTTON_GAP_PX * controller.getScale());
        int inputY = layout.contentRect.y + Math.max(0, (buttonY - gap - inputImageHeight - layout.contentRect.y) / 2);

        drawNameInput(g2d, layout.panelRect, inputY);
        drawButtonRow(g2d, layout.panelRect, buttonY, OverlayButton.START, OverlayButton.QUIT);
    }

    private void drawMenu(Graphics2D g2d, Layout layout) {
        drawCenteredButtonColumn(g2d, layout.contentRect, new OverlayButton[]{
                OverlayButton.RESUME,
                OverlayButton.NEW_GAME,
                OverlayButton.QUIT
        });
    }

    private void drawGameOver(Graphics2D g2d, Layout layout) {
        int buttonY = layout.contentRect.y + layout.contentRect.height - (int) Math.round(BUTTON_HEIGHT_PX * controller.getScale());

        g2d.setFont(UIFonts.body(controller.getScale()));
        g2d.setColor(COLOR_TEXT_MUTED);
        int score = ScoreCalculator.calculate(model.getElapsedDuration(), model.getPegsLeft(), model.getMovesCount());
        String[] lines = new String[]{
                "Moves: " + model.getMovesCount(),
                "Pegs: " + model.getPegsLeft(),
                "Score: " + score
        };
        int lineSpacing = g2d.getFontMetrics().getHeight() + (int) Math.round(8 * controller.getScale());
        int statsHeight = lineSpacing * (lines.length - 1) + g2d.getFontMetrics().getAscent();
        int availableTop = layout.contentRect.y;
        int availableBottom = buttonY - (int) Math.round(28 * controller.getScale());
        int blockCenterY = availableTop + Math.max(0, (availableBottom - availableTop) / 2);
        int firstBaselineY = blockCenterY - statsHeight / 2 + g2d.getFontMetrics().getAscent();
        for (int i = 0; i < lines.length; i++) {
            int baselineY = firstBaselineY + i * lineSpacing;
            drawCenteredTextAtBaseline(g2d, lines[i], layout.contentRect.x + layout.contentRect.width / 2, baselineY);
        }

        drawButtonRow(g2d, layout.panelRect, buttonY, OverlayButton.RESTART, OverlayButton.MENU);
    }

    private void drawNameInput(Graphics2D g2d, Rectangle panelRect, int inputY) {
        BufferedImage inputImage = assets.getImage(nameInputFocused ? "ui_name_input_focus_560x104.png" : "ui_name_input_560x104.png");
        int inputWidth = (int) Math.round(inputImage.getWidth() * controller.getScale());
        int inputHeight = (int) Math.round(inputImage.getHeight() * controller.getScale());
        int inputX = panelRect.x + (panelRect.width - inputWidth) / 2;
        nameInputRect = new Rectangle(inputX, inputY, inputWidth, inputHeight);
        g2d.drawImage(inputImage, inputX, inputY, inputWidth, inputHeight, null);

        String text = nameInput == null ? "" : nameInput;
        g2d.setFont(UIFonts.h2(controller.getScale()));
        g2d.setColor(COLOR_TEXT_INPUT);
        java.awt.FontMetrics fm = g2d.getFontMetrics();
        int textX = inputX + (int) Math.round(24 * controller.getScale());
        int topInset = (int) Math.round(INPUT_TEXT_TOP_INSET_PX * controller.getScale());
        int bottomInset = (int) Math.round(INPUT_TEXT_BOTTOM_INSET_PX * controller.getScale());
        int innerY = inputY + topInset;
        int innerH = inputHeight - topInset - bottomInset;
        int baselineY = innerY + (innerH - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(text, textX, baselineY);

        g2d.setFont(UIFonts.small(controller.getScale()));
        g2d.setColor(COLOR_TEXT_DARK);
        int tabX = inputX + (int) Math.round(20 * controller.getScale());
        int tabY = inputY + (int) Math.round(20 * controller.getScale());
        g2d.drawString("NAME", tabX, tabY);

        g2d.setFont(UIFonts.h2(controller.getScale()));
        g2d.setColor(COLOR_TEXT_INPUT);
        fm = g2d.getFontMetrics();

        if (nameInputFocused && cursorOn) {
            int textWidth = fm.stringWidth(text);
            int cursorX = textX + textWidth + (int) Math.round(4 * controller.getScale());
            int cursorTop = baselineY - fm.getAscent();
            int cursorBottom = baselineY + fm.getDescent();
            int cursorHeight = cursorBottom - cursorTop;
            g2d.fillRect(cursorX, cursorTop, (int) Math.round(2 * controller.getScale()), cursorHeight);
        }
    }

    private void drawButtonRow(Graphics2D g2d, Rectangle panelRect, int y, OverlayButton... buttons) {
        int buttonWidth = (int) Math.round(ROW_BUTTON_WIDTH_PX * controller.getScale());
        int buttonHeight = (int) Math.round(BUTTON_HEIGHT_PX * controller.getScale());
        int gap = (int) Math.round(BUTTON_GAP_PX * controller.getScale());
        int totalWidth = buttonWidth * buttons.length + gap * (buttons.length - 1);
        int startX = panelRect.x + (panelRect.width - totalWidth) / 2;
        for (int i = 0; i < buttons.length; i++) {
            int x = startX + i * (buttonWidth + gap);
            Rectangle rect = new Rectangle(x, y, buttonWidth, buttonHeight);
            buttonRects.put(buttons[i], rect);
            drawButton(g2d, rect, buttons[i]);
        }
    }

    private void drawCenteredButtonColumn(Graphics2D g2d, Rectangle contentRect, OverlayButton[] buttons) {
        int buttonWidth = (int) Math.round(MENU_BUTTON_WIDTH_PX * controller.getScale());
        int buttonHeight = (int) Math.round(BUTTON_HEIGHT_PX * controller.getScale());
        int gap = (int) Math.round(BUTTON_GAP_PX * controller.getScale());
        int totalHeight = buttonHeight * buttons.length + gap * (buttons.length - 1);
        int startY = contentRect.y + (contentRect.height - totalHeight) / 2;
        int x = contentRect.x + (contentRect.width - buttonWidth) / 2;
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
            image = assets.getImage(getPressedAsset(button));
        } else if (hovered == button) {
            image = assets.getImage(getHoverAsset(button));
        } else {
            image = assets.getImage(getNormalAsset(button));
        }
        g2d.drawImage(image, rect.x, rect.y, rect.width, rect.height, null);

        g2d.setFont(UIFonts.h2(controller.getScale()));
        g2d.setColor(new Color(245, 241, 235));
        String label = button.getLabel();
        int textWidth = g2d.getFontMetrics().stringWidth(label);
        int textX = rect.x + (rect.width - textWidth) / 2;
        int textY = centeredTextBaseline(g2d, rect.y, rect.height);
        g2d.drawString(label, textX, textY);
    }

    private int centeredTextBaseline(Graphics2D g2d, int rectY, int rectHeight) {
        java.awt.FontMetrics metrics = g2d.getFontMetrics();
        return rectY + (rectHeight - metrics.getHeight()) / 2 + metrics.getAscent();
    }

    private void drawCenteredText(Graphics2D g2d, String text, Rectangle rect) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int textX = rect.x + (rect.width - textWidth) / 2;
        int textY = centeredTextBaseline(g2d, rect.y, rect.height);
        g2d.drawString(text, textX, textY);
    }

    private void drawCenteredTextAtBaseline(Graphics2D g2d, String text, int centerX, int baselineY) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int textX = centerX - textWidth / 2;
        g2d.drawString(text, textX, baselineY);
    }

    private String getNormalAsset(OverlayButton button) {
        return "ui_button_normal_260x72.png";
    }

    private String getHoverAsset(OverlayButton button) {
        return "ui_button_hover_260x72.png";
    }

    private String getPressedAsset(OverlayButton button) {
        return "ui_button_pressed_260x72.png";
    }

    enum OverlayButton {
        START("START"),
        QUIT("QUIT"),
        RESUME("RESUME"),
        NEW_GAME("NEW GAME"),
        MENU("MENU"),
        RESTART("RESTART");

        private final String label;

        OverlayButton(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static class Layout {
        private final Rectangle panelRect;
        private final Rectangle headerRect;
        private final Rectangle contentRect;

        private Layout(Rectangle panelRect, Rectangle headerRect, Rectangle contentRect) {
            this.panelRect = panelRect;
            this.headerRect = headerRect;
            this.contentRect = contentRect;
        }
    }
}
