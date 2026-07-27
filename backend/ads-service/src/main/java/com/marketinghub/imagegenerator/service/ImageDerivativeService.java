package com.marketinghub.imagegenerator.service;

import com.marketinghub.imagegenerator.dto.ImageGeneratorResponse.ImageGeneratorVariant;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Responsabilidade: criar versões leves de imagens geradas para uso em páginas e PDEs. */
@Service
public class ImageDerivativeService {
  private static final Logger log = LoggerFactory.getLogger(ImageDerivativeService.class);
  private static final int WEB_MAX_WIDTH = 1600;
  private static final int MOBILE_MAX_WIDTH = 900;
  private static final float WEB_QUALITY = 0.82f;
  private static final float MOBILE_QUALITY = 0.78f;

  /** Gera a imagem original e derivados leves para internet mantendo metadados de tamanho. */
  public List<ImageGeneratorVariant> createVariants(String originalFormat, String imageBase64) {
    try {
      byte[] originalBytes = Base64.getDecoder().decode(imageBase64);
      BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
      if (original == null) {
        throw new IllegalArgumentException(
            "Não foi possível ler a imagem original retornada pela IA.");
      }

      List<ImageGeneratorVariant> variants = new ArrayList<>();
      variants.add(
          new ImageGeneratorVariant(
              "original",
              originalFormat,
              imageBase64,
              original.getWidth(),
              original.getHeight(),
              originalBytes.length));
      variants.add(createJpegVariant("web", original, WEB_MAX_WIDTH, WEB_QUALITY));
      variants.add(createJpegVariant("mobile", original, MOBILE_MAX_WIDTH, MOBILE_QUALITY));
      return variants;
    } catch (IOException ex) {
      log.error(
          "Falha ao gerar derivados web da imagem. modulo=image-generator operacao=createVariants",
          ex);
      throw new UncheckedIOException("Não foi possível gerar versões leves da imagem.", ex);
    } catch (IllegalArgumentException ex) {
      log.error(
          "Falha ao validar imagem base64 para derivados web. modulo=image-generator operacao=createVariants",
          ex);
      throw ex;
    }
  }

  /** Cria uma variante JPEG redimensionada para a largura máxima informada. */
  private ImageGeneratorVariant createJpegVariant(
      String role, BufferedImage original, int maxWidth, float quality) throws IOException {
    BufferedImage resized = resize(original, maxWidth);
    byte[] jpegBytes = writeJpeg(resized, quality);
    return new ImageGeneratorVariant(
        role,
        "jpeg",
        Base64.getEncoder().encodeToString(jpegBytes),
        resized.getWidth(),
        resized.getHeight(),
        jpegBytes.length);
  }

  /** Redimensiona a imagem preservando proporção e sem ampliar originais menores. */
  private BufferedImage resize(BufferedImage original, int maxWidth) {
    if (original.getWidth() <= maxWidth) {
      return toRgb(original, original.getWidth(), original.getHeight());
    }
    int targetHeight =
        Math.max(1, Math.round((float) original.getHeight() * maxWidth / original.getWidth()));
    return toRgb(original, maxWidth, targetHeight);
  }

  /** Converte a imagem para RGB com fundo branco para serialização JPEG compatível. */
  private BufferedImage toRgb(BufferedImage source, int width, int height) {
    BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = target.createGraphics();
    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setColor(java.awt.Color.WHITE);
      graphics.fillRect(0, 0, width, height);
      graphics.drawImage(source, 0, 0, width, height, null);
      return target;
    } finally {
      graphics.dispose();
    }
  }

  /** Serializa a imagem em JPEG usando compressão explícita para reduzir peso. */
  private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
    if (!writers.hasNext()) {
      throw new IOException("Codec JPEG não disponível no runtime Java.");
    }
    ImageWriter writer = writers.next();
    try (ByteArrayOutputStream output = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(output)) {
      ImageWriteParam params = writer.getDefaultWriteParam();
      params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      params.setCompressionQuality(quality);
      writer.setOutput(imageOutput);
      writer.write(null, new IIOImage(image, null, null), params);
      return output.toByteArray();
    } finally {
      writer.dispose();
    }
  }
}
