package com.marketinghub.metaadapproverworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: executar os gates comerciais BPM atribuídos à Têmis. */
@Component
@ConditionalOnProperty(
    name = "meta-ad-approver.execution-role",
    havingValue = "review",
    matchIfMissing = true)
public class CommercialBpmTaskConsumer {
  private static final Logger log = LoggerFactory.getLogger(CommercialBpmTaskConsumer.class);
  private static final String AGENT_KEY = "meta-ad-approver";
  private static final List<BpmContract> CONTRACTS =
      List.of(
          new BpmContract("creative-production-approval", "library"),
          new BpmContract("creative-production-approval", "brief"),
          new BpmContract("creative-production-approval", "generate"),
          new BpmContract("creative-production-approval", "commercial"),
          new BpmContract("landing-page-generation", "commercial"));
  private final RestClient backend;
  private final ObjectMapper json;
  private final String codex;
  private final String model;
  private final String repositoryPath;

  /** Configura a fila canônica e a sandbox independente de Têmis. */
  public CommercialBpmTaskConsumer(
      MetaAdApproverProperties properties,
      @Value("${CODEX_COMMAND:codex}") String codex,
      @Value("${CODEX_MODEL:gpt-5.6-sol}") String model,
      @Value("${MARKETING_HUB_REPOSITORY:/workspace/marketing-hub}") String repositoryPath,
      ObjectMapper json) {
    this.backend = BackendRestClientFactory.create(properties);
    this.codex = codex;
    this.model = model;
    this.repositoryPath = repositoryPath;
    this.json = json;
  }

  /** Reserva e revisa uma atividade liberada sem decidir a próxima etapa. */
  @Scheduled(cron = "45 */1 * * * *")
  public void processOne() {
    Map<String, Object> task = null;
    try {
      task = claimNext();
      if (task == null) return;
      JsonNode result = execute(task);
      if ("APPROVED".equals(result.path("decision").asText())) report(task, result);
      else block(task, result);
    } catch (Exception ex) {
      log.error("Falha no gate comercial BPM de Têmis. taskId={}", taskId(task), ex);
      fail(task, ex);
    }
  }

  /** Reserva primeiro criativos e depois landings sem cruzar gates independentes. */
  private Map<String, Object> claimNext() {
    for (BpmContract contract : CONTRACTS) {
      List<Map<String, Object>> pending =
          backend
              .get()
              .uri(
                  "/api/internal/agent-tasks/{agent}/stage-executions/pending?processCode={processCode}&activityId={activityId}",
                  AGENT_KEY,
                  contract.processCode(),
                  contract.activityId())
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      if (pending != null && !pending.isEmpty()) return pending.get(0);
    }
    return null;
  }

  /** Executa o prompt versionado e valida coerência, compliance e prontidão comercial. */
  JsonNode execute(Map<String, Object> task) throws IOException, InterruptedException {
    Path output = Files.createTempFile("temis-bpm-result-", ".json");
    Path processLog = Files.createTempFile("temis-bpm-process-", ".log");
    Path schema = materialize(schemaResourceFor(processCode(task)), ".json");
    try {
      List<String> command =
          new ArrayList<>(
              List.of(
                  codex,
                  "--search",
                  "exec",
                  "-",
                  "--skip-git-repo-check",
                  "--sandbox",
                  "read-only",
                  "--cd",
                  repositoryPath,
                  "--output-schema",
                  schema.toString(),
                  "--output-last-message",
                  output.toString(),
                  "--color",
                  "never"));
      if (model != null && !model.isBlank()) command.addAll(List.of("--model", model));
      Process process =
          new ProcessBuilder(command)
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile())
              .start();
      process.getOutputStream().write(prompt(task).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(40, TimeUnit.MINUTES)) {
        process.destroyForcibly();
        throw new IllegalStateException("Timeout do gate BPM de Têmis após 40 minutos.");
      }
      if (process.exitValue() != 0) {
        throw new IllegalStateException(
            "Codex encerrou com falha: " + Files.readString(processLog));
      }
      JsonNode result = json.readTree(Files.readString(output));
      validate(result);
      return result;
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processLog);
      Files.deleteIfExists(schema);
    }
  }

  /** Persiste a decisão auditável sem publicar landing, campanha ou experimento. */
  private void report(Map<String, Object> task, JsonNode result) throws IOException {
    backend
        .post()
        .uri(
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/result",
            AGENT_KEY,
            taskId(task))
        .body(
            Map.of(
                "resultJson", json.writeValueAsString(result),
                "evidenceJson",
                    json.writeValueAsString(Map.of("reviewer", "Têmis", "model", model))))
        .retrieve()
        .toBodilessEntity();
  }

  /** Mantém o gate fechado diante de falha técnica ou parecer inválido. */
  private void fail(Map<String, Object> task, Exception ex) {
    if (task == null) return;
    try {
      backend
          .post()
          .uri(
              "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure",
              AGENT_KEY,
              taskId(task))
          .body(Map.of("error", ex.toString()))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception callbackEx) {
      log.error("Falha ao registrar bloqueio BPM de Têmis. taskId={}", taskId(task), callbackEx);
    }
  }

  /** Preserva o parecer funcional e mantém o processo fechado quando o gate reprova. */
  private void block(Map<String, Object> task, JsonNode result) throws IOException {
    String resultJson = json.writeValueAsString(result);
    backend
        .post()
        .uri(
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure",
            AGENT_KEY,
            taskId(task))
        .body(
            Map.of(
                "error",
                "Têmis bloqueou o avanço: " + result.path("commercialRationale").asText(),
                "resultJson",
                resultJson,
                "evidenceJson",
                json.writeValueAsString(Map.of("reviewer", "Têmis", "model", model))))
        .retrieve()
        .toBodilessEntity();
  }

  /** Resolve o contexto congelado da tarefa no prompt versionado. */
  private String prompt(Map<String, Object> task) throws IOException {
    return read(promptResourceFor(processCode(task)))
        .replace("{{TASK_CONTEXT}}", json.writeValueAsString(task));
  }

  /** Seleciona o prompt versionado específico do gate avaliado. */
  static String promptResourceFor(String processCode) {
    return "creative-production-approval".equals(processCode)
        ? "prompts/bpm/creative-commercial-review.md"
        : "prompts/bpm/landing-commercial-review.md";
  }

  /** Seleciona o schema versionado específico do gate avaliado. */
  static String schemaResourceFor(String processCode) {
    return "creative-production-approval".equals(processCode)
        ? "prompts/bpm/creative-commercial-review-schema.json"
        : "prompts/bpm/landing-commercial-review-schema.json";
  }

  /** Lê o processo congelado no contrato da tarefa reservada. */
  private String processCode(Map<String, Object> task) {
    Object value = task.get("processCode");
    return value == null ? "" : value.toString();
  }

  /** Exige decisão, evidências e coerência explícita com o plano comercial. */
  static void validate(JsonNode result) {
    if (!List.of("APPROVED", "ADJUST", "BLOCKED").contains(result.path("decision").asText())) {
      throw new IllegalArgumentException("Gate de Têmis sem decisão válida");
    }
    if (result.path("commercialRationale").asText().isBlank()
        || result.path("evidence").isEmpty()
        || result.path("requiredChanges").isMissingNode()) {
      throw new IllegalArgumentException("Gate de Têmis sem evidências suficientes");
    }
  }

  /** Extrai o identificador estável da tarefa reservada. */
  private static long taskId(Map<String, Object> task) {
    return task == null ? -1L : ((Number) task.get("taskId")).longValue();
  }

  /** Materializa o schema versionado apenas durante a execução. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("temis-bpm-schema-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Define uma fronteira BPM suportada pelo executor revisor de Têmis. */
  private record BpmContract(String processCode, String activityId) {}
}
