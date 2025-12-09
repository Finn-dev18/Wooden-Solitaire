public class Game {

    public static void main(String[] args) {

        Board board = new Board();
        MoveValidator validator = new MoveValidator();
        GameStatus status = new GameStatus();
        Input input = new Input();

        System.out.println("=== PEG SOLITAIRE ===");

        while (true) {

            board.print();

            String userIn = input.getUserInput();

            if (userIn.equalsIgnoreCase("exit")) {
                System.out.println("Spiel beendet.");
                break;
            }

            Move move = input.parse(userIn);

            if (move == null) {
                System.out.println("Ungültiges Format!");
                continue;
            }

            if (!validator.isValid(board, move)) {
                System.out.println("Ungültiger Zug!");
                continue;
            }

            // gültig → ausführen
            int fr = move.getFromRow(), fc = move.getFromCol();
            int tr = move.getToRow(), tc = move.getToCol();

            board.set(fr, fc, '○'); // start wird leer
            board.set((fr + tr) / 2, (fc + tc) / 2, '○'); // übersprungener Peg weg
            board.set(tr, tc, '●'); // Peg an Ziel

            // Prüfen ob noch Züge möglich sind
            if (!status.hasMovesLeft(board, validator)) {
                board.print();
                System.out.println("Keine Züge mehr möglich! Spiel vorbei.");
                break;
            }
        }
    }
}
