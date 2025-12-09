public class GameStatus {

    public boolean hasMovesLeft(Board board, MoveValidator validator) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (board.get(r, c) == '●') {
                    if (check(board, validator, r, c, r, c + 2)) return true;
                    if (check(board, validator, r, c, r, c - 2)) return true;
                    if (check(board, validator, r, c, r + 2, c)) return true;
                    if (check(board, validator, r, c, r - 2, c)) return true;
                }
            }
        }
        return false;
    }

    private boolean check(Board board, MoveValidator validator,
                          int fr, int fc, int tr, int tc) {
        return validator.isValid(board, new Move(fr, fc, tr, tc));
    }
}
