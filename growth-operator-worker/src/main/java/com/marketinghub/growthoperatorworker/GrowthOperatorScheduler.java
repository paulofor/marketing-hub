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

  public GrowthOperatorScheduler(
      GrowthOperatorBackendClient backendClient, CodexReadOnlyRunner runner) {
    this.backendClient = backendClient;
    this.runner = runner;
  }

  /** Processa no maximo um diagnostico por ciclo para preservar custo e auditabilidade. */
  @Scheduled(fixedDelay = 60000)
  public void processOne() {
    GrowthOperatorJob job = backendClient.claimPending();
    if (job == null) {
      return;
    }
    try {
      if (!"READ_ONLY_DIAGNOSIS".equals(job.authorityMode())) {
        throw new IllegalStateException(
            "Job recusado por autoridade diferente de somente leitura.");
      }
      backendClient.complete(job.id(), runner.run(job));
    } catch (Exception ex) {
      log.error(
          "Falha no modulo growth-operator-worker ao diagnosticar jobId={} planId={}",
          job.id(),
          job.commercialPlanId(),
          ex);
      backendClient.fail(
          job.id(), ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage());
    }
  }
}
