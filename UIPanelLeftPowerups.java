import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

public class UIPanelLeftPowerups extends JPanel {
    private static final Color COLOR_TEXT = new Color(43, 253, 223);
    private static final Color COLOR_TEXT_MUTED = new Color(224, 230, 255);
    private static final int BASE_PADDING = 16;
    private static final int BASE_BUTTON_WIDTH = 260;
    private static final int BASE_BUTTON_HEIGHT = 72;
    private static final int BASE_ICON_SIZE = 16;

    private final AssetManager assets;
    private final GameModel model;
    private final GameUIController controller;
    private int scale = 2;
    private PowerupType hovered;
    private PowerupType pressed;
    private final Map<PowerupType, Rectangle> buttonRects = new EnumMap<>(PowerupType.class);

    public UIPanelLeftPowerups(AssetManager assets, GameModel model, GameUIController controller) {
        this.assets = assets;
        this.model = model;
        this.controller = controller;
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (controller.isOverlayActive()) return;
                pressed = findButton(e.getX(), e.getY());
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (controller.isOverlayActive()) {
                    pressed = null;
                    repaint();
                    return;
                }
                PowerupType released = findButton(e.getX(), e.getY());
                if (pressed != null && pressed == released) {
                    model.activatePowerup(released);
                    controller.onModelUpdated();
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
                if (controller.isOverlayActive()) {
                    hovered = null;
                    repaint();
                    return;
                }
                hovered = findButton(e.getX(), e.getY());
                repaint();
            }
        });
    }

    public void setScale(int scale) {
        this.scale = Math.max(1, scale);
        revalidate();
        repaint();
    }

    private PowerupType findButton(int x, int y) {
        for (Map.Entry<PowerupType, Rectangle> entry : buttonRects.entrySet()) {
            if (entry.getValue().contains(x, y)) return entry.getKey();
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        int padding = (int) Math.round(BASE_PADDING * scale);
        int buttonWidth = (int) Math.round(BASE_BUTTON_WIDTH * scale);
        int buttonHeight = (int) Math.round(BASE_BUTTON_HEIGHT * scale);
        int iconSize = (int) Math.round(BASE_ICON_SIZE * scale);
        int x = Math.max(padding / 2, (getWidth() - buttonWidth) / 2);
        int y = padding;
        float fontScale = (float) scale;

        g2d.setColor(new Color(21, 16, 68));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(COLOR_TEXT);
        g2d.setFont(getFont().deriveFont(Font.BOLD, 14f * fontScale));
        g2d.drawString("POWERUPS", x, y + (int) Math.round(12 * scale));
        y += padding + (int) Math.round(12 * scale);

        g2d.setFont(getFont().deriveFont(Font.PLAIN, 11f * fontScale));
        g2d.drawString("Hotkeys: 1..5", x, y + (int) Math.round(9 * scale));
        y += padding;

        buttonRects.clear();
        for (PowerupType type : PowerupType.values()) {
            Rectangle rect = new Rectangle(x, y, buttonWidth, buttonHeight);
            buttonRects.put(type, rect);
            BufferedImage buttonImage = getButtonImage(type);
            g2d.drawImage(buttonImage, rect.x, rect.y, rect.width, rect.height, null);

            BufferedImage icon = assets.getImage(getIconName(type));
            int iconX = rect.x + padding;
            int iconY = rect.y + (rect.height - iconSize) / 2;
            g2d.drawImage(icon, iconX, iconY, iconSize, iconSize, null);

            g2d.setFont(getFont().deriveFont(Font.BOLD, 11f * fontScale));
            g2d.setColor(COLOR_TEXT);
            g2d.drawString(type.getLabel(), iconX + iconSize + padding, rect.y + rect.height / 2 + (int) Math.round(1 * scale));

            g2d.setFont(getFont().deriveFont(Font.PLAIN, 9f * fontScale));
            g2d.setColor(COLOR_TEXT_MUTED);
            g2d.drawString(type.getDescription(), iconX + iconSize + padding, rect.y + rect.height / 2 + (int) Math.round(12 * scale));

            int charges = model.getCharges().getOrDefault(type, 0);
            String chargeText = "x" + charges + "/5";
            int chargeWidth = g2d.getFontMetrics().stringWidth(chargeText);
            g2d.drawString(chargeText, rect.x + rect.width - padding - chargeWidth, rect.y + rect.height / 2 + (int) Math.round(1 * scale));

            if (model.getActivePowerup() == type) {
                g2d.setColor(new Color(252, 16, 87));
                g2d.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
            }

            y += buttonHeight + padding / 2;
        }

        g2d.dispose();
    }

    private BufferedImage getButtonImage(PowerupType type) {
        if (pressed == type) return assets.getImage("ui_button_pressed_260x72.png");
        if (hovered == type) return assets.getImage("ui_button_hover_260x72.png");
        return assets.getImage("ui_button_normal_260x72.png");
    }

    private String getIconName(PowerupType type) {
        switch (type) {
            case UNDO:
                return "icon_hint_16.png";
            case BOMB:
                return "icon_bomb_16.png";
            case SWAP:
                return "icon_swap_16.png";
            case BRIDGEJUMP:
                return "icon_laser_16.png";
            case RANDSTURM:
                return "icon_freeze_16.png";
            default:
                return "icon_swap_16.png";
        }
    }
}
