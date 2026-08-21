package com.marketinghub.growthoperatorworker;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Responsabilidade: consumir pendencias e reportar resultados exclusivamente pelo backend. */
@Component
public class GrowthOperatorBackendClient {
  private static final Logger log = LoggerFactory.getLogger(GrowthOperatorBackendClient.class);
  private final RestClient client;

  public GrowthOperatorBackendClient(WorkerProperties properties) {
    this.client = RestClient.builder().baseUrl(properties.getBackendUrl()).build();
  }

  /** Reserva a proxima pendencia ou retorna vazio quando a fila nao possui trabalho. */
  public GrowthOperatorJob claimPending() {
    try {
      return client
          .post()
          .uri("/api/growth-operator/v1/internal/executions/pending/claim")
          .retrieve()
          .body(GrowthOperatorJob.class);
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode().value() == 404) return null;
      log.error(
          "Falha no modulo growth-operator-worker ao reservar pendencia em {}",
          "/api/growth-operator/v1/internal/executions/pending/claim",
          ex);
      throw ex;
    }
  }

  /** Pede ao backend para avaliar a criacao do proximo ciclo do plano configurado. */
  public void ensureAutomaticCycle(Long planId) {
    client
        .post()
        .uri("/api/growth-operator/v1/internal/commercial-plans/{planId}/executions/ensure", planId)
        .retrieve()
        .toBodilessEntity();
  }

  /** Pede ao backend para garantir continuidade de todos os planos comerciais abertos. */
  public void ensureActivePlanCycles() {
    client
        .post()
        .uri("/api/growth-operator/v1/internal/commercial-plans/executions/ensure-active")
        .retrieve()
        .toBodilessEntity();
  }

  /** Envia o diagnostico estruturado e preserva a resposta bruta para auditoria. */
  public void complete(Long id, Map<String, Object> payload) {
    client
        .post()
        .uri("/api/growth-operator/v1/internal/executions/{id}/complete", id)
        .body(payload)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            (request, response) -> {
              throw new IllegalStateException(
                  "Backend recusou o diagnostico: " + response.getStatusCode());
            })
        .toBodilessEntity();
  }

  /** Registra falha tecnica sem transformar a execucao em sucesso. */
  public void fail(Long id, String errorMessage) {
    client
        .post()
        .uri("/api/growth-operator/v1/internal/executions/{id}/fail", id)
        .body(Map.of("errorMessage", errorMessage))
        .retrieve()
        .toBodilessEntity();
  }

  /** Reserva uma atividade BPM específica atribuída ao Operador de Crescimento. */
  public Map<String, Object> claimBpmTask(String processCode, String activityId) {
    List<Map<String, Object>> pending =
        client
            .get()
            .uri(
                "/api/internal/agent-tasks/{agent}/stage-executions/pending?processCode={processCode}&activityId={activityId}",
                "growth-operator",
                processCode,
                activityId)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    return pending == null || pending.isEmpty() ? null : pending.get(0);
  }

  /** Conclui uma atividade BPM preservando resultado, evidências e consumo do modelo. */
  public void completeBpmTask(Long taskId, Map<String, Object> payload) {
    client
        .post()
        .uri(
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/result",
            "growth-operator",
            taskId)
        .body(payload)
        .retrieve()
        .toBodilessEntity();
  }

  /** Bloqueia uma atividade BPM sem liberar a próxima etapa do processo. */
  public void failBpmTask(Long taskId, Map<String, Object> payload) {
    client
        .post()
        .uri(
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure",
            "growth-operator",
            taskId)
        .body(payload)
        .retrieve()
        .toBodilessEntity();
  }
}
