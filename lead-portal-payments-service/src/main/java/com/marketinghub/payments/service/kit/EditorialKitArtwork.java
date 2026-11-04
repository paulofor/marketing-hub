package com.marketinghub.payments.service.kit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Compõe a referência editorial v3 com hierarquia e áreas de leitura próprias de cada formato. */
public final class EditorialKitArtwork {
  private static final int WIDTH = 1080;
  private static final int MARGIN = 64;
  private static final int CONTENT = WIDTH - 2 * MARGIN;
  private static final Color INK = new Color(47, 37, 42);
  private static final Color SECONDARY = new Color(91, 77, 80);

  /** Impede instâncias de um compositor determinístico sem estado. */
  private EditorialKitArtwork() {}

  /** Preserva foto e textos integrais, recusando entradas que não caibam com leitura mínima. */
  public static Path render(
      Path output,
      int height,
      BufferedImage photo,
      String name,
      String contact,
      String service,
      String headline,
      int variant,
      double focusY)
      throws IOException {
    if ((height != 1080 && height != 1920) || photo == null || focusY < 0 || focusY > 1)
      throw new IOException("Formato ou enquadramento editorial inválido.");
    boolean story = height == 1920;
    var image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
    var g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setColor(new Color(249, 245, 238));
      g.fillRect(0, 0, WIDTH, height);
      Color accent = variant % 2 == 0 ? new Color(102, 57, 78) : new Color(42, 83, 67);

      text(
          g,
          name,
          new Rectangle(MARGIN, story ? 158 : 38, CONTENT, story ? 132 : 106),
          "SansSerif",
          Font.PLAIN,
          story ? 54 : 44,
          32,
          INK,
          false);
      g.setColor(accent);
      g.fillRect(MARGIN, story ? 312 : 153, 88, 4);
      Rectangle photoArea =
          story ? new Rectangle(MARGIN, 352, CONTENT, 802) : new Rectangle(520, 183, 496, 642);
      photo(g, photo, photoArea, focusY);

      text(
          g,
          service,
          story ? new Rectangle(MARGIN, 1200, CONTENT, 96) : new Rectangle(MARGIN, 233, 408, 132),
          "SansSerif",
          Font.PLAIN,
          story ? 40 : 34,
          34,
          accent,
          false);
      text(
          g,
          headline,
          story ? new Rectangle(MARGIN, 1318, CONTENT, 204) : new Rectangle(MARGIN, 385, 408, 400),
          "Serif",
          Font.PLAIN,
          story ? 86 : 64,
          story ? 62 : 48,
          INK,
          false);

      int actionY = story ? 1566 : 856;
      g.setColor(accent);
      g.fillRoundRect(MARGIN, actionY, CONTENT, story ? 96 : 72, 12, 12);
      text(
          g,
          "Converse sobre horários no WhatsApp",
          new Rectangle(MARGIN + 24, actionY + 10, CONTENT - 48, story ? 76 : 52),
          "SansSerif",
          Font.PLAIN,
          story ? 40 : 35,
          32,
          Color.WHITE,
          true);
      text(
          g,
          contact,
          new Rectangle(MARGIN, story ? 1694 : 946, CONTENT, story ? 96 : 52),
          "SansSerif",
          Font.PLAIN,
          story ? 36 : 34,
          34,
          SECONDARY,
          false);
      text(
          g,
          "Foto de referência · não é trabalho do profissional",
          new Rectangle(MARGIN, story ? 1810 : 1008, CONTENT, story ? 80 : 64),
          "SansSerif",
          Font.PLAIN,
          34,
          34,
          SECONDARY,
          false);
    } finally {
      g.dispose();
    }
    compactPixels(image);
    ImageIO.write(image, "png", output.toFile());
    return output;
  }

  /** Enquadra a mesma fotografia pela origem aprovada, sem distorcer sua proporção. */
  private static void photo(Graphics2D g, BufferedImage photo, Rectangle area, double focusY) {
    double scale =
        Math.max((double) area.width / photo.getWidth(), (double) area.height / photo.getHeight());
    int width = (int) Math.ceil(photo.getWidth() * scale);
    int height = (int) Math.ceil(photo.getHeight() * scale);
    g.setClip(area);
    g.drawImage(
        photo,
        area.x + (area.width - width) / 2,
        area.y - (int) ((height - area.height) * focusY),
        width,
        height,
        null);
    g.setClip(null);
  }

  /**
   * Ajusta por linhas e tamanho mínimo; nunca abrevia contato, aviso ou promessa silenciosamente.
   */
  private static void text(
      Graphics2D g,
      String value,
      Rectangle area,
      String family,
      int style,
      int maximum,
      int minimum,
      Color color,
      boolean centered)
      throws IOException {
    if (value == null || value.isBlank())
      throw new IOException("Texto editorial obrigatório ausente.");
    for (int size = maximum; size >= minimum; size--) {
      g.setFont(new Font(family, style, size));
      var metrics = g.getFontMetrics();
      List<String> lines = wrap(value, metrics, area.width);
      int lineHeight = metrics.getHeight();
      if (lines.stream().anyMatch(line -> metrics.stringWidth(line) > area.width)
          || lines.size() * lineHeight > area.height) continue;
      g.setColor(color);
      int y = area.y + (area.height - lines.size() * lineHeight) / 2 + metrics.getAscent();
      for (String line : lines) {
        int x = area.x + (centered ? (area.width - metrics.stringWidth(line)) / 2 : 0);
        g.drawString(line, x, y);
        y += lineHeight;
      }
      return;
    }
    throw new IOException(
        "Texto excede a área editorial com legibilidade mínima; revise o briefing.");
  }

  /** Quebra somente entre palavras e conserva caracteres, inclusive contatos sem espaços. */
  private static List<String> wrap(String value, FontMetrics metrics, int width) {
    var lines = new ArrayList<String>();
    String line = "";
    for (String word : value.strip().split("\\s+")) {
      String candidate = line.isEmpty() ? word : line + " " + word;
      if (!line.isEmpty() && metrics.stringWidth(candidate) > width) {
        lines.add(line);
        line = word;
      } else line = candidate;
    }
    if (!line.isEmpty()) lines.add(line);
    return lines;
  }

  /** Limita a quantização a dois níveis por canal para preservar o tamanho binário do pacote. */
  private static void compactPixels(BufferedImage image) {
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int rgb = image.getRGB(x, y);
        int r = Math.min(255, (((rgb >> 16) & 255) + 2) / 4 * 4);
        int g = Math.min(255, (((rgb >> 8) & 255) + 2) / 4 * 4);
        int b = Math.min(255, ((rgb & 255) + 2) / 4 * 4);
        image.setRGB(x, y, (r << 16) | (g << 8) | b);
      }
    }
  }
}
