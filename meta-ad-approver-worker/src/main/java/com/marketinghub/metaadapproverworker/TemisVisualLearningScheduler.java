package com.marketinghub.metaadapproverworker;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: consumir assincronamente consolidações visuais sem bloquear revisões. */
@Component
@ConditionalOnProperty(
    name = "meta-ad-approver.execution-role",
    havingValue = "review",
    matchIfMissing = true)
public class TemisVisualLearningScheduler {
  private static final Logger log = LoggerFactory.getLogger(TemisVisualLearningScheduler.class);
  private final TemisVisualLearningBackendClient backend;
  private final TemisVisualLearningRunner runner;

  /** Inicializa a rotina com fila e consolidador independentes. */
  public TemisVisualLearningScheduler(
      TemisVisualLearningBackendClient backend, TemisVisualLearningRunner runner) {
    this.backend = backend;
    this.runner = runner;
  }

  /** Processa no máximo uma amostra por ciclo para preservar capacidade das revisões comerciais. */
  @Scheduled(cron = "45 */2 * * * *")
  public void processPending() {
    List<TemisVisualLearningJob> jobs = backend.claimPending(1);
    jobs.forEach(this::process);
  }

  /** Isola a falha de uma consolidação e mantém as demais filas do revisor disponíveis. */
  private void process(TemisVisualLearningJob job) {
    try {
      backend.complete(job.runId(), runner.run(job));
      log.info(
          "Aprendizado visual de Têmis consolidado. runId={} contextKey={}",
          job.runId(),
          job.contextKey());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Consolidação visual interrompida. runId={}", job.runId(), ex);
      backend.fail(job, new IllegalStateException("Consolidação interrompida", ex));
    } catch (RuntimeException | IOException ex) {
      log.error("Falha na consolidação visual. runId={}", job.runId(), ex);
      backend.fail(
          job, ex instanceof RuntimeException runtime ? runtime : new IllegalStateException(ex));
    }
  }
}
