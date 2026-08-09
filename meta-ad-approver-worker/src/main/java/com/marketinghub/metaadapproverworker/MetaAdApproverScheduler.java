package com.marketinghub.metaadapproverworker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: consumir a fila do Aprovador Meta em seu executor independente. */
@Component
public class MetaAdApproverScheduler {
  private static final Logger log = LoggerFactory.getLogger(MetaAdApproverScheduler.class);
  private final MetaAdApproverBackendClient backend;
  private final MetaAdApproverCodexRunner runner;
  private final MetaAdApproverProperties properties;

  /** Configura o ciclo sem delegar avanço de gate ao worker. */
  public MetaAdApproverScheduler(
      MetaAdApproverBackendClient backend,
      MetaAdApproverCodexRunner runner,
      MetaAdApproverProperties properties) {
    this.backend = backend;
    this.runner = runner;
    this.properties = properties;
  }

  /** Processa um lote pequeno e isola falhas por criativo. */
  @Scheduled(cron = "30 */1 * * * *")
  public void processPending() {
    for (MetaAdReviewJob job : backend.claimPending(properties.getPendingLimit())) {
      try {
        backend.report(job.creativeId(), runner.run(job));
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        backend.fail(job.creativeId(), new IllegalStateException("Execução interrompida", ex));
        return;
      } catch (RuntimeException | java.io.IOException ex) {
        backend.fail(job.creativeId(), new IllegalStateException("Falha ao revisar anúncio", ex));
      }
    }
    log.debug("Ciclo do Aprovador Meta concluído");
  }
}
