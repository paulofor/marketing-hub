package com.marketinghub.experimentstrategistworker;

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
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Responsabilidade: executar os pareceres de oportunidade reservados a Atena. */
@Component
public class OpportunityReviewConsumer {
  private static final Logger log = LoggerFactory.getLogger(OpportunityReviewConsumer.class);
  private static final String AGENT = "ATENA";
  private final RestClient backend;
  private final ObjectMapper json;
  private final String codex;
  private final String model;
  private final String repositoryPath;

  /** Configura o backend e o executor somente leitura especializado de Atena. */
  public OpportunityReviewConsumer(
      @Value("${BACKEND_URL:http://localhost:8080}") String backendUrl,
      @Value("${STRATEGIST_CODEX_EXECUTABLE:codex}") String codex,
      @Value("${STRATEGIST_MODEL:gpt-5.6-sol}") String model,
      @Value("${STRATEGIST_REPOSITORY_PATH:/workspace}") String repositoryPath,
      ObjectMapper json) {
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
    this.codex = codex;
    this.model = model;
    this.repositoryPath = repositoryPath;
    this.json = json;
  }

  /** Reserva e processa um parecer sem bloquear a fila estratégica principal. */
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
      if (job == null) return;
      complete(job, execute(job));
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() != 404) {
        log.error("Falha HTTP no parecer de Atena. reviewId={}", id(job), ex);
        fail(job, ex);
      }
    } catch (Exception ex) {
      log.error("Falha no parecer de oportunidade de Atena. reviewId={}", id(job), ex);
      fail(job, ex);
    }
  }

  /** Executa o prompt versionado e valida a resposta estruturada. */
  private JsonNode execute(Map<?, ?> job) throws IOException, InterruptedException {
    Path answer = Files.createTempFile("atena-opportunity-review-", ".json");
    Path schema = materialize("prompts/opportunity-review/v1/review-schema.json", ".json");
    Path processLog = Files.createTempFile("atena-opportunity-process-", ".log");
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
                  answer.toString(),
                  "--color",
                  "never"));
      if (model != null && !model.isBlank()) command.addAll(List.of("--model", model));
      Process process =
          new ProcessBuilder(command)
              .redirectErrorStream(true)
              .redirectOutput(processLog.toFile())
              .start();
      process.getOutputStream().write(prompt(job).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(40, TimeUnit.MINUTES)) {
        process.destroyForcibly();
        throw new IllegalStateException("Timeout do parecer de Atena após 40 minutos.");
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

  /** Persiste o parecer completo e sua resposta bruta. */
  private void complete(Map<?, ?> job, JsonNode result) throws IOException {
    backend
        .post()
        .uri(
            "/api/opportunity-dossiers/internal/reviews/{agent}/stage-executions/{id}/complete",
            AGENT,
            id(job))
        .body(
            Map.of(
                "decision", result.get("decision").asText(),
                "rationale", result.get("rationale").asText(),
                "risks", result.get("risks").asText(),
                "recommendation", result.get("recommendation").asText(),
                "rawModelResponse", json.writeValueAsString(result),
                "modelName", model == null || model.isBlank() ? "codex-default" : model))
        .retrieve()
        .toBodilessEntity();
  }

  /** Registra falha terminal com contexto e stack trace preservados. */
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
      log.error("Falha ao registrar erro do parecer de Atena. reviewId={}", id(job), callbackEx);
    }
  }

  /** Resolve o prompt com o contexto persistido pelo backend. */
  private String prompt(Map<?, ?> job) throws IOException {
    return read("prompts/opportunity-review/v1/review.md")
        .replace("{{AGENT_ROLE}}", "Atena, estrategista de experimentos")
        .replace("{{DOSSIER_CONTEXT}}", json.writeValueAsString(job));
  }

  /** Materializa o schema versionado para o Codex CLI. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("atena-opportunity-resource-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Rejeita respostas sem decisão e orientação acionável. */
  private void validate(JsonNode result) {
    if (!result.hasNonNull("decision")
        || !result.hasNonNull("rationale")
        || !result.hasNonNull("risks")
        || !result.hasNonNull("recommendation"))
      throw new IllegalArgumentException("Parecer de oportunidade incompleto.");
  }

  /** Extrai o identificador da execução para auditoria. */
  private Object id(Map<?, ?> job) {
    return job == null ? null : job.get("reviewId");
  }
}
