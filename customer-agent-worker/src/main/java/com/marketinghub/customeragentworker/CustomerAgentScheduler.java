package com.marketinghub.customeragentworker;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final CustomerEvaluationCodexRunner codexRunner;
  @Autowired private AutomaticExecutionControl automaticExecution;

  /** Inicializa o cliente do backend e o executor estruturado da avaliação. */
  public CustomerAgentScheduler(
      @Value("${BACKEND_URL:http://localhost:8080}") String backendUrl,
      CustomerEvaluationCodexRunner codexRunner) {
    this.backend = RestClient.builder().baseUrl(backendUrl).build();
    this.codexRunner = codexRunner;
  }

  /** Consulta uma pendência em PLAY e executa o Codex sem permitir mutações. */
  @Scheduled(fixedDelayString = "${CUSTOMER_AGENT_POLL_MS:60000}")
  void runPending() {
    if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
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

  /** Executa a versão solicitada e persiste baseline e simulação comportamental separados. */
  private void evaluate(Map<?, ?> job) {
    long id = ((Number) job.get("id")).longValue();
    try {
      Map<String, String> result = codexRunner.run(id, job);
      backend
          .post()
          .uri("/api/customer-agent/v1/internal/evaluations/{id}/complete", id)
          .body(result)
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
