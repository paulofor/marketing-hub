package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: consumir e reportar a homologação técnica determinística do PDE. */
@Component
public class PdeAgentValidationHarnessConsumer {
  private static final Logger log =
      LoggerFactory.getLogger(PdeAgentValidationHarnessConsumer.class);
  private static final String AGENT_KEY = "customer-agent";
  private static final String PROCESS_CODE = "pde-construction-approval";
  private static final String ACTIVITY_ID = "technicalHomologation";
  private static final String EXECUTION_IDENTIFIER = "pde-agent-validation-harness-v1";
  private final RestClient backend;
  private final ObjectMapper json;
  private final PdeAgentValidationHarnessRunner runner;
  private final BpmVisualEvidenceBackendClient visualEvidenceBackendClient;
  @Autowired private AutomaticExecutionControl automaticExecution;

  /** Configura fila, harness e persistência visual pelo backend principal. */
  public PdeAgentValidationHarnessConsumer(
      @org.springframework.beans.factory.annotation.Value("${BACKEND_URL:http://localhost:8080}")
          String backendUrl,
      ObjectMapper json,
      PdeAgentValidationHarnessRunner runner,
      BpmVisualEvidenceBackendClient visualEvidenceBackendClient) {
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
    this.json = json;
    this.runner = runner;
    this.visualEvidenceBackendClient = visualEvidenceBackendClient;
  }

  /** Reserva somente a etapa técnica e a conclui sem invocar modelo de IA. */
  @Scheduled(fixedDelay = 60000)
  public void processOne() {
    if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
    Map<String, Object> task = null;
    Path workDirectory = null;
    String serializedInput = "{}";
    try {
      task = claimNext();
      if (task == null) return;
      workDirectory = Files.createTempDirectory("pde-agent-validation-task-" + taskId(task) + "-");
      PdeAgentValidationHarnessRunner.HarnessExecution execution =
          runner.run(task, "TECHNICAL", null, workDirectory);
      serializedInput = execution.serializedInput();
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> uploaded =
          visualEvidenceBackendClient.uploadArtifacts(
              taskId(task), execution.visualEvidence().capture().artifacts());
      ObjectNode result = enrichResult(execution.result(), uploaded);
      if ("APPROVED".equals(result.path("decision").asText())) {
        report(task, result, serializedInput, execution.sourceUrl(), uploaded);
      } else {
        block(task, result, serializedInput, execution.sourceUrl(), uploaded);
      }
    } catch (Exception ex) {
      log.error("Falha na homologação técnica multiagente. taskId={}", taskId(task), ex);
      fail(task, ex, serializedInput);
    } finally {
      deleteDirectory(workDirectory, taskId(task));
    }
  }

  /** Informa o único contrato que este consumidor pode reservar. */
  static boolean supportsContract(String processCode, String activityId) {
    return PROCESS_CODE.equals(processCode) && ACTIVITY_ID.equals(activityId);
  }

  /** Reserva atomicamente a primeira execução elegível do harness. */
  private Map<String, Object> claimNext() {
    List<Map<String, Object>> pending =
        backend
            .get()
            .uri(
                "/api/internal/agent-tasks/{agent}/stage-executions/pending?processCode={processCode}&activityId={activityId}",
                AGENT_KEY,
                PROCESS_CODE,
                ACTIVITY_ID)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    return pending == null || pending.isEmpty() ? null : pending.getFirst();
  }

  /** Substitui caminhos locais por ids, hashes e URLs já persistidos no backend. */
  private ObjectNode enrichResult(
      JsonNode raw, List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> uploaded) {
    ObjectNode result = raw.deepCopy();
    Map<String, BpmVisualEvidenceBackendClient.UploadedVisualEvidence> byKey =
        uploaded.stream()
            .collect(
                Collectors.toMap(
                    BpmVisualEvidenceBackendClient.UploadedVisualEvidence::evidenceKey,
                    value -> value));
    ArrayNode artifacts = (ArrayNode) result.path("artifacts");
    artifacts.forEach(
        value -> {
          ObjectNode artifact = (ObjectNode) value;
          BpmVisualEvidenceBackendClient.UploadedVisualEvidence persisted =
              byKey.get(artifact.path("evidenceKey").asText());
          if (persisted == null) {
            throw new PdeAgentValidationHarnessRunner.HarnessException(
                "O backend não confirmou todos os screenshots do harness.");
          }
          artifact.remove("localPath");
          artifact.put("artifactId", persisted.id());
          artifact.put("contentUrl", persisted.contentUrl());
          artifact.put("sha256", persisted.sha256());
        });
    result
        .withArray("devices")
        .forEach(
            value ->
                replaceKeysWithIds(
                    (ObjectNode) value, "screenshotEvidenceKeys", "screenshotEvidenceIds", byKey));
    result
        .withArray("scenarios")
        .forEach(
            value ->
                replaceKeysWithIds(
                    (ObjectNode) value, "screenshotEvidenceKeys", "screenshotEvidenceIds", byKey));
    return result;
  }

  /** Converte chaves efêmeras em ids auditáveis sem aceitar item ausente. */
  private void replaceKeysWithIds(
      ObjectNode owner,
      String keyField,
      String idField,
      Map<String, BpmVisualEvidenceBackendClient.UploadedVisualEvidence> byKey) {
    ArrayNode ids = owner.putArray(idField);
    owner
        .path(keyField)
        .forEach(
            key -> {
              var persisted = byKey.get(key.asText());
              if (persisted == null) {
                throw new PdeAgentValidationHarnessRunner.HarnessException(
                    "Uma evidência visual referenciada não foi persistida.");
              }
              ids.add(persisted.id());
            });
    owner.remove(keyField);
  }

  /** Persiste resultado, custo zero, entrada integral e URLs efetivamente acessadas. */
  private void report(
      Map<String, Object> task,
      ObjectNode result,
      String input,
      String sourceUrl,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> uploaded)
      throws Exception {
    Map<String, Object> body = commonBody(task, result, input, sourceUrl, uploaded);
    backend
        .post()
        .uri(
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/result",
            AGENT_KEY,
            taskId(task))
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  /** Mantém o processo bloqueado quando algum gate determinístico é reprovado. */
  private void block(
      Map<String, Object> task,
      ObjectNode result,
      String input,
      String sourceUrl,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> uploaded)
      throws Exception {
    Map<String, Object> body = commonBody(task, result, input, sourceUrl, uploaded);
    body.put("error", "O harness reprovou um ou mais critérios técnicos do PDE.");
    body.put("blockerGuidance", blockerGuidance());
    backend
        .post()
        .uri(
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure",
            AGENT_KEY,
            taskId(task))
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  /** Monta o envelope terminal sem modelo, pessoa, campanha ou efeito externo. */
  private Map<String, Object> commonBody(
      Map<String, Object> task,
      ObjectNode result,
      String input,
      String sourceUrl,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> uploaded)
      throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("resultJson", json.writeValueAsString(result));
    body.put(
        "evidenceJson",
        json.writeValueAsString(
            Map.of(
                "evidenceType",
                "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1",
                "sourceReference",
                String.valueOf(task.get("sourceReference")),
                "visualEvidenceIds",
                uploaded.stream()
                    .map(BpmVisualEvidenceBackendClient.UploadedVisualEvidence::id)
                    .toList(),
                "trafficClass",
                "AGENT_VALIDATION",
                "humanEvidenceClaimed",
                false,
                "commercialEvidenceClaimed",
                false,
                "externalSideEffects",
                false)));
    body.put("executionAudit", executionAudit(input, sourceUrl));
    return body;
  }

  /** Registra o comando determinístico sem incluir a credencial usada pelo processo filho. */
  private Map<String, Object> executionAudit(String input, String sourceUrl) {
    Map<String, Object> audit = new LinkedHashMap<>();
    audit.put("executionMode", "DETERMINISTIC");
    audit.put("modelCode", EXECUTION_IDENTIFIER);
    audit.put("reasoningEffort", "NOT_APPLICABLE");
    audit.put("promptSent", input);
    audit.put("agentPromptPart", null);
    audit.put("activityPromptPart", input);
    audit.put(
        "accessedUrls",
        sourceUrl == null || sourceUrl.isBlank()
            ? List.of()
            : List.of(
                Map.of(
                    "url", sourceUrl,
                    "label", "PDE homologado pelo harness",
                    "accessMethod", "PLAYWRIGHT")));
    return audit;
  }

  /** Registra uma falha técnica com causa e orientação sem ocultar a stack trace local. */
  private void fail(Map<String, Object> task, Exception error, String input) {
    if (task == null) return;
    try {
      backend
          .post()
          .uri(
              "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure",
              AGENT_KEY,
              taskId(task))
          .body(
              Map.of(
                  "error",
                  error.toString(),
                  "evidenceJson",
                  json.writeValueAsString(
                      Map.of(
                          "evidenceType",
                          "PDE_AGENT_TECHNICAL_HOMOLOGATION_FAILURE_V1",
                          "sourceReference",
                          String.valueOf(task.get("sourceReference")),
                          "externalSideEffects",
                          false)),
                  "executionAudit",
                  executionAudit(input, null),
                  "blockerGuidance",
                  blockerGuidance()))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception callbackEx) {
      log.error(
          "Falha ao registrar bloqueio do harness multiagente. taskId={}",
          taskId(task),
          callbackEx);
    }
  }

  /** Orienta retorno à autoridade do protótipo em vez de forçar aprovação. */
  private Map<String, Object> blockerGuidance() {
    return Map.of(
        "category",
        "TECHNICAL_FAILURE",
        "recommendedAction",
        "Corrija no protótipo a causa registrada e execute novamente a homologação multiagente.",
        "helpLinks",
        List.of(Map.of("label", "Abrir tarefas dos agentes", "url", "/agent-tasks")));
  }

  /** Remove evidências efêmeras somente depois de concluir o callback. */
  private void deleteDirectory(Path directory, long taskId) {
    if (directory == null || !Files.exists(directory)) return;
    try (var paths = Files.walk(directory)) {
      paths
          .sorted(java.util.Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (Exception ex) {
                  log.warn(
                      "Falha ao remover evidência temporária do harness. taskId={} path={}",
                      taskId,
                      path,
                      ex);
                }
              });
    } catch (Exception ex) {
      log.warn(
          "Falha ao limpar diretório do harness. taskId={} directory={}", taskId, directory, ex);
    }
  }

  /** Extrai o id da tarefa sem falhar durante o tratamento de uma reserva ausente. */
  private static long taskId(Map<String, Object> task) {
    return task == null ? -1L : ((Number) task.get("taskId")).longValue();
  }
}
