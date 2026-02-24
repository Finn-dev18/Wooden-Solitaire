import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class UIPanelLeftPowerups extends JPanel {
    private static final Color COLOR_TEXT = new Color(43, 253, 223);
    private static final Color COLOR_TEXT_MUTED = new Color(224, 230, 255);
    private static final int BASE_PADDING = 16;
    private static final int BASE_BUTTON_WIDTH = 260;
    private static final int BASE_BUTTON_HEIGHT = 72;
    private static final int BASE_ICON_SIZE = 16;
    private static final int BASE_CONTENT_PADDING = 18;
    private static final int BASE_CHARGES_AREA_WIDTH = 80;
    private static final int BASE_ICON_AREA_WIDTH = 34;

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
                if (controller.isOverlayActive() || !model.isPowerupsEnabled()) return;
                pressed = findButton(e.getX(), e.getY());
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (controller.isOverlayActive() || !model.isPowerupsEnabled()) {
                    pressed = null;
                    repaint();
                    return;
                }
                PowerupType released = findButton(e.getX(), e.getY());
                if (pressed != null && pressed == released) {
                    int charges = model.getCharges().getOrDefault(released, 0);
                    if (charges > 0) {
                        model.activatePowerup(released);
                    } else {
                        model.tryBuyPowerup(released);
                    }
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
                if (controller.isOverlayActive() || !model.isPowerupsEnabled()) {
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
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int padding = (int) Math.round(BASE_PADDING * scale);
        int buttonWidth = (int) Math.round(BASE_BUTTON_WIDTH * scale);
        int iconSize = (int) Math.round(BASE_ICON_SIZE * scale);
        int contentPaddingLeft = (int) Math.round(BASE_CONTENT_PADDING * scale);
        int contentPaddingRight = (int) Math.round(BASE_CONTENT_PADDING * scale);
        int contentPaddingVertical = (int) Math.round(10 * scale);
        int chargesAreaWidth = (int) Math.round(BASE_CHARGES_AREA_WIDTH * scale);
        int iconAreaWidth = (int) Math.round(BASE_ICON_AREA_WIDTH * scale);

        int x = Math.max(padding / 2, (getWidth() - buttonWidth) / 2);
        int y = padding;

        g2d.setColor(new Color(21, 16, 68));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(COLOR_TEXT);
        g2d.setFont(UIFonts.h1(scale));
        int lineHeight = g2d.getFontMetrics().getHeight() + 2;
        g2d.drawString("POWERUPS", x, y + lineHeight);
        y += lineHeight + padding;

        g2d.setFont(UIFonts.small(scale));
        lineHeight = g2d.getFontMetrics().getHeight() + 2;
        if (model.isPowerupsEnabled()) {
            g2d.drawString("Hotkeys: 1..5 (use)", x, y + lineHeight);
        } else {
            g2d.drawString("POWERUPS DISABLED", x, y + lineHeight);
        }
        y += lineHeight;

        if (!model.isPowerupsEnabled()) {
            g2d.setColor(COLOR_TEXT_MUTED);
            g2d.setFont(UIFonts.body(scale));
            g2d.drawString("Classic mode: no powerups", x, y + lineHeight);
            buttonRects.clear();
            g2d.dispose();
            return;
        }

        buttonRects.clear();
        for (PowerupType type : PowerupType.values()) {
            g2d.setFont(UIFonts.h2(scale));
            FontMetrics titleFm = g2d.getFontMetrics();
            int titleLineHeight = titleFm.getHeight() + 2;

            g2d.setFont(UIFonts.body(scale));
            FontMetrics bodyFm = g2d.getFontMetrics();
            int bodyLineHeight = bodyFm.getHeight() + 2;

            int minButtonHeight = contentPaddingVertical * 2 + titleLineHeight + (bodyLineHeight * 3);
            int buttonHeight = Math.max((int) Math.round(BASE_BUTTON_HEIGHT * scale), minButtonHeight);

            Rectangle rect = new Rectangle(x, y, buttonWidth, buttonHeight);
            buttonRects.put(type, rect);
            BufferedImage buttonImage = getButtonImage(type);
            g2d.drawImage(buttonImage, rect.x, rect.y, rect.width, rect.height, null);

            int iconX = rect.x + contentPaddingLeft;
            int iconY = rect.y + (rect.height - iconSize) / 2;
            BufferedImage icon = assets.getImage(getIconName(type));
            g2d.drawImage(icon, iconX, iconY, iconSize, iconSize, null);

            int textBaseX = rect.x + contentPaddingLeft + iconAreaWidth;
            int chargesAreaX = rect.x + rect.width - contentPaddingRight - chargesAreaWidth;
            int maxTextWidth = Math.max(0, chargesAreaX - textBaseX);
            int textTopY = rect.y + contentPaddingVertical;

            g2d.setFont(UIFonts.h2(scale));
            g2d.setColor(COLOR_TEXT);
            String title = fitWithEllipsis(type.getLabel(), g2d.getFontMetrics(), maxTextWidth);
            int titleBaseline = textTopY + titleLineHeight;
            g2d.drawString(title, textBaseX, titleBaseline);

            g2d.setFont(UIFonts.body(scale));
            g2d.setColor(COLOR_TEXT_MUTED);
            int descriptionBaseline = titleBaseline + bodyLineHeight;
            drawWrappedText(g2d, type.getDescription(), textBaseX, descriptionBaseline, maxTextWidth, 2, bodyLineHeight);

            int charges = model.getCharges().getOrDefault(type, 0);
            int cap = model.getPowerupCap(type);
            String chargeText = "x" + charges + "/" + cap;
            FontMetrics chargesFm = g2d.getFontMetrics();
            int chargeWidth = chargesFm.stringWidth(chargeText);
            int chargeX = chargesAreaX + Math.max(0, chargesAreaWidth - chargeWidth);
            g2d.drawString(chargeText, chargeX, titleBaseline);

            int infoBaseline = descriptionBaseline + (2 * bodyLineHeight);
            int price = model.getPowerupPrice(type);
            g2d.drawString(fitWithEllipsis("Cost: " + price, bodyFm, maxTextWidth), textBaseX, infoBaseline);

            String buyState = model.canBuyPowerup(type) ? "Buy" : unavailableLabel(type);
            g2d.drawString(fitWithEllipsis(buyState, bodyFm, chargesAreaWidth), chargesAreaX, infoBaseline);

            if (model.getActivePowerup() == type) {
                g2d.setColor(new Color(252, 16, 87));
                g2d.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
            }

            y += buttonHeight + padding / 2;
        }

        g2d.dispose();
    }

    private String unavailableLabel(PowerupType type) {
        int charges = model.getCharges().getOrDefault(type, 0);
        if (charges > 0) {
            return "Use";
        }
        if (charges >= model.getPowerupCap(type) || charges >= model.getInventoryCap()) {
            return "Cap erreicht";
        }
        int missing = model.getMissingCreditsFor(type);
        if (missing > 0) {
            return "Need: " + missing;
        }
        return "Buy";
    }

    List<String> wrapLines(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return lines;
        }
        if (maxWidth <= 0) {
            lines.add("…");
            return lines;
        }

        String[] words = text.trim().split("\\s+");
        String currentLine = "";
        for (String rawWord : words) {
            String word = rawWord;
            if (fm.stringWidth(word) > maxWidth) {
                word = fitWithEllipsis(word, fm, maxWidth);
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine);
                    currentLine = "";
                }
                lines.add(word);
                continue;
            }

            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (fm.stringWidth(candidate) <= maxWidth) {
                currentLine = candidate;
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine);
                }
                currentLine = word;
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth, int maxLines, int lineHeight) {
        FontMetrics fm = g.getFontMetrics();
        List<String> wrapped = wrapLines(text, fm, maxWidth);
        boolean isTruncated = wrapped.size() > maxLines;
        int linesToDraw = Math.min(maxLines, wrapped.size());

        for (int i = 0; i < linesToDraw; i++) {
            String line = wrapped.get(i);
            if (i == linesToDraw - 1 && isTruncated) {
                line = fitWithEllipsis(line, fm, maxWidth, true);
            } else {
                line = fitWithEllipsis(line, fm, maxWidth);
            }
            g.drawString(line, x, y + (i * lineHeight));
        }
    }

    private String fitWithEllipsis(String text, FontMetrics fm, int maxWidth) {
        return fitWithEllipsis(text, fm, maxWidth, false);
    }

    private String fitWithEllipsis(String text, FontMetrics fm, int maxWidth, boolean forceEllipsis) {
        if (text == null) {
            return "";
        }
        if (maxWidth <= 0) {
            return "";
        }

        String ellipsis = "…";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        boolean needsTrim = forceEllipsis || fm.stringWidth(text) > maxWidth;
        if (!needsTrim) {
            return text;
        }
        if (ellipsisWidth >= maxWidth) {
            return ellipsis;
        }

        StringBuilder sb = new StringBuilder(text);
        while (sb.length() > 0 && fm.stringWidth(sb + ellipsis) > maxWidth) {
            sb.setLength(sb.length() - 1);
        }
        return sb + ellipsis;
    }

    private BufferedImage getButtonImage(PowerupType type) {
        boolean hasCharge = model.getCharges().getOrDefault(type, 0) > 0;
        boolean canBuy = model.canBuyPowerup(type);
        if (!hasCharge && !canBuy) {
            return assets.getImage("ui_button_disabled_260x72.png");
        }
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
