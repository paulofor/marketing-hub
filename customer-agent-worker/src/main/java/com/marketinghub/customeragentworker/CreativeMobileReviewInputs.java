package com.marketinghub.customeragentworker;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/** Responsabilidade: fornecer a redução fiel do criativo para a revisão visual em celular. */
final class CreativeMobileReviewInputs {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(CreativeMobileReviewInputs.class);
  static final int WIDTH = 393;

  /** Acrescenta uma prévia local rastreável sem substituir o artefato persistido ou seu hash. */
  static Map<String, Object> enrich(
      Map<String, Object> task, List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> images)
      throws IOException {
    if (!"creative-production-approval".equals(task.get("processCode"))) return task;
    List<Map<String, Object>> previews = new ArrayList<>();
    for (var image : images) {
      if (!"CREATIVE_RENDER".equals(image.evidenceType())) continue;
      Path source = Path.of(image.localPath());
      byte[] originalBytes = Files.readAllBytes(source);
      if (!sha(originalBytes).equals(image.sha256()))
        throw new IOException("O criativo mudou antes de gerar a prévia mobile.");
      BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
      if (original == null || original.getWidth() != 1080 || original.getHeight() != 1350)
        throw new IOException("A prévia mobile exige o criativo original contratado.");
      int height = (int) Math.round((double) original.getHeight() * WIDTH / original.getWidth());
      var preview = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
      var graphics = preview.createGraphics();
      try {
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(original, 0, 0, WIDTH, height, null);
      } finally {
        graphics.dispose();
      }
      var bytes = new ByteArrayOutputStream();
      ImageIO.write(preview, "png", bytes);
      Files.write(previewPath(source), bytes.toByteArray());
      previews.add(
          Map.of(
              "sourceArtifactId",
              image.id(),
              "sourceSha256",
              image.sha256(),
              "previewSha256",
              sha(bytes.toByteArray()),
              "width",
              WIDTH,
              "height",
              height,
              "transformVersion",
              "WIDTH_393_BICUBIC_V1"));
    }
    if (previews.isEmpty())
      throw new IOException("Revisão criativa sem prévia mobile verificável.");
    var enriched = new LinkedHashMap<>(task);
    enriched.put("creativeMobilePreviews", List.copyOf(previews));
    return enriched;
  }

  /** Localiza a derivada efêmera no mesmo diretório gerenciado do arquivo original. */
  static Path previewPath(Path source) {
    return source.resolveSibling(source.getFileName().toString() + ".mobile-393.png");
  }

  /** Calcula o vínculo entre os bytes originais e a redução reproduzível. */
  private static String sha(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (java.security.NoSuchAlgorithmException ex) {
      log.error("SHA-256 indisponível na prévia criativa mobile.", ex);
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }
}
