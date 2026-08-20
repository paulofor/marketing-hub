package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
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
  private final BrowserObservationRunner browserObservationRunner;
  private final CodexObservationAnalyzer observationAnalyzer;
  @Autowired private AutomaticExecutionControl automaticExecution;

  /** Inicializa o consumo da fila observacional e as integrações controladas da execução. */
  public CustomerDigitalObservationScheduler(
      @Value("${BACKEND_URL:http://localhost:8080}") String backendUrl,
      @Value("${CUSTOMER_AGENT_MODEL:gpt-5.6-sol}") String model,
      BrowserObservationRunner browserObservationRunner,
      CodexObservationAnalyzer observationAnalyzer) {
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
    this.model = model;
    this.browserObservationRunner = browserObservationRunner;
    this.observationAnalyzer = observationAnalyzer;
  }

  /** Consulta em PLAY uma observação pendente sem criar navegação quando a fila estiver vazia. */
  @Scheduled(fixedDelayString = "${CUSTOMER_AGENT_OBSERVATION_POLL_MS:60000}")
  void runPending() {
    if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
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
    Path workDirectory = null;
    try {
      workDirectory = Files.createTempDirectory("customer-agent-observation-" + id + "-");
      var browserObservation =
          browserObservationRunner.observe(
              String.valueOf(job.get("authorizedSourcesJson")), workDirectory);
      String template =
          Files.readString(
              Path.of("/app/prompts/customer-agent/v1/digital-observation.md"),
              StandardCharsets.UTF_8);
      String prompt =
          template
              .replace("{{PERSONA_JSON}}", String.valueOf(job.get("persona")))
              .replace("{{OBJECTIVE}}", String.valueOf(job.get("objective")))
              .replace(
                  "{{AUTHORIZED_SOURCES_JSON}}", String.valueOf(job.get("authorizedSourcesJson")))
              .replace(
                  "{{BROWSER_OBSERVATION_JSON}}",
                  mapper.writeValueAsString(browserObservation.facts()));
      String raw = observationAnalyzer.analyze(prompt, workDirectory);
      Map<String, Object> result = mapper.readValue(raw, new TypeReference<>() {});
      long personaId = ((Number) ((Map<?, ?>) job.get("persona")).get("id")).longValue();
      uploadScreenshots(personaId, id, browserObservation);
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
                  "motivationalVector", result.get("motivationalVector"),
                  "rawModelResponse", raw,
                  "model", model))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception ex) {
      log.error(
          "Falha no customer-agent ao observar experiencia digital, observationId={}", id, ex);
      reportFailure(id, ex);
      throw new IllegalStateException("Falha na experiencia digital id=" + id, ex);
    } finally {
      deleteWorkDirectory(workDirectory, id);
    }
  }

  /** Envia screenshots ao armazenamento governado, vinculados à persona e observação. */
  private void uploadScreenshots(
      long personaId, long observationId, BrowserObservationRunner.BrowserObservation observation) {
    for (int index = 0; index < observation.screenshots().size(); index++) {
      Path screenshot = observation.screenshots().get(index);
      var body = new LinkedMultiValueMap<String, Object>();
      body.add("observationId", observationId);
      body.add("memoryLayer", "EXTERNAL_OBSERVATION");
      body.add("sourceUrl", sourceUrl(observation.facts(), index));
      body.add("file", new FileSystemResource(screenshot));
      backend
          .post()
          .uri("/api/customer-agent/v1/personas/{personaId}/memory-evidence", personaId)
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(body)
          .retrieve()
          .toBodilessEntity();
    }
  }

  /** Recupera a URL observada associada à posição do screenshot. */
  private String sourceUrl(Map<String, Object> facts, int index) {
    Object pages = facts.get("pages");
    if (pages instanceof java.util.List<?> list && index < list.size()) {
      Object page = list.get(index);
      if (page instanceof Map<?, ?> values) return String.valueOf(values.get("finalUrl"));
    }
    return null;
  }

  /** Remove os arquivos temporários após sucesso ou falha sem ocultar o erro principal. */
  private void deleteWorkDirectory(Path directory, long observationId) {
    if (directory == null) return;
    try (var paths = Files.walk(directory)) {
      paths
          .sorted(java.util.Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (Exception ex) {
                  log.warn(
                      "Falha ao remover artefato temporário da observação, observationId={}, path={}",
                      observationId,
                      path,
                      ex);
                }
              });
    } catch (Exception ex) {
      log.warn(
          "Falha ao limpar diretório temporário da observação, observationId={}",
          observationId,
          ex);
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
