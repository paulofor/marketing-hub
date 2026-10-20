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

  /**
   * Confere nos pixels que uma quebra editorial não é substituída pelo empacotamento de palavras.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "O convite chegou.,E o look?",
    "Sua próxima ocasião.,Comece por aqui."
  })
  void preservesDeliberateLineBreaks(String first, String second) {
    var actual = new BufferedImage(1080, 250, BufferedImage.TYPE_INT_RGB);
    var expected = new BufferedImage(1080, 250, BufferedImage.TYPE_INT_RGB);
    var g = actual.createGraphics();
    var reference = expected.createGraphics();
    try {
      g.setColor(Color.WHITE);
      reference.setColor(Color.WHITE);
      reference.setFont(new java.awt.Font("DejaVu Sans", java.awt.Font.BOLD, 64));
      var metrics = reference.getFontMetrics();
      reference.drawString(first, 48, metrics.getAscent());
      reference.drawString(second, 48, metrics.getAscent() + metrics.getHeight());
      ProofCardRenderer.text(g, first + "\n" + second, 48, 0, 984, 166, 64, java.awt.Font.BOLD);
      assertThat(actual.getRGB(0, 0, 1080, 250, null, 0, 1080))
          .isEqualTo(expected.getRGB(0, 0, 1080, 250, null, 0, 1080));
      assertThatThrownBy(
              () ->
                  ProofCardRenderer.text(
                      g, first + "\n" + second, 48, 0, 984, 70, 64, java.awt.Font.BOLD))
          .hasMessageContaining("excede");
    } finally {
      g.dispose();
      reference.dispose();
    }
  }

  /** Compara os identificadores privados ao texto da ação, ambos prometidos com fonte mínima 36. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {910118L, 910219L})
  void privateIdentifiersRemainAsReadableAsTheAction(long artifactId) throws Exception {
    var input =
        spec()
            .put("sourceArtifactId", artifactId)
            .put("brandLabel", "EXEMPLO")
            .put("eyebrow", "EXEMPLO")
            .put("ctaText", "EXEMPLO");
    var image = ImageIO.read(new ByteArrayInputStream(renderer.render(input, source(), true)));
    var accent = Color.decode(input.path("accentColor").asText());
    int top = actionTop(image, accent);
    int actionInkHeight = colorHeight(image, Color.WHITE, top, top + 90);
    var identifiers = textBandHeights(image, accent, top - 100);
    assertThat(identifiers).hasSize(3);
    assertThat(identifiers)
        .allSatisfy(height -> assertThat(height).isGreaterThanOrEqualTo(actionInkHeight));
  }

  /** Mede a altura dos glifos coloridos, independentemente da posição exata no template. */
  private static int colorHeight(BufferedImage image, Color color, int from, int to) {
    int first = Integer.MAX_VALUE, last = -1;
    for (int y = from; y < to; y++) {
      for (int x = 64; x < 1016; x++) {
        if (image.getRGB(x, y) == color.getRGB()) {
          first = Math.min(first, y);
          last = y;
        }
      }
    }
    return last < 0 ? 0 : last - first + 1;
  }

  /** Agrupa linhas próximas dos rótulos sem confundir prova, margens ou acentos das letras. */
  private static java.util.List<Integer> textBandHeights(BufferedImage image, Color color, int to) {
    var heights = new java.util.ArrayList<Integer>();
    int first = -1, last = -1;
    for (int y = 0; y < to; y++) {
      boolean ink = false;
      for (int x = 64; x < 1016; x++) {
        if (image.getRGB(x, y) == color.getRGB()) {
          ink = true;
          break;
        }
      }
      if (!ink) continue;
      if (first >= 0 && y - last > 7) {
        heights.add(last - first + 1);
        first = -1;
      }
      if (first < 0) first = y;
      last = y;
    }
    if (first >= 0) heights.add(last - first + 1);
    return heights;
  }

  /**
   * Garante que o limite da demonstração seja lido antes do convite à ação em produtos distintos.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {910118L, 910219L})
  void qualifiesPrivateActionBeforeTheButton(long artifactId) throws Exception {
    var input = spec().put("sourceArtifactId", artifactId);
    var image = ImageIO.read(new ByteArrayInputStream(renderer.render(input, source(), true)));
    int actionTop = actionTop(image, Color.decode(input.path("accentColor").asText()));
    assertThat(actionTop).isPositive();
    long qualificationInk = 0;
    for (int y = actionTop - 64; y < actionTop - 8; y++) {
      for (int x = 64; x < 1016; x++) {
        var color = new Color(image.getRGB(x, y));
        if (color.getRed() < 130 && color.getGreen() < 130 && color.getBlue() < 130)
          qualificationInk++;
      }
    }
    assertThat(qualificationInk)
        .as("A qualificação da demonstração deve anteceder o botão, sem depender da copy gerada")
        .isGreaterThan(300);
  }

  /** Localiza a ação pela faixa contínua de cor, sem depender das coordenadas do template. */
  private static int actionTop(BufferedImage image, Color accent) {
    for (int y = 0; y < image.getHeight(); y++) {
      int count = 0;
      for (int x = 64; x < 1016; x++) {
        if (image.getRGB(x, y) == accent.getRGB()) count++;
      }
      if (count > 850) return y;
    }
    throw new AssertionError("A imagem precisa conter um botão reconhecível.");
  }

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

  /**
   * Explicita o formato ao cliente sem imprimir o modo de revisão e preserva o formato comercial.
   */
  @Test
  void identifiesSiteWithoutLeakingReviewMode() throws Exception {
    var spec = spec().put("eyebrow", "Demonstração");
    assertThat(ProofCardRenderer.eyebrow(spec, true)).isEqualTo("DEMONSTRAÇÃO DO SITE");
    assertThat(ProofCardRenderer.eyebrow(spec, false)).isEqualTo("Demonstração");
  }

  /** Preserva o rótulo solicitado também na imagem privada, além da identificação obrigatória. */
  @Test
  void preservesRequestedCaptionInPrivatePixels() throws Exception {
    var first = spec().put("eyebrow", "Prévia da aplicação");
    var second = spec().put("eyebrow", "Detalhe da aplicação");
    assertThat(renderer.render(first, source(), true))
        .isNotEqualTo(renderer.render(second, source(), true));
    assertThat(ProofCardRenderer.eyebrow(first, true)).isEqualTo("DEMONSTRAÇÃO DO SITE");
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
    assertThat(second.getRGB(0, 0, 1080, 1252, null, 0, 1080))
        .isEqualTo(first.getRGB(0, 0, 1080, 1252, null, 0, 1080));
    assertThat(second.getRGB(0, 1252, 1080, 98, null, 0, 1080))
        .isNotEqualTo(first.getRGB(0, 1252, 1080, 98, null, 0, 1080));
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
      {"templateVersion":"PROOF_CARD_V1","sourceArtifactId":910118,"sourceSha256":"SOURCE_HASH","crop":{"x":0,"y":0,"width":800,"height":500},"brandLabel":"Produto de teste","eyebrow":"Demonstração","headline":"Primeiro resultado, pronto","body":"Uma pequena ação com o que você já tem.","ctaText":"Ver meu primeiro resultado","footer":"Condições na página de destino","backgroundColor":"#F8F3ED","accentColor":"#713953"}
      """);
  }

  /**
   * Recusa metadados internos na copy visível de diferentes produtos, mantendo privacidade
   * legítima.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "910118,ctaText,Experimentar Mira em privado",
    "910219,body,Acesso privado liberado",
    "910320,footer,Credencial de 7 dias",
    "910421,eyebrow,APLICAÇÃO WEB PRIVADA",
    "910522,headline,Aguardando homologação"
  })
  void rejectsInternalWorkflowCopy(long artifact, String field, String text) throws Exception {
    var input = spec().put("sourceArtifactId", artifact).put(field, text);
    assertThatThrownBy(() -> renderer.render(input, source(), true))
        .hasMessageContaining("Linguagem interna");
    assertThat(renderer.render(spec().put("body", "Sua privacidade é preservada."), source(), true))
        .isNotEmpty();
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
