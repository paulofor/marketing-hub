package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Responsabilidade: comprovar replay do executor por HTTP local, sem processo de modelo. */
class AtenaPreservedReplayTest {
  @TempDir Path directory;
  private final ObjectMapper json = new ObjectMapper();

  /**
   * Reenvia bytes após 503/reinício e bloqueia divergência funcional sem inferência ou tokens
   * novos.
   */
  @ParameterizedTest
  @CsvSource({"97004,false", "98004,false", "97004,true", "98004,true"})
  void replaysOrRejectsPreservedStrategyWithoutInference(long productId, boolean contradictory)
      throws Exception {
    ObjectNode task = new PdeValidationScopeTest().task(productId, false);
    task.put("taskId", productId + 1000);
    task.withObject("/taskTarget")
        .put("productInternalName", "Alcyone")
        .putObject("pdeContext")
        .putObject("product")
        .put("productTypeCode", "AI_PRODUCT")
        .put("productTypeInternalName", "Safira");
    ObjectNode result = new PdeMarketStrategyBpmTaskConsumerTest().agentValidationResult();
    result.withObject("/productIdentity").put("mode", "PRESERVE");
    result
        .withObject("/marketStrategicContract/agentValidationPlan")
        .put(
            "continueCriteria",
            contradictory
                ? "Nenhuma condição REFERENCE/REDUCED. Exigir REDUCED."
                : "inputComparisonApplicable=false: nenhuma condição REFERENCE/REDUCED e nenhuma"
                    + " matriz de 18 ou 27 casos.");
    String raw = json.writeValueAsString(result);
    String evidence =
        json.writeValueAsString(
            Map.of("agent", "Atena", "sourceReference", task.path("sourceReference").asText()));
    task.put("retryResultJson", raw).put("retryEvidenceJson", evidence);
    List<String> paths = new ArrayList<>();
    List<JsonNode> callbacks = new ArrayList<>();
    var claimed = new AtomicBoolean();
    var failedOnce = new AtomicBoolean();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String operation = Path.of(exchange.getRequestURI().getPath()).getFileName().toString();
          paths.add(operation);
          byte[] response = new byte[0];
          int status = 204;
          if (operation.equals("pending")) {
            assertThat(exchange.getRequestURI().getQuery())
                .contains("ATENA_PDE_MARKET_STRATEGY_REPLAY_V2");
            response = json.writeValueAsBytes(claimed.getAndSet(true) ? List.of() : List.of(task));
            status = 200;
          } else {
            callbacks.add(json.readTree(exchange.getRequestBody().readAllBytes()));
            if (!failedOnce.getAndSet(true)) status = 503;
          }
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, response.length == 0 ? -1 : response.length);
          if (response.length > 0) exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    try {
      var properties = new WorkerProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      properties.setBpmStateDirectory(directory.toString());
      properties.setCodexCommand(directory.resolve("modelo-proibido").toString());
      var control = mock(AutomaticExecutionControl.class);
      when(control.allowsAutomaticExecution()).thenReturn(true);
      new PdeMarketStrategyBpmTaskConsumer(properties, json, control).processOne();
      new PdeMarketStrategyBpmTaskConsumer(properties, json, control).processOne();
      assertThat(paths)
          .containsExactly(
              "pending",
              contradictory ? "failure" : "result",
              contradictory ? "failure" : "result");
      assertThat(callbacks).hasSize(2);
      assertThat(callbacks.get(1)).isEqualTo(callbacks.get(0));
      assertThat(callbacks.get(0).path("resultJson").asText()).isEqualTo(raw);
      assertThat(callbacks.get(0).path("evidenceJson").asText()).isEqualTo(evidence);
      assertThat(callbacks.get(0).has("modelUsages")).isFalse();
      assertThat(callbacks.get(0).has("executionAudit")).isFalse();
      if (contradictory)
        assertThat(callbacks.get(0).path("error").asText())
            .contains("análise paga")
            .doesNotContain("REFERENCE", "Exception", "{");
    } finally {
      server.stop(0);
    }
  }
}
