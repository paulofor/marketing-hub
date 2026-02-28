package com.marketinghub.watermark.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.watermark.config.WatermarkProperties;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WatermarkRendererTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private WatermarkRenderer renderer;
    private BufferedImage originalImage;
    private byte[] originalBytes;

    @BeforeEach
    void setUp() throws Exception {
        WatermarkProperties properties = new WatermarkProperties();
        renderer = new WatermarkRenderer(properties);
        renderer.init();

        originalImage = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = originalImage.createGraphics();
        graphics.setPaint(Color.BLACK);
        graphics.fillRect(0, 0, originalImage.getWidth(), originalImage.getHeight());
        graphics.dispose();

        originalBytes = toPng(originalImage);

        assertThat(properties.getText()).isEqualTo("PRODUTIVIDADE 360");
    }

    @Test
    void shouldApplyProdutividade360WatermarkAndModifyImage() throws Exception {
        try {
            byte[] watermarkedBytes = renderer.applyWatermark(originalBytes).bytes();

            assertThat(watermarkedBytes).isNotEqualTo(originalBytes);

            BufferedImage watermarkedImage = ImageIO.read(new ByteArrayInputStream(watermarkedBytes));
            assertThat(watermarkedImage).isNotNull();

            boolean anyDifference = false;
            outer:
            for (int x = 0; x < originalImage.getWidth(); x++) {
                for (int y = 0; y < originalImage.getHeight(); y++) {
                    if (originalImage.getRGB(x, y) != watermarkedImage.getRGB(x, y)) {
                        anyDifference = true;
                        break outer;
                    }
                }
            }

            assertThat(anyDifference)
                .as("A imagem deve apresentar diferenças visíveis após a aplicação da marca d'água")
                .isTrue();
        } catch (RuntimeException ex) {
            String message = ex.getMessage();
            if (message != null && message.contains("Fontconfig head is null")) {
                org.junit.jupiter.api.Assumptions.assumeTrue(false, "Ambiente sem suporte a fontes para renderização de imagens");
            }
            throw ex;
        }
    }

    private static byte[] toPng(BufferedImage image) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }
}
