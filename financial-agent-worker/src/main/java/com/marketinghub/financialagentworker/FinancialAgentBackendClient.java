package com.marketinghub.financialagentworker;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Responsabilidade: consumir e reportar conciliacoes exclusivamente pelo backend. */
@Component
public class FinancialAgentBackendClient {
  private static final Logger log = LoggerFactory.getLogger(FinancialAgentBackendClient.class);
  private final RestClient client;

  public FinancialAgentBackendClient(FinancialAgentProperties properties) {
    client = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
  }

  /** Reserva a conciliacao pendente mais antiga. */
  public FinancialAgentJob claimPending() {
    try {
      return client
          .post()
          .uri("/api/financial-agent/v1/internal/executions/pending/claim")
          .retrieve()
          .body(FinancialAgentJob.class);
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode().value() == 404) return null;
      log.error("Falha no financial-agent-worker ao reservar conciliacao", ex);
      throw ex;
    }
  }

  /** Solicita ao backend a conciliacao diaria idempotente do plano configurado. */
  public void ensureDaily(Long planId) {
    client
        .post()
        .uri(
            "/api/financial-agent/v1/internal/commercial-plans/{planId}/executions/ensure-daily",
            planId)
        .retrieve()
        .toBodilessEntity();
  }

  /** Persiste o relatorio financeiro sem aplicar acoes. */
  public void complete(Long id, Map<String, Object> payload) {
    client
        .post()
        .uri("/api/financial-agent/v1/internal/executions/{id}/complete", id)
        .body(payload)
        .retrieve()
        .toBodilessEntity();
  }

  /** Registra falha tecnica com stack preservada no log do worker. */
  public void fail(Long id, String errorMessage) {
    client
        .post()
        .uri("/api/financial-agent/v1/internal/executions/{id}/fail", id)
        .body(Map.of("errorMessage", errorMessage))
        .retrieve()
        .toBodilessEntity();
  }
}
