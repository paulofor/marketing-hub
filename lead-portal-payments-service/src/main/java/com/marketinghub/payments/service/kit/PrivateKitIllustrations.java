package com.marketinghub.payments.service.kit;

import com.marketinghub.payments.integration.image.AgendaCheiaPhotoGenerator;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Responsabilidade: produzir ilustrações sintéticas e determinísticas exclusivas da prova privada.
 */
public final class PrivateKitIllustrations implements AgendaCheiaPhotoGenerator {
  /** Gera a ilustração do perfil canônico sem chamar uma biblioteca ou integração comercial. */
  @Override
  public BufferedImage generate(String executionId, int variant) {
    return generate(executionId, variant, CapellaKitCatalog.byCode(CapellaKitCatalog.NAILS));
  }

  /**
   * Renderiza formas reconhecíveis como demonstração, sem atribuí-las a fotografia de trabalho
   * real.
   */
  @Override
  public BufferedImage generate(String executionId, int variant, CapellaKitProfile profile) {
    BufferedImage image = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    Color base =
        CapellaKitCatalog.NAILS.equals(profile.code())
            ? new Color(176, 96 + variant * 7, 135)
            : new Color(42 + variant * 8, 67, 82);
    g.setPaint(new GradientPaint(0, 0, base, 1080, 1920, new Color(236, 214, 178)));
    g.fillRect(0, 0, 1080, 1920);
    for (int i = 0; i < 12; i++) {
      g.setColor(
          new Color(
              (variant * 21 + i * 31) % 160 + 60,
              (i * 19) % 140 + 80,
              (variant * 13 + i * 17) % 130 + 90));
      g.fillRoundRect(50 + i % 4 * 250, 200 + i / 4 * 280, 160 + variant * 3, 220, 80, 80);
    }
    g.setColor(new Color(255, 255, 255, 230));
    g.setFont(new Font("SansSerif", Font.BOLD, 32));
    g.drawString("ILUSTRAÇÃO SINTÉTICA • PROVA PRIVADA", 65, 180);
    g.dispose();
    return image;
  }
}
