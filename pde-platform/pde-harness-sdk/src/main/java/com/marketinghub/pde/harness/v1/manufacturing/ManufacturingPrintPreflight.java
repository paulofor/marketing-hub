package com.marketinghub.pde.harness.v1.manufacturing;

import com.marketinghub.pde.harness.v1.PdeConversationScope;
import com.marketinghub.pde.harness.v1.internal.PdeHashing;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Confere bytes reais de impressão plana sem rede, geração de imagem, banco ou envio à fábrica. */
public final class ManufacturingPrintPreflight {
  private static final System.Logger LOGGER =
      System.getLogger(ManufacturingPrintPreflight.class.getName());
  private static final int MAX_BYTES = 25 * 1024 * 1024;
  private static final long MAX_PIXELS = 40_000_000L;

  /**
   * Compara arquivo e ficha no escopo confiável informado pelo backend, sem aceitar aprovação do
   * modelo.
   */
  public PrintPreflightResult inspect(
      PdeConversationScope expectedScope,
      PdeConversationScope artifactScope,
      byte[] content,
      String expectedSha256,
      PrintSpecification specification) {
    Objects.requireNonNull(expectedScope, "expectedScope");
    Objects.requireNonNull(specification, "specification");
    if (!expectedScope.equals(artifactScope)) {
      return result(expectedScope, "", specification, 0, 0, 0, List.of("SCOPE_MISMATCH"));
    }
    if (content == null || content.length == 0 || content.length > MAX_BYTES) {
      return result(expectedScope, "", specification, 0, 0, 0, List.of("INVALID_FILE_SIZE"));
    }
    byte[] snapshot = content.clone();
    String hash = PdeHashing.sha256(snapshot);
    if (!hash.equals(expectedSha256)) {
      return result(expectedScope, hash, specification, 0, 0, 0, List.of("HASH_MISMATCH"));
    }
    List<String> issues = new ArrayList<>();
    int width = 0;
    int height = 0;
    double dpi = 0;
    try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(snapshot))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        return result(expectedScope, hash, specification, 0, 0, 0, List.of("UNSUPPORTED_IMAGE"));
      }
      ImageReader reader = readers.next();
      try {
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!format.equals("png") && !format.equals("jpeg")) {
          return result(expectedScope, hash, specification, 0, 0, 0, List.of("UNSUPPORTED_IMAGE"));
        }
        reader.setInput(input, true, false);
        reader.addIIOReadWarningListener(
            (source, warning) -> {
              if (!issues.contains("IMAGE_DECODE_WARNING")) issues.add("IMAGE_DECODE_WARNING");
            });
        width = reader.getWidth(0);
        height = reader.getHeight(0);
        if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
          return result(
              expectedScope, hash, specification, width, height, 0, List.of("PIXEL_LIMIT"));
        }
        double targetWidth = specification.widthMm() + 2 * specification.bleedMm();
        double targetHeight = specification.heightMm() + 2 * specification.bleedMm();
        dpi = Math.min(width * 25.4 / targetWidth, height * 25.4 / targetHeight);
        if (dpi < specification.minimumDpi()) issues.add("INSUFFICIENT_RESOLUTION");
        // Tolera apenas o arredondamento de um pixel, sem cortar ou distorcer a arte
        // silenciosamente.
        if (Math.abs(width - height * targetWidth / targetHeight) > 1.0)
          issues.add("ASPECT_RATIO_MISMATCH");
        BufferedImage image = reader.read(0);
        if (!image.getColorModel().getColorSpace().isCS_sRGB())
          issues.add("COLOR_PROFILE_REQUIRES_REVIEW");
        if (!specification.transparencyAllowed()
            && image.getColorModel().hasAlpha()
            && hasTransparentPixels(image)) issues.add("TRANSPARENCY_NOT_ALLOWED");
      } finally {
        reader.dispose();
      }
    } catch (IOException | RuntimeException ex) {
      LOGGER.log(
          System.Logger.Level.WARNING,
          "Falha no preflight de impressão; scope="
              + expectedScope.fingerprint()
              + "; sha256="
              + hash
              + "; specification="
              + specification.reference(),
          ex);
      issues.add("INVALID_IMAGE");
    }
    return result(expectedScope, hash, specification, width, height, dpi, issues);
  }

  /** Detecta pixels translúcidos em vez de rejeitar um canal alfa inteiramente opaco. */
  private boolean hasTransparentPixels(BufferedImage image) {
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if ((image.getRGB(x, y) >>> 24) != 255) return true;
      }
    }
    return false;
  }

  /** Monta a evidência mínima sem copiar a imagem privada para logs ou respostas. */
  private PrintPreflightResult result(
      PdeConversationScope scope,
      String hash,
      PrintSpecification spec,
      int width,
      int height,
      double dpi,
      List<String> issues) {
    return new PrintPreflightResult(
        scope.fingerprint(), hash, spec.reference(), spec.sku(), width, height, dpi, issues);
  }
}
