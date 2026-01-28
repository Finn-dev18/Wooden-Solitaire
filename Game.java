public class Game {

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            WoodenSolitaireUI ui = new WoodenSolitaireUI();
            ui.setVisible(true);
        });
    }
}
