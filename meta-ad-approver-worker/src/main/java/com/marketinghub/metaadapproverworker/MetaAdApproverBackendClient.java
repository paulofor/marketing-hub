package com.marketinghub.metaadapproverworker;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: consumir a fila e devolver pareceres somente pelos contratos do backend. */
@Component
public class MetaAdApproverBackendClient {
  private static final Logger log = LoggerFactory.getLogger(MetaAdApproverBackendClient.class);
  private final RestClient client;

  /** Configura o cliente do backend como única porta de dados. */
  public MetaAdApproverBackendClient(MetaAdApproverProperties properties) {
    client = BackendRestClientFactory.create(properties);
  }

  /** Reserva revisões no endpoint pending canônico. */
  public List<MetaAdReviewJob> claimPending(int limit) {
    List<Map<String, Object>> values =
        client
            .get()
            .uri(
                "/api/internal/creatives/agent-review/stage-executions/pending?limit={limit}",
                limit)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    return values == null ? List.of() : values.stream().map(MetaAdReviewJob::from).toList();
  }

  /** Persiste o parecer auditável e deixa a decisão de avanço com o backend. */
  public void report(Long creativeId, Map<String, Object> result) {
    client
        .post()
        .uri("/api/internal/creatives/{id}/agent-review/result", creativeId)
        .body(result)
        .retrieve()
        .toBodilessEntity();
  }

  /** Registra falha técnica mantendo o gate fechado. */
  public void fail(Long creativeId, RuntimeException ex) {
    fail(creativeId, Map.of(), ex);
  }

  /** Registra falha do callback sem descartar o request e a resposta bruta já produzidos. */
  public void fail(Long creativeId, Map<String, Object> result, RuntimeException ex) {
    log.error("Falha no Aprovador Meta. creativeId={}", creativeId, ex);
    Map<String, Object> failure = new LinkedHashMap<>();
    failure.put("decision", "FAILED");
    failure.put("error", rootMessage(ex));
    copyAudit(result, failure, "model");
    copyAudit(result, failure, "requestJson");
    copyAudit(result, failure, "responseJson");
    copyAudit(result, failure, "inputTokens");
    copyAudit(result, failure, "outputTokens");
    copyAudit(result, failure, "costUsd");
    report(creativeId, failure);
  }

  /**
   * Copia somente campos de auditoria, sem transformar uma decisão recusada em resultado funcional.
   */
  private void copyAudit(Map<String, Object> source, Map<String, Object> target, String field) {
    if (source != null && source.containsKey(field)) target.put(field, source.get(field));
  }

  /** Extrai a causa específica preservada no log com stack trace. */
  private String rootMessage(Throwable error) {
    Throwable current = error;
    while (current.getCause() != null) current = current.getCause();
    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }
}
