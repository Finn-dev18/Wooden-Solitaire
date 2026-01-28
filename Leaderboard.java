import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Leaderboard {

    private final Map<String, PlayerAccount> players = new LinkedHashMap<>();
    private final List<Entry> entries = new ArrayList<>();

    public Leaderboard() {
        ensurePlayer("Holzspieler");
        ensurePlayer("Strategin");
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
    }

    public List<Entry> getEntries() {
        return new ArrayList<>(entries);
    }

    public record Entry(String playerName, int score, Duration duration, int remainingPegs) {
    }
}
