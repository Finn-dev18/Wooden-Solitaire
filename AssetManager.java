import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class AssetManager {
    private final Map<String, BufferedImage> images = new HashMap<>();

    public BufferedImage getImage(String name) {
        BufferedImage image = images.get(name);
        if (image == null) {
            image = loadImage(name);
            images.put(name, image);
        }
        return image;
    }

    private BufferedImage loadImage(String name) {
        File file = new File("assets", name);
        if (!file.exists()) {
            return createPlaceholder(32, 32, new Color(252, 16, 87));
        }
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            return createPlaceholder(32, 32, new Color(252, 16, 87));
        }
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
