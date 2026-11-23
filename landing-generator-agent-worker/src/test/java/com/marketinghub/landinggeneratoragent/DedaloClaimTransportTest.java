package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: provar replay do transporte real sem perder identidade ou iniciar modelo. */
class DedaloClaimTransportTest {
  /**
   * Perde resposta de reserva e contexto, reinicia consumidor e recupera somente a mesma tarefa.
   */
  @Test
  void recoversLostReceiptAndContextAcrossRestart(@TempDir Path directory) throws Exception {
    var json = new ObjectMapper();
    var identities = new HashSet<String>();
    var receipts = new AtomicInteger();
    var contexts = new AtomicInteger();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          byte[] body = new byte[0];
          int status = 503;
          if (exchange.getRequestURI().getPath().endsWith("/pending/receipt")) {
            String id =
                Arrays.stream(exchange.getRequestURI().getQuery().split("&"))
                    .filter(v -> v.startsWith("claimId="))
                    .findFirst()
                    .orElseThrow()
                    .substring(8);
            identities.add(id);
            if (receipts.incrementAndGet() > 1) {
              status = 200;
              body =
                  json.writeValueAsBytes(
                      List.of(
                          Map.of(
                              "taskId",
                              8001,
                              "claimId",
                              id,
                              "phase",
                              "CLAIMED",
                              "status",
                              "IN_PROGRESS")));
            }
          } else if (exchange.getRequestURI().getPath().endsWith("/claim-context")) {
            assertThat(identities)
                .contains(exchange.getRequestHeaders().getFirst("X-Dedalo-Claim"));
            if (contexts.incrementAndGet() > 1) {
              status = 200;
              body = json.writeValueAsBytes(Map.of("taskId", 8001));
            }
          } else
            throw new AssertionError(
                "Nenhum modelo, callback ou endpoint paralelo deve ser chamado");
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
          if (body.length > 0) exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    var outbox = new PdeConstructionOutbox(directory, json);
    try (var lock = outbox.lock()) {
      var properties = new LandingGeneratorAgentProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      properties.setBpmStateDirectory(directory.toString());
      properties.setCodexCommand("must-never-run");
      for (int i = 0; i < 2; i++) {
        var consumer =
            new PdeConstructionBpmTaskConsumer(
                properties,
                json,
                mock(AutomaticExecutionControl.class),
                mock(CodexTelemetryReporter.class));
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(consumer, "claimNext"))
            .isInstanceOf(RuntimeException.class);
        assertThat(outbox.readClaim()).isNotNull();
      }
      var consumer =
          new PdeConstructionBpmTaskConsumer(
              properties,
              json,
              mock(AutomaticExecutionControl.class),
              mock(CodexTelemetryReporter.class));
      Map<String, Object> task = ReflectionTestUtils.invokeMethod(consumer, "claimNext");
      assertThat(task.get("taskId")).isEqualTo(8001);
      assertThat(identities).hasSize(1);
      assertThat(receipts.get()).isEqualTo(2);
      assertThat(contexts.get()).isEqualTo(2);
      assertThat(Files.exists(outbox.events())).isFalse();
      assertThat(outbox.read()).isNull();
    } finally {
      server.stop(0);
    }
  }

  /** Duas instâncias locais não obtêm simultaneamente o lock da mesma outbox. */
  @Test
  void serializesConsumers(@TempDir Path directory) throws Exception {
    var first = new PdeConstructionOutbox(directory, new ObjectMapper());
    var second = new PdeConstructionOutbox(directory, new ObjectMapper());
    try (var lock = first.lock()) {
      assertThat(second.lock()).isNull();
    }
    try (var lock = second.lock()) {
      assertThat(lock).isNotNull();
    }
  }

  /** Início remoto incerto após reinício permanece preservado sem modelo ou callback substituto. */
  @Test
  void refusesReinferenceAndFabricatedCallbackWhenStartResponseWasLost(@TempDir Path directory)
      throws Exception {
    var json = new ObjectMapper();
    var outbox = new PdeConstructionOutbox(directory, json);
    var task =
        Map.<String, Object>of("taskId", 8002, "executionClaimId", UUID.randomUUID().toString());
    try (var lock = outbox.lock()) {
      outbox.save(
          new PdeConstructionOutbox.Pending(
              task, Map.of("executionMode", "MODEL"), true, null, null));
    }
    var properties = new LandingGeneratorAgentProperties();
    properties.setBpmStateDirectory(directory.toString());
    properties.setBackendUrl("http://127.0.0.1:1");
    properties.setCodexCommand("must-never-run");
    var control = mock(AutomaticExecutionControl.class);
    var telemetry = mock(CodexTelemetryReporter.class);
    new PdeConstructionBpmTaskConsumer(properties, json, control, telemetry).processOne();
    assertThat(outbox.read().modelStarted()).isTrue();
    assertThat(outbox.read().callback()).isNull();
    verifyNoInteractions(control, telemetry);
  }

  /** Mantém processo original vivo e não considera PID de outro instante como sua execução. */
  @Test
  void preservesLiveOriginalProcess(@TempDir Path directory) throws Exception {
    var outbox = new PdeConstructionOutbox(directory, new ObjectMapper());
    Process process = new ProcessBuilder("sleep", "30").start();
    try {
      outbox.recordProcess(process);
      assertThat(outbox.originalProcessAlive()).isTrue();
      process.destroy();
      process.waitFor();
      assertThat(outbox.originalProcessAlive()).isFalse();
      assertThat(outbox.hasProcessIdentity()).isTrue();
    } finally {
      process.destroyForcibly();
    }
  }
}
