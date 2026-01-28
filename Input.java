import java.util.Scanner;

public class Input {

    private Scanner scanner = new Scanner(System.in);

    public String getUserInput() {
        return scanner.nextLine();
    }

    public Move parse(String s) {
        if (s == null) return null;
        try {
            String[] parts = s.trim().toUpperCase().split("\\s+");
            
            if (parts.length != 2) return null;

            int[] from = parseCoordinate(parts[0]);
            int[] to = parseCoordinate(parts[1]);

            if (from == null || to == null) return null;

            return new Move(from[0], from[1], to[0], to[1]);

        } catch (Exception e) {
            return null;
        }
    }

    private int[] parseCoordinate(String p) {
        if (p.length() < 2) return null;
        
        char c1 = p.charAt(0);
        char c2 = p.charAt(1);
        
        int row = -1;
        int col = -1;

        if (Character.isLetter(c1) && Character.isDigit(c2)) {
            col = c1 - 'A';
            row = c2 - '1';
        }
        
        else if (Character.isDigit(c1) && Character.isLetter(c2)) {
            row = c1 - '1';
            col = c2 - 'A';
        } else {
            return null;
        }
        
        return new int[]{row, col};
    }
}
