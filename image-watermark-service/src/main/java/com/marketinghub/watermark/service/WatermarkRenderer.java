package com.marketinghub.watermark.service;

import com.marketinghub.watermark.config.WatermarkProperties;
import jakarta.annotation.PostConstruct;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

            float textOpacity = (float) clamp(properties.getOpacity(), 0.2, 0.95);
            float shadowOpacity = Math.min(textOpacity + 0.25f, 0.98f);

            int fontSize = Math.max(Math.max(width, height) / 6, 56);
            Font font = fallbackFont.deriveFont(Font.BOLD, (float) fontSize);
            FontMetrics metrics = graphics.getFontMetrics(font);

            double angle = Math.toRadians(-28);
            graphics.rotate(angle, width / 2.0, height / 2.0);

            Graphics2D shadowGraphics = (Graphics2D) graphics.create();
            Graphics2D textGraphics = (Graphics2D) graphics.create();
            try {
                shadowGraphics.setFont(font);
                textGraphics.setFont(font);

                shadowGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shadowOpacity));
                shadowGraphics.setColor(Color.BLACK);

                textGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textOpacity));
                textGraphics.setColor(new Color(255, 255, 255));

                String text = properties.getText();
                int textWidth = metrics.stringWidth(text);
                int textHeight = metrics.getHeight();

                double spacingFactor = clamp(properties.getSpacingFactor(), 0.2, 3.0);

                int baseDimension = Math.max(textWidth, textHeight);
                int minimumStep = Math.max((int) (fontSize * 0.45), 12);
                int configuredStep = (int) Math.max(baseDimension * spacingFactor, minimumStep);
                int diagonal = (int) Math.ceil(Math.hypot(width, height));
                int maxStep = Math.max(minimumStep, diagonal / 3);
                int step = Math.min(configuredStep, maxStep);

                int shadowOffset = Math.max(2, fontSize / 18);
                for (int x = -diagonal; x <= diagonal * 2; x += step) {
                    for (int y = -diagonal; y <= diagonal * 2; y += step) {
                        shadowGraphics.drawString(text, x + shadowOffset, y + shadowOffset);
                        textGraphics.drawString(text, x, y);
                    }
                }
            } finally {
                shadowGraphics.dispose();
                textGraphics.dispose();
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(watermarked, "png", outputStream);
            byte[] optimizedBytes = renderOptimizedCopy(watermarked);
            return new WatermarkedImage(
                    outputStream.toByteArray(),
                    "image/png",
                    "png",
                    optimizedBytes,
                    optimizedBytes != null ? "image/jpeg" : null,
                    optimizedBytes != null ? "jpg" : null);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao serializar imagem com marca d'água", ex);
        }
    }

    private byte[] renderOptimizedCopy(BufferedImage watermarked) {
        if (!properties.isGenerateOptimizedCopy()) {
            return null;
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            BufferedImage rgbImage = new BufferedImage(
                    watermarked.getWidth(), watermarked.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgbImage.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
            graphics.drawImage(watermarked, 0, 0, null);
            graphics.dispose();

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                log.warn("Nenhum writer JPEG disponível para gerar cópia otimizada");
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
                writer.write(null, new IIOImage(rgbImage, null, null), params);
            } finally {
                writer.dispose();
            }
            return outputStream.toByteArray();
        } catch (IOException ex) {
            log.warn("Falha ao gerar versão otimizada da imagem com marca d'água", ex);
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
