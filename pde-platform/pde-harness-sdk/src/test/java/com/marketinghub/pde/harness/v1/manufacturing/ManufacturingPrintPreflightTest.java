package com.marketinghub.pde.harness.v1.manufacturing;

import static org.junit.jupiter.api.Assertions.*;

import com.marketinghub.pde.harness.v1.PdeConversationScope;
import com.marketinghub.pde.harness.v1.PdeCustomerScope;
import com.marketinghub.pde.harness.v1.internal.PdeHashing;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Exercita bytes reais e fronteiras do preflight sem provedor, cobrança ou evidência comercial. */
class ManufacturingPrintPreflightTest {
  private final ManufacturingPrintPreflight preflight = new ManufacturingPrintPreflight();

  /** Aprova somente o controle técnico de PNG e JPEG com pixels adequados ao tamanho físico. */
  @ParameterizedTest
  @ValueSource(strings = {"png", "jpeg"})
  void checksRealPixelsWithoutAuthorizingProduction(String format) throws Exception {
    byte[] content = image(600, 900, BufferedImage.TYPE_INT_RGB, format);
    var result = inspect(content, spec(50.8, 76.2, 0, 300, false));
    assertTrue(result.technicalCheckPassed(), result.issues().toString());
    assertFalse(result.productionAuthorized());
    assertEquals(300, result.effectiveDpi(), 0.001);
    assertEquals(600, result.pixelWidth());
    assertEquals(900, result.pixelHeight());
    assertEquals(PdeHashing.sha256(content), result.artifactSha256());
    assertEquals(
        scope("tenant", "hotel-a", "cliente-a", "v1").fingerprint(), result.scopeFingerprint());
    assertEquals("manufacturing-print-preflight-v1", result.contractVersion());
  }

  /** Impede usar o número nominal de DPI para aprovar uma imagem pequena em impressão grande. */
  @Test
  void rejectsLowEffectiveResolution() throws Exception {
    assertTrue(
        inspect(image(60, 90, BufferedImage.TYPE_INT_RGB, "png"), spec(50.8, 76.2, 0, 300, false))
            .issues()
            .contains("INSUFFICIENT_RESOLUTION"));
  }

  /** Considera sangria e não corta silenciosamente a proporção incompatível. */
  @Test
  void checksBleedAndAspectRatio() throws Exception {
    byte[] content = image(600, 900, BufferedImage.TYPE_INT_RGB, "png");
    var result = inspect(content, spec(50.8, 76.2, 3, 300, false));
    assertTrue(result.issues().contains("INSUFFICIENT_RESOLUTION"));
    assertTrue(result.issues().contains("ASPECT_RATIO_MISMATCH"));
  }

  /** Aceita limite definido pela ficha sem impor trezentos DPI a todos os fabricantes. */
  @Test
  void usesSupplierSpecificThreshold() throws Exception {
    assertTrue(
        inspect(image(300, 450, BufferedImage.TYPE_INT_RGB, "png"), spec(50.8, 76.2, 0, 150, false))
            .technicalCheckPassed());
  }

  /** Rejeita referência ao hash de outro arquivo, ainda que os dois sejam imagens válidas. */
  @Test
  void rejectsSwappedFile() throws Exception {
    var scope = scope("tenant", "hotel-a", "cliente-a", "v1");
    var result =
        preflight.inspect(
            scope,
            scope,
            image(600, 900, BufferedImage.TYPE_INT_RGB, "png"),
            "0".repeat(64),
            spec(50.8, 76.2, 0, 300, false));
    assertEquals(List.of("HASH_MISMATCH"), result.issues());
    assertFalse(result.productionAuthorized());
  }

  /** Bloqueia troca de tenant, produto, cliente, versão ou conversa antes de ler bytes. */
  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4})
  void rejectsCrossScope(int changed) throws Exception {
    String[] parts = {"tenant", "hotel-a", "cliente-a", "v1", "brief-1"};
    parts[changed] += "-outro";
    var other =
        new PdeConversationScope(
            new PdeCustomerScope(parts[0], parts[1], parts[2]), parts[3], parts[4]);
    var result =
        preflight.inspect(
            scope("tenant", "hotel-a", "cliente-a", "v1"),
            other,
            image(600, 900, BufferedImage.TYPE_INT_RGB, "png"),
            "irrelevante",
            spec(50.8, 76.2, 0, 300, false));
    assertEquals(List.of("SCOPE_MISMATCH"), result.issues());
    assertEquals("", result.artifactSha256());
  }

  /** Reutiliza o controle em outro produto e cliente, sem exceção por identificador. */
  @Test
  void supportsAnotherProductAndDeterministicReplay() throws Exception {
    var scope = scope("tenant-b", "colecao-b", "cliente-b", "v7");
    byte[] content = image(600, 900, BufferedImage.TYPE_INT_RGB, "png");
    var first =
        preflight.inspect(
            scope, scope, content, PdeHashing.sha256(content), spec(50.8, 76.2, 0, 300, false));
    var second =
        preflight.inspect(
            scope, scope, content, PdeHashing.sha256(content), spec(50.8, 76.2, 0, 300, false));
    assertTrue(first.technicalCheckPassed());
    assertEquals(first, second);
  }

  /** Não confunde SVG, HTML ou arquivo vazio com arquivo pronto para impressão plana. */
  @Test
  void rejectsMissingAndUnsupportedContent() {
    assertFalse(inspect(new byte[0], spec(10, 10, 0, 300, false)).technicalCheckPassed());
    assertEquals(
        List.of("UNSUPPORTED_IMAGE"),
        inspect("<svg/>".getBytes(), spec(10, 10, 0, 300, false)).issues());
    assertEquals(
        List.of("INVALID_FILE_SIZE"),
        inspect(new byte[26 * 1024 * 1024], spec(10, 10, 0, 300, false)).issues());
  }

  /** Impede descompressão excessiva antes de alocar a imagem inteira. */
  @Test
  void rejectsDecompressionBombDimensions() throws Exception {
    byte[] content = image(10, 10, BufferedImage.TYPE_INT_RGB, "png");
    ByteBuffer.wrap(content).putInt(16, 100_000).putInt(20, 100_000);
    assertEquals(List.of("PIXEL_LIMIT"), inspect(content, spec(10, 10, 0, 300, false)).issues());
  }

  /** Arquivo truncado não recebe aprovação somente por possuir um cabeçalho reconhecível. */
  @Test
  void rejectsTruncatedImage() throws Exception {
    byte[] content = image(600, 900, BufferedImage.TYPE_INT_RGB, "png");
    assertFalse(
        inspect(java.util.Arrays.copyOf(content, 40), spec(50.8, 76.2, 0, 300, false))
            .technicalCheckPassed());
  }

  /** Distingue transparência real de canal alfa opaco e respeita a ficha. */
  @Test
  void checksTransparency() throws Exception {
    byte[] transparent = image(600, 900, BufferedImage.TYPE_INT_ARGB, "png");
    assertTrue(
        inspect(transparent, spec(50.8, 76.2, 0, 300, false))
            .issues()
            .contains("TRANSPARENCY_NOT_ALLOWED"));
    assertTrue(inspect(transparent, spec(50.8, 76.2, 0, 300, true)).technicalCheckPassed());
    var opaque = new BufferedImage(600, 900, BufferedImage.TYPE_INT_ARGB);
    var graphics = opaque.createGraphics();
    graphics.setColor(java.awt.Color.WHITE);
    graphics.fillRect(0, 0, 600, 900);
    graphics.dispose();
    assertTrue(
        inspect(bytes(opaque, "png"), spec(50.8, 76.2, 0, 300, false)).technicalCheckPassed());
  }

  /** Não libera perfil de cor fora do recorte sRGB suportado. */
  @Test
  void requiresColorReview() throws Exception {
    assertTrue(
        inspect(
                image(600, 900, BufferedImage.TYPE_BYTE_GRAY, "png"),
                spec(50.8, 76.2, 0, 300, false))
            .issues()
            .contains("COLOR_PROFILE_REQUIRES_REVIEW"));
  }

  /** Rejeita números não físicos e ficha sem origem verificável. */
  @ParameterizedTest
  @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY})
  void rejectsInvalidSpecification(double value) {
    assertThrows(IllegalArgumentException.class, () -> spec(value, 50, 0, 300, false));
    assertThrows(IllegalArgumentException.class, () -> spec(50, 50, 0, value, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PrintSpecification("", "sku", 50, 50, 0, 300, false));
  }

  /** Invoca o preflight com escopo sintético e hash calculado pelos bytes reais. */
  private PrintPreflightResult inspect(byte[] content, PrintSpecification spec) {
    var scope = scope("tenant", "hotel-a", "cliente-a", "v1");
    return preflight.inspect(scope, scope, content, PdeHashing.sha256(content), spec);
  }

  /** Monta ficha sintética explicitamente diferente de uma homologação de fornecedor real. */
  private PrintSpecification spec(
      double width, double height, double bleed, double dpi, boolean alpha) {
    return new PrintSpecification(
        "TEST_ONLY:fixture-v1", "TEST-PRINT", width, height, bleed, dpi, alpha);
  }

  /** Cria contexto separado dos produtos comerciais existentes. */
  private PdeConversationScope scope(
      String tenant, String product, String customer, String version) {
    return new PdeConversationScope(
        new PdeCustomerScope(tenant, product, customer), version, "brief-1");
  }

  /** Cria imagem sintética sem modelo pago nem conteúdo de cliente. */
  private byte[] image(int width, int height, int type, String format) throws Exception {
    return bytes(new BufferedImage(width, height, type), format);
  }

  /** Codifica bytes pelo encoder real para exercitar a leitura de imagem. */
  private byte[] bytes(BufferedImage image, String format) throws Exception {
    var output = new ByteArrayOutputStream();
    assertTrue(ImageIO.write(image, format, output));
    return output.toByteArray();
  }
}
