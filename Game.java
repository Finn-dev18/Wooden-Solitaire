public class Game {

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            WoodenSolitaireGUI ui = new WoodenSolitaireGUI();
            ui.setVisible(true);
        });
    }
}
