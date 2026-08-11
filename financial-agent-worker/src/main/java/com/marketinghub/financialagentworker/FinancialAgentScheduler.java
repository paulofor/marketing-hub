package com.marketinghub.financialagentworker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: consumir periodicamente uma conciliacao financeira pendente. */
@Component
public class FinancialAgentScheduler {
  private static final Logger log = LoggerFactory.getLogger(FinancialAgentScheduler.class);
  private final FinancialAgentBackendClient backend;
  private final FinancialCodexRunner runner;
  private final FinancialAgentProperties properties;

  /** Configura fila, executor Codex e limites operacionais de Plutus. */
  public FinancialAgentScheduler(
      FinancialAgentBackendClient backend,
      FinancialCodexRunner runner,
      FinancialAgentProperties properties) {
    this.backend = backend;
    this.runner = runner;
    this.properties = properties;
  }

  /** Processa no maximo um relatorio por ciclo para controlar custo e auditoria. */
  @Scheduled(fixedDelay = 60000)
  public void processOne() {
    FinancialAgentJob job = null;
    try {
      VideoProductionCycleReview cycle = backend.pendingVideoCycle();
      if (cycle != null) {
        backend.decideVideoCycle(cycle.id(), runner.reviewVideoCycle(cycle));
        return;
      }
      if (properties.getCommercialPlanId() != null) {
        backend.ensureDaily(properties.getCommercialPlanId());
      }
      job = backend.claimPending();
      if (job == null) return;
      if (!"READ_ONLY_FINANCIAL_RECONCILIATION".equals(job.authorityMode())) {
        throw new IllegalStateException("Autoridade financeira recusada.");
      }
      backend.complete(job.id(), runner.run(job));
    } catch (Exception ex) {
      log.error("Falha no financial-agent-worker jobId={}", job == null ? null : job.id(), ex);
      if (job != null)
        backend.fail(job.id(), ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage());
    }
  }
}
