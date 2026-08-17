package com.marketinghub.metaadapproverworker;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: consumir e reportar a fila governada de aprendizado visual pelo backend. */
@Component
public class TemisVisualLearningBackendClient {
  private static final Logger log = LoggerFactory.getLogger(TemisVisualLearningBackendClient.class);
  private final RestClient client;

  /** Inicializa a porta usando somente o backend canônico. */
  public TemisVisualLearningBackendClient(MetaAdApproverProperties properties) {
    this.client = BackendRestClientFactory.create(properties);
  }

  /** Reserva consolidações pendentes pelo endpoint inicial canônico. */
  public List<TemisVisualLearningJob> claimPending(int limit) {
    List<Map<String, Object>> values =
        client
            .get()
            .uri(
                "/api/internal/agent-learning/v1/agents/meta-ad-approver/visual-learning/stage-executions/pending?limit={limit}",
                limit)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    return values == null ? List.of() : values.stream().map(TemisVisualLearningJob::from).toList();
  }

  /** Persiste o replay completo sem promover o playbook. */
  public void complete(Long runId, Map<String, Object> result) {
    client
        .post()
        .uri(
            "/api/internal/agent-learning/v1/agents/meta-ad-approver/visual-learning/stage-executions/{id}/result",
            runId)
        .body(result)
        .retrieve()
        .toBodilessEntity();
  }

  /** Registra falha técnica com stack trace no executor e causa resumida no backend. */
  public void fail(TemisVisualLearningJob job, RuntimeException ex) {
    log.error(
        "Falha na consolidação visual de Têmis. runId={} contextKey={}",
        job.runId(),
        job.contextKey(),
        ex);
    client
        .post()
        .uri(
            "/api/internal/agent-learning/v1/agents/meta-ad-approver/visual-learning/stage-executions/{id}/failure",
            job.runId())
        .body(
            Map.of(
                "producerExecutionId", job.producerExecutionId(),
                "error", rootMessage(ex)))
        .retrieve()
        .toBodilessEntity();
  }

  /** Extrai a causa específica preservada integralmente no log anterior. */
  private String rootMessage(Throwable error) {
    Throwable current = error;
    while (current.getCause() != null) current = current.getCause();
    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }
}
