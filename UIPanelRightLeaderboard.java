import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
    private static final int BASE_PADDING = 16;
    private static final int BASE_BUTTON_WIDTH = 260;
    private static final int BASE_BUTTON_HEIGHT = 72;
    private static final int BASE_BUTTON_GAP = 12;

    private final AssetManager assets;
    private final GameModel model;
    private final LeaderboardManager leaderboard;
    private final GameUIController controller;

    private final StatsPanel statsPanel;
    private final ButtonsPanel buttonsPanel;

    private int scale = 2;

    public UIPanelRightLeaderboard(AssetManager assets, GameModel model, LeaderboardManager leaderboard, GameUIController controller) {
        this.assets = assets;
        this.model = model;
        this.leaderboard = leaderboard;
        this.controller = controller;

        setOpaque(true);
        setBackground(COLOR_BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        statsPanel = new StatsPanel();
        buttonsPanel = new ButtonsPanel();

        add(statsPanel);
        add(Box.createVerticalGlue());
        add(buttonsPanel);
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
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
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

            int padding = (int) Math.round(BASE_PADDING * scale);
            float fontScale = (float) (scale / 2.0);
            int x = padding;
            int y = padding;

            g2d.setColor(COLOR_TEXT);
            g2d.setFont(getFont().deriveFont(Font.BOLD, 14f * fontScale));
            g2d.drawString("STATS", x, y + (int) Math.round(12 * scale));
            y += padding + (int) Math.round(12 * scale);

            g2d.setFont(getFont().deriveFont(Font.PLAIN, 9f * fontScale));
            g2d.setColor(COLOR_TEXT_MUTED);
            g2d.drawString("Züge: " + model.getMovesCount(), x, y);
            y += padding;
            g2d.drawString("Pegs: " + model.getPegsLeft(), x, y);
            y += padding;
            g2d.drawString("Zeit: " + formatDuration(model.getElapsedDuration()), x, y);
            y += padding;
            int score = ScoreCalculator.calculate(model.getElapsedDuration(), model.getPegsLeft(), model.getMovesCount());
            g2d.drawString("Punkte: " + score, x, y);
            y += padding;
            g2d.drawString("Spieler: " + model.getPlayerName(), x, y);
            y += padding;
            g2d.drawString("Credits: " + model.getCredits(), x, y);
            y += padding;

            g2d.setColor(COLOR_TEXT);
            g2d.setFont(getFont().deriveFont(Font.BOLD, 13f * fontScale));
            g2d.drawString("LEADERBOARD", x, y);
            y += padding;

            List<LeaderboardManager.Entry> entries = leaderboard.getTop10();
            g2d.setFont(getFont().deriveFont(Font.PLAIN, 9f * fontScale));
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardManager.Entry entry = entries.get(i);
                String line = (i + 1) + ". " + entry.name() + " - " + entry.score() + " P / " + formatDuration(entry.durationSeconds()) + " / " + entry.pegsLeft() + " Pegs";
                g2d.drawString(line, x, y);
                y += Math.max(8, padding / 2);
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
        g2d.setFont(getFont().deriveFont(Font.BOLD, (float) (12f * scale / 2f)));
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
}
