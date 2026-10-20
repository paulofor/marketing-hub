package com.marketinghub.communicationagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

/** Responsabilidade: compor uma ilustração auditada sem apresentá-la como prova personalizada. */
final class EditorialCardRenderer {
  /** Impede instanciação do renderizador sem estado. */
  private EditorialCardRenderer() {}

  /** Preserva os pixels da ilustração e imprime sua natureza antes do convite. */
  static byte[] render(JsonNode spec, byte[] bytes) throws IOException {
    ProofCardRenderer.validateCustomerCopy(spec);
    var illustration = spec.path("illustration");
    if (!"EDITORIAL_CARD_V1".equals(spec.path("templateVersion").asText())
        || !"GENERATED_ILLUSTRATION".equals(illustration.path("role").asText())
        || !IrisCreativeMaterializer.sha(bytes).equals(illustration.path("sha256").asText()))
      throw new IllegalArgumentException("Composição sem ilustração auditada.");
    var original = ImageIO.read(new ByteArrayInputStream(bytes));
    if (original == null) throw new IllegalArgumentException("Ilustração não decodificável.");
    var crop = illustration.path("crop");
    for (String field : List.of("x", "y", "width", "height"))
      if (!crop.path(field).isIntegralNumber() || !crop.path(field).canConvertToInt())
        throw new IllegalArgumentException("Recorte ilustrativo deve usar pixels inteiros.");
    int x = crop.path("x").asInt(),
        y = crop.path("y").asInt(),
        w = crop.path("width").asInt(),
        h = crop.path("height").asInt();
    if (x < 0
        || y < 0
        || w < 300
        || h < 300
        || (long) x + w > original.getWidth()
        || (long) y + h > original.getHeight())
      throw new IllegalArgumentException("Recorte fora dos pixels ilustrativos.");
    double scale = Math.min(984.0 / w, 640.0 / h);
    if (scale < 0.6 || (w * scale) * (h * scale) < 984 * 640 * 0.75)
      throw new IllegalArgumentException("Ilustração pequena ou pouco legível na composição.");
    var canvas = new BufferedImage(1080, 1350, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = canvas.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      Color accent = ProofCardRenderer.color(spec, "accentColor");
      g.setColor(ProofCardRenderer.color(spec, "backgroundColor"));
      g.fillRect(0, 0, 1080, 1350);
      g.setColor(accent);
      ProofCardRenderer.text(g, spec.path("brandLabel").asText(), 48, 35, 984, 52, 36, Font.BOLD);
      ProofCardRenderer.text(g, spec.path("headline").asText(), 48, 110, 984, 166, 64, Font.BOLD);
      ProofCardRenderer.text(g, spec.path("body").asText(), 48, 280, 984, 95, 36, Font.PLAIN);
      ProofCardRenderer.text(g, spec.path("eyebrow").asText(), 48, 373, 984, 48, 36, Font.PLAIN);
      int width = (int) Math.round(w * scale), height = (int) Math.round(h * scale);
      int left = (1080 - width) / 2, top = 425 + (640 - height) / 2;
      g.setColor(Color.WHITE);
      g.fillRoundRect(40, 420, 1000, 650, 24, 24);
      g.drawImage(original, left, top, left + width, top + height, x, y, x + w, y + h, null);
      g.setColor(accent);
      ProofCardRenderer.text(
          g, "Ilustração por IA · exemplo sem cobrança.", 48, 1082, 984, 50, 36, Font.PLAIN);
      g.fillRoundRect(48, 1150, 984, 95, 22, 22);
      g.setColor(Color.WHITE);
      ProofCardRenderer.text(g, spec.path("ctaText").asText(), 72, 1166, 936, 62, 36, Font.BOLD);
      g.setColor(accent);
      ProofCardRenderer.text(g, spec.path("footer").asText(), 48, 1262, 984, 86, 36, Font.PLAIN);
    } finally {
      g.dispose();
    }
    var out = new ByteArrayOutputStream();
    ImageIO.write(canvas, "png", out);
    return out.toByteArray();
  }
}
