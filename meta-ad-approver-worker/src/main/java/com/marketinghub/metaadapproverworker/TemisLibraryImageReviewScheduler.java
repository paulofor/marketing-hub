package com.marketinghub.metaadapproverworker;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Responsabilidade: acionar a execução independente que revisa entregáveis produzidos. */
@Component
class TemisLibraryImageReviewProcessor {
  private static final Logger log = LoggerFactory.getLogger(TemisLibraryImageReviewProcessor.class);
  private final TemisImageStudioBackendClient backend;
  private final TemisLibraryImageReviewRunner runner;
  private final MetaAdApproverProperties properties;

  /** Inicializa o gate separado da rotina de produção. */
  TemisLibraryImageReviewProcessor(
      TemisImageStudioBackendClient backend,
      TemisLibraryImageReviewRunner runner,
      MetaAdApproverProperties properties) {
    this.backend = backend;
    this.runner = runner;
    this.properties = properties;
  }

  /** Reserva e revisa um lote somente depois da persistência do arquivo DRAFT. */
  public void processPending() {
    List<TemisLibraryReviewJob> jobs =
        backend.claimReviews(properties.getImageStudioPendingLimit());
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      jobs.forEach(job -> executor.submit(() -> process(job)));
    }
  }

  /** Isola falha do parecer e mantém o entregável bloqueado em DRAFT. */
  private void process(TemisLibraryReviewJob job) {
    try {
      backend.reportReview(job.assetId(), runner.run(job));
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error(
          "Revisão da Biblioteca interrompida. assetId={} planId={}",
          job.assetId(),
          job.commercialPlanId(),
          ex);
      reportFailure(job, ex);
    } catch (RuntimeException | IOException ex) {
      log.error(
          "Falha na revisão independente da Biblioteca. assetId={} planId={}",
          job.assetId(),
          job.commercialPlanId(),
          ex);
      reportFailure(job, ex);
    }
  }

  /** Persiste FAILED usando execução diferente da produção para liberar diagnóstico auditável. */
  private void reportFailure(TemisLibraryReviewJob job, Exception ex) {
    backend.reportReview(
        job.assetId(),
        Map.of(
            "decision", "FAILED",
            "reviewerExecutionId", UUID.randomUUID().toString(),
            "summary", "Revisão independente falhou tecnicamente",
            "requestJson", "",
            "responseJson", "",
            "error", rootMessage(ex)));
  }

  /** Extrai a causa específica preservada no log com stack trace. */
  private String rootMessage(Throwable error) {
    Throwable current = error;
    while (current.getCause() != null) current = current.getCause();
    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }
}
