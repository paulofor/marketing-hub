package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: comprovar a identidade e o encerramento da telemetria Codex de Têmis. */
class CodexTelemetryReporterTest {
  @TempDir Path directory;

  /** Publica heartbeat e término do BPM usando o taskId, sem colidir com criativos. */
  @Test
  void reportsBpmTaskWithDedicatedAgentType() throws Exception {
    CountDownLatch heartbeat = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    AtomicReference<String> heartbeatPath = new AtomicReference<>();
    AtomicReference<String> finishPath = new AtomicReference<>();
    AtomicReference<String> finishBody = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/codex-agent-telemetry/v1/internal/",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          String body =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          if (path.endsWith("/heartbeat")) {
            heartbeatPath.set(path);
            heartbeat.countDown();
          } else if (path.endsWith("/finish")) {
            finishPath.set(path);
            finishBody.set(body);
            finish.countDown();
          }
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    server.start();
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
    CodexTelemetryReporter reporter = new CodexTelemetryReporter(properties);
    Path output = directory.resolve("temis.jsonl");
    Files.writeString(output, "{\"type\":\"turn.started\"}\n");
    Process process = mock(Process.class);
    when(process.pid()).thenReturn(321L);
    when(process.isAlive()).thenReturn(true);
    try {
      try (CodexTelemetryReporter.Session session =
          reporter.monitorBpmTask(587L, process, output)) {
        assertThat(heartbeat.await(5, TimeUnit.SECONDS)).isTrue();
        session.success();
      }
      assertThat(finish.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(heartbeatPath.get())
          .isEqualTo("/api/codex-agent-telemetry/v1/internal/TEMIS_BPM/executions/587/heartbeat");
      assertThat(finishPath.get())
          .isEqualTo("/api/codex-agent-telemetry/v1/internal/TEMIS_BPM/executions/587/finish");
      assertThat(finishBody.get()).contains("\"success\":true", "\"eventCount\":1");
    } finally {
      reporter.shutdown();
      server.stop(0);
    }
  }
}
