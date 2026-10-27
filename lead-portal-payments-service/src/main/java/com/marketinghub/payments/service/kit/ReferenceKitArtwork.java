package com.marketinghub.payments.service.kit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Responsabilidade: compor a demonstração fotográfica com hierarquia legível e atribuição honesta.
 */
public final class ReferenceKitArtwork {
  /** Impede instâncias de um compositor sem estado. */
  private ReferenceKitArtwork() {}

  /** Preserva a fotografia e aplica nome, serviço e convite em áreas próprias de alto contraste. */
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
    int width = 1080;
    var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    Color ink = new Color(38, 31, 39);
    g.setColor(new Color(249, 245, 238));
    g.fillRect(0, 0, width, height);
    g.setColor(ink);
    fit(g, name, 68, 56, 86, 968);
    fit(g, contact, 36, 56, 140, 968);
    int photoY = 180, photoHeight = height - 590;
    double scale = Math.max(968.0 / photo.getWidth(), (double) photoHeight / photo.getHeight());
    int w = (int) Math.ceil(photo.getWidth() * scale),
        h = (int) Math.ceil(photo.getHeight() * scale);
    g.setClip(56, photoY, 968, photoHeight);
    g.drawImage(photo, 56 + (968 - w) / 2, photoY - (int) ((h - photoHeight) * focusY), w, h, null);
    g.setClip(null);
    g.setColor(ink);
    fit(g, service, 30, 56, height - 365, 968);
    drawHeadline(g, headline, 56, height - 280);
    Color accent = variant % 2 == 0 ? new Color(92, 53, 77) : new Color(39, 80, 64);
    g.setColor(accent);
    g.fillRoundRect(56, height - 145, 968, 72, 20, 20);
    g.setColor(Color.WHITE);
    fit(g, "Converse sobre horários no WhatsApp", 38, 88, height - 97, 904);
    g.setColor(ink);
    fit(g, "Foto de referência · não é trabalho do profissional", 27, 56, height - 30, 968);
    g.dispose();
    compactPixels(image);
    ImageIO.write(image, "png", output.toFile());
    return output;
  }

  /**
   * Reduz apenas dois níveis por canal no máximo, mantendo PNG legível dentro do limite do pacote.
   */
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

  /** Preserva tamanho mínimo legível e abrevia textos extensos sem ultrapassar a área. */
  private static void fit(Graphics2D g, String text, int size, int x, int y, int width) {
    int minimum = Math.max(24, (int) Math.ceil(size * 0.75));
    g.setFont(new Font("SansSerif", Font.BOLD, size));
    while (size > minimum && g.getFontMetrics().stringWidth(text) > width)
      g.setFont(new Font("SansSerif", Font.BOLD, --size));
    String fitted = text;
    while (g.getFontMetrics().stringWidth(fitted) > width && fitted.length() > 2)
      fitted = fitted.substring(0, fitted.length() - 2) + "…";
    g.drawString(fitted, x, y);
  }

  /** Quebra a mensagem em até duas linhas antes da chamada, sem sobreposição. */
  private static void drawHeadline(Graphics2D g, String text, int x, int y) {
    g.setFont(new Font("Serif", Font.BOLD, 64));
    StringBuilder line = new StringBuilder();
    int row = 0;
    for (String word : text.split("\\s+")) {
      String candidate = line.isEmpty() ? word : line + " " + word;
      if (g.getFontMetrics().stringWidth(candidate) > 968 && !line.isEmpty() && row == 0) {
        g.drawString(line.toString(), x, y);
        line = new StringBuilder(word);
        row++;
      } else line = new StringBuilder(candidate);
    }
    int size = 64;
    while (g.getFontMetrics().stringWidth(line.toString()) > 968 && size > 28)
      g.setFont(new Font("Serif", Font.BOLD, --size));
    g.drawString(line.toString(), x, y + row * 74);
  }
}
