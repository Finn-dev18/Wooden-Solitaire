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
import java.util.EnumMap;
import java.util.Map;

public class UIPanelLeftPowerups extends JPanel {
    private static final Color COLOR_TEXT = new Color(43, 253, 223);
    private static final Color COLOR_TEXT_MUTED = new Color(171, 25, 111);
    private static final int BASE_WIDTH = 220;
    private static final int BASE_PADDING = 16;
    private static final int BASE_BUTTON_WIDTH = 192;
    private static final int BASE_BUTTON_HEIGHT = 64;
    private static final int BASE_ICON_SIZE = 16;
    private static final int BASE_BUY_WIDTH = 56;
    private static final int BASE_BUY_HEIGHT = 26;

    private final AssetManager assets;
    private final GameModel model;
    private final GameUIController controller;
    private double scale = 2;
    private PowerupType hovered;
    private PowerupType pressed;
    private PowerupType hoveredBuy;
    private PowerupType pressedBuy;
    private final Map<PowerupType, Rectangle> buttonRects = new EnumMap<>(PowerupType.class);
    private final Map<PowerupType, Rectangle> buyRects = new EnumMap<>(PowerupType.class);

    public UIPanelLeftPowerups(AssetManager assets, GameModel model, GameUIController controller) {
        this.assets = assets;
        this.model = model;
        this.controller = controller;
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressedBuy = findBuyButton(e.getX(), e.getY());
                if (pressedBuy == null) {
                    pressed = findButton(e.getX(), e.getY());
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PowerupType releasedBuy = findBuyButton(e.getX(), e.getY());
                if (pressedBuy != null && pressedBuy == releasedBuy) {
                    model.purchasePowerup(releasedBuy);
                    controller.onModelUpdated();
                } else {
                    PowerupType released = findButton(e.getX(), e.getY());
                    if (pressed != null && pressed == released) {
                        model.activatePowerup(released);
                        controller.onModelUpdated();
                    }
                }
                pressed = null;
                pressedBuy = null;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = null;
                hoveredBuy = null;
                repaint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hovered = findButton(e.getX(), e.getY());
                hoveredBuy = findBuyButton(e.getX(), e.getY());
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

    private PowerupType findButton(int x, int y) {
        for (Map.Entry<PowerupType, Rectangle> entry : buttonRects.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private PowerupType findBuyButton(int x, int y) {
        for (Map.Entry<PowerupType, Rectangle> entry : buyRects.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int padding = (int) Math.round(BASE_PADDING * scale);
        int buttonWidth = (int) Math.round(BASE_BUTTON_WIDTH * scale);
        int buttonHeight = (int) Math.round(BASE_BUTTON_HEIGHT * scale);
        int iconSize = (int) Math.round(BASE_ICON_SIZE * scale);
        int buyWidth = (int) Math.round(BASE_BUY_WIDTH * scale);
        int buyHeight = (int) Math.round(BASE_BUY_HEIGHT * scale);
        int x = (getWidth() - buttonWidth) / 2;
        int y = padding;
        float fontScale = (float) (scale / 2.0);

        drawPanelBackground(g2d);

        g2d.setColor(COLOR_TEXT);
        g2d.setFont(getFont().deriveFont(Font.BOLD, 16f * fontScale));
        g2d.drawString("POWERUPS", x, y + (int) Math.round(12 * scale));
        y += padding + (int) Math.round(12 * scale);

        g2d.setFont(getFont().deriveFont(Font.PLAIN, 12f * fontScale));
        g2d.drawString("Credits: " + model.getCredits(), x, y + (int) Math.round(10 * scale));
        y += padding;

        buttonRects.clear();
        buyRects.clear();
        for (PowerupType type : PowerupType.values()) {
            Rectangle rect = new Rectangle(x, y, buttonWidth, buttonHeight);
            buttonRects.put(type, rect);
            BufferedImage buttonImage = getButtonImage(type);
            g2d.drawImage(buttonImage, rect.x, rect.y, rect.width, rect.height, null);

            BufferedImage icon = assets.getImage(getIconName(type));
            int iconX = rect.x + padding;
            int iconY = rect.y + (rect.height - iconSize) / 2;
            g2d.drawImage(icon, iconX, iconY, iconSize, iconSize, null);

            g2d.setFont(getFont().deriveFont(Font.BOLD, 12f * fontScale));
            g2d.setColor(COLOR_TEXT);
            g2d.drawString(type.getLabel(), iconX + iconSize + padding, rect.y + rect.height / 2 + (int) Math.round(4 * scale));

            g2d.setFont(getFont().deriveFont(Font.PLAIN, 11f * fontScale));
            g2d.setColor(COLOR_TEXT_MUTED);
            int charges = model.getCharges().getOrDefault(type, 0);
            String chargeText = "x" + charges;
            int chargeWidth = g2d.getFontMetrics().stringWidth(chargeText);
            Rectangle buyRect = new Rectangle(rect.x + rect.width - padding - buyWidth, rect.y + padding / 2, buyWidth, buyHeight);
            int chargeX = rect.x + rect.width - padding - buyWidth - padding - chargeWidth;
            g2d.drawString(chargeText, chargeX, rect.y + rect.height / 2 + (int) Math.round(4 * scale));

            buyRects.put(type, buyRect);
            drawBuyButton(g2d, buyRect, type, fontScale, hoveredBuy == type, pressedBuy == type);

            if (model.getActivePowerup() == type) {
                g2d.setColor(new Color(252, 16, 87));
                g2d.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
            }

            y += buttonHeight + padding / 2;
        }

        g2d.dispose();
    }

    private void drawBuyButton(Graphics2D g2d, Rectangle rect, PowerupType type, float fontScale, boolean hover, boolean pressed) {
        Color fill = hover ? new Color(70, 55, 150) : new Color(53, 42, 140);
        if (pressed) {
            fill = new Color(30, 25, 120);
        }
        g2d.setColor(fill);
        g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
        g2d.setColor(COLOR_TEXT);
        g2d.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
        g2d.setFont(getFont().deriveFont(Font.BOLD, 10f * fontScale));
        String label = "BUY " + type.getCost();
        int textWidth = g2d.getFontMetrics().stringWidth(label);
        int textX = rect.x + (rect.width - textWidth) / 2;
        int textY = rect.y + rect.height / 2 + (int) Math.round(4 * scale / 2);
        g2d.drawString(label, textX, textY);
    }

    private BufferedImage getButtonImage(PowerupType type) {
        if (pressed == type) {
            return assets.getImage("powerup_button_pressed_192x64.png");
        }
        if (hovered == type) {
            return assets.getImage("powerup_button_hover_192x64.png");
        }
        return assets.getImage("powerup_button_normal_192x64.png");
    }

    private String getIconName(PowerupType type) {
        switch (type) {
            case HINT:
                return "icon_hint_16.png";
            case BOMB:
                return "icon_bomb_16.png";
            case SWAP:
                return "icon_swap_16.png";
            case FREEZE:
                return "icon_freeze_16.png";
            case LASER:
                return "icon_laser_16.png";
            case SHIELD:
                return "icon_shield_16.png";
            default:
                return "icon_hint_16.png";
        }
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
