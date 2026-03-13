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

    @Test
    void shouldCoverMeaningfulAreaToDiscourageOriginalReuse() throws Exception {
        byte[] watermarkedBytes = renderer.applyWatermark(originalBytes).bytes();
        BufferedImage watermarkedImage = ImageIO.read(new ByteArrayInputStream(watermarkedBytes));

        int changedPixels = 0;
        int totalPixels = originalImage.getWidth() * originalImage.getHeight();
        for (int x = 0; x < originalImage.getWidth(); x++) {
            for (int y = 0; y < originalImage.getHeight(); y++) {
                if (originalImage.getRGB(x, y) != watermarkedImage.getRGB(x, y)) {
                    changedPixels++;
                }
            }
        }

        double changedRatio = changedPixels / (double) totalPixels;
        assertThat(changedRatio)
                .as("A marca d'água deve ocupar uma área relevante para evitar reutilização indevida")
                .isGreaterThan(0.18);
    }

    @Test
    void shouldGenerateSquareThumbnailSampleFromOriginalImage() throws Exception {
        WatermarkRenderer.WatermarkedImage result = renderer.applyWatermark(originalBytes);

        assertThat(result.optimizedBytes())
                .as("A miniatura precisa ser gerada")
                .isNotNull();
        assertThat(result.optimizedContentType()).isEqualTo("image/jpeg");
        assertThat(result.optimizedExtension()).isEqualTo("jpg");

        BufferedImage sample = ImageIO.read(new ByteArrayInputStream(result.optimizedBytes()));
        assertThat(sample).isNotNull();
        assertThat(sample.getWidth()).isEqualTo(364);
        assertThat(sample.getHeight()).isEqualTo(364);

        int darkPixels = 0;
        int lightPixels = 0;
        for (int x = 0; x < sample.getWidth(); x++) {
            for (int y = 0; y < sample.getHeight(); y++) {
                Color color = new Color(sample.getRGB(x, y));
                if (color.getRed() < 15 && color.getGreen() < 15 && color.getBlue() < 15) {
                    darkPixels++;
                }
                if (color.getRed() > 240 && color.getGreen() > 240 && color.getBlue() > 240) {
                    lightPixels++;
                }
            }
        }

        assertThat(darkPixels)
                .as("A miniatura deve preservar pixels da imagem original")
                .isGreaterThan(0);
        assertThat(lightPixels)
                .as("As faixas laterais/brancas indicam centralização dentro dos 364px")
                .isGreaterThan(0);
    }

    @Test
    void shouldIncreaseContrastOnLightAndDarkAreas() throws Exception {
        BufferedImage contrastImage = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = contrastImage.createGraphics();
        graphics.setPaint(Color.WHITE);
        graphics.fillRect(0, 0, contrastImage.getWidth(), contrastImage.getHeight() / 2);
        graphics.setPaint(Color.BLACK);
        graphics.fillRect(0, contrastImage.getHeight() / 2, contrastImage.getWidth(), contrastImage.getHeight() / 2);
        graphics.dispose();

        byte[] bytes = renderer.applyWatermark(toPng(contrastImage)).bytes();
        BufferedImage watermarked = ImageIO.read(new ByteArrayInputStream(bytes));

        int half = watermarked.getHeight() / 2;
        boolean topHasDarkPixel = false;
        boolean bottomHasLightPixel = false;

        outer:
        for (int x = 0; x < watermarked.getWidth(); x++) {
            for (int y = 0; y < watermarked.getHeight(); y++) {
                Color color = new Color(watermarked.getRGB(x, y), true);
                if (!topHasDarkPixel && y < half && (color.getRed() < 240 || color.getGreen() < 240 || color.getBlue() < 240)) {
                    topHasDarkPixel = true;
                }
                if (!bottomHasLightPixel && y >= half && (color.getRed() > 15 || color.getGreen() > 15 || color.getBlue() > 15)) {
                    bottomHasLightPixel = true;
                }
                if (topHasDarkPixel && bottomHasLightPixel) {
                    break outer;
                }
            }
        }

        assertThat(topHasDarkPixel)
                .as("A metade clara deve receber pixels escurecidos pela sombra da marca d'água")
                .isTrue();
        assertThat(bottomHasLightPixel)
                .as("A metade escura deve receber pixels iluminados pela marca d'água")
                .isTrue();
    }

    private static byte[] toPng(BufferedImage image) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }
}
