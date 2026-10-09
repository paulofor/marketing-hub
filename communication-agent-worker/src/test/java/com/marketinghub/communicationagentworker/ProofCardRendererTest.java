package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: comprovar pixels, recorte, legibilidade e rastreabilidade da peça renderizada.
 */
class ProofCardRendererTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ProofCardRenderer renderer = new ProofCardRenderer();

  /** Produz uma peça real e preserva exatamente o detalhe aprovado em vez de redesenhá-lo. */
  @Test
  void rendersPngWithOriginalPixels() throws Exception {
    byte[] output = renderer.render(spec(), source(), true);
    var pixels = ImageIO.read(new ByteArrayInputStream(output));
    assertThat(pixels.getWidth()).isEqualTo(1080);
    assertThat(pixels.getHeight()).isEqualTo(1350);
    assertThat(pixels.getRGB(540, 772)).isEqualTo(Color.BLUE.getRGB());
    assertThat(IrisCreativeMaterializer.sha(output)).hasSize(64);
  }

  /** Explicita o formato da aplicação privada e preserva o rótulo recebido fora desse modo. */
  @Test
  void identifiesPrivateWebApplication() throws Exception {
    var spec = spec().put("eyebrow", "Demonstração");
    assertThat(ProofCardRenderer.eyebrow(spec, true)).isEqualTo("APLICAÇÃO WEB PRIVADA");
    assertThat(ProofCardRenderer.eyebrow(spec, false)).isEqualTo("Demonstração");
  }

  /** Preserva o rótulo solicitado também na imagem privada, além da identificação obrigatória. */
  @Test
  void preservesRequestedCaptionInPrivatePixels() throws Exception {
    var first = spec().put("eyebrow", "Prévia da aplicação");
    var second = spec().put("eyebrow", "Detalhe da aplicação");
    assertThat(renderer.render(first, source(), true))
        .isNotEqualTo(renderer.render(second, source(), true));
    assertThat(ProofCardRenderer.eyebrow(first, true)).isEqualTo("APLICAÇÃO WEB PRIVADA");
  }

  /**
   * Preserva a ressalva factual de produtos distintos sem substituir o aviso privado ou a prova.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "910118, Sem garantia de clientes ou agendamentos.",
    "910219, Informação educativa; não oferece diagnóstico."
  })
  void preservesFactualFooterInPrivatePixels(long artifactId, String footer) throws Exception {
    var baseline = spec().put("sourceArtifactId", artifactId);
    var corrected = baseline.deepCopy().put("footer", footer);
    byte[] before = renderer.render(baseline, source(), true);
    byte[] after = renderer.render(corrected, source(), true);
    assertThat(IrisCreativeMaterializer.sha(after))
        .isNotEqualTo(IrisCreativeMaterializer.sha(before));
    var first = ImageIO.read(new ByteArrayInputStream(before));
    var second = ImageIO.read(new ByteArrayInputStream(after));
    assertThat(second.getRGB(0, 0, 1080, 1295, null, 0, 1080))
        .isEqualTo(first.getRGB(0, 0, 1080, 1295, null, 0, 1080));
    assertThat(second.getRGB(0, 1295, 1080, 55, null, 0, 1080))
        .isNotEqualTo(first.getRGB(0, 1295, 1080, 55, null, 0, 1080));
    assertThat(second.getRGB(540, 772)).isEqualTo(Color.BLUE.getRGB());
  }

  /** Recusa a perda de ressalva factual também na preparação privada antes de persistir a peça. */
  @Test
  void rejectsMissingOrUnreadablePrivateFooter() throws Exception {
    var missing = spec().put("footer", "");
    assertThatThrownBy(() -> renderer.render(missing, source(), true))
        .hasMessageContaining("Texto obrigatório");
    var unreadable = spec().put("footer", "Ressalva muito longa ".repeat(30));
    assertThatThrownBy(() -> renderer.render(unreadable, source(), true))
        .hasMessageContaining("excede a área legível");
  }

  /** Mantém a ressalva comercial existente sem inserir rótulos da validação privada. */
  @Test
  void preservesCommercialFooterAndLayout() throws Exception {
    var firstSpec = spec().put("footer", "Sem garantia de clientes ou agendamentos.");
    var secondSpec =
        firstSpec.deepCopy().put("footer", "Condições da oferta na página de destino.");
    var first = ImageIO.read(new ByteArrayInputStream(renderer.render(firstSpec, source(), false)));
    var second =
        ImageIO.read(new ByteArrayInputStream(renderer.render(secondSpec, source(), false)));
    assertThat(second.getRGB(0, 0, 1080, 1245, null, 0, 1080))
        .isEqualTo(first.getRGB(0, 0, 1080, 1245, null, 0, 1080));
    assertThat(second.getRGB(0, 1245, 1080, 105, null, 0, 1080))
        .isNotEqualTo(first.getRGB(0, 1245, 1080, 105, null, 0, 1080));
    assertThat(second.getRGB(540, 772)).isEqualTo(Color.BLUE.getRGB());
  }

  /** Amplia a prova privada sem reduzir o conteúdo nem alterar o formato comercial anterior. */
  @Test
  void enlargesPrivateProofKeepingApprovedPixels() throws Exception {
    var privateImage =
        ImageIO.read(new ByteArrayInputStream(renderer.render(spec(), source(), true)));
    var publicImage =
        ImageIO.read(new ByteArrayInputStream(renderer.render(spec(), source(), false)));
    assertThat(bluePixels(privateImage)).isGreaterThan((long) (bluePixels(publicImage) * 1.15));
    assertThat(privateImage.getRGB(540, 772)).isEqualTo(Color.BLUE.getRGB());
    assertThat(publicImage.getRGB(540, 772)).isEqualTo(Color.BLUE.getRGB());
  }

  /** Conta os pixels da prova sintética sem depender do texto ou do identificador de produto. */
  private static long bluePixels(BufferedImage image) {
    return java.util.Arrays.stream(
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
        .filter(pixel -> pixel == Color.BLUE.getRGB())
        .count();
  }

  /** Recusa coordenadas, escala ilegível e textos que seriam truncados. */
  @Test
  void rejectsOutOfBoundsAndUnreadableContent() throws Exception {
    var outside = spec();
    ((ObjectNode) outside.path("crop")).put("x", 700);
    assertThatThrownBy(() -> renderer.render(outside, source(), true))
        .hasMessageContaining("fora dos pixels");
    var hugeText = spec().put("headline", "Texto longo ".repeat(30));
    assertThatThrownBy(() -> renderer.render(hugeText, source(), true))
        .hasMessageContaining("excede a área legível");
    var fractional = spec();
    ((ObjectNode) fractional.path("crop")).put("x", 0.5);
    assertThatThrownBy(() -> renderer.render(fractional, source(), true))
        .hasMessageContaining("Coordenada");
    var ultraWide = spec();
    ((ObjectNode) ultraWide.path("crop")).put("width", 800).put("height", 180);
    assertThatThrownBy(() -> renderer.render(ultraWide, source(), true))
        .hasMessageContaining("proporção entre 1,2:1 e 2,2:1");
  }

  /** Gera uma prévia privada a partir dos bytes aprovados quando a homologação fornece a origem. */
  @Test
  void exportsReviewableRealSourceWhenProvided() throws Exception {
    String path = System.getenv("VEGA_CREATIVE_SOURCE");
    if (path == null) return;
    var spec = spec();
    ((ObjectNode) spec.path("crop"))
        .put("x", 379)
        .put("y", 333)
        .put("width", 682)
        .put("height", 553);
    spec.put("brandLabel", "MUSA")
        .put("headline", "Seu primeiro ajuste, pronto")
        .put("body", "Uma pequena mudança, com o que você já tem.")
        .put("ctaText", "Ver meu primeiro ajuste");
    byte[] output = renderer.render(spec, Files.readAllBytes(Path.of(path)), true);
    Path destination = Path.of(System.getenv("VEGA_CREATIVE_PREVIEW"));
    Files.createDirectories(destination.getParent());
    Files.write(destination, output);
  }

  /** Reproduz a composição com arquivos locais conferidos sem enviar artefatos de QA ao backend. */
  @Test
  void replaysApprovedSourceAndExportsLocalComposition() throws Exception {
    String source = System.getProperty("creative.replay.source");
    org.junit.jupiter.api.Assumptions.assumeTrue(
        source != null, "Replay opcional com fonte aprovada");
    var spec = json.readTree(Files.readString(Path.of(System.getProperty("creative.replay.spec"))));
    byte[] rendered = renderer.render(spec, Files.readAllBytes(Path.of(source)), true);
    var image = ImageIO.read(new ByteArrayInputStream(rendered));
    assertThat(image.getWidth()).isEqualTo(1080);
    assertThat(image.getHeight()).isEqualTo(1350);
    Files.write(Path.of(System.getProperty("creative.replay.output")), rendered);
  }

  /** Monta um briefing sintético curto dentro da área legível do template. */
  static ObjectNode spec() throws Exception {
    return (ObjectNode)
        new ObjectMapper()
            .readTree(
                """
      {"templateVersion":"PROOF_CARD_V1","sourceArtifactId":910118,"sourceSha256":"SOURCE_HASH","crop":{"x":0,"y":0,"width":800,"height":500},"brandLabel":"Produto de teste","eyebrow":"Demonstração","headline":"Primeiro resultado, pronto","body":"Uma pequena ação com o que você já tem.","ctaText":"Ver meu primeiro resultado","footer":"Demonstração privada","backgroundColor":"#F8F3ED","accentColor":"#713953"}
      """);
  }

  /** Produz pixels locais segregados para detectar alteração indevida da prova original. */
  static byte[] source() throws Exception {
    var pixels = new BufferedImage(800, 500, BufferedImage.TYPE_INT_RGB);
    var g = pixels.createGraphics();
    g.setColor(Color.BLUE);
    g.fillRect(0, 0, 800, 500);
    g.dispose();
    var output = new ByteArrayOutputStream();
    ImageIO.write(pixels, "png", output);
    return output.toByteArray();
  }
}
