import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class WoodenSolitaireUI extends JFrame {

    private static final Color COLOR_BG = new Color(248, 245, 240);
    private static final Color COLOR_PANEL = new Color(255, 255, 255);
    private static final Color COLOR_ACCENT = new Color(140, 94, 60);
    private static final Color COLOR_PEG = new Color(60, 60, 60);
    private static final Color COLOR_EMPTY = new Color(230, 225, 220);
    private static final Color COLOR_HIGHLIGHT = new Color(255, 236, 140);
    private static final Color COLOR_VALID_TARGET = new Color(184, 225, 181);

    private final Board board = new Board();
    private final MoveValidator validator = new MoveValidator();
    private final GameStatus status = new GameStatus();
    private final Input input = new Input();
    private final Leaderboard leaderboard = new Leaderboard();

    private PlayerAccount currentPlayer;
    private Instant startTime;
    private int moveCount;
    private int selectedRow = -1;
    private int selectedCol = -1;

    private final JLabel playerValue = new JLabel("-");
    private final JLabel timeValue = new JLabel("00:00");
    private final JLabel movesValue = new JLabel("0");
    private final JLabel pegsValue = new JLabel("0");
    private final JLabel scoreValue = new JLabel("0");
    private final JLabel statusValue = new JLabel("Bitte Spieler wählen und Spiel starten.");
    private final JButton newGameButton = new JButton("Neues Spiel");
    private final JButton submitButton = new JButton("Zug ausführen");
    private final JTextField moveField = new JTextField();
    private final JLabel creditsValue = new JLabel("0");
    private final JLabel hammerCountValue = new JLabel("0");
    private final JButton buyHammerButton = new JButton("Kaufen");
    private final JButton useHammerButton = new JButton("Nutzen");
    private final JButton[][] cells = new JButton[7][7];
    private final Timer timer = new Timer(1000, event -> updateStats());
    private Timer animationTimer;
    private boolean animationInProgress;
    private boolean hammerArmed;

    private static final int HAMMER_COST = 8;

    private final DefaultTableModel leaderboardModel = new DefaultTableModel(
            new Object[]{"Spieler", "Punkte", "Zeit", "Restkugeln"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public WoodenSolitaireUI() {
        super("Wooden Solitaire");
        configureLookAndFeel();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1920, 1080);
        setMinimumSize(new Dimension(1920, 1080));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(16, 16));
        getContentPane().setBackground(COLOR_BG);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        updateBoard();
        updateStats();
        setGameControlsEnabled(false);
        timer.start();
    }

    private void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Ignore and continue with default look and feel.
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(16, 16, 8, 16));
        header.setBackground(COLOR_BG);

        JLabel title = new JLabel("Wooden Solitaire");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(COLOR_ACCENT);
        header.add(title, BorderLayout.WEST);

        JLabel subtitle = new JLabel("Zeitbasiertes Punktesystem mit lokalen Spieleraccounts");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(new Color(90, 90, 90));
        header.add(subtitle, BorderLayout.SOUTH);

        return header;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setBackground(COLOR_BG);
        center.setBorder(new EmptyBorder(0, 16, 16, 0));

        center.add(buildPowerupsPanel(), BorderLayout.WEST);
        center.add(buildBoardPanel(), BorderLayout.CENTER);
        return center;
    }

    private JPanel buildBoardPanel() {
        JPanel boardPanel = new JPanel(new GridLayout(7, 7, 10, 10));
        boardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ACCENT, 2),
                new EmptyBorder(24, 24, 24, 24)));
        boardPanel.setBackground(COLOR_PANEL);

        Font cellFont = new Font("SansSerif", Font.BOLD, 26);
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                final int row = r;
                final int col = c;
                JButton cell = new JButton();
                cell.setFont(cellFont);
                cell.setFocusable(false);
                cell.setBackground(COLOR_EMPTY);
                cell.setPreferredSize(new Dimension(90, 90));
                cell.setBorder(BorderFactory.createLineBorder(new Color(210, 200, 190), 2));
                cell.addActionListener(e -> handleCellClick(row, col));
                cells[r][c] = cell;
                boardPanel.add(cell);
            }
        }

        return boardPanel;
    }

    private JPanel buildPowerupsPanel() {
        JPanel powerups = new JPanel();
        powerups.setLayout(new BoxLayout(powerups, BoxLayout.Y_AXIS));
        powerups.setBackground(COLOR_PANEL);
        powerups.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Powerups");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setAlignmentX(LEFT_ALIGNMENT);
        powerups.add(title);
        powerups.add(Box.createVerticalStrut(12));

        JPanel creditsRow = new JPanel(new BorderLayout());
        creditsRow.setBackground(COLOR_PANEL);
        JLabel creditsLabel = new JLabel("Credits:");
        creditsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        creditsRow.add(creditsLabel, BorderLayout.WEST);
        creditsValue.setFont(new Font("SansSerif", Font.BOLD, 12));
        creditsRow.add(creditsValue, BorderLayout.EAST);
        creditsRow.setAlignmentX(LEFT_ALIGNMENT);
        powerups.add(creditsRow);
        powerups.add(Box.createVerticalStrut(16));

        JLabel hammerTitle = new JLabel("Hammer");
        hammerTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        hammerTitle.setAlignmentX(LEFT_ALIGNMENT);
        powerups.add(hammerTitle);

        JLabel hammerDescription = new JLabel("Zerstört eine Kugel auf dem Feld.");
        hammerDescription.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hammerDescription.setForeground(new Color(90, 90, 90));
        hammerDescription.setAlignmentX(LEFT_ALIGNMENT);
        powerups.add(hammerDescription);
        powerups.add(Box.createVerticalStrut(6));

        JPanel hammerCountRow = new JPanel(new BorderLayout());
        hammerCountRow.setBackground(COLOR_PANEL);
        JLabel countLabel = new JLabel("Verfügbar:");
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hammerCountRow.add(countLabel, BorderLayout.WEST);
        hammerCountValue.setFont(new Font("SansSerif", Font.BOLD, 12));
        hammerCountRow.add(hammerCountValue, BorderLayout.EAST);
        hammerCountRow.setAlignmentX(LEFT_ALIGNMENT);
        powerups.add(hammerCountRow);

        JLabel costLabel = new JLabel("Kosten: " + HAMMER_COST + " Credits");
        costLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        costLabel.setForeground(new Color(90, 90, 90));
        costLabel.setAlignmentX(LEFT_ALIGNMENT);
        powerups.add(Box.createVerticalStrut(6));
        powerups.add(costLabel);

        buyHammerButton.setAlignmentX(LEFT_ALIGNMENT);
        buyHammerButton.addActionListener(e -> buyHammer());
        useHammerButton.setAlignmentX(LEFT_ALIGNMENT);
        useHammerButton.addActionListener(e -> armHammer());
        powerups.add(Box.createVerticalStrut(8));
        powerups.add(buyHammerButton);
        powerups.add(Box.createVerticalStrut(6));
        powerups.add(useHammerButton);

        return powerups;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(COLOR_BG);
        sidebar.setBorder(new EmptyBorder(0, 0, 16, 16));

        sidebar.add(buildAccountPanel());
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(buildStatsPanel());
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(buildLeaderboardPanel());

        return sidebar;
    }

    private JPanel buildAccountPanel() {
        JPanel account = new JPanel();
        account.setBackground(COLOR_PANEL);
        account.setBorder(new EmptyBorder(12, 16, 12, 16));
        account.setLayout(new BoxLayout(account, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Spieleraccount");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setAlignmentX(LEFT_ALIGNMENT);
        account.add(title);
        account.add(Box.createVerticalStrut(8));

        JComboBox<PlayerAccount> playerSelect = new JComboBox<>(leaderboard.getPlayers().toArray(new PlayerAccount[0]));
        playerSelect.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        playerSelect.setFont(new Font("SansSerif", Font.PLAIN, 14));
        playerSelect.addActionListener(e -> selectPlayer((PlayerAccount) playerSelect.getSelectedItem()));
        account.add(playerSelect);

        JButton createButton = new JButton("Neuen Spieler anlegen");
        createButton.setAlignmentX(LEFT_ALIGNMENT);
        createButton.addActionListener(e -> createPlayer(playerSelect));
        account.add(Box.createVerticalStrut(8));
        account.add(createButton);

        JPanel current = new JPanel(new BorderLayout());
        current.setBackground(COLOR_PANEL);
        JLabel currentLabel = new JLabel("Aktiver Spieler:");
        currentLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        current.add(currentLabel, BorderLayout.WEST);
        playerValue.setFont(new Font("SansSerif", Font.BOLD, 12));
        current.add(playerValue, BorderLayout.EAST);
        current.setAlignmentX(LEFT_ALIGNMENT);
        account.add(Box.createVerticalStrut(10));
        account.add(current);

        newGameButton.setAlignmentX(LEFT_ALIGNMENT);
        newGameButton.addActionListener(e -> startNewGame());
        account.add(Box.createVerticalStrut(8));
        account.add(newGameButton);

        return account;
    }

    private JPanel buildStatsPanel() {
        JPanel stats = new JPanel();
        stats.setBackground(COLOR_PANEL);
        stats.setBorder(new EmptyBorder(12, 16, 12, 16));
        stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Spielstatus");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setAlignmentX(LEFT_ALIGNMENT);
        stats.add(title);
        stats.add(Box.createVerticalStrut(8));

        stats.add(createStatRow("Zeit", timeValue));
        stats.add(createStatRow("Züge", movesValue));
        stats.add(createStatRow("Restkugeln", pegsValue));
        stats.add(createStatRow("Punkte", scoreValue));
        stats.add(Box.createVerticalStrut(8));
        stats.add(statusValue);

        statusValue.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusValue.setForeground(new Color(80, 80, 80));
        statusValue.setAlignmentX(LEFT_ALIGNMENT);

        return stats;
    }

    private JPanel createStatRow(String label, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(COLOR_PANEL);
        JLabel labelComponent = new JLabel(label + ":");
        labelComponent.setFont(new Font("SansSerif", Font.PLAIN, 12));
        row.add(labelComponent, BorderLayout.WEST);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        row.add(valueLabel, BorderLayout.EAST);
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private JPanel buildLeaderboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_PANEL);
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Lokales Leaderboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(title, BorderLayout.NORTH);

        JTable table = new JTable(leaderboardModel);
        table.setRowHeight(22);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        refreshLeaderboard();
        return panel;
    }

    private void selectPlayer(PlayerAccount player) {
        currentPlayer = player;
        playerValue.setText(player == null ? "-" : player.getName());
        updatePowerupPanel();
        setGameControlsEnabled(player != null);
    }

    private void createPlayer(JComboBox<PlayerAccount> selector) {
        JTextField nameField = new JTextField();
        Object[] message = {"Name:", nameField};
        int result = JOptionPane.showConfirmDialog(this, message, "Neuen Spieler anlegen", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte einen Namen eingeben.", "Eingabe prüfen", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PlayerAccount account = leaderboard.ensurePlayer(name);
        selector.removeAllItems();
        List<PlayerAccount> players = leaderboard.getPlayers();
        for (PlayerAccount player : players) {
            selector.addItem(player);
        }
        selector.setSelectedItem(account);
    }

    private void startNewGame() {
        board.reset();
        moveCount = 0;
        startTime = Instant.now();
        selectedRow = -1;
        selectedCol = -1;
        hammerArmed = false;
        statusValue.setText("Spiel gestartet. Viel Erfolg!");
        moveField.setText("");
        updateBoard();
        updateStats();
    }

    private void handleMove() {
        if (currentPlayer == null) {
            statusValue.setText("Bitte zuerst einen Spieler wählen.");
            return;
        }
        if (startTime == null) {
            startTime = Instant.now();
        }
        String userIn = moveField.getText();
        Move move = input.parse(userIn);
        if (move == null) {
            statusValue.setText("Ungültiges Format. Beispiel: E4 E6");
            return;
        }
        if (!validator.isValid(board, move)) {
            statusValue.setText("Ungültiger Zug. Bitte erneut versuchen.");
            return;
        }
        selectedRow = -1;
        selectedCol = -1;
        applyMove(move);
        moveField.setText("");
        updateBoard();
    }

    private void finishGame() {
        updateStats();
        int remaining = board.countPegs();
        Duration duration = getElapsed();
        int score = ScoreCalculator.calculate(duration, remaining, moveCount);

        statusValue.setText("Spiel vorbei! Ergebnis gespeichert.");
        scoreValue.setText(String.valueOf(score));

        if (currentPlayer != null) {
            int creditsEarned = ScoreCalculator.calculateCredits(score);
            if (creditsEarned > 0) {
                currentPlayer.addCredits(creditsEarned);
            }
            leaderboard.recordScore(currentPlayer.getName(), score, duration, remaining);
            leaderboard.save();
            refreshLeaderboard();
            updatePowerupPanel();
            JOptionPane.showMessageDialog(this,
                    "Spiel beendet!\nPunkte: " + score +
                            "\nRestkugeln: " + remaining +
                            "\nCredits erhalten: " + creditsEarned,
                    "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateBoard() {
        renderField(board.getField(), true);
    }

    private void renderField(char[][] field, boolean showTargets) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                JButton cell = cells[r][c];
                char value = field[r][c];
                if (value == '●') {
                    cell.setText("●");
                    cell.setForeground(COLOR_PEG);
                    cell.setBackground(new Color(238, 232, 226));
                } else if (value == '○') {
                    cell.setText("○");
                    cell.setForeground(new Color(160, 150, 140));
                    cell.setBackground(COLOR_EMPTY);
                } else {
                    cell.setText("");
                    cell.setBackground(COLOR_PANEL);
                }
                if (r == selectedRow && c == selectedCol) {
                    cell.setBackground(COLOR_HIGHLIGHT);
                }
            }
        }
        if (showTargets) {
            highlightValidTargets();
        }
    }

    private void updateStats() {
        pegsValue.setText(String.valueOf(board.countPegs()));
        movesValue.setText(String.valueOf(moveCount));
        Duration elapsed = getElapsed();
        timeValue.setText(formatDuration(elapsed));
        scoreValue.setText(String.valueOf(ScoreCalculator.calculate(elapsed, board.countPegs(), moveCount)));
    }

    private Duration getElapsed() {
        if (startTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(startTime, Instant.now());
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    private void refreshLeaderboard() {
        leaderboardModel.setRowCount(0);
        for (Leaderboard.Entry entry : leaderboard.getEntries()) {
            leaderboardModel.addRow(new Object[]{
                    entry.playerName(),
                    entry.score(),
                    formatDuration(entry.duration()),
                    entry.remainingPegs()
            });
        }
    }

    private void setGameControlsEnabled(boolean enabled) {
        newGameButton.setEnabled(enabled);
        submitButton.setEnabled(enabled);
        moveField.setEnabled(enabled);
        buyHammerButton.setEnabled(enabled);
        useHammerButton.setEnabled(enabled);
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                cells[r][c].setEnabled(enabled);
            }
        }
        if (!enabled) {
            selectedRow = -1;
            selectedCol = -1;
            updateBoard();
        }
    }

    private void handleCellClick(int row, int col) {
        if (animationInProgress) {
            return;
        }
        if (currentPlayer == null) {
            statusValue.setText("Bitte zuerst einen Spieler wählen.");
            return;
        }
        if (hammerArmed) {
            useHammerOnCell(row, col);
            return;
        }
        if (startTime == null) {
            startTime = Instant.now();
        }
        char value = board.get(row, col);
        if (selectedRow == -1 && value == '●') {
            selectedRow = row;
            selectedCol = col;
            statusValue.setText("Kugel ausgewählt. Zielkugel anklicken.");
            updateBoard();
            return;
        }
        if (selectedRow != -1) {
            Move move = new Move(selectedRow, selectedCol, row, col);
            if (validator.isValid(board, move)) {
                applyMove(move);
                selectedRow = -1;
                selectedCol = -1;
                updateBoard();
                return;
            }
            if (value == '●') {
                selectedRow = row;
                selectedCol = col;
                statusValue.setText("Kugel gewechselt. Ziel wählen.");
                updateBoard();
                return;
            }
            statusValue.setText("Ungültiger Zug. Ziel erneut wählen.");
            updateBoard();
        }
    }

    private void applyMove(Move move) {
        int fr = move.getFromRow();
        int fc = move.getFromCol();
        int tr = move.getToRow();
        int tc = move.getToCol();

        animateMove(move);
    }

    private void highlightValidTargets() {
        if (selectedRow == -1) {
            return;
        }
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                Move move = new Move(selectedRow, selectedCol, r, c);
                if (validator.isValid(board, move)) {
                    cells[r][c].setBackground(COLOR_VALID_TARGET);
                }
            }
        }
    }

    private void animateMove(Move move) {
        if (animationInProgress) {
            return;
        }
        animationInProgress = true;
        setBoardInteractionEnabled(false);
        int fr = move.getFromRow();
        int fc = move.getFromCol();
        int tr = move.getToRow();
        int tc = move.getToCol();
        int mr = (fr + tr) / 2;
        int mc = (fc + tc) / 2;
        char[][] base = copyField(board.getField());
        int[] steps = {0, 1, 2};
        final int[] stepIndex = {0};

        ActionListener listener = event -> {
            char[][] frame = copyField(base);
            frame[fr][fc] = '○';
            frame[mr][mc] = '○';
            frame[tr][tc] = '○';
            if (steps[stepIndex[0]] == 0) {
                frame[fr][fc] = '●';
            } else if (steps[stepIndex[0]] == 1) {
                frame[mr][mc] = '●';
            } else {
                frame[tr][tc] = '●';
            }
            renderField(frame, false);
            stepIndex[0]++;
            if (stepIndex[0] >= steps.length) {
                animationTimer.stop();
                finalizeMove(move);
                animationInProgress = false;
                setBoardInteractionEnabled(true);
                updateBoard();
            }
        };

        animationTimer = new Timer(120, listener);
        animationTimer.start();
    }

    private void finalizeMove(Move move) {
        int fr = move.getFromRow();
        int fc = move.getFromCol();
        int tr = move.getToRow();
        int tc = move.getToCol();

        board.set(fr, fc, '○');
        board.set((fr + tr) / 2, (fc + tc) / 2, '○');
        board.set(tr, tc, '●');
        moveCount++;

        if (!status.hasMovesLeft(board, validator)) {
            finishGame();
        } else {
            updateStats();
        }
    }

    private char[][] copyField(char[][] source) {
        char[][] copy = new char[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    private void setBoardInteractionEnabled(boolean enabled) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                cells[r][c].setEnabled(enabled);
            }
        }
        submitButton.setEnabled(enabled);
        moveField.setEnabled(enabled);
        newGameButton.setEnabled(enabled);
        buyHammerButton.setEnabled(enabled);
        useHammerButton.setEnabled(enabled);
    }

    private void updatePowerupPanel() {
        if (currentPlayer == null) {
            creditsValue.setText("0");
            hammerCountValue.setText("0");
            buyHammerButton.setEnabled(false);
            useHammerButton.setEnabled(false);
            return;
        }
        creditsValue.setText(String.valueOf(currentPlayer.getCredits()));
        hammerCountValue.setText(String.valueOf(currentPlayer.getHammerCount()));
        buyHammerButton.setEnabled(true);
        useHammerButton.setEnabled(currentPlayer.getHammerCount() > 0);
    }

    private void buyHammer() {
        if (currentPlayer == null) {
            statusValue.setText("Bitte zuerst einen Spieler wählen.");
            return;
        }
        if (!currentPlayer.spendCredits(HAMMER_COST)) {
            statusValue.setText("Nicht genug Credits für den Hammer.");
            updatePowerupPanel();
            return;
        }
        currentPlayer.addHammer(1);
        leaderboard.save();
        statusValue.setText("Hammer gekauft.");
        updatePowerupPanel();
    }

    private void armHammer() {
        if (currentPlayer == null) {
            statusValue.setText("Bitte zuerst einen Spieler wählen.");
            return;
        }
        if (currentPlayer.getHammerCount() <= 0) {
            statusValue.setText("Kein Hammer verfügbar.");
            return;
        }
        hammerArmed = true;
        statusValue.setText("Hammer aktiv: Wähle eine Kugel zum Entfernen.");
    }

    private void useHammerOnCell(int row, int col) {
        if (currentPlayer == null) {
            hammerArmed = false;
            return;
        }
        if (board.get(row, col) != '●') {
            statusValue.setText("Hier ist keine Kugel. Hammer bleibt aktiv.");
            return;
        }
        if (!currentPlayer.useHammer()) {
            hammerArmed = false;
            statusValue.setText("Kein Hammer mehr verfügbar.");
            updatePowerupPanel();
            return;
        }
        hammerArmed = false;
        board.set(row, col, '○');
        leaderboard.save();
        updatePowerupPanel();
        updateBoard();
        updateStats();
        if (!status.hasMovesLeft(board, validator)) {
            finishGame();
        } else {
            statusValue.setText("Hammer benutzt.");
        }
    }
}
