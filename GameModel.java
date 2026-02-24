import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameModel {
    public static final int BOARD_SIZE = 7;
    private static final int INVENTORY_CAP = 5;

    private final Board board = new Board();
    private final MoveValidator validator = new MoveValidator();
    private final Map<PowerupType, Integer> charges = new EnumMap<>(PowerupType.class);
    private final Deque<GameSnapshot> history = new ArrayDeque<>();
    private final Random random = new Random();

    private int movesCount;
    private int credits;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean[][] validTargets = new boolean[BOARD_SIZE][BOARD_SIZE];
    private PowerupType activePowerup;
    private int selectionRow = -1;
    private int selectionCol = -1;
    private String statusMessage = "";
    private boolean gameOver;
    private String toastMessage = "";
    private long toastEndTime;
    private String playerName = "Player";
    private long startTimeMs;
    private long endTimeMs;
    private GameMode mode = GameMode.POWERUPS;

    public GameModel() {
        resetGame();
    }

    public void resetGame() {
        board.reset();
        movesCount = 0;
        credits = 0;
        selectedRow = -1;
        selectedCol = -1;
        selectionRow = -1;
        selectionCol = -1;
        activePowerup = null;
        gameOver = false;
        startTimeMs = 0;
        endTimeMs = 0;
        statusMessage = "Wähle eine Kugel für deinen Zug.";
        clearValidTargets();
        initCharges();
        history.clear();
        clearToast();
    }

    private void initCharges() {
        charges.clear();
        for (PowerupType type : PowerupType.values()) {
            charges.put(type, 0);
        }
    }

    public int getMovesCount() { return movesCount; }
    public int getCredits() { return credits; }
    public int getPegsLeft() { return board.countPegs(); }
    public int getSelectedRow() { return selectedRow; }
    public int getSelectedCol() { return selectedCol; }
    public boolean[][] getValidTargets() { return validTargets; }
    public PowerupType getActivePowerup() { return activePowerup; }
    public Map<PowerupType, Integer> getCharges() { return charges; }
    public String getStatusMessage() { return statusMessage; }
    public boolean isGameOver() { return gameOver; }
    public int getSelectionRow() { return selectionRow; }
    public int getSelectionCol() { return selectionCol; }
    public String getToastMessage() { return toastMessage; }
    public String getPlayerName() { return playerName; }
    public GameMode getMode() { return mode; }
    public void setMode(GameMode mode) { if (mode != null) this.mode = mode; }
    public boolean isPowerupsEnabled() { return mode == GameMode.POWERUPS; }

    public void startTimerNow() {
        startTimeMs = System.currentTimeMillis();
        endTimeMs = 0;
    }

    public boolean hasToast() {
        return System.currentTimeMillis() < toastEndTime;
    }

    public Duration getElapsedDuration() {
        if (startTimeMs <= 0) {
            return Duration.ZERO;
        }
        long endTime = endTimeMs > 0 ? endTimeMs : System.currentTimeMillis();
        if (endTime < startTimeMs) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(endTime - startTimeMs);
    }

    public void setPlayerName(String name) {
        if (name == null) return;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return;
        if (trimmed.length() > 12) trimmed = trimmed.substring(0, 12);
        playerName = trimmed;
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

    public void selectPeg(int r, int c) {
        if (!hasPeg(r, c)) {
            statusMessage = "Kein Peg ausgewählt.";
            selectedRow = -1;
            selectedCol = -1;
            clearValidTargets();
            return;
        }
        selectedRow = r;
        selectedCol = c;
        updateValidTargets();
        statusMessage = hasAnyValidTarget() ? "Ziel auswählen." : "Keine gültigen Ziele für diesen Peg.";
    }

    public boolean applyMove(int toRow, int toCol) {
        if (selectedRow < 0 || selectedCol < 0) return false;
        Move move = new Move(selectedRow, selectedCol, toRow, toCol);
        if (!validator.isValid(board, move)) {
            statusMessage = "Ungültiger Zug.";
            return false;
        }
        pushSnapshot();
        int jumpRow = (selectedRow + toRow) / 2;
        int jumpCol = (selectedCol + toCol) / 2;
        board.set(selectedRow, selectedCol, '○');
        board.set(jumpRow, jumpCol, '○');
        board.set(toRow, toCol, '●');

        movesCount++;
        credits += 1;
        selectedRow = -1;
        selectedCol = -1;
        clearValidTargets();
        statusMessage = "Standardzug ausgeführt.";

        rollPowerupReward();
        updateGameOver();
        return true;
    }

    public void activatePowerup(PowerupType type) {
        if (!isPowerupsEnabled()) {
            statusMessage = "Classic mode: no powerups";
            showToast("Classic mode: no powerups");
            return;
        }
        if (type == null || gameOver) return;
        if (charges.getOrDefault(type, 0) <= 0) {
            statusMessage = "Keine Ladungen für " + type.getLabel() + ".";
            showToast("Keine Ladungen: " + type.getLabel());
            return;
        }

        if (type == PowerupType.UNDO || type == PowerupType.RANDSTURM) {
            applyInstantPowerup(type);
            return;
        }

        activePowerup = type;
        selectionRow = -1;
        selectionCol = -1;
        statusMessage = type.getLabel() + " aktiv: Ziel auswählen.";
    }

    private void applyInstantPowerup(PowerupType type) {
        if (type == PowerupType.UNDO) {
            if (history.isEmpty()) {
                statusMessage = "UNDO nicht möglich.";
                showToast("UNDO fehlgeschlagen");
                return;
            }
            restoreSnapshot(history.pop());
            consumeCharge(PowerupType.UNDO);
            activePowerup = null;
            showToast("UNDO ausgeführt");
            statusMessage = "Letzter Zustand wiederhergestellt.";
            return;
        }

        if (type == PowerupType.RANDSTURM) {
            pushSnapshot();
            SlideDirection direction = SlideDirection.random(random);
            applyRandsturm(direction);
            consumeCharge(PowerupType.RANDSTURM);
            activePowerup = null;
            selectedRow = -1;
            selectedCol = -1;
            clearValidTargets();
            statusMessage = "Randsturm nach " + direction.label + "!";
            showToast("Randsturm nach " + direction.label + "!");
            updateGameOver();
        }
    }

    public void cancelPowerup() {
        activePowerup = null;
        selectionRow = -1;
        selectionCol = -1;
        statusMessage = "Powerup abgebrochen.";
    }

    public void applyPowerupClick(int r, int c) {
        if (!isPowerupsEnabled()) {
            statusMessage = "Classic mode: no powerups";
            showToast("Classic mode: no powerups");
            return;
        }
        if (activePowerup == null) return;

        switch (activePowerup) {
            case BOMB:
                applyBomb(r, c);
                break;
            case SWAP:
                applySwap(r, c);
                break;
            case BRIDGEJUMP:
                applyBridgeJump(r, c);
                break;
            default:
                break;
        }
    }

    private void applyBomb(int r, int c) {
        if (!hasPeg(r, c)) {
            statusMessage = "BOMB: Wähle eine Kugel.";
            return;
        }
        pushSnapshot();
        board.set(r, c, '○');
        consumeCharge(PowerupType.BOMB);
        activePowerup = null;
        statusMessage = "Bomb ausgeführt.";
        showToast("Powerup: BOMB");
        updateGameOver();
    }

    private void applySwap(int r, int c) {
        if (!isValidCell(r, c)) {
            statusMessage = "SWAP: Ungültiges Feld.";
            return;
        }
        if (selectionRow < 0) {
            selectionRow = r;
            selectionCol = c;
            statusMessage = "SWAP: Zweites Feld wählen.";
            return;
        }

        if (!isValidCell(selectionRow, selectionCol)) {
            selectionRow = -1;
            selectionCol = -1;
            statusMessage = "SWAP abgebrochen.";
            return;
        }

        pushSnapshot();
        char first = board.get(selectionRow, selectionCol);
        char second = board.get(r, c);
        board.set(selectionRow, selectionCol, second);
        board.set(r, c, first);
        selectionRow = -1;
        selectionCol = -1;
        consumeCharge(PowerupType.SWAP);
        activePowerup = null;
        statusMessage = "Swap ausgeführt.";
        showToast("Powerup: SWAP");
        updateGameOver();
    }

    private void applyBridgeJump(int r, int c) {
        if (selectionRow < 0) {
            if (!hasPeg(r, c)) {
                statusMessage = "BRIDGE: Start muss eine Kugel sein.";
                return;
            }
            selectionRow = r;
            selectionCol = c;
            statusMessage = "BRIDGE: Ziel wählen (Distanz 3).";
            return;
        }

        int fr = selectionRow;
        int fc = selectionCol;
        if (!hasPeg(fr, fc) || !isEmpty(r, c)) {
            statusMessage = "BRIDGE: Ungültiger Start/Ziel.";
            return;
        }

        boolean horizontal = fr == r && Math.abs(fc - c) == 3;
        boolean vertical = fc == c && Math.abs(fr - r) == 3;
        if (!horizontal && !vertical) {
            statusMessage = "BRIDGE: Nur gerade Linie mit Distanz 3.";
            return;
        }

        int stepR = Integer.compare(r, fr);
        int stepC = Integer.compare(c, fc);
        int mid1r = fr + stepR;
        int mid1c = fc + stepC;
        int mid2r = fr + stepR * 2;
        int mid2c = fc + stepC * 2;
        if (!hasPeg(mid1r, mid1c) || !hasPeg(mid2r, mid2c)) {
            statusMessage = "BRIDGE: Zwei übersprungene Felder brauchen Kugeln.";
            return;
        }

        pushSnapshot();
        board.set(fr, fc, '○');
        board.set(mid1r, mid1c, '○');
        board.set(mid2r, mid2c, '○');
        board.set(r, c, '●');
        selectionRow = -1;
        selectionCol = -1;
        consumeCharge(PowerupType.BRIDGEJUMP);
        activePowerup = null;
        statusMessage = "Bridge Jump ausgeführt.";
        showToast("Powerup: BRIDGE JUMP");
        updateGameOver();
    }

    private void consumeCharge(PowerupType type) {
        int remaining = charges.getOrDefault(type, 0);
        if (remaining > 0) charges.put(type, remaining - 1);
    }

    private void rollPowerupReward() {
        if (!isPowerupsEnabled()) return;
        if (random.nextDouble() >= 0.35) return;

        PowerupType reward = rollByWeight();
        int current = charges.getOrDefault(reward, 0);
        if (current >= INVENTORY_CAP) {
            showToast("Inventar voll: " + reward.getLabel());
            statusMessage = "Inventar voll für " + reward.getLabel() + ".";
            return;
        }

        charges.put(reward, current + 1);
        showToast("Powerup erhalten: " + reward.getLabel());
        statusMessage = "Powerup erhalten: " + reward.getLabel();
    }

    private PowerupType rollByWeight() {
        double value = random.nextDouble();
        if (value < 0.30) return PowerupType.UNDO;
        if (value < 0.55) return PowerupType.SWAP;
        if (value < 0.75) return PowerupType.BOMB;
        if (value < 0.90) return PowerupType.BRIDGEJUMP;
        return PowerupType.RANDSTURM;
    }

    private void applyRandsturm(SlideDirection direction) {
        char[][] source = copyField(board.getField());
        char[][] result = new char[BOARD_SIZE][BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                result[r][c] = source[r][c] == ' ' ? ' ' : '○';
            }
        }

        if (direction == SlideDirection.LEFT || direction == SlideDirection.RIGHT) {
            for (int r = 0; r < BOARD_SIZE; r++) {
                List<Integer> validCols = new ArrayList<>();
                int pegCount = 0;
                for (int c = 0; c < BOARD_SIZE; c++) {
                    if (source[r][c] != ' ') validCols.add(c);
                    if (source[r][c] == '●') pegCount++;
                }
                for (int i = 0; i < pegCount; i++) {
                    int index = direction == SlideDirection.LEFT ? i : validCols.size() - 1 - i;
                    result[r][validCols.get(index)] = '●';
                }
            }
        } else {
            for (int c = 0; c < BOARD_SIZE; c++) {
                List<Integer> validRows = new ArrayList<>();
                int pegCount = 0;
                for (int r = 0; r < BOARD_SIZE; r++) {
                    if (source[r][c] != ' ') validRows.add(r);
                    if (source[r][c] == '●') pegCount++;
                }
                for (int i = 0; i < pegCount; i++) {
                    int index = direction == SlideDirection.UP ? i : validRows.size() - 1 - i;
                    result[validRows.get(index)][c] = '●';
                }
            }
        }

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                board.set(r, c, result[r][c]);
            }
        }
    }

    private void pushSnapshot() {
        history.push(new GameSnapshot(
                copyField(board.getField()),
                movesCount,
                credits,
                new EnumMap<>(charges),
                gameOver,
                startTimeMs,
                endTimeMs,
                statusMessage
        ));
    }

    private void restoreSnapshot(GameSnapshot snapshot) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                board.set(r, c, snapshot.field[r][c]);
            }
        }
        movesCount = snapshot.movesCount;
        credits = snapshot.credits;
        charges.clear();
        charges.putAll(snapshot.charges);
        gameOver = snapshot.gameOver;
        startTimeMs = snapshot.startTimeMs;
        endTimeMs = snapshot.endTimeMs;
        statusMessage = snapshot.statusMessage;
        activePowerup = null;
        selectedRow = -1;
        selectedCol = -1;
        selectionRow = -1;
        selectionCol = -1;
        clearValidTargets();
    }

    private char[][] copyField(char[][] source) {
        char[][] copy = new char[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    private void showToast(String message) {
        toastMessage = message;
        toastEndTime = System.currentTimeMillis() + 1700;
    }

    private void clearToast() {
        toastMessage = "";
        toastEndTime = 0;
    }

    private void updateValidTargets() {
        clearValidTargets();
        if (selectedRow < 0) return;
        List<Move> moves = collectMovesFor(selectedRow, selectedCol);
        for (Move move : moves) {
            validTargets[move.getToRow()][move.getToCol()] = true;
        }
    }

    private List<Move> collectMovesFor(int r, int c) {
        List<Move> moves = new ArrayList<>();
        int[][] deltas = {{0, 2}, {0, -2}, {2, 0}, {-2, 0}};
        for (int[] delta : deltas) {
            Move move = new Move(r, c, r + delta[0], c + delta[1]);
            if (validator.isValid(board, move)) {
                moves.add(move);
            }
        }
        return moves;
    }

    private boolean hasAnyValidTarget() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (validTargets[r][c]) return true;
            }
        }
        return false;
    }

    private void clearValidTargets() {
        validTargets = new boolean[BOARD_SIZE][BOARD_SIZE];
    }

    public boolean hasAnyValidMove() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (!hasPeg(r, c)) continue;
                if (!collectMovesFor(r, c).isEmpty()) return true;
            }
        }
        return false;
    }

    private void updateGameOver() {
        if (!hasAnyValidMove()) {
            gameOver = true;
            if (endTimeMs == 0) endTimeMs = System.currentTimeMillis();
            statusMessage = "Keine Züge mehr. Spiel beendet.";
            credits += 5;
        }
    }

    private enum SlideDirection {
        LEFT("LINKS"), RIGHT("RECHTS"), UP("OBEN"), DOWN("UNTEN");

        private final String label;

        SlideDirection(String label) {
            this.label = label;
        }

        private static SlideDirection random(Random random) {
            SlideDirection[] values = values();
            return values[random.nextInt(values.length)];
        }
    }

    private static final class GameSnapshot {
        private final char[][] field;
        private final int movesCount;
        private final int credits;
        private final Map<PowerupType, Integer> charges;
        private final boolean gameOver;
        private final long startTimeMs;
        private final long endTimeMs;
        private final String statusMessage;

        private GameSnapshot(char[][] field,
                             int movesCount,
                             int credits,
                             Map<PowerupType, Integer> charges,
                             boolean gameOver,
                             long startTimeMs,
                             long endTimeMs,
                             String statusMessage) {
            this.field = field;
            this.movesCount = movesCount;
            this.credits = credits;
            this.charges = charges;
            this.gameOver = gameOver;
            this.startTimeMs = startTimeMs;
            this.endTimeMs = endTimeMs;
            this.statusMessage = statusMessage;
        }
    }
}
