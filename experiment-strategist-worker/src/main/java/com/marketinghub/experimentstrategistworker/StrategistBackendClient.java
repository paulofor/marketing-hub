package com.marketinghub.experimentstrategistworker;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Responsabilidade: consumir a fila e reportar resultados exclusivamente pelo backend. */
@Component
public class StrategistBackendClient {
  private static final Logger log = LoggerFactory.getLogger(StrategistBackendClient.class);
  private final RestClient client;

  /** Configura o cliente do backend canonico. */
  public StrategistBackendClient(WorkerProperties properties) {
    client = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
  }

  /** Reserva a pesquisa mais antiga ou retorna vazio. */
  public StrategistJob claim() {
    try {
      return client
          .post()
          .uri("/api/experiment-strategist/v1/internal/executions/pending/claim")
          .retrieve()
          .body(StrategistJob.class);
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode().value() == 404) return null;
      log.error(
          "Falha ao reservar pesquisa do Estrategista; endpoint={}",
          "/api/experiment-strategist/v1/internal/executions/pending/claim",
          ex);
      throw ex;
    }
  }

  /** Envia o parecer e a resposta bruta ao backend. */
  public void complete(Long id, Map<String, Object> payload) {
    client
        .post()
        .uri("/api/experiment-strategist/v1/internal/executions/{id}/complete", id)
        .body(payload)
        .retrieve()
        .toBodilessEntity();
  }

  /** Envia a causa detalhada da falha ao backend. */
  public void fail(Long id, String error) {
    client
        .post()
        .uri("/api/experiment-strategist/v1/internal/executions/{id}/fail", id)
        .body(Map.of("errorMessage", error))
        .retrieve()
        .toBodilessEntity();
  }
}
