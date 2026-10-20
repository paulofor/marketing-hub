package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Responsabilidade: proteger a revisão de Têmis contra troca de peça e ausência de pixels. */
class CreativeReviewImagesTest {
  /** Usa o PNG persistido, exige o mesmo hash no parecer e remove o arquivo após a execução. */
  @Test
  void receivesExactPixelsAndRejectsDifferentArtifact() throws Exception {
    var mapper = new ObjectMapper();
    var pixels =
        new java.awt.image.BufferedImage(1080, 1350, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var output = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(pixels, "png", output);
    byte[] png = output.toByteArray();
    String hash =
        HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(png));
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          boolean content = exchange.getRequestURI().getPath().endsWith("/content");
          byte[] response =
              content
                  ? png
                  : ("[{\"sourceTaskId\":910403,\"prototypeVersion\":\"sandbox-v12\",\"evidence\":{\"id\":910130,\"evidenceType\":\"CREATIVE_RENDER\",\"sha256\":\""
                          + hash
                          + "\"}}]")
                      .getBytes(StandardCharsets.UTF_8);
          exchange
              .getResponseHeaders()
              .set("Content-Type", content ? "image/png" : "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    Path file;
    try {
      var task =
          new HashMap<String, Object>(
              Map.of("taskId", 910405L, "processCode", "creative-production-approval"));
      try (var images =
          CreativeReviewImages.load(
              RestClient.builder()
                  .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                  .build(),
              task)) {
        var command = images.attach(List.of("codex", "exec"));
        assertThat(command).contains("--image");
        file = Path.of(command.getLast());
        assertThat(Files.readAllBytes(file)).isEqualTo(png);
        var result =
            mapper.readTree(
                "{\"renderedAssetAudit\":[{\"artifactId\":910130,\"sha256\":\""
                    + hash
                    + "\",\"assessment\":\"Promessa coerente com a demonstração privada\"}]}");
        CreativeReviewImages.validate(result, mapper.valueToTree(task.get("creativeVisualInputs")));
        assertThatThrownBy(
                () ->
                    CreativeReviewImages.validate(
                        mapper.readTree(result.toString().replace("910130", "999999")),
                        mapper.valueToTree(task.get("creativeVisualInputs"))))
            .hasMessageContaining("peças exatas");
      }
      assertThat(file).doesNotExist();
    } finally {
      server.stop(0);
    }
  }

  /** Entrega fonte, recorte e captura atual sem confundir esses anexos com a peça auditada. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {762, 910762})
  void suppliesVerifiedSourceAndContinuityBeforeModel(long taskId) throws Exception {
    var mapper = new ObjectMapper();
    byte[] finalPng = png(1080, 1350);
    byte[] sourcePng = png(400, 500);
    byte[] currentPng = png(480, 720);
    byte[] illustrationPng = png(1254, 1254);
    var input = mapper.createObjectNode();
    input.put("sourceTaskId", 910403L).put("prototypeVersion", "sandbox-v12");
    input.set(
        "evidence",
        mapper.valueToTree(
            Map.of("id", 910130L, "evidenceType", "CREATIVE_RENDER", "sha256", sha(finalPng))));
    var proof = input.putObject("creativeProof");
    var source = proof.putObject("source");
    source.put("sourceTaskId", 910395L).put("prototypeVersion", "sandbox-v12");
    source.set("evidence", mapper.valueToTree(Map.of("id", 910118L, "sha256", sha(sourcePng))));
    proof.putObject("crop").put("x", 10).put("y", 20).put("width", 300).put("height", 200);
    var destination = proof.putObject("currentDestination");
    destination.put("taskId", 910398L);
    destination
        .putObject("technicalProof")
        .put("prototypeVersion", "sandbox-v12")
        .put("decision", "APPROVED");
    var capture = destination.putArray("captures").addObject();
    capture.put("sourceTaskId", 910398L).put("prototypeVersion", "sandbox-v12");
    capture.set(
        "evidence",
        mapper.valueToTree(
            Map.of(
                "id", 910119L, "sha256", sha(currentPng), "capturedAt", "2026-10-19T20:00:00Z")));
    var corrupt = new java.util.concurrent.atomic.AtomicBoolean(false);
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          byte[] response;
          if (!path.endsWith("/content"))
            response = ("[" + input + "]").getBytes(StandardCharsets.UTF_8);
          else if (path.contains("/illustrations/")) response = illustrationPng;
          else if (path.contains("/910395/")) response = corrupt.get() ? finalPng : sourcePng;
          else if (path.contains("/910398/")) response = currentPng;
          else response = finalPng;
          exchange
              .getResponseHeaders()
              .set("Content-Type", path.endsWith("/content") ? "image/png" : "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    try {
      var task =
          new HashMap<String, Object>(
              Map.of("taskId", taskId, "processCode", "creative-production-approval"));
      var backend =
          RestClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build();
      try (var files = CreativeReviewImages.load(backend, task)) {
        var command = files.attach(List.of("codex", "exec"));
        assertThat(command.stream().filter("--image"::equals).count()).isEqualTo(4);
        var checks = mapper.valueToTree(task.get("creativeEvidenceChecks"));
        assertThat(checks.get(0).path("source").path("hashVerified").asBoolean()).isTrue();
        assertThat(checks.get(0).path("source").path("attachmentIndex").asInt()).isEqualTo(2);
        assertThat(checks.get(0).path("cropPreview").path("attachmentIndex").asInt()).isEqualTo(3);
        assertThat(checks.get(0).path("currentDestination").path("captures").size()).isEqualTo(1);
        assertThat(mapper.valueToTree(task.get("creativeVisualInputs")).size()).isEqualTo(1);
        var prompt =
            new CommercialBpmTaskConsumer(
                    new MetaAdApproverProperties(),
                    "codex",
                    "gpt-6.1-sol",
                    "/unused",
                    "/unused",
                    mapper)
                .prompt(task);
        assertThat(prompt)
            .contains(
                "CREATIVE_PROOF_VERIFICATION_V1",
                "creativeEvidenceChecks",
                sha(sourcePng),
                "2026-10-19T20:00:00Z");
      }
      proof.put("templateVersion", "EDITORIAL_CARD_V1");
      var illustration =
          proof
              .putObject("illustration")
              .put("jobId", "img-sandbox")
              .put("sha256", sha(illustrationPng))
              .put("role", "GENERATED_ILLUSTRATION");
      illustration.putObject("crop").put("x", 0).put("y", 0).put("width", 1080).put("height", 1000);
      try (var files = CreativeReviewImages.load(backend, new HashMap<>(task))) {
        assertThat(
                files.attach(List.of("codex", "exec")).stream().filter("--image"::equals).count())
            .isEqualTo(5);
      }
      illustration.put("sha256", "0".repeat(64));
      assertThatThrownBy(() -> CreativeReviewImages.load(backend, new HashMap<>(task)))
          .hasMessageContaining("ilustração");
      illustration.put("sha256", sha(illustrationPng));
      corrupt.set(true);
      assertThatThrownBy(() -> CreativeReviewImages.load(backend, new HashMap<>(task)))
          .isInstanceOf(java.io.IOException.class)
          .hasMessageContaining("hash")
          .hasMessageContaining(String.valueOf(taskId));
      corrupt.set(false);
      ((com.fasterxml.jackson.databind.node.ObjectNode) proof.path("crop")).put("width", 500);
      assertThatThrownBy(() -> CreativeReviewImages.load(backend, new HashMap<>(task)))
          .isInstanceOf(java.io.IOException.class);
      ((com.fasterxml.jackson.databind.node.ObjectNode) proof.path("crop")).put("width", 300);
      ((com.fasterxml.jackson.databind.node.ObjectNode) destination.path("technicalProof"))
          .put("prototypeVersion", "foreign-v2");
      assertThatThrownBy(() -> CreativeReviewImages.load(backend, new HashMap<>(task)))
          .isInstanceOf(java.io.IOException.class);
    } finally {
      server.stop(0);
    }
  }

  /** Produz pixels locais para simular arquivos do backend sem provedor ou evento comercial. */
  private byte[] png(int width, int height) throws Exception {
    var image =
        new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var out = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", out);
    return out.toByteArray();
  }

  /** Calcula a identidade da fixture usada pelo contrato HTTP local. */
  private String sha(byte[] bytes) throws Exception {
    return HexFormat.of()
        .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
