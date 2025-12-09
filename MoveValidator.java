public class MoveValidator {

    public boolean isValid(Board board, Move move) {
        int fr = move.getFromRow();
        int fc = move.getFromCol();
        int tr = move.getToRow();
        int tc = move.getToCol();

        // Grenzen checken
        if (!inBounds(fr, fc) || !inBounds(tr, tc))
            return false;

        // Start muss ein Peg sein
        if (board.get(fr, fc) != '●') return false;

        // Ziel muss leer sein
        if (board.get(tr, tc) != '○') return false;

        // Bewegung muss 2 Felder sein
        if (Math.abs(fr - tr) == 2 && fc == tc) {
            return board.get((fr + tr) / 2, fc) == '●'; // vertikal
        }
        if (Math.abs(fc - tc) == 2 && fr == tr) {
            return board.get(fr, (fc + tc) / 2) == '●'; // horizontal
        }

        return false;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < 7 && c >= 0 && c < 7;
    }
}
