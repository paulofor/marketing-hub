package com.marketinghub.growthoperatorworker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: consumir periodicamente uma pendencia e reportar seu diagnostico. */
@Component
public class GrowthOperatorScheduler {
  private static final Logger log = LoggerFactory.getLogger(GrowthOperatorScheduler.class);
  private final GrowthOperatorBackendClient backendClient;
  private final CodexReadOnlyRunner runner;
  private final WorkerProperties properties;

  public GrowthOperatorScheduler(
      GrowthOperatorBackendClient backendClient,
      CodexReadOnlyRunner runner,
      WorkerProperties properties) {
    this.backendClient = backendClient;
    this.runner = runner;
    this.properties = properties;
  }

  /** Processa no maximo um diagnostico por ciclo para preservar custo e auditabilidade. */
  @Scheduled(fixedDelay = 60000)
  public void processOne() {
    GrowthOperatorJob job = null;
    try {
      ensureAutomaticCycleWithoutBlockingPendingQueue();
      job = backendClient.claimPending();
      if (job == null) {
        return;
      }
      if (!"READ_ONLY_DIAGNOSIS".equals(job.authorityMode())) {
        throw new IllegalStateException(
            "Job recusado por autoridade diferente de somente leitura.");
      }
      backendClient.complete(job.id(), runner.run(job));
    } catch (Exception ex) {
      log.error(
          "Falha no modulo growth-operator-worker ao diagnosticar jobId={} planId={}",
          job == null ? null : job.id(),
          job == null ? properties.getCommercialPlanId() : job.commercialPlanId(),
          ex);
      if (job != null) {
        backendClient.fail(
            job.id(), ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage());
      }
    }
  }

  /**
   * Avalia o ciclo automatico configurado sem permitir que sua indisponibilidade bloqueie trabalhos
   * ja persistidos na fila principal.
   */
  private void ensureAutomaticCycleWithoutBlockingPendingQueue() {
    if (properties.getCommercialPlanId() == null) {
      return;
    }
    try {
      backendClient.ensureAutomaticCycle(properties.getCommercialPlanId());
    } catch (Exception ex) {
      log.warn(
          "Ciclo automatico indisponivel no growth-operator-worker; a fila pendente continuara sendo consumida planId={}",
          properties.getCommercialPlanId(),
          ex);
    }
  }
}
