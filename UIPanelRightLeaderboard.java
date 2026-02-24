import javax.swing.JPanel;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

public class UIPanelRightLeaderboard extends JPanel {
    private static final Color COLOR_BG = new Color(21, 16, 68);
    private static final Color COLOR_TEXT = new Color(43, 253, 223);
    private static final Color COLOR_TEXT_MUTED = new Color(224, 230, 255);
    private static final Color COLOR_BOX_FILL = new Color(14, 22, 62);
    private static final Color COLOR_BOX_BORDER = new Color(43, 253, 223);
    private static final Color COLOR_BOX_SHADOW = new Color(0, 0, 0, 110);
    private static final int BASE_PADDING = 16;
    private static final int BASE_PANEL_PADDING = 24;
    private static final int BASE_BOX_PADDING = 18;
    private static final int BASE_SECTION_GAP = 18;
    private static final int BASE_STATS_BOX_HEIGHT = 220;
    private static final int BASE_BUTTON_WIDTH = 260;
    private static final int BASE_BUTTON_HEIGHT = 72;
    private static final int BASE_BUTTON_GAP = 12;
    private static final int BASE_LEADERBOARD_MIN_HEIGHT = 220;

    private final AssetManager assets;
    private final GameModel model;
    private final LeaderboardManager classicLeaderboard;
    private final LeaderboardManager powerupsLeaderboard;
    private final GameUIController controller;

    private final StatsPanel statsPanel;
    private final ButtonsPanel buttonsPanel;

    private int scale = 2;

    public UIPanelRightLeaderboard(AssetManager assets, GameModel model, LeaderboardManager classicLeaderboard, LeaderboardManager powerupsLeaderboard, GameUIController controller) {
        this.assets = assets;
        this.model = model;
        this.classicLeaderboard = classicLeaderboard;
        this.powerupsLeaderboard = powerupsLeaderboard;
        this.controller = controller;

        setOpaque(true);
        setBackground(COLOR_BG);
        setLayout(new BorderLayout());

        statsPanel = new StatsPanel();
        buttonsPanel = new ButtonsPanel();

        add(statsPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    public void setScale(int scale) {
        this.scale = Math.max(1, scale);
        int padding = (int) Math.round(BASE_PADDING * this.scale);
        int buttonHeight = (int) Math.round(BASE_BUTTON_HEIGHT * this.scale);
        int buttonGap = (int) Math.round(BASE_BUTTON_GAP * this.scale);
        int buttonsHeight = (padding * 2) + (buttonHeight * 2) + buttonGap;

        statsPanel.setPreferredSize(new Dimension(1, Math.max(220, padding * 14)));
        statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        buttonsPanel.setPreferredSize(new Dimension(1, buttonsHeight));
        buttonsPanel.setMinimumSize(new Dimension(0, buttonsHeight));
        buttonsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonsHeight));

        revalidate();
        repaint();
    }

    private void applyPixelArtHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private class StatsPanel extends JPanel {
        StatsPanel() {
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            applyPixelArtHints(g2d);

            int panelPadding = (int) Math.round(BASE_PANEL_PADDING * scale);
            int boxPadding = (int) Math.round(BASE_BOX_PADDING * scale);
            int gap = (int) Math.round(BASE_SECTION_GAP * scale);
            int statsBoxHeight = (int) Math.round(BASE_STATS_BOX_HEIGHT * scale);
            int leaderboardMinHeight = (int) Math.round(BASE_LEADERBOARD_MIN_HEIGHT * scale);

            int contentWidth = Math.max(0, getWidth() - (panelPadding * 2));
            int statsBoxY = panelPadding;
            int statsBoxW = contentWidth;
            int statsBoxH = Math.min(statsBoxHeight, Math.max(0, getHeight() - (panelPadding * 2)));

            int leaderboardBoxY = statsBoxY + statsBoxH + gap;
            int leaderboardBoxH = getHeight() - panelPadding - leaderboardBoxY;
            leaderboardBoxH = Math.max(leaderboardMinHeight, leaderboardBoxH);
            if (leaderboardBoxY + leaderboardBoxH > getHeight() - panelPadding) {
                leaderboardBoxH = Math.max(0, getHeight() - panelPadding - leaderboardBoxY);
            }

            drawEmbeddedBox(g2d, panelPadding, statsBoxY, statsBoxW, statsBoxH);
            drawEmbeddedBox(g2d, panelPadding, leaderboardBoxY, statsBoxW, leaderboardBoxH);

            int statsContentX = panelPadding + boxPadding;
            int statsContentW = Math.max(0, statsBoxW - (2 * boxPadding));
            int y = statsBoxY + boxPadding;

            g2d.setColor(COLOR_TEXT);
            g2d.setFont(UIFonts.h1(scale));
            FontMetrics h1Metrics = g2d.getFontMetrics();
            int lineHeight = h1Metrics.getHeight() + 4;
            y += h1Metrics.getAscent();
            g2d.drawString("STATS", statsContentX, y);
            y += lineHeight;

            g2d.setFont(UIFonts.body(scale));
            FontMetrics bodyMetrics = g2d.getFontMetrics();
            lineHeight = bodyMetrics.getHeight() + 4;
            g2d.setColor(COLOR_TEXT_MUTED);
            g2d.drawString(ellipsis("Züge: " + model.getMovesCount(), bodyMetrics, statsContentW), statsContentX, y);
            y += lineHeight;
            g2d.drawString(ellipsis("Pegs: " + model.getPegsLeft(), bodyMetrics, statsContentW), statsContentX, y);
            y += lineHeight;
            g2d.drawString(ellipsis("Zeit: " + formatDuration(model.getElapsedDuration()), bodyMetrics, statsContentW), statsContentX, y);
            y += lineHeight;
            int score = ScoreCalculator.calculate(model.getElapsedDuration(), model.getPegsLeft(), model.getMovesCount());
            g2d.drawString(ellipsis("Punkte: " + score, bodyMetrics, statsContentW), statsContentX, y);
            y += lineHeight;
            g2d.drawString(ellipsis("Spieler: " + model.getPlayerName(), bodyMetrics, statsContentW), statsContentX, y);
            y += lineHeight;
            g2d.drawString(ellipsis("Credits: " + model.getCredits(), bodyMetrics, statsContentW), statsContentX, y);

            int leaderboardContentX = panelPadding + boxPadding;
            int leaderboardContentY = leaderboardBoxY + boxPadding;
            int leaderboardContentW = Math.max(0, statsBoxW - (2 * boxPadding));
            int leaderboardContentBottom = leaderboardBoxY + leaderboardBoxH - boxPadding;

            g2d.setColor(COLOR_TEXT);
            g2d.setFont(UIFonts.h1(scale));
            h1Metrics = g2d.getFontMetrics();
            lineHeight = h1Metrics.getHeight() + 4;
            int leaderboardY = leaderboardContentY + h1Metrics.getAscent();
            String leaderboardTitle = model.getMode() == GameMode.CLASSIC ? "LEADERBOARD (CLASSIC)" : "LEADERBOARD (POWERUPS)";
            g2d.drawString(ellipsis(leaderboardTitle, h1Metrics, leaderboardContentW), leaderboardContentX, leaderboardY);
            leaderboardY += lineHeight;

            LeaderboardManager activeLeaderboard = model.getMode() == GameMode.CLASSIC ? classicLeaderboard : powerupsLeaderboard;
            List<LeaderboardManager.Entry> entries = activeLeaderboard.getTop10();
            g2d.setFont(UIFonts.body(scale));
            bodyMetrics = g2d.getFontMetrics();
            lineHeight = bodyMetrics.getHeight() + 4;
            for (int i = 0; i < entries.size(); i++) {
                if (leaderboardY + bodyMetrics.getDescent() > leaderboardContentBottom) {
                    break;
                }
                LeaderboardManager.Entry entry = entries.get(i);
                String line = (i + 1) + ". " + entry.name() + " - " + entry.score() + " P / " + formatDuration(entry.durationSeconds()) + " / " + entry.pegsLeft() + " Pegs";
                g2d.drawString(ellipsis(line, bodyMetrics, leaderboardContentW), leaderboardContentX, leaderboardY);
                leaderboardY += lineHeight;
            }

            g2d.dispose();
        }
    }

    private class ButtonsPanel extends JPanel {
        private Rectangle menuButton;
        private Rectangle restartButton;
        private boolean menuHover;
        private boolean restartHover;
        private boolean menuPressed;
        private boolean restartPressed;

        ButtonsPanel() {
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (controller.isOverlayActive()) {
                        return;
                    }
                    if (menuButton != null && menuButton.contains(e.getX(), e.getY())) {
                        menuPressed = true;
                    }
                    if (restartButton != null && restartButton.contains(e.getX(), e.getY())) {
                        restartPressed = true;
                    }
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (controller.isOverlayActive()) {
                        menuPressed = false;
                        restartPressed = false;
                        repaint();
                        return;
                    }
                    if (menuPressed && menuButton != null && menuButton.contains(e.getX(), e.getY())) {
                        controller.requestMenu();
                    }
                    if (restartPressed && restartButton != null && restartButton.contains(e.getX(), e.getY())) {
                        controller.requestRestart();
                    }
                    menuPressed = false;
                    restartPressed = false;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    menuHover = false;
                    restartHover = false;
                    repaint();
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (controller.isOverlayActive()) {
                        menuHover = false;
                        restartHover = false;
                        repaint();
                        return;
                    }
                    menuHover = menuButton != null && menuButton.contains(e.getX(), e.getY());
                    restartHover = restartButton != null && restartButton.contains(e.getX(), e.getY());
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            applyPixelArtHints(g2d);

            int padding = (int) Math.round(BASE_PADDING * scale);
            int buttonWidth = (int) Math.round(BASE_BUTTON_WIDTH * scale);
            int buttonHeight = (int) Math.round(BASE_BUTTON_HEIGHT * scale);
            int buttonGap = (int) Math.round(BASE_BUTTON_GAP * scale);

            int buttonX = (getWidth() - buttonWidth) / 2;
            int buttonY = padding;
            menuButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
            restartButton = new Rectangle(buttonX, buttonY + buttonHeight + buttonGap, buttonWidth, buttonHeight);

            drawButton(g2d, menuButton, "MENU", menuHover, menuPressed);
            drawButton(g2d, restartButton, "RESTART", restartHover, restartPressed);

            g2d.dispose();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawButton(Graphics2D g2d, Rectangle rect, String text, boolean hover, boolean pressed) {
        BufferedImage image;
        if (pressed) {
            image = assets.getImage("menu_button_pressed_240x64.png");
        } else if (hover) {
            image = assets.getImage("menu_button_hover_240x64.png");
        } else {
            image = assets.getImage("menu_button_normal_240x64.png");
        }
        g2d.drawImage(image, rect.x, rect.y, rect.width, rect.height, null);
        g2d.setColor(new Color(245, 241, 235));
        g2d.setFont(UIFonts.body(scale));
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, rect.x + (rect.width - textWidth) / 2, rect.y + rect.height / 2 + (int) Math.round(6 * scale / 2f));
    }

    private String formatDuration(java.time.Duration duration) {
        long seconds = duration == null ? 0 : duration.getSeconds();
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    private String formatDuration(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    private void drawEmbeddedBox(Graphics2D g2d, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int arc = Math.max(10, (int) Math.round(12 * scale));
        int border = Math.max(2, (int) Math.round(2 * scale));

        g2d.setColor(COLOR_BOX_SHADOW);
        g2d.fillRoundRect(x + border, y + border, w, h, arc, arc);

        g2d.setColor(COLOR_BOX_FILL);
        g2d.fillRoundRect(x, y, w, h, arc, arc);

        g2d.setColor(COLOR_BOX_BORDER);
        for (int i = 0; i < border; i++) {
            g2d.drawRoundRect(x + i, y + i, w - 1 - (2 * i), h - 1 - (2 * i), arc, arc);
        }
    }

    private String ellipsis(String text, FontMetrics fm, int maxWidth) {
        if (text == null || fm == null || maxWidth <= 0) {
            return "";
        }
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        if (ellipsisWidth > maxWidth) {
            return "";
        }
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }
}
