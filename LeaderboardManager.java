import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeaderboardManager {
    private final Path storagePath;
    private final List<Entry> entries = new ArrayList<>();

    public LeaderboardManager(Path storagePath) {
        this.storagePath = storagePath;
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
        String normalizedName = normalizeName(entry.name());
        Entry bestExisting = null;
        for (Entry existing : entries) {
            if (normalizeName(existing.name()).equals(normalizedName)) {
                if (bestExisting == null || entryComparator().compare(existing, bestExisting) < 0) {
                    bestExisting = existing;
                }
            }
        }

        Entry normalizedEntry = new Entry(normalizedName, entry.score(), entry.pegsLeft(), entry.durationSeconds(), entry.timestamp(), entry.powerupsEnabled());
        if (bestExisting != null) {
            if (entryComparator().compare(normalizedEntry, bestExisting) < 0) {
                entries.removeIf(existing -> normalizeName(existing.name()).equals(normalizedName));
                entries.add(normalizedEntry);
            }
        } else {
            entries.add(normalizedEntry);
        }

        sortTrimTop10();
        save();
    }

    private Comparator<Entry> entryComparator() {
        return Comparator
                .comparingInt(Entry::score).reversed()
                .thenComparingLong(Entry::durationSeconds)
                .thenComparingLong(Entry::timestamp);
    }

    private void load() {
        if (!Files.exists(storagePath)) {
            return;
        }
        try {
            String json = Files.readString(storagePath);
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
            deduplicateEntries();
            sortTrimTop10();
            save();
        } catch (IOException e) {
            // Ignore corrupted files.
        }
    }

    private void deduplicateEntries() {
        Map<String, Entry> bestByName = new LinkedHashMap<>();
        for (Entry entry : entries) {
            String normalizedName = normalizeName(entry.name());
            Entry normalizedEntry = new Entry(normalizedName, entry.score(), entry.pegsLeft(), entry.durationSeconds(), entry.timestamp(), entry.powerupsEnabled());
            Entry currentBest = bestByName.get(normalizedName);
            if (currentBest == null || entryComparator().compare(normalizedEntry, currentBest) < 0) {
                bestByName.put(normalizedName, normalizedEntry);
            }
        }
        entries.clear();
        entries.addAll(bestByName.values());
    }

    private void sortTrimTop10() {
        entries.sort(entryComparator());
        if (entries.size() > 10) {
            entries.subList(10, entries.size()).clear();
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        return normalized.isEmpty() ? "Player" : normalized;
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
            Files.writeString(storagePath, builder.toString());
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
