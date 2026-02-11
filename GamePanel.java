import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GamePanel extends JPanel {
    private static final Color COLOR_BG = new Color(39, 30, 112);
    private static final int BASE_BOARD_CANVAS_SIZE = 768;
    private static final int BASE_CELL_SIZE = 32;
    private static final int BASE_SLOT_SIZE = 32;
    private static final int BASE_PEG_SIZE = 32;
    private static final int BASE_GRID_OFFSET_X = (BASE_BOARD_CANVAS_SIZE - (GameModel.BOARD_SIZE * BASE_CELL_SIZE)) / 2;
    private static final int BASE_GRID_OFFSET_Y = (BASE_BOARD_CANVAS_SIZE - (GameModel.BOARD_SIZE * BASE_CELL_SIZE)) / 2;
    private static final int DEFAULT_TOP_UI_MARGIN = 64;
    private static final int DEFAULT_CENTER_MARGIN = 24;

    private final AssetManager assets;
    private final GameModel model;
    private final GameUIController controller;
    private final int boardBasePx;
    private int boardScale = 2;
    private int uiScale = 2;
    private int topUiMargin = DEFAULT_TOP_UI_MARGIN;
    private int centerMargin = DEFAULT_CENTER_MARGIN;
    private Rectangle[][] slotRects = new Rectangle[GameModel.BOARD_SIZE][GameModel.BOARD_SIZE];
    private Point hoverCell;

    public GamePanel(AssetManager assets, GameModel model, GameUIController controller) {
        this.assets = assets;
        this.model = model;
        this.controller = controller;
        this.boardBasePx = resolveBoardBasePx();
        setBackground(COLOR_BG);
        updatePreferredSize();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (controller.isOverlayActive()) return;
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

    public void setBoardScale(int scale) {
        this.boardScale = Math.max(1, scale);
        updatePreferredSize();
        revalidate();
        repaint();
    }

    public void setBoardLayoutMargins(int topUiMargin, int centerMargin) {
        this.topUiMargin = Math.max(0, topUiMargin);
        this.centerMargin = Math.max(0, centerMargin);
        updatePreferredSize();
        revalidate();
        repaint();
    }

    public void setUiScale(int scale) {
        this.uiScale = Math.max(1, scale);
        repaint();
    }

    private Point findHoverCell(int x, int y) {
        return mapPointToCell(x, y);
    }

    private void handleClick(int x, int y) {
        Point cell = mapPointToCell(x, y);
        if (cell == null) return;
        int row = cell.x;
        int col = cell.y;

        if (model.getActivePowerup() != null) {
            model.applyPowerupClick(row, col);
            Toolkit.getDefaultToolkit().beep();
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

        BoardMetrics metrics = calculateBoardMetrics();
        int boardPx = metrics.boardPx;
        int boardX = metrics.boardX;
        int boardY = metrics.boardY;

        BufferedImage boardImage = assets.getImage("board_octagon_768_sym.png");
        g2d.drawImage(boardImage, boardX, boardY, boardPx, boardPx, null);

        int cellPx = BASE_CELL_SIZE * boardScale;
        int slotPx = BASE_SLOT_SIZE * boardScale;
        int pegPx = BASE_PEG_SIZE * boardScale;
        int gridOffsetX = BASE_GRID_OFFSET_X * boardScale;
        int gridOffsetY = BASE_GRID_OFFSET_Y * boardScale;

        BufferedImage slotImage = assets.getImage("slot_empty_32.png");
        BufferedImage hoverImage = assets.getImage("slot_hover_32.png");
        BufferedImage pegNormal = assets.getImage("peg_normal_32.png");
        BufferedImage overlaySelected = assets.getImage("overlay_selected_32.png");
        BufferedImage overlayTarget = assets.getImage("overlay_target_32.png");

        for (int r = 0; r < GameModel.BOARD_SIZE; r++) {
            for (int c = 0; c < GameModel.BOARD_SIZE; c++) {
                if (!model.isValidCell(r, c)) {
                    slotRects[r][c] = null;
                    continue;
                }
                int slotX = boardX + gridOffsetX + c * cellPx;
                int slotY = boardY + gridOffsetY + r * cellPx;
                Rectangle slotRect = new Rectangle(slotX, slotY, slotPx, slotPx);
                slotRects[r][c] = slotRect;
                g2d.drawImage(slotImage, slotX, slotY, slotPx, slotPx, null);
            }
        }

        for (int r = 0; r < GameModel.BOARD_SIZE; r++) {
            for (int c = 0; c < GameModel.BOARD_SIZE; c++) {
                if (!model.hasPeg(r, c)) continue;
                Rectangle slotRect = slotRects[r][c];
                if (slotRect == null) continue;
                g2d.drawImage(pegNormal, slotRect.x, slotRect.y, pegPx, pegPx, null);
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
            if (selected != null) g2d.drawImage(overlaySelected, selected.x, selected.y, selected.width, selected.height, null);
        }

        boolean[][] targets = model.getValidTargets();
        for (int r = 0; r < GameModel.BOARD_SIZE; r++) {
            for (int c = 0; c < GameModel.BOARD_SIZE; c++) {
                if (targets[r][c]) {
                    Rectangle target = slotRects[r][c];
                    if (target != null) g2d.drawImage(overlayTarget, target.x, target.y, target.width, target.height, null);
                }
            }
        }

        if (model.getSelectionRow() >= 0) {
            Rectangle powerSel = slotRects[model.getSelectionRow()][model.getSelectionCol()];
            if (powerSel != null) g2d.drawImage(overlaySelected, powerSel.x, powerSel.y, powerSel.width, powerSel.height, null);
        }

        drawStatus(g2d);
        drawToast(g2d);
        g2d.dispose();
    }

    private void drawStatus(Graphics2D g2d) {
        g2d.setColor(new Color(12, 9, 48, 220));
        int statusHeight = Math.max(30, topUiMargin - 20);
        g2d.fillRect(12, 12, getWidth() - 24, statusHeight);
        g2d.setColor(new Color(224, 230, 255));
        g2d.setFont(getFont().deriveFont((float) (11f * uiScale / 2f)));
        g2d.drawString(model.getStatusMessage(), 20, 12 + Math.max(16, statusHeight / 2));
    }

    private void drawToast(Graphics2D g2d) {
        if (!model.hasToast()) return;
        int width = getWidth() - 80;
        int height = 30 * uiScale;
        int x = 40;
        int y = getHeight() - height - 20;
        g2d.setColor(new Color(252, 16, 87, 220));
        g2d.fillRect(x, y, width, height);
        g2d.setColor(Color.WHITE);
        g2d.setFont(getFont().deriveFont((float) (12f * uiScale / 2f)));
        g2d.drawString(model.getToastMessage(), x + 12, y + height / 2 + 4);
    }

    private Point mapPointToCell(int x, int y) {
        BoardMetrics metrics = calculateBoardMetrics();
        int boardLocalX = x - metrics.boardX;
        int boardLocalY = y - metrics.boardY;
        if (boardLocalX < 0 || boardLocalY < 0 || boardLocalX >= metrics.boardPx || boardLocalY >= metrics.boardPx) {
            return null;
        }

        int gridLocalX = boardLocalX - metrics.gridOffsetX;
        int gridLocalY = boardLocalY - metrics.gridOffsetY;
        if (gridLocalX < 0 || gridLocalY < 0) {
            return null;
        }

        int col = gridLocalX / metrics.cellPx;
        int row = gridLocalY / metrics.cellPx;
        if (row < 0 || row >= GameModel.BOARD_SIZE || col < 0 || col >= GameModel.BOARD_SIZE) {
            return null;
        }
        if (!model.isValidCell(row, col)) {
            return null;
        }
        return new Point(row, col);
    }

    private BoardMetrics calculateBoardMetrics() {
        int boardPx = boardBasePx * boardScale;
        int boardX = (getWidth() - boardPx) / 2;
        int boardY = topUiMargin + ((getHeight() - topUiMargin - boardPx) / 2);
        int cellPx = BASE_CELL_SIZE * boardScale;
        int gridOffsetX = BASE_GRID_OFFSET_X * boardScale;
        int gridOffsetY = BASE_GRID_OFFSET_Y * boardScale;
        return new BoardMetrics(boardX, boardY, boardPx, cellPx, gridOffsetX, gridOffsetY);
    }

    private int resolveBoardBasePx() {
        try {
            return Math.max(1, assets.getImage("board_octagon_768_sym.png").getWidth());
        } catch (RuntimeException ex) {
            return BASE_BOARD_CANVAS_SIZE;
        }
    }

    private void updatePreferredSize() {
        int width = boardBasePx * boardScale;
        int height = topUiMargin + (boardBasePx * boardScale) + centerMargin;
        Dimension boardDimension = new Dimension(width, height);
        setPreferredSize(boardDimension);
        setMinimumSize(boardDimension);
        setMaximumSize(boardDimension);
    }

    private static final class BoardMetrics {
        private final int boardX;
        private final int boardY;
        private final int boardPx;
        private final int cellPx;
        private final int gridOffsetX;
        private final int gridOffsetY;

        private BoardMetrics(int boardX, int boardY, int boardPx, int cellPx, int gridOffsetX, int gridOffsetY) {
            this.boardX = boardX;
            this.boardY = boardY;
            this.boardPx = boardPx;
            this.cellPx = cellPx;
            this.gridOffsetX = gridOffsetX;
            this.gridOffsetY = gridOffsetY;
        }
    }
}
