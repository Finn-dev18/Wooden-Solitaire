import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public class AssetManager {
    private static final Pattern SIZE_PATTERN = Pattern.compile(".*_(\\d+)x(\\d+)\\.png$");
    private final Map<String, BufferedImage> images = new HashMap<>();

    public BufferedImage getImage(String name) {
        BufferedImage image = images.get(name);
        if (image == null) {
            image = loadImage(name);
            images.put(name, image);
        }
        return image;
    }



    public void preload(String... names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            if (name != null && !name.isEmpty()) {
                getImage(name);
            }
        }
    }

    private BufferedImage loadImage(String name) {
        File file = new File("assets", name);
        if (!file.exists()) {
            return createPlaceholderForName(name);
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                return createPlaceholderForName(name);
            }
            return image;
        } catch (IOException e) {
            return createPlaceholderForName(name);
        }
    }

    private BufferedImage createPlaceholderForName(String name) {
        int[] size = parseSize(name);
        return createPlaceholder(size[0], size[1], new Color(252, 16, 87));
    }

    private int[] parseSize(String name) {
        Matcher matcher = SIZE_PATTERN.matcher(name);
        if (matcher.matches()) {
            try {
                int width = Integer.parseInt(matcher.group(1));
                int height = Integer.parseInt(matcher.group(2));
                return new int[]{width, height};
            } catch (NumberFormatException ignored) {
                return new int[]{32, 32};
            }
        }
        return new int[]{32, 32};
    }

    public BufferedImage createPlaceholder(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, width - 1, height - 1);
        g2d.dispose();
        return image;
    }
}
