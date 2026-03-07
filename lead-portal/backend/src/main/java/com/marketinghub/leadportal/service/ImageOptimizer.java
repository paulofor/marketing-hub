package com.marketinghub.leadportal.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ImageOptimizer {

    private static final Logger log = LoggerFactory.getLogger(ImageOptimizer.class);
    private static final int MAX_WIDTH = 1600;
    private static final float OUTPUT_QUALITY = 0.85f;

    public record OptimizedImage(byte[] content, String contentType, String extension) {}

    public OptimizedImage optimize(byte[] content) {
        try {
            BufferedImage image = read(content);
            double scale = image.getWidth() > MAX_WIDTH
                    ? (double) MAX_WIDTH / image.getWidth()
                    : 1.0;

            BufferedImage sanitized = ensureOpaque(image);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thumbnails.of(sanitized)
                    .scale(scale)
                    .outputFormat("jpg")
                    .outputQuality(OUTPUT_QUALITY)
                    .toOutputStream(output);

            return new OptimizedImage(output.toByteArray(), "image/jpeg", "jpg");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to optimize image", ex);
        }
    }

    private BufferedImage read(byte[] content) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(content)) {
            BufferedImage image = javax.imageio.ImageIO.read(input);
            if (image == null) {
                throw new IOException("Content is not a valid image");
            }
            return image;
        }
    }

    private BufferedImage ensureOpaque(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return image;
        }
        BufferedImage rgbImage =
                new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgbImage;
    }
}
