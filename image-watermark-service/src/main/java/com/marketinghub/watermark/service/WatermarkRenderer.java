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
        graphics.drawImage(original, 0, 0, null);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float opacity = (float) Math.max(0.05, Math.min(properties.getOpacity(), 0.6));
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        graphics.setColor(new Color(255, 255, 255));

        int fontSize = Math.max(Math.max(width, height) / 7, 48);
        Font font = fallbackFont.deriveFont(Font.BOLD, fontSize);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);

        double angle = Math.toRadians(-28);
        graphics.rotate(angle, width / 2.0, height / 2.0);

        String text = properties.getText();
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();

        double spacingFactor = properties.getSpacingFactor();
        spacingFactor = Math.max(0.2, Math.min(spacingFactor, 3.0));

        int baseDimension = Math.max(textWidth, textHeight);
        int minimumStep = Math.max((int) (fontSize * 0.6), 16);
        int step = (int) Math.max(baseDimension * spacingFactor, minimumStep);

        for (int x = -width * 2; x < width * 2; x += step) {
            for (int y = -height * 2; y < height * 2; y += step) {
                graphics.drawString(text, x, y);
            }
        }

        graphics.dispose();

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
}
