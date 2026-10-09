package com.marketinghub.communicationagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: renderizar uma peça estática legível preservando a prova e os limites factuais.
 */
@Component
public class ProofCardRenderer {
  /** Compõe prova fiel com rótulos e ressalva factual também na preparação privada. */
  public byte[] render(JsonNode spec, byte[] source, boolean privateValidation) throws IOException {
    if (!"PROOF_CARD_V1".equals(spec.path("templateVersion").asText()))
      throw new IllegalArgumentException("Template criativo não suportado.");
    BufferedImage original = ImageIO.read(new ByteArrayInputStream(source));
    if (original == null) throw new IllegalArgumentException("Origem não é PNG decodificável.");
    JsonNode crop = spec.path("crop");
    int x = integer(crop, "x"),
        y = integer(crop, "y"),
        width = integer(crop, "width"),
        height = integer(crop, "height");
    if (x < 0
        || y < 0
        || width < 100
        || height < 100
        || (long) x + width > original.getWidth()
        || (long) y + height > original.getHeight())
      throw new IllegalArgumentException("O recorte está fora dos pixels aprovados.");
    double cropAspectRatio = (double) width / height;
    if (cropAspectRatio < 1.2 || cropAspectRatio > 2.2)
      throw new IllegalArgumentException(
          "O recorte não ocupa a área de prova com legibilidade; use proporção entre 1,2:1 e 2,2:1.");
    BufferedImage canvas = new BufferedImage(1080, 1350, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = canvas.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      Color background = color(spec, "backgroundColor"), accent = color(spec, "accentColor");
      g.setColor(background);
      g.fillRect(0, 0, 1080, 1350);
      g.setColor(accent);
      text(
          g,
          spec.path("brandLabel").asText(),
          64,
          privateValidation ? 34 : 66,
          952,
          42,
          28,
          Font.BOLD);
      text(
          g,
          eyebrow(spec, privateValidation),
          64,
          privateValidation ? 82 : 123,
          952,
          40,
          25,
          Font.PLAIN);
      g.setColor(new Color(35, 32, 34));
      text(
          g,
          spec.path("headline").asText(),
          64,
          privateValidation ? 132 : 183,
          952,
          privateValidation ? 134 : 152,
          privateValidation ? 56 : 64,
          Font.BOLD);
      text(
          g,
          spec.path("body").asText(),
          64,
          privateValidation ? 276 : 351,
          952,
          privateValidation ? 92 : 115,
          33,
          Font.PLAIN);
      if (privateValidation) {
        g.setColor(accent);
        text(g, spec.path("eyebrow").asText(), 64, 375, 952, 40, 25, Font.PLAIN);
      }
      int proofHeight = privateValidation ? 667 : 550;
      double scale = Math.min(952.0 / width, (double) proofHeight / height);
      int drawWidth = (int) Math.round(width * scale),
          drawHeight = (int) Math.round(height * scale);
      if (scale < 0.7)
        throw new IllegalArgumentException(
            "O recorte perde legibilidade no criativo; selecione um detalhe menor da prova.");
      int left = (1080 - drawWidth) / 2,
          top = (privateValidation ? 442 : 497) + (proofHeight - drawHeight) / 2;
      g.setColor(new Color(255, 255, 255));
      g.fillRoundRect(
          56, privateValidation ? 430 : 485, 968, privateValidation ? 691 : 574, 26, 26);
      g.drawImage(
          original,
          left,
          top,
          left + drawWidth,
          top + drawHeight,
          x,
          y,
          x + width,
          y + height,
          null);
      g.setColor(accent);
      g.fillRoundRect(
          64, privateValidation ? 1148 : 1104, 952, privateValidation ? 90 : 104, 22, 22);
      g.setColor(Color.WHITE);
      text(
          g,
          spec.path("ctaText").asText(),
          92,
          privateValidation ? 1160 : 1125,
          896,
          66,
          36,
          Font.BOLD);
      g.setColor(new Color(70, 64, 68));
      String footer = spec.path("footer").asText();
      if (privateValidation && footer.isBlank())
        throw new IllegalArgumentException("Texto obrigatório do criativo ausente.");
      text(
          g,
          privateValidation ? "Demonstração sintética · sem compra ou cobrança " + footer : footer,
          64,
          privateValidation ? 1252 : 1245,
          952,
          privateValidation ? 98 : 68,
          privateValidation ? 36 : 25,
          Font.PLAIN);
    } finally {
      g.dispose();
    }
    var output = new ByteArrayOutputStream();
    ImageIO.write(canvas, "png", output);
    return output.toByteArray();
  }

  /** Nomeia explicitamente a aplicação privada sem esconder o formato real da entrega. */
  static String eyebrow(JsonNode spec, boolean privateValidation) {
    return privateValidation ? "APLICAÇÃO WEB PRIVADA" : spec.path("eyebrow").asText();
  }

  /** Exige coordenadas inteiras para registrar um recorte reproduzível. */
  private static int integer(JsonNode value, String field) {
    if (!value.path(field).isIntegralNumber())
      throw new IllegalArgumentException("Coordenada de recorte inválida: " + field);
    return value.path(field).asInt(-1);
  }

  /** Aceita somente cores hexadecimais explícitas do briefing. */
  private static Color color(JsonNode spec, String field) {
    String value = spec.path(field).asText();
    if (!value.matches("#[0-9a-fA-F]{6}"))
      throw new IllegalArgumentException("Cor criativa inválida: " + field);
    return Color.decode(value);
  }

  /** Quebra linhas sem reduzir fonte ou cortar texto para esconder transbordamento. */
  private static void text(
      Graphics2D g, String value, int x, int top, int width, int height, int size, int style) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("Texto obrigatório do criativo ausente.");
    g.setFont(new Font("DejaVu Sans", style, size));
    FontMetrics metrics = g.getFontMetrics();
    List<String> lines = new ArrayList<>();
    String line = "";
    for (String word : value.strip().split("\\s+")) {
      if (metrics.stringWidth(word) > width)
        throw new IllegalArgumentException("Palavra não cabe no criativo.");
      String candidate = line.isEmpty() ? word : line + " " + word;
      if (metrics.stringWidth(candidate) > width) {
        lines.add(line);
        line = word;
      } else line = candidate;
    }
    lines.add(line);
    if (lines.size() * metrics.getHeight() > height)
      throw new IllegalArgumentException("Texto excede a área legível do criativo: " + value);
    for (int i = 0; i < lines.size(); i++)
      g.drawString(lines.get(i), x, top + metrics.getAscent() + i * metrics.getHeight());
  }
}
