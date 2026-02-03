import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GameModel {
    public static final int BOARD_SIZE = 7;
    public static final int STATUS_DURATION = 3;

    private final Board board = new Board();
    private final MoveValidator validator = new MoveValidator();
    private final int[][] frozenTurns = new int[BOARD_SIZE][BOARD_SIZE];
    private final int[][] shieldTurns = new int[BOARD_SIZE][BOARD_SIZE];
    private final Map<PowerupType, Integer> charges = new EnumMap<>(PowerupType.class);

    private int movesCount;
    private int credits;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean[][] validTargets = new boolean[BOARD_SIZE][BOARD_SIZE];
    private PowerupType activePowerup;
    private int swapRow = -1;
    private int swapCol = -1;
    private int laserRow = -1;
    private int laserCol = -1;
    private String statusMessage = "";
    private boolean gameOver;
    private int hintFromRow = -1;
    private int hintFromCol = -1;
    private int hintToRow = -1;
    private int hintToCol = -1;
    private long hintEndTime;
    private String playerName = "Player";
    private long startTimeMs;
    private long endTimeMs;

    public GameModel() {
        resetGame();
    }

    public void resetGame() {
        board.reset();
        movesCount = 0;
        selectedRow = -1;
        selectedCol = -1;
        activePowerup = null;
        swapRow = -1;
        swapCol = -1;
        laserRow = -1;
        laserCol = -1;
        hintFromRow = -1;
        hintToRow = -1;
        hintEndTime = 0;
        gameOver = false;
        startTimeMs = System.currentTimeMillis();
        endTimeMs = 0;
        statusMessage = "Wähle eine Kugel für deinen Zug.";
        clearStatuses();
        initCharges();
        clearValidTargets();
    }

    private void initCharges() {
        charges.clear();
        for (PowerupType type : PowerupType.values()) {
            charges.put(type, 0);
        }
    }

    private void clearStatuses() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                frozenTurns[r][c] = 0;
                shieldTurns[r][c] = 0;
            }
        }
    }

    public int getMovesCount() {
        return movesCount;
    }

    public int getCredits() {
        return credits;
    }

    public int getPegsLeft() {
        return board.countPegs();
    }

    public boolean isValidCell(int r, int c) {
        return r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board.get(r, c) != ' ';
    }

    public boolean hasPeg(int r, int c) {
        return isValidCell(r, c) && board.get(r, c) == '●';
    }

    public boolean isEmpty(int r, int c) {
        return isValidCell(r, c) && board.get(r, c) == '○';
    }

    public boolean isFrozen(int r, int c) {
        return frozenTurns[r][c] > 0;
    }

    public boolean isShielded(int r, int c) {
        return shieldTurns[r][c] > 0;
    }

    public int getFrozenTurns(int r, int c) {
        return frozenTurns[r][c];
    }

    public int getShieldTurns(int r, int c) {
        return shieldTurns[r][c];
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    public int getSelectedCol() {
        return selectedCol;
    }

    public boolean[][] getValidTargets() {
        return validTargets;
    }

    public PowerupType getActivePowerup() {
        return activePowerup;
    }

    public Map<PowerupType, Integer> getCharges() {
        return charges;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String name) {
        if (name == null) {
            return;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.length() > 12) {
            trimmed = trimmed.substring(0, 12);
        }
        playerName = trimmed;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public java.time.Duration getElapsedDuration() {
        long endTime = endTimeMs > 0 ? endTimeMs : System.currentTimeMillis();
        if (endTime < startTimeMs) {
            return java.time.Duration.ZERO;
        }
        return java.time.Duration.ofMillis(endTime - startTimeMs);
    }

    public boolean hasHintHighlight() {
        return System.currentTimeMillis() < hintEndTime;
    }

    public int getHintFromRow() {
        return hintFromRow;
    }

    public int getHintFromCol() {
        return hintFromCol;
    }

    public int getHintToRow() {
        return hintToRow;
    }

    public int getHintToCol() {
        return hintToCol;
    }

    public boolean hasLaserPending() {
        return laserRow >= 0 && laserCol >= 0;
    }

    public int getLaserRow() {
        return laserRow;
    }

    public int getLaserCol() {
        return laserCol;
    }

    public int getSwapRow() {
        return swapRow;
    }

    public int getSwapCol() {
        return swapCol;
    }

    public void clearLaserPending() {
        laserRow = -1;
        laserCol = -1;
    }

    public void selectPeg(int r, int c) {
        if (!hasPeg(r, c)) {
            statusMessage = "Kein Peg ausgewählt.";
            selectedRow = -1;
            selectedCol = -1;
            clearValidTargets();
            return;
        }
        if (isFrozen(r, c)) {
            statusMessage = "Dieser Peg ist eingefroren.";
            return;
        }
        selectedRow = r;
        selectedCol = c;
        updateValidTargets();
        if (!hasAnyValidTarget()) {
            statusMessage = "Keine gültigen Ziele für diesen Peg.";
        } else {
            statusMessage = "Ziel auswählen.";
        }
    }

    public boolean applyMove(int toRow, int toCol) {
        if (selectedRow < 0 || selectedCol < 0) {
            return false;
        }
        Move move = new Move(selectedRow, selectedCol, toRow, toCol);
        if (!validator.isValid(board, move)) {
            statusMessage = "Ungültiger Zug.";
            return false;
        }
        if (isFrozen(selectedRow, selectedCol)) {
            statusMessage = "Der ausgewählte Peg ist eingefroren.";
            return false;
        }
        int jumpRow = (selectedRow + toRow) / 2;
        int jumpCol = (selectedCol + toCol) / 2;
        if (isFrozen(jumpRow, jumpCol)) {
            statusMessage = "Der übersprungene Peg ist eingefroren.";
            return false;
        }
        board.set(selectedRow, selectedCol, '○');
        board.set(jumpRow, jumpCol, '○');
        board.set(toRow, toCol, '●');
        movesCount++;
        addCredits(1);
        tickStatusesAfterMove();
        selectedRow = -1;
        selectedCol = -1;
        clearValidTargets();
        statusMessage = "Zug ausgeführt.";
        updateGameOver();
        return true;
    }

    public void activatePowerup(PowerupType type) {
        if (type == null) {
            return;
        }
        int remaining = charges.getOrDefault(type, 0);
        if (remaining <= 0) {
            statusMessage = "Keine Ladungen verfügbar. Shop nutzen.";
            return;
        }
        if (type == PowerupType.HINT) {
            triggerHint();
            return;
        }
        activePowerup = type;
        swapRow = -1;
        swapCol = -1;
        laserRow = -1;
        laserCol = -1;
        statusMessage = type.getLabel() + " aktiv: Ziel auswählen.";
    }

    public void cancelPowerup() {
        activePowerup = null;
        swapRow = -1;
        swapCol = -1;
        laserRow = -1;
        laserCol = -1;
        statusMessage = "Powerup abgebrochen.";
    }

    public void applyPowerupClick(int r, int c) {
        if (activePowerup == null) {
            return;
        }
        switch (activePowerup) {
            case BOMB:
                applyBomb(r, c);
                break;
            case SWAP:
                applySwap(r, c);
                break;
            case FREEZE:
                applyFreeze(r, c);
                break;
            case LASER:
                prepareLaser(r, c);
                break;
            case SHIELD:
                applyShield(r, c);
                break;
            default:
                break;
        }
    }

    private void applyBomb(int r, int c) {
        if (!hasPeg(r, c)) {
            statusMessage = "Bomb braucht einen Peg.";
            return;
        }
        if (isShielded(r, c)) {
            statusMessage = "Dieser Peg ist geschützt.";
            return;
        }
        board.set(r, c, '○');
        consumeCharge(PowerupType.BOMB);
        activePowerup = null;
        statusMessage = "Bomb: Peg entfernt.";
        updateGameOver();
    }

    private void applySwap(int r, int c) {
        if (swapRow < 0) {
            if (!hasPeg(r, c)) {
                statusMessage = "Swap: Wähle zuerst einen Peg.";
                return;
            }
            if (isFrozen(r, c)) {
                statusMessage = "Dieser Peg ist eingefroren.";
                return;
            }
            swapRow = r;
            swapCol = c;
            statusMessage = "Swap: Ziel-Feld wählen.";
            return;
        }
        if (!isEmpty(r, c)) {
            statusMessage = "Swap: Ziel muss leer sein.";
            return;
        }
        board.set(swapRow, swapCol, '○');
        board.set(r, c, '●');
        swapRow = -1;
        swapCol = -1;
        consumeCharge(PowerupType.SWAP);
        activePowerup = null;
        statusMessage = "Swap ausgeführt.";
    }

    private void applyFreeze(int r, int c) {
        if (!hasPeg(r, c)) {
            statusMessage = "Freeze: Wähle einen Peg.";
            return;
        }
        frozenTurns[r][c] = STATUS_DURATION;
        consumeCharge(PowerupType.FREEZE);
        activePowerup = null;
        statusMessage = "Peg eingefroren.";
    }

    private void prepareLaser(int r, int c) {
        if (!isValidCell(r, c)) {
            statusMessage = "Laser: ungültiges Feld.";
            return;
        }
        laserRow = r;
        laserCol = c;
        statusMessage = "Laser: Richtung auswählen (H/V oder Pfeile).";
    }

    public void applyLaserDirection(boolean horizontal) {
        if (!hasLaserPending()) {
            return;
        }
        if (horizontal) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (hasPeg(laserRow, c) && !isShielded(laserRow, c)) {
                    board.set(laserRow, c, '○');
                }
            }
        } else {
            for (int r = 0; r < BOARD_SIZE; r++) {
                if (hasPeg(r, laserCol) && !isShielded(r, laserCol)) {
                    board.set(r, laserCol, '○');
                }
            }
        }
        consumeCharge(PowerupType.LASER);
        activePowerup = null;
        clearLaserPending();
        statusMessage = "Laser ausgelöst.";
        updateGameOver();
    }

    private void applyShield(int r, int c) {
        if (!hasPeg(r, c)) {
            statusMessage = "Shield: Wähle einen Peg.";
            return;
        }
        shieldTurns[r][c] = STATUS_DURATION;
        consumeCharge(PowerupType.SHIELD);
        activePowerup = null;
        statusMessage = "Peg geschützt.";
    }

    private void consumeCharge(PowerupType type) {
        int remaining = charges.getOrDefault(type, 0);
        if (remaining > 0) {
            charges.put(type, remaining - 1);
        }
    }

    public boolean purchasePowerup(PowerupType type) {
        if (type == null) {
            return false;
        }
        int cost = type.getCost();
        if (cost <= 0) {
            return false;
        }
        if (!spendCredits(cost)) {
            statusMessage = "Nicht genug Credits.";
            return false;
        }
        charges.put(type, charges.getOrDefault(type, 0) + 1);
        statusMessage = type.getLabel() + " gekauft.";
        return true;
    }

    private void addCredits(int amount) {
        if (amount > 0) {
            credits += amount;
        }
    }

    private boolean spendCredits(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (credits < amount) {
            return false;
        }
        credits -= amount;
        return true;
    }

    private void triggerHint() {
        Move hintMove = findAnyValidMove();
        if (hintMove == null) {
            statusMessage = "Kein Zug verfügbar.";
            hintEndTime = 0;
            return;
        }
        hintFromRow = hintMove.getFromRow();
        hintFromCol = hintMove.getFromCol();
        hintToRow = hintMove.getToRow();
        hintToCol = hintMove.getToCol();
        hintEndTime = System.currentTimeMillis() + 1500;
        statusMessage = "Hint zeigt einen möglichen Zug.";
    }

    private Move findAnyValidMove() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (!hasPeg(r, c) || isFrozen(r, c)) {
                    continue;
                }
                List<Move> moves = collectMovesFor(r, c);
                if (!moves.isEmpty()) {
                    return moves.get(0);
                }
            }
        }
        return null;
    }

    private void updateValidTargets() {
        clearValidTargets();
        if (selectedRow < 0) {
            return;
        }
        List<Move> moves = collectMovesFor(selectedRow, selectedCol);
        for (Move move : moves) {
            validTargets[move.getToRow()][move.getToCol()] = true;
        }
    }

    private List<Move> collectMovesFor(int r, int c) {
        List<Move> moves = new ArrayList<>();
        int[][] deltas = {{0, 2}, {0, -2}, {2, 0}, {-2, 0}};
        for (int[] delta : deltas) {
            int tr = r + delta[0];
            int tc = c + delta[1];
            Move move = new Move(r, c, tr, tc);
            if (!validator.isValid(board, move)) {
                continue;
            }
            int jumpRow = (r + tr) / 2;
            int jumpCol = (c + tc) / 2;
            if (isFrozen(r, c) || isFrozen(jumpRow, jumpCol)) {
                continue;
            }
            moves.add(move);
        }
        return moves;
    }

    private boolean hasAnyValidTarget() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (validTargets[r][c]) {
                    return true;
                }
            }
        }
        return false;
    }

    private void clearValidTargets() {
        validTargets = new boolean[BOARD_SIZE][BOARD_SIZE];
    }

    private void tickStatusesAfterMove() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (frozenTurns[r][c] > 0) {
                    frozenTurns[r][c]--;
                }
                if (shieldTurns[r][c] > 0) {
                    shieldTurns[r][c]--;
                }
            }
        }
    }

    public boolean hasAnyValidMove() {
        return findAnyValidMove() != null;
    }

    private void updateGameOver() {
        if (!hasAnyValidMove()) {
            gameOver = true;
            if (endTimeMs == 0) {
                endTimeMs = System.currentTimeMillis();
            }
            statusMessage = "Keine Züge mehr. Spiel beendet.";
            addCredits(5);
        }
    }
}
