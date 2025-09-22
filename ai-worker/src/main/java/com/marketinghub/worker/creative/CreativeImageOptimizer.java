package com.marketinghub.worker.creative;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Optimizes image payloads returned by the generation provider before sending them to the backend.
 * <p>
 * The backend rejects uploads above a certain threshold (HTTP 413). In practice the OpenAI API
 * returns PNG images that easily exceed that limit, so we convert them to JPEG, apply compression
 * and gradually downscale until the configured size budget is respected.
 */
@Component
public class CreativeImageOptimizer {
    private static final Logger log = LoggerFactory.getLogger(CreativeImageOptimizer.class);

    private static final List<Float> QUALITY_STEPS = List.of(0.85f, 0.75f, 0.65f, 0.55f, 0.45f);
    private static final List<Double> SCALE_FACTORS = List.of(1.0, 0.9, 0.8, 0.7, 0.6, 0.5);

    private final long maxBytes;
    private final int maxDimension;

    public CreativeImageOptimizer(@Value("${creative.image.max-bytes:900000}") long maxBytes,
                                  @Value("${creative.image.max-dimension:1024}") int maxDimension) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("Maximum image size must be greater than zero");
        }
        if (maxDimension <= 0) {
            throw new IllegalArgumentException("Maximum image dimension must be greater than zero");
        }
        this.maxBytes = maxBytes;
        this.maxDimension = maxDimension;
    }

    public OptimizedImage optimize(byte[] originalBytes) {
        if (originalBytes == null || originalBytes.length == 0) {
            throw new IllegalArgumentException("Image content must not be empty");
        }
        BufferedImage original = readImage(originalBytes);
        BufferedImage normalized = normalize(original);

        byte[] bestCandidate = null;
        float bestQuality = QUALITY_STEPS.get(QUALITY_STEPS.size() - 1);
        double bestScale = SCALE_FACTORS.get(SCALE_FACTORS.size() - 1);

        for (double scaleFactor : SCALE_FACTORS) {
            BufferedImage scaled = scale(normalized, scaleFactor);
            for (float quality : QUALITY_STEPS) {
                byte[] encoded = encodeJpeg(scaled, quality);
                if (bestCandidate == null || encoded.length < bestCandidate.length) {
                    bestCandidate = encoded;
                    bestQuality = quality;
                    bestScale = scaleFactor;
                }
                if (encoded.length <= maxBytes) {
                    log.debug("Optimized image within limit: {} bytes (scale={}, quality={})", encoded.length,
                            scaleFactor, quality);
                    return new OptimizedImage(encoded, "jpg");
                }
            }
        }

        if (bestCandidate != null) {
            log.warn("Returning optimized image above limit {} bytes (size={}, scale={}, quality={})",
                    maxBytes, bestCandidate.length, bestScale, bestQuality);
            return new OptimizedImage(bestCandidate, "jpg");
        }
        throw new IllegalStateException("Failed to optimize image");
    }

    private BufferedImage readImage(byte[] bytes) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalArgumentException("Unsupported image format");
            }
            return image;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode image", e);
        }
    }

    private BufferedImage normalize(BufferedImage source) {
        BufferedImage withoutAlpha = ensureOpaque(source);
        if (withoutAlpha.getWidth() <= maxDimension && withoutAlpha.getHeight() <= maxDimension) {
            return withoutAlpha;
        }
        double scale = Math.min((double) maxDimension / withoutAlpha.getWidth(),
                (double) maxDimension / withoutAlpha.getHeight());
        return resize(withoutAlpha, scale);
    }

    private BufferedImage ensureOpaque(BufferedImage image) {
        if (!image.getColorModel().hasAlpha() && image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = converted.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return converted;
    }

    private BufferedImage scale(BufferedImage image, double factor) {
        if (factor >= 0.999) {
            return image;
        }
        return resize(image, factor);
    }

    private BufferedImage resize(BufferedImage image, double factor) {
        int newWidth = Math.max(1, (int) Math.round(image.getWidth() * factor));
        int newHeight = Math.max(1, (int) Math.round(image.getHeight() * factor));
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(image, 0, 0, newWidth, newHeight, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) {
        var writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG writers available");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), params);
            imageOutput.flush();
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode image as JPEG", e);
        } finally {
            writer.dispose();
        }
    }

    public record OptimizedImage(byte[] content, String extension) { }
}
