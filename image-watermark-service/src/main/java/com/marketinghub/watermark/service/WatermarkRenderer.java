package com.marketinghub.watermark.service;

import com.marketinghub.watermark.config.WatermarkProperties;
import jakarta.annotation.PostConstruct;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WatermarkRenderer {

    private static final Logger log = LoggerFactory.getLogger(WatermarkRenderer.class);
    private static final int SAMPLE_DIMENSION = 364;

    private final WatermarkProperties properties;
    private Font fallbackFont;

    public WatermarkRenderer(WatermarkProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        int defaultSize = 72;
        try {
            fallbackFont = new Font(properties.getFontFamily(), Font.BOLD, defaultSize);
        } catch (Exception ex) {
            log.warn("Fonte '{}' indisponível, utilizando SansSerif", properties.getFontFamily());
            fallbackFont = new Font(Font.SANS_SERIF, Font.BOLD, defaultSize);
        }
    }

    public WatermarkedImage applyWatermark(byte[] originalBytes) {
        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(originalBytes));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Formato de imagem não suportado", ex);
        }
        if (original == null) {
            throw new IllegalArgumentException("Não foi possível ler a imagem original");
        }

        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage watermarked = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = watermarked.createGraphics();
        try {
            graphics.drawImage(original, 0, 0, null);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            float textOpacity = (float) clamp(properties.getOpacity(), 0.25, 0.95);
            float shadowOpacity = Math.min(textOpacity + 0.3f, 0.98f);

            int fontSize = Math.max(Math.max(width, height) / 5, 64);
            Font font = fallbackFont.deriveFont(Font.BOLD, (float) fontSize);
            String text = resolveWatermarkText();

            WatermarkGrid grid = createGridMetrics(graphics, font, text, width, height);
            drawWatermarkLayer(graphics, font, text, grid, width, height, -28, textOpacity, shadowOpacity, 0);
            drawWatermarkLayer(graphics, font, text, grid, width, height, 28, textOpacity * 0.82f, shadowOpacity * 0.75f,
                    grid.step / 2.0);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(watermarked, "png", outputStream);
            SampleImage sampleImage = renderSampleThumbnail(original);
            return new WatermarkedImage(
                    outputStream.toByteArray(),
                    "image/png",
                    "png",
                    sampleImage != null ? sampleImage.bytes() : null,
                    sampleImage != null ? sampleImage.contentType() : null,
                    sampleImage != null ? sampleImage.extension() : null);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao serializar imagem com marca d'água", ex);
        }
    }

    private void drawWatermarkLayer(
            Graphics2D baseGraphics,
            Font font,
            String text,
            WatermarkGrid grid,
            int width,
            int height,
            int angleDegrees,
            float textOpacity,
            float shadowOpacity,
            double phaseShift) {
        Graphics2D layerGraphics = (Graphics2D) baseGraphics.create();
        try {
            layerGraphics.setFont(font);
            layerGraphics.rotate(Math.toRadians(angleDegrees), width / 2.0, height / 2.0);
            layerGraphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int outlineOffset = Math.max(2, grid.fontSize / 20);
            int shadowOffset = Math.max(3, grid.fontSize / 14);

            for (double x = -grid.diagonal + phaseShift; x <= grid.diagonal * 2.0; x += grid.step) {
                for (double y = -grid.diagonal + phaseShift; y <= grid.diagonal * 2.0; y += grid.step) {
                    Point2D point = new Point2D.Double(x, y);
                    drawOutlinedText(layerGraphics, text, point, textOpacity, shadowOpacity, outlineOffset, shadowOffset);
                }
            }
        } finally {
            layerGraphics.dispose();
        }
    }

    private void drawOutlinedText(
            Graphics2D graphics,
            String text,
            Point2D point,
            float textOpacity,
            float shadowOpacity,
            int outlineOffset,
            int shadowOffset) {
        float x = (float) point.getX();
        float y = (float) point.getY();

        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shadowOpacity));
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, x + shadowOffset, y + shadowOffset);

        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shadowOpacity * 0.75f));
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, x - outlineOffset, y);
        graphics.drawString(text, x + outlineOffset, y);
        graphics.drawString(text, x, y - outlineOffset);
        graphics.drawString(text, x, y + outlineOffset);

        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textOpacity));
        graphics.setColor(Color.WHITE);
        graphics.drawString(text, x, y);
    }

    private WatermarkGrid createGridMetrics(Graphics2D graphics, Font font, String text, int width, int height) {
        FontMetrics metrics = graphics.getFontMetrics(font);
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();
        int diagonal = (int) Math.ceil(Math.hypot(width, height));
        double spacingFactor = clamp(properties.getSpacingFactor(), 0.2, 3.0);

        int baseDimension = Math.max(textWidth, textHeight);
        int minimumStep = Math.max((int) (font.getSize2D() * 0.42), 14);
        int configuredStep = (int) Math.max(baseDimension * spacingFactor, minimumStep);
        int maxStep = Math.max(minimumStep, diagonal / 4);
        int step = Math.min(configuredStep, maxStep);

        return new WatermarkGrid(diagonal, step, font.getSize());
    }

    private String resolveWatermarkText() {
        String rawText = properties.getText();
        if (rawText == null || rawText.isBlank()) {
            return "PRODUTIVIDADE 360";
        }
        return rawText.trim();
    }

    private record WatermarkGrid(int diagonal, int step, int fontSize) {}

    private SampleImage renderSampleThumbnail(BufferedImage original) {
        if (!properties.isGenerateOptimizedCopy()) {
            return null;
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            BufferedImage canvas = new BufferedImage(SAMPLE_DIMENSION, SAMPLE_DIMENSION, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = canvas.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, SAMPLE_DIMENSION, SAMPLE_DIMENSION);

                double scale = SAMPLE_DIMENSION / (double) Math.max(original.getWidth(), original.getHeight());
                int scaledWidth = Math.max(1, (int) Math.round(original.getWidth() * scale));
                int scaledHeight = Math.max(1, (int) Math.round(original.getHeight() * scale));
                Image scaledImage = original.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                int offsetX = (SAMPLE_DIMENSION - scaledWidth) / 2;
                int offsetY = (SAMPLE_DIMENSION - scaledHeight) / 2;
                graphics.drawImage(scaledImage, offsetX, offsetY, null);
            } finally {
                graphics.dispose();
            }

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                log.warn("Nenhum writer JPEG disponível para gerar miniatura da amostra");
                return null;
            }
            ImageWriter writer = writers.next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                float quality = (float) Math.max(0.1, Math.min(properties.getOptimizedJpegQuality(), 1.0));
                params.setCompressionQuality(quality);
            }
            try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
                writer.setOutput(imageOutputStream);
                writer.write(null, new IIOImage(canvas, null, null), params);
            } finally {
                writer.dispose();
            }
            return new SampleImage(outputStream.toByteArray(), "image/jpeg", "jpg");
        } catch (IOException ex) {
            log.warn("Falha ao gerar miniatura 364x364 para amostra", ex);
            return null;
        }
    }

    public record WatermarkedImage(
            byte[] bytes,
            String contentType,
            String extension,
            byte[] optimizedBytes,
            String optimizedContentType,
            String optimizedExtension) {

        public boolean hasOptimizedVersion() {
            return optimizedBytes != null
                    && optimizedBytes.length > 0
                    && optimizedContentType != null
                    && optimizedExtension != null;
        }
    }

    private record SampleImage(byte[] bytes, String contentType, String extension) {}

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
