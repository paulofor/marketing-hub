package com.marketinghub.metaadapproverworker;

import java.util.List;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar a fila de criação e edição de imagens atribuída a Têmis. */
@Component
class TemisImageStudioProcessor {
  private static final Logger log = LoggerFactory.getLogger(TemisImageStudioProcessor.class);
  private final TemisImageStudioBackendClient backend;
  private final TemisImageStudioOpenAiClient openAi;
  private final MetaAdApproverProperties properties;

  /** Inicializa o executor sem transferir a decisão de avanço para o worker. */
  TemisImageStudioProcessor(
      TemisImageStudioBackendClient backend,
      TemisImageStudioOpenAiClient openAi,
      MetaAdApproverProperties properties) {
    this.backend = backend;
    this.openAi = openAi;
    this.properties = properties;
  }

  /** Processa produções em paralelo limitado e isola falhas por job. */
  public void processPending() {
    List<TemisImageStudioJob> jobs = backend.claimPending(properties.getImageStudioPendingLimit());
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      jobs.forEach(job -> executor.submit(() -> process(job)));
    }
    log.debug("Ciclo do Estúdio de Imagens de Têmis concluído");
  }

  /** Executa uma única produção e envia o arquivo somente após resposta válida. */
  private void process(TemisImageStudioJob job) {
    String requestJson = "";
    try {
      TemisImageStudioOpenAiClient.Result result = openAi.execute(job);
      requestJson = result.requestJson();
      backend.complete(job, result);
      log.info(
          "Produção visual de Têmis concluída. jobId={} commercialPlanId={}",
          job.jobId(),
          job.commercialPlanId());
    } catch (RuntimeException ex) {
      log.error(
          "Falha na produção visual de Têmis. jobId={} commercialPlanId={}",
          job.jobId(),
          job.commercialPlanId(),
          ex);
      backend.fail(job, ex, requestJson, "");
    }
  }
}
