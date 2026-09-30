package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Valida a leitura auditável do consumo real informado pelo processo Codex. */
class CodexTelemetryReporterTest {
  /** Deve extrair a última medição cumulativa dos eventos oficiais em JSONL. */
  @Test
  void shouldReadLatestOfficialTokenUsage() throws Exception {
    Path output = Files.createTempFile("codex-events-", ".jsonl");
    Files.writeString(
        output,
        """
        WARN mensagem operacional emitida no stderr
        {"type":"item.completed","item":{"type":"agent_message"}}
        {"type":"turn.completed","usage":{"input_tokens":1234,"input_tokens_details":{"cached_tokens":700},"output_tokens":321}}
        """);
    CodexTelemetryReporter reporter =
        new CodexTelemetryReporter(new LandingGeneratorAgentProperties(), new ObjectMapper());

    CodexTelemetryReporter.TokenUsage usage = reporter.readTokenUsage(output);

    assertThat(usage.inputTokens()).isEqualTo(1234);
    assertThat(usage.cachedInputTokens()).isEqualTo(700);
    assertThat(usage.outputTokens()).isEqualTo(321);
    Files.deleteIfExists(output);
  }

  /** Deve manter nulos quando nenhum evento informou usage, sem criar estimativa. */
  @Test
  void shouldKeepTokensUnknownWithoutUsageEvent() throws Exception {
    Path output = Files.createTempFile("codex-events-", ".jsonl");
    Files.writeString(output, "{\"type\":\"thread.started\"}\n");
    CodexTelemetryReporter reporter =
        new CodexTelemetryReporter(new LandingGeneratorAgentProperties(), new ObjectMapper());

    CodexTelemetryReporter.TokenUsage usage = reporter.readTokenUsage(output);

    assertThat(usage.inputTokens()).isNull();
    assertThat(usage.outputTokens()).isNull();
    Files.deleteIfExists(output);
  }

  /** Deve separar tentativas distintas e preservar a identidade em uma retomada da mesma lease. */
  @Test
  void shouldUseStableTelemetryIdentityPerTechnicalExecution() {
    CodexTelemetryReporter reporter =
        new CodexTelemetryReporter(new LandingGeneratorAgentProperties(), new ObjectMapper());

    assertThat(reporter.executionTelemetryId("execution-a"))
        .isEqualTo(reporter.executionTelemetryId("execution-a"));
    assertThat(reporter.executionTelemetryId("execution-a"))
        .isNotEqualTo(reporter.executionTelemetryId("execution-b"));
  }

  /** Deve segregar a telemetria BPM de Dédalo pela identidade canônica da tarefa. */
  @Test
  void shouldReportDedaloBpmTelemetryByTaskId() throws Exception {
    List<String> paths = new CopyOnWriteArrayList<>();
    CountDownLatch heartbeat = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/api/codex-agent-telemetry/v1/internal/DEDALO_BPM/executions/534",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          paths.add(path);
          exchange.getRequestBody().readAllBytes();
          exchange.sendResponseHeaders(200, 0);
          exchange.getResponseBody().write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
          exchange.close();
          if (path.endsWith("/heartbeat")) heartbeat.countDown();
          if (path.endsWith("/finish")) finish.countDown();
        });
    server.start();
    Path output = Files.createTempFile("dedalo-bpm-events-", ".jsonl");
    Files.writeString(output, "{\"type\":\"thread.started\"}\n");
    LandingGeneratorAgentProperties properties = new LandingGeneratorAgentProperties();
    properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
    CodexTelemetryReporter reporter = new CodexTelemetryReporter(properties, new ObjectMapper());
    Process process = new ProcessBuilder("/usr/bin/true").start();
    process.waitFor();
    try {
      try (CodexTelemetryReporter.Session session =
          reporter.monitorBpmTask(534L, process, output)) {
        assertThat(heartbeat.await(2, TimeUnit.SECONDS)).isTrue();
        session.success();
      }

      assertThat(finish.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(paths)
          .contains(
              "/api/codex-agent-telemetry/v1/internal/DEDALO_BPM/executions/534/heartbeat",
              "/api/codex-agent-telemetry/v1/internal/DEDALO_BPM/executions/534/finish");
    } finally {
      reporter.shutdown();
      Files.deleteIfExists(output);
      server.stop(0);
    }
  }
}
