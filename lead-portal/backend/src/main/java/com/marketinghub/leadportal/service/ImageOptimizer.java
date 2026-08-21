package com.marketinghub.leadportal.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

/** Converte imagens recebidas pelo Lead Portal em derivados JPEG adequados ao canal de uso. */
@Service
public class ImageOptimizer {

    private static final int MAX_WIDTH = 1600;
    private static final float OUTPUT_QUALITY = 0.85f;
    private static final int LANDING_MAX_WIDTH = 960;
    private static final float LANDING_OUTPUT_QUALITY = 0.76f;

    /** Representa o conteúdo otimizado e seu contrato de armazenamento. */
    public record OptimizedImage(byte[] content, String contentType, String extension) {}

    /** Otimiza uma imagem de formulário ou prova preservando resolução ampla. */
    public OptimizedImage optimize(byte[] content) {
        return optimize(content, MAX_WIDTH, OUTPUT_QUALITY);
    }

    /** Otimiza uma imagem para carregamento rápido em uma landing pública. */
    public OptimizedImage optimizeForLanding(byte[] content) {
        return optimize(content, LANDING_MAX_WIDTH, LANDING_OUTPUT_QUALITY);
    }

    /** Executa a conversão JPEG com largura e qualidade definidas para o canal. */
    private OptimizedImage optimize(byte[] content, int maxWidth, float outputQuality) {
        try {
            BufferedImage image = read(content);
            double scale = image.getWidth() > maxWidth
                    ? (double) maxWidth / image.getWidth()
                    : 1.0;

            BufferedImage sanitized = ensureOpaque(image);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thumbnails.of(sanitized)
                    .scale(scale)
                    .outputFormat("jpg")
                    .outputQuality(outputQuality)
                    .toOutputStream(output);

            return new OptimizedImage(output.toByteArray(), "image/jpeg", "jpg");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to optimize image", ex);
        }
    }

    /** Decodifica o conteúdo e rejeita arquivos que não representam uma imagem válida. */
    private BufferedImage read(byte[] content) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(content)) {
            BufferedImage image = javax.imageio.ImageIO.read(input);
            if (image == null) {
                throw new IOException("Content is not a valid image");
            }
            return image;
        }
    }

    /** Remove transparência sobre fundo branco para permitir a codificação JPEG. */
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
