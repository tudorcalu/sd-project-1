package org.search.indexing.strategy;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public class ImageIndexingStrategy implements IndexingStrategy {
    private static final int SAMPLE_SIZE = 64;

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".gif") || name.endsWith(".bmp");
    }

    @Override
    public IndexPayload extract(Path file) throws IOException {
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            return new IndexPayload("image", "", "error=unreadable_image;", null);
        }
        BufferedImage sample = resize(image, SAMPLE_SIZE, SAMPLE_SIZE);
        String dominantColor = detectDominantColor(sample);
        String metadata = "dominant_color=" + dominantColor + "; file_type=image;";
        return new IndexPayload("image", dominantColor, metadata, dominantColor);
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        Image scaled = source.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();
        return resized;
    }

    static String detectDominantColor(BufferedImage image) {
        long sumR = 0;
        long sumG = 0;
        long sumB = 0;
        int pixels = image.getWidth() * image.getHeight();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                sumR += (rgb >> 16) & 0xFF;
                sumG += (rgb >> 8) & 0xFF;
                sumB += rgb & 0xFF;
            }
        }
        return bucketColor((int) (sumR / pixels), (int) (sumG / pixels), (int) (sumB / pixels));
    }

    static String bucketColor(int r, int g, int b) {
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        if (max - min < 25) {
            return max > 200 ? "white" : (max < 60 ? "black" : "gray");
        }
        if (r >= g && r >= b) {
            if (g > b + 40) {
                return "orange";
            }
            if (g > 100) {
                return "yellow";
            }
            return "red";
        }
        if (g >= r && g >= b) {
            return "green";
        }
        if (b >= r && b >= g) {
            if (g > r + 30) {
                return "cyan";
            }
            return "blue";
        }
        if (r > 120 && b > 120) {
            return "purple";
        }
        return "gray";
    }
}
