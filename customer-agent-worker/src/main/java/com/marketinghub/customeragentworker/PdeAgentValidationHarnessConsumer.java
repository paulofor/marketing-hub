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

  @org.springframework.beans.factory.annotation.Value(
      "${CUSTOMER_AGENT_BPM_STATE_DIRECTORY:${java.io.tmpdir}/psique-bpm}")
  private String stateDirectory = System.getProperty("java.io.tmpdir") + "/psique-bpm";

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

  /** Recupera captura e callback da etapa técnica, sem invocar modelo nem repetir a entrega. */
  @Scheduled(fixedDelay = 60000)
  public void processOne() {
    if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
    var directory = Path.of(stateDirectory).resolve("technical-harness");
    var outbox = new CustomerBpmTaskOutbox(directory, json);
    Map<String, Object> task = null;
    try (var lock = outbox.lock()) {
      if (lock == null) return;
      var pending = outbox.read();
      if (pending == null) {
        task = claimNext();
        if (task == null) return;
        pending = new CustomerBpmTaskOutbox.Pending(task, Map.of(), List.of(), false, null, null);
        outbox.save(pending);
      } else task = pending.task();
      if (pending.callback() == null) {
        Map<String, Object> callback;
        String operation;
        try {
          var workDirectory = directory.resolve("task-" + taskId(task));
          var execution = runner.run(task, "TECHNICAL", null, workDirectory);
          var uploaded =
              visualEvidenceBackendClient.uploadArtifacts(
                  taskId(task), execution.visualEvidence().capture().artifacts());
          var result = enrichResult(execution.result(), uploaded);
          callback =
              commonBody(
                  task, result, execution.serializedInput(), execution.sourceUrl(), uploaded);
          operation = "APPROVED".equals(result.path("decision").asText()) ? "result" : "failure";
          if ("failure".equals(operation)) {
            callback.put("error", "O harness reprovou um ou mais critérios técnicos do PDE.");
            callback.put("blockerGuidance", blockerGuidance());
          }
        } catch (Exception ex) {
          log.error("Falha na homologação técnica multiagente. taskId={}", taskId(task), ex);
          operation = "failure";
          Path input =
              directory.resolve("task-" + taskId(task)).resolve("agent-validation-input.json");
          callback =
              failureBody(task, ex, Files.isRegularFile(input) ? Files.readString(input) : "{}");
        }
        pending =
            new CustomerBpmTaskOutbox.Pending(
                task, Map.of(), List.of(), false, operation, callback);
        outbox.save(pending);
      }
      backend
          .post()
          .uri(
              "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/{operation}",
              AGENT_KEY,
              taskId(task),
              pending.operation())
          .body(pending.callback())
          .retrieve()
          .toBodilessEntity();
      outbox.acknowledge();
    } catch (Exception ex) {
      log.error(
          "Entrega do harness preservada para reenvio sem nova execução. taskId={}",
          taskId(task),
          ex);
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

  /** Vincula jornadas e encerramentos a ids, hashes e URLs já persistidos no backend. */
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
            throw PdeAgentValidationHarnessRunner.HarnessException.executor(
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
    result
        .path("terminalResultEvidence")
        .path("cases")
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
                throw PdeAgentValidationHarnessRunner.HarnessException.executor(
                    "Uma evidência visual referenciada não foi persistida.");
              }
              ids.add(persisted.id());
            });
    owner.remove(keyField);
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

  /** Separa mensagem de negócio e diagnóstico sanitizado antes de persistir o callback original. */
  private Map<String, Object> failureBody(Map<String, Object> task, Exception error, String input)
      throws Exception {
    return Map.of(
        "error",
        "EXECUTOR_FAILURE".equals(failureGuidance(error).get("category"))
            ? "PDE_HARNESS_EXECUTOR_FAILURE_V1|A homologação foi interrompida; recupere a tentativa no executor antes de retomar."
            : "Não foi possível homologar esta versão. A causa observada e as provas precisam de correção antes de repetir os testes.",
        "evidenceJson",
        json.writeValueAsString(
            Map.of(
                "evidenceType",
                "PDE_AGENT_TECHNICAL_HOMOLOGATION_FAILURE_V1",
                "sourceReference",
                String.valueOf(task.get("sourceReference")),
                "diagnostic",
                sanitizedDiagnostic(error),
                "externalSideEffects",
                false)),
        "executionAudit",
        executionAudit(input, null),
        "blockerGuidance",
        failureGuidance(error));
  }

  /** Conserva diagnóstico limitado sem credenciais de acesso, cookies ou cabeçalhos privados. */
  private static String sanitizedDiagnostic(Exception error) {
    String diagnostic =
        error
            .toString()
            .replaceAll("[a-fA-F0-9]{64}", "[redigido]")
            .replaceAll(
                "(?i)(accessKey|recoveryKey|sessionToken|Cookie|X-PDE-Internal-Token)[^\\r\\n]*",
                "$1=[redigido]");
    return diagnostic.substring(0, Math.min(8000, diagnostic.length()));
  }

  /** Orienta retorno à autoridade do protótipo em vez de forçar aprovação. */
  private static Map<String, Object> blockerGuidance() {
    return Map.of(
        "category",
        "TECHNICAL_FAILURE",
        "recommendedAction",
        "Corrija no protótipo a causa registrada e execute novamente a homologação multiagente.",
        "helpLinks",
        List.of(Map.of("label", "Abrir tarefas dos agentes", "url", "/agent-tasks")));
  }

  /**
   * Separa indisponibilidade do executor de uma falha observada no protótipo para preservar a mesma
   * versão quando o navegador nem chegou a homologá-la.
   */
  static Map<String, Object> failureGuidance(Exception error) {
    boolean executorFailure =
        !(error instanceof PdeAgentValidationHarnessRunner.HarnessException harnessError)
            || harnessError.isExecutorFailure();
    if (!executorFailure) return blockerGuidance();
    return Map.of(
        "category",
        "EXECUTOR_FAILURE",
        "recommendedAction",
        "Corrija o executor ou o catálogo do harness e repita a mesma homologação, sem criar nova versão do protótipo.",
        "helpLinks",
        List.of(Map.of("label", "Abrir tarefas dos agentes", "url", "/agent-tasks")));
  }

  /** Extrai o id da tarefa sem falhar durante o tratamento de uma reserva ausente. */
  private static long taskId(Map<String, Object> task) {
    return task == null ? -1L : ((Number) task.get("taskId")).longValue();
  }
}
