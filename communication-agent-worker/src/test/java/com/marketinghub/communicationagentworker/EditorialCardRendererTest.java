package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: conferir pixels ilustrativos, limites visíveis e origem auditada sem
 * inferência.
 */
class EditorialCardRendererTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Confere nos pixels a distinção entre ilustração e demonstração antes da ação. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"Look para a Ocasião", "Outra ocasião"})
  void distinguishesEditorialImageFromSiteResultBeforeAction(String brand) throws Exception {
    byte[] png = ProofCardRendererTest.source();
    var input = spec(png, 800, 500).put("brandLabel", brand);
    var actual =
        javax.imageio.ImageIO.read(
            new java.io.ByteArrayInputStream(EditorialCardRenderer.render(input, png)));
    var expected =
        new java.awt.image.BufferedImage(984, 110, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var g = expected.createGraphics();
    try {
      g.setRenderingHint(
          java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(
          java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
          java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(java.awt.Color.decode(input.path("backgroundColor").asText()));
      g.fillRect(0, 0, 984, 110);
      g.setColor(java.awt.Color.decode(input.path("accentColor").asText()));
      g.setFont(new java.awt.Font("DejaVu Sans", java.awt.Font.PLAIN, 36));
      var metrics = g.getFontMetrics();
      String first = "Ilustração por IA; não é resultado do site.";
      String second = "No site: exemplo com dados de teste, sem cobrança.";
      assertThat(metrics.stringWidth(first)).isLessThanOrEqualTo(984);
      assertThat(metrics.stringWidth(second)).isLessThanOrEqualTo(984);
      g.drawString(first, 0, metrics.getAscent());
      g.drawString(second, 0, metrics.getAscent() + metrics.getHeight());
      assertThat(actual.getRGB(48, 1032, 984, 110, null, 0, 984))
          .isEqualTo(expected.getRGB(0, 0, 984, 110, null, 0, 984));
    } finally {
      g.dispose();
    }
  }

  /** Recusa fonte adulterada, papel fictício e recorte ilegível antes de produzir PNG. */
  @Test
  void preservesProvenanceAndRejectsInvalidCrops() throws Exception {
    byte[] png = ProofCardRendererTest.source();
    var spec = spec(png, 800, 500);
    byte[] rendered = EditorialCardRenderer.render(spec, png);
    var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(rendered));
    assertThat(image.getWidth()).isEqualTo(1080);
    assertThat(image.getHeight()).isEqualTo(1350);
    var illustration = (ObjectNode) spec.path("illustration");
    illustration.put("sha256", "0".repeat(64));
    assertThatThrownBy(() -> EditorialCardRenderer.render(spec, png))
        .hasMessageContaining("auditada");
    illustration.put("sha256", IrisCreativeMaterializer.sha(png)).put("role", "PRODUCT_PROOF");
    assertThatThrownBy(() -> EditorialCardRenderer.render(spec, png))
        .hasMessageContaining("auditada");
    illustration.put("role", "GENERATED_ILLUSTRATION");
    ((ObjectNode) illustration.path("crop")).put("x", 99999);
    assertThatThrownBy(() -> EditorialCardRenderer.render(spec, png))
        .hasMessageContaining("pixels");
  }

  /** Reproduz imagem e, quando fornecida, especificação reais sem upload ou nova inferência. */
  @Test
  void replaysAuditedImageLocally() throws Exception {
    String path = System.getenv("IRIS_ILLUSTRATION_FILE");
    org.junit.jupiter.api.Assumptions.assumeTrue(path != null, "Imagem auditada local opcional.");
    byte[] png = Files.readAllBytes(Path.of(path));
    var original = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
    var spec = spec(png, original.getWidth(), (int) (original.getHeight() * 0.67));
    ((ObjectNode) spec.path("illustration").path("crop"))
        .put("y", (int) (original.getHeight() * 0.12));
    String specPath = System.getenv("IRIS_RENDER_SPEC");
    if (specPath != null) spec = (ObjectNode) json.readTree(Files.readString(Path.of(specPath)));
    byte[] rendered = EditorialCardRenderer.render(spec, png);
    Path output = Path.of(System.getenv("IRIS_RENDER_OUTPUT"));
    Files.write(output, rendered);
    Files.writeString(Path.of(output + ".json"), spec.toPrettyString());
    assertThat(rendered.length).isGreaterThan(10000);
  }

  /** Monta uma proposta local cuja ilustração permanece distinta da prova funcional. */
  ObjectNode spec(byte[] png, int width, int height) {
    var s =
        json.createObjectNode()
            .put("templateVersion", "EDITORIAL_CARD_V1")
            .put("brandLabel", "Look para a Ocasião")
            .put("headline", "Um convite.\nTrês jeitos de ir.")
            .put("body", "Explore combinações para a ocasião\ncom as peças que você tem.")
            .put("eyebrow", "Inspiração para começar")
            .put("ctaText", "Escolher minhas combinações")
            .put("footer", "Personalização não comprovada.")
            .put("backgroundColor", "#F5F1E9")
            .put("accentColor", "#263A50");
    s.putObject("illustration")
        .put("jobId", "img-sandbox")
        .put("sha256", IrisCreativeMaterializer.sha(png))
        .put("role", "GENERATED_ILLUSTRATION")
        .putObject("crop")
        .put("x", 0)
        .put("y", 0)
        .put("width", width)
        .put("height", height);
    return s;
  }
}
