package com.marketinghub.growthoperatorworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Responsabilidade: executar os pareceres de oportunidade reservados a Hermes. */
@Component
public class OpportunityReviewConsumer {
  private static final Logger log = LoggerFactory.getLogger(OpportunityReviewConsumer.class);
  private static final String AGENT = "HERMES";
  private final RestClient backend;
  private final ObjectMapper json;
  private final WorkerProperties properties;

  /** Configura backend e executor somente leitura de Hermes. */
  public OpportunityReviewConsumer(WorkerProperties properties, ObjectMapper json) {
    this.properties = properties;
    this.backend = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
    this.json = json;
  }

  /** Reserva um parecer sem bloquear os diagnósticos dos planos comerciais. */
  @Scheduled(fixedDelay = 60000)
  public void processOne() {
    Map<?, ?> job = null;
    try {
      job =
          backend
              .post()
              .uri(
                  "/api/opportunity-dossiers/internal/reviews/{agent}/stage-executions/pending",
                  AGENT)
              .retrieve()
              .body(Map.class);
      if (job != null) complete(job, execute(job));
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() != 404) {
        log.error("Falha HTTP no parecer de Hermes. reviewId={}", id(job), ex);
        fail(job, ex);
      }
    } catch (Exception ex) {
      log.error("Falha no parecer de oportunidade de Hermes. reviewId={}", id(job), ex);
      fail(job, ex);
    }
  }

  /** Executa o prompt coordenador versionado e valida o resultado. */
  private JsonNode execute(Map<?, ?> job) throws IOException, InterruptedException {
    Path answer = Files.createTempFile("hermes-opportunity-review-", ".json");
    Path schema = materialize("prompts/opportunity-review/v1/review-schema.json", ".json");
    Path processLog = Files.createTempFile("hermes-opportunity-process-", ".log");
    try {
      List<String> command =
          new ArrayList<>(
              List.of(
                  properties.getCodexCommand(),
                  "--search",
                  "exec",
                  "-",
                  "--skip-git-repo-check",
                  "--sandbox",
                  "read-only",
                  "--cd",
                  properties.getRepositoryPath(),
                  "--output-schema",
                  schema.toString(),
                  "--output-last-message",
                  answer.toString(),
                  "--color",
                  "never"));
      if (properties.getModel() != null && !properties.getModel().isBlank())
        command.addAll(List.of("--model", properties.getModel()));
      Process process =
          new ProcessBuilder(command)
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile())
              .start();
      process.getOutputStream().write(prompt(job).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(properties.getCodexTimeout().toMinutes(), TimeUnit.MINUTES)) {
        process.destroyForcibly();
        throw new IllegalStateException("Timeout do parecer de Hermes.");
      }
      if (process.exitValue() != 0)
        throw new IllegalStateException(
            "Codex encerrou com falha: " + Files.readString(processLog));
      JsonNode result = json.readTree(Files.readString(answer));
      validate(result);
      return result;
    } finally {
      Files.deleteIfExists(answer);
      Files.deleteIfExists(schema);
      Files.deleteIfExists(processLog);
    }
  }

  /** Persiste a coordenação recomendada sem executar decisões. */
  private void complete(Map<?, ?> job, JsonNode result) throws IOException {
    backend
        .post()
        .uri(
            "/api/opportunity-dossiers/internal/reviews/{agent}/stage-executions/{id}/complete",
            AGENT,
            id(job))
        .body(
            Map.of(
                "decision",
                result.get("decision").asText(),
                "rationale",
                result.get("rationale").asText(),
                "risks",
                result.get("risks").asText(),
                "recommendation",
                result.get("recommendation").asText(),
                "rawModelResponse",
                json.writeValueAsString(result),
                "modelName",
                properties.getModel() == null || properties.getModel().isBlank()
                    ? "codex-default"
                    : properties.getModel()))
        .retrieve()
        .toBodilessEntity();
  }

  /** Registra falha técnica com contexto operacional. */
  private void fail(Map<?, ?> job, Exception ex) {
    if (job == null) return;
    try {
      backend
          .post()
          .uri(
              "/api/opportunity-dossiers/internal/reviews/{agent}/stage-executions/{id}/fail",
              AGENT,
              id(job))
          .body(Map.of("errorMessage", ex.toString()))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception callbackEx) {
      log.error("Falha ao registrar erro do parecer de Hermes. reviewId={}", id(job), callbackEx);
    }
  }

  /** Resolve o prompt com o contexto auditável. */
  private String prompt(Map<?, ?> job) throws IOException {
    return read("prompts/opportunity-review/v1/review.md")
        .replace("{{DOSSIER_CONTEXT}}", json.writeValueAsString(job));
  }

  /** Materializa um recurso versionado. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("hermes-opportunity-resource-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Rejeita respostas incompletas. */
  private void validate(JsonNode result) {
    if (!result.hasNonNull("decision")
        || !result.hasNonNull("rationale")
        || !result.hasNonNull("risks")
        || !result.hasNonNull("recommendation"))
      throw new IllegalArgumentException("Parecer de oportunidade incompleto.");
  }

  /** Extrai o identificador para auditoria. */
  private Object id(Map<?, ?> job) {
    return job == null ? null : job.get("reviewId");
  }
}
