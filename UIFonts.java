import java.awt.Font;

public final class UIFonts {
    private static final Font BASE_PLAIN = new Font("SansSerif", Font.PLAIN, 12);

    private static final float H1_PX = 22f;
    private static final float H2_PX = 16f;
    private static final float BODY_PX = 13f;
    private static final float SMALL_PX = 12f;
    private static final float MESSAGE_PX = 14f;

    private UIFonts() {
    }

    public static Font h1(int uiScale) {
        return scaled(Font.BOLD, H1_PX, H1_PX, uiScale);
    }

    public static Font h2(int uiScale) {
        return scaled(Font.BOLD, H2_PX, H2_PX, uiScale);
    }

    public static Font body(int uiScale) {
        return scaled(Font.PLAIN, BODY_PX, BODY_PX, uiScale);
    }

    public static Font small(int uiScale) {
        return scaled(Font.PLAIN, SMALL_PX, SMALL_PX, uiScale);
    }

    public static Font message(int uiScale) {
        return scaled(Font.PLAIN, MESSAGE_PX, MESSAGE_PX, uiScale);
    }

    private static Font scaled(int style, float sizePx, float minPx, int uiScale) {
        int normalizedScale = Math.max(1, uiScale);
        float scaledSize = sizePx * normalizedScale;
        float finalSize = Math.max(scaledSize, minPx);
        return BASE_PLAIN.deriveFont(style, finalSize);
    }
}
