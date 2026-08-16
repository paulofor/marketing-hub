package com.marketinghub.metaadapproverworker;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
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
  private final TemisImageStudioProcessor imageStudio;
  private final TemisCreativeImprovementProcessor creativeImprovement;
  private final TemisLibraryImageReviewProcessor libraryReview;

  /** Configura o ciclo sem delegar avanço de gate ao worker. */
  public MetaAdApproverScheduler(
      MetaAdApproverBackendClient backend,
      MetaAdApproverCodexRunner runner,
      MetaAdApproverProperties properties,
      TemisImageStudioProcessor imageStudio,
      TemisCreativeImprovementProcessor creativeImprovement,
      TemisLibraryImageReviewProcessor libraryReview) {
    this.backend = backend;
    this.runner = runner;
    this.properties = properties;
    this.imageStudio = imageStudio;
    this.creativeImprovement = creativeImprovement;
    this.libraryReview = libraryReview;
  }

  /** Mantém construtor reduzido para testes isolados do gate histórico. */
  MetaAdApproverScheduler(
      MetaAdApproverBackendClient backend,
      MetaAdApproverCodexRunner runner,
      MetaAdApproverProperties properties) {
    this(backend, runner, properties, null, null, null);
  }

  /** Processa um lote pequeno e isola falhas por criativo. */
  @Scheduled(cron = "30 */1 * * * *")
  public void processPending() {
    List<MetaAdReviewJob> jobs = backend.claimPending(properties.getPendingLimit());
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      jobs.forEach(job -> executor.submit(() -> process(job)));
    }
    runSafely("estúdio de imagens", imageStudio == null ? null : imageStudio::processPending);
    runSafely(
        "retrabalho de criativos",
        creativeImprovement == null ? null : creativeImprovement::processPending);
    runSafely(
        "revisão independente da biblioteca",
        libraryReview == null ? null : libraryReview::processPending);
    log.debug("Ciclo do Aprovador Meta concluído");
  }

  /** Isola cada operação interna para uma fila indisponível não paralisar as demais. */
  private void runSafely(String operation, Runnable action) {
    if (action == null) return;
    try {
      action.run();
    } catch (RuntimeException ex) {
      log.error("Falha no ciclo interno de Têmis. operation={}", operation, ex);
    }
  }

  /** Executa um criativo isoladamente para que um Codex lento não bloqueie o restante do lote. */
  private void process(MetaAdReviewJob job) {
    log.info(
        "Revisão iniciada pelo Aprovador Meta. experimentId={} creativeId={}",
        job.experimentId(),
        job.creativeId());
    try {
      backend.report(job.creativeId(), runner.run(job));
      log.info(
          "Revisão concluída pelo Aprovador Meta. experimentId={} creativeId={}",
          job.experimentId(),
          job.creativeId());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error(
          "Revisão interrompida pelo Aprovador Meta. experimentId={} creativeId={}",
          job.experimentId(),
          job.creativeId(),
          ex);
      backend.fail(job.creativeId(), new IllegalStateException("Execução interrompida", ex));
    } catch (RuntimeException | IOException ex) {
      log.error(
          "Falha na revisão do Aprovador Meta. experimentId={} creativeId={}",
          job.experimentId(),
          job.creativeId(),
          ex);
      backend.fail(job.creativeId(), new IllegalStateException("Falha ao revisar anúncio", ex));
    }
  }
}
