package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: comprovar a entrega dos encerramentos com capturas persistidas no callback. */
class PdeAgentValidationHarnessConsumerTest {
  @org.junit.jupiter.api.io.TempDir Path state;
  private final ObjectMapper json = new ObjectMapper();

  /**
   * Exercita pending, upload e callback reais, preservando outra identidade e bloqueio sem recibo.
   */
  @ParameterizedTest
  @ValueSource(longs = {91004, 97004})
  void terminalCasesReceiveOnlyPersistedScreenshotIds(long taskId) throws Exception {
    AtomicReference<JsonNode> callback = new AtomicReference<>();
    AtomicReference<String> destination = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          byte[] response;
          if (exchange.getRequestURI().getPath().endsWith("/pending")) {
            response =
                ("[{\"taskId\":" + taskId + ",\"sourceReference\":\"experiment:97092\"}]")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
          } else {
            callback.set(json.readTree(exchange.getRequestBody()));
            destination.set(exchange.getRequestURI().getPath());
            response = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    try {
      var runner = mock(PdeAgentValidationHarnessRunner.class);
      var uploads = mock(BpmVisualEvidenceBackendClient.class);
      var raw =
          json.readTree(
              """
          {"decision":"APPROVED","artifacts":[{"evidenceKey":"terminal-proof","localPath":"/temporary/capture.png"}],
           "devices":[],"scenarios":[],"terminalResultEvidence":{"cases":[{"screenshotEvidenceKeys":["terminal-proof"]}]}}
          """);
      var capture =
          new BpmVisualEvidenceRunner.CaptureOutput(
              "synthetic-capture", "MULTI_DEVICE", List.of(), List.of());
      when(runner.run(anyMap(), eq("TECHNICAL"), isNull(), any(Path.class)))
          .thenReturn(
              new PdeAgentValidationHarnessRunner.HarnessExecution(
                  raw,
                  new BpmVisualEvidenceRunner.VisualEvidenceBundle(capture, null),
                  "{}",
                  "http://127.0.0.1/prototype"));
      var uploaded =
          new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
              97001L,
              "synthetic-capture",
              "terminal-proof",
              "FULL_PAGE",
              "Encerramento",
              "PIXEL_7",
              1,
              null,
              412,
              915,
              1200,
              0,
              "http://127.0.0.1/prototype",
              "http://127.0.0.1/prototype",
              "/api/evidence/97001",
              100L,
              "a".repeat(64),
              java.time.Instant.parse("2026-10-20T12:00:00Z"),
              null);
      when(uploads.uploadArtifacts(eq(taskId), anyList())).thenReturn(List.of(uploaded));
      var consumer =
          new PdeAgentValidationHarnessConsumer(
              "http://127.0.0.1:" + server.getAddress().getPort(), json, runner, uploads);
      org.springframework.test.util.ReflectionTestUtils.setField(
          consumer, "stateDirectory", state.toString());
      consumer.processOne();
      assertThat(destination.get()).endsWith("/" + taskId + "/result");
      var result = json.readTree(callback.get().path("resultJson").asText());
      assertThat(result.at("/terminalResultEvidence/cases/0/screenshotEvidenceIds/0").asLong())
          .isEqualTo(97001L);
      assertThat(result.at("/terminalResultEvidence/cases/0").has("screenshotEvidenceKeys"))
          .isFalse();
      assertThat(result.at("/artifacts/0").has("localPath")).isFalse();
      when(uploads.uploadArtifacts(eq(taskId), anyList())).thenReturn(List.of());
      consumer.processOne();
      assertThat(destination.get()).endsWith("/" + taskId + "/failure");
    } finally {
      server.stop(0);
    }
  }

  /** Callback perdido é reenviado pelo consumidor reiniciado sem repetir o navegador ou upload. */
  @org.junit.jupiter.api.Test
  void preservesCallbackAcrossRestartAndSanitizesFailure() throws Exception {
    var delivered = new java.util.ArrayList<JsonNode>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          byte[] response;
          int status = 200;
          if (exchange.getRequestURI().getPath().endsWith("/pending"))
            response = "[{\"taskId\":95111,\"sourceReference\":\"experiment:95115\"}]".getBytes();
          else {
            delivered.add(json.readTree(exchange.getRequestBody()));
            response = "{}".getBytes();
            if (delivered.size() == 1) status = 503;
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    try {
      var runner = mock(PdeAgentValidationHarnessRunner.class);
      when(runner.run(anyMap(), eq("TECHNICAL"), isNull(), any(Path.class)))
          .thenThrow(
              PdeAgentValidationHarnessRunner.HarnessException.executor(
                  "HTTP409\nrecoveryKey=" + "a".repeat(64) + "\nCookie=private-cookie"));
      for (int i = 0; i < 2; i++) {
        var consumer =
            new PdeAgentValidationHarnessConsumer(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                json,
                runner,
                mock(BpmVisualEvidenceBackendClient.class));
        org.springframework.test.util.ReflectionTestUtils.setField(
            consumer, "stateDirectory", state.toString());
        consumer.processOne();
      }
      verify(runner, times(1)).run(anyMap(), eq("TECHNICAL"), isNull(), any(Path.class));
      assertThat(delivered).hasSize(2);
      assertThat(delivered.get(0)).isEqualTo(delivered.get(1));
      assertThat(delivered.getFirst().path("error").asText())
          .doesNotContain("HTTP409", "Exception", "private-cookie");
      assertThat(delivered.getFirst().path("evidenceJson").asText())
          .contains("HTTP409", "redigido")
          .doesNotContain("a".repeat(64), "private-cookie");
      assertThat(java.nio.file.Files.exists(state.resolve("technical-harness/pending.json")))
          .isFalse();
    } finally {
      server.stop(0);
    }
  }
}
