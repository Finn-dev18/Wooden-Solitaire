import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel {
    private static final Color COLOR_BG = new Color(39, 30, 112);
    private static final int BASE_BOARD_CANVAS_SIZE = 320;
    private static final int BASE_SLOT = 32;
    private static final double BOARD_ART_FILL = 0.94;

    private final AssetManager assets;
    private final GameModel model;
    private final GameUIController controller;
    private int scale = 2;
    private Rectangle[][] slotRects = new Rectangle[GameModel.BOARD_SIZE][GameModel.BOARD_SIZE];
    private Point hoverCell;

    public GamePanel(AssetManager assets, GameModel model, GameUIController controller) {
        this.assets = assets;
        this.model = model;
        this.controller = controller;
        setBackground(COLOR_BG);
        int size = BASE_BOARD_CANVAS_SIZE * scale;
        setPreferredSize(new Dimension(size, size));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (controller.isOverlayActive()) {
                    return;
                }
                handleClick(e.getX(), e.getY());
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (controller.isOverlayActive()) {
                    hoverCell = null;
                    repaint();
                    return;
                }
                hoverCell = findHoverCell(e.getX(), e.getY());
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverCell = null;
                repaint();
            }
        });
    }

    public void setScale(int scale) {
        this.scale = Math.max(2, scale);
        int size = BASE_BOARD_CANVAS_SIZE * this.scale;
        setPreferredSize(new Dimension(size, size));
        revalidate();
        repaint();
    }

    private Point findHoverCell(int x, int y) {
        for (int r = 0; r < GameModel.BOARD_SIZE; r++) {
            for (int c = 0; c < GameModel.BOARD_SIZE; c++) {
                Rectangle rect = slotRects[r][c];
                if (rect != null && rect.contains(x, y)) {
                    return new Point(r, c);
                }
            }
        }
        return null;
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
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        int size = BASE_BOARD_CANVAS_SIZE * scale;
        int canvasX = (getWidth() - size) / 2;
        int canvasY = (getHeight() - size) / 2;

        int boardArtSize = (int) Math.round(size * BOARD_ART_FILL);
        int boardX = canvasX + (size - boardArtSize) / 2;
        int boardY = canvasY + (size - boardArtSize) / 2;

        BufferedImage boardImage = assets.getImage("board_octagon_768.png");
        g2d.drawImage(boardImage, boardX, boardY, boardArtSize, boardArtSize, null);

        int slotSize = BASE_SLOT * scale;
        int gridSize = slotSize * GameModel.BOARD_SIZE;
        int inset = (boardArtSize - gridSize) / 2;

        BufferedImage slotImage = assets.getImage("slot_empty_32.png");
        BufferedImage hoverImage = assets.getImage("slot_hover_32.png");
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
                int slotX = boardX + inset + c * slotSize;
                int slotY = boardY + inset + r * slotSize;
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
            }
        }

        if (hoverCell != null && controller.getOverlayState() == OverlayState.NONE) {
            Rectangle hoverRect = slotRects[hoverCell.x][hoverCell.y];
            if (hoverRect != null) {
                g2d.drawImage(hoverImage, hoverRect.x, hoverRect.y, hoverRect.width, hoverRect.height, null);
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
