package com.marketinghub.financialagentworker;

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

/** Responsabilidade: homologar a fila econômica real com HTTP e modelo locais na imagem final. */
public final class PdeEconomicsImageSmoke {
  static final List<String> CASES =
      List.of(
          "successor",
          "discovery",
          "later-version",
          "legacy",
          "drift",
          "missing",
          "timestamp",
          "contribution",
          "budget",
          "stop");

  /** Impede instanciar o comando de homologação. */
  private PdeEconomicsImageSmoke() {}

  /** Executa todos os cenários sem acesso à rede externa ou aos dados produtivos. */
  public static void main(String[] args) throws Exception {
    for (String scenario : CASES) {
      runScenario(scenario, Path.of("/tests"));
      System.out.println("PASS Plutus " + scenario);
    }
  }

  /** Percorre PLAY, pending, prompt, schema, validação e callback com correlação isolada. */
  static void runScenario(String scenario, Path resources) throws Exception {
    boolean legacy = "legacy".equals(scenario);
    boolean beforeModel = List.of("drift", "missing").contains(scenario);
    boolean invalid = List.of("timestamp", "contribution", "budget").contains(scenario);
    boolean stopped = "stop".equals(scenario);
    var json = new ObjectMapper();
    Path directory = Files.createTempDirectory("plutus-economics-smoke-");
    ObjectNode response =
        (ObjectNode)
            json.readTree(Files.readString(resources.resolve("bpm/private-economics.json")));
    ObjectNode economics = (ObjectNode) response.path("economics");
    if (legacy) {
      response.remove("contractVersion");
      response.remove("mode");
    }
    if ("timestamp".equals(scenario)) economics.put("deadline", "2026-09-17T02:59:00Z");
    if ("contribution".equals(scenario)) economics.put("contributionPerSaleBrl", 26.95);
    if ("budget".equals(scenario)) economics.put("maxBudgetBrl", 100);
    Files.writeString(directory.resolve("response.json"), response.toString());
    var contract = json.createObjectNode();
    contract.put(
        "contractVersion", "drift".equals(scenario) ? "MARKET_STRATEGY_V2" : "MARKET_STRATEGY_V3");
    contract.put("status", "READY_FOR_PRIVATE_VALIDATION");
    contract
        .putObject("privateValidationPlan")
        .put("minimumIndependentReadings", 2)
        .putArray("requiredSignals")
        .add("EXPERIENCE_STARTED")
        .add("VALUE_MOMENT")
        .add("READY_RESULT_USED")
        .add("PREFERRED_OVER_FREE")
        .add("CHECKOUT_STARTED");
    var context = json.createObjectNode();
    context.putObject("learningSalesCycle").put("productId", 900004).put("experimentId", 900092);
    if (!"missing".equals(scenario)) {
      context
          .putArray("completedActivities")
          .addObject()
          .put("taskId", 900359)
          .put("activityId", "marketStrategy")
          .putObject("result")
          .set("marketStrategicContract", contract);
    }
    String source =
        "discovery".equals(scenario) ? "product-discovery-cycle:900064" : "experiment:900092";
    Map<String, Object> task =
        Map.of(
            "taskId",
            900360,
            "processVersion",
            legacy ? 5 : "later-version".equals(scenario) ? 7 : 6,
            "sourceReference",
            source,
            "processContextJson",
            context.toString());
    List<String> operations = new ArrayList<>();
    List<JsonNode> callbacks = new ArrayList<>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          String operation = Path.of(path).getFileName().toString();
          operations.add(operation);
          byte[] output = new byte[0];
          int status = 204;
          if ("automatic-execution".equals(operation)) {
            status = 200;
            output = json.writeValueAsBytes(Map.of("automaticExecutionEnabled", !stopped));
          } else if ("pending".equals(operation)) {
            require(
                exchange.getRequestURI().getQuery().contains("activityId=economics"),
                "Fila não canônica");
            status = 200;
            output = json.writeValueAsBytes(List.of(task));
          } else if (List.of("result", "failure").contains(operation)) {
            require(
                path.contains("financial-agent/stage-executions/900360/"),
                "Callback sem correlação");
            callbacks.add(json.readTree(exchange.getRequestBody().readAllBytes()));
          } else {
            status = 404;
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, output.length == 0 ? -1 : output.length);
          if (output.length > 0) exchange.getResponseBody().write(output);
          exchange.close();
        });
    server.start();
    try {
      var properties = new FinancialAgentProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      properties.setRepositoryPath(directory.toString());
      properties.setCodexCommand(
          resources.resolve("bpm/fake-codex.mjs").toAbsolutePath().toString());
      properties.setModel("fixture-plutus-no-network");
      properties.setCodexTimeout(Duration.ofSeconds(10));
      new PdeEconomicsBpmTaskConsumer(properties, json, new AutomaticExecutionControl(properties))
          .processOne();
      if (stopped) {
        require(operations.equals(List.of("automatic-execution")), "STOP consumiu tarefa");
        return;
      }
      require(
          operations.equals(
              List.of(
                  "automatic-execution", "pending", beforeModel || invalid ? "failure" : "result")),
          "Sequência divergente: " + operations);
      require(callbacks.size() == 1, "Callback ausente ou duplicado");
      JsonNode callback = callbacks.getFirst();
      JsonNode evidence = json.readTree(callback.path("evidenceJson").asText());
      require(source.equals(evidence.path("sourceReference").asText()), "Origem contaminada");
      require(
          (legacy ? "pde-commercial-plan-v4" : "pde-commercial-plan-v5")
              .equals(evidence.path("promptVersion").asText()),
          "Versão indevida para a origem da tarefa");
      if (beforeModel) {
        require(
            !Files.exists(directory.resolve("prompt.txt")), "Contrato inválido consumiu modelo");
        require(
            "CONTRACT_DRIFT".equals(callback.path("blockerGuidance").path("category").asText()),
            "Causa do bloqueio perdida");
        return;
      }
      require(
          response.equals(json.readTree(callback.path("resultJson").asText())),
          "Resposta bruta alterada");
      String prompt = Files.readString(directory.resolve("prompt.txt"));
      JsonNode schema = json.readTree(Files.readString(directory.resolve("schema.json")));
      require(
          prompt.equals(callback.path("executionAudit").path("promptSent").asText()),
          "Prompt auditado difere do executado");
      require(
          callback.path("modelUsages").get(0).path("inputTokens").asInt() == 7, "Consumo perdido");
      if (!legacy) {
        require(
            prompt.contains("YYYY-MM-DD") && prompt.contains("checkout **simulado**"),
            "Prompt privado não utilizado");
        require(
            schema
                .path("properties")
                .path("contractVersion")
                .path("enum")
                .get(0)
                .asText()
                .equals("PDE_PRIVATE_ECONOMICS_V1"),
            "Schema privado não utilizado");
        require(
            schema
                .path("properties")
                .path("economics")
                .path("properties")
                .path("deadline")
                .has("pattern"),
            "Schema perdeu prazo ISO");
      }
      if (invalid) require(!callback.path("error").asText().isBlank(), "Falha não foi registrada");
    } finally {
      server.stop(0);
      try (var files = Files.list(directory)) {
        for (Path file : files.toList()) Files.delete(file);
      }
      Files.delete(directory);
    }
  }

  /** Interrompe a homologação quando o contrato observado divergir do esperado. */
  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
}
