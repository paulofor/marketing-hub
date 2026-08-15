package com.marketinghub.customeragentworker;

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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: executar as atividades BPM de percepção de cliente atribuídas à Psique. */
@Component
public class CustomerBpmTaskConsumer {
  private static final Logger log = LoggerFactory.getLogger(CustomerBpmTaskConsumer.class);
  private static final String AGENT_KEY = "customer-agent";
  private final RestClient backend;
  private final ObjectMapper json;
  private final String codex;
  private final String model;
  private final String repositoryPath;

  /** Configura a fila canônica, o modelo e a sandbox somente leitura de Psique. */
  public CustomerBpmTaskConsumer(
      @Value("${BACKEND_URL:http://localhost:8080}") String backendUrl,
      @Value("${CUSTOMER_AGENT_CODEX_EXECUTABLE:codex}") String codex,
      @Value("${CUSTOMER_AGENT_MODEL:gpt-5.6-sol}") String model,
      @Value("${CUSTOMER_AGENT_REPOSITORY_PATH:/workspace}") String repositoryPath,
      ObjectMapper json) {
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
    this.codex = codex;
    this.model = model;
    this.repositoryPath = repositoryPath;
    this.json = json;
  }

  /** Reserva e avalia uma atividade liberada sem escolher a próxima etapa do processo. */
  @Scheduled(fixedDelay = 60000)
  public void processOne() {
    Map<String, Object> task = null;
    try {
      List<Map<String, Object>> pending =
          backend
              .get()
              .uri(
                  "/api/internal/agent-tasks/{agent}/stage-executions/pending?processCode=landing-page-generation&activityId=customer",
                  AGENT_KEY)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});
      if (pending == null || pending.isEmpty()) return;
      task = pending.get(0);
      JsonNode result = execute(task);
      if ("APPROVED".equals(result.path("decision").asText())) report(task, result);
      else block(task, result);
    } catch (Exception ex) {
      log.error("Falha na atividade BPM de Psique. taskId={}", taskId(task), ex);
      fail(task, ex);
    }
  }

  /** Executa o prompt versionado e exige um parecer estruturado sobre a experiência da cliente. */
  JsonNode execute(Map<String, Object> task) throws IOException, InterruptedException {
    Path output = Files.createTempFile("psique-bpm-result-", ".json");
    Path processLog = Files.createTempFile("psique-bpm-process-", ".log");
    Path schema = materialize("prompts/bpm/landing-customer-review-schema.json", ".json");
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
        throw new IllegalStateException("Timeout da atividade BPM de Psique após 40 minutos.");
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

  /** Persiste o parecer e as evidências na própria atividade BPM. */
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
                    json.writeValueAsString(Map.of("reviewer", "Psique", "model", model))))
        .retrieve()
        .toBodilessEntity();
  }

  /** Mantém o gate fechado quando o parecer não pôde ser produzido ou persistido. */
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
      log.error("Falha ao registrar bloqueio BPM de Psique. taskId={}", taskId(task), callbackEx);
    }
  }

  /** Preserva o parecer funcional e impede avanço quando a cliente exige ajuste. */
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
                "Psique bloqueou o avanço: " + result.path("customerPerspective").asText(),
                "resultJson",
                resultJson,
                "evidenceJson",
                json.writeValueAsString(Map.of("reviewer", "Psique", "model", model))))
        .retrieve()
        .toBodilessEntity();
  }

  /** Resolve o contexto da atividade sem consultar banco ou decidir o avanço do processo. */
  private String prompt(Map<String, Object> task) throws IOException {
    return read("prompts/bpm/landing-customer-review.md")
        .replace("{{TASK_CONTEXT}}", json.writeValueAsString(task));
  }

  /** Valida que clareza, confiança, valor e objeções receberam decisão explícita. */
  static void validate(JsonNode result) {
    if (!List.of("APPROVED", "ADJUST", "BLOCKED").contains(result.path("decision").asText())) {
      throw new IllegalArgumentException("Parecer de Psique sem decisão válida");
    }
    if (result.path("customerPerspective").asText().isBlank()
        || result.path("evidence").isEmpty()
        || result.path("requiredChanges").isMissingNode()) {
      throw new IllegalArgumentException("Parecer de Psique sem evidências suficientes");
    }
  }

  /** Extrai o identificador estável da tarefa reservada. */
  private static long taskId(Map<String, Object> task) {
    return task == null ? -1L : ((Number) task.get("taskId")).longValue();
  }

  /** Materializa o schema versionado apenas durante a execução. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("psique-bpm-schema-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
