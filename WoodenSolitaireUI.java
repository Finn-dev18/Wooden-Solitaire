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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class WoodenSolitaireUI extends JFrame {

    private static final Color COLOR_BG = new Color(248, 245, 240);
    private static final Color COLOR_PANEL = new Color(255, 255, 255);
    private static final Color COLOR_ACCENT = new Color(140, 94, 60);
    private static final Color COLOR_PEG = new Color(60, 60, 60);
    private static final Color COLOR_EMPTY = new Color(230, 225, 220);

    private final Board board = new Board();
    private final MoveValidator validator = new MoveValidator();
    private final GameStatus status = new GameStatus();
    private final Input input = new Input();
    private final Leaderboard leaderboard = new Leaderboard();

    private PlayerAccount currentPlayer;
    private Instant startTime;
    private int moveCount;

    private final JLabel playerValue = new JLabel("-");
    private final JLabel timeValue = new JLabel("00:00");
    private final JLabel movesValue = new JLabel("0");
    private final JLabel pegsValue = new JLabel("0");
    private final JLabel scoreValue = new JLabel("0");
    private final JLabel statusValue = new JLabel("Bitte Spieler wählen und Spiel starten.");
    private final JButton newGameButton = new JButton("Neues Spiel");
    private final JButton submitButton = new JButton("Zug ausführen");
    private final JTextField moveField = new JTextField();
    private final JButton[][] cells = new JButton[7][7];

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
        setSize(920, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(16, 16));
        getContentPane().setBackground(COLOR_BG);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        updateBoard();
        updateStats();
        setGameControlsEnabled(false);
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

        center.add(buildBoardPanel(), BorderLayout.CENTER);
        center.add(buildMovePanel(), BorderLayout.SOUTH);
        return center;
    }

    private JPanel buildBoardPanel() {
        JPanel boardPanel = new JPanel(new GridLayout(7, 7, 6, 6));
        boardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ACCENT, 2),
                new EmptyBorder(16, 16, 16, 16)));
        boardPanel.setBackground(COLOR_PANEL);

        Font cellFont = new Font("SansSerif", Font.BOLD, 22);
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                JButton cell = new JButton();
                cell.setFont(cellFont);
                cell.setFocusable(false);
                cell.setBackground(COLOR_EMPTY);
                cell.setPreferredSize(new Dimension(64, 64));
                cell.setBorder(BorderFactory.createLineBorder(new Color(210, 200, 190)));
                cell.setEnabled(false);
                cells[r][c] = cell;
                boardPanel.add(cell);
            }
        }

        return boardPanel;
    }

    private JPanel buildMovePanel() {
        JPanel movePanel = new JPanel();
        movePanel.setBackground(COLOR_PANEL);
        movePanel.setBorder(new EmptyBorder(12, 16, 12, 16));
        movePanel.setLayout(new BoxLayout(movePanel, BoxLayout.X_AXIS));

        JLabel moveLabel = new JLabel("Zug (z.B. E4 E6):");
        moveLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        moveField.setMaximumSize(new Dimension(180, 32));
        moveField.setFont(new Font("SansSerif", Font.PLAIN, 14));

        submitButton.addActionListener(e -> handleMove());
        submitButton.setBackground(COLOR_ACCENT);
        submitButton.setForeground(Color.WHITE);
        submitButton.setFocusPainted(false);

        movePanel.add(moveLabel);
        movePanel.add(Box.createHorizontalStrut(12));
        movePanel.add(moveField);
        movePanel.add(Box.createHorizontalStrut(12));
        movePanel.add(submitButton);
        movePanel.add(Box.createHorizontalGlue());

        return movePanel;
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
        int fr = move.getFromRow();
        int fc = move.getFromCol();
        int tr = move.getToRow();
        int tc = move.getToCol();

        board.set(fr, fc, '○');
        board.set((fr + tr) / 2, (fc + tc) / 2, '○');
        board.set(tr, tc, '●');
        moveCount++;
        moveField.setText("");
        updateBoard();

        if (!status.hasMovesLeft(board, validator)) {
            finishGame();
        } else {
            updateStats();
        }
    }

    private void finishGame() {
        updateStats();
        int remaining = board.countPegs();
        Duration duration = getElapsed();
        int score = ScoreCalculator.calculate(duration, remaining, moveCount);

        statusValue.setText("Spiel vorbei! Ergebnis gespeichert.");
        scoreValue.setText(String.valueOf(score));

        if (currentPlayer != null) {
            leaderboard.recordScore(currentPlayer.getName(), score, duration, remaining);
            refreshLeaderboard();
        }
    }

    private void updateBoard() {
        char[][] field = board.getField();
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
            }
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
    }
}
