public class MoveValidator {

    public boolean isValid(Board board, Move move) {
        int fr = move.getFromRow();
        int fc = move.getFromCol();
        int tr = move.getToRow();
        int tc = move.getToCol();

        if (!inBounds(fr, fc) || !inBounds(tr, tc))
            return false;
  
        if (board.get(fr, fc) != '●') return false;
        if (board.get(tr, tc) != '○') return false;

        if (Math.abs(fr - tr) == 2 && fc == tc) {
            return board.get((fr + tr) / 2, fc) == '●';
        }
        if (Math.abs(fc - tc) == 2 && fr == tr) {
            return board.get(fr, (fc + tc) / 2) == '●';
        }
        return false;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < 7 && c >= 0 && c < 7;
    }
}
