package com.marketinghub.watermark.service;

import com.marketinghub.watermark.config.WatermarkProperties;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WatermarkRendererTest {

    @Test
    void shouldApplyWatermarkGeneratingPngImage() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Ambiente sem suporte gráfico não permite testar renderização de marca d'água");

        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        WatermarkProperties properties = new WatermarkProperties();
        properties.setText("TESTE");
        WatermarkRenderer renderer = new WatermarkRenderer(properties);
        renderer.init();

        WatermarkRenderer.WatermarkedImage watermarked = renderer.applyWatermark(baos.toByteArray());

        assertThat(watermarked).isNotNull();
        assertThat(watermarked.bytes()).isNotEmpty();
        assertThat(watermarked.contentType()).isEqualTo("image/png");
        assertThat(watermarked.extension()).isEqualTo("png");
    }
}
