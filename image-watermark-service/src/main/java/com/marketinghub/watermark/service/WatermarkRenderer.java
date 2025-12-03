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
import javax.imageio.ImageIO;
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
        int step = (int) (Math.max(textWidth, textHeight) * 1.5);

        for (int x = -width * 2; x < width * 2; x += step) {
            for (int y = -height * 2; y < height * 2; y += step) {
                graphics.drawString(text, x, y);
            }
        }

        graphics.dispose();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(watermarked, "png", outputStream);
            return new WatermarkedImage(outputStream.toByteArray(), "image/png", "png");
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao serializar imagem com marca d'água", ex);
        }
    }

    public record WatermarkedImage(byte[] bytes, String contentType, String extension) {}
}
