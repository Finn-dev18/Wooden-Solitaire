import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class OverlayPanel extends JPanel {
    private static final Color COLOR_TEXT = new Color(245, 241, 235);
    private static final Color COLOR_TEXT_MUTED = new Color(224, 230, 255);
    private static final Color COLOR_TEXT_DARK = new Color(245, 241, 235);
    private static final Color COLOR_TEXT_INPUT = new Color(245, 241, 235);
    private static final Color COLOR_CYAN = new Color(43, 253, 223);

    private static final int MAX_NAME_LENGTH = 12;
    private static final int PANEL_PADDING_PX = 48;
    private static final int HEADER_HEIGHT_PX = 96;
    private static final int MENU_BUTTON_WIDTH_PX = 360;
    private static final int ROW_BUTTON_WIDTH_PX = 260;
    private static final int BUTTON_HEIGHT_PX = 72;
    private static final int BUTTON_GAP_PX = 24;
    private static final int INPUT_TO_BUTTON_GAP_PX = 24;

    private final AssetManager assets;
    private final GameModel model;
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

    public OverlayPanel(AssetManager assets, GameModel model, WoodenSolitaireGUI controller) {
        this.assets = assets;
        this.model = model;
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
                if (pressed != null && pressed == released && !isButtonDisabled(pressed)) {
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
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && !nameInput.isEmpty()) {
                    nameInput = nameInput.substring(0, nameInput.length() - 1);
                    repaint();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (!isNameEntryActive() || !nameInputFocused) {
                    return;
                }
                char ch = e.getKeyChar();
                if (Character.isISOControl(ch) || nameInput.length() >= MAX_NAME_LENGTH) {
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
            case CLASSIC_MODE:
                controller.setMode(GameMode.CLASSIC);
                controller.setOverlayState(OverlayState.PRE_GAME_NAME);
                break;
            case POWERUPS_MODE:
                controller.setMode(GameMode.POWERUPS);
                controller.setOverlayState(OverlayState.PRE_GAME_NAME);
                break;
            case QUIT:
                System.exit(0);
                break;
            case RESUME:
                controller.setOverlayState(OverlayState.NONE);
                break;
            case NEW_GAME:
            case RESTART:
                model.resetGame();
                controller.setOverlayState(OverlayState.MODE_SELECT);
                break;
            case MENU:
                controller.setOverlayState(OverlayState.MENU);
                break;
            case CLOSE:
                controller.setOverlayState(OverlayState.NONE);
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
                model.isPowerupsEnabled());
        controller.getActiveLeaderboard().addEntry(entry);
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
                model.isPowerupsEnabled());
        return controller.getActiveLeaderboard().isTop10Candidate(entry);
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
        if (state == OverlayState.INFO_SHEET) {
            drawInfoSheet(g2d);
        } else {
            drawPanelAndContents(g2d);
        }

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
        int panelWidth = panelImage.getWidth() * controller.getScale();
        int panelHeight = panelImage.getHeight() * controller.getScale();
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
        int panelPadding = PANEL_PADDING_PX * scale;
        int headerHeight = HEADER_HEIGHT_PX * scale;

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
        if (state == OverlayState.MODE_SELECT) {
            drawModeSelect(g2d, layout);
        } else if (state == OverlayState.PRE_GAME_NAME) {
            drawPreGame(g2d, layout);
        } else if (state == OverlayState.MENU) {
            drawMenu(g2d, layout);
        } else if (state == OverlayState.GAME_OVER) {
            drawGameOver(g2d, layout);
        }
    }

    private String getOverlayTitle() {
        if (state == OverlayState.MODE_SELECT) {
            return "SELECT MODE";
        }
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

    private void drawModeSelect(Graphics2D g2d, Layout layout) {
        int buttonHeight = BUTTON_HEIGHT_PX * controller.getScale();
        int gap = BUTTON_GAP_PX * controller.getScale();
        int totalHeight = (buttonHeight * 2) + gap;
        int rowStartY = layout.contentRect.y + Math.max(0, (layout.contentRect.height - totalHeight) / 2);
        drawButtonRow(g2d, layout.panelRect, rowStartY, OverlayButton.CLASSIC_MODE, OverlayButton.POWERUPS_MODE);
        for (OverlayButton button : new OverlayButton[]{OverlayButton.CLASSIC_MODE, OverlayButton.POWERUPS_MODE}) {
            Rectangle rect = buttonRects.get(button);
            if (rect != null && button.getSubLabel() != null) {
                g2d.setFont(UIFonts.small(controller.getScale()));
                g2d.setColor(COLOR_TEXT_MUTED);
                drawCenteredTextAtBaseline(g2d, button.getSubLabel(), rect.x + rect.width / 2, rect.y + rect.height - (10 * controller.getScale()));
            }
        }
    }

    private void drawPreGame(Graphics2D g2d, Layout layout) {
        int scale = controller.getScale();
        int buttonWidth = MENU_BUTTON_WIDTH_PX * scale;
        int buttonHeight = BUTTON_HEIGHT_PX * scale;
        int inputWidth = 560 * scale;
        int inputHeight = 104 * scale;

        int totalHeight = inputHeight + INPUT_TO_BUTTON_GAP_PX * scale + buttonHeight;
        int startY = layout.contentRect.y + (layout.contentRect.height - totalHeight) / 2;
        int inputX = layout.panelRect.x + (layout.panelRect.width - inputWidth) / 2;
        int inputY = startY;

        nameInputRect = new Rectangle(inputX, inputY, inputWidth, inputHeight);
        drawNameInput(g2d, nameInputRect);

        int buttonX = layout.panelRect.x + (layout.panelRect.width - buttonWidth) / 2;
        int buttonY = inputY + inputHeight + INPUT_TO_BUTTON_GAP_PX * scale;
        Rectangle startRect = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
        buttonRects.put(OverlayButton.START, startRect);
        drawButton(g2d, startRect, OverlayButton.START);
    }

    private void drawMenu(Graphics2D g2d, Layout layout) {
        drawCenteredButtonColumn(g2d, layout.contentRect, new OverlayButton[]{OverlayButton.RESUME, OverlayButton.NEW_GAME, OverlayButton.QUIT});
    }

    private void drawGameOver(Graphics2D g2d, Layout layout) {
        int scale = controller.getScale();
        g2d.setColor(COLOR_TEXT_MUTED);
        g2d.setFont(UIFonts.body(scale));

        String[] lines = {
                "Player: " + model.getPlayerName(),
                "Pegs Left: " + model.getPegsLeft(),
                "Moves: " + model.getMovesCount(),
                "Time: " + model.getElapsedDuration().toSeconds() + "s"
        };

        int lineSpacing = g2d.getFontMetrics().getHeight() + (8 * scale);
        int statsHeight = lineSpacing * (lines.length - 1) + g2d.getFontMetrics().getAscent();
        int blockCenterY = layout.contentRect.y + (layout.contentRect.height / 2) - (40 * scale);
        int firstBaselineY = blockCenterY - statsHeight / 2 + g2d.getFontMetrics().getAscent();

        for (int i = 0; i < lines.length; i++) {
            drawCenteredTextAtBaseline(g2d, lines[i], layout.panelRect.x + layout.panelRect.width / 2, firstBaselineY + i * lineSpacing);
        }

        int buttonY = layout.panelRect.y + layout.panelRect.height - (BUTTON_HEIGHT_PX * scale) - (PANEL_PADDING_PX * scale);
        drawButtonRow(g2d, layout.panelRect, buttonY, OverlayButton.RESTART, OverlayButton.MENU);
    }

    private void drawNameInput(Graphics2D g2d, Rectangle rect) {
        BufferedImage inputImage = assets.getImage(nameInputFocused ? "ui_name_input_focus_560x104.png" : "ui_name_input_560x104.png");
        g2d.drawImage(inputImage, rect.x, rect.y, rect.width, rect.height, null);

        String text = nameInput == null ? "" : nameInput;
        String visibleText = text;
        if (isNameEntryActive() && nameInputFocused && cursorOn) {
            visibleText += "_";
        }
        g2d.setFont(UIFonts.h2(controller.getScale()));
        g2d.setColor(COLOR_TEXT_INPUT);
        FontMetrics fm = g2d.getFontMetrics();
        int scale = controller.getScale();
        int textX = rect.x + (24 * scale);
        int padY = (16 * scale);
        int innerY = rect.y + padY;
        int innerH = rect.height - padY * 2;
        int baselineY = innerY + (innerH - fm.getHeight()) / 2 + fm.getAscent();
        baselineY += (6 * scale);
        g2d.drawString(visibleText, textX, baselineY);

        Rectangle tabRect = new Rectangle(
                rect.x + (18 * scale),
                rect.y + (6 * scale),
                104 * scale,
                30 * scale);

        g2d.setFont(UIFonts.small(scale));
        g2d.setColor(COLOR_TEXT_DARK);
        drawCenteredText(g2d, "NAME", tabRect);
    }

    private void drawInfoSheet(Graphics2D g2d) {
        buttonRects.clear();
        nameInputRect = null;

        int scale = controller.getScale();
        int panelW = 1240 * scale;
        int panelH = 760 * scale;
        int panelX = (getWidth() - panelW) / 2;
        int panelY = (getHeight() - panelH) / 2;
        Rectangle panelRect = new Rectangle(panelX, panelY, panelW, panelH);

        g2d.drawImage(assets.getImage("ui_info_panel_1240x760.png"), panelX, panelY, panelW, panelH, null);

        Font h1 = UIFonts.h1(scale).deriveFont(Font.BOLD, 28f * scale);
        g2d.setFont(h1);
        g2d.setColor(COLOR_TEXT);
        Rectangle headerRect = new Rectangle(panelX + (260 * scale), panelY + (42 * scale), panelW - (520 * scale), 96 * scale);
        drawCenteredText(g2d, "INFO", headerRect);

        int iconSize = 32 * scale;
        g2d.drawImage(assets.getImage("icon_question_32.png"), panelX + (90 * scale), panelY + (78 * scale), iconSize, iconSize, null);

        int innerTop = panelY + (190 * scale);
        int leftX = panelX + (90 * scale);
        int rightX = panelX + (590 * scale);
        int boxW = 560 * scale;
        int boxH = 520 * scale;

        Rectangle leftBox = new Rectangle(leftX, innerTop, boxW, boxH);
        Rectangle rightBox = new Rectangle(rightX, innerTop, boxW, boxH);
        g2d.drawImage(assets.getImage("ui_info_box_560x520.png"), leftBox.x, leftBox.y, leftBox.width, leftBox.height, null);
        g2d.drawImage(assets.getImage("ui_info_box_560x520.png"), rightBox.x, rightBox.y, rightBox.width, rightBox.height, null);

        drawInfoLeftContent(g2d, leftBox, scale);
        drawInfoRightContent(g2d, rightBox, scale);

        int buttonW = 360 * scale;
        int buttonH = 88 * scale;
        int buttonX = panelX + (panelW - buttonW) / 2;
        int buttonY = panelY + panelH - (120 * scale);
        Rectangle closeRect = new Rectangle(buttonX, buttonY, buttonW, buttonH);
        buttonRects.put(OverlayButton.CLOSE, closeRect);
        drawButton(g2d, closeRect, OverlayButton.CLOSE);
    }

    private void drawInfoLeftContent(Graphics2D g2d, Rectangle box, int scale) {
        int padding = 22 * scale;
        int x = box.x + padding;
        int maxWidth = box.width - (padding * 2);
        int y = box.y + padding;

        Font sectionFont = UIFonts.h2(scale).deriveFont(Font.BOLD, 20f * scale);
        Font bodyFont = UIFonts.body(scale).deriveFont(Font.PLAIN, 14f * scale);

        y = drawSection(g2d, x, y, maxWidth, sectionFont, bodyFont, "A) SPIELREGELN", Arrays.asList(
                "• Ziel: Am Ende so wenige Kugeln wie möglich (ideal: 1).",
                "• Standardzug: Springe über genau 1 Kugel in ein leeres Feld.",
                "• Die übersprungene Kugel wird entfernt.",
                "• Züge sind nur horizontal/vertikal gültig."), scale);

        y = drawSection(g2d, x, y + (8 * scale), maxWidth, sectionFont, bodyFont, "B) CREDITS & SHOP", Arrays.asList(
                "• Credits gibt es für gültige Standardzüge.",
                "• Powerups kauft man mit Credits (Runden-Limits beachten)."), scale);

        drawSection(g2d, x, y + (8 * scale), maxWidth, sectionFont, bodyFont, "D) STEUERUNG", Arrays.asList(
                "• Maus: Felder anklicken (Start → Ziel).",
                "• Hotkeys 1–5: gekauftes Powerup benutzen.",
                "• ? unten links: Info öffnen.",
                "• ESC: Overlay schließen."), scale);
    }

    private void drawInfoRightContent(Graphics2D g2d, Rectangle box, int scale) {
        int padding = 22 * scale;
        int x = box.x + padding;
        int maxWidth = box.width - (padding * 2);
        int y = box.y + padding;

        Font sectionFont = UIFonts.h2(scale).deriveFont(Font.BOLD, 20f * scale);
        Font bodyFont = UIFonts.body(scale).deriveFont(Font.PLAIN, 14f * scale);
        Font powerupNameFont = UIFonts.h2(scale).deriveFont(Font.BOLD, 18f * scale);

        y = drawSection(g2d, x, y, maxWidth, sectionFont, bodyFont, "B) CREDITS & SHOP", Arrays.asList(
                "• Credits sind gespeichert (zwischen Runden)."), scale);

        g2d.setFont(sectionFont);
        g2d.setColor(COLOR_TEXT);
        int titleBaseline = y + g2d.getFontMetrics().getAscent() + (10 * scale);
        g2d.drawString("C) POWERUPS", x, titleBaseline);
        y = titleBaseline + (14 * scale);

        PowerupRow[] rows = {
                new PowerupRow("icon_undo_64.png", "UNDO", "macht den letzten Schritt rückgängig."),
                new PowerupRow("icon_move_64.png", "MOVE", "versetzt eine Kugel auf ein leeres Feld."),
                new PowerupRow("icon_bomb_64.png", "BOMB", "entfernt eine Kugel."),
                new PowerupRow("icon_bridge_64.png", "BRIDGE", "Sprung Distanz 3, entfernt 2 Zwischenkugeln."),
                new PowerupRow("icon_randsturm_64.png", "RANDSTURM", "schiebt alle Kugeln zufällig zu einer Wand.")
        };

        int rowHeight = 64 * scale;
        int iconSize = 40 * scale;
        for (PowerupRow row : rows) {
            int rowTop = y;
            int iconX = x;
            int iconY = rowTop + (rowHeight - iconSize) / 2;
            g2d.drawImage(assets.getImage(row.icon), iconX, iconY, iconSize, iconSize, null);

            int textX = x + iconSize + (14 * scale);
            g2d.setFont(powerupNameFont);
            g2d.setColor(COLOR_CYAN);
            int nameBaseline = rowTop + g2d.getFontMetrics().getAscent() + (4 * scale);
            g2d.drawString(row.name + ":", textX, nameBaseline);

            g2d.setFont(bodyFont);
            g2d.setColor(COLOR_TEXT_MUTED);
            int descMaxWidth = maxWidth - (iconSize + (14 * scale));
            String desc = fitWithEllipsis(row.description, g2d.getFontMetrics(), descMaxWidth);
            int descBaseline = nameBaseline + g2d.getFontMetrics().getHeight() - (2 * scale);
            g2d.drawString(desc, textX, descBaseline);

            y += rowHeight;
        }
    }

    private int drawSection(Graphics2D g2d, int x, int y, int maxWidth, Font sectionFont, Font bodyFont, String title,
                            List<String> lines, int scale) {
        g2d.setFont(sectionFont);
        g2d.setColor(COLOR_TEXT);
        int sectionBaseline = y + g2d.getFontMetrics().getAscent();
        g2d.drawString(title, x, sectionBaseline);

        y = sectionBaseline + (8 * scale);
        g2d.setFont(bodyFont);
        g2d.setColor(COLOR_TEXT_MUTED);
        int lineHeight = g2d.getFontMetrics().getHeight() + (2 * scale);
        for (String line : lines) {
            for (String wrapped : wrapText(line, g2d.getFontMetrics(), maxWidth)) {
                y += lineHeight;
                g2d.drawString(wrapped, x, y);
            }
        }
        return y + (8 * scale);
    }

    private List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (line.length() > 0) {
                    lines.add(line.toString());
                }
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private void drawButtonRow(Graphics2D g2d, Rectangle panelRect, int y, OverlayButton... buttons) {
        int buttonWidth = ROW_BUTTON_WIDTH_PX * controller.getScale();
        int buttonHeight = BUTTON_HEIGHT_PX * controller.getScale();
        int gap = BUTTON_GAP_PX * controller.getScale();
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
        int buttonWidth = MENU_BUTTON_WIDTH_PX * controller.getScale();
        int buttonHeight = BUTTON_HEIGHT_PX * controller.getScale();
        int gap = BUTTON_GAP_PX * controller.getScale();
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
        boolean disabled = isButtonDisabled(button);
        BufferedImage image;

        if (state == OverlayState.INFO_SHEET && button == OverlayButton.CLOSE) {
            if (pressed == button) {
                image = assets.getImage("ui_info_close_button_pressed_360x88.png");
            } else if (hovered == button) {
                image = assets.getImage("ui_info_close_button_hover_360x88.png");
            } else {
                image = assets.getImage("ui_info_close_button_normal_360x88.png");
            }
        } else if (disabled) {
            image = assets.getImage("ui_button_disabled_260x72.png");
        } else if (pressed == button) {
            image = assets.getImage("ui_button_pressed_260x72.png");
        } else if (hovered == button) {
            image = assets.getImage("ui_button_hover_260x72.png");
        } else {
            image = assets.getImage("ui_button_normal_260x72.png");
        }

        g2d.drawImage(image, rect.x, rect.y, rect.width, rect.height, null);

        g2d.setFont(state == OverlayState.INFO_SHEET ? UIFonts.h2(controller.getScale()).deriveFont(Font.BOLD, 20f * controller.getScale()) : UIFonts.h2(controller.getScale()));
        g2d.setColor(COLOR_TEXT);
        String label = button.getLabel();
        int textWidth = g2d.getFontMetrics().stringWidth(label);
        int textX = rect.x + (rect.width - textWidth) / 2;
        int textY = centeredTextBaseline(g2d, rect.y, rect.height) - (button.hasSubLabel() ? (9 * controller.getScale()) : 0);
        g2d.drawString(label, textX, textY);
    }

    private boolean isButtonDisabled(OverlayButton button) {
        return button == OverlayButton.START && !canStart();
    }

    private String fitWithEllipsis(String text, FontMetrics fm, int maxWidth) {
        if (text == null || text.isEmpty() || fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            String candidate = text.substring(0, mid) + ellipsis;
            if (fm.stringWidth(candidate) <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(0, Math.max(0, low)) + ellipsis;
    }

    private int centeredTextBaseline(Graphics2D g2d, int rectY, int rectHeight) {
        FontMetrics metrics = g2d.getFontMetrics();
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

    enum OverlayButton {
        CLASSIC_MODE("CLASSIC", "No Powerups"),
        POWERUPS_MODE("POWERUPS", "Powerups Enabled"),
        START("START", null),
        QUIT("QUIT", null),
        RESUME("RESUME", null),
        NEW_GAME("NEW GAME", null),
        MENU("MENU", null),
        RESTART("RESTART", null),
        CLOSE("CLOSE", null);

        private final String label;
        private final String subLabel;

        OverlayButton(String label, String subLabel) {
            this.label = label;
            this.subLabel = subLabel;
        }

        public String getLabel() {
            return label;
        }

        public String getSubLabel() {
            return subLabel;
        }

        public boolean hasSubLabel() {
            return subLabel != null;
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

    private static class PowerupRow {
        private final String icon;
        private final String name;
        private final String description;

        private PowerupRow(String icon, String name, String description) {
            this.icon = icon;
            this.name = name;
            this.description = description;
        }
    }
}
