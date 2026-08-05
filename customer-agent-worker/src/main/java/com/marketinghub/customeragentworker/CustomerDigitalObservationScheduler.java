package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Responsabilidade: executar navegacoes publicas governadas e registrar memoria observacional. */
@Component
public class CustomerDigitalObservationScheduler {
  private static final Logger log =
      LoggerFactory.getLogger(CustomerDigitalObservationScheduler.class);
  private final RestClient backend;
  private final String model;
  private final ObjectMapper mapper = new ObjectMapper();

  public CustomerDigitalObservationScheduler(
      @Value("${BACKEND_URL:http://localhost:8080}") String backendUrl,
      @Value("${CUSTOMER_AGENT_MODEL:gpt-5.6-sol}") String model) {
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
    this.model = model;
  }

  /** Consulta uma observacao pendente sem criar navegacao quando a fila estiver vazia. */
  @Scheduled(fixedDelayString = "${CUSTOMER_AGENT_OBSERVATION_POLL_MS:60000}")
  void runPending() {
    try {
      Map<?, ?> job =
          backend
              .post()
              .uri("/api/customer-agent/v1/internal/digital-observations/pending/claim")
              .retrieve()
              .body(Map.class);
      if (job != null) observe(job);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() != 404) {
        log.error("Falha no customer-agent ao consultar observacao pendente", ex);
        throw ex;
      }
    }
  }

  /** Executa o Codex em sandbox e persiste separadamente as camadas da experiencia. */
  private void observe(Map<?, ?> job) {
    long id = ((Number) job.get("id")).longValue();
    try {
      String template =
          Files.readString(
              Path.of("/app/prompts/customer-agent/v1/digital-observation.md"),
              StandardCharsets.UTF_8);
      String prompt =
          template
              .replace("{{PERSONA_JSON}}", String.valueOf(job.get("persona")))
              .replace("{{OBJECTIVE}}", String.valueOf(job.get("objective")))
              .replace(
                  "{{AUTHORIZED_SOURCES_JSON}}", String.valueOf(job.get("authorizedSourcesJson")));
      Path output = Files.createTempFile("customer-agent-observation-", ".json");
      Process process =
          new ProcessBuilder(
                  "codex",
                  "exec",
                  "--sandbox",
                  "read-only",
                  "--model",
                  model,
                  "--skip-git-repo-check",
                  "--output-schema",
                  "/app/prompts/customer-agent/v1/digital-observation-schema.json",
                  prompt)
              .redirectErrorStream(true)
              .redirectOutput(output.toFile())
              .start();
      if (!process.waitFor(10, TimeUnit.MINUTES)) {
        process.destroyForcibly();
        throw new IllegalStateException("Timeout da experiencia digital.");
      }
      String raw = Files.readString(output, StandardCharsets.UTF_8);
      Files.deleteIfExists(output);
      if (process.exitValue() != 0) throw new IllegalStateException("Codex falhou: " + raw);
      Map<String, Object> result = mapper.readValue(raw, new TypeReference<>() {});
      backend
          .post()
          .uri("/api/customer-agent/v1/internal/digital-observations/{id}/complete", id)
          .body(
              Map.of(
                  "observationJson", mapper.writeValueAsString(result.get("observation")),
                  "simulatedReactionJson",
                      mapper.writeValueAsString(result.get("simulatedReaction")),
                  "commercialHypothesisJson",
                      mapper.writeValueAsString(result.get("commercialHypothesis")),
                  "rawModelResponse", raw,
                  "model", model))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception ex) {
      log.error("Falha no customer-agent ao observar experiencia digital, observationId={}", id, ex);
      reportFailure(id, ex);
      throw new IllegalStateException("Falha na experiencia digital id=" + id, ex);
    }
  }

  /** Registra a falha terminal para liberar a fila e preservar a auditoria. */
  private void reportFailure(long id, Exception ex) {
    try {
      backend
          .post()
          .uri("/api/customer-agent/v1/internal/digital-observations/{id}/fail", id)
          .body(Map.of("error", String.valueOf(ex.getMessage())))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception callbackEx) {
      log.error("Falha ao registrar erro da observacao, observationId={}", id, callbackEx);
    }
  }
}
