package com.marketinghub.landinggeneratoragent;

import java.util.Map;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/**
 * Responsabilidade: confirmar atividade da reserva durante preparação e processo sem inferir custo.
 */
final class DedaloClaimHeartbeat implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(DedaloClaimHeartbeat.class);
  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread thread = new Thread(r, "dedalo-claim-heartbeat");
            thread.setDaemon(true);
            return thread;
          });
  private volatile Object taskId;
  private final RestClient backend;
  private final PdeConstructionOutbox outbox;

  /** Inicia acompanhamento independente do carregamento de contexto e do processo do modelo. */
  DedaloClaimHeartbeat(RestClient backend, PdeConstructionOutbox outbox) {
    this.backend = backend;
    this.outbox = outbox;
    executor.scheduleWithFixedDelay(this::pulse, 0, 20, TimeUnit.SECONDS);
  }

  /** Registra falha completa sem apagar identidade nem autorizar nova inferência. */
  private void pulse() {
    try {
      confirm();
    } catch (Exception ex) {
      log.error(
          "Dédalo não confirmou heartbeat da reserva; identidade local preservada. taskId={}",
          taskId,
          ex);
    }
  }

  /** Exige confirmação síncrona antes do início faturável; falha fecha a execução. */
  void confirm() throws java.io.IOException {
    Map<String, Object> claim = outbox.readClaim();
    if (claim == null || claim.get("taskId") == null) return;
    taskId = claim.get("taskId");
    backend
        .put()
        .uri(
            "/api/internal/agent-tasks/landing-generator/stage-executions/{id}/claim-heartbeat",
            claim.get("taskId"))
        .header("X-Dedalo-Claim", String.valueOf(claim.get("claimId")))
        .retrieve()
        .toBodilessEntity();
  }

  /** Encerra apenas o acompanhamento desta passagem, sem tocar no processo ou nos artefatos. */
  @Override
  public void close() {
    executor.shutdownNow();
  }
}
