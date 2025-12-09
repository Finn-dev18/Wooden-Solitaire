public class Game {

    public static void main(String[] args) {

        Board board = new Board();
        MoveValidator validator = new MoveValidator();
        GameStatus status = new GameStatus();
        Input input = new Input();
        int moveCount = 0;

        while (true) {

            board.print();
            System.out.printf("Züge: %d | Übrige Bälle: %d%n", moveCount, board.countPegs());

            String userIn = input.getUserInput();

            if (userIn.equalsIgnoreCase("exit")) {
                System.out.println("Spiel beendet.");
                System.out.printf("Endstand - Züge: %d | Übrige Bälle: %d%n", moveCount, board.countPegs());
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

            int fr = move.getFromRow(), fc = move.getFromCol();
            int tr = move.getToRow(), tc = move.getToCol();

            board.set(fr, fc, '○')
            board.set((fr + tr) / 2, (fc + tc) / 2, '○');
            board.set(tr, tc, '●');
            moveCount++;

            if (!status.hasMovesLeft(board, validator)) {
                board.print();
                System.out.printf("Züge: %d | Übrige Bälle: %d%n", moveCount, board.countPegs());
                System.out.println("Keine Züge mehr möglich! Spiel vorbei.");
                System.out.printf("Endstand - Züge: %d | Übrige Bälle: %d%n", moveCount, board.countPegs());
                break;
            }
        }
    }
}
