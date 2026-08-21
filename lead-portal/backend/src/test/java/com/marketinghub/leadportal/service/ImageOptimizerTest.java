package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Valida os contratos de dimensão e formato dos derivados visuais do Lead Portal. */
class ImageOptimizerTest {

    /** Confirma que o derivado da landing usa JPEG e limita a largura a 960 pixels. */
    @Test
    void shouldCreateLandingDerivativeWithWebDimensions() throws IOException {
        BufferedImage source = new BufferedImage(1800, 1200, BufferedImage.TYPE_INT_RGB);
        var graphics = source.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.dispose();
        ByteArrayOutputStream sourceBytes = new ByteArrayOutputStream();
        ImageIO.write(source, "png", sourceBytes);

        ImageOptimizer.OptimizedImage optimized =
                new ImageOptimizer().optimizeForLanding(sourceBytes.toByteArray());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(optimized.content()));

        assertThat(optimized.contentType()).isEqualTo("image/jpeg");
        assertThat(optimized.extension()).isEqualTo("jpg");
        assertThat(decoded.getWidth()).isEqualTo(960);
        assertThat(decoded.getHeight()).isEqualTo(640);
    }
}
