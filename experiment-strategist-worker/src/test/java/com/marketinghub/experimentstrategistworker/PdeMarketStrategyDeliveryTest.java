package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: reproduzir perda de callback e reinício usando worker e HTTP locais reais. */
class PdeMarketStrategyDeliveryTest {
  @TempDir Path temporary;
  private final ObjectMapper json = new ObjectMapper();
  private final List<JsonNode> callbacks = new ArrayList<>();
  private final List<String> paths = new ArrayList<>();
  private HttpServer server;
  private WorkerProperties properties;
  private AutomaticExecutionControl control;
  private int callbackFailures;
  private int auditFailures;
  private int claims;
  private long targetProductId = 4;

  /** Inicia backend HTTP e modelo descartáveis, sem qualquer credencial ou conexão produtiva. */
  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", this::request);
    server.start();
    properties = new WorkerProperties();
    properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
    properties.setBpmStateDirectory(temporary.resolve("state").toString());
    properties.setRepositoryPath(temporary.toString());
    properties.setModel("fixture-atena-no-network");
    properties.setCodexTimeout(Duration.ofSeconds(5));
    var executable = temporary.resolve("codex-fixture.py");
    Files.writeString(
        executable,
        """
        #!/usr/bin/env python3
        import sys,json,pathlib
        root=pathlib.Path(__file__).parent
        (root/'prompt.txt').write_text(sys.stdin.read())
        with (root/'invocations.txt').open('a') as f: f.write('call\\n')
        args=sys.argv
        pathlib.Path(args[args.index('--output-last-message')+1]).write_text((root/'answer.json').read_text())
        print(json.dumps({'type':'turn.completed','usage':{'input_tokens':100,'cached_input_tokens':20,'output_tokens':10}}))
        """);
    assertThat(executable.toFile().setExecutable(true)).isTrue();
    properties.setCodexCommand(executable.toString());
    Files.writeString(
        temporary.resolve("answer.json"),
        PdeMarketStrategyBpmTaskConsumerTest.validResult("APPROVE", "null", "null"));
    control = mock(AutomaticExecutionControl.class);
    when(control.allowsAutomaticExecution()).thenReturn(true);
  }

  /** Encerra o servidor da fixture mesmo quando uma asserção falha. */
  @AfterEach
  void stop() {
    if (server != null) server.stop(0);
  }

  /** Comprova instrução integral no modelo e na auditoria, preservando o contexto do sucessor. */
  @Test
  void completesAndPreservesSuccessorContext() throws Exception {
    consumer().processOne();
    assertThat(callbacks).hasSize(1);
    assertThat(paths).containsExactly("pending", "execution-audit", "result");
    assertThat(callbacks.getFirst().path("modelUsages").get(0).path("inputTokens").asLong())
        .isEqualTo(100);
    String prompt = Files.readString(temporary.resolve("prompt.txt"));
    String constitution =
        Files.readString(
            Path.of("src/main/resources/prompts/experiment-strategist/v1/agent-core.md"));
    assertThat(prompt)
        .startsWith(constitution + "\n\n")
        .contains(
            "experiment:92", "learningSalesCycle", "v8-fixture", "não exigir nova descoberta");
    assertThat(callbacks.getFirst().path("executionAudit").path("promptSent").asText())
        .isEqualTo(prompt);
    assertThat(callbacks.getFirst().path("executionAudit").path("agentPromptPart").asText())
        .isEqualTo(constitution);
    assertThat(Files.exists(temporary.resolve("state/pending.json"))).isFalse();
    assertThat(invocations()).isEqualTo(1);
  }

  /**
   * Reproduz #358: preserva o parecer durante indisponibilidade e o envia após recriar o worker.
   */
  @Test
  void retriesPersistedResultAfterBackendAndWorkerRestart() throws Exception {
    callbackFailures = 2;
    consumer().processOne();
    assertThat(Files.exists(temporary.resolve("state/model-result.json"))).isTrue();
    consumer().processOne();
    consumer().processOne();
    assertThat(callbacks).hasSize(3);
    assertThat(callbacks.get(1)).isEqualTo(callbacks.get(0));
    assertThat(callbacks.get(2)).isEqualTo(callbacks.get(0));
    assertThat(invocations()).isEqualTo(1);
    assertThat(claims).isEqualTo(1);
    assertThat(Files.exists(temporary.resolve("state/pending.json"))).isFalse();
  }

  /** Não inicia o modelo quando o backend não confirmou a auditoria; retoma a mesma reserva. */
  @Test
  void waitsForAuditBeforeInvokingModel() throws Exception {
    auditFailures = 1;
    consumer().processOne();
    assertThat(invocations()).isZero();
    consumer().processOne();
    assertThat(claims).isEqualTo(1);
    assertThat(invocations()).isEqualTo(1);
    assertThat(callbacks).hasSize(1);
  }

  /** Drena o resultado já produzido mesmo em STOP, sem descobrir nem executar outra tarefa. */
  @Test
  void stopStillAllowsDeliveryOfExistingResult() throws Exception {
    callbackFailures = 1;
    consumer().processOne();
    when(control.allowsAutomaticExecution()).thenReturn(false);
    consumer().processOne();
    consumer().processOne();
    assertThat(claims).isEqualTo(1);
    assertThat(invocations()).isEqualTo(1);
    assertThat(callbacks).hasSize(2);
  }

  /** Conserva o bloqueio funcional em reenvio, sem transformá-lo em aprovação ou erro de rede. */
  @Test
  void preservesFunctionalDecisionAcrossTransportFailure() throws Exception {
    Files.writeString(
        temporary.resolve("answer.json"),
        PdeMarketStrategyBpmTaskConsumerTest.validResult("ADJUST", "null", "null"));
    callbackFailures = 1;
    consumer().processOne();
    consumer().processOne();
    assertThat(paths).containsExactly("pending", "execution-audit", "failure", "failure");
    assertThat(callbacks.getFirst().path("blockerGuidance").path("category").asText())
        .isEqualTo("MISSING_EVIDENCE");
    assertThat(callbacks.get(1)).isEqualTo(callbacks.getFirst());
    assertThat(invocations()).isEqualTo(1);
  }

  /**
   * Mantém resposta inválida e auditoria como falha técnica, permitindo correção pela atividade.
   */
  @Test
  void invalidResponseRemainsAuditableAndBlocked() throws Exception {
    Files.writeString(temporary.resolve("answer.json"), "{\"decision\":\"APPROVE\"}");
    consumer().processOne();
    assertThat(paths).containsExactly("pending", "execution-audit", "failure");
    JsonNode callback = callbacks.getFirst();
    assertThat(callback.path("resultJson").asText()).contains("APPROVE");
    assertThat(callback.path("executionAudit").path("promptSent").asText())
        .contains("experiment:92");
    assertThat(callback.path("blockerGuidance").path("category").asText())
        .isEqualTo("TECHNICAL_FAILURE");
  }

  /** Um processo interrompido vira falha auditada e repetível, sem nova inferência silenciosa. */
  @Test
  void interruptedInferenceDoesNotStayInProgress() throws Exception {
    callbackFailures = 1;
    consumer().processOne();
    Path state = temporary.resolve("state/pending.json");
    var pending =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(Files.readString(state));
    pending.putNull("operation");
    pending.putNull("callback");
    Files.writeString(state, pending.toString());
    consumer().processOne();
    assertThat(paths.getLast()).isEqualTo("failure");
    assertThat(callbacks.getLast().path("error").asText()).contains("interrompida");
    assertThat(invocations()).isEqualTo(1);
  }

  /** Bloqueia contexto de outro produto antes de enviar qualquer dado ao modelo. */
  @Test
  void refusesCrossProductLearningBeforeModel() throws Exception {
    targetProductId = 5;
    consumer().processOne();
    assertThat(invocations()).isZero();
    assertThat(paths).containsExactly("pending", "failure");
    assertThat(callbacks.getFirst().path("executionAudit").path("executionMode").asText())
        .isEqualTo("NOT_STARTED");
    assertThat(callbacks.getFirst().path("error").asText()).contains("não corresponde");
  }

  /** Cria uma instância nova do executor para comprovar retomada sem estado em memória. */
  private PdeMarketStrategyBpmTaskConsumer consumer() {
    return new PdeMarketStrategyBpmTaskConsumer(properties, json, control);
  }

  /** Conta somente chamadas efetivas ao processo de modelo simulado. */
  private long invocations() throws IOException {
    Path file = temporary.resolve("invocations.txt");
    return Files.exists(file) ? Files.readAllLines(file).size() : 0;
  }

  /** Simula contratos reais da fila e indisponibilidade antes de confirmar o callback. */
  private void request(HttpExchange exchange) throws IOException {
    String operation = Path.of(exchange.getRequestURI().getPath()).getFileName().toString();
    paths.add(operation);
    byte[] payload = exchange.getRequestBody().readAllBytes();
    int status = 204;
    byte[] response = new byte[0];
    if ("pending".equals(operation)) {
      claims++;
      Map<String, Object> task = new LinkedHashMap<>();
      task.put("taskId", 358);
      task.put("sourceReference", "experiment:92");
      task.put("taskTarget", Map.of("productId", targetProductId));
      task.put(
          "processContextJson",
          "{\"learningSalesCycle\":{\"productId\":4,\"experimentId\":92,\"productVersion\":\"v8-fixture\"}}");
      response = json.writeValueAsBytes(List.of(task));
      status = 200;
    } else if ("execution-audit".equals(operation)) {
      if (auditFailures-- > 0) status = 503;
    } else {
      callbacks.add(json.readTree(payload));
      if (callbackFailures-- > 0) status = 503;
    }
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, response.length == 0 ? -1 : response.length);
    if (response.length > 0) exchange.getResponseBody().write(response);
    exchange.close();
  }
}
