import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Leaderboard {

    private static final Path STORAGE_PATH = Paths.get("leaderboard.json");
    private final Map<String, PlayerAccount> players = new LinkedHashMap<>();
    private final List<Entry> entries = new ArrayList<>();

    public Leaderboard() {
        load();
    }

    public PlayerAccount ensurePlayer(String name) {
        String key = name.trim();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Spielername darf nicht leer sein.");
        }
        PlayerAccount account = players.get(key.toLowerCase());
        if (account == null) {
            account = new PlayerAccount(key);
            players.put(key.toLowerCase(), account);
            save();
        }
        return account;
    }

    public List<PlayerAccount> getPlayers() {
        return new ArrayList<>(players.values());
    }

    public void recordScore(String playerName, int score, Duration duration, int remainingPegs) {
        PlayerAccount player = ensurePlayer(playerName);
        entries.add(new Entry(player.getName(), score, duration, remainingPegs));
        entries.sort(Comparator
                .comparingInt(Entry::score).reversed()
                .thenComparing(Entry::duration)
                .thenComparingInt(Entry::remainingPegs));
        if (entries.size() > 10) {
            entries.subList(10, entries.size()).clear();
        }
        save();
    }

    public List<Entry> getEntries() {
        return new ArrayList<>(entries);
    }

    public void save() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"players\": [\n");
        int index = 0;
        for (PlayerAccount player : players.values()) {
            if (index > 0) {
                builder.append(",\n");
            }
            builder.append("    {\"name\":\"")
                    .append(escape(player.getName()))
                    .append("\",\"credits\":")
                    .append(player.getCredits())
                    .append(",\"hammer\":")
                    .append(player.getHammerCount())
                    .append(",\"slide\":")
                    .append(player.getSlideCount())
                    .append("}");
            index++;
        }
        builder.append("\n  ],\n");
        builder.append("  \"entries\": [\n");
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (i > 0) {
                builder.append(",\n");
            }
            builder.append("    {\"player\":\"")
                    .append(escape(entry.playerName()))
                    .append("\",\"score\":")
                    .append(entry.score())
                    .append(",\"durationSeconds\":")
                    .append(entry.duration().getSeconds())
                    .append(",\"remainingPegs\":")
                    .append(entry.remainingPegs())
                    .append("}");
        }
        builder.append("\n  ]\n");
        builder.append("}\n");
        try {
            Files.writeString(STORAGE_PATH, builder.toString());
        } catch (IOException e) {
            // Ignore persistence errors to keep game responsive.
        }
    }

    private void load() {
        if (!Files.exists(STORAGE_PATH)) {
            return;
        }
        try {
            String json = Files.readString(STORAGE_PATH);
            Pattern playerPattern = Pattern.compile("\\{\\s*\"name\"\\s*:\\s*\"(.*?)\"\\s*,\\s*\"credits\"\\s*:\\s*(\\d+)\\s*,\\s*\"hammer\"\\s*:\\s*(\\d+)(?:\\s*,\\s*\"slide\"\\s*:\\s*(\\d+))?\\s*\\}");
            Matcher playerMatcher = playerPattern.matcher(json);
            while (playerMatcher.find()) {
                String name = unescape(playerMatcher.group(1));
                int credits = Integer.parseInt(playerMatcher.group(2));
                int hammerCount = Integer.parseInt(playerMatcher.group(3));
                int slideCount = playerMatcher.group(4) == null ? 0 : Integer.parseInt(playerMatcher.group(4));
                players.put(name.toLowerCase(), new PlayerAccount(name, credits, hammerCount, slideCount));
            }
            Pattern entryPattern = Pattern.compile("\\{\\s*\"player\"\\s*:\\s*\"(.*?)\"\\s*,\\s*\"score\"\\s*:\\s*(\\d+)\\s*,\\s*\"durationSeconds\"\\s*:\\s*(\\d+)\\s*,\\s*\"remainingPegs\"\\s*:\\s*(\\d+)\\s*\\}");
            Matcher entryMatcher = entryPattern.matcher(json);
            while (entryMatcher.find()) {
                String playerName = unescape(entryMatcher.group(1));
                int score = Integer.parseInt(entryMatcher.group(2));
                long seconds = Long.parseLong(entryMatcher.group(3));
                int remaining = Integer.parseInt(entryMatcher.group(4));
                entries.add(new Entry(playerName, score, Duration.ofSeconds(seconds), remaining));
            }
        } catch (IOException e) {
            // Ignore corrupted files.
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    public void removePlayer(String name) {
        if (name == null) {
            return;
        }
        players.remove(name.toLowerCase());
        entries.removeIf(entry -> entry.playerName().equalsIgnoreCase(name));
        save();
    }

    public record Entry(String playerName, int score, Duration duration, int remainingPegs) {
    }
}
