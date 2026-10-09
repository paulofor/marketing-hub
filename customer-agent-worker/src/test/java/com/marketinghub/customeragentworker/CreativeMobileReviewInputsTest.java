package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: comprovar a prévia mobile e seu vínculo com os pixels efetivamente revisados.
 */
class CreativeMobileReviewInputsTest {
  @TempDir Path directory;

  /**
   * Anexa original e redução real para identidades independentes sem duplicar o artefato aprovado.
   */
  @ParameterizedTest
  @ValueSource(longs = {91018L, 93276L})
  void suppliesRealMobilePixelsAndAuditableSource(long artifactId) throws Exception {
    var image = source(artifactId);
    byte[] original = Files.readAllBytes(Path.of(image.localPath()));
    var task =
        Map.<String, Object>of(
            "processCode", "creative-production-approval", "taskId", artifactId + 1);
    var enriched = CreativeMobileReviewInputs.enrich(task, List.of(image));
    Path preview = CreativeMobileReviewInputs.previewPath(Path.of(image.localPath()));
    var pixels = ImageIO.read(preview.toFile());
    assertThat(pixels.getWidth()).isEqualTo(393);
    assertThat(pixels.getHeight()).isEqualTo(491);
    assertThat(pixels.getRGB(100, 100)).isEqualTo(Color.BLUE.getRGB());
    assertThat(Files.readAllBytes(Path.of(image.localPath()))).isEqualTo(original);
    var previews = (List<?>) enriched.get("creativeMobilePreviews");
    assertThat(previews).hasSize(1);
    assertThat(previews.getFirst())
        .isEqualTo(
            Map.of(
                "sourceArtifactId",
                artifactId,
                "sourceSha256",
                image.sha256(),
                "previewSha256",
                sha(Files.readAllBytes(preview)),
                "width",
                393,
                "height",
                491,
                "transformVersion",
                "WIDTH_393_BICUBIC_V1"));
    var consumer =
        new CustomerBpmTaskConsumer(
            "http://backend:8000", "codex", "fixture", "max", "/workspace", "", new ObjectMapper());
    var command =
        consumer.command(
            directory.resolve("result.json"), directory.resolve("schema.json"), List.of(image));
    assertThat(command)
        .containsSubsequence("--image", image.localPath(), "--image", preview.toString());
    assertThat(CustomerBpmTaskConsumer.evidenceFields("Psique", "fixture", enriched))
        .containsEntry("creativeMobilePreviews", enriched.get("creativeMobilePreviews"));
  }

  /** Bloqueia a chamada antes do modelo quando falta a redução ou a origem mudou. */
  @Test
  void preventsPaidReviewWithoutVerifiedPreview() throws Exception {
    var image = source(94019L);
    var consumer =
        new CustomerBpmTaskConsumer(
            "http://backend:8000", "codex", "fixture", "max", "/workspace", "", new ObjectMapper());
    assertThatThrownBy(
            () ->
                consumer.command(
                    directory.resolve("result.json"),
                    directory.resolve("schema.json"),
                    List.of(image)))
        .hasMessageContaining("Prévia mobile criativa ausente");
    Files.writeString(Path.of(image.localPath()), "origem alterada");
    assertThatThrownBy(
            () ->
                CreativeMobileReviewInputs.enrich(
                    Map.of("processCode", "creative-production-approval"), List.of(image)))
        .hasMessageContaining("mudou antes");
  }

  /**
   * Mantém o fluxo de captura de páginas e revisões antes válidas fora da transformação criativa.
   */
  @Test
  void preservesOtherReviewContracts() throws Exception {
    var task = Map.<String, Object>of("processCode", "landing-page-generation", "taskId", 95031L);
    assertThat(CreativeMobileReviewInputs.enrich(task, List.of())).isSameAs(task);
  }

  /**
   * Reproduz a redução de uma peça fornecida, conferindo bytes e comando antes de qualquer modelo.
   */
  @Test
  void replaysSuppliedCreativeWithoutReplacingPersistedPixels() throws Exception {
    String sourcePath = System.getProperty("creative.mobile.source");
    Assumptions.assumeTrue(sourcePath != null, "Replay local opcional com peça fornecida.");
    byte[] original = Files.readAllBytes(Path.of(sourcePath));
    String expectedHash = System.getProperty("creative.mobile.sha256");
    assertThat(sha(original)).isEqualTo(expectedHash);
    Path copy = directory.resolve("creative.png");
    Files.write(copy, original);
    long artifactId = Long.parseLong(System.getProperty("creative.mobile.artifactId"));
    var image =
        new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
            artifactId,
            "replay",
            "creative",
            "CREATIVE_RENDER",
            "Peça fornecida",
            "CREATIVE_1080X1350",
            1,
            null,
            1080,
            1350,
            1350,
            0,
            "http://backend/replay",
            "http://backend/replay",
            "/api/agent-tasks/replay/visual-evidence/" + artifactId,
            (long) original.length,
            expectedHash,
            Instant.EPOCH,
            copy.toString());
    var task =
        CreativeMobileReviewInputs.enrich(
            Map.of("processCode", "creative-production-approval", "taskId", artifactId + 1),
            List.of(image));
    Path preview = CreativeMobileReviewInputs.previewPath(copy);
    var consumer =
        new CustomerBpmTaskConsumer(
            "http://backend:8000", "codex", "replay", "max", "/workspace", "", new ObjectMapper());
    assertThat(
            consumer.command(
                directory.resolve("result.json"), directory.resolve("schema.json"), List.of(image)))
        .containsSubsequence("--image", copy.toString(), "--image", preview.toString());
    assertThat(Files.readAllBytes(copy)).isEqualTo(original);
    Files.copy(
        preview,
        Path.of(System.getProperty("creative.mobile.output")),
        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    Files.writeString(
        Path.of(System.getProperty("creative.mobile.manifest")),
        new ObjectMapper().writeValueAsString(task.get("creativeMobilePreviews")));
  }

  /** Cria uma peça sintética local com hash verdadeiro e sem fontes ou efeitos externos. */
  private BpmVisualEvidenceBackendClient.UploadedVisualEvidence source(long id) throws Exception {
    var pixels = new BufferedImage(1080, 1350, BufferedImage.TYPE_INT_RGB);
    var graphics = pixels.createGraphics();
    graphics.setColor(Color.BLUE);
    graphics.fillRect(0, 0, 1080, 1350);
    graphics.dispose();
    Path file = directory.resolve("creative-" + id + ".png");
    ImageIO.write(pixels, "png", file.toFile());
    byte[] bytes = Files.readAllBytes(file);
    return new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
        id,
        "fixture",
        "creative-" + id,
        "CREATIVE_RENDER",
        "Peça sintética",
        "CREATIVE_1080X1350",
        1,
        null,
        1080,
        1350,
        1350,
        0,
        "http://backend/fixture",
        "http://backend/fixture",
        "/api/agent-tasks/fixture/visual-evidence/" + id,
        (long) bytes.length,
        sha(bytes),
        Instant.EPOCH,
        file.toAbsolutePath().toString());
  }

  /** Calcula o hash dos bytes para conferir o manifesto produzido pelo executor. */
  private static String sha(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
