package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Responsabilidade: comprovar imagem exata anexada a Psique e preservação do parecer em falhas
 * técnicas.
 */
class CreativeVisualInputTest {
  @TempDir Path directory;
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  /**
   * Baixa, confere hash, anexa pixels e exige auditoria de cada arquivo sem produzir nova
   * evidência.
   */
  @Test
  void downloadsAndAuditsExactCreative() throws Exception {
    var pixels =
        new java.awt.image.BufferedImage(1080, 1350, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var output = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(pixels, "png", output);
    byte[] bytes = output.toByteArray();
    String hash =
        HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    String root = "/api/internal/agent-tasks/customer-agent/stage-executions/910404/visual-inputs";
    server.createContext(
        root,
        exchange -> {
          byte[] response =
              exchange.getRequestURI().getPath().endsWith("/content")
                  ? bytes
                  : ("[{\"sourceTaskId\":910403,\"prototypeVersion\":\"sandbox-v12\",\"evidence\":{\"id\":910130,\"evidenceType\":\"CREATIVE_RENDER\",\"sha256\":\""
                          + hash
                          + "\"}}]")
                      .getBytes(StandardCharsets.UTF_8);
          exchange
              .getResponseHeaders()
              .set(
                  "Content-Type",
                  exchange.getRequestURI().getPath().endsWith("/content")
                      ? "image/png"
                      : "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    try {
      var client =
          new BpmVisualEvidenceBackendClient("http://127.0.0.1:" + server.getAddress().getPort());
      var images = client.creativeInputs(910404L, directory);
      assertThat(images).hasSize(1);
      assertThat(Files.readAllBytes(Path.of(images.getFirst().localPath()))).isEqualTo(bytes);
      var consumer =
          new CustomerBpmTaskConsumer(
              "http://localhost:1", "codex", "gpt-5.6-sol", "max", "/workspace", "", json);
      assertThat(
              consumer.command(
                  directory.resolve("out.json"), directory.resolve("schema.json"), images))
          .containsSubsequence("--image", images.getFirst().localPath());
      var result =
          json.readTree(
              "{\"renderedAssetAudit\":[{\"artifactId\":910130,\"sha256\":\""
                  + hash
                  + "\",\"assessment\":\"Peça inspecionada nos pixels recebidos\"}]}");
      CustomerBpmTaskConsumer.validateCreativeAudit(result, images);
      assertThatThrownBy(
              () ->
                  CustomerBpmTaskConsumer.validateCreativeAudit(
                      json.readTree(result.toString().replace(hash, "a".repeat(64))), images))
          .hasMessageContaining("imagem exata");
    } finally {
      server.stop(0);
    }
  }

  /** Mantém o diagnóstico original visível mesmo quando a validação do contrato o rejeita. */
  @Test
  void preservesRawBlockedResponseOnContractFailure() throws Exception {
    AtomicReference<String> callback = new AtomicReference<>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          callback.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    server.start();
    try {
      String raw =
          "{\n  \"decision\": \"BLOCKED\", \"requiredChanges\": [\"Falta a imagem real\"]\n}";
      var consumer =
          new CustomerBpmTaskConsumer(
              "http://127.0.0.1:" + server.getAddress().getPort(),
              "codex",
              "gpt-5.6-sol",
              "max",
              "/workspace",
              "",
              json);
      var execution =
          new CustomerBpmTaskConsumer.BpmExecution(
              json.readTree(raw),
              CustomerBpmTaskConsumer.TokenUsage.empty(),
              "prompt",
              "núcleo",
              "atividade",
              List.of(),
              List.of(),
              raw);
      org.springframework.test.util.ReflectionTestUtils.invokeMethod(
          consumer,
          "fail",
          Map.of(
              "taskId",
              910404L,
              "processCode",
              "creative-production-approval",
              "activityId",
              "customer"),
          new IllegalArgumentException("Composição inválida"),
          execution,
          null);
      assertThat(json.readTree(callback.get()).path("resultJson").asText()).isEqualTo(raw);
    } finally {
      server.stop(0);
    }
  }
}
