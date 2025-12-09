public class Board {

    private char[][] field = {
            {' ', ' ', '●', '●', '●', ' ', ' '},
            {' ', ' ', '●', '●', '●', ' ', ' '},
            {'●', '●', '●', '●', '●', '●', '●'},
            {'●', '●', '●', '○', '●', '●', '●'},
            {'●', '●', '●', '●', '●', '●', '●'},
            {' ', ' ', '●', '●', '●', ' ', ' '},
            {' ', ' ', '●', '●', '●', ' ', ' '}
    };

    public char[][] getField() {
        return field;
    }

    public char get(int r, int c) {
        return field[r][c];
    }

    public void set(int r, int c, char value) {
        field[r][c] = value;
    }

    public int countPegs() {
        int count = 0;
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (field[r][c] == '●') {
                    count++;
                }
            }
        }
        return count;
    }

    public void print() {
        System.out.println("    A  B  C  D  E  F  G");
        for (int r = 0; r < 7; r++) {
            System.out.print((r + 1) + "  ");
            for (int c = 0; c < 7; c++) {
                System.out.print(" " + field[r][c] + " ");
            }
            System.out.println();
        }
    }
}
