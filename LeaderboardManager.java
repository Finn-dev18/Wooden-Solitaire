import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeaderboardManager {
    private static final Path STORAGE_PATH = Paths.get("leaderboard.json");
    private final List<Entry> entries = new ArrayList<>();

    public LeaderboardManager() {
        load();
    }

    public List<Entry> getTop10() {
        return new ArrayList<>(entries);
    }

    public boolean isTop10Candidate(Entry entry) {
        List<Entry> combined = new ArrayList<>(entries);
        combined.add(entry);
        combined.sort(entryComparator());
        return combined.indexOf(entry) < 10;
    }

    public void addEntry(Entry entry) {
        entries.add(entry);
        entries.sort(entryComparator());
        if (entries.size() > 10) {
            entries.subList(10, entries.size()).clear();
        }
        save();
    }

    private Comparator<Entry> entryComparator() {
        return Comparator
                .comparingInt(Entry::score).reversed()
                .thenComparingLong(Entry::durationSeconds)
                .thenComparingLong(Entry::timestamp);
    }

    private void load() {
        if (!Files.exists(STORAGE_PATH)) {
            return;
        }
        try {
            String json = Files.readString(STORAGE_PATH);
            Pattern pattern = Pattern.compile("\\{\\s*\\\"name\\\"\\s*:\\s*\\\"(.*?)\\\"\\s*,\\s*\\\"score\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"pegsLeft\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"durationSeconds\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"timestamp\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"powerupsEnabled\\\"\\s*:\\s*(true|false)\\s*\\}");
            Matcher matcher = pattern.matcher(json);
            while (matcher.find()) {
                String name = unescape(matcher.group(1));
                int score = Integer.parseInt(matcher.group(2));
                int pegs = Integer.parseInt(matcher.group(3));
                long durationSeconds = Long.parseLong(matcher.group(4));
                long timestamp = Long.parseLong(matcher.group(5));
                boolean powerupsEnabled = Boolean.parseBoolean(matcher.group(6));
                entries.add(new Entry(name, score, pegs, durationSeconds, timestamp, powerupsEnabled));
            }
            entries.sort(entryComparator());
            if (entries.size() > 10) {
                entries.subList(10, entries.size()).clear();
            }
        } catch (IOException e) {
            // Ignore corrupted files.
        }
    }

    private void save() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n  \"entries\": [\n");
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (i > 0) {
                builder.append(",\n");
            }
            builder.append("    {\"name\":\"")
                    .append(escape(entry.name()))
                    .append("\",\"score\":")
                    .append(entry.score())
                    .append(",\"pegsLeft\":")
                    .append(entry.pegsLeft())
                    .append(",\"durationSeconds\":")
                    .append(entry.durationSeconds())
                    .append(",\"timestamp\":")
                    .append(entry.timestamp())
                    .append(",\"powerupsEnabled\":")
                    .append(entry.powerupsEnabled())
                    .append("}");
        }
        builder.append("\n  ]\n}\n");
        try {
            Files.writeString(STORAGE_PATH, builder.toString());
        } catch (IOException e) {
            // Ignore persistence errors.
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    public record Entry(String name, int score, int pegsLeft, long durationSeconds, long timestamp, boolean powerupsEnabled) {
    }
}
