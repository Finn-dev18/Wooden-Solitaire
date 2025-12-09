import java.util.Scanner;

public class Input {

    private Scanner scanner = new Scanner(System.in);

    public String getUserInput() {
        System.out.print("Zug eingeben (z.B. E4 E6 oder 'exit'): ");
        return scanner.nextLine();
    }

    public Move parse(String s) {
        try {
            String[] parts = s.split(" ");
            String p1 = parts[0];
            String p2 = parts[1];

            int fromCol = p1.charAt(0) - 'A';
            int fromRow = p1.charAt(1) - '1';
            int toCol   = p2.charAt(0) - 'A';
            int toRow   = p2.charAt(1) - '1';

            return new Move(fromRow, fromCol, toRow, toCol);

        } catch (Exception e) {
            return null;
        }
    }
}
