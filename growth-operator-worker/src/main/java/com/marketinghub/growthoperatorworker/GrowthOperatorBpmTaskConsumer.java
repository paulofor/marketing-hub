package com.marketinghub.growthoperatorworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: consumir as atividades BPM comerciais atribuídas a Hermes. */
@Component
public class GrowthOperatorBpmTaskConsumer {
  private static final Logger log = LoggerFactory.getLogger(GrowthOperatorBpmTaskConsumer.class);
  private static final List<BpmContract> CONTRACTS =
      List.of(
          new BpmContract("operacao-otimizacao-experimento", "task-1"),
          new BpmContract("operacao-otimizacao-experimento", "task-2"),
          new BpmContract("operacao-otimizacao-experimento", "task-3"),
          new BpmContract("operacao-otimizacao-experimento", "task-4"),
          new BpmContract("operacao-otimizacao-experimento", "task-10"));
  private final GrowthOperatorBackendClient backend;
  private final GrowthOperatorBpmRunner runner;
  private final WorkerProperties properties;
  private final ObjectMapper json;
  @Autowired private AutomaticExecutionControl automaticExecution;

  /** Configura a fila canônica, o executor somente leitura e a auditoria de Hermes. */
  public GrowthOperatorBpmTaskConsumer(
      GrowthOperatorBackendClient backend,
      GrowthOperatorBpmRunner runner,
      WorkerProperties properties,
      ObjectMapper json) {
    this.backend = backend;
    this.runner = runner;
    this.properties = properties;
    this.json = json;
  }

  /** Reserva em PLAY no máximo uma atividade elegível por ciclo. */
  @Scheduled(cron = "15 */1 * * * *")
  public void processOne() {
    if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
    Map<String, Object> task = null;
    GrowthOperatorBpmRunner.BpmExecution execution = null;
    try {
      task = claimNext();
      if (task == null) return;
      task = requireMarketStrategicContract(task);
      execution = runner.run(task);
      if ("COMPLETED".equals(execution.result().path("executionStatus").asText())) {
        backend.completeBpmTask(taskId(task), payload(task, execution));
      } else {
        backend.failBpmTask(
            taskId(task),
            failurePayload(
                "Hermes bloqueou o avanço: "
                    + execution.result().path("recommendedAction").asText(),
                task,
                execution));
      }
    } catch (Exception ex) {
      log.error(
          "Falha no módulo growth-operator-worker ao executar atividade BPM. taskId={} sourceReference={}",
          taskIdOrNull(task),
          task == null ? null : task.get("sourceReference"),
          ex);
      if (task != null) {
        GrowthOperatorBpmRunner.TokenUsage usage =
            execution != null
                ? execution.usage()
                : ex instanceof GrowthOperatorBpmRunner.BpmExecutionException bpm
                    ? bpm.usage()
                    : GrowthOperatorBpmRunner.TokenUsage.empty();
        List<JsonNode> toolUsage =
            execution != null
                ? execution.toolUsage()
                : ex instanceof GrowthOperatorBpmRunner.BpmExecutionException bpm
                    ? bpm.toolUsage()
                    : List.of();
        failCallback(task, ex, usage, toolUsage);
      }
    }
  }

  /** Busca somente os contratos suportados por Hermes em ordem explícita. */
  private Map<String, Object> claimNext() {
    for (BpmContract contract : CONTRACTS) {
      Map<String, Object> task =
          backend.claimBpmTask(contract.processCode(), contract.activityId());
      if (task != null && !task.isEmpty()) return task;
    }
    return null;
  }

  /** Confirma que Hermes consome somente atividades operacionais posteriores à autorização. */
  static boolean supportsContract(String processCode, String activityId) {
    return CONTRACTS.stream()
        .anyMatch(
            contract ->
                contract.processCode().equals(processCode)
                    && contract.activityId().equals(activityId));
  }

  /** Bloqueia antes do modelo quando Atena ainda não entregou uma estratégia operável. */
  Map<String, Object> requireMarketStrategicContract(Map<String, Object> task) throws Exception {
    JsonNode processContext =
        json.readTree(String.valueOf(task.getOrDefault("processContextJson", "{}")));
    JsonNode wrapper = processContext.path("marketStrategicContract");
    JsonNode contract = wrapper.path("contract");
    if (!"AVAILABLE".equals(wrapper.path("availability").asText())
        || !"MARKET_STRATEGY_V2".equals(wrapper.path("contractVersion").asText())
        || !wrapper.path("contentHash").asText().matches("[0-9a-f]{64}")
        || !"MARKET_STRATEGY_V2".equals(contract.path("contractVersion").asText())
        || !"READY_FOR_OPERATION".equals(contract.path("status").asText())
        || !"ATENA_DEFINES_STRATEGY_HERMES_OPERATES_GROWTH"
            .equals(contract.path("operatorBoundary").asText())) {
      throw new IllegalStateException(
          "Atena precisa produzir um Contrato Estratégico de Mercado v2 pronto antes de Hermes operar crescimento.");
    }
    Map<String, Object> enriched = new HashMap<>(task);
    enriched.put("marketStrategicContract", json.convertValue(wrapper, Object.class));
    return enriched;
  }

  /** Monta o callback de sucesso com parecer, fontes consultadas e tokens medidos. */
  private Map<String, Object> payload(
      Map<String, Object> task, GrowthOperatorBpmRunner.BpmExecution execution) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("resultJson", json.writeValueAsString(execution.result()));
    body.put("evidenceJson", evidence(task, execution.toolUsage()));
    putModelUsage(body, execution.usage());
    return body;
  }

  /** Monta o callback funcional que mantém o processo fechado diante de evidência insuficiente. */
  private Map<String, Object> failurePayload(
      String error, Map<String, Object> task, GrowthOperatorBpmRunner.BpmExecution execution)
      throws Exception {
    Map<String, Object> body = payload(task, execution);
    body.put("error", error);
    return body;
  }

  /** Registra uma falha técnica sem perder o consumo medido antes da interrupção. */
  private void failCallback(
      Map<String, Object> task,
      Exception ex,
      GrowthOperatorBpmRunner.TokenUsage usage,
      List<JsonNode> toolUsage) {
    try {
      Map<String, Object> body = new HashMap<>();
      body.put("error", ex.toString());
      body.put("evidenceJson", evidence(task, toolUsage));
      putModelUsage(body, usage);
      backend.failBpmTask(taskId(task), body);
    } catch (Exception callbackEx) {
      log.error(
          "Falha ao registrar bloqueio da atividade BPM de Hermes. taskId={}",
          taskIdOrNull(task),
          callbackEx);
    }
  }

  /** Serializa a evidência operacional sem misturá-la com o resultado funcional. */
  private String evidence(Map<String, Object> task, List<JsonNode> toolUsage) throws Exception {
    Map<String, Object> evidence = new HashMap<>();
    evidence.put("agent", "Hermes");
    evidence.put("model", modelCode());
    evidence.put("sourceReference", String.valueOf(task.get("sourceReference")));
    evidence.put("activityId", String.valueOf(task.get("activityId")));
    evidence.put("accessMode", "READ_ONLY");
    evidence.put("externalSideEffects", false);
    evidence.put("toolUsage", toolUsage);
    if (task.containsKey("marketStrategicContract")) {
      JsonNode contract = json.valueToTree(task.get("marketStrategicContract"));
      evidence.put(
          "marketStrategicContractReference",
          Map.of(
              "strategistExecutionId",
              contract.path("strategistExecutionId").asLong(),
              "contractVersion",
              contract.path("contractVersion").asText(),
              "contentHash",
              contract.path("contentHash").asText()));
    }
    return json.writeValueAsString(evidence);
  }

  /** Acrescenta ao callback somente contadores realmente informados pelo Codex. */
  private void putModelUsage(Map<String, Object> body, GrowthOperatorBpmRunner.TokenUsage usage) {
    if (usage == null || !usage.informed()) return;
    body.put(
        "modelUsages",
        List.of(
            Map.of(
                "modelCode", modelCode(),
                "serviceTier", "STANDARD",
                "inputTokens", usage.inputTokens(),
                "cachedInputTokens", usage.cachedInputTokens(),
                "outputTokens", usage.outputTokens())));
  }

  /** Resolve o nome real do modelo sem inventar uma tarifa local. */
  private String modelCode() {
    return properties.getModel() == null || properties.getModel().isBlank()
        ? "codex-default"
        : properties.getModel();
  }

  /** Extrai o identificador obrigatório da tarefa reservada. */
  private long taskId(Map<String, Object> task) {
    return ((Number) task.get("taskId")).longValue();
  }

  /** Extrai o identificador quando disponível para contextualizar logs. */
  private Object taskIdOrNull(Map<String, Object> task) {
    return task == null ? null : task.get("taskId");
  }

  /** Define uma fronteira BPM suportada pelo executor de Hermes. */
  private record BpmContract(String processCode, String activityId) {}
}
