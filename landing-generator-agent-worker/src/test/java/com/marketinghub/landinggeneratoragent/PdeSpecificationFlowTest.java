package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Valida o worker real de especificação com processos locais e callbacks HTTP sem IA paga. */
class PdeSpecificationFlowTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Exercita identidades distintas, falha funcional e recusa prévia de contexto divergente. */
  @ParameterizedTest
  @CsvSource({
    "7, journey, READY, false",
    "97001, journey, READY, false",
    "7, deliverables, READY, false",
    "97001, deliverables, READY, false",
    "7, access, READY, false",
    "97001, access, READY, false",
    "7, journey, BLOCKED, false",
    "97001, journey, READY, true"
  })
  void runsSpecificationWithoutWorkspaceOrFabricatedAcceptance(
      long productId, String activity, String decision, boolean mismatched, @TempDir Path directory)
      throws Exception {
    var task = input(productId);
    task.put("taskId", 900000 + productId);
    task.put("processCode", "pde-construction-approval");
    task.put("activityId", activity);
    task.put("sourceReference", "experiment:92");
    if (mismatched) task.withObject("/taskTarget/pdeContext/product").put("id", 800001);
    Files.writeString(directory.resolve("response.json"), result(activity, decision).toString());
    var executable = directory.resolve("fake-codex.mjs");
    try (var script = getClass().getResourceAsStream("/bpm/fake-codex.mjs")) {
      Files.copy(script, executable);
    }
    assertThat(executable.toFile().setExecutable(true)).isTrue();
    var operations = new ArrayList<String>();
    var callback = new AtomicReference<JsonNode>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          byte[] response = new byte[0];
          int status = 204;
          if (path.endsWith("/automatic-execution")) {
            response = json.writeValueAsBytes(Map.of("automaticExecutionEnabled", true));
            status = 200;
          } else if (path.endsWith("/pending")) {
            response =
                json.writeValueAsBytes(
                    exchange.getRequestURI().getQuery().contains("activityId=" + activity)
                        ? List.of(task)
                        : List.of());
            status = 200;
          } else {
            String operation = Path.of(path).getFileName().toString();
            operations.add(operation);
            JsonNode body = json.readTree(exchange.getRequestBody().readAllBytes());
            if (!"execution-audit".equals(operation)) callback.set(body);
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, response.length == 0 ? -1 : response.length);
          if (response.length > 0) exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    try {
      var properties = new LandingGeneratorAgentProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      properties.setRepositoryPath(directory.toString());
      properties.setCodexCommand(executable.toString());
      properties.setModel("fixture-specification-no-network");
      properties.setCodexTimeout(Duration.ofSeconds(10));
      var telemetry = mock(CodexTelemetryReporter.class);
      when(telemetry.monitorBpmTask(anyLong(), any(), any()))
          .thenReturn(mock(CodexTelemetryReporter.Session.class));
      new PdeConstructionBpmTaskConsumer(
              properties,
              json,
              new AutomaticExecutionControl(properties.getBackendUrl()),
              telemetry)
          .processOne();

      if (mismatched) {
        assertThat(operations).containsExactly("failure");
        assertThat(Files.exists(directory.resolve("prompt.txt"))).isFalse();
        assertThat(callback.get().path("error").asText())
            .contains("Identidade do catálogo diverge");
        assertThat(callback.get().has("modelUsages")).isFalse();
        return;
      }
      assertThat(operations)
          .containsExactly("execution-audit", decision.equals("READY") ? "result" : "failure");
      assertThat(json.readTree(callback.get().path("resultJson").asText()))
          .isEqualTo(result(activity, decision));
      assertThat(callback.get().path("modelUsages").get(0).path("inputTokens").asInt())
          .isEqualTo(7);
      String prompt = Files.readString(directory.resolve("prompt.txt"));
      assertThat(prompt)
          .contains(
              "Limite executável das atividades de especificação",
              "não implementa arquivos",
              "technicalHomologation",
              "não aprova o protótipo",
              productId == 7 ? "Capella" : "Outra identidade QA",
              productId == 7 ? "Quartzo" : "Opala");
      assertThat(callback.get().path("executionAudit").path("promptSent").asText())
          .isEqualTo(prompt);
      var args = json.readValue(Files.readString(directory.resolve("arguments.json")), List.class);
      assertThat(args).containsSubsequence("--config", "features.shell_tool=false");
      assertThat(args).containsSubsequence("--sandbox", "read-only");
      assertThat(args)
          .doesNotContain("danger-full-access", "--dangerously-bypass-approvals-and-sandbox");
      assertThat(task.path("taskTarget").path("pdeContext").has("privatePrototypeAcceptance"))
          .isFalse();
      assertThat(task.path("taskTarget").path("publicUrl").isNull()).isTrue();
    } finally {
      server.stop(0);
    }
  }

  /** Usa o contrato exportado pelo backend quando disponível, ou a fixture segregada do worker. */
  private ObjectNode input(long productId) throws Exception {
    String handoff = System.getProperty("pde.specification.context.input");
    if (handoff != null)
      return (ObjectNode)
          json.readTree(Files.readString(Path.of(handoff).resolve(productId + ".json")));
    var input =
        (ObjectNode) json.readTree(getClass().getResourceAsStream("/bpm/specification-input.json"));
    input.withObject("/taskTarget").put("productId", productId);
    input
        .withObject("/taskTarget/pdeContext/product")
        .put("id", productId)
        .put("internalName", productId == 7 ? "Capella" : "Outra identidade QA")
        .put("productTypeInternalName", productId == 7 ? "Quartzo" : "Opala");
    return input;
  }

  /** Cria saídas declaradamente simuladas, sem URL, artefato ou aprovação de implementação. */
  private ObjectNode result(String activity, String decision) {
    var result =
        json.createObjectNode()
            .put("decision", decision)
            .put("rationale", "Contrato simulado; implementação e provas ainda são necessárias.")
            .put(
                "selectedApproach", "Entrada curta, resultado completo e retomada com isolamento.");
    for (String alternative : List.of("Direta", "Guiada", "Progressiva")) {
      result
          .withArray("/alternatives")
          .addObject()
          .put("name", alternative)
          .put("benefit", "Entrada compreensível e resultado utilizável")
          .put("risk", "Necessita comprovar recuperação e qualidade")
          .put("effort", "Esforço controlado")
          .put("salesFit", "Mantém resultado e limites contratados");
    }
    result.putArray("acceptanceCriteria").add("Homologação real antes de qualquer aprovação.");
    result.putArray("requiredChanges").add("Implementação privada com provas da mesma versão.");
    var signals =
        json.valueToTree(
            List.of(
                "EXPERIENCE_STARTED",
                "VALUE_MOMENT",
                "READY_RESULT_USED",
                "PREFERRED_OVER_FREE",
                "CHECKOUT_STARTED"));
    if (activity.equals("journey")) {
      var contract =
          result
              .putObject("experienceContract")
              .put("maxValueTimeMinutes", 10)
              .put("checkoutMode", "SIMULATED_NO_CHARGE");
      contract.set("instrumentationEvents", signals);
      for (String stage : List.of("entrada", "resultado", "retomada"))
        contract.withArray("/stages").addObject().put("id", stage);
    } else if (activity.equals("deliverables")) {
      var contract = result.putObject("deliveryPackage").put("version", "pde-private-prototype-v1");
      contract.set("instrumentationEvents", signals);
      for (String asset : List.of("entrada", "processamento", "resultado"))
        contract.withArray("/assets").addObject().put("id", asset);
    } else {
      var contract = result.putObject("accessContract");
      contract.set("observability", signals);
      contract.putArray("errorStates").addObject().put("code", "INVALID_ACCESS");
    }
    return result;
  }
}
