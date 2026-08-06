package com.marketinghub.customeragentworker;

import java.io.PrintWriter;
import java.io.StringWriter;
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

/** Responsabilidade: executar avaliacoes pendentes em sandbox somente leitura. */
@Component
public class CustomerAgentScheduler {
  private static final Logger log = LoggerFactory.getLogger(CustomerAgentScheduler.class);
  private final RestClient backend;
  private final String model;
  private final long executionTimeoutMinutes;

  /** Inicializa o cliente do backend, o modelo e o limite operacional da avaliação. */
  public CustomerAgentScheduler(
      @Value("${BACKEND_URL:http://localhost:8080}") String backendUrl,
      @Value("${CUSTOMER_AGENT_MODEL:gpt-5.6-sol}") String model,
      @Value("${CUSTOMER_AGENT_EVALUATION_TIMEOUT_MINUTES:40}") long executionTimeoutMinutes) {
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
    this.model = model;
    this.executionTimeoutMinutes = executionTimeoutMinutes;
  }

  /** Consulta uma pendencia e executa o Codex sem permitir mutacoes. */
  @Scheduled(fixedDelayString = "${CUSTOMER_AGENT_POLL_MS:60000}")
  void runPending() {
    try {
      Map<?, ?> job =
          backend
              .post()
              .uri("/api/customer-agent/v1/internal/evaluations/pending/claim")
              .retrieve()
              .body(Map.class);
      if (job != null) evaluate(job);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() != 404) throw ex;
    }
  }

  /** Monta o prompt versionado e persiste somente a avaliacao simulada. */
  private void evaluate(Map<?, ?> job) {
    long id = ((Number) job.get("id")).longValue();
    try {
      String template = Files.readString(Path.of("/app/prompts/customer-agent/v1/evaluation.md"));
      String prompt =
          template
              .replace("{{PERSONA_JSON}}", String.valueOf(job.get("persona")))
              .replace("{{ASSET_REFERENCE}}", String.valueOf(job.get("assetReference")));
      Path output = Files.createTempFile("customer-agent-evaluation-", ".json");
      Process process =
          new ProcessBuilder(
                  "codex",
                  "exec",
                  "--sandbox",
                  "read-only",
                  "--model",
                  model,
                  "--skip-git-repo-check",
                  prompt)
              .redirectErrorStream(true)
              .redirectOutput(output.toFile())
              .start();
      if (!process.waitFor(executionTimeoutMinutes, TimeUnit.MINUTES)) {
        process.destroyForcibly();
        throw new IllegalStateException(
            "Timeout do Codex após " + executionTimeoutMinutes + " minutos.");
      }
      String raw = Files.readString(output, StandardCharsets.UTF_8);
      Files.deleteIfExists(output);
      if (process.exitValue() != 0) throw new IllegalStateException("Codex falhou: " + raw);
      backend
          .post()
          .uri("/api/customer-agent/v1/internal/evaluations/{id}/complete", id)
          .body(
              Map.of(
                  "assessment",
                  raw,
                  "hypothesisJson",
                  raw,
                  "rawModelResponse",
                  raw,
                  "model",
                  model))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception ex) {
      log.error("Falha no modulo customer-agent ao avaliar ativo, evaluationId={}", id, ex);
      reportFailure("evaluations", id, ex);
      throw new IllegalStateException("Falha ao avaliar ativo no Agente Cliente id=" + id, ex);
    }
  }

  /** Registra no backend a falha terminal para não deixar a avaliação presa em execução. */
  private void reportFailure(String resource, long id, Exception ex) {
    try {
      backend
          .post()
          .uri("/api/customer-agent/v1/internal/{resource}/{id}/fail", resource, id)
          .body(Map.of("error", detailedError(ex)))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception callbackEx) {
      log.error("Falha ao registrar erro do customer-agent, evaluationId={}", id, callbackEx);
    }
  }

  /** Serializa mensagem, causas e stack trace para diagnóstico persistido no backend. */
  static String detailedError(Exception ex) {
    StringWriter detail = new StringWriter();
    ex.printStackTrace(new PrintWriter(detail));
    String value = detail.toString();
    return value.length() <= 16000 ? value : value.substring(0, 16000);
  }
}
