import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PowerupConfig {
    private static final int DEFAULT_CREDITS_PER_VALID_MOVE = 2;
    private static final int DEFAULT_CREDITS_PER_GAME_WIN_BONUS = 20;
    private static final int DEFAULT_CREDITS_PER_GAME_OVER_BONUS = 6;
    private static final int DEFAULT_INVENTORY_CAP = 5;

    private final int creditsPerValidMove;
    private final int creditsPerGameWinBonus;
    private final int creditsPerGameOverBonus;
    private final int inventoryCap;
    private final Map<PowerupType, Integer> perRunCaps;
    private final Map<PowerupType, Integer> prices;

    private PowerupConfig(int creditsPerValidMove,
                          int creditsPerGameWinBonus,
                          int creditsPerGameOverBonus,
                          int inventoryCap,
                          Map<PowerupType, Integer> perRunCaps,
                          Map<PowerupType, Integer> prices) {
        this.creditsPerValidMove = Math.max(0, creditsPerValidMove);
        this.creditsPerGameWinBonus = Math.max(0, creditsPerGameWinBonus);
        this.creditsPerGameOverBonus = Math.max(0, creditsPerGameOverBonus);
        this.inventoryCap = Math.max(1, inventoryCap);
        this.perRunCaps = Collections.unmodifiableMap(new EnumMap<>(perRunCaps));
        this.prices = Collections.unmodifiableMap(new EnumMap<>(prices));
    }

    public static PowerupConfig loadFromFile(Path path) {
        PowerupConfig fallback = defaults();
        if (path == null || !Files.exists(path)) {
            return fallback;
        }
        try {
            String json = Files.readString(path);
            int creditsPerValidMove = extractInt(json, "creditsPerValidMove", fallback.getCreditsPerValidMove());
            int creditsPerGameWinBonus = extractInt(json, "creditsPerGameWinBonus", fallback.getCreditsPerGameWinBonus());
            int creditsPerGameOverBonus = extractInt(json, "creditsPerGameOverBonus", fallback.getCreditsPerGameOverBonus());
            int inventoryCap = extractInt(json, "inventoryCap", fallback.getInventoryCap());

            Map<PowerupType, Integer> perRunCaps = extractPowerupMap(json, "perRunCaps", fallback.perRunCaps, 1);
            Map<PowerupType, Integer> prices = extractPowerupMap(json, "prices", fallback.prices, 0);

            return new PowerupConfig(
                    creditsPerValidMove,
                    creditsPerGameWinBonus,
                    creditsPerGameOverBonus,
                    inventoryCap,
                    perRunCaps,
                    prices);
        } catch (IOException e) {
            return fallback;
        }
    }

    public static PowerupConfig defaults() {
        Map<PowerupType, Integer> perRunCaps = new EnumMap<>(PowerupType.class);
        Map<PowerupType, Integer> prices = new EnumMap<>(PowerupType.class);
        for (PowerupType type : PowerupType.values()) {
            perRunCaps.put(type, 1);
            prices.put(type, 40);
        }
        perRunCaps.put(PowerupType.UNDO, 2);
        perRunCaps.put(PowerupType.MOVE, 2);
        perRunCaps.put(PowerupType.BOMB, 2);
        perRunCaps.put(PowerupType.BRIDGE, 1);
        perRunCaps.put(PowerupType.RANDSTURM, 1);

        prices.put(PowerupType.UNDO, 20);
        prices.put(PowerupType.MOVE, 40);
        prices.put(PowerupType.BOMB, 55);
        prices.put(PowerupType.BRIDGE, 65);
        prices.put(PowerupType.RANDSTURM, 80);

        return new PowerupConfig(
                DEFAULT_CREDITS_PER_VALID_MOVE,
                DEFAULT_CREDITS_PER_GAME_WIN_BONUS,
                DEFAULT_CREDITS_PER_GAME_OVER_BONUS,
                DEFAULT_INVENTORY_CAP,
                perRunCaps,
                prices);
    }

    private static int extractInt(String json, String key, int fallback) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : fallback;
    }

    private static Map<PowerupType, Integer> extractPowerupMap(String json,
                                                                String key,
                                                                Map<PowerupType, Integer> fallback,
                                                                int minValue) {
        Map<PowerupType, Integer> result = new EnumMap<>(PowerupType.class);
        result.putAll(fallback);

        Pattern objectPattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
        Matcher objectMatcher = objectPattern.matcher(json);
        if (!objectMatcher.find()) {
            return result;
        }

        String objectBody = objectMatcher.group(1);
        Pattern pairPattern = Pattern.compile("\\\"([A-Za-z_]+)\\\"\\s*:\\s*(\\d+)");
        Matcher pairMatcher = pairPattern.matcher(objectBody);
        while (pairMatcher.find()) {
            try {
                PowerupType type = PowerupType.valueOf(pairMatcher.group(1).trim().toUpperCase());
                int value = Math.max(minValue, Integer.parseInt(pairMatcher.group(2)));
                result.put(type, value);
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown powerup keys.
            }
        }
        return result;
    }

    public int getCreditsPerValidMove() {
        return creditsPerValidMove;
    }

    public int getCreditsPerGameWinBonus() {
        return creditsPerGameWinBonus;
    }

    public int getCreditsPerGameOverBonus() {
        return creditsPerGameOverBonus;
    }

    public int getInventoryCap() {
        return inventoryCap;
    }

    public int getPerRunCap(PowerupType type) {
        return perRunCaps.getOrDefault(type, 1);
    }

    public int getPrice(PowerupType type) {
        return prices.getOrDefault(type, 0);
    }
}
