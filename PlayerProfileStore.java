import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerProfileStore {
    private final Path storagePath;
    private final Map<String, Integer> creditsByPlayer = new HashMap<>();

    public PlayerProfileStore(Path storagePath) {
        this.storagePath = storagePath;
        load();
    }

    public void ensurePlayerProfile(String playerName) {
        String name = normalizePlayerName(playerName);
        if (!creditsByPlayer.containsKey(name)) {
            creditsByPlayer.put(name, 0);
            save();
        }
    }

    public int getCredits(String playerName) {
        return creditsByPlayer.getOrDefault(normalizePlayerName(playerName), 0);
    }

    public int addCredits(String playerName, int delta) {
        if (delta <= 0) {
            return getCredits(playerName);
        }
        String name = normalizePlayerName(playerName);
        int next = getCredits(name) + delta;
        creditsByPlayer.put(name, next);
        save();
        return next;
    }

    public boolean spendCredits(String playerName, int cost) {
        if (cost < 0) {
            return false;
        }
        String name = normalizePlayerName(playerName);
        int current = getCredits(name);
        if (current < cost) {
            return false;
        }
        creditsByPlayer.put(name, current - cost);
        save();
        return true;
    }

    public void load() {
        creditsByPlayer.clear();
        if (!Files.exists(storagePath)) {
            return;
        }
        try {
            String json = Files.readString(storagePath);
            Pattern playerPattern = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*\\{\\s*\\\"credits\\\"\\s*:\\s*(\\d+)\\s*}");
            Matcher matcher = playerPattern.matcher(json);
            while (matcher.find()) {
                String name = unescape(matcher.group(1));
                int credits = Integer.parseInt(matcher.group(2));
                creditsByPlayer.put(normalizePlayerName(name), Math.max(0, credits));
            }
        } catch (IOException e) {
            // Ignore corrupted files and continue with empty in-memory store.
        }
    }

    public void save() {
        StringBuilder out = new StringBuilder();
        out.append("{\n  \"players\": {\n");
        int index = 0;
        for (Map.Entry<String, Integer> entry : creditsByPlayer.entrySet()) {
            if (index++ > 0) {
                out.append(",\n");
            }
            out.append("    \"")
                    .append(escape(entry.getKey()))
                    .append("\": { \"credits\": ")
                    .append(Math.max(0, entry.getValue()))
                    .append(" }");
        }
        out.append("\n  }\n}\n");

        try {
            Files.writeString(storagePath, out.toString());
        } catch (IOException e) {
            // Ignore save errors to avoid crashing gameplay.
        }
    }

    private String normalizePlayerName(String playerName) {
        String normalized = playerName == null ? "" : playerName.trim();
        return normalized.isEmpty() ? "Player" : normalized;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
