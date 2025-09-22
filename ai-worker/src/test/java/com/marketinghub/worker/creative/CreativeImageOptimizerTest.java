package com.marketinghub.worker.creative;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class CreativeImageOptimizerTest {
    CreativeImageOptimizer optimizer = new CreativeImageOptimizer(350_000, 800);

    @Test
    void compressesLargePngBelowConfiguredLimit() throws IOException {
        byte[] original = createRandomPng(1200, 1200);

        CreativeImageOptimizer.OptimizedImage optimized = optimizer.optimize(original);

        assertThat(optimized.extension()).isEqualTo("jpg");
        assertThat(optimized.content().length).isLessThanOrEqualTo(350_000);
        assertThat(optimized.content().length).isLessThan(original.length);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(optimized.content()));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getColorModel().hasAlpha()).isFalse();
        assertThat(decoded.getWidth()).isLessThanOrEqualTo(800);
        assertThat(decoded.getHeight()).isLessThanOrEqualTo(800);
    }

    private static byte[] createRandomPng(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(123);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = random.nextInt(256);
                int g = random.nextInt(256);
                int b = random.nextInt(256);
                Color color = new Color(r, g, b, random.nextInt(256));
                image.setRGB(x, y, color.getRGB());
            }
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
