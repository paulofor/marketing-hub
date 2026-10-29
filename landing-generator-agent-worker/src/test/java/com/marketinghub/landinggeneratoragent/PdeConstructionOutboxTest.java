package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: comprovar recuperação local sem nova inferência nem perda de resposta ou
 * tokens.
 */
class PdeConstructionOutboxTest {
  private final ObjectMapper json = new ObjectMapper();

  /**
   * Recupera o mesmo callback após indisponibilidade e reinício, inclusive com execução pausada.
   */
  @ParameterizedTest
  @ValueSource(longs = {858L, 9858L})
  void retriesExactResultBeforeClaimingAnyNewWork(long taskId, @TempDir Path dir) throws Exception {
    var calls = new AtomicInteger();
    var payloads = new ArrayList<JsonNode>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          assertThat(exchange.getRequestURI().getPath()).endsWith("/" + taskId + "/result");
          payloads.add(json.readTree(exchange.getRequestBody().readAllBytes()));
          exchange.sendResponseHeaders(calls.incrementAndGet() <= 2 ? 503 : 204, -1);
          exchange.close();
        });
    server.start();
    try {
      var props = properties(dir, server);
      var control = mock(AutomaticExecutionControl.class);
      var telemetry = mock(CodexTelemetryReporter.class);
      var task = task(taskId);
      var outbox = new PdeConstructionOutbox(dir, json);
      Map<String, Object> body =
          Map.of(
              "resultJson",
              "{\"decision\":\"APPROVE\"}",
              "evidenceJson",
              "{\"sourceReference\":\"experiment:" + taskId + "\"}",
              "modelUsages",
              List.of(Map.of("inputTokens", 120, "outputTokens", 40)));
      try (var lock = outbox.lock()) {
        outbox.save(new PdeConstructionOutbox.Pending(task, audit(), true, "result", body));
        Files.writeString(outbox.output(), "resposta original");
      }
      for (int attempt = 0; attempt < 3; attempt++) {
        new PdeConstructionBpmTaskConsumer(props, json, control, telemetry).processOne();
        if (attempt < 2) {
          assertThat(outbox.read().callback()).isEqualTo(json.convertValue(body, Map.class));
          assertThat(Files.readString(outbox.output())).isEqualTo("resposta original");
        }
      }
      assertThat(calls.get()).isEqualTo(3);
      assertThat(payloads).allMatch(payloads.getFirst()::equals);
      assertThat(outbox.read()).isNull();
      assertThat(Files.exists(outbox.output())).isFalse();
      verifyNoInteractions(control, telemetry);
    } finally {
      server.stop(0);
    }
  }

  /** Interrompe replay automático após duas recusas, conservando o parecer original. */
  @Test
  void preservesRejectedContractWithoutEndlessReplay(@TempDir Path dir) throws Exception {
    var calls = new AtomicInteger();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          calls.incrementAndGet();
          exchange.getRequestBody().readAllBytes();
          exchange.sendResponseHeaders(409, -1);
          exchange.close();
        });
    server.start();
    try {
      var outbox = new PdeConstructionOutbox(dir, json);
      var body = Map.<String, Object>of("resultJson", "resposta original");
      try (var lock = outbox.lock()) {
        outbox.save(new PdeConstructionOutbox.Pending(task(858L), audit(), true, "result", body));
      }
      var control = mock(AutomaticExecutionControl.class);
      for (int i = 0; i < 4; i++) {
        new PdeConstructionBpmTaskConsumer(
                properties(dir, server), json, control, mock(CodexTelemetryReporter.class))
            .processOne();
      }
      assertThat(calls.get()).isEqualTo(2);
      assertThat(outbox.read().callback()).isEqualTo(body);
      assertThat(outbox.read().deliveryRefusals()).isEqualTo(2);
      verifyNoInteractions(control);
    } finally {
      server.stop(0);
    }
  }

  /**
   * Converte saída bruta completa em parecer e conserva configuração e tokens da execução original.
   */
  @Test
  void recoversRawFinalAfterRestartWithoutInvokingModel(@TempDir Path dir) throws Exception {
    recoverRaw(dir, true);
  }

  /**
   * Sem resposta final, registra falha e tokens observados sem fabricar aprovação nem reinferir.
   */
  @Test
  void interruptedModelWithoutFinalReportsItsKnownUsage(@TempDir Path dir) throws Exception {
    recoverRaw(dir, false);
  }

  /**
   * Exercita recuperação por HTTP com executor desligado e modelo configurado diferente do
   * original.
   */
  private void recoverRaw(Path dir, boolean complete) throws Exception {
    var payloads = new ArrayList<JsonNode>();
    var paths = new ArrayList<String>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          paths.add(exchange.getRequestURI().getPath());
          payloads.add(json.readTree(exchange.getRequestBody().readAllBytes()));
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    server.start();
    try {
      var props = properties(dir, server);
      props.setStrategicModel("changed-after-restart");
      var control = mock(AutomaticExecutionControl.class);
      var telemetry = mock(CodexTelemetryReporter.class);
      var outbox = new PdeConstructionOutbox(dir, json);
      try (var lock = outbox.lock()) {
        outbox.save(new PdeConstructionOutbox.Pending(task(858L), audit(), true, null, null));
        if (complete)
          Files.writeString(
              outbox.output(),
              new PdeConstructionBpmTaskConsumerTest().validProductArchitectureResult());
        Files.writeString(
            outbox.events(),
            "{\"usage\":{\"input_tokens\":120,\"cached_input_tokens\":30,\"output_tokens\":40}}\n");
      }
      new PdeConstructionBpmTaskConsumer(props, json, control, telemetry).processOne();
      assertThat(paths).hasSize(1);
      assertThat(paths.getFirst()).endsWith(complete ? "/result" : "/failure");
      var body = payloads.getFirst();
      assertThat(body.path("executionAudit").path("modelCode").asText())
          .isEqualTo("original-model");
      assertThat(body.path("modelUsages").get(0).path("modelCode").asText())
          .isEqualTo("original-model");
      assertThat(body.path("modelUsages").get(0).path("inputTokens").asInt()).isEqualTo(120);
      if (complete)
        assertThat(json.readTree(body.path("resultJson").asText()).path("decision").asText())
            .isEqualTo("APPROVE");
      else assertThat(body.path("error").asText()).startsWith("DEDALO_INTERRUPTED_OUTPUT|");
      assertThat(outbox.read()).isNull();
      verifyNoInteractions(control, telemetry);
    } finally {
      server.stop(0);
    }
  }

  /**
   * Valida a resposta recuperada do caso real sem incluir dados privados nas fixtures versionadas.
   */
  @Test
  void validatesOptionalRecoveredArchitecture() throws Exception {
    String file = System.getProperty("dedalo.recovered.result");
    org.junit.jupiter.api.Assumptions.assumeTrue(file != null);
    PdeConstructionBpmTaskConsumer.validate(
        json.readTree(Files.readString(Path.of(file))),
        "pde-commercial-plan-offer",
        "productArchitecture");
  }

  /** Define diretório exclusivo e servidor local; não há acesso ao provedor no teste. */
  private LandingGeneratorAgentProperties properties(Path dir, HttpServer server) {
    var result = new LandingGeneratorAgentProperties();
    result.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
    result.setBpmStateDirectory(dir.toString());
    result.setCodexCommand("must-never-run");
    return result;
  }

  /** Produz identidade segregada de tarefa e experimento para os replays. */
  private Map<String, Object> task(long id) {
    return Map.of(
        "taskId",
        id,
        "sourceReference",
        "experiment:" + id,
        "processCode",
        "pde-commercial-plan-offer",
        "activityId",
        "productArchitecture");
  }

  /** Conserva os metadados originais de configuração sem recalculá-los no replay. */
  private Map<String, Object> audit() {
    return Map.of(
        "executionMode",
        "MODEL",
        "modelCode",
        "original-model",
        "reasoningEffort",
        "max",
        "promptSent",
        "prompt preservado",
        "agentPromptPart",
        "agente",
        "activityPromptPart",
        "atividade");
  }
}
