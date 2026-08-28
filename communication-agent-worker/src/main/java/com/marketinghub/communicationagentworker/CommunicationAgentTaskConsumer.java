package com.marketinghub.communicationagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: consumir as atividades BPM pertencentes exclusivamente a Íris. */
@Component
public class CommunicationAgentTaskConsumer {
  private static final Logger log = LoggerFactory.getLogger(CommunicationAgentTaskConsumer.class);
  private static final List<BpmContract> CONTRACTS =
      List.of(
          new BpmContract("pde-communication-sales-journey", "communicationContract"),
          new BpmContract("creative-production-approval", "nonAudiovisual"),
          new BpmContract("landing-page-generation", "select"),
          new BpmContract("landing-page-generation", "strategy"),
          new BpmContract("landing-page-generation", "compose"),
          new BpmContract("landing-page-generation", "html"));
  private final CommunicationAgentBackendClient backend;
  private final CommunicationAgentCodexRunner runner;
  private final CommunicationAgentProperties properties;
  private final AutomaticExecutionControl automaticExecution;
  private final ObjectMapper json;

  /** Configura fila, executor, controle PLAY/STOP e auditoria da agente. */
  public CommunicationAgentTaskConsumer(
      CommunicationAgentBackendClient backend,
      CommunicationAgentCodexRunner runner,
      CommunicationAgentProperties properties,
      AutomaticExecutionControl automaticExecution,
      ObjectMapper json) {
    this.backend = backend;
    this.runner = runner;
    this.properties = properties;
    this.automaticExecution = automaticExecution;
    this.json = json;
  }

  /** Reserva e executa no máximo uma atividade elegível por ciclo. */
  @Scheduled(cron = "25 */1 * * * *")
  public void processOne() {
    if (!automaticExecution.allowsAutomaticExecution()) return;
    Map<String, Object> task = null;
    CommunicationAgentCodexRunner.Execution execution = null;
    Instant startedAt = Instant.now();
    try {
      task = claimNext();
      if (task == null) return;
      execution = runner.run(task);
      if ("COMPLETED".equals(execution.result().path("executionStatus").asText())) {
        backend.complete(taskId(task), successPayload(task, execution, startedAt));
      } else {
        backend.fail(
            taskId(task),
            failurePayload(
                "Íris bloqueou a materialização por evidência ou contrato insuficiente.",
                task,
                execution,
                startedAt));
      }
    } catch (Exception ex) {
      log.error(
          "Falha no módulo communication-agent-worker. taskId={} processCode={} activityId={}",
          task == null ? null : task.get("taskId"),
          task == null ? null : task.get("processCode"),
          task == null ? null : task.get("activityId"),
          ex);
      if (task != null) failSafely(task, execution, startedAt, ex);
    }
  }

  /** Busca contratos suportados em ordem explícita sem consultar filas de outros agentes. */
  private Map<String, Object> claimNext() {
    for (BpmContract contract : CONTRACTS) {
      Map<String, Object> task = backend.claim(contract.processCode(), contract.activityId());
      if (task != null && !task.isEmpty()) return task;
    }
    return null;
  }

  /** Confirma se processo e atividade pertencem ao catálogo local de Íris. */
  static boolean supportsContract(String processCode, String activityId) {
    return CONTRACTS.stream()
        .anyMatch(
            contract ->
                contract.processCode().equals(processCode)
                    && contract.activityId().equals(activityId));
  }

  /** Monta o callback de sucesso com saída funcional e auditoria separadas. */
  private Map<String, Object> successPayload(
      Map<String, Object> task,
      CommunicationAgentCodexRunner.Execution execution,
      Instant startedAt) {
    Map<String, Object> payload = basePayload(task, execution, startedAt);
    payload.put("resultJson", execution.rawResponse());
    return payload;
  }

  /** Monta o callback de bloqueio preservando o parecer recebido do modelo. */
  private Map<String, Object> failurePayload(
      String error,
      Map<String, Object> task,
      CommunicationAgentCodexRunner.Execution execution,
      Instant startedAt) {
    Map<String, Object> payload = basePayload(task, execution, startedAt);
    payload.put("error", error);
    payload.put("resultJson", execution.rawResponse());
    return payload;
  }

  /** Registra falha técnica sem mascarar uma segunda falha de callback. */
  private void failSafely(
      Map<String, Object> task,
      CommunicationAgentCodexRunner.Execution execution,
      Instant startedAt,
      Exception ex) {
    try {
      CommunicationAgentCodexRunner.TokenUsage usage =
          execution != null
              ? execution.usage()
              : ex instanceof CommunicationAgentCodexRunner.ExecutionException failure
                  ? failure.usage()
                  : CommunicationAgentCodexRunner.TokenUsage.empty();
      String prompt =
          execution != null
              ? execution.promptSent()
              : ex instanceof CommunicationAgentCodexRunner.ExecutionException failure
                  ? failure.promptSent()
                  : "Falha antes da montagem do prompt.";
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("error", ex.toString());
      payload.put("evidenceJson", evidence(task, startedAt, Instant.now(), false));
      putUsage(payload, usage);
      payload.put("executionAudit", executionAudit(prompt));
      backend.fail(taskId(task), payload);
    } catch (Exception callbackEx) {
      log.error(
          "Falha ao registrar bloqueio técnico de Íris. taskId={}", task.get("taskId"), callbackEx);
    }
  }

  /** Monta campos compartilhados pelos callbacks funcionais. */
  private Map<String, Object> basePayload(
      Map<String, Object> task,
      CommunicationAgentCodexRunner.Execution execution,
      Instant startedAt) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("evidenceJson", evidence(task, startedAt, Instant.now(), true));
    putUsage(payload, execution.usage());
    payload.put("executionAudit", executionAudit(execution.promptSent()));
    return payload;
  }

  /** Serializa a linhagem operacional sem misturá-la ao pacote publicável. */
  private String evidence(
      Map<String, Object> task, Instant startedAt, Instant finishedAt, boolean modelResponded) {
    try {
      JsonNode context =
          json.readTree(String.valueOf(task.getOrDefault("processContextJson", "{}")));
      Map<String, Object> evidence = new LinkedHashMap<>();
      evidence.put("agent", "Íris");
      evidence.put("agentKey", "communication-director");
      evidence.put("taskId", task.get("taskId"));
      evidence.put("processCode", task.get("processCode"));
      evidence.put("processVersion", task.get("processVersion"));
      evidence.put("activityId", task.get("activityId"));
      evidence.put("sourceReference", task.get("sourceReference"));
      evidence.put("strategicContractReference", context.path("marketStrategicContract"));
      evidence.put(
          "communicationInputReference", context.path("communicationMaterializationContext"));
      evidence.put("startedAt", startedAt);
      evidence.put("finishedAt", finishedAt);
      evidence.put("modelResponded", modelResponded);
      evidence.put("externalSideEffects", false);
      evidence.put("published", false);
      evidence.put("externalMediaSpendUsd", 0);
      return json.writeValueAsString(evidence);
    } catch (Exception ex) {
      log.error("Falha ao serializar evidência de Íris. taskId={}", task.get("taskId"), ex);
      throw new IllegalStateException("Não foi possível serializar a evidência de Íris.", ex);
    }
  }

  /** Acrescenta somente tokens medidos pelo runtime do modelo. */
  private void putUsage(
      Map<String, Object> payload, CommunicationAgentCodexRunner.TokenUsage usage) {
    if (usage == null || !usage.informed()) return;
    payload.put(
        "modelUsages",
        List.of(
            Map.of(
                "modelCode", modelCode(),
                "serviceTier", "STANDARD",
                "inputTokens", usage.inputTokens(),
                "cachedInputTokens", usage.cachedInputTokens(),
                "outputTokens", usage.outputTokens())));
  }

  /** Monta a auditoria imutável do request enviado ao Codex. */
  private Map<String, Object> executionAudit(String prompt) {
    return Map.of(
        "modelCode",
        modelCode(),
        "reasoningEffort",
        properties.getReasoningEffort(),
        "requestedServiceTier",
        "default",
        "effectiveServiceTier",
        "STANDARD",
        "serviceTierExceptionReason",
        properties.getServiceTierExceptionReason(),
        "promptSent",
        prompt);
  }

  /** Resolve o nome do modelo sem inventar preço local. */
  private String modelCode() {
    return properties.getModel() == null || properties.getModel().isBlank()
        ? "codex-default"
        : properties.getModel();
  }

  /** Extrai o identificador obrigatório da tarefa reservada. */
  private long taskId(Map<String, Object> task) {
    return ((Number) task.get("taskId")).longValue();
  }

  /** Define uma fronteira BPM consumida por Íris. */
  private record BpmContract(String processCode, String activityId) {}
}
