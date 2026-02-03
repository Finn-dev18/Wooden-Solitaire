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
    private static final Color COLOR_TEXT = new Color(43, 253, 223);
    private static final Color COLOR_TEXT_MUTED = new Color(171, 25, 111);
    private static final int BASE_WIDTH = 260;
    private static final int BASE_PADDING = 16;
    private static final int BASE_BUTTON_WIDTH = 240;
    private static final int BASE_BUTTON_HEIGHT = 64;
    private static final int BASE_BUTTON_GAP = 8;

    private final AssetManager assets;
    private final GameModel model;
    private final LeaderboardManager leaderboard;
    private final GameUIController controller;
    private double scale = 2;
    private Rectangle menuButton;
    private Rectangle restartButton;
    private Rectangle nameButton;
    private boolean menuHover;
    private boolean restartHover;
    private boolean nameHover;
    private boolean menuPressed;
    private boolean restartPressed;
    private boolean namePressed;

    public UIPanelRightLeaderboard(AssetManager assets, GameModel model, LeaderboardManager leaderboard, GameUIController controller) {
        this.assets = assets;
        this.model = model;
        this.leaderboard = leaderboard;
        this.controller = controller;
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (nameButton != null && nameButton.contains(e.getX(), e.getY())) {
                    namePressed = true;
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
                if (namePressed && nameButton != null && nameButton.contains(e.getX(), e.getY())) {
                    controller.requestPlayerName();
                }
                if (menuPressed && menuButton != null && menuButton.contains(e.getX(), e.getY())) {
                    controller.requestMenu();
                }
                if (restartPressed && restartButton != null && restartButton.contains(e.getX(), e.getY())) {
                    controller.requestRestart();
                }
                menuPressed = false;
                restartPressed = false;
                namePressed = false;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                menuHover = false;
                restartHover = false;
                nameHover = false;
                repaint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                nameHover = nameButton != null && nameButton.contains(e.getX(), e.getY());
                menuHover = menuButton != null && menuButton.contains(e.getX(), e.getY());
                restartHover = restartButton != null && restartButton.contains(e.getX(), e.getY());
                repaint();
            }
        });
    }

    public void setScale(double scale) {
        this.scale = Math.max(1, scale);
        setPreferredSize(new Dimension((int) Math.round(BASE_WIDTH * this.scale), 1));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        drawPanelBackground(g2d);

        int padding = (int) Math.round(BASE_PADDING * scale);
        float fontScale = (float) (scale / 2.0);
        int x = padding;
        int y = padding;

        g2d.setColor(COLOR_TEXT);
        g2d.setFont(getFont().deriveFont(Font.BOLD, 16f * fontScale));
        g2d.drawString("STATS", x, y + (int) Math.round(12 * scale));
        y += padding + (int) Math.round(12 * scale);

        g2d.setFont(getFont().deriveFont(Font.PLAIN, 12f * fontScale));
        g2d.setColor(COLOR_TEXT_MUTED);
        g2d.drawString("Züge: " + model.getMovesCount(), x, y);
        y += padding;
        g2d.drawString("Pegs: " + model.getPegsLeft(), x, y);
        y += padding;
        g2d.drawString("Spieler: " + model.getPlayerName(), x, y);
        y += padding;
        g2d.drawString("Credits: " + model.getCredits(), x, y);
        y += padding;
        g2d.drawString("Modus: Pixelart", x, y);
        y += padding;

        g2d.setColor(COLOR_TEXT);
        g2d.setFont(getFont().deriveFont(Font.BOLD, 12f * fontScale));
        g2d.drawString("Status:", x, y);
        y += padding / 2;
        g2d.setFont(getFont().deriveFont(Font.PLAIN, 11f * fontScale));
        g2d.setColor(COLOR_TEXT_MUTED);
        g2d.drawString(model.getStatusMessage(), x, y);
        y += padding * 2;

        g2d.setColor(COLOR_TEXT);
        g2d.setFont(getFont().deriveFont(Font.BOLD, 16f * fontScale));
        g2d.drawString("LEADERBOARD", x, y);
        y += padding;

        List<LeaderboardManager.Entry> entries = leaderboard.getTop10();
        g2d.setFont(getFont().deriveFont(Font.PLAIN, 11f * fontScale));
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardManager.Entry entry = entries.get(i);
            String line = (i + 1) + ". " + entry.name() + " - " + entry.pegsLeft() + " / " + entry.moves();
            g2d.drawString(line, x, y);
            y += padding / 2;
        }

        int buttonWidth = (int) Math.round(BASE_BUTTON_WIDTH * scale);
        int buttonHeight = (int) Math.round(BASE_BUTTON_HEIGHT * scale);
        int buttonGap = (int) Math.round(BASE_BUTTON_GAP * scale);
        int buttonX = (getWidth() - buttonWidth) / 2;
        int buttonY = getHeight() - padding - (buttonHeight * 3) - buttonGap * 2;
        nameButton = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
        menuButton = new Rectangle(buttonX, buttonY + buttonHeight + buttonGap, buttonWidth, buttonHeight);
        restartButton = new Rectangle(buttonX, buttonY + (buttonHeight + buttonGap) * 2, buttonWidth, buttonHeight);

        drawButton(g2d, nameButton, "NAME", nameHover, namePressed);
        drawButton(g2d, menuButton, "MENU", menuHover, menuPressed);
        drawButton(g2d, restartButton, "RESTART", restartHover, restartPressed);

        g2d.dispose();
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
        g2d.setColor(COLOR_TEXT);
        g2d.setFont(getFont().deriveFont(Font.BOLD, (float) (14f * scale / 2f)));
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, rect.x + (rect.width - textWidth) / 2, rect.y + rect.height / 2 + (int) Math.round(5 * scale / 2f));
    }

    private void drawPanelBackground(Graphics2D g2d) {
        BufferedImage panel = assets.getImage("panel_9slice_24.png");
        int slice = panel.getWidth() / 3;
        int scaledSlice = (int) Math.round(slice * scale);
        int x = 0;
        int y = 0;
        int w = getWidth();
        int h = getHeight();
        g2d.drawImage(panel, x, y, x + scaledSlice, y + scaledSlice, 0, 0, slice, slice, null);
        g2d.drawImage(panel, x + scaledSlice, y, x + w - scaledSlice, y + scaledSlice,
                slice, 0, panel.getWidth() - slice, slice, null);
        g2d.drawImage(panel, x + w - scaledSlice, y, x + w, y + scaledSlice,
                panel.getWidth() - slice, 0, panel.getWidth(), slice, null);
        g2d.drawImage(panel, x, y + scaledSlice, x + scaledSlice, y + h - scaledSlice,
                0, slice, slice, panel.getHeight() - slice, null);
        g2d.drawImage(panel, x + scaledSlice, y + scaledSlice, x + w - scaledSlice, y + h - scaledSlice,
                slice, slice, panel.getWidth() - slice, panel.getHeight() - slice, null);
        g2d.drawImage(panel, x + w - scaledSlice, y + scaledSlice, x + w, y + h - scaledSlice,
                panel.getWidth() - slice, slice, panel.getWidth(), panel.getHeight() - slice, null);
        g2d.drawImage(panel, x, y + h - scaledSlice, x + scaledSlice, y + h,
                0, panel.getHeight() - slice, slice, panel.getHeight(), null);
        g2d.drawImage(panel, x + scaledSlice, y + h - scaledSlice, x + w - scaledSlice, y + h,
                slice, panel.getHeight() - slice, panel.getWidth() - slice, panel.getHeight(), null);
        g2d.drawImage(panel, x + w - scaledSlice, y + h - scaledSlice, x + w, y + h,
                panel.getWidth() - slice, panel.getHeight() - slice, panel.getWidth(), panel.getHeight(), null);
    }
}
