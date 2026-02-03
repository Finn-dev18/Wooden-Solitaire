import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel {
    private static final Color COLOR_BG = new Color(39, 30, 112);
    private static final Color COLOR_FROZEN = new Color(43, 253, 223);
    private static final Color COLOR_SHIELD = new Color(252, 16, 87);
    private static final int BASE_BOARD_SIZE = 512;
    private static final int BASE_CELL = 64;
    private static final int BASE_INSET = 32;
    private static final int BASE_SLOT = 32;
    private static final int BASE_SLOT_OFFSET = 16;

    private final AssetManager assets;
    private final GameModel model;
    private final GameUIController controller;
    private double scale = 2;
    private Rectangle[][] slotRects = new Rectangle[GameModel.BOARD_SIZE][GameModel.BOARD_SIZE];

    public GamePanel(AssetManager assets, GameModel model, GameUIController controller) {
        this.assets = assets;
        this.model = model;
        this.controller = controller;
        setBackground(COLOR_BG);
        int size = (int) Math.round(BASE_BOARD_SIZE * scale);
        setPreferredSize(new Dimension(size, size));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    public void setScale(double scale) {
        this.scale = Math.max(1, scale);
        int size = (int) Math.round(BASE_BOARD_SIZE * this.scale);
        setPreferredSize(new Dimension(size, size));
        revalidate();
        repaint();
    }

    private void handleClick(int x, int y) {
        int row = -1;
        int col = -1;
        for (int r = 0; r < GameModel.BOARD_SIZE; r++) {
            for (int c = 0; c < GameModel.BOARD_SIZE; c++) {
                Rectangle rect = slotRects[r][c];
                if (rect != null && rect.contains(x, y)) {
                    row = r;
                    col = c;
                    break;
                }
            }
        }
        if (row < 0) {
            return;
        }
        if (model.getActivePowerup() != null && model.getActivePowerup() != PowerupType.HINT) {
            model.applyPowerupClick(row, col);
            if (model.getActivePowerup() == PowerupType.LASER && model.hasLaserPending()) {
                controller.requestLaserDirection();
            }
            controller.onModelUpdated();
            return;
        }
        boolean[][] validTargets = model.getValidTargets();
        if (validTargets[row][col]) {
            model.applyMove(row, col);
            controller.onModelUpdated();
            return;
        }
        if (model.hasPeg(row, col)) {
            model.selectPeg(row, col);
            controller.onModelUpdated();
            return;
        }
        model.selectPeg(-1, -1);
        controller.onModelUpdated();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        int size = (int) Math.round(BASE_BOARD_SIZE * scale);
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;

        BufferedImage boardImage = assets.getImage("board_octagon_512.png");
        g2d.drawImage(boardImage, x, y, size, size, null);

        int cell = (int) Math.round(BASE_CELL * scale);
        int inset = (int) Math.round(BASE_INSET * scale);
        int slotSize = (int) Math.round(BASE_SLOT * scale);
        int slotOffset = (int) Math.round(BASE_SLOT_OFFSET * scale);

        BufferedImage slotImage = assets.getImage("slot_normal_32.png");
        BufferedImage pegNormal = assets.getImage("peg_normal_32.png");
        BufferedImage pegSpecial = assets.getImage("peg_special_32.png");
        BufferedImage overlaySelected = assets.getImage("overlay_selected_32.png");
        BufferedImage overlayTarget = assets.getImage("overlay_target_32.png");

        for (int r = 0; r < GameModel.BOARD_SIZE; r++) {
            for (int c = 0; c < GameModel.BOARD_SIZE; c++) {
                if (!model.isValidCell(r, c)) {
                    slotRects[r][c] = null;
                    continue;
                }
                int slotX = x + inset + c * cell + slotOffset;
                int slotY = y + inset + r * cell + slotOffset;
                Rectangle slotRect = new Rectangle(slotX, slotY, slotSize, slotSize);
                slotRects[r][c] = slotRect;
                g2d.drawImage(slotImage, slotX, slotY, slotSize, slotSize, null);
            }
        }

        for (int r = 0; r < GameModel.BOARD_SIZE; r++) {
            for (int c = 0; c < GameModel.BOARD_SIZE; c++) {
                if (!model.hasPeg(r, c)) {
                    continue;
                }
                Rectangle slotRect = slotRects[r][c];
                if (slotRect == null) {
                    continue;
                }
                BufferedImage pegImage = model.isShielded(r, c) ? pegSpecial : pegNormal;
                g2d.drawImage(pegImage, slotRect.x, slotRect.y, slotRect.width, slotRect.height, null);
                if (model.isFrozen(r, c)) {
                    g2d.setColor(COLOR_FROZEN);
                    g2d.drawRect(slotRect.x, slotRect.y, slotRect.width - 1, slotRect.height - 1);
                }
                if (model.isShielded(r, c)) {
                    g2d.setColor(COLOR_SHIELD);
                    g2d.drawRect(slotRect.x + 2, slotRect.y + 2, slotRect.width - 5, slotRect.height - 5);
                }
            }
        }

        if (model.getSelectedRow() >= 0) {
            Rectangle selected = slotRects[model.getSelectedRow()][model.getSelectedCol()];
            if (selected != null) {
                g2d.drawImage(overlaySelected, selected.x, selected.y, selected.width, selected.height, null);
            }
        }

        boolean[][] targets = model.getValidTargets();
        for (int r = 0; r < GameModel.BOARD_SIZE; r++) {
            for (int c = 0; c < GameModel.BOARD_SIZE; c++) {
                if (targets[r][c]) {
                    Rectangle target = slotRects[r][c];
                    if (target != null) {
                        g2d.drawImage(overlayTarget, target.x, target.y, target.width, target.height, null);
                    }
                }
            }
        }

        if (model.getSwapRow() >= 0) {
            Rectangle swap = slotRects[model.getSwapRow()][model.getSwapCol()];
            if (swap != null) {
                g2d.drawImage(overlaySelected, swap.x, swap.y, swap.width, swap.height, null);
            }
        }

        if (model.hasLaserPending()) {
            Rectangle laser = slotRects[model.getLaserRow()][model.getLaserCol()];
            if (laser != null) {
                g2d.drawImage(overlayTarget, laser.x, laser.y, laser.width, laser.height, null);
            }
        }

        if (model.hasHintHighlight()) {
            Rectangle hintFrom = slotRects[model.getHintFromRow()][model.getHintFromCol()];
            Rectangle hintTo = slotRects[model.getHintToRow()][model.getHintToCol()];
            if (hintFrom != null) {
                g2d.drawImage(overlaySelected, hintFrom.x, hintFrom.y, hintFrom.width, hintFrom.height, null);
            }
            if (hintTo != null) {
                g2d.drawImage(overlayTarget, hintTo.x, hintTo.y, hintTo.width, hintTo.height, null);
            }
        }

        g2d.dispose();
    }
}
